package com.example.be.infrastructure.security.service;

public interface SecurityService {
    String getCurrentUserId();
    String getCurrentUsername();
    boolean isAuthenticated();
}
