package com.example.be.core.admin.nhanvien.controller;

import com.example.be.core.admin.nhanvien.model.request.ResetPasswordRequest;
import com.example.be.core.admin.nhanvien.repository.ResetPasswordRequestRepository;
import com.example.be.core.common.dto.ApiResponse;

;import com.example.be.entity.NhanVien;
import com.example.be.repository.NhanVienRepository;
import com.example.be.core.admin.nhanvien.service.NhanVienEmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reset-password-requests")
@RequiredArgsConstructor
public class ResetPasswordRequestController {

    private final NhanVienRepository nhanVienRepository;
    private final PasswordEncoder passwordEncoder;
    private final NhanVienEmailService nhanVienEmailService;

    @PostMapping("/request")
    public ResponseEntity<?> requestReset(@RequestParam String email) {
        NhanVien nv = nhanVienRepository.findByEmail(email).orElse(null);
        if (nv == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Không tìm thấy nhân viên với email này")); // ✅
        }
        if (nv.getResetStatus() == NhanVien.ResetStatus.PENDING) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Đã có yêu cầu đang chờ xử lý")); // ✅
        }
        nv.setResetStatus(NhanVien.ResetStatus.PENDING);
        nv.setResetRequestedAt(LocalDateTime.now());
        nhanVienRepository.save(nv);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã gửi yêu cầu reset mật khẩu tới admin")); // ✅
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests() {
        List<NhanVien> list = nhanVienRepository.findByResetStatus(NhanVien.ResetStatus.PENDING);
        return ResponseEntity.ok(ApiResponse.success(list)); // ✅
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<?> approveReset(@PathVariable String id) {
        NhanVien nv = nhanVienRepository.findById(id).orElse(null);
        if (nv == null || nv.getResetStatus() != NhanVien.ResetStatus.PENDING) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Yêu cầu không hợp lệ hoặc đã xử lý")); // ✅
        }
        String newPassword = taoMatKhauTam();
        nv.setMatKhau(passwordEncoder.encode(newPassword));
        nv.setResetStatus(null);
        nv.setResetRequestedAt(null);
        nhanVienRepository.save(nv);

        nhanVienEmailService.guiEmailTaiKhoanNhanVien(
            nv.getEmail(),
            nv.getTen(),
            nv.getTenTaiKhoan(),
            newPassword,
            nv.getPhanQuyen() != null ? nv.getPhanQuyen().getTen() : "Nhân viên"
        );
        return ResponseEntity.ok(ApiResponse.success(null, "Đã reset mật khẩu và gửi email cho nhân viên")); // ✅
    }

    private String taoMatKhauTam() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#";
        StringBuilder sb = new StringBuilder();
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}
