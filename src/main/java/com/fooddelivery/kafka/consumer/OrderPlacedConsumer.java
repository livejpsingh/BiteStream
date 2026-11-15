package com.fooddelivery.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.event.DeliveryAssignedEvent;
import com.fooddelivery.event.OrderPlacedEvent;
import com.fooddelivery.event.RestaurantNotificationEvent;
import com.fooddelivery.kafka.producer.DeliveryEventProducer;
import com.fooddelivery.kafka.producer.NotificationEventProducer;
import com.fooddelivery.model.entity.DeliveryAgent;
import com.fooddelivery.model.entity.DeliveryAssignment;
import com.fooddelivery.model.entity.Order;
import com.fooddelivery.model.entity.Restaurant;
import com.fooddelivery.model.enums.OrderStatus;
import com.fooddelivery.model.enums.OtpType;
import com.fooddelivery.repository.DeliveryAssignmentRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.service.DeliveryAgentService;
import com.fooddelivery.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPlacedConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationEventProducer notificationEventProducer;
    private final DeliveryEventProducer deliveryEventProducer;
    private final DeliveryAgentService deliveryAgentService;
    private final OtpService otpService;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;

    @KafkaListener(
            topics = "${food-delivery.topics.order-placed}",
            groupId = "food-delivery-orchestrator-group"
    )
    @Transactional
    public void handleOrderPlaced(Object rawPayload) {
        try {
            OrderPlacedEvent event = (rawPayload instanceof OrderPlacedEvent)
                    ? (OrderPlacedEvent) rawPayload
                    : objectMapper.convertValue(rawPayload, OrderPlacedEvent.class);

            log.info("====> [Kafka Consumer] Received OrderPlacedEvent for Order: {}", event.getOrderNumber());

            Order order = orderRepository.findById(event.getOrderId()).orElse(null);
            if (order == null) {
                log.error("Order not found in database: {}", event.getOrderId());
                return;
            }

            Restaurant restaurant = restaurantRepository.findById(event.getRestaurantId())
                    .orElse(order.getRestaurant());

            // 1. Send Event to Restaurant (Notification & Kitchen Prep Queue)
            List<String> itemsSummary = event.getItems().stream()
                    .map(i -> i.getQuantity() + "x " + i.getItemName())
                    .toList();

            RestaurantNotificationEvent restNotification = RestaurantNotificationEvent.builder()
                    .restaurantId(restaurant.getId())
                    .restaurantName(restaurant.getName())
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .queuePosition(event.getQueuePosition())
                    .estimatedPrepTimeMinutes(event.getEstimatedPrepTimeMinutes())
                    .itemSummary(itemsSummary)
                    .specialInstructions(order.getOrderMetadata() != null ? order.getOrderMetadata().getSpecialInstructions() : null)
                    .notifiedAt(Instant.now())
                    .build();

            notificationEventProducer.publishRestaurantNotification(restNotification);

            // 2. Assign Delivery Partner from Fleet of 20 Agents
            Optional<DeliveryAgent> assignedAgentOpt = deliveryAgentService.findAndAssignAgent(
                    restaurant.getLatitude(),
                    restaurant.getLongitude()
            );

            if (assignedAgentOpt.isEmpty()) {
                log.warn("All 20 delivery agents are currently busy for order {}", order.getOrderNumber());
                order.addEventLog("DISPATCH_WAITING", "food.delivery.assigned",
                        "No available delivery agent found. Order placed in dispatch waiting queue.", "DISPATCHER");
                orderRepository.save(order);
                return;
            }

            DeliveryAgent agent = assignedAgentOpt.get();

            // 3. Generate 6-digit Pickup OTP stored in Redis with 15-minute TTL
            String pickupOtp = otpService.createAndStoreOtp(order.getId(), OtpType.PICKUP);

            // 4. Save Delivery Assignment
            DeliveryAssignment assignment = DeliveryAssignment.builder()
                    .order(order)
                    .deliveryAgent(agent)
                    .status("ASSIGNED")
                    .pickupOtp(pickupOtp)
                    .pickupOtpVerified(false)
                    .deliveryOtpVerified(false)
                    .distanceKm(order.getDistanceKm())
                    .createdAt(Instant.now())
                    .build();

            deliveryAssignmentRepository.save(assignment);

            order.setStatus(OrderStatus.RESTAURANT_ACCEPTED);
            order.addEventLog("DELIVERY_ASSIGNED", "food.delivery.assigned",
                    "Assigned partner " + agent.getName() + " (" + agent.getVehicleType() + "). Pickup OTP generated.",
                    "DISPATCHER");
            orderRepository.save(order);

            // 5. Publish DeliveryAssignedEvent to Kafka
            DeliveryAssignedEvent deliveryAssignedEvent = DeliveryAssignedEvent.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .deliveryAgentId(agent.getId())
                    .deliveryAgentName(agent.getName())
                    .deliveryAgentPhone(agent.getPhone())
                    .vehicleType(agent.getVehicleType())
                    .restaurantId(restaurant.getId())
                    .restaurantName(restaurant.getName())
                    .restaurantAddress(restaurant.getAddress())
                    .customerName(order.getCustomerName())
                    .customerAddress(order.getCustomerAddress())
                    .distanceKm(order.getDistanceKm())
                    .estimatedTravelTimeMinutes(order.getEstimatedTravelTimeMinutes())
                    .estimatedPickupMinutes(order.getEstimatedPrepTimeMinutes())
                    .pickupOtp(pickupOtp)
                    .assignedAt(Instant.now())
                    .build();

            deliveryEventProducer.publishDeliveryAssigned(deliveryAssignedEvent);

        } catch (Exception e) {
            log.error("Error processing OrderPlacedEvent", e);
            throw new RuntimeException(e);
        }
    }
}
