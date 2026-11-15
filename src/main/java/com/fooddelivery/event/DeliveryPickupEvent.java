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
public class DeliveryPickupEvent implements Serializable {
    private Long orderId;
    private String orderNumber;
    private Long deliveryAgentId;
    private String deliveryAgentName;
    private String customerPhone;
    private String customerAddress;
    private Double distanceKm;
    private String deliveryOtp;
    private Instant pickedUpAt;
}
