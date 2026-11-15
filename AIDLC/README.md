# BiteStream — Architecture & Design Lifecycle Documentation (AIDLC)
## Real-Time Event-Driven Food Delivery & Dispatch Platform (Kafka + Spring Boot + Postgres + Redis)

Welcome to the **BiteStream AIDLC (Architecture & Design Lifecycle Documentation)**. This comprehensive suite of design documents, architecture diagrams, sequence flows, ER schemas, and operational specifications serves as the single source of truth for engineering, architecture, and DevOps teams.

---

## 📚 Document Index

| Document | Description | Key Diagrams & Highlights |
| :--- | :--- | :--- |
| **[01-ARCHITECTURE-DESIGN.md](01-ARCHITECTURE-DESIGN.md)** | System High-Level Architecture & Component Blueprint | High-Level Event Architecture, C4 Container Diagram, Redis vs. EHCache Benchmark, Hybrid Data Model Topology |
| **[02-SEQUENCE-DIAGRAMS.md](02-SEQUENCE-DIAGRAMS.md)** | End-to-End Dynamic Interaction Flows | Full Lifecycle Sequence Diagram, Kafka Async Dispatch Flow, Two-Step OTP Verification Flow, Failure/Retry Flows |
| **[03-DATA-MODELS-ER-DIAGRAM.md](03-DATA-MODELS-ER-DIAGRAM.md)** | Database Schema & Entity-Relationship Specifications | Mermaid `erDiagram`, Relational Tables, PostgreSQL JSONB Schemas, GIN Indexes, Flyway Versioning |
| **[04-STATE-MACHINE-SPEC.md](04-STATE-MACHINE-SPEC.md)** | Order & Delivery Agent State Machine Specifications | Mermaid `stateDiagram-v2`, Allowed State Transitions Matrix, Terminal States, Concurrency Guards |
| **[05-API-SPECIFICATION.md](05-API-SPECIFICATION.md)** | Complete RESTful Interface Specification | OpenAPI/Swagger Contracts, cURL commands, Request/Response Payloads, HTTP Status Codes |
| **[06-EVENT-DRIVEN-KAFKA-SPEC.md](06-EVENT-DRIVEN-KAFKA-SPEC.md)** | Event Backbone, Topic Schemas & Consumer Topology | 5 Kafka Topics, Event Payloads, Partitioning Strategy (`orderNumber` key), Consumer Groups, Retry & DLT |
| **[07-SECURITY-DEPLOYMENT-GUIDE.md](07-SECURITY-DEPLOYMENT-GUIDE.md)** | Security Architecture & Production Deployment Blueprint | OTP Defense In-Depth, Secret Management, Docker Compose / Kubernetes Deployment, Observability Setup |

---

## 🎯 Executive Summary & Platform Core Capabilities

1. **Event-Driven Asynchronous Backbone**: Built on **Apache Kafka (KRaft mode)** for high-throughput, low-latency messaging decoupling Order Placement, Restaurant Kitchen Notifications, Agent Dispatch, and Customer Lifecycle alerts.
2. **PostgreSQL Relational Core + JSONB Extensibility**: Relational POJO modeling for referential integrity (Foreign Keys, cascading constraints, financial amounts) combined with `JSONB` columns (`order_metadata`, `event_logs`, `operating_hours`) indexed with PostgreSQL GIN.
3. **Optimized Distributed Redis Cache**:
   - Sub-millisecond TTL-based storage for **Two-Step OTP Security** (Pickup OTP: 15-min TTL, Delivery OTP: 30-min TTL).
   - High-throughput caching for restaurant profiles and menus (`@Cacheable(value = "restaurant_menus")`).
4. **Logistics & Queue Distance ETA Engine**:
   - Calculates delivery transit based on distance bands (**2.0 km, 3.0 km, 4.0 km, 5.0 km**) at average urban speed (20 km/h) + pickup buffer.
   - Adjusts preparation time dynamically based on the restaurant's active kitchen queue depth (+3 min per order ahead).
5. **Pre-Seeded Master Fleet & Catalog**:
   - **5 Stores / Restaurants** across distinct cuisines.
   - **10 Menu Items per store** (50 items total) with prep times and pricing.
   - **20 Delivery Pickup Partners** with vehicle types and geographical coordinates.
