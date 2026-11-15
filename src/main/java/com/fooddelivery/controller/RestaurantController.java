package com.fooddelivery.controller;

import com.fooddelivery.dto.MenuItemResponse;
import com.fooddelivery.dto.RestaurantResponse;
import com.fooddelivery.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurants", description = "Endpoints for exploring registered restaurants and their dynamic menus")
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping
    @Operation(summary = "List all active restaurants", description = "Retrieves all currently active restaurants with address, coordinates, and contact details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of active restaurants retrieved successfully")
    })
    public ResponseEntity<List<RestaurantResponse>> getAllRestaurants() {
        return ResponseEntity.ok(restaurantService.getAllActiveRestaurants());
    }

    @GetMapping("/{id}/menu")
    @Operation(summary = "Get menu for a restaurant", description = "Fetches the full menu item catalog with item IDs, preparation times, and prices.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Menu items retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Restaurant not found with provided ID")
    })
    public ResponseEntity<List<MenuItemResponse>> getRestaurantMenu(
            @Parameter(description = "Primary key ID of the restaurant", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getMenuItemsForRestaurant(id));
    }
}
