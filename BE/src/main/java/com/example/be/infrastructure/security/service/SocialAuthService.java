package com.example.be.infrastructure.security.service;

import com.example.be.entity.KhachHang;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.constants.VaiTro;
import com.example.be.infrastructure.exceptions.BusinessException;
import com.example.be.infrastructure.security.JwtTokenProvider;
import com.example.be.infrastructure.security.dto.AuthResponse;
import com.example.be.infrastructure.security.dto.SocialLoginRequest;
import com.example.be.repository.KhachHangRepository;
import com.example.be.repository.NhanVienRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAuthService {

    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public AuthResponse processSocialLogin(SocialLoginRequest request) {
        String provider = request.getProvider() != null ? request.getProvider().toUpperCase().trim() : "";
        if (!"GOOGLE".equals(provider) && !"FACEBOOK".equals(provider)) {
            throw new BusinessException("Phương thức đăng nhập không hợp lệ. Chỉ hỗ trợ GOOGLE hoặc FACEBOOK");
        }

        String email = request.getEmail();
        String name = request.getName();
        String avatarUrl = request.getAvatarUrl();
        String providerId = request.getProviderId();
        String token = request.getToken();

        // 1. Nếu có token, cố gắng verify/parse thêm thông tin chính xác từ provider
        if (token != null && !token.isBlank()) {
            try {
                if ("GOOGLE".equals(provider)) {
                    SocialUserInfo googleInfo = verifyGoogleToken(token);
                    if (googleInfo != null) {
                        if (googleInfo.email != null) email = googleInfo.email;
                        if (googleInfo.name != null) name = googleInfo.name;
                        if (googleInfo.avatarUrl != null) avatarUrl = googleInfo.avatarUrl;
                        if (googleInfo.id != null) providerId = googleInfo.id;
                    }
                } else if ("FACEBOOK".equals(provider)) {
                    SocialUserInfo fbInfo = verifyFacebookToken(token);
                    if (fbInfo != null) {
                        if (fbInfo.email != null) email = fbInfo.email;
                        if (fbInfo.name != null) name = fbInfo.name;
                        if (fbInfo.avatarUrl != null) avatarUrl = fbInfo.avatarUrl;
                        if (fbInfo.id != null) providerId = fbInfo.id;
                    }
                }
            } catch (Exception e) {
                log.warn("Không thể xác thực trực tiếp token với {}: {}. Sử dụng payload gửi lên.", provider, e.getMessage());
            }
        }

        // 2. Chuẩn hóa thông tin
        if ((email == null || email.isBlank()) && (providerId != null && !providerId.isBlank())) {
            email = provider.toLowerCase() + "_" + providerId + "@social.aerostride.vn";
        }

        if (email == null || email.isBlank()) {
            throw new BusinessException("Không lấy được thông tin email từ tài khoản " + provider);
        }

        email = email.trim().toLowerCase();

        if (name == null || name.isBlank()) {
            name = "Khách hàng " + ("GOOGLE".equals(provider) ? "Google" : "Facebook");
        }

        // 3. Kiểm tra xem email có thuộc nhân viên không (nhân viên không được dùng luồng khách hàng)
        if (nhanVienRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Email này thuộc tài khoản nhân viên. Vui lòng đăng nhập tại trang quản trị.");
        }

        // 4. Tìm hoặc tạo mới khách hàng
        KhachHang khachHang = findOrCreateCustomer(email, name, avatarUrl, provider, providerId);

        // 5. Tạo token đăng nhập
        String username = khachHang.getTenTaiKhoan();
        String role = "ROLE_" + VaiTro.KHACH_HANG.name();
        String accessToken = jwtTokenProvider.generateToken(username);
        var refreshToken = refreshTokenService.createRefreshToken(username, role);

        log.info("Social login thành công cho khách hàng: [{}] via [{}]", username, provider);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .username(username)
                .role(role)
                .build();
    }

    private KhachHang findOrCreateCustomer(String email, String name, String avatarUrl, String provider, String providerId) {
        Optional<KhachHang> existingByEmail = khachHangRepository.findFirstByEmailIgnoreCase(email);
        if (existingByEmail.isPresent()) {
            KhachHang kh = existingByEmail.get();
            boolean needSave = false;

            if ((kh.getHinhAnh() == null || kh.getHinhAnh().isBlank()) && avatarUrl != null && !avatarUrl.isBlank()) {
                kh.setHinhAnh(avatarUrl);
                needSave = true;
            }
            if ((kh.getTen() == null || kh.getTen().isBlank()) && name != null && !name.isBlank()) {
                kh.setTen(name);
                needSave = true;
            }
            if (kh.getTrangThai() == null || !TrangThai.DANG_HOAT_DONG.equals(kh.getTrangThai())) {
                kh.setTrangThai(TrangThai.DANG_HOAT_DONG);
                needSave = true;
            }
            if (kh.getXoaMem() != null && kh.getXoaMem()) {
                kh.setXoaMem(false);
                needSave = true;
            }
            if (needSave) {
                kh = khachHangRepository.save(kh);
            }
            log.info("Đã liên kết đăng nhập {} với tài khoản khách hàng có sẵn: username=[{}], email=[{}]", 
                    provider, kh.getTenTaiKhoan(), kh.getEmail());
            return kh;
        }

        // Tạo tên tài khoản độc nhất
        String baseUsername = email.contains("@") ? email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9_]", "") : provider.toLowerCase() + "_" + (providerId != null ? providerId : System.currentTimeMillis());
        if (baseUsername.length() < 3) {
            baseUsername = provider.toLowerCase() + "_" + baseUsername;
        }
        String candidateUsername = baseUsername;
        int counter = 1;
        while (khachHangRepository.findByTenTaiKhoan(candidateUsername).isPresent()) {
            candidateUsername = baseUsername + counter;
            counter++;
        }

        KhachHang newKhachHang = KhachHang.builder()
                .tenTaiKhoan(candidateUsername)
                .email(email)
                .matKhau(passwordEncoder.encode(UUID.randomUUID().toString()))
                .hinhAnh(avatarUrl)
                .xoaMem(false)
                .build();

        newKhachHang.setTen(name);
        newKhachHang.setMa("KH_OA2_" + (System.currentTimeMillis() % 1000000));
        newKhachHang.setTrangThai(TrangThai.DANG_HOAT_DONG);

        return khachHangRepository.save(newKhachHang);
    }

    private SocialUserInfo verifyGoogleToken(String token) {
        // A. Thử parse ID Token dạng JWT nếu có
        try {
            if (token.contains(".")) {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                    JsonNode node = objectMapper.readTree(payloadJson);
                    SocialUserInfo info = new SocialUserInfo();
                    if (node.has("email")) info.email = node.get("email").asText();
                    if (node.has("name")) info.name = node.get("name").asText();
                    if (node.has("picture")) info.avatarUrl = node.get("picture").asText();
                    if (node.has("sub")) info.id = node.get("sub").asText();
                    if (info.email != null) return info;
                }
            }
        } catch (Exception ignored) {}

        // B. Thử gọi Google Tokeninfo API
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + token;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode node = objectMapper.readTree(response.getBody());
                SocialUserInfo info = new SocialUserInfo();
                if (node.has("email")) info.email = node.get("email").asText();
                if (node.has("name")) info.name = node.get("name").asText();
                if (node.has("picture")) info.avatarUrl = node.get("picture").asText();
                if (node.has("sub")) info.id = node.get("sub").asText();
                return info;
            }
        } catch (Exception ignored) {}

        // C. Thử gọi Google UserInfo API (nếu token là OAuth Access Token)
        try {
            String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + token;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode node = objectMapper.readTree(response.getBody());
                SocialUserInfo info = new SocialUserInfo();
                if (node.has("email")) info.email = node.get("email").asText();
                if (node.has("name")) info.name = node.get("name").asText();
                if (node.has("picture")) info.avatarUrl = node.get("picture").asText();
                if (node.has("sub")) info.id = node.get("sub").asText();
                return info;
            }
        } catch (Exception ignored) {}

        return null;
    }

    private SocialUserInfo verifyFacebookToken(String token) {
        try {
            String url = "https://graph.facebook.com/me?fields=id,name,email,picture.type(large)&access_token=" + token;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode node = objectMapper.readTree(response.getBody());
                SocialUserInfo info = new SocialUserInfo();
                if (node.has("email")) info.email = node.get("email").asText();
                if (node.has("name")) info.name = node.get("name").asText();
                if (node.has("id")) info.id = node.get("id").asText();
                if (node.has("picture") && node.get("picture").has("data") && node.get("picture").get("data").has("url")) {
                    info.avatarUrl = node.get("picture").get("data").get("url").asText();
                }
                return info;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static class SocialUserInfo {
        String id;
        String email;
        String name;
        String avatarUrl;
    }
}
