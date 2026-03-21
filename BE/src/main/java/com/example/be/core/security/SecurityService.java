package com.example.be.core.security;

public interface SecurityService {
    /**
     * Deletes expired refresh tokens and OTPs from the database.
     */
    void cleanupExpiredTokens();
}
