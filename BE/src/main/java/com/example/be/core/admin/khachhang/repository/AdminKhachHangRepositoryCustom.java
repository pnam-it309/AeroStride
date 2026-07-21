package com.example.be.core.admin.khachhang.repository;

import com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminKhachHangRepositoryCustom {

    List<AdminKhachHangResponse> hienThi();

    AdminKhachHangResponse detail(String id);

    Page<AdminKhachHangResponse> filterAll(
            com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest request,
            Pageable pageable
    );
}
