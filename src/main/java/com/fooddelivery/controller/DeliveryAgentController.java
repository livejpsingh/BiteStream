package com.fooddelivery.controller;

import com.fooddelivery.dto.DeliveryAgentResponse;
import com.fooddelivery.service.DeliveryAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-agents")
@RequiredArgsConstructor
public class DeliveryAgentController {

    private final DeliveryAgentService deliveryAgentService;

    @GetMapping
    public ResponseEntity<List<DeliveryAgentResponse>> getAllAgents() {
        return ResponseEntity.ok(deliveryAgentService.getAllAgents());
    }

    @GetMapping("/available")
    public ResponseEntity<List<DeliveryAgentResponse>> getAvailableAgents() {
        return ResponseEntity.ok(deliveryAgentService.getAvailableAgents());
    }
}
