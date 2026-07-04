package com.example.be.core.customer.sanpham.service.impl;

import com.example.be.core.common.base.BaseCodeNameEntity;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.core.customer.sanpham.model.request.CustomerSearchProductRequest;
import com.example.be.core.customer.sanpham.model.response.*;
import com.example.be.core.customer.sanpham.repository.*;
import com.example.be.core.customer.sanpham.service.CustomerSanPhamService;
import com.example.be.entity.*;
import com.example.be.infrastructure.constants.GioiTinhKhachHang;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.utils.SearchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerSanPhamServiceImpl implements CustomerSanPhamService {

    private final CustomerSanPhamRepository customerSanPhamRepository;
    private final CustomerSanPhamChiTietRepository customerSanPhamChiTietRepository;
    private final CustomerSanPhamAnhChiTietRepository customerSanPhamAnhChiTietRepository;
    private final CustomerSanPhamChiTietDotGiamGiaRepository customerSanPhamChiTietDotGiamGiaRepository;

    private final CustomerSanPhamThuongHieuRepository thuongHieuRepository;
    private final CustomerSanPhamXuatXuRepository xuatXuRepository;
    private final CustomerSanPhamMucDichChayRepository mucDichChayRepository;
    private final CustomerSanPhamCoGiayRepository coGiayRepository;
    private final CustomerSanPhamChatLieuRepository chatLieuRepository;
    private final CustomerSanPhamDeGiayRepository deGiayRepository;
    private final CustomerSanPhamMauSacRepository mauSacRepository;
    private final CustomerSanPhamKichThuocRepository kichThuocRepository;

    // Lấy danh sách sản phẩm (có hỗ trợ lọc theo danh mục, thương hiệu, màu sắc, giới tính...)
    @Override
    public PageResponse<CustomerProductResponse> getProducts(CustomerSearchProductRequest request) {
        Specification<SanPham> spec = Specification.where(CustomerSanPhamSpecification.notDeleted())
                .and(CustomerSanPhamSpecification.hasKeyword(request.getKeyword()))
                .and(CustomerSanPhamSpecification.hasTrangThai(request.getTrangThai() != null ? request.getTrangThai() : TrangThai.DANG_HOAT_DONG))
                .and(CustomerSanPhamSpecification.hasThuongHieu(request.getThuongHieuId()))
                .and(CustomerSanPhamSpecification.hasGioiTinhKhachHang(request.getGioiTinhKhachHang()))
                .and(CustomerSanPhamSpecification.hasXuatXu(request.getXuatXuId()))
                .and(CustomerSanPhamSpecification.hasMucDichChay(request.getMucDichChayId()))
                .and(CustomerSanPhamSpecification.hasChatLieu(request.getChatLieuId()));

        Page<SanPham> page = SearchUtils.execute(request, pageable -> customerSanPhamRepository.findAll(spec, pageable));

        List<String> ids = page.getContent().stream().map(SanPham::getId).toList();
        Map<String, CustomerProductVariantStats> stats = ids.isEmpty() ? Map.of() :
                customerSanPhamChiTietRepository.summarizeBySanPhamIds(ids).stream()
                        .collect(Collectors.toMap(CustomerProductVariantStats::getSanPhamId, s -> s));

        return PageResponse.from(page.map(sp -> {
            CustomerProductVariantStats s = stats.get(sp.getId());
            return CustomerProductResponse.builder()
                    .id(sp.getId())
                    .maSanPham(sp.getMa())
                    .tenSanPham(sp.getTen())
                    .idThuongHieu(sp.getThuongHieu() != null ? sp.getThuongHieu().getId() : null)
                    .tenThuongHieu(sp.getThuongHieu() != null ? sp.getThuongHieu().getTen() : null)
                    .idXuatXu(sp.getXuatXu() != null ? sp.getXuatXu().getId() : null)
                    .tenXuatXu(sp.getXuatXu() != null ? sp.getXuatXu().getTen() : null)
                    .idMucDichChay(sp.getMucDichChay() != null ? sp.getMucDichChay().getId() : null)
                    .tenMucDichChay(sp.getMucDichChay() != null ? sp.getMucDichChay().getTen() : null)
                    .idCoGiay(sp.getCoGiay() != null ? sp.getCoGiay().getId() : null)
                    .tenCoGiay(sp.getCoGiay() != null ? sp.getCoGiay().getTen() : null)
                    .idChatLieu(sp.getChatLieu() != null ? sp.getChatLieu().getId() : null)
                    .tenChatLieu(sp.getChatLieu() != null ? sp.getChatLieu().getTen() : null)
                    .idDeGiay(sp.getDeGiay() != null ? sp.getDeGiay().getId() : null)
                    .tenDeGiay(sp.getDeGiay() != null ? sp.getDeGiay().getTen() : null)
                    .gioiTinhKhachHang(sp.getGioiTinhKhachHang())
                    .moTaNgan(sp.getMoTaChiTiet())
                    .hinhAnh(sp.getHinhAnh())
                    .trangThai(sp.getTrangThai())
                    .ngayTao(sp.getNgayTao())
                    .ngayCapNhat(sp.getNgayCapNhat())
                    .tongBienThe(s != null ? s.getTongBienThe() : 0L)
                    .tongSoLuongTon(s != null ? s.getTongSoLuongTon() : 0L)
                    .giaBanThapNhat(s != null ? s.getGiaBanThapNhat() : null)
                    .giaBanCaoNhat(s != null ? s.getGiaBanCaoNhat() : null)
                    .build();
        }));
    }

    // Lấy danh sách các thuộc tính (danh mục, thương hiệu, màu sắc, size...) để hiển thị lên bộ lọc tìm kiếm
    @Override
    public CustomerProductFormOptionsResponse getFormOptions() {
        Sort sortByName = Sort.by(Sort.Direction.ASC, "ten");
        return CustomerProductFormOptionsResponse.builder()
                .thuongHieus(mapOptions(thuongHieuRepository.findAll(sortByName)))
                .xuatXus(mapOptions(xuatXuRepository.findAll(sortByName)))
                .mucDichChays(mapOptions(mucDichChayRepository.findAll(sortByName)))
                .coGiays(mapOptions(coGiayRepository.findAll(sortByName)))
                .chatLieus(mapOptions(chatLieuRepository.findAll(sortByName)))
                .deGiays(mapOptions(deGiayRepository.findAll(sortByName)))
                .mauSacs(mapOptions(mauSacRepository.findAll(sortByName)))
                .kichThuocs(mapOptions(kichThuocRepository.findAll(sortByName)))
                .gioiTinhKhachHangs(Arrays.stream(GioiTinhKhachHang.values()).map(Enum::name).toList())
                .trangThais(Arrays.stream(TrangThai.values())
                        .filter(tt -> tt != TrangThai.DA_XOA)
                        .map(Enum::name)
                        .toList())
                .build();
    }

    // Lấy thông tin chi tiết của một sản phẩm kèm theo danh sách các biến thể của nó
    @Override
    public CustomerProductDetailResponse getProductDetail(String id) {
        SanPham sp = customerSanPhamRepository.findByIdAndXoaMemFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.SAN_PHAM_NOT_FOUND));

        List<ChiTietSanPham> variants = customerSanPhamChiTietRepository.findBySanPhamIdAndXoaMemFalseOrderByNgayTaoDesc(id);
        List<CustomerProductVariantResponse> variantResponses = mapVariants(variants);

        java.math.BigDecimal minPrice = null;
        java.math.BigDecimal maxPrice = null;
        List<String> availableColors = new java.util.ArrayList<>();
        java.util.Map<String, List<String>> availableSizesByColor = new java.util.LinkedHashMap<>();
        Integer totalStock = 0;
        java.util.Map<String, Integer> stockByColor = new java.util.HashMap<>();

        for (CustomerProductVariantResponse v : variantResponses) {
            java.math.BigDecimal price = v.getGiaBan();
            if (price != null && price.compareTo(java.math.BigDecimal.ZERO) > 0) {
                if (minPrice == null || price.compareTo(minPrice) < 0) minPrice = price;
                if (maxPrice == null || price.compareTo(maxPrice) > 0) maxPrice = price;
            }
            
            String color = v.getTenMauSac();
            String size = v.getTenKichThuoc();
            Integer qty = v.getSoLuong() != null ? v.getSoLuong() : 0;
            totalStock += qty;

            if (color != null && !color.trim().isEmpty()) {
                if (!availableColors.contains(color)) {
                    availableColors.add(color);
                }
                
                stockByColor.put(color, stockByColor.getOrDefault(color, 0) + qty);

                if (size != null && !size.trim().isEmpty()) {
                    availableSizesByColor.computeIfAbsent(color, k -> new java.util.ArrayList<>())
                                         .add(size);
                }
            }
        }
        
        availableSizesByColor.replaceAll((c, list) -> {
            return list.stream().distinct()
                       .sorted((a, b) -> {
                           try {
                               return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
                           } catch (Exception e) {
                               return a.compareTo(b);
                           }
                       })
                       .collect(java.util.stream.Collectors.toList());
        });

        return CustomerProductDetailResponse.builder()
                .id(sp.getId())
                .maSanPham(sp.getMa())
                .tenSanPham(sp.getTen())
                .idThuongHieu(sp.getThuongHieu() != null ? sp.getThuongHieu().getId() : null)
                .tenThuongHieu(sp.getThuongHieu() != null ? sp.getThuongHieu().getTen() : null)
                .idXuatXu(sp.getXuatXu() != null ? sp.getXuatXu().getId() : null)
                .tenXuatXu(sp.getXuatXu() != null ? sp.getXuatXu().getTen() : null)
                .idMucDichChay(sp.getMucDichChay() != null ? sp.getMucDichChay().getId() : null)
                .tenMucDichChay(sp.getMucDichChay() != null ? sp.getMucDichChay().getTen() : null)
                .idCoGiay(sp.getCoGiay() != null ? sp.getCoGiay().getId() : null)
                .tenCoGiay(sp.getCoGiay() != null ? sp.getCoGiay().getTen() : null)
                .idChatLieu(sp.getChatLieu() != null ? sp.getChatLieu().getId() : null)
                .tenChatLieu(sp.getChatLieu() != null ? sp.getChatLieu().getTen() : null)
                .idDeGiay(sp.getDeGiay() != null ? sp.getDeGiay().getId() : null)
                .tenDeGiay(sp.getDeGiay() != null ? sp.getDeGiay().getTen() : null)
                .gioiTinhKhachHang(sp.getGioiTinhKhachHang())
                .moTaNgan(sp.getMoTaChiTiet())
                .moTaChiTiet(sp.getMoTaChiTiet())
                .hinhAnh(sp.getHinhAnh())
                .trangThai(sp.getTrangThai())
                .ngayTao(sp.getNgayTao())
                .nguoiTao(sp.getNguoiTao())
                .ngayCapNhat(sp.getNgayCapNhat())
                .nguoiCapNhat(sp.getNguoiCapNhat())
                .variants(variantResponses)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .availableColors(availableColors)
                .availableSizesByColor(availableSizesByColor)
                .totalStock(totalStock)
                .stockByColor(stockByColor)
                .build();
    }

    // Mapper chuyển đổi danh sách các thực thể danh mục, thương hiệu, màu sắc... sang DTO dùng chung
    private List<CustomerProductOptionResponse> mapOptions(List<? extends BaseCodeNameEntity> entities) {
        return entities.stream()
                .filter(Objects::nonNull)
                .filter(e -> !Boolean.TRUE.equals(e.getXoaMem()))
                .filter(e -> e.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                .map(e -> CustomerProductOptionResponse.builder()
                        .id(e.getId())
                        .ma(e.getMa())
                        .ten(e.getTen())
                        .moTa(e instanceof MauSac ? ((MauSac) e).getMaMauHex() :
                              e instanceof KichThuoc ? ((KichThuoc) e).getGiaTriKichThuoc() : null)
                        .build())
                .toList();
    }

    // Mapper chuyển đổi danh sách biến thể (ChiTietSanPham) sang DTO, có tính toán áp dụng khuyến mãi
    private List<CustomerProductVariantResponse> mapVariants(List<ChiTietSanPham> variants) {
        if (variants == null || variants.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> ids = variants.stream().map(ChiTietSanPham::getId).toList();

        // 1. Bulk-fetch images
        List<AnhChiTietSanPham> images = customerSanPhamAnhChiTietRepository
                .findAllByChiTietSanPhamIdInAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(ids);

        Map<String, List<CustomerProductVariantImageResponse>> imageMap = images.stream()
                .filter(img -> img.getChiTietSanPham() != null)
                .collect(Collectors.groupingBy(
                        img -> img.getChiTietSanPham().getId(),
                        Collectors.mapping(img -> CustomerProductVariantImageResponse.builder()
                                .id(img.getId())
                                .duongDanAnh(img.getDuongDanAnh())
                                .moTa(img.getMoTa())
                                .hinhAnhDaiDien(img.getHinhAnhDaiDien())
                                .trangThai(img.getTrangThai())
                                .ngayTao(img.getNgayTao())
                                .ngayCapNhat(img.getNgayCapNhat())
                                .build(), Collectors.toList())
                ));

        // 2. Bulk-fetch campaign details
        List<ChiTietDotGiamGia> relations = customerSanPhamChiTietDotGiamGiaRepository.findAllByChiTietSanPhamIdIn(ids);

        Map<String, List<ChiTietDotGiamGia>> relationMap = relations.stream()
                .filter(rel -> rel.getChiTietSanPham() != null)
                .collect(Collectors.groupingBy(
                        rel -> rel.getChiTietSanPham().getId()
                ));

        long now = System.currentTimeMillis();

        // 3. Map to responses in memory
        return variants.stream().map(v -> {
            v.setChiTietDotGiamGias(new java.util.LinkedHashSet<>(relationMap.getOrDefault(v.getId(), new ArrayList<>())));
            List<CustomerProductVariantImageResponse> imgs = imageMap.getOrDefault(v.getId(), new ArrayList<>());

            BigDecimal activeDiscount = BigDecimal.ZERO;
            if (v.getChiTietDotGiamGias() != null) {
                List<DotGiamGia> validCampaigns = new ArrayList<>();
                for (ChiTietDotGiamGia ct : v.getChiTietDotGiamGias()) {
                    DotGiamGia d = ct.getDotGiamGia();
                    if (d != null && d.getTrangThai() == TrangThai.DANG_HOAT_DONG) {
                        if (d.getNgayBatDau() != null && d.getNgayKetThuc() != null
                                && d.getNgayBatDau() <= now && now <= d.getNgayKetThuc()) {
                            if (d.getSoTienGiam() != null) {
                                validCampaigns.add(d);
                            }
                        }
                    }
                }
                
                if (!validCampaigns.isEmpty()) {
                    validCampaigns.sort((c1, c2) -> c2.getSoTienGiam().compareTo(c1.getSoTienGiam()));
                    if (validCampaigns.size() == 1) {
                        activeDiscount = validCampaigns.get(0).getSoTienGiam();
                    } else {
                        DotGiamGia d1 = validCampaigns.get(0);
                        DotGiamGia d2 = validCampaigns.get(1);
                        BigDecimal m1 = d1.getSoTienGiam();
                        BigDecimal m2 = d2.getSoTienGiam();
                        long overlapStart = Math.max(d1.getNgayBatDau(), d2.getNgayBatDau());
                        long overlapEnd = Math.min(d1.getNgayKetThuc(), d2.getNgayKetThuc());
                        long overlapDays = (overlapEnd - overlapStart) / (1000L * 60 * 60 * 24);
                        if (overlapDays < 3) {
                            activeDiscount = m1;
                        } else {
                            activeDiscount = m1.add(m2).divide(new BigDecimal("2"), 0, java.math.RoundingMode.HALF_UP);
                        }
                    }
                }
            }

            return CustomerProductVariantResponse.builder()
                    .id(v.getId())
                    .idSanPham(v.getSanPham() != null ? v.getSanPham().getId() : null)
                    .maSanPham(v.getSanPham() != null ? v.getSanPham().getMa() : null)
                    .tenSanPham(v.getSanPham() != null ? v.getSanPham().getTen() : null)
                    .tenSanPhamDayDu(v.getSanPham() != null
                            ? String.format("%s [%s - %s]",
                            v.getSanPham().getTen(),
                            v.getMauSac() != null ? v.getMauSac().getTen() : "?",
                            v.getKichThuoc() != null ? v.getKichThuoc().getTen() : "?")
                            : null)
                    .tenThuongHieu(v.getSanPham() != null && v.getSanPham().getThuongHieu() != null ? v.getSanPham().getThuongHieu().getTen() : null)
                    .tenChatLieu(v.getSanPham() != null && v.getSanPham().getChatLieu() != null ? v.getSanPham().getChatLieu().getTen() : null)
                    .maChiTietSanPham(v.getMaChiTietSanPham())
                    .idMauSac(v.getMauSac() != null ? v.getMauSac().getId() : null)
                    .tenMauSac(v.getMauSac() != null ? v.getMauSac().getTen() : null)
                    .maMauHex(v.getMauSac() != null ? v.getMauSac().getMaMauHex() : null)
                    .idKichThuoc(v.getKichThuoc() != null ? v.getKichThuoc().getId() : null)
                    .tenKichThuoc(v.getKichThuoc() != null ? v.getKichThuoc().getTen() : null)
                    .giaTriKichThuoc(v.getKichThuoc() != null ? v.getKichThuoc().getGiaTriKichThuoc() : null)
                    .soLuong(v.getSoLuong())
                    .giaNhap(v.getGiaNhap())
                    .giaBan(v.getGiaBan())
                    .phanTramGiam(activeDiscount)
                    .trangThai(v.getTrangThai())
                    .ngayTao(v.getNgayTao())
                    .ngayCapNhat(v.getNgayCapNhat())
                    .images(imgs)
                    .build();
        }).toList();
    }
}
