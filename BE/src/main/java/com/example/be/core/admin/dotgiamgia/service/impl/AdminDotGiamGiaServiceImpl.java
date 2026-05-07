package com.example.be.core.admin.dotgiamgia.service.impl;

import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaRequest;
import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaSearchRequest;
import com.example.be.core.admin.dotgiamgia.model.response.AdminDotGiamGiaResponse;
import com.example.be.core.admin.dotgiamgia.repository.AdminDotGiamGiaRepository;
import com.example.be.core.admin.dotgiamgia.service.AdminDotGiamGiaService;
import com.example.be.core.admin.sanpham.mapper.AdminSanPhamMapper;
import com.example.be.core.admin.sanpham.model.response.ProductVariantResponse;
import com.example.be.core.admin.sanpham.repository.AdminChiTietSanPhamRepository;
import com.example.be.entity.ChiTietDotGiamGia;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.DotGiamGia;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.infrastructure.exceptions.SystemException;
import com.example.be.repository.ChiTietDotGiamGiaRepository;
import com.example.be.utils.AccountUtils;
import com.example.be.utils.ExcelUtils;
import com.example.be.utils.PaginationUtils;
import com.example.be.utils.SearchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Transactional(readOnly = true)
    public Page<AdminDotGiamGiaResponse> search(AdminDotGiamGiaSearchRequest request) {
        Long startLong = null;
        Long endLong = null;

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                startLong = sdf.parse(request.getStartDate()).getTime();
            }
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                // End of day
                endLong = sdf.parse(request.getEndDate()).getTime() + 86399999L;
            }
        } catch (Exception e) {
            // Ignore parse errors
        }

        final Long finalStart = startLong;
        final Long finalEnd = endLong;

        return SearchUtils.execute(request, pageable -> repo.phanTrang(
                request.getKeyword(),
                request.getTrangThai(),
                System.currentTimeMillis(),
                finalStart,
                finalEnd,
                pageable
        ));
    }

    @Override
    @Transactional
    public void add(AdminDotGiamGiaRequest req) {
        DotGiamGia d = new DotGiamGia();
        BeanUtils.copyProperties(req, d);
        if (d.getMa() == null || d.getMa().trim().isEmpty()) {
            // Generating DGG sequential or unique code
            d.setMa("DGG" + System.currentTimeMillis() % 1000000);
        }
        repo.save(d);
        saveProducts(d, req.getListIdChiTietSanPham());
    }

    @Override
    @Transactional
    public void update(AdminDotGiamGiaRequest req, String id) {
        DotGiamGia d = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đợt giảm giá với id: " + id));
        BeanUtils.copyProperties(req, d);
        d.setId(id); // Keep the ID
        repo.save(d);

        chiTietDotGiamGiaRepo.deleteByDotGiamGiaId(id);
        saveProducts(d, req.getListIdChiTietSanPham());
    }

    private void saveProducts(DotGiamGia d, List<String> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) return;

        List<ChiTietDotGiamGia> detailEntities = variantIds.stream()
                .map(vid -> chiTietSanPhamRepo.findById(vid).orElse(null))
                .filter(v -> v != null)
                .map(v -> {
                    ChiTietDotGiamGia ct = new ChiTietDotGiamGia();
                    ct.setDotGiamGia(d);
                    ct.setChiTietSanPham(v);
                    ct.setGiaTriGiam(d.getSoTienGiam());
                    return ct;
                })
                .collect(Collectors.toList());

        chiTietDotGiamGiaRepo.saveAll(detailEntities);
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy đợt giảm giá");
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public void updateStatus(String id, TrangThai status) {
        DotGiamGia d = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đợt giảm giá"));
        d.setTrangThai(status);
        repo.saveAndFlush(d);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        Pageable pageable = PaginationUtils.createPageable(0, Integer.MAX_VALUE, "id", "desc");
        List<AdminDotGiamGiaResponse> data = repo.phanTrang(null, null, System.currentTimeMillis(), null, null, pageable).getContent();

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
            throw new SystemException("Lỗi xuất file Excel: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDotGiamGiaResponse findById(String id) {
        AdminDotGiamGiaResponse res = repo.getDetailById(id);
        if (res == null) {
            throw new ResourceNotFoundException("Không tìm thấy chi tiết đợt giảm giá");
        }
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getAvailableVariants() {
        return chiTietSanPhamRepo.findAllByXoaMemFalse()
                .stream()
                .map(v -> mapper.toVariantResponse(v, List.of()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getAppliedVariants(String campaignId) {
        return chiTietDotGiamGiaRepo.findByDotGiamGiaId(campaignId)
                .stream()
                .map(ChiTietDotGiamGia::getChiTietSanPham)
                .filter(v -> !Boolean.TRUE.equals(v.getXoaMem()))
                .map(v -> mapper.toVariantResponse(v, List.of()))
                .collect(Collectors.toList());
    }
}
