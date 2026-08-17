package com.example.be.infrastructure.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequest {

    @NotBlank(message = "Nhà cung cấp đăng nhập không được để trống (GOOGLE hoặc FACEBOOK)")
    private String provider; // "GOOGLE" or "FACEBOOK"

    private String token; // Google ID Token / Access Token or Facebook Access Token

    private String email;

    private String name;

    private String avatarUrl;

    private String providerId; // Google sub / Facebook user ID
}
