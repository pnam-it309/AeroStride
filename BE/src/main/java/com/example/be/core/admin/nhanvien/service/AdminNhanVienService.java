package com.example.be.core.admin.nhanvien.service;

import com.example.be.core.admin.nhanvien.model.request.AdminNhanVienRequest;
import com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminNhanVienService {
    List<AdminNhanVienResponse> hienThi();

    Page<AdminNhanVienResponse> phanTrang(AdminNhanVienRequest request);

    Page<AdminNhanVienResponse> timKiem(AdminNhanVienRequest request);

    Page<AdminNhanVienResponse> locNV(AdminNhanVienRequest request);

    AdminNhanVienResponse detail(String id);

    AdminNhanVienResponse add(AdminNhanVienRequest request);

    AdminNhanVienResponse update(String id, AdminNhanVienRequest request);

    void doiTrangThai(String id, TrangThai trangThai);

    void delete(String id);

    byte[] exportExcel();
 
    List<com.example.be.entity.PhanQuyen> getAllPhanQuyen();
}
