package com.fooddelivery.controller;

import com.fooddelivery.dto.OrderPlacementRequest;
import com.fooddelivery.dto.OrderResponse;
import com.fooddelivery.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Endpoints for placing orders, calculating queue-distance ETAs, and tracking lifecycle status")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new food delivery order",
            description = "Validates delivery distance (2.0 to 5.0 km), calculates dynamic kitchen queue + transit ETA, persists POJO+JSONB entities, and streams 'food.orders.placed' event to Kafka.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order successfully created and queued for dispatch"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or distance outside 2-5 km band"),
            @ApiResponse(responseCode = "404", description = "Restaurant or menu item not found")
    })
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderPlacementRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch order details by internal Order ID", description = "Retrieves order status, line items, timestamps, and full event logs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order details found"),
            @ApiResponse(responseCode = "404", description = "Order not found with provided ID")
    })
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Primary key ID of the order", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Fetch order details by public tracking number", description = "Retrieves public order status using the unique tracking number (e.g. ORD-20261114-XXXX).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order details found"),
            @ApiResponse(responseCode = "404", description = "Order not found with provided order number")
    })
    public ResponseEntity<OrderResponse> getOrderByNumber(
            @Parameter(description = "Unique public order tracking number", example = "ORD-20261114-A1B2C3")
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByOrderNumber(orderNumber));
    }
}
