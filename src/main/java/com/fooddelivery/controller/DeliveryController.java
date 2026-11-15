package com.fooddelivery.controller;

import com.fooddelivery.dto.DeliveryConfirmationResponse;
import com.fooddelivery.dto.OtpVerificationRequest;
import com.fooddelivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    /**
     * Agent confirms pickup at restaurant by supplying the 6-digit Pickup OTP
     */
    @PostMapping("/{orderId}/pickup")
    public ResponseEntity<DeliveryConfirmationResponse> confirmPickup(
            @PathVariable Long orderId,
            @Valid @RequestBody OtpVerificationRequest request) {
        DeliveryConfirmationResponse response = deliveryService.confirmPickup(orderId, request.getOtp());
        return ResponseEntity.ok(response);
    }

    /**
     * Agent completes delivery at customer location by verifying the 6-digit Delivery OTP
     */
    @PostMapping("/{orderId}/complete")
    public ResponseEntity<DeliveryConfirmationResponse> confirmDelivery(
            @PathVariable Long orderId,
            @Valid @RequestBody OtpVerificationRequest request) {
        DeliveryConfirmationResponse response = deliveryService.confirmDelivery(orderId, request.getOtp());
        return ResponseEntity.ok(response);
    }

    /**
     * Utility endpoint for integration testing & inspection of Redis active OTPs
     */
    @GetMapping("/{orderId}/otp")
    public ResponseEntity<Map<String, Object>> getActiveOtps(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryService.getActiveOtps(orderId));
    }
}
