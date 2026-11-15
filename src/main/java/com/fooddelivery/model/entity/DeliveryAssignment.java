package com.fooddelivery.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "delivery_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAssignment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "delivery_agent_id", nullable = false)
    private DeliveryAgent deliveryAgent;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "pickup_otp", length = 6)
    private String pickupOtp;

    @Column(name = "delivery_otp", length = 6)
    private String deliveryOtp;

    @Column(name = "pickup_otp_verified")
    @Builder.Default
    private Boolean pickupOtpVerified = false;

    @Column(name = "delivery_otp_verified")
    @Builder.Default
    private Boolean deliveryOtpVerified = false;

    @Column(name = "pickup_time")
    private Instant pickupTime;

    @Column(name = "delivered_time")
    private Instant deliveredTime;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
