# BiteStream — Real-Time Event-Driven Food Delivery & Dispatch Platform

**BiteStream** is a production-grade, event-driven food delivery and dispatch backend built with **Spring Boot 4.1.2**, **Java 25 LTS**, **Apache Maven 3.10.0**, **Apache Kafka** (KRaft mode), **PostgreSQL 16** (Hybrid POJO + JSONB) with **Flyway** migrations, and **Redis 7**.

The platform coordinates the entire food order lifecycle asynchronously:
1. **Order Intake**: Customer places an order for a restaurant.
2. **Streaming Event Backbone**: Order event is streamed via Apache Kafka (`food.orders.placed`).
3. **Kitchen & Dispatch Fan-Out**: Real-time consumers notify the restaurant kitchen queue and dispatch the optimal available agent from a fleet of **20 delivery partners** based on proximity.
4. **Logistics & Queue Distance ETA**: Dynamically calculates preparation and transit delivery times based on distance bands (**2.0 km, 3.0 km, 4.0 km, 5.0 km**) and active store queue depth.
5. **Two-Step OTP Security Protocol**: High-security handover powered by **Redis with native sub-millisecond TTL**:
   - **Pickup OTP**: Shared between Delivery Agent and Restaurant upon food handover (15-min TTL).
   - **Delivery OTP**: Shared between Customer and Delivery Agent upon doorstep packet delivery (30-min TTL).

---

## 🏛️ Architectural Overview

```
                      +-----------------------------+
                      |   Customer / Web Client     |
                      +--------------+--------------+
                                     |
                          POST /api/orders (Distance: 2..5km)
                                     v
                      +-----------------------------+
                      |     Order Service (REST)    |
                      +--------------+--------------+
                                     |
                          Writes Order (POJO+JSONB)
                                     v
                      +-----------------------------+
                      |    PostgreSQL Database      |
                      +-----------------------------+
                                     |
                      Publishes 'food.orders.placed'
                                     v
                          +=====================+
                          ||    APACHE KAFKA   ||
                          +=====================+
                         /                       \
  Consumes 'food.orders.placed'             Consumes 'food.orders.placed'
                       /                           \
                      v                             v
       +----------------------------+  +----------------------------+
       | Restaurant Kitchen Consumer|  |  Delivery Dispatch Consumer |
       +--------------+-------------+  +--------------+-------------+
                      |                               |
       Notifies store KOT & queue      Matches 1 of 20 Agents,
       updates status: PREPARING       generates 6-digit Pickup OTP
                      |                               |
       Emits: 'food.restaurant.notif'  Emits: 'food.delivery.assigned'
                                                      |
                                                      v
                                        +---------------------------+
                                        |   Redis Distributed Cache |
                                        |   (TTL-backed OTP Store)  |
                                        +---------------------------+
                                                      |
                   ------------------------------------------------------
                  |                                                      |
                  v                                                      v
        [Step 1: Restaurant Pickup]                          [Step 2: Customer Delivery]
      POST /api/deliveries/{id}/pickup                     POST /api/deliveries/{id}/complete
      Validates Pickup OTP from Redis                      Validates Delivery OTP from Redis
      Emits 'food.delivery.pickup'                         Emits 'food.delivery.completed'
      Generates Customer Delivery OTP                      Frees agent back to AVAILABLE
```

---

## 💡 Key Architectural Decisions

### 1. Cache Selection: Redis vs. EHCache

| Dimension | Redis (Selected Choice ✅) | EHCache | Why Redis is Optimized for this Solution |
| :--- | :--- | :--- | :--- |
| **Architecture** | Distributed, out-of-process in-memory server | In-process JVM heap / local memory | Multiple Spring Boot replicas & Kafka consumers need shared access to the same OTPs and agent state without replication lag. |
| **OTP Expiration (TTL)** | Native key-level TTL eviction (`SETEX` / `PSETEX`) | Cache-level or manual entry expiry | Redis automatically purges expired OTPs after 15/30 minutes, avoiding custom scheduler sweeps and preventing brute force replay. |
| **Concurrency & Atomic Ops** | Single-threaded atomic commands (`GETDEL`, Lua scripts) | Requires synchronization / locking | Guarantees atomic one-time consumption of Pickup and Delivery OTPs across concurrent microservices. |
| **Geospatial & Fleet Tracking** | Built-in Geospatial indexes (`GEOADD`, `GEODIST`) | Not natively supported | Ideal for querying nearest available delivery agents to restaurants in sub-millisecond latency. |
| **Scalability** | Clustered independently from app instances | Constrained by JVM heap size & GC pauses | App restarts or deployments do not lose active OTPs or cached menus. |

### 2. Data Modeling: Hybrid Relational POJO + PostgreSQL JSONB

