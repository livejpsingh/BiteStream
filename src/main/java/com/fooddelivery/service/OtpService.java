package com.fooddelivery.service;

import com.fooddelivery.model.enums.OtpType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@Slf4j
public class OtpService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${food-delivery.logistics.pickup-otp-ttl-minutes:15}")
    private int pickupOtpTtlMinutes;

    @Value("${food-delivery.logistics.delivery-otp-ttl-minutes:30}")
    private int deliveryOtpTtlMinutes;

    public OtpService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generates a 6-digit secure numerical OTP
     */
    public String generateSecureOtp() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Stores OTP in Redis with TTL based on OtpType (Pickup vs Delivery)
     */
    public String createAndStoreOtp(Long orderId, OtpType type) {
        String otp = generateSecureOtp();
        String redisKey = buildKey(orderId, type);
        Duration ttl = (type == OtpType.PICKUP)
                ? Duration.ofMinutes(pickupOtpTtlMinutes)
                : Duration.ofMinutes(deliveryOtpTtlMinutes);

        redisTemplate.opsForValue().set(redisKey, otp, ttl);
        log.info("Generated and stored {} OTP for orderId {}: [REDACTED/STORED] with TTL {} min",
                type, orderId, ttl.toMinutes());
        return otp;
    }

    /**
     * Verifies provided OTP against Redis.
     * If valid, deletes key (one-time use guarantee) and returns true.
     */
    public boolean verifyOtp(Long orderId, OtpType type, String providedOtp) {
        if (providedOtp == null || providedOtp.trim().isEmpty()) {
            return false;
        }

        String redisKey = buildKey(orderId, type);
        Object storedVal = redisTemplate.opsForValue().get(redisKey);

        if (storedVal == null) {
            log.warn("OTP verification failed: no active or unexpired {} OTP found for orderId {}", type, orderId);
            return false;
        }

        String storedOtp = storedVal.toString().trim();
        boolean matches = storedOtp.equals(providedOtp.trim());

        if (matches) {
            redisTemplate.delete(redisKey); // Invalidate immediately upon successful verification
            log.info("Successfully verified and invalidated {} OTP for orderId {}", type, orderId);
            return true;
        } else {
            log.warn("Mismatch on {} OTP verification for orderId {}", type, orderId);
            return false;
        }
    }

    /**
     * Inspect active OTP (useful for automated testing / API validation)
     */
    public String getActiveOtp(Long orderId, OtpType type) {
        String redisKey = buildKey(orderId, type);
        Object storedVal = redisTemplate.opsForValue().get(redisKey);
        return storedVal != null ? storedVal.toString() : null;
    }

    private String buildKey(Long orderId, OtpType type) {
        return "food_delivery:otp:" + type.name().toLowerCase() + ":" + orderId;
    }
}
