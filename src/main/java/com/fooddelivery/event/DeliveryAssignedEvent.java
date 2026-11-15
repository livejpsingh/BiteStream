package com.fooddelivery.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignedEvent implements Serializable {
    private Long orderId;
    private String orderNumber;
    private Long deliveryAgentId;
    private String deliveryAgentName;
    private String deliveryAgentPhone;
    private String vehicleType;
    private Long restaurantId;
    private String restaurantName;
    private String restaurantAddress;
    private String customerName;
    private String customerAddress;
    private Double distanceKm;
    private Integer estimatedTravelTimeMinutes;
    private Integer estimatedPickupMinutes;
    private String pickupOtp;
    private Instant assignedAt;
}
