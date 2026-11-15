package com.fooddelivery.model.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderMetadata implements Serializable {
    private String specialInstructions;
    private String paymentMethod;
    private Double tipAmount;
    private Double discountAmount;
    private String priority;
    private Map<String, Object> additionalAttributes;
}
