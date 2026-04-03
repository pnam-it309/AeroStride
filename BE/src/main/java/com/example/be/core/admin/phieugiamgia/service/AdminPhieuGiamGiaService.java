package com.example.be.core.admin.phieugiamgia.service;

import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse;
import org.springframework.data.domain.Page;

import java.util.List;

//public interface AdminPhieuGiamGiaService {
//    PageResponse<AdminPhieuGiamGiaResponse> search(PageRequest pageRequest);
//    AdminPhieuGiamGiaResponse getOne(String id);
//    AdminPhieuGiamGiaResponse create(AdminPhieuGiamGiaRequest request);
//    AdminPhieuGiamGiaResponse update(String id, AdminPhieuGiamGiaRequest request);
//    Boolean delete(String id);
//
//}

public interface AdminPhieuGiamGiaService {

    List<AdminPhieuGiamGiaResponse> hienThi();

    AdminPhieuGiamGiaResponse detail(String ma);

    Page<AdminPhieuGiamGiaResponse> phanTrang(Integer pageNo, Integer pageSize, String keyword);

    void delete(Integer id);

    void add(AdminPhieuGiamGiaRequest req);

    void update(AdminPhieuGiamGiaRequest req, Integer id);
}
