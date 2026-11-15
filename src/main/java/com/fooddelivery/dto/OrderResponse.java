package com.fooddelivery.dto;

import com.fooddelivery.model.enums.OrderStatus;
import com.fooddelivery.model.json.EventLogEntry;
import com.fooddelivery.model.json.OrderMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    private Long restaurantId;
    private String restaurantName;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private Double distanceKm;
    private Integer queuePosition;
    private Integer estimatedPrepTimeMinutes;
    private Integer estimatedTravelTimeMinutes;
    private Integer estimatedTotalDeliveryTimeMinutes;
    private BigDecimal totalAmount;
    private List<OrderItemDetailDto> items;
    private OrderMetadata metadata;
    private List<EventLogEntry> lifecycleEvents;
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDetailDto {
        private Long id;
        private Long menuItemId;
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
