package com.fooddelivery.controller;

import com.fooddelivery.dto.DeliveryAgentResponse;
import com.fooddelivery.service.DeliveryAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-agents")
@RequiredArgsConstructor
@Tag(name = "Delivery Agents", description = "Endpoints for tracking delivery partners and fleet availability")
public class DeliveryAgentController {

    private final DeliveryAgentService deliveryAgentService;

    @GetMapping
    @Operation(summary = "List all delivery agents", description = "Retrieves the complete fleet of 20 delivery agents and their active status (AVAILABLE, ASSIGNED, BUSY).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Complete list of delivery agents")
    })
    public ResponseEntity<List<DeliveryAgentResponse>> getAllAgents() {
        return ResponseEntity.ok(deliveryAgentService.getAllAgents());
    }

    @GetMapping("/available")
    @Operation(summary = "List available delivery agents", description = "Retrieves only agents currently in AVAILABLE status and ready for dispatch.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of currently available agents")
    })
    public ResponseEntity<List<DeliveryAgentResponse>> getAvailableAgents() {
        return ResponseEntity.ok(deliveryAgentService.getAvailableAgents());
    }
}
