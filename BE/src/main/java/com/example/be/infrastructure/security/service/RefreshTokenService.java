package com.example.be.infrastructure.security.service;

import com.example.be.entity.KhachHang;
import com.example.be.entity.RefreshToken;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.constants.VaiTro;
import com.example.be.repository.KhachHangRepository;
import com.example.be.repository.NhanVienRepository;
import com.example.be.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * RefreshTokenService
 *
 * Manages JWT refresh tokens for both KhachHang and NhanVien.
 * The token is associated with the correct table based on who is logging in.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh_expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Creates a refresh token for the given username.
     * Looks up NhanVien first, then KhachHang.
     */
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        RefreshToken.RefreshTokenBuilder builder = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        // Check staff/admin table first
        NhanVien nhanVien = nhanVienRepository.findByTenTaiKhoan(username).orElse(null);
        if (nhanVien != null) {
            // Delete old tokens for this user first
            refreshTokenRepository.deleteByNhanVienId(nhanVien.getId());
            return refreshTokenRepository.save(builder.nhanVien(nhanVien).build());
        }

        // Fall back to customer
        KhachHang khachHang = khachHangRepository.findByTenTaiKhoan(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + username));

        refreshTokenRepository.deleteByKhachHangId(khachHang.getId());
        return refreshTokenRepository.save(builder.khachHang(khachHang).build());
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token hết hạn. Vui lòng đăng nhập lại.");
        }
        return token;
    }

    /**
     * Returns the username (tenTaiKhoan) from either KhachHang or NhanVien linked token.
     */
    public String getUsernameFromToken(RefreshToken token) {
        if (token.getNhanVien() != null) {
            return token.getNhanVien().getTenTaiKhoan();
        }
        if (token.getKhachHang() != null) {
            return token.getKhachHang().getTenTaiKhoan();
        }
        throw new RuntimeException("Token không liên kết với tài khoản nào.");
    }

    /**
     * Returns the VaiTro from either KhachHang or NhanVien linked token.
     */
    public VaiTro getVaiTroFromToken(RefreshToken token) {
        if (token.getNhanVien() != null) {
            NhanVien nv = token.getNhanVien();
            if (nv.getPhanQuyen() != null && "ADMIN".equals(nv.getPhanQuyen().getMa())) {
                return VaiTro.QUAN_TRI_VIEN;
            }
            return VaiTro.NHAN_VIEN;
        }
        if (token.getKhachHang() != null) {
            return VaiTro.KHACH_HANG;
        }
        return VaiTro.KHACH_HANG;
    }

    @Transactional
    public void deleteByUsername(String username) {
        nhanVienRepository.findByTenTaiKhoan(username)
                .ifPresentOrElse(
                        nv -> refreshTokenRepository.deleteByNhanVienId(nv.getId()),
                        () -> khachHangRepository.findByTenTaiKhoan(username)
                                .ifPresent(kh -> refreshTokenRepository.deleteByKhachHangId(kh.getId()))
                );
    }
}
