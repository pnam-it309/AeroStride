package com.example.be.core.admin.dotgiamgia.service.impl;

import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaRequest;
import com.example.be.core.admin.dotgiamgia.model.response.AdminDotGiamGiaResponse;
import com.example.be.core.admin.dotgiamgia.repository.AdminDotGiamGiaRepository;
import com.example.be.core.admin.dotgiamgia.service.AdminDotGiamGiaService;
import com.example.be.core.admin.sanpham.mapper.AdminSanPhamMapper;
import com.example.be.core.admin.sanpham.model.response.ProductVariantResponse;
import com.example.be.core.admin.sanpham.repository.AdminChiTietSanPhamRepository;
import com.example.be.entity.DotGiamGia;
import com.example.be.repository.ChiTietDotGiamGiaRepository;
import com.example.be.utils.ExcelUtils;
import com.example.be.utils.MaGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDotGiamGiaServiceImpl implements AdminDotGiamGiaService {

    private final AdminDotGiamGiaRepository repo;
    private final AdminChiTietSanPhamRepository chiTietSanPhamRepo;
    private final AdminSanPhamMapper mapper;
    private final ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepo;

    @Override
    public Page<AdminDotGiamGiaResponse> phanTrang(Integer pageNo, Integer pageSize, String keyword) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        return repo.phanTrang(keyword, pageable);
    }

    @Override
    @Transactional
    public void add(AdminDotGiamGiaRequest req) {
        DotGiamGia d = new DotGiamGia();
        BeanUtils.copyProperties(req, d);
        if (d.getMa() == null || d.getMa().trim().isEmpty()) {
            d.setMa(MaGenerator.generate(DotGiamGia.class));
        }
        repo.save(d);
        saveProducts(d, req.getListIdChiTietSanPham());
    }

    @Override
    @Transactional
    public void update(AdminDotGiamGiaRequest req, String id) {
        DotGiamGia d = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt giảm giá"));
        BeanUtils.copyProperties(req, d);
        repo.save(d);
        
        chiTietDotGiamGiaRepo.deleteByDotGiamGiaId(id);
        saveProducts(d, req.getListIdChiTietSanPham());
    }

    private void saveProducts(DotGiamGia d, List<String> variantIds) {
        if (variantIds == null) return;
        for (String vid : variantIds) {
            com.example.be.entity.ChiTietSanPham v = chiTietSanPhamRepo.findById(vid).orElse(null);
            if (v != null) {
                com.example.be.entity.ChiTietDotGiamGia ct = new com.example.be.entity.ChiTietDotGiamGia();
                ct.setDotGiamGia(d);
                ct.setChiTietSanPham(v);
                ct.setGiaTriGiam(d.getSoTienGiam());
                chiTietDotGiamGiaRepo.save(ct);
            }
        }
    }

    @Override
    public void delete(String id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public void updateStatus(String id, com.example.be.infrastructure.constants.TrangThai status) {
        DotGiamGia d = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt giảm giá"));
        d.setTrangThai(status);
        repo.saveAndFlush(d);
    }

    @Override
    public byte[] exportExcel() {
        // Fetch all without keyword, for total export
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("id").descending());
        List<AdminDotGiamGiaResponse> data = repo.phanTrang(null, pageable).getContent();
        
        String[] headers = {"STT", "Mã", "Tên", "Giá trị (%)", "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái"};
        
        try {
            return ExcelUtils.exportToExcel("Đợt giảm giá", headers, data, item -> new Object[]{
                data.indexOf(item) + 1,
                item.getMa(),
                item.getTen(),
                item.getSoTienGiam(),
                item.getNgayBatDau(),
                item.getNgayKetThuc()
            });
        } catch (IOException e) {
            throw new RuntimeException("Lỗi xuất file Excel: " + e.getMessage());
        }
    }

    @Override
    public AdminDotGiamGiaResponse findById(String id) {
        return repo.getDetailById(id);
    }

    @Override
    public List<ProductVariantResponse> getAvailableVariants() {
        return chiTietSanPhamRepo.findAllByXoaMemFalse()
                .stream()
                .map(v -> mapper.toVariantResponse(v, List.of()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductVariantResponse> getAppliedVariants(String campaignId) {
        return chiTietDotGiamGiaRepo.findByDotGiamGiaId(campaignId)
                .stream()
                .map(ct -> ct.getChiTietSanPham())
                .filter(v -> !Boolean.TRUE.equals(v.getXoaMem()))
                .map(v -> mapper.toVariantResponse(v, List.of()))
                .collect(Collectors.toList());
    }
}
