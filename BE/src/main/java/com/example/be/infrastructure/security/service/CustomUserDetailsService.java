package com.example.be.infrastructure.security.service;

import com.example.be.entity.KhachHang;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.constants.VaiTro;
import com.example.be.repository.KhachHangRepository;
import com.example.be.repository.NhanVienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CustomUserDetailsService
 *
 * Authentication flow:
 * 1. First looks up NhanVien (staff/admin) by username → ROLE_NHAN_VIEN or ROLE_QUAN_TRI_VIEN
 * 2. If not found, looks up KhachHang (customer) by username → ROLE_KHACH_HANG
 *
 * This allows both tables to share the same login endpoint.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // 1. Check staff/admin table first (CMS users)
        NhanVien nhanVien = nhanVienRepository.findByTenTaiKhoanOrEmail(identifier, identifier).orElse(null);
        if (nhanVien != null) {
            VaiTro role = VaiTro.NHAN_VIEN;
            // ma_phan_quyen = 'ADMIN' in SQL mapping to getMa() in Java
            if (nhanVien.getPhanQuyen() != null && "ADMIN".equals(nhanVien.getPhanQuyen().getMa())) {
                role = VaiTro.QUAN_TRI_VIEN;
            }
            return buildUserDetails(
                    nhanVien.getTenTaiKhoan(),
                    nhanVien.getMatKhau(),
                    role
            );
        }

        // 2. Fall back to customer table (web users)
        KhachHang khachHang = khachHangRepository.findByTenTaiKhoanOrEmail(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy tài khoản: " + identifier));

        return buildUserDetails(
                khachHang.getTenTaiKhoan(),
                khachHang.getMatKhau(),
                VaiTro.KHACH_HANG
        );
    }

    private UserDetails buildUserDetails(String username, String password, VaiTro vaiTro) {
        String role = vaiTro != null ? vaiTro.name() : VaiTro.KHACH_HANG.name();
        return org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password(password != null ? password : "")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role)))
                .build();
    }
}