Rather than choosing pure relational or pure document format, this solution adopts an **industry-standard hybrid design**:

- **Relational POJO SQL Tables** for high-integrity transactional core:
  - `restaurants`: Store metadata, location coordinates, contact numbers.
  - `menu_items`: Dish names, descriptions, prices, prep times.
  - `delivery_agents`: 20 pickup partners with status (`AVAILABLE`, `ASSIGNED`, `BUSY`), vehicle types, phone numbers.
  - `orders` & `order_items`: Financial accounting, order items, foreign keys with strict constraints.
  - `delivery_assignments`: Links orders to assigned delivery partners.
- **JSONB Columns** for flexible, semi-structured, and audit attributes:
  - `orders.order_metadata` (`JSONB`): Stores customer instructions, dynamic discounts, tip amount, payment mode, and routing metadata without altering SQL schemas.
  - `orders.event_logs` (`JSONB`): Chronological, indexed audit log of every Kafka event state transition (`ORDER_PLACED`, `DELIVERY_ASSIGNED`, `PICKED_UP`, `DELIVERED`).
  - `restaurants.operating_hours` (`JSONB`): Variable weekly schedules and break timings per day.
  - **Indexed with PostgreSQL GIN (Generalized Inverted Index)**: Enables lightning-fast querying on internal JSON attributes (`CREATE INDEX ... USING gin (order_metadata)`).

---

## ⏱️ Queue Delay & Distance-based Delivery Time Estimation

The application implements realistic logistics scheduling:

### 1. Distance Transit Calculations (Average City Speed: 20 km/h + 4 min buffer)
- **2.0 km**: $\frac{2.0}{20} \times 60 = 6\text{ min} + 4\text{ min buffer} = \mathbf{10\text{ minutes}}$
- **3.0 km**: $\frac{3.0}{20} \times 60 = 9\text{ min} + 4\text{ min buffer} = \mathbf{13\text{ minutes}}$
- **4.0 km**: $\frac{4.0}{20} \times 60 = 12\text{ min} + 4\text{ min buffer} = \mathbf{16\text{ minutes}}$
- **5.0 km**: $\frac{5.0}{20} \times 60 = 15\text{ min} + 4\text{ min buffer} = \mathbf{19\text{ minutes}}$

### 2. Kitchen Preparation Time & Queue Position Delay
- **Base Kitchen Prep**: Maximum preparation time among the ordered dishes (typically 12–20 minutes).
- **Queue Position Penalty**: Each pending order currently ahead in the kitchen queue adds **3 minutes**.
- **Total Delivery ETA**:
  $$\text{Total Delivery ETA} = \text{Base Item Prep Time} + (\text{Orders Ahead} \times 3\text{ min}) + \text{Distance Travel Time}$$

---

## 🔐 Two-Step OTP Confirmation Flow

```
1. Customer Places Order 
      │
      ▼
2. Kafka Consumer assigns Delivery Partner & generates 6-digit PICKUP OTP
      │ (Stored in Redis with 15-minute TTL)
      ▼
3. Delivery Partner arrives at Restaurant
      │ Partner provides Pickup OTP -> POST /api/deliveries/{orderId}/pickup
      ▼
4. System verifies Pickup OTP with Redis
      │ Invalids Pickup OTP
      │ Changes Order Status -> PICKED_UP
      │ Generates 6-digit DELIVERY OTP for Customer (Redis 30-min TTL)
      │ Emits 'food.delivery.pickup' event
      ▼
5. Partner arrives at Customer doorstep (distance: 2, 3, 4, 5 km)
      │ Customer provides Delivery OTP -> POST /api/deliveries/{orderId}/complete
      ▼
6. System verifies Delivery OTP with Redis
      │ Marks Order Status -> DELIVERED
      │ Releases Delivery Agent -> AVAILABLE
      │ Emits 'food.delivery.completed' event
```

---

## 🗄️ Flyway Database Migrations & Seed Data

The project contains two migration scripts executed automatically on startup:

- **`V1__init_schema.sql`**: Creates tables, constraints, foreign keys, and PostgreSQL GIN indexes for JSONB columns.
- **`V2__seed_data.sql`**: Seeds:
  - **5 Stores / Restaurants**:
    1. *Spice Symphony Gourmet* (North Indian & Mughlai)
    2. *Bella Napoli Woodfire Pizzeria* (Authentic Italian)
    3. *Dragon Wok Pan-Asian* (Chinese & Thai Street Food)
    4. *Green Delight Organic & Salads* (Healthy Bowls & Vegan)
    5. *Burger Crave Smokehouse* (Gourmet Burgers & BBQ)
  - **10 Menu Items per Restaurant** (50 menu items total) with distinct names, descriptions, prices, and prep times.
  - **20 Delivery Pickup Agents** with full names, phone numbers, vehicle types (*Electric Scooter, Honda Activa, Ather 450X, Yamaha FZ, etc.*), and initial coordinates.

