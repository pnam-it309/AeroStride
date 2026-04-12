package com.example.be.core.admin.hoadon.service;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonResponse;
import com.example.be.entity.HoaDon;
import org.springframework.data.domain.Page;

public interface AdminHoaDonService {
    // Trả về Page của Spring để tránh lỗi mapping thủ công
    Page<AdminHoaDonResponse> getPage(AdminHoaDonRequest request);

    HoaDon detail(String id);

    HoaDon updateStatus(String id, Integer status);

    java.util.Map<String, Long> getCounts();

    byte[] exportExcel(AdminHoaDonRequest request);

    String generateInvoiceHtml(String id);
}
