package com.fooddelivery.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.event.DeliveryAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryDispatchConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${food-delivery.topics.delivery-assigned}",
            groupId = "food-delivery-rider-app-group"
    )
    public void onDeliveryAssigned(Object rawPayload) {
        try {
            DeliveryAssignedEvent event = (rawPayload instanceof DeliveryAssignedEvent)
                    ? (DeliveryAssignedEvent) rawPayload
                    : objectMapper.convertValue(rawPayload, DeliveryAssignedEvent.class);

            log.info("======================================================================");
            log.info(">>> [DELIVERY PARTNER APP NOTIFICATION] Order Dispatched!");
            log.info(">>> Assigned Rider: {} (Phone: {}, Vehicle: {})",
                    event.getDeliveryAgentName(), event.getDeliveryAgentPhone(), event.getVehicleType());
            log.info(">>> Order #: {}", event.getOrderNumber());
            log.info(">>> Store: {} at {}", event.getRestaurantName(), event.getRestaurantAddress());
            log.info(">>> Customer: {} at {}", event.getCustomerName(), event.getCustomerAddress());
            log.info(">>> Trip Distance: {} km | Transit ETA: {} mins | Kitchen Prep: {} mins",
                    event.getDistanceKm(), event.getEstimatedTravelTimeMinutes(), event.getEstimatedPickupMinutes());
            log.info(">>> CONFIRMATION PICKUP OTP (Share with Restaurant upon arrival): {}", event.getPickupOtp());
            log.info("======================================================================");

        } catch (Exception e) {
            log.error("Failed to process DeliveryAssignedEvent", e);
        }
    }
}
