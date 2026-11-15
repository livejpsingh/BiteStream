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
public class DeliveryCompletedEvent implements Serializable {
    private Long orderId;
    private String orderNumber;
    private Long deliveryAgentId;
    private String deliveryAgentName;
    private Double distanceKm;
    private Instant deliveredAt;
}
