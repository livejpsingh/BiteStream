package com.fooddelivery.repository;

import com.fooddelivery.model.entity.DeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {
    Optional<DeliveryAssignment> findByOrderId(Long orderId);
    Optional<DeliveryAssignment> findByOrderOrderNumber(String orderNumber);
}
