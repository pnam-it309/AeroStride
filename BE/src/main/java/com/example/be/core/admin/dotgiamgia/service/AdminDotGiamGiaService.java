package com.example.be.core.admin.dotgiamgia.service;

import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaRequest;
import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaSearchRequest;
import com.example.be.core.admin.dotgiamgia.model.response.AdminDotGiamGiaResponse;
import org.springframework.data.domain.Page;
import java.util.List;

public interface AdminDotGiamGiaService {

    Page<AdminDotGiamGiaResponse> search(AdminDotGiamGiaSearchRequest request);

    void add(AdminDotGiamGiaRequest req);

    void update(AdminDotGiamGiaRequest req, String id);

    void delete(String id);

    void updateStatus(String id, com.example.be.infrastructure.constants.TrangThai status);

    byte[] exportExcel();

    AdminDotGiamGiaResponse findById(String id);

    List<com.example.be.core.admin.sanpham.model.response.ProductVariantResponse> getAvailableVariants();

    List<com.example.be.core.admin.sanpham.model.response.ProductVariantResponse> getAppliedVariants(String campaignId);
}
