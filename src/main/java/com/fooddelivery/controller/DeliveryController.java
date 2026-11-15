package com.fooddelivery.controller;

import com.fooddelivery.dto.DeliveryConfirmationResponse;
import com.fooddelivery.dto.OtpVerificationRequest;
import com.fooddelivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@Tag(name = "Deliveries & OTP", description = "Two-Step OTP Security Protocol for Restaurant Pickup and Doorstep Customer Handover")
public class DeliveryController {

    private final DeliveryService deliveryService;

    /**
     * Agent confirms pickup at restaurant by supplying the 6-digit Pickup OTP
     */
    @PostMapping("/{orderId}/pickup")
    @Operation(summary = "Confirm Restaurant Pickup (Step 1 OTP Handover)",
            description = "Validates the 6-digit numeric Pickup OTP in Redis (15-min TTL). Upon verification, status transitions to OUT_FOR_DELIVERY, generates customer Delivery OTP, and streams 'food.delivery.pickup'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pickup confirmed successfully; Delivery OTP generated for customer"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired Pickup OTP"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Order not in valid state for pickup")
    })
    public ResponseEntity<DeliveryConfirmationResponse> confirmPickup(
            @Parameter(description = "Internal Order ID", example = "1")
            @PathVariable Long orderId,
            @Valid @RequestBody OtpVerificationRequest request) {
        DeliveryConfirmationResponse response = deliveryService.confirmPickup(orderId, request.getOtp());
        return ResponseEntity.ok(response);
    }

    /**
     * Agent completes delivery at customer location by verifying the 6-digit Delivery OTP
     */
    @PostMapping("/{orderId}/complete")
    @Operation(summary = "Complete Doorstep Delivery (Step 2 OTP Handover)",
            description = "Validates the 6-digit numeric Delivery OTP in Redis (30-min TTL). Upon verification, marks order DELIVERED, frees delivery agent back to AVAILABLE, and streams 'food.delivery.completed'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Delivery successfully completed and verified"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired Delivery OTP"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Order not in valid state for delivery completion")
    })
    public ResponseEntity<DeliveryConfirmationResponse> confirmDelivery(
            @Parameter(description = "Internal Order ID", example = "1")
            @PathVariable Long orderId,
            @Valid @RequestBody OtpVerificationRequest request) {
        DeliveryConfirmationResponse response = deliveryService.confirmDelivery(orderId, request.getOtp());
        return ResponseEntity.ok(response);
    }

    /**
     * Utility endpoint for integration testing & inspection of Redis active OTPs
     */
    @GetMapping("/{orderId}/otp")
    @Operation(summary = "Inspect active OTPs (Dev/Testing Utility)",
            description = "Inspects active OTP states and TTLs in Redis for testing and simulation verification.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active OTPs retrieved"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<Map<String, Object>> getActiveOtps(
            @Parameter(description = "Internal Order ID", example = "1")
            @PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryService.getActiveOtps(orderId));
    }
}
