package com.example.be.core.admin.dotgiamgia.service;

import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaRequest;
import com.example.be.core.admin.dotgiamgia.model.response.AdminDotGiamGiaResponse;
import org.springframework.data.domain.Page;

public interface AdminDotGiamGiaService {

    Page<AdminDotGiamGiaResponse> phanTrang(Integer pageNo, Integer pageSize, String keyword);

    void add(AdminDotGiamGiaRequest req);

    void update(AdminDotGiamGiaRequest req, Long id);

    void delete(Long id);
}
