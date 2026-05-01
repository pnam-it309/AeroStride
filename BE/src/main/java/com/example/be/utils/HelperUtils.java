package com.example.be.utils;

import java.util.UUID;

public final class HelperUtils {
    private HelperUtils() {}

    /**
     * Generates a random UUID string.
     * @return String representation of a random UUID
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
}
