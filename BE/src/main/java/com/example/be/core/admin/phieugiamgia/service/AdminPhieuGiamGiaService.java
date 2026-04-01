package com.example.be.core.admin.phieugiamgia.service;

import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.core.common.dto.PageResponse;

public interface AdminPhieuGiamGiaService {
    PageResponse<AdminPhieuGiamGiaResponse> search(PageRequest pageRequest);
    AdminPhieuGiamGiaResponse getOne(String id);
    AdminPhieuGiamGiaResponse create(AdminPhieuGiamGiaRequest request);
    AdminPhieuGiamGiaResponse update(String id, AdminPhieuGiamGiaRequest request);
    Boolean delete(String id);
}
