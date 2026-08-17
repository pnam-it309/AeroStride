package com.example.be.core.admin.danhgia.service.impl;

import com.example.be.core.admin.danhgia.model.request.AdminDanhGiaFilterRequest;
import com.example.be.core.admin.danhgia.model.response.AdminDanhGiaConfigResponse;
import com.example.be.core.admin.danhgia.model.response.AdminDanhGiaResponse;
import com.example.be.core.admin.danhgia.repository.AdminDanhGiaRepository;
import com.example.be.core.admin.danhgia.repository.AdminDanhGiaSpecification;
import com.example.be.core.admin.danhgia.service.AdminDanhGiaService;
import com.example.be.core.admin.danhgia.service.ReviewConfigService;
import com.example.be.entity.DanhGiaSanPham;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDanhGiaServiceImpl implements AdminDanhGiaService {

    private final AdminDanhGiaRepository repository;
    private final ReviewConfigService reviewConfigService;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminDanhGiaResponse> getPageDanhGia(AdminDanhGiaFilterRequest request, Pageable pageable) {
        Page<DanhGiaSanPham> page = repository.findAll(AdminDanhGiaSpecification.filter(request), pageable);
        return page.map(AdminDanhGiaResponse::new);
    }

    @Override
    @Transactional
    public AdminDanhGiaResponse updateStatus(String id, DanhGiaSanPham.TrangThaiDanhGia trangThai) {
        DanhGiaSanPham entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + id));
        entity.setTrangThai(trangThai);
        return new AdminDanhGiaResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDanhGiaConfigResponse getConfigAndStats() {
        return reviewConfigService.getConfigAndStats();
    }

    @Override
    public AdminDanhGiaConfigResponse setAutoApprove(boolean enabled) {
        reviewConfigService.setAutoApproveEnabled(enabled);
        return reviewConfigService.getConfigAndStats();
    }

    @Override
    @Transactional
    public void deleteDanhGia(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + id);
        }
        repository.deleteById(id);
    }
}
