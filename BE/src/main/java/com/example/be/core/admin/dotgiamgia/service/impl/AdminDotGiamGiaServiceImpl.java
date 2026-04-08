package com.example.be.core.admin.dotgiamgia.service.impl;

import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaRequest;
import com.example.be.core.admin.dotgiamgia.model.response.AdminDotGiamGiaResponse;
import com.example.be.core.admin.dotgiamgia.repository.AdminDotGiamGiaRepository;
import com.example.be.core.admin.dotgiamgia.service.AdminDotGiamGiaService;
import com.example.be.entity.DotGiamGia;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class AdminDotGiamGiaServiceImpl implements AdminDotGiamGiaService {

    private final AdminDotGiamGiaRepository repo;

    @Override
    public Page<AdminDotGiamGiaResponse> phanTrang(Integer pageNo, Integer pageSize, String keyword) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        return repo.phanTrang(keyword, pageable);
    }

    @Override
    public void add(AdminDotGiamGiaRequest req) {
        DotGiamGia d = new DotGiamGia();
        BeanUtils.copyProperties(req, d);
        repo.save(d);
    }

    @Override
    public void update(AdminDotGiamGiaRequest req, String id) {
        DotGiamGia d = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt giảm giá"));
        BeanUtils.copyProperties(req, d);
        repo.save(d);
    }

    @Override
    public void delete(String id) {
        repo.deleteById(id);
    }
}
