package com.example.be.core.admin.phieugiamgia.service.impl;

import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse;
import com.example.be.core.admin.phieugiamgia.repository.AdminPhieuGiamGiaRepository;
import com.example.be.core.admin.phieugiamgia.service.AdminPhieuGiamGiaService;
import com.example.be.entity.PhieuGiamGia;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@RequiredArgsConstructor
@Service
public class AdminPhieuGiamGiaServiceImpl implements AdminPhieuGiamGiaService {

    @Autowired
    private AdminPhieuGiamGiaRepository repo;

    @Override
    public List<AdminPhieuGiamGiaResponse> hienThi() {
        return repo.hienThi();
    }

    @Override
    public AdminPhieuGiamGiaResponse detail(String ma) {
        return repo.detail(ma);
    }

    @Override
    public Page<AdminPhieuGiamGiaResponse> phanTrang(Integer pageNo, Integer pageSize, String keyword) {
        // Sắp xếp theo ID giảm dần hoặc ngayTao giảm dần để dữ liệu mới luôn ở trên cùng
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        return repo.phanTrang(keyword, pageable);
    }

    @Override
    public void delete(Integer id) {
        repo.deleteById(id);
    }

    @Override
    public void add(AdminPhieuGiamGiaRequest req) {
        PhieuGiamGia p = new PhieuGiamGia();
        BeanUtils.copyProperties(req, p);
        // Nên set mặc định hoặc xử lý logic mã tự sinh ở đây nếu cần
        // p.setTrangThai(1);
        repo.save(p);
    }

    @Override
    public void update(AdminPhieuGiamGiaRequest req, Integer id) {
        PhieuGiamGia p = repo.findById(id).get();
        BeanUtils.copyProperties(req, p);
        repo.save(p);
    }
}
