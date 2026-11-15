package com.fooddelivery.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.event.DeliveryCompletedEvent;
import com.fooddelivery.event.DeliveryPickupEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryLifecycleConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${food-delivery.topics.delivery-pickup}",
            groupId = "food-delivery-lifecycle-group"
    )
    public void onOrderPickedUp(Object rawPayload) {
        try {
            DeliveryPickupEvent event = (rawPayload instanceof DeliveryPickupEvent)
                    ? (DeliveryPickupEvent) rawPayload
                    : objectMapper.convertValue(rawPayload, DeliveryPickupEvent.class);

            log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            log.info(">>> [CUSTOMER NOTIFICATION] Order #{} has been PICKED UP!", event.getOrderNumber());
            log.info(">>> Partner {} is on the way to your location ({} km away)",
                    event.getDeliveryAgentName(), event.getDistanceKm());
            log.info(">>> YOUR DELIVERY OTP (Provide to partner upon packet arrival): {}", event.getDeliveryOtp());
            log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        } catch (Exception e) {
            log.error("Failed to process DeliveryPickupEvent", e);
        }
    }

    @KafkaListener(
            topics = "${food-delivery.topics.delivery-completed}",
            groupId = "food-delivery-lifecycle-group"
    )
    public void onOrderCompleted(Object rawPayload) {
        try {
            DeliveryCompletedEvent event = (rawPayload instanceof DeliveryCompletedEvent)
                    ? (DeliveryCompletedEvent) rawPayload
                    : objectMapper.convertValue(rawPayload, DeliveryCompletedEvent.class);

            log.info("**********************************************************************");
            log.info(">>> [AUDIT & SETTLEMENT] Order #{} marked DELIVERED successfully!", event.getOrderNumber());
            log.info(">>> Delivered by: {} | Distance: {} km | Timestamp: {}",
                    event.getDeliveryAgentName(), event.getDistanceKm(), event.getDeliveredAt());
            log.info("**********************************************************************");

        } catch (Exception e) {
            log.error("Failed to process DeliveryCompletedEvent", e);
        }
    }
}
