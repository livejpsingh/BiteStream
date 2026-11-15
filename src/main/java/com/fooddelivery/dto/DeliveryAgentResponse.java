package com.fooddelivery.dto;

import com.fooddelivery.model.enums.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAgentResponse implements Serializable {
    private Long id;
    private String name;
    private String phone;
    private String vehicleType;
    private AgentStatus status;
    private Double currentLatitude;
    private Double currentLongitude;
    private BigDecimal rating;
}
