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

    /**
     * Sends account credentials email to a newly created KhachHang.
     * @param to          Recipient email.
     * @param tenKhachHang Customer's display name.
     * @param tenTaiKhoan  Generated username.
     * @param matKhau      Plain-text temporary password.
     */
    void guiEmailTaiKhoanKhachHang(String to, String tenKhachHang,
                                   String tenTaiKhoan, String matKhau);

    /**
     * Sends account credentials email to a newly created NhanVien.
     * @param to          Recipient email.
     * @param tenNhanVien Employee's display name.
     * @param tenTaiKhoan Generated username.
     * @param matKhau     Plain-text temporary password.
     * @param vaiTro      Role/position label.
     */
    void guiEmailTaiKhoanNhanVien(String to, String tenNhanVien,
                                  String tenTaiKhoan, String matKhau, String vaiTro);
}
