package com.example.be.core.admin.khachhang.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async
    public void guiEmailTaiKhoan(String toEmail, String tenKhachHang,
                                 String tenTaiKhoan, String matKhau) {
        try {
            Context ctx = new Context();
            // Bổ sung title cho base-template
            ctx.setVariable("title", "Xác nhận tài khoản AeroStride");

            ctx.setVariable("tenKhachHang", tenKhachHang);
            ctx.setVariable("tenTaiKhoan",  tenTaiKhoan);
            ctx.setVariable("matKhau",      matKhau);

            // Thymeleaf sẽ xử lý file con, file con tự kéo file base theo
            String html = templateEngine.process("email/tai-khoan-khach-hang", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("🎉 Tài khoản AeroStride của bạn đã được tạo");
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Đã gửi email thành công tới: {}", toEmail);

        } catch (Exception e) {
            log.error("Lỗi gửi email: {}", e.getMessage());
        }
    }
}
