# BiteStream — Developer Guide & Contributor Handbook

Welcome to the **BiteStream Developer Guide**. This document contains comprehensive architecture references, local setup instructions, Docker compose profiles, observability guides, database workflows, and engineering standards for developing and maintaining the BiteStream platform.

---

## 📋 Table of Contents

1. [Architecture & System Design](#-architecture--system-design)
2. [Prerequisites & Development Toolchain](#-prerequisites--development-toolchain)
3. [Quick Start & Local Environment](#-quick-start--local-environment)
4. [Docker Compose Profiles & Workflows](#-docker-compose-profiles--workflows)
5. [Centralized Observability & Logging Stack](#-centralized-observability--logging-stack)
6. [Database Management & Migrations](#-database-management--migrations)
7. [Apache Kafka Event Stream Architecture](#-apache-kafka-event-stream-architecture)
8. [Redis Caching & Two-Step OTP Protocol](#-redis-caching--two-step-otp-protocol)
9. [OpenAPI 3 / Swagger API Documentation](#-openapi-3--swagger-api-documentation)
10. [Testing & Code Quality](#-testing--code-quality)
11. [Git Workflow & CI/CD Pipelines](#-git-workflow--cicd-pipelines)

---

## 🏛️ Architecture & System Design

BiteStream is designed around **event-driven choreography** using Apache Kafka, PostgreSQL with native JSONB, Redis in-memory key-value cache, and Spring Boot:

```
                            +-----------------------------+
                            |     Client / API Caller     |
                            +--------------+--------------+
                                           |
                                [HTTP POST /api/orders]
                                           v
+-----------------------------------------------------------------------------------+
| BiteStream Spring Boot Core (Java 25 LTS, Spring Boot 4.1.2)                      |
|                                                                                   |
|  [REST Controllers]  -->  [Order / Restaurant Services]  -->  [JPA / Hibernate]   |
|         |                                                           |             |
|         v                                                           v             |
|  [Kafka Template]                                             [PostgreSQL 16]     |
|         |                                                     (POJO + JSONB)      |
+---------|-----------------------------------------------------------+-------------+
          |                                                           ^
          | (Publishes 'food.orders.placed')                          |
          v                                                           |
+=====================================================================|=============+
| Apache Kafka 3.7.0 (KRaft mode)                                     |             |
|                                                                     |             |
|  Topic: food.orders.placed -----------------------------------------+             |
|    |--> Consumer Group: food-delivery-orchestrator-group (Matches rider & OTP)    |
|    |--> Consumer Group: food-restaurant-kitchen-group (Notifies KOT queue)       |
|                                                                                   |
|  Topic: food.delivery.assigned ---> Consumer Group: food-delivery-rider-app-group |
|  Topic: food.delivery.pickup   ---> Consumer Group: food-delivery-lifecycle-group |
|  Topic: food.delivery.completed---> Consumer Group: food-delivery-lifecycle-group |
+===================================================================================+
                                           |
                                           v
                             +---------------------------+
                             |    Redis 7 Distributed    |
                             |   - Pickup OTP (15m TTL)  |
                             |   - Delivery OTP (30m TTL)|
                             |   - Restaurant Menu Cache |
                             +---------------------------+
```

---

## 🛠️ Prerequisites & Development Toolchain

Ensure the following tools are installed on your workstation:

| Tool | Version | Recommended Installation | Verification Command |
|---|---|---|---|
| **JDK** | **Java 25 LTS** | [Eclipse Temurin 25](https://adoptium.net/) or SDKMAN (`sdk install java 25-tem`) | `java -version` |
| **Apache Maven** | **3.10.0+** | Included via Maven Wrapper (`./mvnw` or `mvnw.cmd`) | `./mvnw -version` |
| **Docker Desktop** | **27.x+** | Docker Desktop on Windows/macOS/Linux with Docker Compose v2 | `docker compose version` |
| **Git** | **2.40+** | Official Git | `git --version` |

---

## 🚀 Quick Start & Local Environment

### 1. Clone & Setup Workspace
```bash
git clone https://github.com/livejpsingh/BiteStream.git
cd BiteStream
```

### 2. Configure Environment Properties
The application includes configuration templates:
- `src/main/resources/application.yml` (Local development defaults)
- `src/main/resources/application-docker.yml` (Containerized defaults)

Copy `.env.example` to `.env` if custom secrets or credentials are needed:
```bash
cp .env.example .env
```

### 3. Build with Maven
```bash
# On Linux/macOS
./mvnw clean package -DskipTests

# On Windows PowerShell
.\mvnw.cmd clean package -DskipTests
```

---

## 🐳 Docker Compose Profiles & Workflows

The root `docker-compose.yml` supports multiple **Compose Profiles** to accommodate different developer workflows:

### Profile Matrix

| Workflow | Command | Started Containers |
|---|---|---|
| **Local Dev (Infra Only)** | `docker compose up -d` | `bitestream-postgres`, `bitestream-redis`, `bitestream-kafka` |
| **Full App in Docker** | `docker compose --profile app-container up -d` | Infra + `bitestream-app` (`:8080`) |
| **Full Stack + Observability** | `docker compose --profile app-container --profile logging up -d` | Infra + App + `prometheus`, `grafana`, `loki`, `promtail` |
| **Observability Only** | `docker compose --profile logging up -d` | `bitestream-prometheus`, `bitestream-grafana`, `bitestream-loki`, `bitestream-promtail` |

### Starting and Stopping Services
```bash
# 1. Start all infrastructure and run application locally in IDE
docker compose up -d
./mvnw spring-boot:run

# 2. Rebuild and start containerized application
docker compose --profile app-container up -d --build app

# 3. View live application logs
docker logs -f bitestream-app

# 4. Tear down all containers and clean network
docker compose --profile app-container --profile logging down
```

---

## 📊 Centralized Observability & Logging Stack

BiteStream includes a production-ready observability sub-project located at [`logging/`](file:///c:/workspace/public/BiteStream/logging).

```
                      +---------------------------------------+
                      |       BiteStream Spring Boot App      |
                      |  - Actuator (/actuator/prometheus)    |
                      |  - Logback (/var/log/bitestream/*.log)|
                      +-------------------+-------------------+
                                          |
                   +----------------------+----------------------+
                   | (HTTP Metrics Scrape)                       | (File & Container Logs)
                   v                                             v
        +--------------------+                         +-------------------+
        |     Prometheus     |                         |     Promtail      |
        |  (Port: 9090)      |                         |  (Port: 9080)     |
        +---------+----------+                         +---------+---------+
                  |                                              | (Push Logs)
                  |                                              v
                  |                                    +-------------------+
                  |                                    |   Grafana Loki    |
                  |                                    |   (Port: 3100)    |
                  |                                    +---------+---------+
                  |                                              |
                  +-----------------------+----------------------+
                                          | (Datasources)
                                          v
                              +-----------------------+
                              |        Grafana        |
                              |     (Port: 3000)      |
                              +-----------------------+
```

### Observability Endpoints & Credentials

| Service | Port | Default URL | Credentials | Purpose |
|---|---|---|---|---|
| **Grafana** | `3000` | [http://localhost:3000](http://localhost:3000) | `admin` / `admin` | Unified monitoring & log exploration portal |
| **Prometheus** | `9090` | [http://localhost:9090](http://localhost:9090) | None | Time-series metrics engine & scraping target |
| **Grafana Loki** | `3100` | [http://localhost:3100](http://localhost:3100) | None | Log aggregation endpoint & LogQL processor |
| **Promtail** | `9080` | [http://localhost:9080](http://localhost:9080) | None | Log collection and forwarder agent |
| **Spring Actuator**| `8080` | [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus) | None | Raw Prometheus metrics scrape endpoint |

### Pre-Configured Grafana Dashboards
Located in [`logging/grafana/dashboards/`](file:///c:/workspace/public/BiteStream/logging/grafana/dashboards):
1. **`BiteStream - Spring Boot Metrics & Observability`**:
   - Real-time JVM memory heap usage & garbage collection pauses.
   - HTTP throughput (requests/sec) and latency percentiles (P95, P99).
   - HikariCP database pool active, idle, and pending connections.
   - Process CPU usage and active thread counts.
2. **`BiteStream - Centralized Logs & Audit`**:
   - Centralized live log stream auto-scraped from Docker and Logback.
   - Log volume categorized by level (`INFO`, `WARN`, `ERROR`).
   - Filterable error search console with stack traces.

### Independent Startup of Logging Sub-Project
```powershell
# Windows
cd logging
.\start-logging.ps1

# Linux / macOS
cd logging
chmod +x start-logging.sh
./start-logging.sh
```

---

## 🗄️ Database Management & Migrations

### Hybrid Relational POJO + JSONB Architecture
BiteStream stores transactional entities in relational tables while leveraging PostgreSQL JSONB for flexible attributes and immutable event audit logs:

- **Relational Tables**: `restaurants`, `menu_items`, `delivery_agents`, `orders`, `order_items`, `delivery_assignments`.
- **JSONB Attributes**:
  - `orders.order_metadata`: Stores instructions, payment modes, dynamic voucher discounts.
  - `orders.event_logs`: Chronological log of Kafka event state transitions.
  - `restaurants.operating_hours`: Weekly schedule breakdown.
- **GIN Indexes**: Fast search on JSONB fields (`CREATE INDEX ... USING gin(order_metadata)`).

### Seeding Test Data
Initial seed data containing **5 restaurants**, **50 menu items**, and **20 delivery partners** is located at `src/main/resources/db/migration/V2__seed_data.sql`.

To manually apply or refresh seed data into the PostgreSQL container:
```powershell
# Windows PowerShell
Get-Content src/main/resources/db/migration/V2__seed_data.sql | docker exec -i bitestream-postgres psql -U postgres -d postgres

# Linux / macOS
docker exec -i bitestream-postgres psql -U postgres -d postgres < src/main/resources/db/migration/V2__seed_data.sql
```

---

## 📨 Apache Kafka Event Stream Architecture

### Topics & Event Handlers

| Topic | Key | Payload | Producing Service | Consumer Groups |
|---|---|---|---|---|
| `food.orders.placed` | `orderId` | `OrderPlacedEvent` | `OrderService` | `food-delivery-orchestrator-group`<br>`food-restaurant-kitchen-group` |
| `food.restaurant.notifications` | `restaurantId` | `RestaurantNotificationEvent` | `RestaurantKitchenConsumer` | `food-restaurant-kitchen-group` |
| `food.delivery.assigned` | `orderId` | `DeliveryAssignedEvent` | `DeliveryDispatchConsumer` | `food-delivery-rider-app-group` |
| `food.delivery.pickup` | `orderId` | `DeliveryPickupEvent` | `DeliveryService` | `food-delivery-lifecycle-group` |
| `food.delivery.completed` | `orderId` | `DeliveryCompletedEvent` | `DeliveryService` | `food-delivery-lifecycle-group` |

---

## 🔐 Redis Caching & Two-Step OTP Protocol

1. **Pickup OTP (Restaurant Handover)**:
   - Generated when delivery rider is assigned.
   - Stored in Redis with **15-minute TTL**: `otp:pickup:{orderId}`.
   - Validated atomically upon restaurant food handover via `POST /api/deliveries/{orderId}/pickup`.
2. **Delivery OTP (Customer Handover)**:
   - Generated automatically after pickup validation.
   - Stored in Redis with **30-minute TTL**: `otp:delivery:{orderId}`.
   - Validated atomically at the customer's doorstep via `POST /api/deliveries/{orderId}/complete`.
   - Releases the delivery rider back to `AVAILABLE`.

---

## 📖 OpenAPI 3 / Swagger API Documentation

Interactive API exploration is available at:
- **Swagger UI Console**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI v3 JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 🧪 Testing & Code Quality

Execute the automated test suite covering logistics calculation, queue delay logic, and Redis OTP TTL validations:
```bash
./mvnw clean test
```

### Running Specific Tests
```bash
./mvnw test -Dtest=LogisticsServiceTest
./mvnw test -Dtest=OtpServiceTest
```

---

## 🌳 Git Workflow & CI/CD Pipelines

### Branching Strategy
- **`main`**: Production-ready branch. Automatically deployed to production via CD pipeline.
- **`develop`**: Integration branch for active feature development. Deployed to staging.
- **`feature/<name>`**: Feature branches branched from `develop` and merged via Pull Requests.

### GitHub Actions CI/CD
- **CI Workflow (`.github/workflows/ci.yml`)**:
  - Runs on every `push` and `pull_request` to `main` and `develop`.
  - Sets up Java 25 LTS, caches Maven dependencies, and runs `./mvnw clean test`.
  - Packages artifacts and validates Docker builds.
- **CD Workflow (`.github/workflows/cd.yml`)**:
  - Automatically builds multi-platform Docker container images.
  - Pushes images to GitHub Container Registry (GHCR).
