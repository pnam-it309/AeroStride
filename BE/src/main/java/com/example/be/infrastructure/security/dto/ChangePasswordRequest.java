package com.example.be.infrastructure.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Yêu cầu đổi mật khẩu của người dùng đang đăng nhập.
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Vui lòng nhập mật khẩu cũ")
    private String matKhauCu;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String matKhauMoi;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu mới")
    private String xacNhanMatKhau;
}
