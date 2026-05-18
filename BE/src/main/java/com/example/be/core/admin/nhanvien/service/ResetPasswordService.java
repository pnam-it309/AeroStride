package com.example.be.core.admin.nhanvien.service;

import com.example.be.entity.NhanVien;

import java.util.List;

/**
 * Service xử lý logic nghiệp vụ cho yêu cầu reset mật khẩu nhân viên.
 */
public interface ResetPasswordService {

    /**
     * Nhân viên gửi yêu cầu reset mật khẩu qua email.
     */
    void requestReset(String email);

    /**
     * Lấy danh sách yêu cầu reset đang chờ duyệt.
     */
    List<NhanVien> getPendingRequests();

    /**
     * Admin duyệt yêu cầu reset, tạo mật khẩu mới và gửi email.
     */
    void approveReset(String nhanVienId);
}
