package com.example.be.core.admin.nhanvien.service;

import com.example.be.core.admin.nhanvien.model.request.AdminNhanVienRequest;
import com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminNhanVienService {

    /** Lấy tất cả nhân viên không phân trang (dùng cho export, select). */
    List<AdminNhanVienResponse> hienThi();

    /**
     * Tìm kiếm + lọc + phân trang nhân viên.
     * Gộp 3 method cũ (phanTrang / timKiem / locNV) thành 1.
     * Khi keyword/trangThai/gioiTinh là null → trả toàn bộ.
     */
    Page<AdminNhanVienResponse> search(AdminNhanVienRequest request);

    AdminNhanVienResponse detail(String id);

    AdminNhanVienResponse add(AdminNhanVienRequest request);

    AdminNhanVienResponse update(String id, AdminNhanVienRequest request);

    void doiTrangThai(String id, TrangThai trangThai);

    void delete(String id);

    byte[] exportExcel();

    List<com.example.be.entity.PhanQuyen> getAllPhanQuyen();

    String uploadAvatar(org.springframework.web.multipart.MultipartFile file);
}
