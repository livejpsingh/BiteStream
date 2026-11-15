package com.fooddelivery.dto;

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
public class RestaurantResponse implements Serializable {
    private Long id;
    private String name;
    private String cuisineType;
    private String address;
    private String contactNumber;
    private Double latitude;
    private Double longitude;
    private Map<String, Object> operatingHours;
    private Boolean isActive;
}
