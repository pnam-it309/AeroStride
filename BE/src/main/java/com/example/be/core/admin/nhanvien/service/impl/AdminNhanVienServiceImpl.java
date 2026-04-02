package com.example.be.core.admin.nhanvien.service.impl;

import com.example.be.core.admin.nhanvien.model.request.AdminNhanVienRequest;
import com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse;
import com.example.be.core.admin.nhanvien.repository.AdminNhanVienRepository;
import com.example.be.core.admin.nhanvien.service.AdminNhanVienService;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.RestApiException;
import com.example.be.repository.PhanQuyenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminNhanVienServiceImpl implements AdminNhanVienService {

    @Override
    public PageResponse<AdminNhanVienResponse> search(PageRequest pageRequest) {
        throw new UnsupportedOperationException("AdminNhanVienService.search is not implemented yet");
    }

    @Override
    public AdminNhanVienResponse getOne(String id) {
        throw new UnsupportedOperationException("AdminNhanVienService.getOne is not implemented yet");
    }

    @Override
    public AdminNhanVienResponse create(AdminNhanVienRequest request) {
        throw new UnsupportedOperationException("AdminNhanVienService.create is not implemented yet");
    }

    @Override
    public AdminNhanVienResponse update(String id, AdminNhanVienRequest request) {
        throw new UnsupportedOperationException("AdminNhanVienService.update is not implemented yet");
    }

    @Override
    public Boolean delete(String id) {
        throw new UnsupportedOperationException("AdminNhanVienService.delete is not implemented yet");
    }
}
