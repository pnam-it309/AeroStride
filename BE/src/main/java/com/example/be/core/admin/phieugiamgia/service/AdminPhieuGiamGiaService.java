package com.example.be.core.admin.phieugiamgia.service;

import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminPhieuGiamGiaService {

    List<AdminPhieuGiamGiaResponse> hienThi();

    AdminPhieuGiamGiaResponse detail(String ma);

    Page<AdminPhieuGiamGiaResponse> phanTrang(Integer pageNo, Integer pageSize, String keyword);

    void delete(String id);

    void add(AdminPhieuGiamGiaRequest req);

    void update(AdminPhieuGiamGiaRequest req, String id);

    byte[] exportExcel();

    byte[] downloadTemplate();

    void importExcel(org.springframework.web.multipart.MultipartFile file);
}
