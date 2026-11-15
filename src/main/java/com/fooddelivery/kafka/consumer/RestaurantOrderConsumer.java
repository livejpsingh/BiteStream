package com.fooddelivery.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.event.RestaurantNotificationEvent;
import com.fooddelivery.model.entity.Order;
import com.fooddelivery.model.enums.OrderStatus;
import com.fooddelivery.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantOrderConsumer {

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "${food-delivery.topics.restaurant-notification}",
            groupId = "food-restaurant-kitchen-group"
    )
    @Transactional
    public void onRestaurantNotification(Object rawPayload) {
        try {
            RestaurantNotificationEvent event = (rawPayload instanceof RestaurantNotificationEvent)
                    ? (RestaurantNotificationEvent) rawPayload
                    : objectMapper.convertValue(rawPayload, RestaurantNotificationEvent.class);

            log.info("----------------------------------------------------------------------");
            log.info(">>> [RESTAURANT KITCHEN TERMINAL] Order Alert for Store: {}", event.getRestaurantName());
            log.info(">>> Order #: {} | Queue Position: #{}", event.getOrderNumber(), event.getQueuePosition());
            log.info(">>> Estimated Cooking Time: {} minutes", event.getEstimatedPrepTimeMinutes());
            log.info(">>> Items to Prepare: {}", event.getItemSummary());
            if (event.getSpecialInstructions() != null && !event.getSpecialInstructions().isBlank()) {
                log.info(">>> Chef Instructions: {}", event.getSpecialInstructions());
            }
            log.info("----------------------------------------------------------------------");

            // Update order status to PREPARING
            orderRepository.findById(event.getOrderId()).ifPresent(order -> {
                order.setStatus(OrderStatus.PREPARING);
                order.addEventLog("KITCHEN_PREPARING", "food.restaurant.notifications",
                        "Store " + event.getRestaurantName() + " started cooking order. Position in queue: #" + event.getQueuePosition(),
                        "RESTAURANT_KOT");
                orderRepository.save(order);
            });

        } catch (Exception e) {
            log.error("Failed to process RestaurantNotificationEvent", e);
        }
    }
}
