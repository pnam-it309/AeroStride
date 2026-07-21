package com.example.be.core.admin.hoadon.service;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonDetailResponse;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonResponse;
import com.example.be.entity.HoaDon;
import org.springframework.data.domain.Page;

public interface AdminHoaDonService {
    // Trả về Page của Spring để tránh lỗi mapping thủ công
    Page<AdminHoaDonResponse> getPage(AdminHoaDonRequest request);

    AdminHoaDonDetailResponse detail(String id);

    AdminHoaDonDetailResponse updateStatus(String id, Integer status, String note);

    AdminHoaDonDetailResponse updateInfo(String id, com.example.be.core.admin.hoadon.model.request.AdminUpdateHoaDonRequest request);

    AdminHoaDonDetailResponse updateHdct(String id, com.example.be.core.admin.hoadon.model.request.AdminUpdateHdctRequest request);

    AdminHoaDonDetailResponse removeHdct(String id, String idHdct);

    AdminHoaDonDetailResponse confirmRefund(String id);

    java.util.Map<String, Long> getCounts(AdminHoaDonRequest request);

    byte[] exportExcel(AdminHoaDonRequest request);

    String generateInvoiceHtml(String id);
}
