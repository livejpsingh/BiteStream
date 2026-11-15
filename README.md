# BiteStream — Real-Time Event-Driven Food Delivery & Dispatch Platform

[![Java 25 LTS](https://img.shields.io/badge/Java-25%20LTS-orange.svg?style=flat&logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-22-dd0031.svg?style=flat&logo=angular)](https://angular.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-3.4-38bdf8.svg?style=flat&logo=tailwindcss)](https://tailwindcss.com/)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7.0%20(KRaft)-black.svg?style=flat&logo=apachekafka)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%20(JSONB)-336791.svg?style=flat&logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7%20(TTL%20OTP)-dc382d.svg?style=flat&logo=redis)](https://redis.io/)
[![Grafana](https://img.shields.io/badge/Grafana-11.2-F46800.svg?style=flat&logo=grafana)](https://grafana.com/)
[![Prometheus](https://img.shields.io/badge/Prometheus-2.54-E6522C.svg?style=flat&logo=prometheus)](https://prometheus.io/)

**BiteStream** is a production-grade, event-driven food delivery and dispatch platform featuring a high-performance **Spring Boot 4.1.2** backend (Java 25 LTS, Maven 3.10.0), a modern **Angular 22** web console (Tailwind CSS, Nginx reverse proxy), **Apache Kafka** in KRaft mode, **PostgreSQL 16** (hybrid relational POJO + JSONB with GIN indexing), **Redis 7** (sub-millisecond TTL OTP store), and full-stack **Observability** (Prometheus, Grafana, Loki, Promtail).

The platform coordinates the entire food order lifecycle asynchronously:
1. **Order Intake**: Customer places an order via the Angular Web UI or REST API.
2. **Streaming Event Backbone**: Orders are published to Apache Kafka (`food.orders.placed`).
3. **Kitchen & Dispatch Fan-Out**: Real-time Kafka consumers notify the restaurant kitchen queue and dispatch the optimal available rider from a fleet of **20 delivery partners** based on proximity and vehicle type.
4. **Logistics & Distance ETA**: Dynamically calculates transit and kitchen prep times based on distance bands (**2.0 km, 3.0 km, 4.0 km, 5.0 km**) and active store queue depth.
5. **Two-Step OTP Security Protocol**: High-security physical handover powered by **Redis with native sub-millisecond TTL**:
   - **Pickup OTP**: Shared between Delivery Rider and Restaurant upon food handover (15-min TTL).
   - **Delivery OTP**: Shared between Customer and Delivery Rider upon doorstep handover (30-min TTL).
6. **Unified Observability**: All web traffic, backend event logs, and JVM metrics stream in real time to Prometheus, Grafana, and Loki.

---

## 🏛️ Architectural Overview

```
                      +-------------------------------------------------+
                      |     BiteStream Angular Dashboard (Port: 4200)   |
                      |          (Tailwind CSS, Nginx Reverse Proxy)    |
                      +------------------------+------------------------+
                                               |
                                     HTTP Requests (/api/*)
                                               v
                      +-------------------------------------------------+
                      |     BiteStream Spring Boot Core (Port: 8080)    |
                      |            (Java 25 LTS, Spring Boot 4.1.2)     |
                      +-------------------+-----------------------------+
                                          |
                           Writes Order (POJO + JSONB)
                                          v
                      +-------------------------------------------------+
                      |     PostgreSQL 16 Database (Port: 5432)         |
                      |       (Hybrid Relational + GIN Indexed JSONB)   |
                      +-------------------+-----------------------------+
                                          |
                           Publishes 'food.orders.placed'
                                          v
                              +=======================+
                              ||     APACHE KAFKA     ||
                              ||  (KRaft Mode :9092)  ||
                              +=======================+
                             /                         \
      Consumes 'food.orders.placed'               Consumes 'food.orders.placed'
                           /                             \
                          v                               v
           +-----------------------------+ +-----------------------------+
           | Restaurant Kitchen Consumer | |  Delivery Dispatch Consumer |
           +--------------+--------------+ +--------------+--------------+
                          |                               |
           Notifies store KOT queue,       Matches 1 of 20 fleet riders,
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

## 🌐 Stack Port Matrix & Live Endpoints

| Service | Container Name | Host Port | Internal Port | Purpose / Default URL |
|---|---|---|---|---|
| **Angular Dashboard** | `bitestream-frontend` | **`4200`** | `80` | [http://localhost:4200](http://localhost:4200) (Web UI + Nginx Proxy) |
| **Spring Boot API** | `bitestream-app` | **`8080`** | `8080` | [http://localhost:8080](http://localhost:8080) (Core Backend Engine) |
| **Swagger UI** | `bitestream-app` | **`8080`** | `8080` | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| **OpenAPI Docs** | `bitestream-app` | **`8080`** | `8080` | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |
| **Grafana** | `bitestream-grafana` | **`3000`** | `3000` | [http://localhost:3000](http://localhost:3000) (`admin` / `admin`) |
| **Prometheus** | `bitestream-prometheus` | **`9090`** | `9090` | [http://localhost:9090/targets](http://localhost:9090/targets) |
| **Grafana Loki** | `bitestream-loki` | **`3100`** | `3100` | [http://localhost:3100/ready](http://localhost:3100/ready) |
| **PostgreSQL 16** | `bitestream-postgres` | **`5432`** | `5432` | JDBC: `jdbc:postgresql://localhost:5432/postgres` |
| **Redis 7** | `bitestream-redis` | **`6379`** | `6379` | In-memory OTP TTL store & menu cache |
| **Apache Kafka** | `bitestream-kafka` | **`9092`**, **`9094`** | `9092` | KRaft Event Broker (External: `localhost:9094`) |

---

## 💡 Key Architectural Decisions

### 1. Cache Selection: Redis vs. EHCache

| Dimension | Redis (Selected Choice ✅) | EHCache | Why Redis is Optimized for this Solution |
| :--- | :--- | :--- | :--- |
| **Architecture** | Distributed, out-of-process in-memory server | In-process JVM heap / local memory | Multiple Spring Boot replicas & Kafka consumers share the exact same OTP state and delivery rider fleet without replication delay. |
| **OTP Expiration (TTL)** | Native key-level TTL eviction (`SETEX` / `PSETEX`) | Cache-level or manual entry expiry | Redis automatically purges expired OTPs after 15/30 minutes, avoiding custom scheduler sweeps and preventing brute force replay. |
| **Concurrency & Atomic Ops** | Single-threaded atomic commands (`GETDEL`, Lua scripts) | Requires synchronization / locking | Guarantees atomic one-time consumption of Pickup and Delivery OTPs across concurrent microservices. |
| **Geospatial Tracking** | Built-in Geospatial indexes (`GEOADD`, `GEODIST`) | Not natively supported | Ideal for querying nearest available delivery agents to restaurants with sub-millisecond latency. |
| **Scalability** | Clustered independently from app instances | Constrained by JVM heap size & GC pauses | App restarts or deployments do not lose active OTPs or cached menus. |

### 2. Data Modeling: Hybrid Relational POJO + PostgreSQL JSONB

- **Relational POJO SQL Tables** for high-integrity transactional entities:
  - `restaurants`: Store metadata, location coordinates, contact numbers.
  - `menu_items`: Dish names, descriptions, prices, prep times.
  - `delivery_agents`: 20 pickup partners with status (`AVAILABLE`, `ASSIGNED`, `BUSY`), vehicle types, phone numbers.
  - `orders` & `order_items`: Financial accounting, items, foreign keys with strict constraints.
  - `delivery_assignments`: Links orders to assigned delivery partners.
- **JSONB Columns** for flexible, semi-structured, and audit attributes:
  - `orders.order_metadata` (`JSONB`): Customer instructions, dynamic voucher discounts, payment mode, and routing metadata.
  - `orders.event_logs` (`JSONB`): Chronological, indexed audit log of every Kafka event state transition (`ORDER_PLACED`, `DELIVERY_ASSIGNED`, `PICKED_UP`, `DELIVERED`).
  - `restaurants.operating_hours` (`JSONB`): Variable weekly schedules and break timings per day.
  - **Indexed with PostgreSQL GIN (Generalized Inverted Index)**: Lightning-fast querying on internal JSON attributes.

### 3. Frontend Architecture: Scoped Component CSS Isolation

The Angular application ([`frontend/`](frontend/)) implements an industry-standard two-tier CSS structure:
- **Global Styles (`frontend/src/styles.css`)**: Tailwind base directives, shared design tokens (`--color-brand`), and shared component classes (`.glass-card`, `.btn-brand`, `.badge-tag`).
- **Scoped Styles (`*.component.css`)**: Angular's native `ViewEncapsulation.Emulated` isolates styles to each component host. New features can be added without bleeding into or breaking existing screens.

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
      │ (Stored in Redis with 15-minute TTL: otp:pickup:{orderId})
      ▼
3. Delivery Partner arrives at Restaurant
      │ Partner provides Pickup OTP -> POST /api/deliveries/{orderId}/pickup
      ▼
4. System verifies Pickup OTP with Redis
      │ Invalids Pickup OTP atomically
      │ Changes Order Status -> PICKED_UP
      │ Generates 6-digit DELIVERY OTP for Customer (Redis 30-min TTL: otp:delivery:{orderId})
      │ Emits 'food.delivery.pickup' event
      ▼
5. Partner arrives at Customer doorstep (distance: 2, 3, 4, 5 km)
      │ Customer provides Delivery OTP -> POST /api/deliveries/{orderId}/complete
      ▼
6. System verifies Delivery OTP with Redis
      │ Marks Order Status -> DELIVERED
      │ Releases Delivery Agent back to -> AVAILABLE
      │ Emits 'food.delivery.completed' event
```

---

## 💻 Angular Web UI Dashboard (`frontend/`)

The web dashboard is located in [`frontend/`](frontend/) and provides an interactive control center:

- **Store Catalog**: Interactive store picker showing 5 restaurants, cuisine badges, opening hours, and 10 dishes per restaurant with prep times.
- **Order Placement Builder**: Interactive distance selector (**2.0 km, 3.0 km, 4.0 km, 5.0 km**), live ETA calculations, cart quantity adjustments, and direct Kafka order dispatch.
- **Live Dispatch Tracker**:
  - **Order State Machine**: Visual timeline tracking `ORDER_PLACED` &rarr; `KITCHEN_PREPARING` &rarr; `PICKED_UP` &rarr; `DELIVERED`.
  - **Two-Step OTP Cards**: Active Redis OTP hints with one-click Auto-Fill and atomic verification.
  - **Fleet Grid**: Live status of 20 delivery riders (Electric Scooter, Ather 450X, Yamaha FZ, Honda Activa).
  - **Kafka Event Mesh**: Active topics and consumer group assignments.

### Running the Frontend Locally (Dev Mode)
```bash
cd frontend
npm install
npm start
```
*Accessible at [http://localhost:4200](http://localhost:4200) with automatic proxy forwarding to `:8080`.*

---

## 🐳 Docker Compose Profiles & Deployment

BiteStream provides flexible Docker profiles to suit different development and deployment needs:

| Workflow | Command | Active Containers |
|---|---|---|
| **Infrastructure Only** | `docker compose up -d` | PostgreSQL (`5432`), Redis (`6379`), Kafka (`9092`/`9094`) |
| **Full Stack (App + UI)** | `docker compose --profile app-container up -d` | Infra + Spring Boot Backend (`8080`) + Angular Frontend (`4200`) |
| **Full Stack + Observability** | `docker compose --profile app-container --profile logging up -d` | All 9 Containers: Infra + Backend + Frontend + Prometheus + Grafana + Loki + Promtail |
| **Observability Only** | `docker compose --profile logging up -d` | Prometheus (`9090`), Grafana (`3000`), Loki (`3100`), Promtail |

```bash
# 1. Run complete stack (Backend + Frontend + Infrastructure)
docker compose --profile app-container up -d

# 2. Run complete stack with monitoring & centralized logging
docker compose --profile app-container --profile logging up -d

# 3. View live application logs
docker logs -f bitestream-app
docker logs -f bitestream-frontend

# 4. Tear down stack
docker compose --profile app-container --profile logging down
```

---

## 📊 Centralized Observability & Logging (Prometheus, Grafana, Loki)

BiteStream includes an enterprise observability stack under [`logging/`](logging/):

* **Grafana Portal**: [http://localhost:3000](http://localhost:3000) (Credentials: `admin` / `admin`)
  * **Metrics Dashboard**: `BiteStream - Spring Boot Metrics & Observability`
    - Live JVM heap %, CPU %, thread counts, HikariCP database pool, and HTTP request throughput by URI.
  * **Logs Dashboard**: `BiteStream - Centralized Logs & Audit`
    - Live Spring Boot backend log stream (`{job="bitestream-app"}`).
    - Live Angular frontend / Nginx web traffic log stream (`{container="bitestream-frontend"}`).
    - Full unified multi-container logs (`{container=~"bitestream-.*"}`).
* **Prometheus Targets**: [http://localhost:9090/targets](http://localhost:9090/targets) (Scrapes `bitestream-app:8080/actuator/prometheus`)
* **Loki Log Aggregator**: [http://localhost:3100/ready](http://localhost:3100/ready)
* **Promtail Shipper**: Collects container logs & `/var/log/bitestream/*.log`

To run the logging stack independently:
```powershell
# Windows
.\logging\start-logging.ps1

# Linux / macOS
./logging/start-logging.sh
```

---

## 📖 Interactive OpenAPI 3 / Swagger Documentation

BiteStream provides interactive OpenAPI 3 documentation out of the box:

* **Swagger UI Interface**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **OpenAPI Specification (JSON)**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

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
      { "menuItemId": 1, "quantity": 2 },
      { "menuItemId": 5, "quantity": 3 }
    ]
  }'
```

### 5. Inspect Active OTPs from Redis
```bash
curl -X GET http://localhost:8080/api/deliveries/1/otp
```

### 6. Confirm Restaurant Pickup (Step 1 OTP)
```bash
curl -X POST http://localhost:8080/api/deliveries/1/pickup \
  -H "Content-Type: application/json" \
  -d '{ "otp": "684920" }'
```

### 7. Confirm Customer Doorstep Delivery (Step 2 OTP)
```bash
curl -X POST http://localhost:8080/api/deliveries/1/complete \
  -H "Content-Type: application/json" \
  -d '{ "otp": "512394" }'
```

---

## 🧪 Testing & Quality Assurance

Run the automated test suite covering logistics calculation, queue delay logic, and Redis OTP TTL validations:
```bash
./mvnw clean test
```

---

## 👨‍💻 Developer Guide & Contributor Handbook

For deep-dive architecture specs, Kafka consumer groups, Flyway migrations, and code standards, refer to:

👉 **[BiteStream Developer Guide (DEVELOPER.md)](DEVELOPER.md)**

---

## 🚀 GitHub Actions CI/CD Pipeline

BiteStream features production-ready GitHub Actions workflows for both **`main`** and **`develop`** branches:

| Pipeline | Workflow File | Trigger Conditions | Branch & Environment Matrix | Key Responsibilities |
| :--- | :--- | :--- | :--- | :--- |
| **Continuous Integration (CI)** | [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | `push`, `pull_request`, `workflow_dispatch` | **`main`** & **`develop`** | - JDK 25 (Temurin) + Maven cache<br>- Unit & integration test execution (`./mvnw clean test`)<br>- Surefire test report publishing<br>- Application JAR packaging & artifact archiving<br>- Docker multi-stage build validation |
| **Continuous Deployment (CD)** | [`.github/workflows/cd.yml`](.github/workflows/cd.yml) | `push`, `workflow_dispatch` (manual) | `develop` &rarr; **Staging**<br>`main` &rarr; **Production** | - Target environment & tag resolution<br>- Docker Buildx container image build<br>- Multi-tag publication to GitHub Container Registry (GHCR)<br>- Staged environment rollout |
