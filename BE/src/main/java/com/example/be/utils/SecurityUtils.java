package com.example.be.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Utility class for Spring Security operations.
 * Provides safe and centralized access to the current authenticated user's information.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Prevent instantiation
    }

    /**
     * Extracts the email (username) of the currently authenticated user.
     *
     * @return Optional containing email if user is authenticated, otherwise empty.
     */
    public static Optional<String> getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return Optional.of(((UserDetails) principal).getUsername());
        } else if (principal instanceof String) {
            return Optional.of((String) principal);
        }

        return Optional.empty();
    }

    /**
     * Extracts the ID of the currently authenticated user.
     * Note: This assumes the UserDetails implementation or Token contains the ID.
     * If using a custom UserDetails (e.g., UserPrincipal), cast and return ID.
     *
     * @return Optional containing ID if available, otherwise empty.
     */
    public static Optional<String> getCurrentUserId() {
        // Implementation depends on how ID is stored in Authentication principle
        // Commonly, we cast principal to a custom class (e.g. UserPrincipal) that has getId()
        // For now, we return empty as a placeholder - customize based on your UserDetails implementation
        return Optional.empty();
    }
}
