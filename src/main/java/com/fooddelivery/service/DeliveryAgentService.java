package com.fooddelivery.service;

import com.fooddelivery.dto.DeliveryAgentResponse;
import com.fooddelivery.model.entity.DeliveryAgent;
import com.fooddelivery.model.enums.AgentStatus;
import com.fooddelivery.repository.DeliveryAgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryAgentService {

    private final DeliveryAgentRepository deliveryAgentRepository;

    @Transactional(readOnly = true)
    public List<DeliveryAgentResponse> getAllAgents() {
        return deliveryAgentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryAgentResponse> getAvailableAgents() {
        return deliveryAgentRepository.findByStatus(AgentStatus.AVAILABLE).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Finds the most optimal available agent closest to restaurant coordinates
     */
    @Transactional
    public Optional<DeliveryAgent> findAndAssignAgent(Double restaurantLat, Double restaurantLng) {
        List<DeliveryAgent> availableAgents = deliveryAgentRepository.findByStatus(AgentStatus.AVAILABLE);

        if (availableAgents.isEmpty()) {
            log.warn("No available delivery agents currently found among the fleet of 20!");
            return Optional.empty();
        }

        // Sort by Euclidean distance to restaurant to pick the nearest available partner
        DeliveryAgent selectedAgent = availableAgents.stream()
                .min(Comparator.comparingDouble(agent ->
                        calculateGeoDistance(restaurantLat, restaurantLng,
                                agent.getCurrentLatitude(), agent.getCurrentLongitude())))
                .orElse(availableAgents.get(0));

        selectedAgent.setStatus(AgentStatus.ASSIGNED);
        deliveryAgentRepository.save(selectedAgent);
        log.info("Assigned Delivery Agent [ID: {}, Name: {}, Vehicle: {}] for dispatch",
                selectedAgent.getId(), selectedAgent.getName(), selectedAgent.getVehicleType());

        return Optional.of(selectedAgent);
    }

    @Transactional
    public void releaseAgent(Long agentId) {
        deliveryAgentRepository.findById(agentId).ifPresent(agent -> {
            agent.setStatus(AgentStatus.AVAILABLE);
            deliveryAgentRepository.save(agent);
            log.info("Released Delivery Agent [ID: {}, Name: {}] back to AVAILABLE status",
                    agent.getId(), agent.getName());
        });
    }

    private double calculateGeoDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c; // Earth's radius in KM
    }

    private DeliveryAgentResponse toResponse(DeliveryAgent agent) {
        return DeliveryAgentResponse.builder()
                .id(agent.getId())
                .name(agent.getName())
                .phone(agent.getPhone())
                .vehicleType(agent.getVehicleType())
                .status(agent.getStatus())
                .currentLatitude(agent.getCurrentLatitude())
                .currentLongitude(agent.getCurrentLongitude())
                .rating(agent.getRating())
                .build();
    }
}
