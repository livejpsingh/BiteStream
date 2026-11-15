package com.fooddelivery.controller;

import com.fooddelivery.dto.MenuItemResponse;
import com.fooddelivery.dto.RestaurantResponse;
import com.fooddelivery.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping
    public ResponseEntity<List<RestaurantResponse>> getAllRestaurants() {
        return ResponseEntity.ok(restaurantService.getAllActiveRestaurants());
    }

    @GetMapping("/{id}/menu")
    public ResponseEntity<List<MenuItemResponse>> getRestaurantMenu(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getMenuItemsForRestaurant(id));
    }
}
