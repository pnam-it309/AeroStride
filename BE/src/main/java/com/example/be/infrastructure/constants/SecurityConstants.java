package com.example.be.infrastructure.constants;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    
    public static final String[] PUBLIC_URLS = {
        RoutesConstant.AUTH + "/**",
        RoutesConstant.CUSTOMER + "/landing/**",
        RoutesConstant.CUSTOMER + "/san-pham/**",
        RoutesConstant.CUSTOMER + "/cart/**",
        "/uploads/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/ws/**",
        "/ws-chat/**",
        RoutesConstant.CUSTOMER_CHAT + "/**",
        RoutesConstant.CUSTOMER + "/phieu-giam-gia/**",
        RoutesConstant.CUSTOMER + "/order/**",
        RoutesConstant.CUSTOMER + "/review/**",
        RoutesConstant.CUSTOMER + "/lien-he/**",
        RoutesConstant.CUSTOMER + "/tin-tuc/**",
        RoutesConstant.CUSTOMER + "/gioi-thieu/**",
        RoutesConstant.CUSTOMER_SITEMAP + "/**",
        RoutesConstant.CONFIG + "/**",
        RoutesConstant.ADMIN + "/ghn/**",
        RoutesConstant.PAYMENT + "/**",
        RoutesConstant.CRON + "/**",
        "/actuator/**",
        "/",
        "/error"
    };
}
