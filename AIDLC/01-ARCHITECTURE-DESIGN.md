# 01 - BiteStream System Architecture & Component Blueprint

## 1. High-Level Architecture Diagram

```mermaid
flowchart TD
    subgraph Clients["External Consumers & Clients"]
        C1["Customer Mobile App / Web"]
        C2["Restaurant Kitchen Tablet / KOT"]
        C3["Delivery Partner Rider App"]
    end

    subgraph Edge["API Gateway & Ingress Layer"]
        GW["REST API Gateway (Port 8080)"]
    end

    subgraph AppCluster["Spring Boot 4 Core Services"]
        OC["Order Service"]
        ETA["ETA & Distance Engine"]
        DC["Delivery Dispatch Service"]
        RC["Restaurant & Menu Service"]
        OTP["Two-Step OTP Engine"]
        SEC["Global Exception & Validation Handler"]
    end

    subgraph KafkaCluster["Apache Kafka Cluster (Event Backbone)"]
        T1[("Topic: food.orders.placed")]
        T2[("Topic: food.restaurant.notifications")]
        T3[("Topic: food.delivery.assigned")]
        T4[("Topic: food.delivery.pickup")]
        T5[("Topic: food.delivery.completed")]
    end

    subgraph Storage["Persistent & In-Memory Storage"]
        PG[("PostgreSQL 16 Database\n- Relational Core + Foreign Keys\n- JSONB Metadata & Event Logs\n- GIN Inverted Indexes")]
        RD[("Redis 7 In-Memory Cache\n- Pickup OTP (TTL: 15m)\n- Delivery OTP (TTL: 30m)\n- Restaurant Menu Cache")]
    end

    %% Client Interactions
    C1 -->|"1. POST /api/orders"| GW
    C2 -->|"4. Verifies Pickup OTP"| GW
    C3 -->|"6. Verifies Delivery OTP"| GW

    %% Gateway to App
    GW --> OC
    GW --> DC
    GW --> RC

    %% App to Kafka
    OC -->|"Publishes Order Placed"| T1
    DC -->|"Publishes Delivery Assigned"| T3
    DC -->|"Publishes Delivery Picked Up"| T4
    DC -->|"Publishes Delivery Completed"| T5

    %% Kafka Listeners
    T1 -->|"Consumes Placed Event"| OC
    OC -->|"Emits Store KOT"| T2
    T2 -->|"Alerts Store Tablet"| C2
    T3 -->|"Pushes Dispatch & Pickup OTP"| C3
    T4 -->|"Pushes Delivery OTP to Customer"| C1

    %% App to Databases
    OC <-->|"ACID Relational Writes + JSONB"| PG
    DC <-->|"Assignment Status & Timestamps"| PG
    RC <-->|"Read Menus / Store Profiles"| PG
    OTP <-->|"Atomic Key Verification & TTL Eviction"| RD
    RC <-->|"Cache-Aside Pattern (@Cacheable)"| RD
```

---

## 2. C4 Container Diagram

```mermaid
flowchart LR
    subgraph Users["System Actors"]
        Customer["Customer\n[Person]"]
        StoreOwner["Store Chef / Manager\n[Person]"]
        Rider["Delivery Rider\n[Person]"]
    end

    subgraph FoodDeliveryPlatform["Food Delivery System Boundary"]
        SpringBootApp["Spring Boot Backend Service\n[Java 25, Spring Boot 4.1.2]\nHandles REST endpoints, Kafka orchestrators, logistics algorithms"]
        Kafka["Apache Kafka Event Bus\n[Kafka 3.7 KRaft]\nMessage streaming broker for order & delivery events"]
        PostgresDB["PostgreSQL Database\n[Postgres 16]\nRelational entities & JSONB order payloads with Flyway"]
        RedisCache["Redis Distributed Cache\n[Redis 7]\nFast in-memory OTP TTL storage & menu cache"]
    end

    Customer -->|"Places order, receives updates [HTTPS/JSON]"| SpringBootApp
    StoreOwner -->|"Receives KOT, checks queue [HTTPS/JSON]"| SpringBootApp
    Rider -->|"Receives dispatch, validates OTPs [HTTPS/JSON]"| SpringBootApp

    SpringBootApp -->|"Publishes & consumes domain events [TCP/PLAINTEXT]"| Kafka
    SpringBootApp -->|"Reads & writes orders, items, agents [JDBC/SQL]"| PostgresDB
    SpringBootApp -->|"Sets and verifies OTPs with TTL [RESP/TCP]"| RedisCache
```

---

## 3. Detailed Component Architecture

