# 02 - Dynamic Sequence Diagrams

## 1. End-to-End Order & Delivery Lifecycle Flow

This sequence diagram depicts the complete journey of a customer order from placement through distance calculation, Kafka event emission, restaurant notification, agent dispatch, and two-step OTP verification.

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Customer (App)
    participant API as Spring Boot API Gateway
    participant OrderSvc as Order Service
    participant EtaSvc as ETA & Logistics Engine
    participant DB as PostgreSQL 16
    participant Kafka as Apache Kafka Broker
    participant KitchenConsumer as Restaurant Kitchen Consumer
    actor Store as Restaurant Kitchen (Tablet)
    participant DispatchConsumer as Delivery Dispatch Consumer
    participant OtpSvc as OTP Engine (Redis)
    actor Rider as Delivery Partner (Rider App)

    %% Step 1: Order Placement
    Customer->>API: POST /api/orders (Store ID, Items, Distance: 3.0 km)
    API->>OrderSvc: placeOrder(request)
    OrderSvc->>DB: Query store & active queue count ahead
    DB-->>OrderSvc: Return queue count = 1
    OrderSvc->>EtaSvc: calculatePreparationTimeMinutes(items, queue=1)
    EtaSvc-->>OrderSvc: Prep ETA = 18 mins
    OrderSvc->>EtaSvc: calculateTravelTimeMinutes(distance=3.0 km)
    EtaSvc-->>OrderSvc: Travel ETA = 13 mins (Total ETA = 31 mins)
    OrderSvc->>DB: INSERT INTO orders & order_items (status=PLACED)
    DB-->>OrderSvc: Order saved (ID: 101, ORD-1763)
    OrderSvc->>Kafka: Publish OrderPlacedEvent to 'food.orders.placed'
    OrderSvc-->>Customer: HTTP 201 Created (Order Placed, ETA: 31 mins)

    %% Step 2: Asynchronous Event Processing
    par Kitchen Notification
        Kafka->>KitchenConsumer: Consume 'food.orders.placed'
        KitchenConsumer->>DB: UPDATE orders SET status='PREPARING'
        KitchenConsumer->>Kafka: Publish to 'food.restaurant.notifications'
        Kafka->>Store: Display KOT on Kitchen Tablet (Queue #1, Prep: 18m)
    and Delivery Partner Dispatch
        Kafka->>DispatchConsumer: Consume 'food.orders.placed'
        DispatchConsumer->>DB: Query closest AVAILABLE agent (from 20 agents)
        DB-->>DispatchConsumer: Assigned Agent: Aarav Sharma (ID: 1)
        DispatchConsumer->>OtpSvc: createAndStoreOtp(orderId=101, type=PICKUP)
        OtpSvc->>OtpSvc: Generate 6-digit OTP (e.g. 684920), SET in Redis with 15-min TTL
        DispatchConsumer->>DB: INSERT delivery_assignments (status=ASSIGNED)
        DispatchConsumer->>DB: UPDATE orders SET status='RESTAURANT_ACCEPTED'
        DispatchConsumer->>Kafka: Publish to 'food.delivery.assigned'
        Kafka->>Rider: Push Dispatch Alert (Store address, Distance, Pickup OTP: 684920)
    end

    %% Step 3: Food Handover & Pickup OTP
    note over Rider, Store: Rider arrives at Store when food is ready
    Rider->>Store: Shares Pickup OTP: 684920
    Store->>API: POST /api/deliveries/101/pickup { "otp": "684920" }
    API->>OtpSvc: verifyOtp(orderId=101, type=PICKUP, "684920")
    OtpSvc->>OtpSvc: Match in Redis & Delete Key (One-time use)
    OtpSvc-->>API: OTP Valid (True)
    API->>DB: UPDATE delivery_assignments SET pickup_otp_verified=true, status='PICKED_UP'
    API->>DB: UPDATE orders SET status='PICKED_UP'
    API->>OtpSvc: createAndStoreOtp(orderId=101, type=DELIVERY)
    OtpSvc->>OtpSvc: Generate Delivery OTP (e.g. 512394), SET in Redis with 30-min TTL
    API->>Kafka: Publish DeliveryPickupEvent to 'food.delivery.pickup'
    Kafka->>Customer: Push Notification ("Order picked up! Your Delivery OTP: 512394")
    API-->>Store: HTTP 200 OK (Pickup Confirmed)

    %% Step 4: Doorstep Delivery & Customer OTP
    note over Rider, Customer: Rider reaches Customer doorstep (3.0 km away)
    Customer->>Rider: Shares Delivery OTP: 512394
    Rider->>API: POST /api/deliveries/101/complete { "otp": "512394" }
    API->>OtpSvc: verifyOtp(orderId=101, type=DELIVERY, "512394")
    OtpSvc->>OtpSvc: Match in Redis & Delete Key
    OtpSvc-->>API: OTP Valid (True)
    API->>DB: UPDATE delivery_assignments SET delivery_otp_verified=true, status='DELIVERED'
    API->>DB: UPDATE orders SET status='DELIVERED'
    API->>DB: UPDATE delivery_agents SET status='AVAILABLE' (Release Rider 1)
    API->>Kafka: Publish DeliveryCompletedEvent to 'food.delivery.completed'
    Kafka->>Customer: Push Notification ("Order Delivered! Bon Appétit")
    API-->>Rider: HTTP 200 OK (Delivery Completed Successfully)
```

---

## 2. Two-Step OTP Verification Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Client as Caller (Agent / Store)
    participant DelivSvc as DeliveryService
    participant OtpSvc as OtpService
    participant Redis as Redis 7 (In-Memory)
    participant DB as PostgreSQL 16
    participant Kafka as Kafka Event Bus

    rect rgb(240, 248, 255)
    note right of Client: Phase 1: Restaurant Handover (Pickup OTP)
    Client->>DelivSvc: confirmPickup(orderId, providedOtp)
    DelivSvc->>OtpSvc: verifyOtp(orderId, PICKUP, providedOtp)
    OtpSvc->>Redis: GET food_delivery:otp:pickup:{orderId}
    alt OTP Not Found or Expired in Redis
        Redis-->>OtpSvc: null
        OtpSvc-->>DelivSvc: false
        DelivSvc-->>Client: 400 Bad Request ("Invalid or expired Pickup OTP")
    else OTP Matches
        Redis-->>OtpSvc: "684920"
        OtpSvc->>Redis: DEL food_delivery:otp:pickup:{orderId}
        OtpSvc-->>DelivSvc: true
        DelivSvc->>DB: Set pickup_otp_verified=true, status=PICKED_UP
        DelivSvc->>OtpSvc: createAndStoreOtp(orderId, DELIVERY)
        OtpSvc->>Redis: SETEX food_delivery:otp:delivery:{orderId} 1800 "512394"
        DelivSvc->>Kafka: Publish DeliveryPickupEvent
        DelivSvc-->>Client: 200 OK (Pickup Confirmed, Delivery OTP Generated)
    end
    end

    rect rgb(245, 255, 245)
    note right of Client: Phase 2: Customer Handover (Delivery OTP)
    Client->>DelivSvc: confirmDelivery(orderId, providedOtp)
    DelivSvc->>OtpSvc: verifyOtp(orderId, DELIVERY, providedOtp)
    OtpSvc->>Redis: GET food_delivery:otp:delivery:{orderId}
    alt OTP Mismatch
        Redis-->>OtpSvc: "512394"
        OtpSvc-->>DelivSvc: false (Mismatch on "999999")
        DelivSvc-->>Client: 400 Bad Request ("Invalid or expired Delivery OTP")
    else OTP Matches
        Redis-->>OtpSvc: "512394"
        OtpSvc->>Redis: DEL food_delivery:otp:delivery:{orderId}
        OtpSvc-->>DelivSvc: true
        DelivSvc->>DB: Set delivery_otp_verified=true, status=DELIVERED
        DelivSvc->>DB: Set delivery_agents.status='AVAILABLE'
        DelivSvc->>Kafka: Publish DeliveryCompletedEvent
        DelivSvc-->>Client: 200 OK (Delivery Complete, Agent Freed)
    end
    end
```

---

## 3. Kafka Consumer Error & Retry Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Broker as Kafka Broker ('food.orders.placed')
    participant Consumer as OrderPlacedConsumer
    participant ErrorHandler as DefaultErrorHandler (FixedBackOff / Exponential)
    participant DLT as Dead Letter Topic ('food.orders.placed.DLT')
    participant Alert as Ops Monitoring / PagerDuty

    Broker->>Consumer: Deliver ConsumerRecord(offset=402, OrderId=999)
    Consumer->>Consumer: Execute business processing...
    alt Transient DB Lock / Network Blip
        Consumer-->>ErrorHandler: Throws DataAccessException
        loop Retry 1 to 3 with Backoff
            ErrorHandler->>Consumer: Retry invoke handleOrderPlaced()
        end
        Consumer-->>ErrorHandler: Success on Retry 2
        Consumer->>Broker: Commit Consumer Offset (402)
    else Poison Pill Payload / Permanent Exception
        Consumer-->>ErrorHandler: Throws Deserialization / Malformed Exception
        loop Exhaust 3 Retries
            ErrorHandler->>Consumer: Retry invocation
        end
        ErrorHandler->>DLT: Publish poisoned record to 'food.orders.placed.DLT'
        ErrorHandler->>Alert: Emit PagerDuty alert (Poison record isolated)
        ErrorHandler->>Broker: Commit offset to avoid partition stalling
    end
```
