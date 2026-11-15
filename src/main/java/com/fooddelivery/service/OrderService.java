package com.fooddelivery.service;

import com.fooddelivery.dto.OrderPlacementRequest;
import com.fooddelivery.dto.OrderResponse;
import com.fooddelivery.event.OrderPlacedEvent;
import com.fooddelivery.kafka.producer.OrderEventProducer;
import com.fooddelivery.model.entity.MenuItem;
import com.fooddelivery.model.entity.Order;
import com.fooddelivery.model.entity.OrderItem;
import com.fooddelivery.model.entity.Restaurant;
import com.fooddelivery.model.enums.OrderStatus;
import com.fooddelivery.model.json.OrderMetadata;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final EtaCalculationService etaCalculationService;
    private final OrderEventProducer orderEventProducer;

    @Transactional
    public OrderResponse placeOrder(OrderPlacementRequest request) {
        log.info("Received order request from customer: {} for restaurant ID: {} at distance: {} km",
                request.getCustomerName(), request.getRestaurantId(), request.getDistanceKm());

        // 1. Validate Restaurant
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with ID: " + request.getRestaurantId()));

        if (Boolean.FALSE.equals(restaurant.getIsActive())) {
            throw new IllegalStateException("Restaurant is currently closed or inactive");
        }

        // 2. Fetch and calculate order items & totals
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Integer> prepTimes = new ArrayList<>();
        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderPlacedEvent.OrderItemDetail> eventItemDetails = new ArrayList<>();

        for (OrderPlacementRequest.ItemOrderDto itemDto : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemDto.getMenuItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Menu item not found with ID: " + itemDto.getMenuItemId()));

            if (!menuItem.getRestaurant().getId().equals(restaurant.getId())) {
                throw new IllegalArgumentException("Item '" + menuItem.getName() + "' does not belong to restaurant " + restaurant.getName());
            }

            BigDecimal lineTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
            prepTimes.add(menuItem.getPrepTimeMinutes());

            OrderItem orderItem = OrderItem.builder()
                    .menuItem(menuItem)
                    .itemName(menuItem.getName())
                    .quantity(itemDto.getQuantity())
                    .unitPrice(menuItem.getPrice())
                    .subtotal(lineTotal)
                    .build();
            orderItems.add(orderItem);

            eventItemDetails.add(OrderPlacedEvent.OrderItemDetail.builder()
                    .menuItemId(menuItem.getId())
                    .itemName(menuItem.getName())
                    .quantity(itemDto.getQuantity())
                    .unitPrice(menuItem.getPrice())
                    .subtotal(lineTotal)
                    .build());
        }

        // 3. Queue and Distance ETA Estimation
        int activeOrdersAhead = orderRepository.countActiveOrdersForRestaurant(
                restaurant.getId(),
                List.of(OrderStatus.PLACED, OrderStatus.RESTAURANT_ACCEPTED, OrderStatus.PREPARING)
        );
        int queuePosition = activeOrdersAhead + 1;

        int estimatedPrepMinutes = etaCalculationService.calculatePreparationTimeMinutes(prepTimes, activeOrdersAhead);
        int estimatedTravelMinutes = etaCalculationService.calculateTravelTimeMinutes(request.getDistanceKm());
        int estimatedTotalDeliveryMinutes = etaCalculationService.calculateTotalDeliveryEta(estimatedPrepMinutes, estimatedTravelMinutes);

        // 4. Build Order with JSONB metadata and audit logs
        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + (100 + new Random().nextInt(900));

        OrderMetadata metadata = OrderMetadata.builder()
                .specialInstructions(request.getSpecialInstructions())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "ONLINE_UPI")
                .priority(request.getDistanceKm() > 4.0 ? "HIGH_DISTANCE" : "STANDARD")
                .additionalAttributes(Map.of("queue_depth", activeOrdersAhead))
                .build();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerAddress(request.getCustomerAddress())
                .customerLatitude(request.getCustomerLatitude() != null ? request.getCustomerLatitude() : restaurant.getLatitude() + 0.015)
                .customerLongitude(request.getCustomerLongitude() != null ? request.getCustomerLongitude() : restaurant.getLongitude() + 0.015)
                .restaurant(restaurant)
                .status(OrderStatus.PLACED)
                .distanceKm(request.getDistanceKm())
                .queuePosition(queuePosition)
                .estimatedPrepTimeMinutes(estimatedPrepMinutes)
                .estimatedTravelTimeMinutes(estimatedTravelMinutes)
                .estimatedTotalTimeMinutes(estimatedTotalDeliveryMinutes)
                .totalAmount(totalAmount)
                .orderMetadata(metadata)
                .eventLogs(new ArrayList<>())
                .build();

        for (OrderItem oi : orderItems) {
            order.addItem(oi);
        }

        order.addEventLog("ORDER_PLACED", "food.orders.placed",
                "Order created with Queue position #" + queuePosition + ", Distance " + request.getDistanceKm() + " km", "API_GATEWAY");

        Order savedOrder = orderRepository.save(order);

        // 5. Produce Event to Kafka
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .restaurantId(restaurant.getId())
                .restaurantName(restaurant.getName())
                .customerName(savedOrder.getCustomerName())
                .customerPhone(savedOrder.getCustomerPhone())
                .customerAddress(savedOrder.getCustomerAddress())
                .distanceKm(savedOrder.getDistanceKm())
                .queuePosition(queuePosition)
                .estimatedPrepTimeMinutes(estimatedPrepMinutes)
                .estimatedTravelTimeMinutes(estimatedTravelMinutes)
                .estimatedTotalTimeMinutes(estimatedTotalDeliveryMinutes)
                .totalAmount(totalAmount)
                .items(eventItemDetails)
                .placedAt(Instant.now())
                .build();

        orderEventProducer.publishOrderPlaced(event);

        return toOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));
        return toOrderResponse(order);
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderResponse.OrderItemDetailDto> itemsDto = order.getItems().stream()
                .map(i -> OrderResponse.OrderItemDetailDto.builder()
                        .id(i.getId())
                        .menuItemId(i.getMenuItem().getId())
                        .itemName(i.getItemName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getSubtotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .restaurantId(order.getRestaurant().getId())
                .restaurantName(order.getRestaurant().getName())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .customerAddress(order.getCustomerAddress())
                .distanceKm(order.getDistanceKm())
                .queuePosition(order.getQueuePosition())
                .estimatedPrepTimeMinutes(order.getEstimatedPrepTimeMinutes())
                .estimatedTravelTimeMinutes(order.getEstimatedTravelTimeMinutes())
                .estimatedTotalDeliveryTimeMinutes(order.getEstimatedTotalTimeMinutes())
                .totalAmount(order.getTotalAmount())
                .items(itemsDto)
                .metadata(order.getOrderMetadata())
                .lifecycleEvents(order.getEventLogs())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
