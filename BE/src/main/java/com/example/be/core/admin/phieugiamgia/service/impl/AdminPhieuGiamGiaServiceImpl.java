package com.example.be.core.admin.phieugiamgia.service.impl;

import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse;
import com.example.be.core.admin.phieugiamgia.repository.AdminPhieuGiamGiaRepository;
import com.example.be.core.admin.phieugiamgia.service.AdminPhieuGiamGiaService;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.entity.PhieuGiamGia;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPhieuGiamGiaServiceImpl implements AdminPhieuGiamGiaService {

    @Override
    public PageResponse<AdminPhieuGiamGiaResponse> search(PageRequest pageRequest) {
        throw new UnsupportedOperationException("AdminPhieuGiamGiaService.search is not implemented yet");
    }

    @Override
    public AdminPhieuGiamGiaResponse getOne(String id) {
        throw new UnsupportedOperationException("AdminPhieuGiamGiaService.getOne is not implemented yet");
    }

    @Override
    public AdminPhieuGiamGiaResponse create(AdminPhieuGiamGiaRequest request) {
        throw new UnsupportedOperationException("AdminPhieuGiamGiaService.create is not implemented yet");
    }

    @Override
    public AdminPhieuGiamGiaResponse update(String id, AdminPhieuGiamGiaRequest request) {
        throw new UnsupportedOperationException("AdminPhieuGiamGiaService.update is not implemented yet");
    }

    @Override
    public Boolean delete(String id) {
        throw new UnsupportedOperationException("AdminPhieuGiamGiaService.delete is not implemented yet");
    }
}
