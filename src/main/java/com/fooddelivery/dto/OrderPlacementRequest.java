package com.fooddelivery.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacementRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer phone is required")
    private String customerPhone;

    @NotBlank(message = "Customer address is required")
    private String customerAddress;

    private Double customerLatitude;
    private Double customerLongitude;

    @NotNull(message = "Restaurant ID is required")
    private Long restaurantId;

    /**
     * Delivery distance in kilometers (e.g., 2.0, 3.0, 4.0, 5.0 km)
     */
    @NotNull(message = "Distance in km is required")
    @DecimalMin(value = "0.5", message = "Minimum distance is 0.5 km")
    @DecimalMax(value = "25.0", message = "Maximum delivery radius is 25 km")
    private Double distanceKm;

    @NotEmpty(message = "At least one menu item must be selected")
    private List<ItemOrderDto> items;

    private String specialInstructions;
    private String paymentMethod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemOrderDto {
        @NotNull(message = "Menu item ID is required")
        private Long menuItemId;

        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
