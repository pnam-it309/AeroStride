package com.example.be.core.admin.hoadon.repository;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface AdminHoaDonRepositoryCustom {

    Page<AdminHoaDonResponse> getAllHoaDon(Pageable pageable, AdminHoaDonRequest req);

    List<Map<String, Object>> countByTrangThai(AdminHoaDonRequest req);

    long countWithFilter(AdminHoaDonRequest req);
}
