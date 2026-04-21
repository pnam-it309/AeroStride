package com.example.be.infrastructure.notification;

import com.example.be.core.notification.EmailService;
import com.example.be.core.notification.dto.EmailRequest;
import com.example.be.infrastructure.exceptions.EmailProcessingException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async("mailExecutor")
    @Override
    public void sendHtmlEmail(EmailRequest request) {
        try {
            log.info("Starting to send async email to: {}", request.getTo());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, 
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, 
                    StandardCharsets.UTF_8.name()
            );

            Context context = new Context();
            context.setVariables(request.getVariables());

            String html = templateEngine.process("email/" + request.getTemplateName(), context);

            helper.setFrom(fromEmail);
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", request.getTo());
            
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email to: {}. Error: {}", request.getTo(), e.getMessage());
            // Log but do not throw to prevent transaction rollback in caller
        }
    }

    @Async("mailExecutor")
    @Override
    public void sendWelcomeEmail(String to, String name) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("title", "Welcome to AeroStride");
        
        EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Welcome to AeroStride!")
                .templateName("welcome-email")
                .variables(variables)
                .build();
        
        this.sendHtmlEmail(request);
    }

    @Async("mailExecutor")
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("token", token);
        variables.put("title", "Password Reset Request");
        
        EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("AeroStride - Password Reset")
                .templateName("password-reset")
                .variables(variables)
                .build();
        
        this.sendHtmlEmail(request);
    }

    @Async("mailExecutor")
    @Override
    public void guiEmailTaiKhoanKhachHang(String to, String tenKhachHang,
                                          String tenTaiKhoan, String matKhau) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", "Xác nhận tài khoản AeroStride");
        variables.put("tenKhachHang", tenKhachHang);
        variables.put("tenTaiKhoan", tenTaiKhoan);
        variables.put("matKhau", matKhau);

        EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject(" Tài khoản AeroStride của bạn đã được tạo")
                .templateName("tai-khoan-khach-hang")
                .variables(variables)
                .build();

        this.sendHtmlEmail(request);
    }

    @Async("mailExecutor")
    @Override
    public void guiEmailTaiKhoanNhanVien(String to, String tenNhanVien,
                                         String tenTaiKhoan, String matKhau, String vaiTro) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", "Chào mừng nhân viên AeroStride");
        variables.put("tenNhanVien", tenNhanVien);
        variables.put("tenTaiKhoan", tenTaiKhoan);
        variables.put("matKhau", matKhau);
        variables.put("vaiTro", vaiTro != null ? vaiTro : "Nhân viên");

        EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Tài khoản nhân viên AeroStride của bạn đã được tạo")
                .templateName("tai-khoan-nhan-vien")
                .variables(variables)
                .build();

        this.sendHtmlEmail(request);
    }
}
