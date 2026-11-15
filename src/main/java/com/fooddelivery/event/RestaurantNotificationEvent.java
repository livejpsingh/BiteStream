package com.fooddelivery.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantNotificationEvent implements Serializable {
    private Long restaurantId;
    private String restaurantName;
    private Long orderId;
    private String orderNumber;
    private Integer queuePosition;
    private Integer estimatedPrepTimeMinutes;
    private List<String> itemSummary;
    private String specialInstructions;
    private Instant notifiedAt;
}
