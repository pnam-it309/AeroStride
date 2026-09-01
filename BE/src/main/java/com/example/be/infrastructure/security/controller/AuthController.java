package com.example.be.infrastructure.security.controller;

import com.example.be.entity.KhachHang;
import com.example.be.entity.NhanVien;
import com.example.be.entity.RefreshToken;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.security.JwtTokenProvider;
import com.example.be.infrastructure.security.dto.AuthResponse;
import com.example.be.infrastructure.security.dto.ChangePasswordRequest;
import com.example.be.infrastructure.security.dto.CurrentUserResponse;
import com.example.be.infrastructure.security.dto.UpdateProfileRequest;
import com.example.be.infrastructure.security.dto.LoginRequest;
import com.example.be.infrastructure.security.dto.RegisterRequest;
import com.example.be.infrastructure.security.dto.TokenRefreshRequest;
import com.example.be.infrastructure.security.dto.SocialLoginRequest;
import com.example.be.infrastructure.security.service.SocialAuthService;
import com.example.be.infrastructure.security.service.RefreshTokenService;
import com.example.be.infrastructure.config.ratelimit.RateLimit;
import com.example.be.infrastructure.exceptions.BusinessException;
import com.example.be.infrastructure.exceptions.UnauthorizedException;
import com.example.be.repository.KhachHangRepository;
import com.example.be.repository.NhanVienRepository;
import com.example.be.utils.CodeUtils;
import com.example.be.core.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoutesConstant.AUTH)
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final PasswordEncoder passwordEncoder;
    private final SocialAuthService socialAuthService;

    @PostMapping("/social-login")
    @RateLimit(limit = 10, windowSeconds = 60)
    public ResponseEntity<ApiResponse<AuthResponse>> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        log.info("SOCIAL LOGIN ATTEMPT: Provider [{}] Email [{}]", request.getProvider(), request.getEmail());
        AuthResponse authResponse = socialAuthService.processSocialLogin(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Đăng nhập bằng " + request.getProvider() + " thành công"));
    }

    @PostMapping("/login")
    @RateLimit(limit = 5, windowSeconds = 60)
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("LOGIN ATTEMPT: User [{}] Type [{}]", loginRequest.getUsername(), loginRequest.getLoginType());

        String identifier = loginRequest.getUsername();
        if (loginRequest.getLoginType() != null && !loginRequest.getLoginType().isBlank()) {
            identifier = loginRequest.getLoginType().toUpperCase() + "|" + identifier;
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername(), role);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken.getToken())
                .username(userDetails.getUsername())
                .role(role)
                .build();

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Đăng nhập thành công"));
    }

    @PostMapping("/register")
    @RateLimit(limit = 5, windowSeconds = 60)
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        String username = request.getTenTaiKhoan() != null ? request.getTenTaiKhoan().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String sdt = request.getSdt() != null ? request.getSdt().trim() : "";

        log.info("REGISTER ATTEMPT: account [{}], email [{}]", username, email);

        if (khachHangRepository.existsByTenTaiKhoanIgnoreCase(username) || nhanVienRepository.findByTenTaiKhoan(username).isPresent()) {
            throw new BusinessException("Tên tài khoản '" + username + "' đã được sử dụng. Vui lòng chọn tên tài khoản khác.");
        }
        if (khachHangRepository.existsByEmailIgnoreCase(email) || nhanVienRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Email '" + email + "' đã tồn tại trong hệ thống. Vui lòng đăng nhập hoặc sử dụng chức năng Đăng nhập bằng Google / Quên mật khẩu.");
        }
        if (!sdt.isBlank() && khachHangRepository.findFirstBySdt(sdt).isPresent()) {
            throw new BusinessException("Số điện thoại '" + sdt + "' đã được sử dụng cho một tài khoản khác.");
        }

        KhachHang khachHang = KhachHang.builder()
                .tenTaiKhoan(username)
                .email(email)
                .sdt(sdt)
                .matKhau(passwordEncoder.encode(request.getMatKhau()))
                .ngaySinh(request.getNgaySinh())
                .gioiTinh(request.getGioiTinh() != null ? request.getGioiTinh() : true)
                .xoaMem(false)
                .build();
        khachHang.setTen(request.getTen());
        khachHang.setMa(CodeUtils.generateRandom(KhachHang.class, khachHangRepository::existsByMa));
        khachHangRepository.save(khachHang);

        // Tự động đăng nhập sau khi đăng ký thành công (luồng CLIENT)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("CLIENT|" + username, request.getMatKhau()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        String jwt = jwtTokenProvider.generateToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername(), role);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken.getToken())
                .username(userDetails.getUsername())
                .role(role)
                .build();

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Đăng ký thành công"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(token -> {
                    String username = refreshTokenService.getUsernameFromToken(token);
                    String newAccessToken = jwtTokenProvider.generateToken(username);
                    AuthResponse authResponse = AuthResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(request.getRefreshToken())
                            .username(username)
                            .build();
                    return ResponseEntity.ok(ApiResponse.success(authResponse));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        if (authentication != null) {
            refreshTokenService.deleteByUsername(authentication.getName());
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    /** Thông tin nhân viên đang đăng nhập (cho header admin & trang hồ sơ). */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser(Authentication authentication) {
        NhanVien nv = requireCurrentNhanVien(authentication);

        String role = authentication.getAuthorities().isEmpty()
                ? null
                : authentication.getAuthorities().iterator().next().getAuthority();

        CurrentUserResponse response = CurrentUserResponse.builder()
                .id(nv.getId())
                .tenTaiKhoan(nv.getTenTaiKhoan())
                .ten(nv.getTen())
                .chucVu(nv.getPhanQuyen() != null ? nv.getPhanQuyen().getTen() : null)
                .role(role)
                .ma(nv.getMa())
                .email(nv.getEmail())
                .sdt(nv.getSdt())
                .hinhAnh(nv.getHinhAnh())
                .gioiTinh(nv.getGioiTinh())
                .ngaySinh(nv.getNgaySinh() != null ? nv.getNgaySinh().toString() : null)
                .diaChiChiTiet(nv.getDiaChiChiTiet())
                .phuongXa(nv.getPhuongXa())
                .thanhPho(nv.getThanhPho())
                .tinh(nv.getTinh())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** Đổi mật khẩu của nhân viên đang đăng nhập. */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        NhanVien nv = requireCurrentNhanVien(authentication);

        if (!passwordEncoder.matches(request.getMatKhauCu(), nv.getMatKhau())) {
            throw new BusinessException("Mật khẩu cũ không chính xác");
        }
        if (!request.getMatKhauMoi().equals(request.getXacNhanMatKhau())) {
            throw new BusinessException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        nv.setMatKhau(passwordEncoder.encode(request.getMatKhauMoi()));
        nhanVienRepository.save(nv);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công"));
    }

    /** Cập nhật thông tin cá nhân của nhân viên đang đăng nhập. */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> updateProfile(
            @RequestBody UpdateProfileRequest request, Authentication authentication) {
        NhanVien nv = requireCurrentNhanVien(authentication);

        if (request.getTen() != null && !request.getTen().isBlank()) {
            nv.setTen(request.getTen().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String email = request.getEmail().trim();
            if (nhanVienRepository.existsByEmailAndIdNot(email, nv.getId())) {
                throw new BusinessException("Email đã được sử dụng bởi tài khoản khác");
            }
            nv.setEmail(email);
        }
        if (request.getSdt() != null && !request.getSdt().isBlank()) {
            String sdt = request.getSdt().trim();
            if (nhanVienRepository.existsBySdtAndIdNot(sdt, nv.getId())) {
                throw new BusinessException("Số điện thoại đã được sử dụng bởi tài khoản khác");
            }
            nv.setSdt(sdt);
        }
        if (request.getGioiTinh() != null) {
            nv.setGioiTinh(request.getGioiTinh());
        }
        if (request.getNgaySinh() != null) {
            nv.setNgaySinh(request.getNgaySinh());
        }
        if (request.getDiaChiChiTiet() != null) {
            nv.setDiaChiChiTiet(request.getDiaChiChiTiet().trim());
        }
        if (request.getPhuongXa() != null) {
            nv.setPhuongXa(request.getPhuongXa().trim());
        }
        if (request.getThanhPho() != null) {
            nv.setThanhPho(request.getThanhPho().trim());
        }
        if (request.getTinh() != null) {
            nv.setTinh(request.getTinh().trim());
        }
        if (request.getHinhAnh() != null) {
            nv.setHinhAnh(request.getHinhAnh());
        }

        NhanVien updated = nhanVienRepository.save(nv);

        String role = authentication.getAuthorities().isEmpty()
                ? null
                : authentication.getAuthorities().iterator().next().getAuthority();

        CurrentUserResponse response = CurrentUserResponse.builder()
                .id(updated.getId())
                .tenTaiKhoan(updated.getTenTaiKhoan())
                .ten(updated.getTen())
                .chucVu(updated.getPhanQuyen() != null ? updated.getPhanQuyen().getTen() : null)
                .role(role)
                .ma(updated.getMa())
                .email(updated.getEmail())
                .sdt(updated.getSdt())
                .hinhAnh(updated.getHinhAnh())
                .gioiTinh(updated.getGioiTinh())
                .ngaySinh(updated.getNgaySinh() != null ? updated.getNgaySinh().toString() : null)
                .diaChiChiTiet(updated.getDiaChiChiTiet())
                .phuongXa(updated.getPhuongXa())
                .thanhPho(updated.getThanhPho())
                .tinh(updated.getTinh())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thông tin thành công"));
    }

    /** Lấy nhân viên hiện tại từ SecurityContext hoặc ném lỗi nếu chưa đăng nhập / không phải nhân viên. */
    private String normalizeAuthenticationName(String authenticationName) {
        if (authenticationName == null) {
            return "";
        }
        int separatorIndex = authenticationName.indexOf('|');
        if (separatorIndex >= 0 && separatorIndex + 1 < authenticationName.length()) {
            return authenticationName.substring(separatorIndex + 1);
        }
        return authenticationName;
    }

    private NhanVien requireCurrentNhanVien(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Bạn chưa đăng nhập");
        }
        String identifier = normalizeAuthenticationName(authentication.getName());
        return nhanVienRepository.findCurrentProfileByIdentifier(identifier)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy thông tin nhân viên"));
    }
}
