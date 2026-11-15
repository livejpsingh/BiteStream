package com.fooddelivery.service;

import com.fooddelivery.dto.MenuItemResponse;
import com.fooddelivery.dto.RestaurantResponse;
import com.fooddelivery.model.entity.Restaurant;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    /**
     * Cached with Redis to avoid repeated queries to PostgreSQL for static store directories
     */
    @Cacheable(value = "restaurants")
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getAllActiveRestaurants() {
        log.info("Fetching all active restaurants from PostgreSQL (cache miss)...");
        return restaurantRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Restaurant getRestaurantById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with id: " + id));
    }

    /**
     * Cached with Redis per restaurant ID
     */
    @Cacheable(value = "restaurant_menus", key = "#restaurantId")
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenuItemsForRestaurant(Long restaurantId) {
        log.info("Fetching menu items from PostgreSQL for restaurant id {} (cache miss)...", restaurantId);
        return menuItemRepository.findByRestaurantIdAndIsAvailableTrue(restaurantId).stream()
                .map(item -> MenuItemResponse.builder()
                        .id(item.getId())
                        .restaurantId(restaurantId)
                        .name(item.getName())
                        .description(item.getDescription())
                        .price(item.getPrice())
                        .prepTimeMinutes(item.getPrepTimeMinutes())
                        .isAvailable(item.getIsAvailable())
                        .build())
                .toList();
    }

    private RestaurantResponse toResponse(Restaurant r) {
        return RestaurantResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .cuisineType(r.getCuisineType())
                .address(r.getAddress())
                .contactNumber(r.getContactNumber())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .operatingHours(r.getOperatingHours())
                .isActive(r.getIsActive())
                .build();
    }
}
