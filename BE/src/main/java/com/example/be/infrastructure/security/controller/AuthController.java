package com.example.be.infrastructure.security.controller;

import com.example.be.entity.KhachHang;
import com.example.be.entity.NhanVien;
import com.example.be.entity.RefreshToken;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.security.JwtTokenProvider;
import com.example.be.infrastructure.security.dto.AuthResponse;
import com.example.be.infrastructure.security.dto.ChangePasswordRequest;
import com.example.be.infrastructure.security.dto.CurrentUserResponse;
import com.example.be.infrastructure.security.dto.LoginRequest;
import com.example.be.infrastructure.security.dto.RegisterRequest;
import com.example.be.infrastructure.security.dto.TokenRefreshRequest;
import com.example.be.infrastructure.security.service.RefreshTokenService;
import com.example.be.infrastructure.config.ratelimit.RateLimit;
import com.example.be.infrastructure.exceptions.BusinessException;
import com.example.be.repository.KhachHangRepository;
import com.example.be.repository.NhanVienRepository;
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
        log.info("REGISTER ATTEMPT: account [{}], email [{}]", request.getTenTaiKhoan(), request.getEmail());

        if (khachHangRepository.findByTenTaiKhoan(request.getTenTaiKhoan()).isPresent()) {
            throw new RuntimeException("Tên tài khoản đã được sử dụng");
        }
        if (khachHangRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        KhachHang khachHang = KhachHang.builder()
                .tenTaiKhoan(request.getTenTaiKhoan())
                .email(request.getEmail())
                .sdt(request.getSdt())
                .matKhau(passwordEncoder.encode(request.getMatKhau()))
                .xoaMem(false)
                .build();
        khachHang.setTen(request.getTen());
        khachHangRepository.save(khachHang);

        // Tự động đăng nhập sau khi đăng ký thành công (luồng CLIENT)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("CLIENT|" + request.getTenTaiKhoan(), request.getMatKhau()));
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

    /** Lấy nhân viên hiện tại từ SecurityContext hoặc ném lỗi nếu chưa đăng nhập / không phải nhân viên. */
    private NhanVien requireCurrentNhanVien(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException("Bạn chưa đăng nhập");
        }
        return nhanVienRepository.findByTenTaiKhoan(authentication.getName())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin nhân viên"));
    }
}
