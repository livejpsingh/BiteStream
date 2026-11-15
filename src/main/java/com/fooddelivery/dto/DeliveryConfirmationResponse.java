package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryConfirmationResponse {
    private boolean success;
    private String message;
    private Long orderId;
    private String orderNumber;
    private String status;
    private String deliveryAgentName;
    private String generatedDeliveryOtp; // Non-null when pickup is confirmed, for demonstration / dispatch
    private Instant actionTimestamp;
}
