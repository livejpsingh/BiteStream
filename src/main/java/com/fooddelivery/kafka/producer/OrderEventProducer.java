package com.fooddelivery.kafka.producer;

import com.fooddelivery.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${food-delivery.topics.order-placed}")
    private String orderPlacedTopic;

    public void publishOrderPlaced(OrderPlacedEvent event) {
        log.info("Publishing OrderPlacedEvent to Kafka topic [{}]: OrderNumber={}",
                orderPlacedTopic, event.getOrderNumber());
        kafkaTemplate.send(orderPlacedTopic, event.getOrderNumber(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully delivered OrderPlacedEvent to partition {} at offset {}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish OrderPlacedEvent for order {}", event.getOrderNumber(), ex);
                    }
                });
    }
}
