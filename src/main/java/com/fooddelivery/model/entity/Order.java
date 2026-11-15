package com.fooddelivery.model.entity;

import com.fooddelivery.model.enums.OrderStatus;
import com.fooddelivery.model.json.EventLogEntry;
import com.fooddelivery.model.json.OrderMetadata;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 60)
    private String orderNumber;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "customer_address", nullable = false)
    private String customerAddress;

    @Column(name = "customer_latitude", nullable = false)
    private Double customerLatitude;

    @Column(name = "customer_longitude", nullable = false)
    private Double customerLongitude;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PLACED;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "queue_position")
    @Builder.Default
    private Integer queuePosition = 1;

    @Column(name = "estimated_prep_time_minutes", nullable = false)
    private Integer estimatedPrepTimeMinutes;

    @Column(name = "estimated_travel_time_minutes", nullable = false)
    private Integer estimatedTravelTimeMinutes;

    @Column(name = "estimated_total_time_minutes", nullable = false)
    private Integer estimatedTotalTimeMinutes;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Optimized JSONB: Dynamic metadata (instructions, payment, discounts, extras)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "order_metadata", columnDefinition = "jsonb")
    @Builder.Default
    private OrderMetadata orderMetadata = new OrderMetadata();

    // Optimized JSONB: Event stream audit trail of Kafka lifecycle
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_logs", columnDefinition = "jsonb")
    @Builder.Default
    private List<EventLogEntry> eventLogs = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    public void addEventLog(String eventType, String topic, String description, String triggeredBy) {
        if (this.eventLogs == null) {
            this.eventLogs = new ArrayList<>();
        }
        this.eventLogs.add(EventLogEntry.builder()
                .eventType(eventType)
                .topic(topic)
                .timestamp(Instant.now())
                .description(description)
                .triggeredBy(triggeredBy)
                .build());
    }

    public void addItem(OrderItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
        item.setOrder(this);
    }
}
