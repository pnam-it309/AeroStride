package com.example.be.core.admin.khachhang.service;

import com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest;
import com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminKhachHangService {

    /** Lấy tất cả khách hàng không phân trang (dùng cho export, select). */
    List<AdminKhachHangResponse> hienThi();

    /**
     * Tìm kiếm + lọc + phân trang khách hàng.
     * Gộp 3 method cũ (phanTrang / timKiem / locKH) thành 1.
     * Khi keyword/trangThai/gioiTinh là null → trả toàn bộ.
     */
    Page<AdminKhachHangResponse> search(AdminKhachHangRequest request);

    AdminKhachHangResponse detail(String id);

    AdminKhachHangResponse add(AdminKhachHangRequest request);

    AdminKhachHangResponse update(String id, AdminKhachHangRequest request);

    void doiTrangThai(String id, TrangThai trangThai);

    void delete(String id);

    byte[] exportExcel();
}
