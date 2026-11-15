package com.fooddelivery.service;

import com.fooddelivery.model.enums.OtpType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OtpServiceTest {

    @Test
    @DisplayName("Generate secure 6-digit numeric OTP")
    void testGenerateSecureOtp() {
        OtpService otpService = new OtpService(null);
        for (int i = 0; i < 50; i++) {
            String otp = otpService.generateSecureOtp();
            assertNotNull(otp);
            assertEquals(6, otp.length());
            assertTrue(otp.matches("^[0-9]{6}$"), "OTP should be numeric 6 digits, got: " + otp);
            int code = Integer.parseInt(otp);
            assertTrue(code >= 100000 && code <= 999999);
        }
    }

    @Test
    @DisplayName("Verify empty or null OTP input rejection")
    void testVerifyEmptyOtp() {
        OtpService otpService = new OtpService(null);
        assertFalse(otpService.verifyOtp(101L, OtpType.PICKUP, null));
        assertFalse(otpService.verifyOtp(101L, OtpType.PICKUP, ""));
        assertFalse(otpService.verifyOtp(101L, OtpType.PICKUP, "   "));
        assertFalse(otpService.verifyOtp(101L, OtpType.DELIVERY, null));
    }
}
