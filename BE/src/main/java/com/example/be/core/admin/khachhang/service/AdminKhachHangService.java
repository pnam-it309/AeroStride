package com.example.be.core.admin.khachhang.service;

import com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest;
import com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminKhachHangService {
    List<AdminKhachHangResponse> hienThi();

    Page<AdminKhachHangResponse> phanTrang(AdminKhachHangRequest request);

    Page<AdminKhachHangResponse> timKiem(AdminKhachHangRequest request);

    Page<AdminKhachHangResponse> locKH(AdminKhachHangRequest request);

    AdminKhachHangResponse detail(String id);

    AdminKhachHangResponse add(AdminKhachHangRequest request);

    AdminKhachHangResponse update(String id, AdminKhachHangRequest request);

    void doiTrangThai(String id, TrangThai trangThai);

    void delete(String id);

    byte[] exportExcel();
}
