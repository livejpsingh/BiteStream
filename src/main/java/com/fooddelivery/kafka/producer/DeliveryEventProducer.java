package com.fooddelivery.kafka.producer;

import com.fooddelivery.event.DeliveryAssignedEvent;
import com.fooddelivery.event.DeliveryCompletedEvent;
import com.fooddelivery.event.DeliveryPickupEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${food-delivery.topics.delivery-assigned}")
    private String deliveryAssignedTopic;

    @Value("${food-delivery.topics.delivery-pickup}")
    private String deliveryPickupTopic;

    @Value("${food-delivery.topics.delivery-completed}")
    private String deliveryCompletedTopic;

    public void publishDeliveryAssigned(DeliveryAssignedEvent event) {
        log.info("Publishing DeliveryAssignedEvent to [{}]: OrderNumber={}, AgentId={}, Distance={}km, PickupOTP={}",
                deliveryAssignedTopic, event.getOrderNumber(), event.getDeliveryAgentId(), event.getDistanceKm(), event.getPickupOtp());
        kafkaTemplate.send(deliveryAssignedTopic, event.getOrderNumber(), event);
    }

    public void publishDeliveryPickup(DeliveryPickupEvent event) {
        log.info("Publishing DeliveryPickupEvent to [{}]: OrderNumber={}, AgentId={}, DeliveryOTP={}",
                deliveryPickupTopic, event.getOrderNumber(), event.getDeliveryAgentId(), event.getDeliveryOtp());
        kafkaTemplate.send(deliveryPickupTopic, event.getOrderNumber(), event);
    }

    public void publishDeliveryCompleted(DeliveryCompletedEvent event) {
        log.info("Publishing DeliveryCompletedEvent to [{}]: OrderNumber={}, AgentId={}",
                deliveryCompletedTopic, event.getOrderNumber(), event.getDeliveryAgentId());
        kafkaTemplate.send(deliveryCompletedTopic, event.getOrderNumber(), event);
    }
}
