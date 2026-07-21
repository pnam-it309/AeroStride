package com.example.be.core.admin.danhgia.service.impl;

import com.example.be.core.admin.danhgia.model.request.AdminDanhGiaFilterRequest;
import com.example.be.core.admin.danhgia.model.response.AdminDanhGiaResponse;
import com.example.be.core.admin.danhgia.repository.AdminDanhGiaRepository;
import com.example.be.core.admin.danhgia.repository.AdminDanhGiaSpecification;
import com.example.be.core.admin.danhgia.service.AdminDanhGiaService;
import com.example.be.entity.DanhGiaSanPham;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDanhGiaServiceImpl implements AdminDanhGiaService {

    private final AdminDanhGiaRepository repository;

    @Override
    public Page<AdminDanhGiaResponse> getPageDanhGia(AdminDanhGiaFilterRequest request, Pageable pageable) {
        Page<DanhGiaSanPham> page = repository.findAll(AdminDanhGiaSpecification.filter(request), pageable);
        return page.map(AdminDanhGiaResponse::new);
    }

    @Override
    public AdminDanhGiaResponse updateStatus(String id, DanhGiaSanPham.TrangThaiDanhGia trangThai) {
        DanhGiaSanPham entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + id));
        entity.setTrangThai(trangThai);
        return new AdminDanhGiaResponse(repository.save(entity));
    }

    @Override
    public void deleteDanhGia(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + id);
        }
        repository.deleteById(id);
    }
}
