package com.example.be.infrastructure.security.service;

import com.example.be.entity.KhachHang;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.constants.VaiTro;
import com.example.be.repository.KhachHangRepository;
import com.example.be.repository.NhanVienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * OAuth2UserService
 *
 * Handles login via Google/GitHub OAuth2.
 * If the email is not in NhanVien (staff accounts are never auto-created),
 * a KhachHang account with KHACH_HANG role is automatically created.
 */
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email != null) {
            // Staff accounts are never auto-created via OAuth2
            boolean isStaff = nhanVienRepository.findByEmail(email).isPresent();
            if (!isStaff) {
                // Auto-create customer account if not exists
                boolean customerExists = khachHangRepository.existsByEmail(email);
                if (!customerExists) {
                    KhachHang newKhachHang = KhachHang.builder()
                            .tenTaiKhoan(email)
                            .email(email)
                            .matKhau("") // OAuth2 users don't have a password
                            .build();
                    
                    // Set base class fields not available in Lombok @Builder
                    newKhachHang.setTen(name != null ? name : "Khách hàng");
                    newKhachHang.setMa("KH_OA2_" + (System.currentTimeMillis() % 100000));
                    newKhachHang.setTrangThai(TrangThai.DANG_HOAT_DONG);
                    
                    khachHangRepository.save(newKhachHang);
                }
            }
        }

        return oAuth2User;
    }
}
