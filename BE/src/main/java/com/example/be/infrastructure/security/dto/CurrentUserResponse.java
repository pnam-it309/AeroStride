package com.example.be.infrastructure.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thông tin người dùng đang đăng nhập (nhân viên) cho header admin & trang hồ sơ.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurrentUserResponse {
    private String id;
    private String tenTaiKhoan;
    private String ten;        // họ tên hiển thị
    private String chucVu;     // tên phân quyền (Quản trị viên / Nhân viên ...)
    private String role;       // authority thô: ROLE_QUAN_TRI_VIEN ...
    private String ma;         // mã nhân viên
    private String email;
    private String sdt;
    private String hinhAnh;
    private Boolean gioiTinh;
    private String ngaySinh;   // ISO yyyy-MM-dd
    private String diaChiChiTiet;
    private String phuongXa;
    private String thanhPho;
    private String tinh;
}
