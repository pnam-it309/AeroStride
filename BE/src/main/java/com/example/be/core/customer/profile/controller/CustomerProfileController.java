package com.example.be.core.customer.profile.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.customer.profile.model.response.CustomerProfileResponse;
import com.example.be.entity.DiaChi;
import com.example.be.entity.KhachHang;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.repository.DiaChiRepository;
import com.example.be.repository.KhachHangRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping(RoutesConstant.CUSTOMER_PROFILE)
@RequiredArgsConstructor
public class CustomerProfileController {

    private final KhachHangRepository khachHangRepository;
    private final DiaChiRepository diaChiRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    @PreAuthorize("hasRole('KHACH_HANG')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getMyProfile(Authentication authentication) {
        String username = authentication.getName();
        KhachHang khachHang = khachHangRepository.findByTenTaiKhoan(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        CustomerProfileResponse.CustomerProfileResponseBuilder builder = CustomerProfileResponse.builder()
                .id(khachHang.getId())
                .tenTaiKhoan(khachHang.getTenTaiKhoan())
                .email(khachHang.getEmail())
                .ten(khachHang.getTen())
                .sdt(khachHang.getSdt())
                .hinhAnh(khachHang.getHinhAnh());

        java.util.List<DiaChi> diaChis = diaChiRepository.findByKhachHangId(khachHang.getId());
        
        Optional<DiaChi> defaultDiaChi = diaChis.stream()
                .filter(dc -> Boolean.TRUE.equals(dc.getLaMacDinh()))
                .findFirst();

        if (defaultDiaChi.isEmpty() && !diaChis.isEmpty()) {
            defaultDiaChi = Optional.of(diaChis.get(0));
        }

        if (defaultDiaChi.isPresent()) {
            DiaChi dc = defaultDiaChi.get();
            builder.diaChiChiTiet(dc.getDiaChiChiTiet())
                   .phuongXa(dc.getPhuongXa())
                   .quanHuyen(dc.getThanhPho())
                   .tinhThanh(dc.getTinh());
            
            if (dc.getTenNguoiNhan() != null) {
                builder.ten(dc.getTenNguoiNhan());
            }
            if (dc.getSdtNguoiNhan() != null) {
                builder.sdt(dc.getSdtNguoiNhan());
            }
        }

        return ResponseEntity.ok(ApiResponse.success(builder.build()));
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('KHACH_HANG')")
    @Transactional
    public ResponseEntity<ApiResponse<String>> updateProfile(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.example.be.core.customer.profile.model.request.CustomerUpdateProfileRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        KhachHang khachHang = khachHangRepository.findByTenTaiKhoan(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        khachHang.setTen(request.getTen());
        khachHang.setSdt(request.getSdt());
        if (request.getHinhAnh() != null && !request.getHinhAnh().isEmpty()) {
            khachHang.setHinhAnh(request.getHinhAnh());
        }
        
        khachHangRepository.save(khachHang);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công"));
    }

    @PutMapping("/change-password")
    @PreAuthorize("hasRole('KHACH_HANG')")
    @Transactional
    public ResponseEntity<ApiResponse<String>> changePassword(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.example.be.core.customer.profile.model.request.CustomerChangePasswordRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        KhachHang khachHang = khachHangRepository.findByTenTaiKhoan(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        if (!passwordEncoder.matches(request.getMatKhauCu(), khachHang.getMatKhau())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác");
        }

        if (!request.getMatKhauMoi().equals(request.getXacNhanMatKhau())) {
            throw new RuntimeException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        khachHang.setMatKhau(passwordEncoder.encode(request.getMatKhauMoi()));
        khachHangRepository.save(khachHang);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công"));
    }
}
