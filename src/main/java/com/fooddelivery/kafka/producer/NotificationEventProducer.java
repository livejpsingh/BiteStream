package com.fooddelivery.kafka.producer;

import com.fooddelivery.event.RestaurantNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${food-delivery.topics.restaurant-notification}")
    private String restaurantNotificationTopic;

    public void publishRestaurantNotification(RestaurantNotificationEvent event) {
        log.info("Publishing RestaurantNotificationEvent to topic [{}]: RestaurantId={}, OrderNumber={}, QueuePos={}",
                restaurantNotificationTopic, event.getRestaurantId(), event.getOrderNumber(), event.getQueuePosition());
        kafkaTemplate.send(restaurantNotificationTopic, String.valueOf(event.getRestaurantId()), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Delivered RestaurantNotificationEvent for store {} to partition {}",
                                event.getRestaurantName(), result.getRecordMetadata().partition());
                    } else {
                        log.error("Failed to publish RestaurantNotificationEvent for store {}", event.getRestaurantName(), ex);
                    }
                });
    }
}
