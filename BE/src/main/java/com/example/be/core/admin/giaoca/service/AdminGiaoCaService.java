package com.example.be.core.admin.giaoca.service;

import com.example.be.core.admin.giaoca.model.request.AdminChotCaRequest;
import com.example.be.core.admin.giaoca.model.request.AdminMoCaRequest;
import com.example.be.core.admin.giaoca.model.response.AdminGiaoCaResponse;

import java.util.List;

public interface AdminGiaoCaService {

    AdminGiaoCaResponse moCa(AdminMoCaRequest request);

    AdminGiaoCaResponse getCaHienTai(String username);

    AdminGiaoCaResponse chotCa(String username, AdminChotCaRequest request);

    List<AdminGiaoCaResponse> getAllLichSu();
}
