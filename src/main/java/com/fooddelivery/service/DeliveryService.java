package com.fooddelivery.service;

import com.fooddelivery.dto.DeliveryConfirmationResponse;
import com.fooddelivery.event.DeliveryCompletedEvent;
import com.fooddelivery.event.DeliveryPickupEvent;
import com.fooddelivery.kafka.producer.DeliveryEventProducer;
import com.fooddelivery.model.entity.DeliveryAssignment;
import com.fooddelivery.model.entity.Order;
import com.fooddelivery.model.enums.AgentStatus;
import com.fooddelivery.model.enums.OrderStatus;
import com.fooddelivery.model.enums.OtpType;
import com.fooddelivery.repository.DeliveryAssignmentRepository;
import com.fooddelivery.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final OrderRepository orderRepository;
    private final OtpService otpService;
    private final DeliveryAgentService deliveryAgentService;
    private final DeliveryEventProducer deliveryEventProducer;

    /**
     * Verifies Pickup OTP when delivery agent reaches the restaurant.
     * Transitions order to PICKED_UP, issues Delivery OTP for customer.
     */
    @Transactional
    public DeliveryConfirmationResponse confirmPickup(Long orderId, String providedOtp) {
        log.info("Attempting pickup confirmation for order ID: {}", orderId);

        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No delivery assignment found for order ID: " + orderId));

        if (Boolean.TRUE.equals(assignment.getPickupOtpVerified())) {
            throw new IllegalStateException("Food has already been picked up for order ID: " + orderId);
        }

        // 1. Verify OTP with Redis
        boolean isValid = otpService.verifyOtp(orderId, OtpType.PICKUP, providedOtp);
        if (!isValid) {
            log.warn("Invalid or expired Pickup OTP provided for order ID: {}", orderId);
            throw new IllegalArgumentException("Invalid or expired Pickup OTP!");
        }

        // 2. Update Assignment & Order Status
        assignment.setPickupOtpVerified(true);
        assignment.setPickupTime(Instant.now());
        assignment.setStatus("PICKED_UP");

        Order order = assignment.getOrder();
        order.setStatus(OrderStatus.PICKED_UP);
        order.setUpdatedAt(Instant.now());

        // 3. Generate Delivery OTP for customer handover (30 mins TTL in Redis)
        String deliveryOtp = otpService.createAndStoreOtp(orderId, OtpType.DELIVERY);
        assignment.setDeliveryOtp(deliveryOtp);

        order.addEventLog("DELIVERY_PICKED_UP", "food.delivery.pickup",
                "Agent " + assignment.getDeliveryAgent().getName() + " picked up order from restaurant. Customer Delivery OTP generated.",
                "RESTAURANT_AGENT_APP");

        deliveryAssignmentRepository.save(assignment);
        orderRepository.save(order);

        // 4. Emit DeliveryPickupEvent to Kafka
        DeliveryPickupEvent event = DeliveryPickupEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .deliveryAgentId(assignment.getDeliveryAgent().getId())
                .deliveryAgentName(assignment.getDeliveryAgent().getName())
                .customerPhone(order.getCustomerPhone())
                .customerAddress(order.getCustomerAddress())
                .distanceKm(order.getDistanceKm())
                .deliveryOtp(deliveryOtp)
                .pickedUpAt(Instant.now())
                .build();

        deliveryEventProducer.publishDeliveryPickup(event);

        return DeliveryConfirmationResponse.builder()
                .success(true)
                .message("Pickup confirmed successfully! Delivery partner is en route.")
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(OrderStatus.PICKED_UP.name())
                .deliveryAgentName(assignment.getDeliveryAgent().getName())
                .generatedDeliveryOtp(deliveryOtp)
                .actionTimestamp(Instant.now())
                .build();
    }

    /**
     * Verifies Delivery OTP upon arrival at customer location.
     * Transitions order to DELIVERED, marks delivery complete, frees agent.
     */
    @Transactional
    public DeliveryConfirmationResponse confirmDelivery(Long orderId, String providedOtp) {
        log.info("Attempting delivery completion confirmation for order ID: {}", orderId);

        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No delivery assignment found for order ID: " + orderId));

        if (!Boolean.TRUE.equals(assignment.getPickupOtpVerified())) {
            throw new IllegalStateException("Cannot confirm delivery before pickup has been completed!");
        }

        if (Boolean.TRUE.equals(assignment.getDeliveryOtpVerified())) {
            throw new IllegalStateException("Order has already been marked DELIVERED!");
        }

        // 1. Verify OTP with Redis
        boolean isValid = otpService.verifyOtp(orderId, OtpType.DELIVERY, providedOtp);
        if (!isValid) {
            log.warn("Invalid or expired Delivery OTP provided for order ID: {}", orderId);
            throw new IllegalArgumentException("Invalid or expired Delivery OTP!");
        }

        // 2. Update Status
        assignment.setDeliveryOtpVerified(true);
        assignment.setDeliveredTime(Instant.now());
        assignment.setStatus("DELIVERED");

        Order order = assignment.getOrder();
        order.setStatus(OrderStatus.DELIVERED);
        order.setUpdatedAt(Instant.now());

        // 3. Free Delivery Agent back to AVAILABLE fleet
        deliveryAgentService.releaseAgent(assignment.getDeliveryAgent().getId());

        order.addEventLog("ORDER_DELIVERED", "food.delivery.completed",
                "Order delivered successfully to customer by " + assignment.getDeliveryAgent().getName(),
                "CUSTOMER_AGENT_APP");

        deliveryAssignmentRepository.save(assignment);
        orderRepository.save(order);

        // 4. Emit DeliveryCompletedEvent to Kafka
        DeliveryCompletedEvent event = DeliveryCompletedEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .deliveryAgentId(assignment.getDeliveryAgent().getId())
                .deliveryAgentName(assignment.getDeliveryAgent().getName())
                .distanceKm(order.getDistanceKm())
                .deliveredAt(Instant.now())
                .build();

        deliveryEventProducer.publishDeliveryCompleted(event);

        return DeliveryConfirmationResponse.builder()
                .success(true)
                .message("Order delivered successfully to customer!")
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(OrderStatus.DELIVERED.name())
                .deliveryAgentName(assignment.getDeliveryAgent().getName())
                .generatedDeliveryOtp(null)
                .actionTimestamp(Instant.now())
                .build();
    }

    /**
     * Inspect active OTPs (utility for testing/demo)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getActiveOtps(Long orderId) {
        String pickupOtp = otpService.getActiveOtp(orderId, OtpType.PICKUP);
        String deliveryOtp = otpService.getActiveOtp(orderId, OtpType.DELIVERY);
        return Map.of(
                "orderId", orderId,
                "activePickupOtpInRedis", pickupOtp != null ? pickupOtp : "EXPIRED_OR_NOT_ISSUED",
                "activeDeliveryOtpInRedis", deliveryOtp != null ? deliveryOtp : "EXPIRED_OR_NOT_ISSUED"
        );
    }
}
