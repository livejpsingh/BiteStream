package com.fooddelivery.repository;

import com.fooddelivery.model.entity.Order;
import com.fooddelivery.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByRestaurantIdAndStatusIn(Long restaurantId, Collection<OrderStatus> statuses);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.restaurant.id = :restaurantId AND o.status IN :statuses")
    int countActiveOrdersForRestaurant(@Param("restaurantId") Long restaurantId,
                                      @Param("statuses") Collection<OrderStatus> statuses);

    List<Order> findByCustomerPhoneOrderByCreatedAtDesc(String customerPhone);
}
