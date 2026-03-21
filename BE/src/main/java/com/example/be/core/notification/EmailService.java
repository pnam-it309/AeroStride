package com.example.be.core.notification;

import com.example.be.core.notification.dto.EmailRequest;

public interface EmailService {
    
    /**
     * Sends an HTML email based on a Thymeleaf template.
     * @param request The email request containing recipient, subject, template, and variables.
     */
    void sendHtmlEmail(EmailRequest request);

    /**
     * Sends a welcome email to a new user.
     * @param to Recipient email address.
     * @param name User's name.
     */
    void sendWelcomeEmail(String to, String name);

    /**
     * Sends a password reset email.
     * @param to Recipient email address.
     * @param token Reset token.
     */
    void sendPasswordResetEmail(String to, String token);
}
