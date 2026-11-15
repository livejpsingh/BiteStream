package com.fooddelivery.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class EtaCalculationService {

    @Value("${food-delivery.logistics.speed-km-per-hour:20.0}")
    private double averageSpeedKmh;

    @Value("${food-delivery.logistics.buffer-pickup-minutes:4}")
    private int bufferPickupMinutes;

    @Value("${food-delivery.logistics.queue-penalty-per-order-minutes:3}")
    private int queuePenaltyPerOrderMinutes;

    /**
     * Calculates travel time for urban delivery based on distance (e.g., 2, 3, 4, 5 km)
     * Example:
     * - 2.0 km -> ~6 min transit + 4 min buffer = 10 minutes
     * - 3.0 km -> ~9 min transit + 4 min buffer = 13 minutes
     * - 4.0 km -> ~12 min transit + 4 min buffer = 16 minutes
     * - 5.0 km -> ~15 min transit + 4 min buffer = 19 minutes
     */
    public int calculateTravelTimeMinutes(Double distanceKm) {
        if (distanceKm == null || distanceKm <= 0) {
            distanceKm = 2.0; // default baseline distance
        }
        double transitMinutes = (distanceKm / averageSpeedKmh) * 60.0;
        int totalTravelTime = (int) Math.ceil(transitMinutes) + bufferPickupMinutes;
        log.debug("Distance: {} km, Transit: {} min, Buffer: {} min -> Total Travel ETA: {} min",
                distanceKm, transitMinutes, bufferPickupMinutes, totalTravelTime);
        return totalTravelTime;
    }

    /**
     * Calculates kitchen prep time factoring in restaurant queue position and item complexities
     */
    public int calculatePreparationTimeMinutes(List<Integer> itemPrepTimes, int activeQueueCount) {
        int basePrepTime = itemPrepTimes != null && !itemPrepTimes.isEmpty()
                ? itemPrepTimes.stream().max(Integer::compareTo).orElse(15)
                : 15;

        int queueDelay = activeQueueCount * queuePenaltyPerOrderMinutes;
        int totalPrepTime = basePrepTime + queueDelay;

        log.debug("Base prep: {} min, Queue position delay ({} orders ahead * {} min): {} min -> Total Prep: {} min",
                basePrepTime, activeQueueCount, queuePenaltyPerOrderMinutes, queueDelay, totalPrepTime);
        return totalPrepTime;
    }

    /**
     * Total end-to-end delivery ETA
     */
    public int calculateTotalDeliveryEta(int prepTimeMinutes, int travelTimeMinutes) {
        return prepTimeMinutes + travelTimeMinutes;
    }
}
