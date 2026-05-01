package com.example.be.core.admin.khachhang.service;

import com.example.be.core.admin.khachhang.model.request.AdminDiaChiRequest;
import com.example.be.core.admin.khachhang.model.response.AdminDiaChiResponse;
import java.util.List;

public interface AdminDiaChiService {
    List<AdminDiaChiResponse> getByKhachHangId(String khId);
    AdminDiaChiResponse add(AdminDiaChiRequest request);
    AdminDiaChiResponse update(String id, AdminDiaChiRequest request);
    void delete(String id);
    void setDefault(String id);
}
