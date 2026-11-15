-- ============================================================================
-- V1__init_schema.sql
-- Database Schema for Event-driven Food Delivery Platform
-- Hybrid Relational POJO + JSONB optimized for high throughput and flexible events
-- ============================================================================

-- 1. Restaurants (Stores)
CREATE TABLE IF NOT EXISTS restaurants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    cuisine_type VARCHAR(50) NOT NULL,
    address VARCHAR(255) NOT NULL,
    contact_number VARCHAR(20) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    operating_hours JSONB DEFAULT '{}'::jsonb,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_restaurants_active ON restaurants(is_active);

-- 2. Menu Items (10 items per store)
CREATE TABLE IF NOT EXISTS menu_items (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL,
    prep_time_minutes INT NOT NULL DEFAULT 15,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_menu_items_restaurant ON menu_items(restaurant_id, is_available);

-- 3. Delivery Agents (20 Pickup Delivery Partners)
CREATE TABLE IF NOT EXISTS delivery_agents (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    current_latitude DOUBLE PRECISION NOT NULL,
    current_longitude DOUBLE PRECISION NOT NULL,
    rating NUMERIC(3,2) DEFAULT 4.80,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agents_status ON delivery_agents(status);

-- 4. Orders
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(60) NOT NULL UNIQUE,
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_address VARCHAR(255) NOT NULL,
    customer_latitude DOUBLE PRECISION NOT NULL,
    customer_longitude DOUBLE PRECISION NOT NULL,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id),
    status VARCHAR(30) NOT NULL,
    distance_km DOUBLE PRECISION NOT NULL,
    queue_position INT DEFAULT 1,
    estimated_prep_time_minutes INT NOT NULL,
    estimated_travel_time_minutes INT NOT NULL,
    estimated_total_time_minutes INT NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    order_metadata JSONB DEFAULT '{}'::jsonb,
    event_logs JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_restaurant_status ON orders(restaurant_id, status);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_metadata_gin ON orders USING gin (order_metadata);
CREATE INDEX idx_orders_events_gin ON orders USING gin (event_logs);

-- 5. Order Items
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id BIGINT NOT NULL REFERENCES menu_items(id),
    item_name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(10,2) NOT NULL,
    subtotal NUMERIC(10,2) NOT NULL
);

CREATE INDEX idx_order_items_order ON order_items(order_id);

-- 6. Delivery Assignments
CREATE TABLE IF NOT EXISTS delivery_assignments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    delivery_agent_id BIGINT NOT NULL REFERENCES delivery_agents(id),
    status VARCHAR(30) NOT NULL,
    pickup_otp VARCHAR(6),
    delivery_otp VARCHAR(6),
    pickup_otp_verified BOOLEAN DEFAULT FALSE,
    delivery_otp_verified BOOLEAN DEFAULT FALSE,
    pickup_time TIMESTAMP WITH TIME ZONE,
    delivered_time TIMESTAMP WITH TIME ZONE,
    distance_km DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_delivery_assignments_order ON delivery_assignments(order_id);
CREATE INDEX idx_delivery_assignments_agent ON delivery_assignments(delivery_agent_id);
