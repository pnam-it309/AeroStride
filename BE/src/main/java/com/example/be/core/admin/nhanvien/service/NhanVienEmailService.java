package com.example.be.core.admin.nhanvien.service;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
@RequiredArgsConstructor
public class NhanVienEmailService {
    private final SpringTemplateEngine templateEngine;
    private final JavaMailSenderImpl mailSender;

    @Async
    public void guiEmailTaiKhoanNhanVien(String toEmail, String tenNhanVien,
                                         String tenTaiKhoan, String matKhau,
                                         String vaiTro) {
        try {
            Context ctx = new Context();
            ctx.setVariable("title",       "Chào mừng nhân viên AeroStride");
            ctx.setVariable("tenNhanVien", tenNhanVien);
            ctx.setVariable("tenTaiKhoan", tenTaiKhoan);
            ctx.setVariable("matKhau",     matKhau);
            ctx.setVariable("vaiTro",      vaiTro != null ? vaiTro : "Nhân viên");

            String html = templateEngine.process("email/tai-khoan-nhan-vien", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("👋 Tài khoản nhân viên AeroStride của bạn đã được tạo");
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Đã gửi email nhân viên thành công tới: {}", toEmail);

        } catch (Exception e) {
            log.error("Lỗi gửi email nhân viên: {}", e.getMessage());
        }
    }
}
