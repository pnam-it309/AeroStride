package com.example.be.core.admin.danhgia.service;

import com.example.be.core.admin.danhgia.model.request.AdminDanhGiaFilterRequest;
import com.example.be.core.admin.danhgia.model.response.AdminDanhGiaResponse;
import com.example.be.entity.DanhGiaSanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminDanhGiaService {
    Page<AdminDanhGiaResponse> getPageDanhGia(AdminDanhGiaFilterRequest request, Pageable pageable);
    AdminDanhGiaResponse updateStatus(String id, DanhGiaSanPham.TrangThaiDanhGia trangThai);
    void deleteDanhGia(String id);
}
