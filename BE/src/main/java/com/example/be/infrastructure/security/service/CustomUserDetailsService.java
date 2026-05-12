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
        String tempLoginType = null;
        String tempRealIdentifier = identifier;

        // Phân tách prefix nếu có (định dạng: TYPE|IDENTIFIER)
        if (identifier != null && identifier.contains("|")) {
            String[] parts = identifier.split("\\|", 2);
            if (parts.length == 2) {
                tempLoginType = parts[0];
                tempRealIdentifier = parts[1];
            }
        }

        final String loginType = tempLoginType;
        final String realIdentifier = tempRealIdentifier;

        // Luồng đăng nhập cho ADMIN/STAFF
        if (loginType == null || "ADMIN".equals(loginType)) {
            NhanVien nhanVien = nhanVienRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(realIdentifier, realIdentifier, realIdentifier, realIdentifier)
                    .orElse(null);

            if (nhanVien != null) {
                VaiTro role = VaiTro.NHAN_VIEN;
                if (nhanVien.getPhanQuyen() != null && "ADMIN".equals(nhanVien.getPhanQuyen().getMa())) {
                    role = VaiTro.QUAN_TRI_VIEN;
                }
                return buildUserDetails(nhanVien.getTenTaiKhoan(), nhanVien.getMatKhau(), role);
            }
            
            // Nếu yêu cầu đích danh ADMIN mà không thấy thì throw luôn
            if ("ADMIN".equals(loginType)) {
                throw new UsernameNotFoundException("Không tìm thấy tài khoản quản trị: " + realIdentifier);
            }
        }

        // Luồng đăng nhập cho CLIENT (Khách hàng)
        if (loginType == null || "CLIENT".equals(loginType)) {
            KhachHang khachHang = khachHangRepository.findByTenTaiKhoanOrEmailOrSdtOrMa(realIdentifier, realIdentifier, realIdentifier, realIdentifier)
                    .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản khách hàng: " + realIdentifier));

            return buildUserDetails(khachHang.getTenTaiKhoan(), khachHang.getMatKhau(), VaiTro.KHACH_HANG);
        }

        throw new UsernameNotFoundException("Không tìm thấy tài khoản: " + realIdentifier);
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
