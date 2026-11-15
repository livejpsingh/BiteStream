package com.fooddelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerificationRequest {

    @NotBlank(message = "OTP cannot be blank")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "OTP must be a 4 to 6 digit numerical code")
    private String otp;

    private String remarks;
}
