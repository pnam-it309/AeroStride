package com.example.be.infrastructure.bootstrap;

import com.example.be.entity.NhanVien;
import com.example.be.entity.PhanQuyen;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.repository.NhanVienRepository;
import com.example.be.repository.PhanQuyenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DefaultAdminBootstrap {

    private final NhanVienRepository nhanVienRepository;
    private final PhanQuyenRepository phanQuyenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.username:}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.name:Admin He Thong}")
    private String adminName;

    @Value("${app.bootstrap.admin.phone:0123456789}")
    private String adminPhone;

    @Value("${app.bootstrap.admin.birth-date:2002-06-19}")
    private String adminBirthDate;

    @Value("${app.bootstrap.admin.gender:true}")
    private boolean adminGender;

    @Bean
    public ApplicationRunner ensureDefaultAdminRunner() {
        return args -> ensureDefaultAdmin();
    }

    @Transactional
    public void ensureDefaultAdmin() {
        if (!bootstrapEnabled) {
            log.info("Default admin bootstrap is disabled.");
            return;
        }

        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            log.warn("Skipping default admin bootstrap because APP_BOOTSTRAP_ADMIN_EMAIL or APP_BOOTSTRAP_ADMIN_PASSWORD is empty.");
            return;
        }

        PhanQuyen adminRole = phanQuyenRepository.findByMaIgnoreCase("ADMIN")
                .orElseGet(this::createAdminRole);

        Optional<NhanVien> existingAdminOptional = nhanVienRepository.findByEmail(adminEmail);
        NhanVien admin = existingAdminOptional.orElseGet(NhanVien::new);

        if (!existingAdminOptional.isPresent()) {
            String desiredUsername = StringUtils.hasText(adminUsername) ? adminUsername.trim() : adminEmail.trim();
            if (nhanVienRepository.existsByTenTaiKhoan(desiredUsername)) {
                desiredUsername = adminEmail.trim();
            }
            admin.setTenTaiKhoan(desiredUsername);
        }

        admin.setPhanQuyen(adminRole);
        admin.setEmail(adminEmail.trim());
        admin.setTen(normalizeText(adminName, "Admin He Thong"));
        admin.setSdt(normalizeText(adminPhone, null));
        admin.setNgaySinh(parseBirthDate(adminBirthDate));
        admin.setGioiTinh(adminGender);
        admin.setXoaMem(Boolean.FALSE);
        admin.setTrangThai(TrangThai.DANG_HOAT_DONG);

        if (!StringUtils.hasText(admin.getMatKhau()) || !passwordEncoder.matches(adminPassword, admin.getMatKhau())) {
            admin.setMatKhau(passwordEncoder.encode(adminPassword));
        }

        nhanVienRepository.save(admin);
        log.info("Default admin account is ready for email [{}].", adminEmail);
    }

    private PhanQuyen createAdminRole() {
        PhanQuyen role = new PhanQuyen();
        role.setMa("ADMIN");
        role.setTen("Quản trị viên");
        role.setQuyenHan("FULL_ACCESS");
        role.setMoTa("Quyền cao nhất hệ thống");
        role.setTrangThai(TrangThai.DANG_HOAT_DONG);
        return phanQuyenRepository.save(role);
    }

    private LocalDate parseBirthDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private String normalizeText(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }
}
