# 03 - Data Models & Entity-Relationship (ER) Diagram

## 1. Complete Entity-Relationship (ER) Diagram

```mermaid
erDiagram
    RESTAURANTS ||--o{ MENU_ITEMS : "offers (1-to-many)"
    RESTAURANTS ||--o{ ORDERS : "fulfills (1-to-many)"
    ORDERS ||--|{ ORDER_ITEMS : "contains (1-to-many)"
    MENU_ITEMS ||--o{ ORDER_ITEMS : "referenced_in (1-to-many)"
    ORDERS ||--o| DELIVERY_ASSIGNMENTS : "assigned_to (1-to-1)"
    DELIVERY_AGENTS ||--o{ DELIVERY_ASSIGNMENTS : "delivers (1-to-many)"

    RESTAURANTS {
        bigint id PK "Primary Key"
        varchar name "Store Brand Name"
        varchar cuisine_type "Cuisine Category"
        varchar address "Physical Address"
        varchar contact_number "Phone Number"
        double_precision latitude "GPS Latitude"
        double_precision longitude "GPS Longitude"
        jsonb operating_hours "Daily Schedule (JSONB)"
        boolean is_active "Active Status Flag"
        timestamptz created_at "Creation Timestamp"
    }

    MENU_ITEMS {
        bigint id PK "Primary Key"
        bigint restaurant_id FK "References RESTAURANTS(id)"
        varchar name "Dish Name"
        text description "Item Details"
        numeric price "Unit Price (10,2)"
        integer prep_time_minutes "Kitchen Preparation Minutes"
        boolean is_available "Stock Availability Flag"
        timestamptz created_at "Creation Timestamp"
    }

    DELIVERY_AGENTS {
        bigint id PK "Primary Key"
        varchar name "Rider Full Name"
        varchar phone UK "Unique Mobile Number"
        varchar vehicle_type "Vehicle Category (E-Bike, Scooter, Bike)"
        varchar status "AVAILABLE | ASSIGNED | BUSY | OFFLINE"
        double_precision current_latitude "Rider GPS Latitude"
        double_precision current_longitude "Rider GPS Longitude"
        numeric rating "Customer Rating (3,2)"
        timestamptz created_at "Creation Timestamp"
    }

    ORDERS {
        bigint id PK "Primary Key"
        varchar order_number UK "Unique Tracking Reference (ORD-...)"
        varchar customer_name "Customer Full Name"
        varchar customer_phone "Customer Contact Number"
        varchar customer_address "Delivery Street Address"
        double_precision customer_latitude "Delivery GPS Latitude"
        double_precision customer_longitude "Delivery GPS Longitude"
        bigint restaurant_id FK "References RESTAURANTS(id)"
        varchar status "Order Lifecycle Enum Status"
        double_precision distance_km "Distance (2.0, 3.0, 4.0, 5.0 km)"
        integer queue_position "Store Kitchen Queue Index"
        integer estimated_prep_time_minutes "Cooking Duration"
        integer estimated_travel_time_minutes "Transit Duration"
        integer estimated_total_time_minutes "Total ETA"
        numeric total_amount "Total Billed Amount (10,2)"
        jsonb order_metadata "Dynamic Attributes & Instructions (JSONB)"
        jsonb event_logs "Kafka Lifecycle Audit Log Stream (JSONB)"
        timestamptz created_at "Order Placement Timestamp"
        timestamptz updated_at "Last State Change Timestamp"
    }

    ORDER_ITEMS {
        bigint id PK "Primary Key"
        bigint order_id FK "References ORDERS(id) ON DELETE CASCADE"
        bigint menu_item_id FK "References MENU_ITEMS(id)"
        varchar item_name "Snapshotted Item Name"
        integer quantity "Item Quantity"
        numeric unit_price "Unit Price at Order Time (10,2)"
        numeric subtotal "Line Subtotal (10,2)"
    }

    DELIVERY_ASSIGNMENTS {
        bigint id PK "Primary Key"
        bigint order_id UK,FK "References ORDERS(id) ON DELETE CASCADE"
        bigint delivery_agent_id FK "References DELIVERY_AGENTS(id)"
        varchar status "ASSIGNED | PICKED_UP | DELIVERED"
        varchar pickup_otp "6-Digit Pickup OTP"
        varchar delivery_otp "6-Digit Delivery OTP"
        boolean pickup_otp_verified "Pickup Verification Status"
        boolean delivery_otp_verified "Delivery Verification Status"
        timestamptz pickup_time "Restaurant Food Handover Time"
        timestamptz delivered_time "Customer Doorstep Handover Time"
        double_precision distance_km "Delivery Trip Distance"
        timestamptz created_at "Assignment Timestamp"
    }
```

---

## 2. Table Specifications & Indexing Strategy

### 2.1 Indexing Matrix

