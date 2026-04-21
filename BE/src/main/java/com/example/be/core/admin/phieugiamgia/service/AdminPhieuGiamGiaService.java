package com.example.be.core.admin.phieugiamgia.service;

import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminPhieuGiamGiaService {

    List<AdminPhieuGiamGiaResponse> hienThi();

    AdminPhieuGiamGiaResponse detail(String id);

    Page<AdminPhieuGiamGiaResponse> phanTrang(AdminPhieuGiamGiaRequest request);

    void delete(String id);

    void add(AdminPhieuGiamGiaRequest req);

    void update(AdminPhieuGiamGiaRequest req, String id);

    void updateStatus(String id, com.example.be.infrastructure.constants.TrangThai status);

    byte[] exportExcel();

    byte[] downloadTemplate();

    void importExcel(org.springframework.web.multipart.MultipartFile file);
}
