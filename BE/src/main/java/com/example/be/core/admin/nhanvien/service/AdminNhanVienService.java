package com.example.be.core.admin.nhanvien.service;

import com.example.be.core.admin.nhanvien.model.request.AdminNhanVienRequest;
import com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.core.common.dto.PageResponse;

public interface AdminNhanVienService {
    PageResponse<AdminNhanVienResponse> search(PageRequest pageRequest);
    AdminNhanVienResponse getOne(String id);
    AdminNhanVienResponse create(AdminNhanVienRequest request);
    AdminNhanVienResponse update(String id, AdminNhanVienRequest request);
    Boolean delete(String id);
}
