package com.fooddelivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fooddelivery.dto.*;
import com.fooddelivery.model.enums.AgentStatus;
import com.fooddelivery.model.enums.OrderStatus;
import com.fooddelivery.model.json.OrderMetadata;
import com.fooddelivery.service.DeliveryAgentService;
import com.fooddelivery.service.DeliveryService;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FoodDeliveryApiEndToEndTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // Lightweight mock implementations to avoid Java 25 ByteBuddy agent locking
    private OrderService orderService;
    private DeliveryService deliveryService;
    private RestaurantService restaurantService;
    private DeliveryAgentService deliveryAgentService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        orderService = new OrderService(null, null, null, null, null) {
            @Override
            public OrderResponse placeOrder(OrderPlacementRequest request) {
                return OrderResponse.builder()
                        .orderId(101L)
                        .orderNumber("ORD-1763105000000-482")
                        .status(OrderStatus.PLACED)
                        .restaurantId(request.getRestaurantId())
                        .restaurantName("Spice Symphony Gourmet")
                        .customerName(request.getCustomerName())
                        .customerPhone(request.getCustomerPhone())
                        .customerAddress(request.getCustomerAddress())
                        .distanceKm(request.getDistanceKm())
                        .queuePosition(1)
                        .estimatedPrepTimeMinutes(18)
                        .estimatedTravelTimeMinutes(13)
                        .estimatedTotalDeliveryTimeMinutes(31)
                        .totalAmount(BigDecimal.valueOf(40.48))
                        .metadata(OrderMetadata.builder()
                                .specialInstructions(request.getSpecialInstructions())
                                .paymentMethod(request.getPaymentMethod())
                                .priority("STANDARD")
                                .build())
                        .createdAt(Instant.now())
                        .build();
            }

            @Override
            public OrderResponse getOrderById(Long id) {
                return OrderResponse.builder()
                        .orderId(id)
                        .orderNumber("ORD-1763105000000-482")
                        .status(OrderStatus.PLACED)
                        .customerName("Rahul Verma")
                        .distanceKm(3.0)
                        .build();
            }

            @Override
            public OrderResponse getOrderByOrderNumber(String orderNumber) {
                return OrderResponse.builder()
                        .orderId(101L)
                        .orderNumber(orderNumber)
                        .status(OrderStatus.PLACED)
                        .customerName("Rahul Verma")
                        .build();
            }
        };

        deliveryService = new DeliveryService(null, null, null, null, null) {
            @Override
            public DeliveryConfirmationResponse confirmPickup(Long orderId, String providedOtp) {
                if (!"684920".equals(providedOtp)) {
                    throw new IllegalArgumentException("Invalid or expired Pickup OTP!");
                }
                return DeliveryConfirmationResponse.builder()
                        .success(true)
                        .message("Pickup confirmed successfully! Delivery partner is en route.")
                        .orderId(orderId)
                        .orderNumber("ORD-1763105000000-482")
                        .status("PICKED_UP")
                        .deliveryAgentName("Aarav Sharma")
                        .generatedDeliveryOtp("512394")
                        .actionTimestamp(Instant.now())
                        .build();
            }

            @Override
            public DeliveryConfirmationResponse confirmDelivery(Long orderId, String providedOtp) {
                if (!"512394".equals(providedOtp)) {
                    throw new IllegalArgumentException("Invalid or expired Delivery OTP!");
                }
                return DeliveryConfirmationResponse.builder()
                        .success(true)
                        .message("Order delivered successfully to customer!")
                        .orderId(orderId)
                        .orderNumber("ORD-1763105000000-482")
                        .status("DELIVERED")
                        .deliveryAgentName("Aarav Sharma")
                        .actionTimestamp(Instant.now())
                        .build();
            }

            @Override
            public Map<String, Object> getActiveOtps(Long orderId) {
                return Map.of(
                        "orderId", orderId,
                        "activePickupOtpInRedis", "684920",
                        "activeDeliveryOtpInRedis", "512394"
                );
            }
        };

        restaurantService = new RestaurantService(null, null) {
            @Override
            public List<RestaurantResponse> getAllActiveRestaurants() {
                return List.of(RestaurantResponse.builder()
                        .id(1L)
                        .name("Spice Symphony Gourmet")
                        .cuisineType("North Indian & Mughlai")
                        .address("124 Heritage Boulevard, Sector 14")
                        .contactNumber("+91-9876543201")
                        .latitude(28.6139)
                        .longitude(77.2090)
                        .isActive(true)
                        .build());
            }

            @Override
            public List<MenuItemResponse> getMenuItemsForRestaurant(Long restaurantId) {
                return List.of(MenuItemResponse.builder()
                        .id(1L)
                        .restaurantId(restaurantId)
                        .name("Butter Chicken Royale")
                        .description("Tender tandoori chicken simmered in rich gravy")
                        .price(BigDecimal.valueOf(14.99))
                        .prepTimeMinutes(18)
                        .isAvailable(true)
                        .build());
            }
        };

        deliveryAgentService = new DeliveryAgentService(null) {
            @Override
            public List<DeliveryAgentResponse> getAllAgents() {
                return List.of(DeliveryAgentResponse.builder()
                        .id(1L)
                        .name("Aarav Sharma")
                        .phone("+91-9800000001")
                        .vehicleType("Electric Scooter")
                        .status(AgentStatus.AVAILABLE)
                        .currentLatitude(28.6150)
                        .currentLongitude(77.2100)
                        .rating(BigDecimal.valueOf(4.90))
                        .build());
            }

            @Override
            public List<DeliveryAgentResponse> getAvailableAgents() {
                return getAllAgents();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new OrderController(orderService),
                        new DeliveryController(deliveryService),
                        new RestaurantController(restaurantService),
                        new DeliveryAgentController(deliveryAgentService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("GET /api/restaurants - Fetch all active restaurants")
    void testGetAllRestaurants() throws Exception {
        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Spice Symphony Gourmet")))
                .andExpect(jsonPath("$[0].cuisineType", is("North Indian & Mughlai")));
    }

    @Test
    @DisplayName("GET /api/restaurants/1/menu - Fetch menu items for restaurant")
    void testGetRestaurantMenu() throws Exception {
        mockMvc.perform(get("/api/restaurants/1/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Butter Chicken Royale")))
                .andExpect(jsonPath("$[0].prepTimeMinutes", is(18)));
    }

    @Test
    @DisplayName("GET /api/delivery-agents - Fetch delivery agents fleet")
    void testGetAllDeliveryAgents() throws Exception {
        mockMvc.perform(get("/api/delivery-agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Aarav Sharma")))
                .andExpect(jsonPath("$[0].vehicleType", is("Electric Scooter")))
                .andExpect(jsonPath("$[0].status", is("AVAILABLE")));
    }

    @Test
    @DisplayName("POST /api/orders - Successfully place order (Distance: 3.0 km, ETA: 31 min)")
    void testPlaceOrderSuccess() throws Exception {
        OrderPlacementRequest request = OrderPlacementRequest.builder()
                .customerName("Rahul Verma")
                .customerPhone("+91-9876540001")
                .customerAddress("Apartment 402, Lotus Towers")
                .customerLatitude(28.6250)
                .customerLongitude(77.2150)
                .restaurantId(1L)
                .distanceKm(3.0)
                .specialInstructions("Extra spicy, do not ring doorbell")
                .paymentMethod("ONLINE_UPI")
                .items(List.of(
                        new OrderPlacementRequest.ItemOrderDto(1L, 2),
                        new OrderPlacementRequest.ItemOrderDto(5L, 3)
                ))
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId", is(101)))
                .andExpect(jsonPath("$.orderNumber", is("ORD-1763105000000-482")))
                .andExpect(jsonPath("$.status", is("PLACED")))
                .andExpect(jsonPath("$.distanceKm", is(3.0)))
                .andExpect(jsonPath("$.queuePosition", is(1)))
                .andExpect(jsonPath("$.estimatedPrepTimeMinutes", is(18)))
                .andExpect(jsonPath("$.estimatedTravelTimeMinutes", is(13)))
                .andExpect(jsonPath("$.estimatedTotalDeliveryTimeMinutes", is(31)))
                .andExpect(jsonPath("$.totalAmount", is(40.48)));
    }

    @Test
    @DisplayName("POST /api/orders - Validation Failure on Invalid Distance or Empty Items")
    void testPlaceOrderValidationFailure() throws Exception {
        OrderPlacementRequest invalidRequest = OrderPlacementRequest.builder()
                .customerName("") // blank
                .customerPhone("+91-9876540001")
                .customerAddress("Test Address")
                .restaurantId(1L)
                .distanceKm(0.1) // below min 0.5 km
                .items(List.of()) // empty items
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Error")))
                .andExpect(jsonPath("$.details.customerName", notNullValue()))
                .andExpect(jsonPath("$.details.distanceKm", notNullValue()))
                .andExpect(jsonPath("$.details.items", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/deliveries/101/pickup - Confirm Pickup with Valid 6-Digit OTP")
    void testConfirmPickupSuccess() throws Exception {
        OtpVerificationRequest otpRequest = new OtpVerificationRequest("684920", "Picked up packet");

        mockMvc.perform(post("/api/deliveries/101/pickup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.status", is("PICKED_UP")))
                .andExpect(jsonPath("$.deliveryAgentName", is("Aarav Sharma")))
                .andExpect(jsonPath("$.generatedDeliveryOtp", is("512394")));
    }

    @Test
    @DisplayName("POST /api/deliveries/101/pickup - Reject on Invalid Pickup OTP")
    void testConfirmPickupInvalidOtp() throws Exception {
        OtpVerificationRequest otpRequest = new OtpVerificationRequest("999999", "Wrong OTP");

        mockMvc.perform(post("/api/deliveries/101/pickup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Invalid or expired Pickup OTP")));
    }

    @Test
    @DisplayName("POST /api/deliveries/101/complete - Confirm Delivery with Customer OTP")
    void testConfirmDeliverySuccess() throws Exception {
        OtpVerificationRequest otpRequest = new OtpVerificationRequest("512394", "Delivered to customer");

        mockMvc.perform(post("/api/deliveries/101/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.status", is("DELIVERED")))
                .andExpect(jsonPath("$.deliveryAgentName", is("Aarav Sharma")));
    }

    @Test
    @DisplayName("GET /api/deliveries/101/otp - Inspect Active OTPs")
    void testGetActiveOtps() throws Exception {
        mockMvc.perform(get("/api/deliveries/101/otp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", is(101)))
                .andExpect(jsonPath("$.activePickupOtpInRedis", is("684920")))
                .andExpect(jsonPath("$.activeDeliveryOtpInRedis", is("512394")));
    }
}
