package com.example.be.infrastructure.constants;

/**
 * Global application constants for AeroStride project.
 * Uses shared values for pagination, sorting, and other common configurations
 * to avoid hardcoded values across services and repositories.
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // Pagination defaults
    public static final String DEFAULT_PAGE_NUMBER = "1";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIR = "desc";

    // Application common values
    public static final String SYSTEM_USER = "SYSTEM";
}
