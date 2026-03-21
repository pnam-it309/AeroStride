package com.example.be.infrastructure.constants;

public final class RoutesConstant {
    private RoutesConstant() {}

    public static final String API_PREFIX = "/api/v1";
    
    // Auth routes
    public static final String AUTH = API_PREFIX + "/auth";
    
    // Admin routes
    public static final String ADMIN = API_PREFIX + "/admin";
    
    // Staff routes
    public static final String STAFF = API_PREFIX + "/staff";
    
    // Customer routes (Mobile/Web)
    public static final String CUSTOMER = API_PREFIX + "/customer";
}
