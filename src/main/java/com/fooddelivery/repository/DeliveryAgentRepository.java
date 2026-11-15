package com.fooddelivery.repository;

import com.fooddelivery.model.entity.DeliveryAgent;
import com.fooddelivery.model.enums.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgent, Long> {
    List<DeliveryAgent> findByStatus(AgentStatus status);

    Optional<DeliveryAgent> findByPhone(String phone);

    @Query(value = "SELECT * FROM delivery_agents WHERE status = 'AVAILABLE' ORDER BY id ASC LIMIT 1", nativeQuery = true)
    Optional<DeliveryAgent> findFirstAvailableAgent();

    long countByStatus(AgentStatus status);
}