### 3.1 Order Management Component (`com.fooddelivery.service.OrderService`)
* **Responsibilities**:
  - Validates menu items against the designated store.
  - Computes active queue depth of orders ahead at the store.
  - Queries `EtaCalculationService` for preparation and transit ETA.
  - Constructs the relational `Order` entity, embeds `OrderMetadata` and initial `EventLogEntry` in PostgreSQL `jsonb` columns.
  - Emits `OrderPlacedEvent` to Kafka topic `food.orders.placed`.

### 3.2 Logistics & ETA Engine (`com.fooddelivery.service.EtaCalculationService`)
* **Responsibilities**:
  - Distance modeling: Computes transit time for 2.0 km, 3.0 km, 4.0 km, 5.0 km at 20 km/h baseline urban speed with 4 minutes buffer.
  - Queue penalty modeling: Ingests queue position count ($Q$) and adds 3 minutes delay per order ahead in the restaurant kitchen:
    $$\text{ETA}_{\text{total}} = \text{PrepTime}_{\text{item}} + (Q \times 3\text{ min}) + \left(\frac{D}{20} \times 60 + 4\right)$$

### 3.3 Two-Step OTP Security Service (`com.fooddelivery.service.OtpService`)
* **Responsibilities**:
  - Generates cryptographically secure 6-digit numerical codes using `SecureRandom`.
  - Writes OTPs to Redis with distinct TTL windows:
    - `food_delivery:otp:pickup:{orderId}`: **15 minutes TTL**
    - `food_delivery:otp:delivery:{orderId}`: **30 minutes TTL**
  - Executes atomic comparison and key deletion upon successful verification (`GETDEL` semantics).

### 3.4 Delivery Dispatch Service (`com.fooddelivery.service.DeliveryAgentService`)
* **Responsibilities**:
  - Maintains fleet status (`AVAILABLE`, `ASSIGNED`, `BUSY`, `OFFLINE`) for the 20 delivery agents.
  - Calculates Euclidean/Haversine distance between restaurant coordinates and agent coordinates.
  - Atomically assigns the closest available partner to the order.

---

## 4. Cache Evaluation Matrix: Redis vs. EHCache

| Dimension | Redis Cache (Selected ✅) | EHCache (Rejected ❌) | Architectural Impact |
| :--- | :--- | :--- | :--- |
| **Topology** | Distributed external in-memory data store | In-process JVM heap / local memory | Essential for multi-instance microservices. When multiple pods consume Kafka events, Redis ensures global cache coherence without cluster lag. |
| **TTL Eviction** | Native sub-millisecond per-key TTL (`SETEX`) | Cache-level or manual entry expiration | Pickup and Delivery OTPs naturally expire after 15/30 minutes without requiring custom background sweeper threads. |
| **Concurrency & Atomic Ops** | Single-threaded atomic primitives (`GETDEL`, `SET NX`) | Thread locking / JVM synchronization | Guarantees atomic verification of OTPs and prevents double-assignment of delivery agents. |
| **Geospatial Queries** | Built-in Geospatial commands (`GEOADD`, `GEODIST`) | Not natively supported | Enables lightning-fast radius searches for nearest available agents around restaurant coordinates. |
| **Resilience & Restarts** | Independent lifecycle, AOF/RDB persistence | Cleared on application JVM restart | Application rolling restarts do not wipe active OTPs or unexpired menu caches. |

---

## 5. Hybrid Data Architecture: POJO Relational + JSONB

```mermaid
flowchart TD
    subgraph PostgreSQL["PostgreSQL 16 (Hybrid Storage Model)"]
        subgraph Relational["POJO Relational Tables (ACID & Constraints)"]
            T_REST["restaurants\n(id, name, address, lat, lng, contact)"]
            T_MENU["menu_items\n(id, restaurant_id, name, price, prep_time)"]
            T_AGENT["delivery_agents\n(id, name, phone, vehicle, status)"]
            T_ORDER["orders\n(id, order_number, restaurant_id, status, total)"]
            T_ITEMS["order_items\n(id, order_id, menu_item_id, qty, price)"]
            T_ASSIGN["delivery_assignments\n(id, order_id, agent_id, status, otps)"]
        end

        subgraph JsonbCol["PostgreSQL JSONB Dynamic Columns"]
            J_HOURS["operating_hours JSONB\n(Store hours schedule by day)"]
            J_META["order_metadata JSONB\n(Customer instructions, discounts, tips, GPS)"]
            J_EVENTS["event_logs JSONB\n(Chronological audit stream of Kafka state changes)"]
        end

        subgraph GIN["GIN Indexes (Sub-millisecond Search)"]
            GIN_META["idx_orders_metadata_gin\nUSING gin (order_metadata)"]
            GIN_EVENTS["idx_orders_events_gin\nUSING gin (event_logs)"]
        end
    end

    T_REST -.-> J_HOURS
    T_ORDER -.-> J_META
    T_ORDER -.-> J_EVENTS
    J_META -.-> GIN_META
    J_EVENTS -.-> GIN_EVENTS
```
