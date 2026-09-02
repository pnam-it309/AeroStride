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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final java.util.concurrent.Executor mailExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.frontend_url}")
    private String frontendUrl;

    @Value("${resend.api_key:}")
    private String resendApiKey;

    @Value("${resend.from:AeroStride <onboarding@resend.dev>}")
    private String resendFrom;

    @Async("mailExecutor")
    @Override
    public void sendHtmlEmail(EmailRequest request) {
        if (request == null || request.getTo() == null || request.getTo().isBlank()) {
            return;
        }
        try {
            log.info("Starting to send async email to: {}", request.getTo());
            
            Map<String, Object> vars = request.getVariables() != null ? new HashMap<>(request.getVariables()) : new HashMap<>();
            vars.putIfAbsent("baseUrl", frontendUrl);
            vars.putIfAbsent("frontendUrl", frontendUrl);

            Context context = new Context();
            context.setVariables(vars);

            String html = templateEngine.process("email/" + request.getTemplateName(), context);

            // 1. Ưu tiên gửi qua Resend HTTP API (cổng 443 HTTPS - hoàn toàn không bị chặn trên Render/Cloud)
            if (resendApiKey != null && !resendApiKey.isBlank()) {
                boolean sent = sendViaResend(request.getTo(), request.getSubject(), html);
                if (sent) {
                    return;
                }
                log.warn("Resend API failed, attempting SMTP fallback...");
            }

            // 2. Fallback sang JavaMailSender (SMTP) nếu Resend chưa cấu hình hoặc thất bại
            if (fromEmail != null && !fromEmail.isBlank()) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(
                        message, 
                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, 
                        StandardCharsets.UTF_8.name()
                );

                try {
                    helper.setFrom(fromEmail, "AeroStride");
                } catch (Exception e) {
                    helper.setFrom(fromEmail);
                }
                helper.setTo(request.getTo());
                helper.setSubject(request.getSubject());
                helper.setText(html, true);

                mailSender.send(message);
                log.info("Email sent successfully via SMTP to: {}", request.getTo());
            } else {
                log.warn("Không thể gửi email đến {}: Chưa cấu hình RESEND_API_KEY hoặc MAIL_USERNAME.", request.getTo());
            }
            
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email to: {}. Error: {}", request.getTo(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during async email sending to: {}. Error: {}", request.getTo(), e.getMessage());
        }
    }

    private boolean sendViaResend(String to, String subject, String html) {
        try {
            log.info("Sending email to {} via Resend HTTP API...", to);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("from", resendFrom != null && !resendFrom.isBlank() ? resendFrom : "AeroStride <onboarding@resend.dev>");
            payload.put("to", List.of(to));
            payload.put("subject", subject);
            payload.put("html", html);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "AeroStride-Backend")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email sent successfully via Resend to: {}, Response: {}", to, response.body());
                return true;
            } else {
                if (response.statusCode() == 403 && response.body().contains("testing emails")) {
                    log.error("RESEND CHẶN GỬI (403): Domain 'onboarding@resend.dev' CHỈ cho phép gửi email đến đúng địa chỉ email bạn dùng đăng ký tài khoản Resend. Để gửi đến '{}', bạn cần thêm và xác thực domain riêng tại https://resend.com/domains và đặt biến RESEND_FROM=AeroStride <no-reply@yourdomain.com>.", to);
                } else {
                    log.error("Resend API returned error {}: {}", response.statusCode(), response.body());
                }
                return false;
            }
        } catch (Exception e) {
            log.error("Exception during Resend API email sending to {}: {}", to, e.getMessage());
            return false;
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

    @Async("mailExecutor")
    @Override
    public void guiEmailCapNhatTrangThaiHoaDon(String to, String tenKhachHang, String maHoaDon,
                                               String trangThaiLabel, String ghiChu) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", "Cập nhật trạng thái đơn hàng");
        variables.put("tenKhachHang", tenKhachHang);
        variables.put("maHoaDon", maHoaDon);
        variables.put("trangThaiLabel", trangThaiLabel);
        variables.put("ghiChu", ghiChu);

        EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("AeroStride - Cập nhật đơn hàng #" + maHoaDon + ": " + trangThaiLabel)
                .templateName("order-status-update")
                .variables(variables)
                .build();

        this.sendHtmlEmail(request);
    }

    @Async("mailExecutor")
    @Override
    public void guiEmailXacNhanDatHang(String to, String tenNguoiNhan, String maHoaDon, 
                                        String soDienThoai, String diaChi, String phuongThuc,
                                        java.math.BigDecimal tamTinh, java.math.BigDecimal phiVanChuyen,
                                        java.math.BigDecimal tienGiam, java.math.BigDecimal tongTien,
                                        java.util.List<java.util.Map<String, Object>> items) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", "Xác nhận đặt hàng thành công #" + maHoaDon);
        variables.put("tenNguoiNhan", tenNguoiNhan);
        variables.put("orderCode", maHoaDon);
        variables.put("soDienThoai", soDienThoai);
        variables.put("diaChi", diaChi);
        variables.put("phuongThuc", phuongThuc);
        variables.put("tamtinh", tamTinh);
        variables.put("phiVanChuyen", phiVanChuyen);
        variables.put("tienGiam", tienGiam);
        variables.put("tongTienSauGiam", tongTien);
        variables.put("items", items);

        EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("🎉 AeroStride - Xác nhận đơn hàng #" + maHoaDon + " thành công")
                .templateName("order-confirmation")
                .variables(variables)
                .build();

        this.sendHtmlEmail(request);
    }

    @Async("mailExecutor")
    @Override
    public void guiEmailVanChuyen(String to, String tenKhachHang, String maHoaDon,
                                   String maVanDon, String donViVanChuyen, Long ngayGiaoDuKien, String linkTraCuu) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", "Đơn hàng đang được giao");
        variables.put("tenKhachHang", tenKhachHang);
        variables.put("maHoaDon", maHoaDon);
        variables.put("maVanDon", maVanDon != null ? maVanDon : "Chờ cập nhật");
        variables.put("donViVanChuyen", donViVanChuyen != null ? donViVanChuyen : "Giao Hàng Nhanh (GHN)");
        variables.put("ngayGiaoDuKien", ngayGiaoDuKien);
        variables.put("linkTraCuu", linkTraCuu != null ? linkTraCuu : (frontendUrl + "/tra-cuu?code=" + (maHoaDon != null ? maHoaDon : "")));

        EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("🚚 AeroStride - Đơn hàng #" + maHoaDon + " đang trên đường giao đến bạn!")
                .templateName("shipping-email")
                .variables(variables)
                .build();

        this.sendHtmlEmail(request);
    }

    @Async("mailExecutor")
    @Override
    public void guiEmailChucMungSinhNhat(String to, String tenKhachHang, String maVoucher,
                                         String tenVoucher, java.math.BigDecimal giaTriGiam, String loaiGiam, Long ngayHetHan) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", "Chúc Mừng Sinh Nhật Quý Khách!");
        variables.put("tenKhachHang", tenKhachHang);
        variables.put("maVoucher", maVoucher);
        variables.put("tenVoucher", tenVoucher);
        variables.put("giaTriGiam", giaTriGiam);
        variables.put("loaiGiam", loaiGiam);
        variables.put("ngayHetHan", ngayHetHan);

        EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("🎂 Happy Birthday! AeroStride tặng bạn món quà sinh nhật đặc biệt 🎁")
                .templateName("birthday-email")
                .variables(variables)
                .build();

        this.sendHtmlEmail(request);
    }
}
