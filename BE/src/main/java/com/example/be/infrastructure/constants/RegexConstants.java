package com.example.be.infrastructure.constants;

/**
 * Shared regular expression patterns for data validation.
 * Use these constants in @Pattern annotations or manual validation logic
 * to ensure consistency across the AeroStride ecosystem.
 */
public final class RegexConstants {

    private RegexConstants() {
        // Prevent instantiation
    }

    /**
     * Standard email format validation.
     */
    public static final String EMAIL = "^[A-Za-z0-9+_.-]+@(.+)$";

    /**
     * Vietnam phone number validation (10 digits, starts with 03, 05, 07, 08, 09).
     */
    public static final String VIETNAM_PHONE_NUMBER = "^(03|05|07|08|09|01[2|6|8|9])+([0-9]{8})$";

    /**
     * Strong password validation:
     * - Minimum 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one number
     * - At least one special character
     */
    public static final String STRONG_PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
}
