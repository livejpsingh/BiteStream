package com.fooddelivery.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent implements Serializable {
    private Long orderId;
    private String orderNumber;
    private Long restaurantId;
    private String restaurantName;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private Double distanceKm;
    private Integer queuePosition;
    private Integer estimatedPrepTimeMinutes;
    private Integer estimatedTravelTimeMinutes;
    private Integer estimatedTotalTimeMinutes;
    private BigDecimal totalAmount;
    private List<OrderItemDetail> items;
    private Instant placedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDetail implements Serializable {
        private Long menuItemId;
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
