package com.fooddelivery.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EtaCalculationServiceTest {

    private EtaCalculationService etaCalculationService;

    @BeforeEach
    void setUp() {
        etaCalculationService = new EtaCalculationService();
        ReflectionTestUtils.setField(etaCalculationService, "averageSpeedKmh", 20.0);
        ReflectionTestUtils.setField(etaCalculationService, "bufferPickupMinutes", 4);
        ReflectionTestUtils.setField(etaCalculationService, "queuePenaltyPerOrderMinutes", 3);
    }

    @Test
    @DisplayName("Verify distance-based travel time for 2, 3, 4, 5 km")
    void testTravelTimeCalculationsForVariousDistances() {
        // 2 km: (2 / 20) * 60 = 6 min transit + 4 buffer = 10 min
        int travelTime2km = etaCalculationService.calculateTravelTimeMinutes(2.0);
        assertEquals(10, travelTime2km);

        // 3 km: (3 / 20) * 60 = 9 min transit + 4 buffer = 13 min
        int travelTime3km = etaCalculationService.calculateTravelTimeMinutes(3.0);
        assertEquals(13, travelTime3km);

        // 4 km: (4 / 20) * 60 = 12 min transit + 4 buffer = 16 min
        int travelTime4km = etaCalculationService.calculateTravelTimeMinutes(4.0);
        assertEquals(16, travelTime4km);

        // 5 km: (5 / 20) * 60 = 15 min transit + 4 buffer = 19 min
        int travelTime5km = etaCalculationService.calculateTravelTimeMinutes(5.0);
        assertEquals(19, travelTime5km);
    }

    @Test
    @DisplayName("Verify restaurant kitchen queue delay penalty")
    void testKitchenQueuePenalty() {
        List<Integer> itemPrepTimes = List.of(12, 18, 15); // max is 18
        // 0 orders in queue
        int prepTimeZeroQueue = etaCalculationService.calculatePreparationTimeMinutes(itemPrepTimes, 0);
        assertEquals(18, prepTimeZeroQueue);

        // 2 orders ahead: 18 + (2 * 3) = 24 mins
        int prepTimeTwoAhead = etaCalculationService.calculatePreparationTimeMinutes(itemPrepTimes, 2);
        assertEquals(24, prepTimeTwoAhead);

        // 5 orders ahead: 18 + (5 * 3) = 33 mins
        int prepTimeFiveAhead = etaCalculationService.calculatePreparationTimeMinutes(itemPrepTimes, 5);
        assertEquals(33, prepTimeFiveAhead);
    }

    @Test
    @DisplayName("Verify total delivery ETA aggregation")
    void testTotalDeliveryEta() {
        int totalEta = etaCalculationService.calculateTotalDeliveryEta(24, 13);
        assertEquals(37, totalEta);
    }
}
