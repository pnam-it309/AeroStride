package com.example.be.infrastructure.constants;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    
    public static final String[] PUBLIC_URLS = {
        RoutesConstant.AUTH + "/**",
        RoutesConstant.RESET_PASSWORD + "/request",
        RoutesConstant.CUSTOMER + "/landing/**",
        RoutesConstant.CUSTOMER + "/san-pham/**",
        RoutesConstant.API_PREFIX + "/storage/proxy",
        "/uploads/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/ws/**",
        "/ws-chat/**",
        RoutesConstant.CUSTOMER_CHAT + "/**",
        "/actuator/**"
    };
}
