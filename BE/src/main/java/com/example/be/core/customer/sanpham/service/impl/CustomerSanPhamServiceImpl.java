package com.example.be.core.customer.sanpham.service.impl;

import com.example.be.core.common.base.BaseCodeNameEntity;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.core.customer.sanpham.model.request.CustomerSearchProductRequest;
import com.example.be.core.customer.sanpham.model.request.RecommendQuizRequest;
import com.example.be.core.customer.sanpham.model.response.*;
import com.example.be.core.customer.sanpham.repository.*;
import com.example.be.core.customer.sanpham.service.CustomerSanPhamService;
import com.example.be.entity.*;
import com.example.be.repository.HoaDonChiTietRepository;
import com.example.be.infrastructure.constants.GioiTinhKhachHang;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.utils.SearchUtils;
import com.example.be.utils.CodeUtils;
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
    private final HoaDonChiTietRepository hoaDonChiTietRepository;

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
        // Multi-select or single specs
        Specification<SanPham> mucDichSpec = null;
        List<String> effectiveMucDich = request.getEffectiveMucDichChayIds();
        if (effectiveMucDich != null && !effectiveMucDich.isEmpty()) {
            mucDichSpec = CustomerSanPhamSpecification.hasMucDichChayIn(effectiveMucDich);
        }

        Specification<SanPham> thuongHieuSpec = null;
        List<String> effectiveThuongHieu = request.getEffectiveThuongHieuIds();
        if (effectiveThuongHieu != null && !effectiveThuongHieu.isEmpty()) {
            thuongHieuSpec = CustomerSanPhamSpecification.hasThuongHieuIn(effectiveThuongHieu);
        }

        Specification<SanPham> chatLieuSpec = null;
        List<String> effectiveChatLieu = request.getEffectiveChatLieuIds();
        if (effectiveChatLieu != null && !effectiveChatLieu.isEmpty()) {
            chatLieuSpec = CustomerSanPhamSpecification.hasChatLieuIn(effectiveChatLieu);
        }

        Specification<SanPham> xuatXuSpec = null;
        List<String> effectiveXuatXu = request.getEffectiveXuatXuIds();
        if (effectiveXuatXu != null && !effectiveXuatXu.isEmpty()) {
            xuatXuSpec = CustomerSanPhamSpecification.hasXuatXuIn(effectiveXuatXu);
        }

        Specification<SanPham> kichThuocSpec = null;
        List<String> effectiveKichThuocs = request.getEffectiveKichThuocs();
        if (effectiveKichThuocs != null && !effectiveKichThuocs.isEmpty()) {
            kichThuocSpec = CustomerSanPhamSpecification.hasKichThuocIn(effectiveKichThuocs);
        }

        Specification<SanPham> spec = Specification.where(CustomerSanPhamSpecification.notDeleted())
                .and(CustomerSanPhamSpecification.hasKeyword(request.getKeyword()))
                .and(CustomerSanPhamSpecification.hasTrangThai(request.getTrangThai() != null ? request.getTrangThai() : TrangThai.DANG_HOAT_DONG))
                .and(thuongHieuSpec)
                .and(CustomerSanPhamSpecification.hasGioiTinhKhachHang(request.getGioiTinhKhachHang()))
                .and(xuatXuSpec)
                .and(mucDichSpec)
                .and(chatLieuSpec)
                .and(CustomerSanPhamSpecification.hasMinGia(request.getMinGia()))
                .and(CustomerSanPhamSpecification.hasMaxGia(request.getMaxGia()))
                .and(kichThuocSpec);

        // Detect if price sorting is requested (use raw value before getSortBy() transforms it)
        String originalSortBy = request.getRawSortBy();
        boolean isPriceSort = "price_asc".equals(originalSortBy) || "price_desc".equals(originalSortBy);

        Page<SanPham> page = SearchUtils.execute(request, pageable -> customerSanPhamRepository.findAll(spec, pageable));

        List<String> ids = page.getContent().stream().map(SanPham::getId).toList();
        Map<String, CustomerProductVariantStats> stats = ids.isEmpty() ? Map.of() :
                customerSanPhamChiTietRepository.summarizeBySanPhamIds(ids).stream()
                        .collect(Collectors.toMap(CustomerProductVariantStats::getSanPhamId, s -> s));

        Map<String, String> firstVariantImages = new java.util.HashMap<>();
        if (!ids.isEmpty()) {
            List<Object[]> variantImages = customerSanPhamAnhChiTietRepository.findFirstVariantImagesBySanPhamIds(ids);
            for (Object[] row : variantImages) {
                String spId = (String) row[0];
                String imgUrl = (String) row[1];
                if (spId != null && imgUrl != null && !imgUrl.trim().isEmpty()) {
                    firstVariantImages.putIfAbsent(spId, imgUrl);
                }
            }
        }

        PageResponse<CustomerProductResponse> result = PageResponse.from(page.map(sp -> {
            CustomerProductVariantStats s = stats.get(sp.getId());
            String varImg = firstVariantImages.get(sp.getId());
            String finalImg = (varImg != null && !varImg.trim().isEmpty()) ? varImg : sp.getHinhAnh();

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
                    .hinhAnh(finalImg)
                    .trangThai(sp.getTrangThai())
                    .ngayTao(sp.getNgayTao())
                    .ngayCapNhat(sp.getNgayCapNhat())
                    .tongBienThe(s != null ? s.getTongBienThe() : 0L)
                    .tongSoLuongTon(s != null ? s.getTongSoLuongTon() : 0L)
                    .giaBanThapNhat(s != null ? s.getGiaBanThapNhat() : null)
                    .giaBanCaoNhat(s != null ? s.getGiaBanCaoNhat() : null)
                    .build();
        }));

        // Price-based sorting at Java level (giaBanThapNhat is computed from variants, not a DB column)
        if (isPriceSort && result.getContent() != null && !result.getContent().isEmpty()) {
            List<CustomerProductResponse> sorted = new java.util.ArrayList<>(result.getContent());
            if ("price_asc".equals(originalSortBy)) {
                sorted.sort(Comparator.comparing(
                        CustomerProductResponse::getGiaBanThapNhat,
                        Comparator.nullsLast(Comparator.naturalOrder())));
            } else {
                sorted.sort(Comparator.comparing(
                        CustomerProductResponse::getGiaBanThapNhat,
                        Comparator.nullsLast(Comparator.reverseOrder())));
            }
            result.setContent(sorted);
        }

        return result;
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
        SanPham sp = customerSanPhamRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.SAN_PHAM_NOT_FOUND));

        if (sp.getTrangThai() != TrangThai.DANG_HOAT_DONG) {
            throw new ResourceNotFoundException("Sản phẩm này đã ngừng bán hoặc không khả dụng.");
        }

        List<ChiTietSanPham> variants = customerSanPhamChiTietRepository.findBySanPhamIdAndXoaMemFalseOrderByNgayTaoDesc(id)
                .stream()
                .filter(v -> v.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                .filter(v -> v.getMauSac() != null && v.getMauSac().getTrangThai() == TrangThai.DANG_HOAT_DONG && !Boolean.TRUE.equals(v.getMauSac().getXoaMem()))
                .filter(v -> v.getKichThuoc() != null && v.getKichThuoc().getTrangThai() == TrangThai.DANG_HOAT_DONG && !Boolean.TRUE.equals(v.getKichThuoc().getXoaMem()))
                .toList();

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

        String productMainImg = sp.getHinhAnh();
        if (productMainImg == null || productMainImg.trim().isEmpty() || productMainImg.equalsIgnoreCase("null") || productMainImg.equalsIgnoreCase("undefined")) {
            for (CustomerProductVariantResponse v : variantResponses) {
                if (v.getImages() != null && !v.getImages().isEmpty()) {
                    for (var img : v.getImages()) {
                        if (img != null && img.getDuongDanAnh() != null && !img.getDuongDanAnh().trim().isEmpty() && !img.getDuongDanAnh().equalsIgnoreCase("null")) {
                            productMainImg = img.getDuongDanAnh();
                            break;
                        }
                    }
                    if (productMainImg != null) break;
                }
            }
        }

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
                .hinhAnh(productMainImg)
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
                .daBan(hoaDonChiTietRepository.countSoldQuantityBySanPhamId(sp.getId()))
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
                for (ChiTietDotGiamGia ct : v.getChiTietDotGiamGias()) {
                    DotGiamGia d = ct.getDotGiamGia();
                    if (d != null && d.getTrangThai() == TrangThai.DANG_HOAT_DONG) {
                        if (d.getNgayBatDau() != null && d.getNgayKetThuc() != null
                                && d.getNgayBatDau() <= now && now <= d.getNgayKetThuc()) {
                            BigDecimal val = ct.getGiaTriGiam() != null ? ct.getGiaTriGiam() : d.getSoTienGiam();
                            if (val != null && val.compareTo(activeDiscount) > 0) {
                                activeDiscount = val;
                            }
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
                    .giaBan(v.getGiaBan())
                    .phanTramGiam(activeDiscount)
                    .trangThai(v.getTrangThai())
                    .ngayTao(v.getNgayTao())
                    .ngayCapNhat(v.getNgayCapNhat())
                    .images(imgs)
                    .build();
        }).toList();
    }

    @Override
    public RecommendQuizResponse getRecommendQuiz(RecommendQuizRequest request) {
        Map<String, String> answers = request.getAnswers();
        if (answers == null) {
            answers = new HashMap<>();
        }

        // Bước 1: Hỏi giới tính
        String gioiTinh = answers.get("gioiTinh");
        if (gioiTinh == null || gioiTinh.isBlank()) {
            return RecommendQuizResponse.builder()
                    .nextQuestion(RecommendQuizQuestion.builder()
                            .key("gioiTinh")
                            .questionText("Bạn đang tìm giày dành cho đối tượng nào?")
                            .options(List.of(
                                    new RecommendQuizOption("Nam", "NAM"),
                                    new RecommendQuizOption("Nữ", "NU"),
                                    new RecommendQuizOption("Unisex (Cả nam và nữ)", "UNISEX")
                            ))
                            .build())
                    .recommendedProducts(Collections.emptyList())
                    .build();
        }

        // Bước 2: Hỏi mục đích sử dụng
        String mucDichChay = answers.get("mucDichChay");
        if (mucDichChay == null || mucDichChay.isBlank()) {
            List<RecommendQuizOption> purposeOptions = mucDichChayRepository.findAll().stream()
                    .filter(x -> x.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .map(x -> new RecommendQuizOption(x.getTen(), x.getId()))
                    .collect(Collectors.toList());

            return RecommendQuizResponse.builder()
                    .nextQuestion(RecommendQuizQuestion.builder()
                            .key("mucDichChay")
                            .questionText("Mục đích sử dụng chính của đôi giày này là gì?")
                            .options(purposeOptions)
                            .build())
                    .recommendedProducts(Collections.emptyList())
                    .build();
        }

        // Bước 3: Hỏi thương hiệu
        String thuongHieu = answers.get("thuongHieu");
        if (thuongHieu == null || thuongHieu.isBlank()) {
            List<RecommendQuizOption> brandOptions = thuongHieuRepository.findAll().stream()
                    .filter(x -> x.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .map(x -> new RecommendQuizOption(x.getTen(), x.getId()))
                    .collect(Collectors.toList());
            brandOptions.add(0, new RecommendQuizOption("Tất cả thương hiệu / Không quan trọng", "ALL"));

            return RecommendQuizResponse.builder()
                    .nextQuestion(RecommendQuizQuestion.builder()
                            .key("thuongHieu")
                            .questionText("Bạn yêu thích hoặc đang hướng tới thương hiệu nào?")
                            .options(brandOptions)
                            .build())
                    .recommendedProducts(Collections.emptyList())
                    .build();
        }

        // Bước 4: Hỏi kích cỡ
        String kichThuoc = answers.get("kichThuoc");
        if (kichThuoc == null || kichThuoc.isBlank()) {
            List<RecommendQuizOption> sizeOptions = kichThuocRepository.findAll().stream()
                    .filter(x -> x.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                    .sorted((a, b) -> {
                        try {
                            double valA = Double.parseDouble(a.getTen());
                            double valB = Double.parseDouble(b.getTen());
                            return Double.compare(valA, valB);
                        } catch (Exception e) {
                            return a.getTen().compareTo(b.getTen());
                        }
                    })
                    .map(x -> new RecommendQuizOption("Size " + x.getTen(), x.getId()))
                    .collect(Collectors.toList());

            return RecommendQuizResponse.builder()
                    .nextQuestion(RecommendQuizQuestion.builder()
                            .key("kichThuoc")
                            .questionText("Kích cỡ giày của bạn là bao nhiêu (Size EU)?")
                            .options(sizeOptions)
                            .build())
                    .recommendedProducts(Collections.emptyList())
                    .build();
        }

        // Bước 5: Hỏi khoảng giá mong muốn
        String khoangGia = answers.get("khoangGia");
        if (khoangGia == null || khoangGia.isBlank()) {
            return RecommendQuizResponse.builder()
                    .nextQuestion(RecommendQuizQuestion.builder()
                            .key("khoangGia")
                            .questionText("Mức giá mong muốn cho đôi giày của bạn?")
                            .options(List.of(
                                    new RecommendQuizOption("Dưới 1 triệu VNĐ", "UNDER_1M"),
                                    new RecommendQuizOption("Từ 1 - 2 triệu VNĐ", "1M_TO_2M"),
                                    new RecommendQuizOption("Trên 2 triệu VNĐ", "OVER_2M"),
                                    new RecommendQuizOption("Bất kỳ mức giá nào", "ALL")
                            ))
                            .build())
                    .recommendedProducts(Collections.emptyList())
                    .build();
        }

        List<CustomerProductResponse> finalRecommended = calculateRecommendedProducts(answers);
        return RecommendQuizResponse.builder()
                .nextQuestion(null)
                .recommendedProducts(finalRecommended)
                .build();
    }

    private List<CustomerProductResponse> calculateRecommendedProducts(Map<String, String> answers) {
        if (answers == null || answers.isEmpty()) {
            CustomerSearchProductRequest defaultReq = new CustomerSearchProductRequest();
            defaultReq.setPage(0);
            defaultReq.setSize(12);
            defaultReq.setTrangThai(TrangThai.DANG_HOAT_DONG);
            return getProducts(defaultReq).getContent();
        }

        String gioiTinh = answers.get("gioiTinh");
        String mucDichChay = answers.get("mucDichChay");
        String thuongHieu = answers.get("thuongHieu");
        String kichThuoc = answers.get("kichThuoc");
        String khoangGia = answers.get("khoangGia");

        CustomerSearchProductRequest searchReq = new CustomerSearchProductRequest();
        searchReq.setPage(0);
        searchReq.setSize(100);
        searchReq.setTrangThai(TrangThai.DANG_HOAT_DONG);

        if (gioiTinh != null && !gioiTinh.isBlank() && !"UNISEX".equalsIgnoreCase(gioiTinh)) {
            searchReq.setGioiTinhKhachHang(gioiTinh);
        }
        if (thuongHieu != null && !thuongHieu.isBlank() && !"ALL".equalsIgnoreCase(thuongHieu)) {
            searchReq.setThuongHieuId(thuongHieu);
        }
        if (mucDichChay != null && !mucDichChay.isBlank()) {
            searchReq.setMucDichChayId(mucDichChay);
        }

        PageResponse<CustomerProductResponse> baseProductsPage = getProducts(searchReq);
        List<CustomerProductResponse> baseProducts = baseProductsPage.getContent();

        List<CustomerProductResponse> recommended = baseProducts.stream()
                .filter(sp -> {
                    List<ChiTietSanPham> variants = customerSanPhamChiTietRepository
                            .findBySanPhamIdAndXoaMemFalse(sp.getId());

                    if (variants == null || variants.isEmpty()) {
                        return false;
                    }

                    if (kichThuoc != null && !kichThuoc.isBlank()) {
                        boolean matchSize = variants.stream()
                                .anyMatch(v -> v.getKichThuoc() != null 
                                        && v.getKichThuoc().getId().equals(kichThuoc)
                                        && TrangThai.DANG_HOAT_DONG.equals(v.getTrangThai())
                                        && v.getSoLuong() > 0);
                        if (!matchSize) return false;
                    }

                    if (khoangGia == null || khoangGia.isBlank() || "ALL".equalsIgnoreCase(khoangGia)) {
                        return true;
                    }

                    return variants.stream().anyMatch(v -> {
                        if (!TrangThai.DANG_HOAT_DONG.equals(v.getTrangThai()) || v.getSoLuong() <= 0) {
                            return false;
                        }
                        BigDecimal price = v.getGiaBan() != null ? v.getGiaBan() : BigDecimal.ZERO;
                        double val = price.doubleValue();
                        if ("UNDER_1M".equalsIgnoreCase(khoangGia)) {
                            return val < 1000000.0;
                        } else if ("1M_TO_2M".equalsIgnoreCase(khoangGia)) {
                            return val >= 1000000.0 && val <= 2000000.0;
                        } else if ("OVER_2M".equalsIgnoreCase(khoangGia)) {
                            return val > 2000000.0;
                        }
                        return false;
                    });
                })
                .collect(Collectors.toList());

        if (recommended.isEmpty()) {
            recommended = baseProducts;
        }
        if (recommended.isEmpty()) {
            CustomerSearchProductRequest fallbackReq = new CustomerSearchProductRequest();
            fallbackReq.setPage(0);
            fallbackReq.setSize(12);
            fallbackReq.setTrangThai(TrangThai.DANG_HOAT_DONG);
            recommended = getProducts(fallbackReq).getContent();
        }

        return recommended;
    }
}
