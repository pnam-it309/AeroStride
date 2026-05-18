package com.example.be.core.admin.nhanvien.service.impl;

import com.example.be.core.admin.nhanvien.repository.AdminNhanVienRepository;
import com.example.be.core.admin.nhanvien.service.ResetPasswordService;
import com.example.be.core.notification.EmailService;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.exceptions.BusinessException;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.utils.AccountUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResetPasswordServiceImpl implements ResetPasswordService {

    private final AdminNhanVienRepository nhanVienRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    @Transactional
    public void requestReset(String email) {
        NhanVien nv = nhanVienRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(MessageConstants.RESET_PASSWORD_NOT_FOUND));

        if (nv.getResetStatus() == NhanVien.ResetStatus.PENDING) {
            throw new BusinessException(MessageConstants.RESET_PASSWORD_ALREADY_PENDING);
        }

        nv.setResetStatus(NhanVien.ResetStatus.PENDING);
        nv.setResetRequestedAt(LocalDateTime.now());
        nhanVienRepository.save(nv);
        log.info("Reset password requested for employee: {}", nv.getMa());
    }

    @Override
    public List<NhanVien> getPendingRequests() {
        return nhanVienRepository.findByResetStatus(NhanVien.ResetStatus.PENDING);
    }

    @Override
    @Transactional
    public void approveReset(String nhanVienId) {
        NhanVien nv = nhanVienRepository.findById(nhanVienId)
                .orElseThrow(() -> new BusinessException(MessageConstants.RESET_PASSWORD_INVALID_REQUEST));

        if (nv.getResetStatus() != NhanVien.ResetStatus.PENDING) {
            throw new BusinessException(MessageConstants.RESET_PASSWORD_INVALID_REQUEST);
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
        log.info("Password reset approved for employee: {}", nv.getMa());
    }
}
