package com.example.be.core.admin.nhanvien.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.notification.EmailService;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.repository.NhanVienRepository;
import com.example.be.utils.AccountUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(RoutesConstant.RESET_PASSWORD)
@RequiredArgsConstructor
public class ResetPasswordRequestController {

    private final NhanVienRepository nhanVienRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @PostMapping(RoutesConstant.REQUEST)
    public ResponseEntity<?> requestReset(@RequestParam String email) {
        NhanVien nv = nhanVienRepository.findByEmail(email).orElse(null);
        if (nv == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Không tìm thấy nhân viên với email này"));
        }
        if (nv.getResetStatus() == NhanVien.ResetStatus.PENDING) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Đã có yêu cầu đang chờ xử lý"));
        }
        nv.setResetStatus(NhanVien.ResetStatus.PENDING);
        nv.setResetRequestedAt(LocalDateTime.now());
        nhanVienRepository.save(nv);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã gửi yêu cầu reset mật khẩu tới admin"));
    }

    @GetMapping(RoutesConstant.PENDING)
    @PreAuthorize("hasRole('QUAN_TRI_VIEN')")
    public ResponseEntity<?> getPendingRequests() {
        List<NhanVien> list = nhanVienRepository.findByResetStatus(NhanVien.ResetStatus.PENDING);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping(RoutesConstant.APPROVE)
    @PreAuthorize("hasRole('QUAN_TRI_VIEN')")
    public ResponseEntity<?> approveReset(@PathVariable String id) {
        NhanVien nv = nhanVienRepository.findById(id).orElse(null);
        if (nv == null || nv.getResetStatus() != NhanVien.ResetStatus.PENDING) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Yêu cầu không hợp lệ hoặc đã xử lý"));
        }

        String newPassword = AccountUtils.taoMatKhauNgauNhien(10);
        nv.setMatKhau(passwordEncoder.encode(newPassword));
        nv.setResetStatus(null);
        nv.setResetRequestedAt(null);
        nhanVienRepository.save(nv);

        String tenVaiTro = nv.getPhanQuyen() != null ? nv.getPhanQuyen().getTen() : "Nhân viên";
        emailService.guiEmailTaiKhoanNhanVien(
                nv.getEmail(), nv.getTen(), nv.getTenTaiKhoan(), newPassword, tenVaiTro
        );
        return ResponseEntity.ok(ApiResponse.success(null, "Đã reset mật khẩu và gửi email cho nhân viên"));
    }
}
