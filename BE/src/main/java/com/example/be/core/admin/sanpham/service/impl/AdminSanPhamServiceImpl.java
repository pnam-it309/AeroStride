package com.example.be.core.admin.sanpham.service.impl;

import com.example.be.core.admin.sanpham.mapper.AdminSanPhamMapper;
import com.example.be.core.admin.sanpham.model.request.*;
import com.example.be.core.admin.sanpham.model.response.*;
import com.example.be.core.admin.sanpham.repository.*;
import com.example.be.core.admin.sanpham.service.AdminSanPhamService;
import com.example.be.core.admin.sanpham.service.ProductOptionService;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.entity.*;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.DuplicateResourceException;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.infrastructure.exceptions.ValidationException;
import com.example.be.repository.*;
import com.example.be.utils.CodeUtils;
import com.example.be.utils.SearchUtils;
import lombok.RequiredArgsConstructor;
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
    private final AdminSanPhamMapper adminSanPhamMapper;
    
    // Delegate lookup logic to ProductOptionService (SRP)
    private final ProductOptionService productOptionService;
    
    // Core Repos for resolving entities
    private final ThuongHieuRepository thuongHieuRepository;
    private final DanhMucRepository danhMucRepository;
    private final XuatXuRepository xuatXuRepository;
    private final MucDichChayRepository mucDichChayRepository;
    private final CoGiayRepository coGiayRepository;
    private final ChatLieuRepository chatLieuRepository;
    private final DeGiayRepository deGiayRepository;
    private final MauSacRepository mauSacRepository;
    private final KichThuocRepository kichThuocRepository;

    @Override
    public List<ProductVariantResponse> getVariantsByProductId(String productId) {
        return adminChiTietSanPhamRepository.findBySanPhamIdAndXoaMemFalseOrderByNgayTaoDesc(productId)
                .stream().map(this::mapVariant).toList();
    }

    @Override
    public List<ProductVariantResponse> getAllVariants() {
        return adminChiTietSanPhamRepository.findAllByXoaMemFalse()
                .stream().map(this::mapVariant).toList();
    }

    @Override
    public ProductFormOptionsResponse getFormOptions() {
        return productOptionService.getFormOptions();
    }

    @Override
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request) {
        validateVariantRequests(request.getVariants());
        String code = StringUtils.hasText(request.getMaSanPham()) ? request.getMaSanPham().trim().toUpperCase() : CodeUtils.generateRandom(SanPham.class);
        
        if (adminSanPhamRepository.existsByMaIgnoreCaseAndXoaMemFalse(code)) {
            throw new DuplicateResourceException("Mã sản phẩm đã tồn tại: " + code);
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

    @Override
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

    @Override
    public ProductDetailResponse getProductDetail(String id) {
        return buildProductDetailResponse(id);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(String id, UpdateProductRequest request) {
        SanPham sp = getProductOrThrow(id);
        applyProductData(sp, request, sp.getMa());
        adminSanPhamRepository.save(sp);
        return buildProductDetailResponse(id);
    }

    @Override
    @Transactional
    public void deleteProduct(String id) {
        SanPham sp = getProductOrThrow(id);
        sp.setXoaMem(true);
        sp.setTrangThai(TrangThai.DA_XOA);
        adminSanPhamRepository.save(sp);
        
        adminChiTietSanPhamRepository.findBySanPhamIdAndXoaMemFalse(id).forEach(this::softDeleteVariant);
    }

    @Override
    @Transactional
    public void updateStatus(String id, TrangThai status) {
        SanPham sp = getProductOrThrow(id);
        sp.setTrangThai(status);
        adminSanPhamRepository.save(sp);
        
        adminChiTietSanPhamRepository.findBySanPhamIdAndXoaMemFalse(id).forEach(v -> {
            v.setTrangThai(status);
            adminChiTietSanPhamRepository.save(v);
        });
    }

    @Override
    @Transactional
    public ProductVariantResponse addVariant(String productId, ProductVariantRequest request) {
        return mapVariant(createOrUpdateVariant(null, getProductOrThrow(productId), request));
    }

    @Override
    @Transactional
    public ProductVariantResponse updateVariant(String variantId, ProductVariantRequest request) {
        ChiTietSanPham v = getVariantOrThrow(variantId);
        return mapVariant(createOrUpdateVariant(v, v.getSanPham(), request));
    }

    @Override
    @Transactional
    public void deleteVariant(String variantId) {
        softDeleteVariant(getVariantOrThrow(variantId));
    }

    // --- Helper Methods ---

    private void applyProductData(SanPham sp, Object request, String code) {
        // Use a common interface or reflection if needed, but for now simple check
        if (request instanceof CreateProductRequest req) {
            sp.setTen(req.getTenSanPham());
            sp.setDanhMuc(danhMucRepository.findById(req.getIdDanhMuc()).orElse(null));
            sp.setThuongHieu(thuongHieuRepository.findById(req.getIdThuongHieu()).orElse(null));
            sp.setXuatXu(xuatXuRepository.findById(req.getIdXuatXu()).orElse(null));
            sp.setMucDichChay(mucDichChayRepository.findById(req.getIdMucDichChay()).orElse(null));
            sp.setCoGiay(coGiayRepository.findById(req.getIdCoGiay()).orElse(null));
            sp.setChatLieu(chatLieuRepository.findById(req.getIdChatLieu()).orElse(null));
            sp.setDeGiay(deGiayRepository.findById(req.getIdDeGiay()).orElse(null));
            sp.setGioiTinhKhachHang(req.getGioiTinhKhachHang());
            sp.setHinhAnh(req.getHinhAnh());
            sp.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : TrangThai.DANG_HOAT_DONG);
        } else if (request instanceof UpdateProductRequest req) {
            sp.setTen(req.getTenSanPham());
            sp.setDanhMuc(danhMucRepository.findById(req.getIdDanhMuc()).orElse(null));
            sp.setThuongHieu(thuongHieuRepository.findById(req.getIdThuongHieu()).orElse(null));
            sp.setXuatXu(xuatXuRepository.findById(req.getIdXuatXu()).orElse(null));
            sp.setMucDichChay(mucDichChayRepository.findById(req.getIdMucDichChay()).orElse(null));
            sp.setCoGiay(coGiayRepository.findById(req.getIdCoGiay()).orElse(null));
            sp.setChatLieu(chatLieuRepository.findById(req.getIdChatLieu()).orElse(null));
            sp.setDeGiay(deGiayRepository.findById(req.getIdDeGiay()).orElse(null));
            sp.setGioiTinhKhachHang(req.getGioiTinhKhachHang());
            sp.setHinhAnh(req.getHinhAnh());
            sp.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : TrangThai.DANG_HOAT_DONG);
        }
        sp.setMa(code);
        sp.setXoaMem(false);
    }

    private ChiTietSanPham createOrUpdateVariant(ChiTietSanPham existing, SanPham sp, ProductVariantRequest req) {
        ChiTietSanPham v = existing != null ? existing : new ChiTietSanPham();
        v.setSanPham(sp);
        v.setMauSac(mauSacRepository.findById(req.getIdMauSac()).orElseThrow(() -> new ResourceNotFoundException("Màu sắc không tồn tại")));
        v.setKichThuoc(kichThuocRepository.findById(req.getIdKichThuoc()).orElseThrow(() -> new ResourceNotFoundException("Kích thước không tồn tại")));
        v.setSoLuong(req.getSoLuong());
        v.setGiaNhap(req.getGiaNhap());
        v.setGiaBan(req.getGiaBan());
        v.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : TrangThai.DANG_HOAT_DONG);
        v.setXoaMem(false);
        
        if (v.getMaChiTietSanPham() == null) {
            v.setMaChiTietSanPham(sp.getMa() + "-" + v.getMauSac().getMa() + "-" + v.getKichThuoc().getTen());
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
        return adminChiTietSanPhamRepository.findByIdAndXoaMemFalse(id).orElseThrow(() -> new ResourceNotFoundException("Biến thể không tồn tại"));
    }

    private AnhChiTietSanPham getVariantImageOrThrow(String imageId) {
        return adminAnhChiTietSanPhamRepository.findByIdAndXoaMemFalse(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Ảnh biến thể không tồn tại"));
    }

    private SanPham getProductOrThrow(String id) {
        return adminSanPhamRepository.findByIdAndXoaMemFalse(id).orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
    }

    private void unsetMainImageFlags(String variantId) {
        List<AnhChiTietSanPham> existingImages = adminAnhChiTietSanPhamRepository.findByChiTietSanPhamIdAndXoaMemFalse(variantId);
        existingImages.forEach(image -> image.setHinhAnhDaiDien(false));
        adminAnhChiTietSanPhamRepository.saveAll(existingImages);
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
            if (!keys.add(key)) throw new ValidationException("Dữ liệu biến thể bị trùng lặp tổ hợp màu sắc và kích thước");
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

    @Override
    @Transactional
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

    @Override
    @Transactional
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

    @Override
    @Transactional
    public ProductVariantImageResponse setMainVariantImage(String imageId) {
        AnhChiTietSanPham image = getVariantImageOrThrow(imageId);
        unsetMainImageFlags(image.getChiTietSanPham().getId());
        image.setHinhAnhDaiDien(true);
        image.setTrangThai(TrangThai.DANG_HOAT_DONG);
        image.setXoaMem(false);
        return adminSanPhamMapper.toVariantImageResponse(adminAnhChiTietSanPhamRepository.save(image));
    }

    @Override
    @Transactional
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
    public java.math.BigDecimal getMaxPrice() {
        java.math.BigDecimal maxPrice = adminChiTietSanPhamRepository.findMaxGiaBan();
        return maxPrice != null ? maxPrice : new java.math.BigDecimal("100000000");
    }
}
