# BiteStream Observability & Centralized Logging Sub-Project

This sub-project provides enterprise-grade observability and centralized logging for the **BiteStream** real-time food delivery and event-driven dispatch platform.

---

## 🏛️ Architecture Overview

```
                      +---------------------------------------+
                      |       BiteStream Spring Boot App      |
                      |  - Actuator (/actuator/prometheus)    |
                      |  - Logback (/var/log/bitestream/*.log)|
                      +-------------------+-------------------+
                                          |
                      +-------------------+-------------------+
                      |      Angular Frontend (Nginx)         |
                      |  - Port 4200 (Web UI & Reverse Proxy) |
                      |  - Nginx Access & Error Logs          |
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
                                          | (Datasources: prometheus-ds & loki-ds)
                                          v
                              +-----------------------+
                              |        Grafana        |
                              |     (Port: 3000)      |
                              +-----------------------+
```

### Components

1. **Prometheus (`:9090`)**:
   - Collects time-series metrics from BiteStream Spring Boot Actuator (`/actuator/prometheus`) every 5 seconds.
   - Scrapes JVM heap memory, garbage collection, CPU %, active threads, HikariCP database pool, and HTTP request throughput/latencies.
   - Provisioned with datasource UID: `prometheus-ds`.

2. **Grafana Loki (`:3100`)**:
   - High-performance, horizontally-scalable log aggregation engine optimized for Grafana.
   - Ingests application logs from Spring Boot (`{job="bitestream-app"}`) and web traffic from Angular Nginx (`{container="bitestream-frontend"}`).
   - Provisioned with datasource UID: `loki-ds`.

3. **Promtail (`:9080`)**:
   - Lightweight log shipping agent that tails `/var/log/bitestream/*.log` and Docker container stdout/stderr.
   - Extracts timestamps, log levels (`INFO`, `WARN`, `ERROR`), logger names, and threads via regex before streaming to Loki.

4. **Grafana (`:3000`)**:
   - Web visualization portal with pre-provisioned datasources and dashboards.
   - Default credentials: `admin` / `admin`.

---

## 📁 Directory Structure

```
logging/
├── docker-compose.yml                     # Standalone compose file for logging stack
├── README.md                              # This documentation
├── prometheus/
│   └── prometheus.yml                     # Prometheus scrape targets & intervals
├── loki/
│   └── loki-config.yml                    # Loki storage, schema, and limits configuration
├── promtail/
│   └── promtail-config.yml                # Promtail file & Docker log scraping pipeline
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/
│   │   │   └── datasources.yml            # Auto-provisions prometheus-ds & loki-ds
│   │   └── dashboards/
│   │       └── dashboards.yml             # Auto-loads pre-built dashboards
│   └── dashboards/
│       ├── bitestream-metrics.json        # Spring Boot & JVM observability dashboard
│       └── bitestream-logs.json           # Live centralized application logs dashboard
├── start-logging.ps1                      # Windows PowerShell startup script
├── stop-logging.ps1                       # Windows PowerShell shutdown script
└── start-logging.sh                       # Unix/macOS shell startup script
```

---

## 🚀 Quick Start Guide

### Option 1: Start alongside BiteStream from Root

From the repository root:
```bash
# Start BiteStream infrastructure, backend, frontend, and the logging stack together
docker compose --profile app-container --profile logging up -d
```

### Option 2: Start Logging Sub-Project Independently

Ensure the BiteStream stack is running (`docker compose --profile app-container up -d`), then navigate to the `logging/` directory:

#### On Windows (PowerShell):
```powershell
.\start-logging.ps1
```

#### On Linux / macOS:
```bash
chmod +x start-logging.sh
./start-logging.sh
```

---

## 🌐 Accessing Services & Dashboards

| Service | Port | Default URL | Credentials | Notes |
|---|---|---|---|---|
| **Grafana** | `3000` | [http://localhost:3000](http://localhost:3000) | `admin` / `admin` | Dashboards pre-loaded under folder **BiteStream** |
| **Prometheus** | `9090` | [http://localhost:9090](http://localhost:9090) | None | Scrape target: `bitestream-app:8080/actuator/prometheus` |
| **Loki API** | `3100` | [http://localhost:3100/ready](http://localhost:3100/ready) | None | Log aggregation endpoint & LogQL processor |
| **Promtail** | `9080` | [http://localhost:9080](http://localhost:9080) | None | Log agent tailing app files & Docker containers |
| **BiteStream Actuator** | `8080` | [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus) | None | Raw Prometheus metrics endpoint |

---

## 📊 Pre-Built Grafana Dashboards

### 1. BiteStream - Spring Boot Metrics & Observability (`bitestream-metrics`)
- **Application Overview**: Uptime, CPU Usage %, JVM Heap Memory %, Active Threads.
- **HTTP Request Throughput**: Request rate (req/sec) broken down by endpoint (`/api/orders`, `/api/restaurants`, `/api/delivery-agents`, `/api/deliveries/*`) and HTTP status code (`200`, `201`, `400`, `500`).
- **Latency Distribution**: P95 and P99 latency percentiles via Micrometer timers.
- **Database Connection Pool (HikariCP)**: Active, idle, and pending connections.

### 2. BiteStream - Centralized Logs & Audit (`bitestream-logs`)
- **Backend Log Volume by Level**: Real-time rate of logs categorized by level (`INFO`, `WARN`, `ERROR`).
- **Total Error Counter**: Total errors logged in the last 1 hour.
- **Spring Boot Backend Log Stream**: Live stream from `/var/log/bitestream/bitestream.log` with level, logger, thread, and message.
- **Angular Frontend & Nginx Access Logs**: Live HTTP request stream from the frontend container (`bitestream-frontend`) showing client IP, route, status code, and latency.
- **All Containers Log Stream**: Full unified log console covering PostgreSQL, Redis, Kafka, Spring Boot, and Angular.

---

## 🔍 Useful LogQL & PromQL Queries

### Useful PromQL Queries (Metrics)

- **Total HTTP Request Rate by Endpoint**:
  ```promql
  sum by (uri, status) (rate(http_server_requests_seconds_count[1m]))
  ```
- **P95 Latency Percentile**:
  ```promql
  histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[1m])) by (le))
  ```
- **P99 Latency Percentile**:
  ```promql
  histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[1m])) by (le))
  ```
- **JVM Heap Memory Utilization %**:
  ```promql
  (jvm_memory_used_bytes{area="heap",job="bitestream-app"} / jvm_memory_max_bytes{area="heap",job="bitestream-app"}) * 100
  ```
- **HikariCP Active DB Connections**:
  ```promql
  hikaricp_connections_active
  ```

### Useful LogQL Queries (Loki Logs)

- **Live Backend Application Logs**:
  ```logql
  {job="bitestream-app"}
  ```
- **Live Angular Frontend / Nginx Web Traffic**:
  ```logql
  {container="bitestream-frontend"}
  ```
- **Filter Backend Errors**:
  ```logql
  {job="bitestream-app"} |= "ERROR"
  ```
- **Kafka Event Publishing Logs**:
  ```logql
  {job="bitestream-app"} |= "OrderPlacedEvent"
  ```
- **Two-Step OTP Lifecycle Logs**:
  ```logql
  {job="bitestream-app"} |= "OTP"
  ```
- **All BiteStream Containers Combined**:
  ```logql
  {container=~"bitestream-.*"}
  ```
