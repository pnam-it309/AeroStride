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
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.infrastructure.exceptions.SystemException;
import com.example.be.core.admin.dotgiamgia.repository.AdminChiTietDotGiamGiaRepository;
import com.example.be.utils.AccountUtils;
import com.example.be.utils.ExcelUtils;
import com.example.be.utils.SearchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.be.core.admin.dotgiamgia.repository.AdminDotGiamGiaSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import com.example.be.core.admin.sanpham.repository.AdminAnhChiTietSanPhamRepository;
import com.example.be.entity.AnhChiTietSanPham;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDotGiamGiaServiceImpl implements AdminDotGiamGiaService {

    private final AdminDotGiamGiaRepository repo;
    private final AdminChiTietSanPhamRepository chiTietSanPhamRepo;
    private final AdminAnhChiTietSanPhamRepository anhChiTietSanPhamRepo;
    private final AdminSanPhamMapper mapper;
    private final AdminChiTietDotGiamGiaRepository chiTietDotGiamGiaRepo;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;



    @Override
    @Transactional(readOnly = true)
    public Page<AdminDotGiamGiaResponse> search(AdminDotGiamGiaSearchRequest request) {
        Long startLong = null;
        Long endLong = null;

        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                java.time.LocalDate startDate = java.time.LocalDate.parse(request.getStartDate(), formatter);
                startLong = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                java.time.LocalDate endDate = java.time.LocalDate.parse(request.getEndDate(), formatter);
                endLong = endDate.atTime(java.time.LocalTime.MAX).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
        } catch (Exception e) {
            // Ignore parse errors
        }

        final Long finalStart = startLong;
        final Long finalEnd = endLong;

        Specification<DotGiamGia> spec = Specification.where(AdminDotGiamGiaSpecification.keywordLike(request.getKeyword()))
                .and(AdminDotGiamGiaSpecification.filterTrangThai(request.getTrangThai(), System.currentTimeMillis()))
                .and(AdminDotGiamGiaSpecification.startDateAfter(finalStart))
                .and(AdminDotGiamGiaSpecification.endDateBefore(finalEnd));

        return SearchUtils.execute(request, pageable -> repo.findAll(spec, pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public void add(AdminDotGiamGiaRequest req) {
        validateRequest(req);
        DotGiamGia d = new DotGiamGia();
        BeanUtils.copyProperties(req, d);
        if (d.getMa() == null || d.getMa().trim().isEmpty()) {
            // Generating DGG sequential or unique code
            d.setMa("DGG" + System.currentTimeMillis() % 1000000);
        }
        if (d.getTrangThai() == null) {
            d.setTrangThai(TrangThai.DANG_HOAT_DONG);
        }
        DotGiamGia saved = repo.saveAndFlush(d);
        saveProducts(saved, req.getListIdChiTietSanPham());
    }

    @Override
    @Transactional
    public void update(AdminDotGiamGiaRequest req, String id) {
        validateRequest(req);
        DotGiamGia d = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.DOT_GIAM_GIA_NOT_FOUND_ID + id));
        BeanUtils.copyProperties(req, d);
        d.setId(id); // Keep the ID
        DotGiamGia saved = repo.saveAndFlush(d);

        chiTietDotGiamGiaRepo.deleteByDotGiamGiaId(id);
        chiTietDotGiamGiaRepo.flush();
        saveProducts(saved, req.getListIdChiTietSanPham());
    }

    private void validateRequest(AdminDotGiamGiaRequest req) {
        if (req.getTen() != null) {
            req.setTen(req.getTen().trim());
        }
        if (req.getNgayBatDau() != null && req.getNgayKetThuc() != null) {
            if (req.getNgayBatDau() >= req.getNgayKetThuc()) {
                throw new SystemException("Ngày kết thúc phải sau ngày bắt đầu");
            }
        }
    }

    private void saveProducts(DotGiamGia d, List<String> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) return;

        List<String> distinctVariantIds = variantIds.stream()
                .filter(id -> id != null && !id.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (distinctVariantIds.isEmpty()) return;

        List<ChiTietSanPham> variants = chiTietSanPhamRepo.findAllById(distinctVariantIds);
        if (variants.isEmpty()) return;

        List<ChiTietDotGiamGia> details = variants.stream().map(v -> {
            ChiTietDotGiamGia ct = new ChiTietDotGiamGia();
            ct.setDotGiamGia(d);
            ct.setChiTietSanPham(v);
            ct.setGiaTriGiam(d.getSoTienGiam());
            ct.setTrangThai(d.getTrangThai() != null ? d.getTrangThai() : TrangThai.DANG_HOAT_DONG);
            return ct;
        }).collect(Collectors.toList());

        chiTietDotGiamGiaRepo.saveAll(details);
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException(MessageConstants.DOT_GIAM_GIA_NOT_FOUND);
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public void updateStatus(String id, TrangThai status) {
        DotGiamGia d = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.DOT_GIAM_GIA_NOT_FOUND));
        d.setTrangThai(status);
        if (status == TrangThai.NGUNG_HOAT_DONG) {
            d.setNgayKetThuc(System.currentTimeMillis());
        }
        repo.saveAndFlush(d);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        List<AdminDotGiamGiaResponse> data = repo.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        String[] headers = {"STT", "Mã", "Tên", "Giá trị (%)", "Giảm tối đa (đ)", "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái"};

        try {
            return ExcelUtils.exportToExcel("Đợt giảm giá", headers, data, item -> new Object[]{
                data.indexOf(item) + 1,
                item.getMa(),
                item.getTen(),
                item.getSoTienGiam(),
                item.getGiamToiDa(),
                item.getNgayBatDau(),
                item.getNgayKetThuc()
            });
        } catch (IOException e) {
            throw new SystemException(MessageConstants.EXCEL_EXPORT_ERROR + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDotGiamGiaResponse findById(String id) {
        DotGiamGia d = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.DOT_GIAM_GIA_DETAIL_NOT_FOUND));
        return toResponse(d);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getAvailableVariants() {
        List<ChiTietSanPham> variants = chiTietSanPhamRepo.findAllByXoaMemFalse();
        return mapVariants(variants);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getAppliedVariants(String campaignId) {
        // EntityGraph đã JOIN sẵn chiTietSanPham + sanPham + thuongHieu + chatLieu + mauSac + kichThuoc
        List<ChiTietDotGiamGia> relations = chiTietDotGiamGiaRepo.findByDotGiamGiaId(campaignId);
        List<ChiTietSanPham> variants = relations.stream()
                .map(ChiTietDotGiamGia::getChiTietSanPham)
                .filter(v -> v != null && !Boolean.TRUE.equals(v.getXoaMem()))
                .toList();
        if (variants.isEmpty()) return List.of();

        // Gán discount relations đã có sẵn (từ EntityGraph), không cần query thêm
        java.util.Map<String, List<ChiTietDotGiamGia>> relationMap = relations.stream()
                .filter(rel -> rel.getChiTietSanPham() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        rel -> rel.getChiTietSanPham().getId()
                ));

        // Map trực tiếp mà KHÔNG query ảnh (FE chỉ cần info cơ bản cho bảng applied)
        return variants.stream().map(v -> {
            v.setChiTietDotGiamGias(new java.util.LinkedHashSet<>(relationMap.getOrDefault(v.getId(), java.util.Collections.emptyList())));
            return mapper.toVariantResponse(v, java.util.Collections.emptyList());
        }).toList();
    }

    private List<ProductVariantResponse> mapVariants(List<ChiTietSanPham> variants) {
        if (variants == null || variants.isEmpty()) {
            return List.of();
        }

        List<String> ids = variants.stream().map(ChiTietSanPham::getId).toList();

        // 1. Bulk-fetch images trong 1 query duy nhất
        List<AnhChiTietSanPham> images = anhChiTietSanPhamRepo
                .findAllByChiTietSanPhamIdInAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(ids);

        java.util.Map<String, List<com.example.be.core.admin.sanpham.model.response.ProductVariantImageResponse>> imageMap = images.stream()
                .filter(img -> img.getChiTietSanPham() != null)
                .collect(Collectors.groupingBy(
                        img -> img.getChiTietSanPham().getId(),
                        Collectors.mapping(mapper::toVariantImageResponse, Collectors.toList())
                ));

        // 2. Bulk-fetch quan hệ đợt giảm giá trong 1 query duy nhất
        List<ChiTietDotGiamGia> relations = chiTietDotGiamGiaRepo.findAllByChiTietSanPhamIdIn(ids);

        java.util.Map<String, List<ChiTietDotGiamGia>> relationMap = relations.stream()
                .filter(rel -> rel.getChiTietSanPham() != null)
                .collect(Collectors.groupingBy(
                        rel -> rel.getChiTietSanPham().getId()
                ));

        // 3. Map sang response trong memory (0 query phát sinh)
        return variants.stream().map(v -> {
            v.setChiTietDotGiamGias(new java.util.LinkedHashSet<>(relationMap.getOrDefault(v.getId(), java.util.Collections.emptyList())));
            List<com.example.be.core.admin.sanpham.model.response.ProductVariantImageResponse> imgs = imageMap.getOrDefault(v.getId(), java.util.Collections.emptyList());
            return mapper.toVariantResponse(v, imgs);
        }).toList();
    }

    private AdminDotGiamGiaResponse toResponse(DotGiamGia d) {
        if (d == null) return null;
        return AdminDotGiamGiaResponse.builder()
                .id(d.getId())
                .ma(d.getMa())
                .ten(d.getTen())
                .loaiGiamGia(d.getLoaiGiamGia())
                .soTienGiam(d.getSoTienGiam())
                .dieuKienGiamGia(d.getDieuKienGiamGia())
                .giamToiDa(d.getGiamToiDa())
                .ngayBatDau(d.getNgayBatDau())
                .ngayKetThuc(d.getNgayKetThuc())
                .mucUuTien(d.getMucUuTien())
                .trangThai(d.getTrangThai() != null ? d.getTrangThai().name() : null)
                .moTa(d.getMoTa())
                .isFlashSale(d.getIsFlashSale())
                .khungGio(d.getKhungGio())
                .build();
    }
}
