package com.example.be.core.admin.thuoctinh.service;

import com.example.be.core.admin.thuoctinh.model.request.AdminAttributeRequest;
import com.example.be.core.admin.thuoctinh.model.response.AdminAttributeResponse;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.core.common.dto.PageResponse;

public interface AdminAttributeManagementService {

    PageResponse<AdminAttributeResponse> search(String keyword, String trangThai, PageRequest pageRequest);

    AdminAttributeResponse getById(String id);

    AdminAttributeResponse create(AdminAttributeRequest request);

    AdminAttributeResponse update(String id, AdminAttributeRequest request);

    void delete(String id);
}