| Table | Index Name | Type | Column(s) | Architectural Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `restaurants` | `idx_restaurants_active` | B-tree | `(is_active)` | Quick filtering of operational restaurants |
| `menu_items` | `idx_menu_items_restaurant` | B-tree | `(restaurant_id, is_available)` | Index-only scans when fetching active store menus |
| `delivery_agents`| `idx_agents_status` | B-tree | `(status)` | Fast lookup of `AVAILABLE` agents during dispatch |
| `orders` | `idx_orders_restaurant_status` | B-tree | `(restaurant_id, status)` | Fast calculation of store queue depth ahead |
| `orders` | `idx_orders_status` | B-tree | `(status)` | Administrative queries on active orders |
| `orders` | `idx_orders_metadata_gin` | **GIN** | `order_metadata` | Sub-millisecond JSONB search by instructions or tags |
| `orders` | `idx_orders_events_gin` | **GIN** | `event_logs` | Sub-millisecond JSONB query over event history |
| `order_items` | `idx_order_items_order` | B-tree | `(order_id)` | Fast joining of line items to order header |
| `delivery_assignments`| `idx_delivery_assignments_order` | B-tree | `(order_id)` | Instant lookup of order assignment record |
| `delivery_assignments`| `idx_delivery_assignments_agent` | B-tree | `(delivery_agent_id)` | Audit & tracking of agent assignments |

---

## 3. JSONB Schema Specifications

### 3.1 `orders.order_metadata` Schema
Stores non-relational, flexible metadata without requiring DDL alterations:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "OrderMetadata",
  "type": "object",
  "properties": {
    "specialInstructions": {
      "type": "string",
      "description": "Customer notes for chef/rider (e.g., 'Extra spicy, do not ring bell')"
    },
    "paymentMethod": {
      "type": "string",
      "enum": ["ONLINE_UPI", "CREDIT_CARD", "DEBIT_CARD", "NET_BANKING", "CASH_ON_DELIVERY"]
    },
    "tipAmount": {
      "type": "number",
      "description": "Optional rider tip"
    },
    "discountAmount": {
      "type": "number",
      "description": "Promotional discounts applied"
    },
    "priority": {
      "type": "string",
      "enum": ["STANDARD", "HIGH_DISTANCE", "VIP"]
    },
    "additionalAttributes": {
      "type": "object",
      "description": "Arbitrary key-value metadata (e.g. queue_depth, promo_code)"
    }
  }
}
```

### 3.2 `orders.event_logs` Schema
Stores an immutable audit trail of Kafka event lifecycle transitions:
```json
[
  {
    "eventType": "ORDER_PLACED",
    "topic": "food.orders.placed",
    "timestamp": "2026-11-14T08:15:00.120Z",
    "description": "Order created with Queue position #1, Distance 3.0 km",
    "triggeredBy": "API_GATEWAY"
  },
  {
    "eventType": "DELIVERY_ASSIGNED",
    "topic": "food.delivery.assigned",
    "timestamp": "2026-11-14T08:15:01.450Z",
    "description": "Assigned partner Aarav Sharma (Electric Scooter). Pickup OTP generated.",
    "triggeredBy": "DISPATCHER"
  },
  {
    "eventType": "KITCHEN_PREPARING",
    "topic": "food.restaurant.notifications",
    "timestamp": "2026-11-14T08:15:02.100Z",
    "description": "Store Spice Symphony Gourmet started cooking order. Position in queue: #1",
    "triggeredBy": "RESTAURANT_KOT"
  },
  {
    "eventType": "DELIVERY_PICKED_UP",
    "topic": "food.delivery.pickup",
    "timestamp": "2026-11-14T08:20:00.000Z",
    "description": "Agent Aarav Sharma picked up order from restaurant. Customer Delivery OTP generated.",
    "triggeredBy": "RESTAURANT_AGENT_APP"
  },
  {
    "eventType": "ORDER_DELIVERED",
    "topic": "food.delivery.completed",
    "timestamp": "2026-11-14T08:33:00.000Z",
    "description": "Order delivered successfully to customer by Aarav Sharma",
    "triggeredBy": "CUSTOMER_AGENT_APP"
  }
]
```

### 3.3 `restaurants.operating_hours` Schema
```json
{
  "monday": { "open": "11:00", "close": "23:00" },
  "tuesday": { "open": "11:00", "close": "23:00" },
  "wednesday": { "open": "11:00", "close": "23:00" },
  "thursday": { "open": "11:00", "close": "23:00" },
  "friday": { "open": "11:00", "close": "23:59" },
  "saturday": { "open": "11:00", "close": "23:59" },
  "sunday": { "open": "11:00", "close": "23:00" }
}
```

---

## 4. Pre-Seeded Catalog & Master Fleet

| Entity | Quantity | Key Details |
| :--- | :---: | :--- |
| **Restaurants (Stores)** | **5** | 1. *Spice Symphony Gourmet* (North Indian)<br>2. *Bella Napoli Woodfire Pizzeria* (Italian)<br>3. *Dragon Wok Pan-Asian* (Pan-Asian)<br>4. *Green Delight Organic & Salads* (Healthy/Vegan)<br>5. *Burger Crave Smokehouse* (Burgers/BBQ) |
| **Menu Items** | **50** | **10 items per store**, pricing ranging from \$3.50 (Garlic Naan) to \$18.00 (Tandoori Platter), preparation durations between 4 and 22 minutes |
| **Delivery Agents** | **20** | 20 dedicated riders with verified vehicles (*Electric Scooters, Ather 450X, Ola S1, Honda Activa, Yamaha FZ, TVS Apache*), active ratings 4.75 - 4.95, and distributed coordinates |