---

## 🚀 How to Run

### Docker Compose Profiles Workflow

BiteStream provides flexible Docker profiles to suit different development environments:

| Workflow | Command | Components |
|---|---|---|
| **Infrastructure Only** | `docker compose up -d` | PostgreSQL (`5432`), Redis (`6379`), Kafka (`9092`/`9094`) |
| **Full Stack in Docker** | `docker compose --profile app-container up -d` | Infra + BiteStream App (`8080`) |
| **Full Stack + Observability** | `docker compose --profile app-container --profile logging up -d` | Infra + App + Prometheus (`9090`), Grafana (`3000`), Loki (`3100`), Promtail |

```bash
# 1. Run full application stack inside Docker
docker compose --profile app-container up -d

# 2. Or run infrastructure in Docker and Spring Boot locally
docker compose up -d
./mvnw clean spring-boot:run
```

---

## 📊 Centralized Observability & Logging (Prometheus, Grafana, Loki)

BiteStream includes an enterprise observability stack under [`logging/`](logging/):

* **Grafana Portal**: [http://localhost:3000](http://localhost:3000) (Credentials: `admin` / `admin`)
  * **JVM & Metrics Dashboard**: `BiteStream - Spring Boot Metrics & Observability`
  * **Logs Dashboard**: `BiteStream - Centralized Logs & Audit`
* **Prometheus Targets**: [http://localhost:9090/targets](http://localhost:9090/targets) (Scrapes `bitestream-app:8080/actuator/prometheus`)
* **Loki Log Aggregator**: [http://localhost:3100/ready](http://localhost:3100/ready)
* **Promtail Log Shipper**: Collects container logs & `/var/log/bitestream/*.log`

To run the logging stack independently:
```powershell
# Windows
.\logging\start-logging.ps1

# Linux / macOS
./logging/start-logging.sh
```

---

## 👨‍💻 Developer Guide & Contributor Documentation

For architecture deep-dives, Kafka topic topologies, Redis OTP TTL rules, database migration workflows, and local developer setup, please refer to the complete handbook:

👉 **[BiteStream Developer Guide (DEVELOPER.md)](DEVELOPER.md)**

---

## 📖 Interactive OpenAPI 3 / Swagger Documentation

BiteStream provides fully interactive **OpenAPI 3 / Swagger UI** documentation out of the box:

* **Swagger UI Interface**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI Specification (JSON)**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
* **OpenAPI Specification (YAML)**: [http://localhost:8080/v3/api-docs.yaml](http://localhost:8080/v3/api-docs.yaml)

---

## 📡 REST API Walkthrough (cURL Examples)

### 1. View Available Restaurants (Cached in Redis)
```bash
curl -X GET http://localhost:8080/api/restaurants
```

### 2. View 10 Menu Items for a Store (e.g., Restaurant 1)
```bash
curl -X GET http://localhost:8080/api/restaurants/1/menu
```

### 3. View 20 Delivery Agents Fleet
```bash
curl -X GET http://localhost:8080/api/delivery-agents
```

### 4. Place an Order (Distance: 3.0 km)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Rahul Verma",
    "customerPhone": "+91-9876540001",
    "customerAddress": "Apartment 402, Lotus Towers, City Center",
    "restaurantId": 1,
    "distanceKm": 3.0,
    "specialInstructions": "Extra spicy, please do not ring the doorbell",
    "paymentMethod": "UPI_GPAY",
    "items": [
      {
        "menuItemId": 1,
        "quantity": 2
      },
      {
        "menuItemId": 5,
        "quantity": 3
      }
    ]
  }'
```

*Response:*
```json
{
  "orderId": 1,
  "orderNumber": "ORD-1763105000000-482",
  "status": "PLACED",
  "restaurantId": 1,
  "restaurantName": "Spice Symphony Gourmet",
  "customerName": "Rahul Verma",
  "distanceKm": 3.0,
  "queuePosition": 1,
  "estimatedPrepTimeMinutes": 18,
  "estimatedTravelTimeMinutes": 13,
  "estimatedTotalDeliveryTimeMinutes": 31,
  "totalAmount": 40.48,
  "metadata": {
    "specialInstructions": "Extra spicy, please do not ring the doorbell",
    "paymentMethod": "UPI_GPAY",
    "priority": "STANDARD"
  }
}
```

### 5. Inspect Generated OTPs (Inspection endpoint for testing)
```bash
curl -X GET http://localhost:8080/api/deliveries/1/otp
```
*Output:*
```json
{
  "orderId": 1,
  "activePickupOtpInRedis": "684920",
  "activeDeliveryOtpInRedis": "EXPIRED_OR_NOT_ISSUED"
}
```

### 6. Confirm Pickup with Pickup OTP
Delivery agent arrives at restaurant and provides the Pickup OTP:
```bash
curl -X POST http://localhost:8080/api/deliveries/1/pickup \
  -H "Content-Type: application/json" \
  -d '{
    "otp": "684920"
  }'
```
*Output:*
```json
{
  "success": true,
  "message": "Pickup confirmed successfully! Delivery partner is en route.",
  "orderId": 1,
  "orderNumber": "ORD-1763105000000-482",
  "status": "PICKED_UP",
  "deliveryAgentName": "Aarav Sharma",
  "generatedDeliveryOtp": "512394",
  "actionTimestamp": "2026-11-14T08:20:00Z"
}
```

### 7. Confirm Final Delivery with Customer Delivery OTP
Delivery agent reaches customer's address (3.0 km away) and verifies Delivery OTP:
```bash
curl -X POST http://localhost:8080/api/deliveries/1/complete \
  -H "Content-Type: application/json" \
  -d '{
    "otp": "512394"
  }'
```
*Output:*
```json
{
  "success": true,
  "message": "Order delivered successfully to customer!",
  "orderId": 1,
  "orderNumber": "ORD-1763105000000-482",
  "status": "DELIVERED",
  "deliveryAgentName": "Aarav Sharma",
  "actionTimestamp": "2026-11-14T08:33:00Z"
}
```

### 8. View Order Audit Trail & JSONB Event Stream
```bash
curl -X GET http://localhost:8080/api/orders/1
```
Notice the `lifecycleEvents` array containing timestamps and sources for every Kafka event transition:
```json
"lifecycleEvents": [
  {
    "eventType": "ORDER_PLACED",
    "topic": "food.orders.placed",
    "timestamp": "2026-11-14T08:15:00Z",
    "description": "Order created with Queue position #1, Distance 3.0 km",
    "triggeredBy": "API_GATEWAY"
  },
  {
    "eventType": "DELIVERY_ASSIGNED",
    "topic": "food.delivery.assigned",
    "timestamp": "2026-11-14T08:15:01Z",
    "description": "Assigned partner Aarav Sharma (Electric Scooter). Pickup OTP generated.",
    "triggeredBy": "DISPATCHER"
  },
  {
    "eventType": "KITCHEN_PREPARING",
    "topic": "food.restaurant.notifications",
    "timestamp": "2026-11-14T08:15:02Z",
    "description": "Store Spice Symphony Gourmet started cooking order. Position in queue: #1",
    "triggeredBy": "RESTAURANT_KOT"
  },
  {
    "eventType": "DELIVERY_PICKED_UP",
    "topic": "food.delivery.pickup",
    "timestamp": "2026-11-14T08:20:00Z",
    "description": "Agent Aarav Sharma picked up order from restaurant. Customer Delivery OTP generated.",
    "triggeredBy": "RESTAURANT_AGENT_APP"
  },
  {
    "eventType": "ORDER_DELIVERED",
    "topic": "food.delivery.completed",
    "timestamp": "2026-11-14T08:33:00Z",
    "description": "Order delivered successfully to customer by Aarav Sharma",
    "triggeredBy": "CUSTOMER_AGENT_APP"
  }
]
```

---

## 🧪 Testing
Run the test suite covering distance calculations, queue scheduling penalties, and Redis OTP TTL validations:
```bash
mvn test
```
All unit tests validate:
- 2, 3, 4, 5 km transit times.
- Queue delays per pending order.
- Secure 6-digit numeric OTP generation.
- Redis key insertion, TTL management, and atomic one-time validation.

---

## 🚀 GitHub Actions CI/CD Pipeline

BiteStream features production-ready GitHub Actions workflows configured for both **`main`** and **`develop`** branches:

| Pipeline | Workflow File | Trigger Conditions | Branch & Environment Matrix | Key Responsibilities |
| :--- | :--- | :--- | :--- | :--- |
| **Continuous Integration (CI)** | [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | `push`, `pull_request`, `workflow_dispatch` | **`main`** & **`develop`** | - JDK 25 (Temurin) + Maven cache<br>- Unit & integration test execution (`./mvnw clean test`)<br>- Surefire test report publishing<br>- Application JAR packaging & artifact archiving<br>- Docker multi-stage build validation |
| **Continuous Deployment (CD)** | [`.github/workflows/cd.yml`](.github/workflows/cd.yml) | `push`, `workflow_dispatch` (manual) | `develop` &rarr; **Staging**<br>`main` &rarr; **Production** | - Target environment & tag resolution<br>- Docker Buildx container image build<br>- Multi-tag publication to GitHub Container Registry (GHCR)<br>- Staged environment rollout |

