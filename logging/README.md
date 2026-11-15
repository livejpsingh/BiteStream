# BiteStream Observability & Centralized Logging Sub-Project

This sub-project provides enterprise-grade observability and centralized logging for the **BiteStream** real-time food delivery and event-driven dispatch platform.

---

## Architecture Overview

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

### Components

1. **Prometheus (`:9090`)**:
   - Collects time-series metrics from BiteStream Spring Boot Actuator (`/actuator/prometheus`) every 5 seconds.
   - Monitors JVM memory, garbage collection, CPU, active threads, HikariCP database pool, and HTTP request throughput/latencies.

2. **Grafana Loki (`:3100`)**:
   - High-performance, horizontally-scalable log aggregation engine optimized for Grafana.
   - Indexes metadata (labels such as `job`, `level`, `thread`, `app`) rather than full text for ultra-fast queries and minimal storage footprint.

3. **Promtail (`:9080`)**:
   - Lightweight log shipping agent that tails `/var/log/bitestream/*.log` and Docker container stdout/stderr.
   - Parses timestamps, log levels (`INFO`, `WARN`, `ERROR`), logger names, and threads via regex pipeline before forwarding to Loki.

4. **Grafana (`:3000`)**:
   - Web visualization portal with pre-provisioned datasources (Prometheus & Loki) and dashboards.
   - Default credentials: `admin` / `admin`.

---

## Directory Structure

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
│   │   │   └── datasources.yml            # Auto-provisions Prometheus & Loki
│   │   └── dashboards/
│   │       └── dashboards.yml             # Auto-loads pre-built dashboards
│   └── dashboards/
│       ├── bitestream-metrics.json        # Spring Boot & JVM observability dashboard
│       └── bitestream-logs.json           # Live centralized application logs dashboard
├── start-logging.ps1                      # Windows PowerShell startup script
└── start-logging.sh                       # Unix/macOS shell startup script
```

---

## Quick Start Guide

### Option 1: Start alongside BiteStream from Root

From the repository root:
```bash
# Start BiteStream infrastructure, application, and the logging stack together
docker compose --profile app-container --profile logging up -d
```

### Option 2: Start Logging Sub-Project Independently

Ensure the BiteStream stack is running (`docker compose --profile app-container up -d`), then navigate to the `logging/` directory:

#### On Windows (PowerShell):
```powershell
.\start-logging.ps1
```
*Or directly via docker compose:*
```powershell
docker compose up -d
```

#### On Linux / macOS:
```bash
chmod +x start-logging.sh
./start-logging.sh
```

---

## Accessing Services & Dashboards

| Service | URL | Credentials | Notes |
|---|---|---|---|
| **Grafana** | [http://localhost:3000](http://localhost:3000) | `admin` / `admin` | Dashboards pre-loaded under folder **BiteStream** |
| **Prometheus** | [http://localhost:9090](http://localhost:9090) | None | Target: `bitestream-app:8080/actuator/prometheus` |
| **Loki API** | [http://localhost:3100](http://localhost:3100) | None | Query via Grafana Explore or LogQL |
| **BiteStream Actuator** | [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus) | None | Raw Prometheus metrics endpoint |

---

## Pre-Built Grafana Dashboards

### 1. BiteStream - Spring Boot Metrics & Observability (`bitestream-metrics`)
- **Application Overview**: Uptime, CPU Usage %, JVM Heap Memory %, Active Threads.
- **HTTP Traffic**: Request Rate (req/sec) grouped by URI and HTTP Status Code.
- **Latency Distribution**: P95 and P99 latency percentiles via Micrometer histograms.
- **Database Pool (HikariCP)**: Active connections, idle connections, pending threads.

### 2. BiteStream - Centralized Logs & Audit (`bitestream-logs`)
- **Log Volume Rate**: Live time-series stream of logs categorized by level (`INFO`, `WARN`, `ERROR`).
- **Error Count**: Total errors recorded in the last 1 hour.
- **Live Log Stream**: Color-coded, auto-scrolling log console with search and filter capabilities.

---

## Useful LogQL & PromQL Queries

### Useful PromQL Queries
- **Request Rate**:
  ```promql
  sum by (uri, status) (rate(http_server_requests_seconds_count{application="bitestream"}[1m]))
  ```
- **P95 Latency**:
  ```promql
  histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application="bitestream"}[1m])) by (le))
  ```
- **JVM Heap Memory**:
  ```promql
  (jvm_memory_used_bytes{application="bitestream",area="heap"} / jvm_memory_max_bytes{application="bitestream",area="heap"}) * 100
  ```

### Useful LogQL Queries (Loki)
- **All Application Logs**:
  ```logql
  {job=~"bitestream-app.*"}
  ```
- **Filter for Errors**:
  ```logql
  {job=~"bitestream-app.*"} |= "ERROR"
  ```
- **Kafka Consumer Logs**:
  ```logql
  {job=~"bitestream-app.*"} |= "ConsumerCoordinator"
  ```
