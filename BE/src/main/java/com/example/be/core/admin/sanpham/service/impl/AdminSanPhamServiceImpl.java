package com.example.be.core.admin.sanpham.service.impl;

import com.example.be.core.admin.sanpham.mapper.AdminSanPhamMapper;
import com.example.be.core.admin.sanpham.model.request.*;
import com.example.be.core.admin.sanpham.model.response.*;
import com.example.be.core.admin.dotgiamgia.repository.AdminChiTietDotGiamGiaRepository;
import com.example.be.core.admin.sanpham.repository.*;
import com.example.be.core.admin.sanpham.service.AdminSanPhamService;
import com.example.be.core.admin.sanpham.service.ProductOptionService;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.entity.*;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.DuplicateResourceException;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.infrastructure.exceptions.ValidationException;
import com.example.be.repository.*;
import com.example.be.utils.CodeUtils;
import com.example.be.utils.SearchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSanPhamServiceImpl implements AdminSanPhamService {

    private final AdminSanPhamRepository adminSanPhamRepository;
    private final AdminChiTietSanPhamRepository adminChiTietSanPhamRepository;
    private final AdminAnhChiTietSanPhamRepository adminAnhChiTietSanPhamRepository;
    private final AdminChiTietDotGiamGiaRepository adminChiTietDotGiamGiaRepository;
    private final AdminSanPhamMapper adminSanPhamMapper;
    
    // Delegate lookup logic to ProductOptionService (SRP)
    private final ProductOptionService productOptionService;
    
    // Core Repos for resolving entities
    private final ThuongHieuRepository thuongHieuRepository;
    private final XuatXuRepository xuatXuRepository;
    private final MucDichChayRepository mucDichChayRepository;
    private final CoGiayRepository coGiayRepository;
    private final ChatLieuRepository chatLieuRepository;
    private final DeGiayRepository deGiayRepository;
    private final MauSacRepository mauSacRepository;
    private final KichThuocRepository kichThuocRepository;

    // Lấy danh sách biến thể theo ID sản phẩm gốc
    @Override
    public List<ProductVariantResponse> getVariantsByProductId(String productId) {
        return mapVariants(adminChiTietSanPhamRepository.findBySanPhamIdAndXoaMemFalseOrderByNgayTaoDesc(productId));
    }

    // Lấy toàn bộ danh sách biến thể sản phẩm đang hoạt động
    @Override
    public List<ProductVariantResponse> getAllVariants() {
        return mapVariants(adminChiTietSanPhamRepository.findAllByXoaMemFalse());
    }

    // Lấy danh sách các tùy chọn cho form (danh mục, thương hiệu, chất liệu, đế giày...)
    @Override
    public ProductFormOptionsResponse getFormOptions() {
        return productOptionService.getFormOptions();
    }

    // Tạo mới một sản phẩm và các biến thể đi kèm, tự động generate mã sản phẩm nếu chưa có
    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDetailResponse createProduct(CreateProductRequest request) {
        validateVariantRequests(request.getVariants());
        String code = StringUtils.hasText(request.getMaSanPham()) 
                ? request.getMaSanPham().trim().toUpperCase() 
                : CodeUtils.generateRandom(SanPham.class, adminSanPhamRepository::existsByMa);
        
        if (adminSanPhamRepository.existsByMaIgnoreCaseAndXoaMemFalse(code)) {
            throw new DuplicateResourceException(MessageConstants.SAN_PHAM_MA_EXISTS + code);
        }

        SanPham sanPham = new SanPham();
        applyProductData(sanPham, request, code);
        sanPham = adminSanPhamRepository.save(sanPham);

        if (request.getVariants() != null) {
            for (ProductVariantRequest vReq : request.getVariants()) {
                createOrUpdateVariant(null, sanPham, vReq);
            }
        }
        return buildProductDetailResponse(sanPham.getId());
    }

    // Lấy danh sách sản phẩm (có phân trang và tìm kiếm theo điều kiện) kết hợp thông tin tồn kho
    @Override
    @Cacheable(value = "products", key = "#request.toString()", unless = "#result == null")
    public PageResponse<ProductResponse> getProducts(SearchProductRequest request) {
        Specification<SanPham> spec = Specification.where(AdminSanPhamSpecification.notDeleted())
                .and(AdminSanPhamSpecification.hasKeyword(request.getKeyword()))
                .and(AdminSanPhamSpecification.hasTrangThai(request.getTrangThai()));
        
        Page<SanPham> page = SearchUtils.execute(request, pageable -> adminSanPhamRepository.findAll(spec, pageable));
        
        // Load statistics for better UI info
        List<String> ids = page.getContent().stream().map(SanPham::getId).toList();
        Map<String, ProductVariantStatisticsProjection> stats = ids.isEmpty() ? Map.of() : 
            adminChiTietSanPhamRepository.summarizeBySanPhamIds(ids).stream()
                .collect(Collectors.toMap(ProductVariantStatisticsProjection::getSanPhamId, s -> s));

        return PageResponse.from(page.map(sp -> adminSanPhamMapper.toProductResponse(sp, stats.get(sp.getId()))));
    }

    // Lấy thông tin chi tiết một sản phẩm (bao gồm các biến thể)
    @Override
    @Cacheable(value = "productDetail", key = "#id", unless = "#result == null")
    public ProductDetailResponse getProductDetail(String id) {
        return buildProductDetailResponse(id);
    }

    // Cập nhật thông tin chung của sản phẩm gốc
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productDetail", key = "#id")
    })
    public ProductDetailResponse updateProduct(String id, UpdateProductRequest request) {
        SanPham sp = getProductOrThrow(id);
        applyProductData(sp, request, sp.getMa());
        adminSanPhamRepository.save(sp);
        return buildProductDetailResponse(id);
    }

    // Xóa mềm sản phẩm và toàn bộ các biến thể thuộc sản phẩm đó
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productDetail", key = "#id")
    })
    public void deleteProduct(String id) {
        SanPham sp = getProductOrThrow(id);
        sp.setXoaMem(true);
        sp.setTrangThai(TrangThai.DA_XOA);
        adminSanPhamRepository.save(sp);
        
        adminChiTietSanPhamRepository.findBySanPhamIdAndXoaMemFalse(id).forEach(this::softDeleteVariant);
    }

    // Thay đổi trạng thái (hoạt động/ngừng hoạt động) của sản phẩm và cập nhật cả biến thể
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productDetail", key = "#id")
    })
    public void updateStatus(String id, TrangThai status) {
        SanPham sp = getProductOrThrow(id);
        sp.setTrangThai(status);
        adminSanPhamRepository.save(sp);
        
        adminChiTietSanPhamRepository.findBySanPhamIdAndXoaMemFalse(id).forEach(v -> {
            v.setTrangThai(status);
            adminChiTietSanPhamRepository.save(v);
        });
    }

    // Thêm mới một biến thể sản phẩm với màu sắc, kích thước, số lượng cụ thể
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productDetail", key = "#productId")
    })
    public ProductVariantResponse addVariant(String productId, ProductVariantRequest request) {
        return mapVariant(createOrUpdateVariant(null, getProductOrThrow(productId), request));
    }

    // Cập nhật thông tin của một biến thể đã có
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productDetail", key = "#result.idSanPham")
    })
    public ProductVariantResponse updateVariant(String variantId, ProductVariantRequest request) {
        ChiTietSanPham v = getVariantOrThrow(variantId);
        return mapVariant(createOrUpdateVariant(v, v.getSanPham(), request));
    }

    // Xóa mềm một biến thể (không hiển thị ngoài giao diện nữa)
    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteVariant(String variantId) {
        ChiTietSanPham v = getVariantOrThrow(variantId);
        softDeleteVariant(v);
    }

    // --- Helper Methods ---

    private void applyProductData(SanPham sp, Object request, String code) {
        // Use a common interface or reflection if needed, but for now simple check
        if (request instanceof CreateProductRequest req) {
            sp.setTen(req.getTenSanPham());
            sp.setThuongHieu(thuongHieuRepository.findById(req.getIdThuongHieu()).orElse(null));
            sp.setXuatXu(xuatXuRepository.findById(req.getIdXuatXu()).orElse(null));
            sp.setMucDichChay(mucDichChayRepository.findById(req.getIdMucDichChay()).orElse(null));
            sp.setCoGiay(coGiayRepository.findById(req.getIdCoGiay()).orElse(null));
            sp.setChatLieu(chatLieuRepository.findById(req.getIdChatLieu()).orElse(null));
            sp.setDeGiay(deGiayRepository.findById(req.getIdDeGiay()).orElse(null));
            sp.setGioiTinhKhachHang(req.getGioiTinhKhachHang());
            sp.setHinhAnh(req.getHinhAnh());
            sp.setMoTaChiTiet(req.getMoTaChiTiet());
            sp.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : TrangThai.DANG_HOAT_DONG);
        } else if (request instanceof UpdateProductRequest req) {
            sp.setTen(req.getTenSanPham());
            sp.setThuongHieu(thuongHieuRepository.findById(req.getIdThuongHieu()).orElse(null));
            sp.setXuatXu(xuatXuRepository.findById(req.getIdXuatXu()).orElse(null));
            sp.setMucDichChay(mucDichChayRepository.findById(req.getIdMucDichChay()).orElse(null));
            sp.setCoGiay(coGiayRepository.findById(req.getIdCoGiay()).orElse(null));
            sp.setChatLieu(chatLieuRepository.findById(req.getIdChatLieu()).orElse(null));
            sp.setDeGiay(deGiayRepository.findById(req.getIdDeGiay()).orElse(null));
            sp.setGioiTinhKhachHang(req.getGioiTinhKhachHang());
            sp.setHinhAnh(req.getHinhAnh());
            sp.setMoTaChiTiet(req.getMoTaChiTiet());
            sp.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : TrangThai.DANG_HOAT_DONG);
        }
        sp.setMa(code);
        sp.setXoaMem(false);
    }

    private ChiTietSanPham createOrUpdateVariant(ChiTietSanPham existing, SanPham sp, ProductVariantRequest req) {
        if (existing == null) {
            if (adminChiTietSanPhamRepository.existsBySanPhamIdAndMauSacIdAndKichThuocIdAndXoaMemFalse(
                    sp.getId(), req.getIdMauSac(), req.getIdKichThuoc())) {
                throw new DuplicateResourceException(MessageConstants.SAN_PHAM_VARIANT_DUPLICATE);
            }
        } else {
            if (adminChiTietSanPhamRepository.existsBySanPhamIdAndMauSacIdAndKichThuocIdAndXoaMemFalseAndIdNot(
                    sp.getId(), req.getIdMauSac(), req.getIdKichThuoc(), existing.getId())) {
                throw new DuplicateResourceException(MessageConstants.SAN_PHAM_VARIANT_DUPLICATE);
            }
        }

        ChiTietSanPham v = existing != null ? existing : new ChiTietSanPham();
        v.setSanPham(sp);
        v.setMauSac(mauSacRepository.findById(req.getIdMauSac()).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.MAU_SAC_NOT_FOUND)));
        v.setKichThuoc(kichThuocRepository.findById(req.getIdKichThuoc()).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.KICH_THUOC_NOT_FOUND)));
        v.setSoLuong(req.getSoLuong());
        v.setGiaNhap(req.getGiaNhap());
        v.setGiaBan(req.getGiaBan());
        v.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : TrangThai.DANG_HOAT_DONG);
        v.setXoaMem(false);
        
        if (v.getMaChiTietSanPham() == null || v.getMaChiTietSanPham().trim().isEmpty()) {
            List<String> existingMas = adminChiTietSanPhamRepository.findBySanPhamIdAndXoaMemFalse(sp.getId()).stream()
                    .map(ChiTietSanPham::getMaChiTietSanPham)
                    .filter(Objects::nonNull)
                    .toList();

            String baseMa = sp.getMa();
            int suffix = 1;
            String generatedMa;
            do {
                generatedMa = String.format("%s-%02d", baseMa, suffix++);
            } while (existingMas.contains(generatedMa) || adminChiTietSanPhamRepository.existsByMaChiTietSanPhamIgnoreCaseAndXoaMemFalse(generatedMa));

            v.setMaChiTietSanPham(generatedMa);
        }
        
        v = adminChiTietSanPhamRepository.save(v);
        
        // Handle images if provided
        if (req.getImages() != null && !req.getImages().isEmpty()) {
            syncVariantImages(v, req.getImages());
        }
        return v;
    }

    private void syncVariantImages(ChiTietSanPham variant, List<ProductVariantImageRequest> imageRequests) {
        List<AnhChiTietSanPham> existingImages = adminAnhChiTietSanPhamRepository
                .findByChiTietSanPhamIdAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(variant.getId());

        Map<String, AnhChiTietSanPham> existingImagesByUrl = existingImages.stream()
                .filter(image -> StringUtils.hasText(image.getDuongDanAnh()))
                .collect(Collectors.toMap(
                        AnhChiTietSanPham::getDuongDanAnh,
                        image -> image,
                        (current, ignored) -> current,
                        LinkedHashMap::new
                ));

        boolean hasIncomingMainImage = imageRequests.stream()
                .anyMatch(imageRequest -> Boolean.TRUE.equals(imageRequest.getHinhAnhDaiDien()));

        if (hasIncomingMainImage) {
            existingImages.forEach(image -> image.setHinhAnhDaiDien(false));
            adminAnhChiTietSanPhamRepository.saveAll(existingImages);
        }

        imageRequests.forEach(imageRequest -> {
            AnhChiTietSanPham image = existingImagesByUrl.getOrDefault(imageRequest.getDuongDanAnh(), new AnhChiTietSanPham());
            image.setChiTietSanPham(variant);
            image.setDuongDanAnh(imageRequest.getDuongDanAnh());
            image.setMoTa(imageRequest.getMoTa());
            image.setHinhAnhDaiDien(Boolean.TRUE.equals(imageRequest.getHinhAnhDaiDien()));
            image.setTrangThai(imageRequest.getTrangThai() != null ? imageRequest.getTrangThai() : TrangThai.DANG_HOAT_DONG);
            image.setXoaMem(false);
            adminAnhChiTietSanPhamRepository.save(image);
        });
    }

    private void softDeleteVariant(ChiTietSanPham v) {
        v.setXoaMem(true);
        v.setTrangThai(TrangThai.DA_XOA);
        adminChiTietSanPhamRepository.save(v);
        adminAnhChiTietSanPhamRepository.findByChiTietSanPhamIdAndXoaMemFalse(v.getId()).forEach(img -> {
            img.setXoaMem(true);
            img.setTrangThai(TrangThai.DA_XOA);
            adminAnhChiTietSanPhamRepository.save(img);
        });
    }

    private ProductDetailResponse buildProductDetailResponse(String id) {
        SanPham sp = getProductOrThrow(id);
        List<ProductVariantResponse> variants = getVariantsByProductId(id);
        return adminSanPhamMapper.toProductDetailResponse(sp, variants);
    }

    private ChiTietSanPham getVariantOrThrow(String id) {
        return adminChiTietSanPhamRepository.findByIdAndXoaMemFalse(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.BIEN_THE_NOT_FOUND));
    }

    private AnhChiTietSanPham getVariantImageOrThrow(String imageId) {
        return adminAnhChiTietSanPhamRepository.findByIdAndXoaMemFalse(imageId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.ANH_BIEN_THE_NOT_FOUND));
    }

    private SanPham getProductOrThrow(String id) {
        return adminSanPhamRepository.findByIdAndXoaMemFalse(id).orElseThrow(() -> new ResourceNotFoundException(MessageConstants.SAN_PHAM_NOT_FOUND));
    }

    private void unsetMainImageFlags(String variantId) {
        List<AnhChiTietSanPham> existingImages = adminAnhChiTietSanPhamRepository.findByChiTietSanPhamIdAndXoaMemFalse(variantId);
        existingImages.forEach(image -> image.setHinhAnhDaiDien(false));
        adminAnhChiTietSanPhamRepository.saveAll(existingImages);
    }

    private List<ProductVariantResponse> mapVariants(List<ChiTietSanPham> variants) {
        if (variants == null || variants.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> ids = variants.stream().map(ChiTietSanPham::getId).toList();

        // 1. Bulk-fetch images
        List<AnhChiTietSanPham> images = adminAnhChiTietSanPhamRepository
                .findAllByChiTietSanPhamIdInAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(ids);
        
        Map<String, List<ProductVariantImageResponse>> imageMap = images.stream()
                .filter(img -> img.getChiTietSanPham() != null)
                .collect(Collectors.groupingBy(
                        img -> img.getChiTietSanPham().getId(),
                        Collectors.mapping(adminSanPhamMapper::toVariantImageResponse, Collectors.toList())
                ));

        // 2. Bulk-fetch campaign details
        List<ChiTietDotGiamGia> relations = adminChiTietDotGiamGiaRepository.findAllByChiTietSanPhamIdIn(ids);
        
        Map<String, List<ChiTietDotGiamGia>> relationMap = relations.stream()
                .filter(rel -> rel.getChiTietSanPham() != null)
                .collect(Collectors.groupingBy(
                        rel -> rel.getChiTietSanPham().getId()
                ));

        // 3. Map to responses in memory
        return variants.stream().map(v -> {
            v.setChiTietDotGiamGias(new java.util.LinkedHashSet<>(relationMap.getOrDefault(v.getId(), new ArrayList<>())));
            List<ProductVariantImageResponse> imgs = imageMap.getOrDefault(v.getId(), new ArrayList<>());
            return adminSanPhamMapper.toVariantResponse(v, imgs);
        }).toList();
    }

    private ProductVariantResponse mapVariant(ChiTietSanPham v) {
        List<ProductVariantImageResponse> imgs = adminAnhChiTietSanPhamRepository
                .findByChiTietSanPhamIdAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(v.getId())
                .stream().map(adminSanPhamMapper::toVariantImageResponse).toList();
        return adminSanPhamMapper.toVariantResponse(v, imgs);
    }

    private void validateVariantRequests(List<ProductVariantRequest> reqs) {
        if (reqs == null) return;
        Set<String> keys = new HashSet<>();
        for (ProductVariantRequest r : reqs) {
            String key = r.getIdMauSac() + "-" + r.getIdKichThuoc();
            if (!keys.add(key)) throw new ValidationException(MessageConstants.SAN_PHAM_VARIANT_DUPLICATE);
        }
    }

    // Implementing required but unused interface methods to maintain contract
    @Override
    public byte[] exportExcel() {
        return new byte[0];
    }

    @Override
    public byte[] downloadTemplate() {
        return new byte[0];
    }

    @Override
    @Transactional
    public void importExcel(org.springframework.web.multipart.MultipartFile file) {
        // Implementation for product import
    }

    // Thêm hình ảnh mới cho một biến thể cụ thể
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productDetail", allEntries = true)
    })
    public ProductVariantImageResponse addVariantImage(String variantId, ProductVariantImageRequest request) {
        ChiTietSanPham variant = getVariantOrThrow(variantId);

        if (Boolean.TRUE.equals(request.getHinhAnhDaiDien())) {
            unsetMainImageFlags(variantId);
        }

        AnhChiTietSanPham image = new AnhChiTietSanPham();
        image.setChiTietSanPham(variant);
        image.setDuongDanAnh(request.getDuongDanAnh());
        image.setMoTa(request.getMoTa());
        image.setHinhAnhDaiDien(Boolean.TRUE.equals(request.getHinhAnhDaiDien()));
        image.setTrangThai(request.getTrangThai() != null ? request.getTrangThai() : TrangThai.DANG_HOAT_DONG);
        image.setXoaMem(false);

        return adminSanPhamMapper.toVariantImageResponse(adminAnhChiTietSanPhamRepository.save(image));
    }

    // Cập nhật thuộc tính của hình ảnh biến thể (VD: đổi thành ảnh đại diện)
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productDetail", allEntries = true)
    })
    public ProductVariantImageResponse updateVariantImage(String imageId, UpdateProductVariantImageRequest request) {
        AnhChiTietSanPham image = getVariantImageOrThrow(imageId);

        if (StringUtils.hasText(request.getDuongDanAnh())) {
            image.setDuongDanAnh(request.getDuongDanAnh().trim());
        }
        if (request.getMoTa() != null) {
            image.setMoTa(request.getMoTa());
        }
        if (request.getTrangThai() != null) {
            image.setTrangThai(request.getTrangThai());
        }
        if (request.getHinhAnhDaiDien() != null) {
            if (Boolean.TRUE.equals(request.getHinhAnhDaiDien())) {
                unsetMainImageFlags(image.getChiTietSanPham().getId());
            }
            image.setHinhAnhDaiDien(request.getHinhAnhDaiDien());
        }

        image.setXoaMem(false);
        return adminSanPhamMapper.toVariantImageResponse(adminAnhChiTietSanPhamRepository.save(image));
    }

    // Thiết lập một hình ảnh thành ảnh đại diện chính của biến thể
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productDetail", allEntries = true)
    })
    public ProductVariantImageResponse setMainVariantImage(String imageId) {
        AnhChiTietSanPham image = getVariantImageOrThrow(imageId);
        unsetMainImageFlags(image.getChiTietSanPham().getId());
        image.setHinhAnhDaiDien(true);
        image.setTrangThai(TrangThai.DANG_HOAT_DONG);
        image.setXoaMem(false);
        return adminSanPhamMapper.toVariantImageResponse(adminAnhChiTietSanPhamRepository.save(image));
    }

    // Xóa một hình ảnh khỏi biến thể, nếu là ảnh chính sẽ tự động đổi ảnh khác lên thay
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productDetail", allEntries = true)
    })
    public void deleteVariantImage(String imageId) {
        AnhChiTietSanPham image = getVariantImageOrThrow(imageId);
        String variantId = image.getChiTietSanPham().getId();
        boolean wasMainImage = Boolean.TRUE.equals(image.getHinhAnhDaiDien());

        image.setXoaMem(true);
        image.setTrangThai(TrangThai.DA_XOA);
        image.setHinhAnhDaiDien(false);
        adminAnhChiTietSanPhamRepository.save(image);

        if (wasMainImage) {
            adminAnhChiTietSanPhamRepository.findFirstByChiTietSanPhamIdAndXoaMemFalseOrderByNgayTaoAsc(variantId)
                    .ifPresent(nextImage -> {
                        nextImage.setHinhAnhDaiDien(true);
                        nextImage.setTrangThai(TrangThai.DANG_HOAT_DONG);
                        adminAnhChiTietSanPhamRepository.save(nextImage);
                    });
        }
    }

    @Override
    public List<ProductVariantResponse> searchVariantsForAi(String keyword, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, Integer limit) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit != null ? limit : 20);
        List<ChiTietSanPham> variants = adminChiTietSanPhamRepository.searchVariantsForAi(keyword, minPrice, maxPrice, pageable);
        return mapVariants(variants);
    }

    @Override
    public java.math.BigDecimal getMaxPrice() {
        return adminChiTietSanPhamRepository.findFirstByXoaMemFalseOrderByGiaBanDesc()
                .map(ChiTietSanPham::getGiaBan)
                .orElse(new java.math.BigDecimal("6500000"));
    }
}
