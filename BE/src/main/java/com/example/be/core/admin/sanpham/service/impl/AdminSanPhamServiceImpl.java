package com.example.be.core.admin.sanpham.service.impl;

import com.example.be.core.admin.sanpham.mapper.AdminSanPhamMapper;
import com.example.be.core.admin.sanpham.model.request.CreateProductRequest;
import com.example.be.core.admin.sanpham.model.request.ProductVariantImageRequest;
import com.example.be.core.admin.sanpham.model.request.ProductVariantRequest;
import com.example.be.core.admin.sanpham.model.request.SearchProductRequest;
import com.example.be.core.admin.sanpham.model.request.UpdateProductRequest;
import com.example.be.core.admin.sanpham.model.request.UpdateProductVariantImageRequest;
import com.example.be.core.admin.sanpham.model.response.ProductDetailResponse;
import com.example.be.core.admin.sanpham.model.response.ProductFormOptionsResponse;
import com.example.be.core.admin.sanpham.model.response.ProductOptionResponse;
import com.example.be.core.admin.sanpham.model.response.ProductResponse;
import com.example.be.core.admin.sanpham.model.response.ProductVariantImageResponse;
import com.example.be.core.admin.sanpham.model.response.ProductVariantResponse;
import com.example.be.core.admin.sanpham.repository.AdminAnhChiTietSanPhamRepository;
import com.example.be.core.admin.sanpham.repository.AdminChiTietSanPhamRepository;
import com.example.be.core.admin.sanpham.repository.AdminSanPhamRepository;
import com.example.be.core.admin.sanpham.repository.AdminSanPhamSpecification;
import com.example.be.core.admin.sanpham.repository.ProductVariantStatisticsProjection;
import com.example.be.core.admin.sanpham.service.AdminSanPhamService;
import com.example.be.core.common.base.BaseCodeNameEntity;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.entity.AnhChiTietSanPham;
import com.example.be.entity.ChatLieu;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.CoGiay;
import com.example.be.entity.DanhMuc;
import com.example.be.entity.DeGiay;
import com.example.be.entity.KichThuoc;
import com.example.be.entity.MauSac;
import com.example.be.entity.MucDichChay;
import com.example.be.entity.SanPham;
import com.example.be.entity.ThuongHieu;
import com.example.be.entity.XuatXu;
import com.example.be.infrastructure.constants.GioiTinhKhachHang;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.DuplicateResourceException;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.infrastructure.exceptions.ValidationException;
import com.example.be.repository.ChatLieuRepository;
import com.example.be.repository.CoGiayRepository;
import com.example.be.repository.DanhMucRepository;
import com.example.be.repository.DeGiayRepository;
import com.example.be.repository.KichThuocRepository;
import com.example.be.repository.MauSacRepository;
import com.example.be.repository.MucDichChayRepository;
import com.example.be.repository.ThuongHieuRepository;
import com.example.be.repository.XuatXuRepository;
import com.example.be.utils.ExcelUtils;
import com.example.be.utils.MaGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSanPhamServiceImpl implements AdminSanPhamService {

    private final AdminSanPhamRepository adminSanPhamRepository;
    private final AdminChiTietSanPhamRepository adminChiTietSanPhamRepository;
    private final AdminAnhChiTietSanPhamRepository adminAnhChiTietSanPhamRepository;

    private final ThuongHieuRepository thuongHieuRepository;
    private final DanhMucRepository danhMucRepository;
    private final XuatXuRepository xuatXuRepository;
    private final MucDichChayRepository mucDichChayRepository;
    private final CoGiayRepository coGiayRepository;
    private final ChatLieuRepository chatLieuRepository;
    private final DeGiayRepository deGiayRepository;
    private final MauSacRepository mauSacRepository;
    private final KichThuocRepository kichThuocRepository;

    private final AdminSanPhamMapper adminSanPhamMapper;

    @Override
    public List<ProductVariantResponse> getVariantsByProductId(String productId) {
        return adminChiTietSanPhamRepository
                .findBySanPhamIdAndXoaMemFalseOrderByNgayTaoDesc(productId)
                .stream()
                .map(this::mapVariant)
                .toList();
    }

    @Override
    public ProductFormOptionsResponse getFormOptions() {
        Sort sortByName = Sort.by(Sort.Direction.ASC, "ten");
        return ProductFormOptionsResponse.builder()
                .danhMucs(mapOptions(danhMucRepository.findAll(sortByName)))
                .thuongHieus(mapOptions(thuongHieuRepository.findAll(sortByName)))
                .xuatXus(mapOptions(xuatXuRepository.findAll(sortByName)))
                .mucDichChays(mapOptions(mucDichChayRepository.findAll(sortByName)))
                .coGiays(mapCoGiayOptions(coGiayRepository.findAll(sortByName)))
                .chatLieus(mapOptions(chatLieuRepository.findAll(sortByName)))
                .deGiays(mapOptions(deGiayRepository.findAll(sortByName)))
                .mauSacs(mapMauSacOptions(mauSacRepository.findAll(sortByName)))
                .kichThuocs(mapKichThuocOptions(kichThuocRepository.findAll(sortByName)))
                .gioiTinhKhachHangs(Arrays.stream(GioiTinhKhachHang.values()).map(Enum::name).toList())
                .trangThais(Arrays.stream(TrangThai.values())
                        .filter(trangThai -> trangThai != TrangThai.DA_XOA)
                        .map(Enum::name)
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request) {
        validateVariantRequests(request.getVariants());

        String normalizedProductCode = normalizeCode(request.getMaSanPham());
        if (normalizedProductCode == null || normalizedProductCode.trim().isEmpty()) {
            normalizedProductCode = MaGenerator.generate(SanPham.class);
        }

        if (adminSanPhamRepository.existsByMaIgnoreCaseAndXoaMemFalse(normalizedProductCode)) {
            throw new DuplicateResourceException("Ma san pham da ton tai: " + normalizedProductCode);
        }

        SanPham sanPham = new SanPham();
        applyProductData(sanPham, request, normalizedProductCode);
        sanPham = adminSanPhamRepository.save(sanPham);

        createVariantsForProduct(sanPham, request.getVariants());
        return buildProductDetailResponse(sanPham.getId());
    }

    @Override
    public PageResponse<ProductResponse> getProducts(SearchProductRequest request) {
        Specification<SanPham> specification = Specification.where(AdminSanPhamSpecification.notDeleted())
                .and(AdminSanPhamSpecification.hasKeyword(request.getKeyword()))
                .and(AdminSanPhamSpecification.hasDanhMuc(request.getDanhMucId()))
                .and(AdminSanPhamSpecification.hasThuongHieu(request.getThuongHieuId()))
                .and(AdminSanPhamSpecification.hasTrangThai(request.getTrangThai()))
                .and(AdminSanPhamSpecification.hasGioiTinhKhachHang(request.getGioiTinhKhachHang()))
                .and(AdminSanPhamSpecification.hasXuatXu(request.getXuatXuId()))
                .and(AdminSanPhamSpecification.hasMucDichChay(request.getMucDichChayId()))
                .and(AdminSanPhamSpecification.hasChatLieu(request.getChatLieuId()));

        Page<SanPham> sanPhamPage = adminSanPhamRepository.findAll(specification, buildPageable(request));
        Map<String, ProductVariantStatisticsProjection> statisticsByProductId = loadStatisticsMap(sanPhamPage.getContent());

        Page<ProductResponse> responsePage = sanPhamPage.map(sanPham ->
                adminSanPhamMapper.toProductResponse(sanPham, statisticsByProductId.get(sanPham.getId()))
        );
        return PageResponse.from(responsePage);
    }

    @Override
    public ProductDetailResponse getProductDetail(String id) {
        return buildProductDetailResponse(id);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(String id, UpdateProductRequest request) {
        SanPham sanPham = getProductOrThrow(id);
        String normalizedProductCode = normalizeCode(request.getMaSanPham());

        if (normalizedProductCode != null
                && adminSanPhamRepository.existsByMaIgnoreCaseAndXoaMemFalseAndIdNot(normalizedProductCode, id)) {
            throw new DuplicateResourceException("Ma san pham da ton tai: " + normalizedProductCode);
        }

        applyProductData(sanPham, request, normalizedProductCode);
        adminSanPhamRepository.save(sanPham);

        // Update variants if provided in the request
        // Note: UpdateProductRequest doesn't have variants field in current implementation
        // This functionality can be added later if needed
        // if (request.getVariants() != null) {
        //     updateVariantsForProduct(sanPham, request.getVariants());
        // }

        return buildProductDetailResponse(id);
    }

    private void updateVariantsForProduct(SanPham sanPham, List<ProductVariantRequest> variantRequests) {
        // Simple implementation: delete old NOT in list and update/add new
        // For now, let's use the safer approach of updating existing or adding new
        for (ProductVariantRequest variantRequest : variantRequests) {
            String mauSacId = variantRequest.getIdMauSac();
            String kichThuocId = variantRequest.getIdKichThuoc();
            
            Optional<ChiTietSanPham> existing = adminChiTietSanPhamRepository
                .findBySanPhamIdAndMauSacIdAndKichThuocIdAndXoaMemFalse(sanPham.getId(), mauSacId, kichThuocId);
            
            if (existing.isPresent()) {
                createOrUpdateVariant(existing.get(), sanPham, variantRequest);
            } else {
                createOrUpdateVariant(null, sanPham, variantRequest);
            }
        }
    }

    @Override
    @Transactional
    public void deleteProduct(String id) {
        SanPham sanPham = getProductOrThrow(id);
        softDeleteProduct(sanPham);
    }

    @Override
    @Transactional
    public void updateStatus(String id, TrangThai status) {
        SanPham sanPham = getProductOrThrow(id);
        sanPham.setTrangThai(status);
        adminSanPhamRepository.save(sanPham);
    }

    @Override
    @Transactional
    public ProductVariantResponse addVariant(String productId, ProductVariantRequest request) {
        validateVariantRequests(List.of(request));
        SanPham sanPham = getProductOrThrow(productId);
        ChiTietSanPham variant = createOrUpdateVariant(null, sanPham, request);
        return buildVariantResponse(variant.getId());
    }

    @Override
    @Transactional
    public ProductVariantResponse updateVariant(String variantId, ProductVariantRequest request) {
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            throw new ValidationException("Cap nhat anh bien the qua endpoint /variant-images");
        }
        ChiTietSanPham existingVariant = getVariantOrThrow(variantId);
        createOrUpdateVariant(existingVariant, existingVariant.getSanPham(), request);
        return buildVariantResponse(variantId);
    }

    @Override
    @Transactional
    public void deleteVariant(String variantId) {
        ChiTietSanPham variant = getVariantOrThrow(variantId);
        softDeleteVariant(variant);
    }

    @Override
    @Transactional
    public ProductVariantImageResponse addVariantImage(String variantId, ProductVariantImageRequest request) {
        ChiTietSanPham variant = getVariantOrThrow(variantId);
        AnhChiTietSanPham image = new AnhChiTietSanPham();
        image.setChiTietSanPham(variant);
        image.setDuongDanAnh(normalizeRequiredText(request.getDuongDanAnh(), "duongDanAnh"));
        image.setMoTa(normalizeText(request.getMoTa()));
        image.setTrangThai(resolveWriteStatus(request.getTrangThai(), "trangThai anh"));
        image.setXoaMem(Boolean.FALSE);

        List<AnhChiTietSanPham> activeImages = adminAnhChiTietSanPhamRepository
                .findByChiTietSanPhamIdAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(variantId);
        if (Boolean.TRUE.equals(request.getHinhAnhDaiDien())) {
            clearRepresentativeImages(activeImages);
            image.setHinhAnhDaiDien(Boolean.TRUE);
        } else {
            image.setHinhAnhDaiDien(activeImages.isEmpty());
        }

        image = adminAnhChiTietSanPhamRepository.save(image);
        return adminSanPhamMapper.toVariantImageResponse(image);
    }

    @Override
    @Transactional
    public ProductVariantImageResponse updateVariantImage(String imageId, UpdateProductVariantImageRequest request) {
        AnhChiTietSanPham image = getVariantImageOrThrow(imageId);
        List<AnhChiTietSanPham> activeImages = adminAnhChiTietSanPhamRepository
                .findByChiTietSanPhamIdAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(image.getChiTietSanPham().getId());

        if (request.getDuongDanAnh() != null) {
            image.setDuongDanAnh(normalizeRequiredText(request.getDuongDanAnh(), "duongDanAnh"));
        }
        if (request.getMoTa() != null) {
            image.setMoTa(normalizeText(request.getMoTa()));
        }
        if (request.getTrangThai() != null) {
            image.setTrangThai(resolveWriteStatus(request.getTrangThai(), "trangThai anh"));
        }
        if (request.getHinhAnhDaiDien() != null) {
            handleRepresentativeFlagUpdate(image, activeImages, request.getHinhAnhDaiDien());
        }

        image = adminAnhChiTietSanPhamRepository.save(image);
        return adminSanPhamMapper.toVariantImageResponse(image);
    }

    @Override
    @Transactional
    public void deleteVariantImage(String imageId) {
        AnhChiTietSanPham image = getVariantImageOrThrow(imageId);
        boolean wasRepresentative = Boolean.TRUE.equals(image.getHinhAnhDaiDien());
        String variantId = image.getChiTietSanPham().getId();

        image.setXoaMem(Boolean.TRUE);
        image.setTrangThai(TrangThai.DA_XOA);
        image.setHinhAnhDaiDien(Boolean.FALSE);
        adminAnhChiTietSanPhamRepository.save(image);

        if (wasRepresentative) {
            adminAnhChiTietSanPhamRepository
                    .findFirstByChiTietSanPhamIdAndXoaMemFalseOrderByNgayTaoAsc(variantId)
                    .ifPresent(remainingImage -> {
                        remainingImage.setHinhAnhDaiDien(Boolean.TRUE);
                        adminAnhChiTietSanPhamRepository.save(remainingImage);
                    });
        }
    }

    private ProductDetailResponse buildProductDetailResponse(String productId) {
        SanPham sanPham = getProductOrThrow(productId);
        List<ProductVariantResponse> variants = adminChiTietSanPhamRepository
                .findBySanPhamIdAndXoaMemFalseOrderByNgayTaoDesc(productId)
                .stream()
                .map(this::mapVariant)
                .toList();
        return adminSanPhamMapper.toProductDetailResponse(sanPham, variants);
    }

    private ProductVariantResponse buildVariantResponse(String variantId) {
        return mapVariant(getVariantOrThrow(variantId));
    }

    private ProductVariantResponse mapVariant(ChiTietSanPham variant) {
        List<ProductVariantImageResponse> images = adminAnhChiTietSanPhamRepository
                .findByChiTietSanPhamIdAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(variant.getId())
                .stream()
                .map(adminSanPhamMapper::toVariantImageResponse)
                .toList();
        return adminSanPhamMapper.toVariantResponse(variant, images);
    }

    private void applyProductData(SanPham sanPham, CreateProductRequest request, String normalizedProductCode) {
        sanPham.setMa(normalizedProductCode);
        sanPham.setTen(normalizeRequiredText(request.getTenSanPham(), "tenSanPham"));
        sanPham.setDanhMuc(resolveDanhMuc(request.getIdDanhMuc()));
        sanPham.setThuongHieu(resolveThuongHieu(request.getIdThuongHieu()));
        sanPham.setXuatXu(resolveXuatXu(request.getIdXuatXu()));
        sanPham.setMucDichChay(resolveMucDichChay(request.getIdMucDichChay()));
        sanPham.setCoGiay(resolveCoGiay(request.getIdCoGiay()));
        sanPham.setChatLieu(resolveChatLieu(request.getIdChatLieu()));
        sanPham.setDeGiay(resolveDeGiay(request.getIdDeGiay()));
        if (request.getGioiTinhKhachHang() == null) {
            throw new ValidationException("gioiTinhKhachHang khong duoc de trong");
        }
        sanPham.setGioiTinhKhachHang(request.getGioiTinhKhachHang());
        sanPham.setMoTaNgan(normalizeText(request.getMoTaNgan()));
        sanPham.setMoTaChiTiet(normalizeText(request.getMoTaChiTiet()));
        sanPham.setHinhAnh(normalizeText(request.getHinhAnh()));
        sanPham.setTrangThai(resolveWriteStatus(request.getTrangThai(), "trangThai san pham"));
        sanPham.setXoaMem(Boolean.FALSE);
    }

    private void applyProductData(SanPham sanPham, UpdateProductRequest request, String normalizedProductCode) {
        if (normalizedProductCode != null) {
            sanPham.setMa(normalizedProductCode);
        }
        sanPham.setTen(normalizeRequiredText(request.getTenSanPham(), "tenSanPham"));
        sanPham.setDanhMuc(resolveDanhMuc(request.getIdDanhMuc()));
        sanPham.setThuongHieu(resolveThuongHieu(request.getIdThuongHieu()));
        sanPham.setXuatXu(resolveXuatXu(request.getIdXuatXu()));
        sanPham.setMucDichChay(resolveMucDichChay(request.getIdMucDichChay()));
        sanPham.setCoGiay(resolveCoGiay(request.getIdCoGiay()));
        sanPham.setChatLieu(resolveChatLieu(request.getIdChatLieu()));
        sanPham.setDeGiay(resolveDeGiay(request.getIdDeGiay()));
        if (request.getGioiTinhKhachHang() == null) {
            throw new ValidationException("gioiTinhKhachHang khong duoc de trong");
        }
        sanPham.setGioiTinhKhachHang(request.getGioiTinhKhachHang());
        sanPham.setMoTaNgan(normalizeText(request.getMoTaNgan()));
        sanPham.setMoTaChiTiet(normalizeText(request.getMoTaChiTiet()));
        sanPham.setHinhAnh(normalizeText(request.getHinhAnh()));
        sanPham.setTrangThai(resolveWriteStatus(request.getTrangThai(), "trangThai san pham"));
        sanPham.setXoaMem(Boolean.FALSE);
    }

    private void createVariantsForProduct(SanPham sanPham, List<ProductVariantRequest> variantRequests) {
        if (variantRequests == null || variantRequests.isEmpty()) {
            return;
        }
        for (ProductVariantRequest variantRequest : variantRequests) {
            createOrUpdateVariant(null, sanPham, variantRequest);
        }
    }

    private ChiTietSanPham createOrUpdateVariant(ChiTietSanPham existingVariant, SanPham sanPham, ProductVariantRequest request) {
        MauSac mauSac = resolveMauSac(request.getIdMauSac());
        KichThuoc kichThuoc = resolveKichThuoc(request.getIdKichThuoc());
        String normalizedVariantCode = normalizeCode(request.getMaChiTietSanPham());

        validateVariantUniqueness(sanPham.getId(), mauSac.getId(), kichThuoc.getId(), existingVariant != null ? existingVariant.getId() : null);
        if (normalizedVariantCode != null) {
            validateVariantCodeUniqueness(normalizedVariantCode, existingVariant != null ? existingVariant.getId() : null);
        }

        ChiTietSanPham variant = existingVariant != null ? existingVariant : new ChiTietSanPham();
        variant.setSanPham(sanPham);
        variant.setMauSac(mauSac);
        variant.setKichThuoc(kichThuoc);
        if (normalizedVariantCode != null || existingVariant == null) {
            variant.setMaChiTietSanPham(normalizedVariantCode);
        }
        variant.setSoLuong(request.getSoLuong());
        variant.setGiaNhap(request.getGiaNhap());
        variant.setGiaBan(request.getGiaBan());
        variant.setTrangThai(resolveWriteStatus(request.getTrangThai(), "trangThai bien the"));
        variant.setXoaMem(Boolean.FALSE);
        variant = adminChiTietSanPhamRepository.save(variant);

        if (existingVariant == null && request.getImages() != null && !request.getImages().isEmpty()) {
            createImagesForVariant(variant, request.getImages());
        }
        return variant;
    }

    private void createImagesForVariant(ChiTietSanPham variant, List<ProductVariantImageRequest> imageRequests) {
        assertSingleRepresentative(imageRequests);
        boolean representativeAssigned = false;
        for (ProductVariantImageRequest imageRequest : imageRequests) {
            AnhChiTietSanPham image = new AnhChiTietSanPham();
            image.setChiTietSanPham(variant);
            image.setDuongDanAnh(normalizeRequiredText(imageRequest.getDuongDanAnh(), "duongDanAnh"));
            image.setMoTa(normalizeText(imageRequest.getMoTa()));
            image.setTrangThai(resolveWriteStatus(imageRequest.getTrangThai(), "trangThai anh"));
            image.setXoaMem(Boolean.FALSE);

            boolean shouldBeRepresentative = Boolean.TRUE.equals(imageRequest.getHinhAnhDaiDien()) || (!representativeAssigned && !containsRepresentative(imageRequests));
            image.setHinhAnhDaiDien(shouldBeRepresentative);
            representativeAssigned = representativeAssigned || shouldBeRepresentative;

            adminAnhChiTietSanPhamRepository.save(image);
        }
    }

    private void softDeleteProduct(SanPham sanPham) {
        sanPham.setXoaMem(Boolean.TRUE);
        sanPham.setTrangThai(TrangThai.DA_XOA);
        adminSanPhamRepository.save(sanPham);

        List<ChiTietSanPham> variants = adminChiTietSanPhamRepository.findBySanPhamIdAndXoaMemFalse(sanPham.getId());
        for (ChiTietSanPham variant : variants) {
            softDeleteVariant(variant);
        }
    }

    private void softDeleteVariant(ChiTietSanPham variant) {
        variant.setXoaMem(Boolean.TRUE);
        variant.setTrangThai(TrangThai.DA_XOA);
        adminChiTietSanPhamRepository.save(variant);

        List<AnhChiTietSanPham> images = adminAnhChiTietSanPhamRepository.findByChiTietSanPhamIdAndXoaMemFalse(variant.getId());
        for (AnhChiTietSanPham image : images) {
            image.setXoaMem(Boolean.TRUE);
            image.setTrangThai(TrangThai.DA_XOA);
            image.setHinhAnhDaiDien(Boolean.FALSE);
            adminAnhChiTietSanPhamRepository.save(image);
        }
    }

    private void handleRepresentativeFlagUpdate(
            AnhChiTietSanPham image,
            List<AnhChiTietSanPham> activeImages,
            Boolean requestedRepresentativeFlag
    ) {
        if (Boolean.TRUE.equals(requestedRepresentativeFlag)) {
            clearRepresentativeImages(activeImages);
            image.setHinhAnhDaiDien(Boolean.TRUE);
            return;
        }

        if (!Boolean.TRUE.equals(image.getHinhAnhDaiDien())) {
            image.setHinhAnhDaiDien(Boolean.FALSE);
            return;
        }

        AnhChiTietSanPham replacement = activeImages.stream()
                .filter(existingImage -> !existingImage.getId().equals(image.getId()))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Bien the phai co it nhat mot anh dai dien dang hoat dong"));

        replacement.setHinhAnhDaiDien(Boolean.TRUE);
        adminAnhChiTietSanPhamRepository.save(replacement);
        image.setHinhAnhDaiDien(Boolean.FALSE);
    }

    private List<ProductOptionResponse> mapOptions(List<? extends BaseCodeNameEntity> entities) {
        return entities.stream()
                .filter(Objects::nonNull)
                .filter(entity -> !Boolean.TRUE.equals(resolveSoftDelete(entity)))
                .filter(entity -> entity.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                .map(entity -> ProductOptionResponse.builder()
                        .id(entity.getId())
                        .ma(entity.getMa())
                        .ten(entity.getTen())
                        .build())
                .toList();
    }

    private List<ProductOptionResponse> mapMauSacOptions(List<MauSac> entities) {
        return entities.stream()
                .filter(Objects::nonNull)
                .filter(entity -> !Boolean.TRUE.equals(entity.getXoaMem()))
                .filter(entity -> entity.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                .map(entity -> ProductOptionResponse.builder()
                        .id(entity.getId())
                        .ma(entity.getMa())
                        .ten(entity.getTen())
                        .moTa(entity.getMaMauHex())
                        .build())
                .toList();
    }

    private List<ProductOptionResponse> mapKichThuocOptions(List<KichThuoc> entities) {
        return entities.stream()
                .filter(Objects::nonNull)
                .filter(entity -> !Boolean.TRUE.equals(entity.getXoaMem()))
                .filter(entity -> entity.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                .map(entity -> ProductOptionResponse.builder()
                        .id(entity.getId())
                        .ma(entity.getMa())
                        .ten(entity.getTen())
                        .moTa(entity.getGiaTriKichThuoc())
                        .build())
                .toList();
    }

    private List<ProductOptionResponse> mapCoGiayOptions(List<CoGiay> entities) {
        return entities.stream()
                .filter(Objects::nonNull)
                .filter(entity -> !Boolean.TRUE.equals(entity.getXoaMem()))
                .filter(entity -> entity.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                .map(entity -> ProductOptionResponse.builder()
                        .id(entity.getId())
                        .ma(entity.getMa())
                        .ten(entity.getTen())
                        .moTa(entity.getGiaTriKichThuoc())
                        .build())
                .toList();
    }

    private Map<String, ProductVariantStatisticsProjection> loadStatisticsMap(List<SanPham> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }
        List<String> productIds = products.stream().map(SanPham::getId).toList();
        return adminChiTietSanPhamRepository.summarizeBySanPhamIds(productIds)
                .stream()
                .collect(Collectors.toMap(ProductVariantStatisticsProjection::getSanPhamId, statistics -> statistics));
    }

    private Pageable buildPageable(SearchProductRequest request) {
        int page = Math.max(0, request.getPage());
        int size = request.getSize() <= 0 ? 10 : request.getSize();
        String sortBy = resolveSortBy(request.getSortBy());
        Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    private String resolveSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return "ngayTao";
        }
        return switch (sortBy.trim()) {
            case "ma", "maSanPham", "ma_san_pham" -> "ma";
            case "ten", "tenSanPham", "ten_san_pham" -> "ten";
            case "trangThai" -> "trangThai";
            case "ngayCapNhat", "updatedAt" -> "ngayCapNhat";
            case "tenThuongHieu" -> "thuongHieu.ten";
            case "tenDanhMuc" -> "danhMuc.ten";
            default -> "ngayTao";
        };
    }

    private void validateVariantRequests(List<ProductVariantRequest> variantRequests) {
        if (variantRequests == null || variantRequests.isEmpty()) {
            return;
        }
        Set<String> combinations = new HashSet<>();
        for (ProductVariantRequest variantRequest : variantRequests) {
            String combinationKey = normalizeRequiredText(variantRequest.getIdMauSac(), "idMauSac")
                    + "::"
                    + normalizeRequiredText(variantRequest.getIdKichThuoc(), "idKichThuoc");
            if (!combinations.add(combinationKey)) {
                throw new ValidationException("Danh sach bien the co to hop mau sac - kich thuoc bi trung");
            }
            assertSingleRepresentative(variantRequest.getImages());
        }
    }

    private void assertSingleRepresentative(List<ProductVariantImageRequest> imageRequests) {
        if (imageRequests == null || imageRequests.isEmpty()) {
            return;
        }
        long representativeCount = imageRequests.stream()
                .filter(imageRequest -> Boolean.TRUE.equals(imageRequest.getHinhAnhDaiDien()))
                .count();
        if (representativeCount > 1) {
            throw new ValidationException("Moi bien the chi duoc co toi da mot anh dai dien");
        }
    }

    private boolean containsRepresentative(List<ProductVariantImageRequest> imageRequests) {
        if (imageRequests == null || imageRequests.isEmpty()) {
            return false;
        }
        return imageRequests.stream().anyMatch(imageRequest -> Boolean.TRUE.equals(imageRequest.getHinhAnhDaiDien()));
    }

    private void clearRepresentativeImages(List<AnhChiTietSanPham> activeImages) {
        for (AnhChiTietSanPham existingImage : activeImages) {
            if (Boolean.TRUE.equals(existingImage.getHinhAnhDaiDien())) {
                existingImage.setHinhAnhDaiDien(Boolean.FALSE);
                adminAnhChiTietSanPhamRepository.save(existingImage);
            }
        }
    }

    private void validateVariantUniqueness(String productId, String mauSacId, String kichThuocId, String excludeId) {
        boolean exists = excludeId == null
                ? adminChiTietSanPhamRepository.existsBySanPhamIdAndMauSacIdAndKichThuocIdAndXoaMemFalse(productId, mauSacId, kichThuocId)
                : adminChiTietSanPhamRepository.existsBySanPhamIdAndMauSacIdAndKichThuocIdAndXoaMemFalseAndIdNot(productId, mauSacId, kichThuocId, excludeId);
        if (exists) {
            throw new DuplicateResourceException("Da ton tai bien the voi to hop san pham - mau sac - kich thuoc nay");
        }
    }

    private void validateVariantCodeUniqueness(String variantCode, String excludeId) {
        boolean exists = excludeId == null
                ? adminChiTietSanPhamRepository.existsByMaChiTietSanPhamIgnoreCaseAndXoaMemFalse(variantCode)
                : adminChiTietSanPhamRepository.existsByMaChiTietSanPhamIgnoreCaseAndXoaMemFalseAndIdNot(variantCode, excludeId);
        if (exists) {
            throw new DuplicateResourceException("Ma chi tiet san pham da ton tai: " + variantCode);
        }
    }

    private SanPham getProductOrThrow(String productId) {
        return adminSanPhamRepository.findByIdAndXoaMemFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham voi id: " + productId));
    }

    private ChiTietSanPham getVariantOrThrow(String variantId) {
        return adminChiTietSanPhamRepository.findByIdAndXoaMemFalse(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay bien the voi id: " + variantId));
    }

    private AnhChiTietSanPham getVariantImageOrThrow(String imageId) {
        return adminAnhChiTietSanPhamRepository.findByIdAndXoaMemFalse(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay anh bien the voi id: " + imageId));
    }

    private ThuongHieu resolveThuongHieu(String id) {
        ThuongHieu entity = thuongHieuRepository.findById(normalizeRequiredText(id, "idThuongHieu"))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay thuong hieu voi id: " + id));
        validateReferenceState("Thuong hieu", entity.getTrangThai(), entity.getXoaMem());
        return entity;
    }

    private DanhMuc resolveDanhMuc(String id) {
        DanhMuc entity = danhMucRepository.findById(normalizeRequiredText(id, "idDanhMuc"))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh muc voi id: " + id));
        validateReferenceState("Danh muc", entity.getTrangThai(), entity.getXoaMem());
        return entity;
    }

    private XuatXu resolveXuatXu(String id) {
        XuatXu entity = xuatXuRepository.findById(normalizeRequiredText(id, "idXuatXu"))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay xuat xu voi id: " + id));
        validateReferenceState("Xuat xu", entity.getTrangThai(), entity.getXoaMem());
        return entity;
    }

    private MucDichChay resolveMucDichChay(String id) {
        MucDichChay entity = mucDichChayRepository.findById(normalizeRequiredText(id, "idMucDichChay"))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay muc dich chay voi id: " + id));
        validateReferenceState("Muc dich chay", entity.getTrangThai(), entity.getXoaMem());
        return entity;
    }

    private CoGiay resolveCoGiay(String id) {
        CoGiay entity = coGiayRepository.findById(normalizeRequiredText(id, "idCoGiay"))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay co giay voi id: " + id));
        validateReferenceState("Co giay", entity.getTrangThai(), entity.getXoaMem());
        return entity;
    }

    private ChatLieu resolveChatLieu(String id) {
        ChatLieu entity = chatLieuRepository.findById(normalizeRequiredText(id, "idChatLieu"))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay chat lieu voi id: " + id));
        validateReferenceState("Chat lieu", entity.getTrangThai(), entity.getXoaMem());
        return entity;
    }

    private DeGiay resolveDeGiay(String id) {
        DeGiay entity = deGiayRepository.findById(normalizeRequiredText(id, "idDeGiay"))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay de giay voi id: " + id));
        validateReferenceState("De giay", entity.getTrangThai(), entity.getXoaMem());
        return entity;
    }

    private MauSac resolveMauSac(String id) {
        MauSac entity = mauSacRepository.findById(normalizeRequiredText(id, "idMauSac"))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay mau sac voi id: " + id));
        validateReferenceState("Mau sac", entity.getTrangThai(), entity.getXoaMem());
        return entity;
    }

    private KichThuoc resolveKichThuoc(String id) {
        KichThuoc entity = kichThuocRepository.findById(normalizeRequiredText(id, "idKichThuoc"))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay kich thuoc voi id: " + id));
        validateReferenceState("Kich thuoc", entity.getTrangThai(), entity.getXoaMem());
        return entity;
    }

    private void validateReferenceState(String resourceName, TrangThai trangThai, Boolean xoaMem) {
        if (Boolean.TRUE.equals(xoaMem) || trangThai == TrangThai.DA_XOA) {
            throw new ValidationException(resourceName + " da bi xoa mem hoac khong con hieu luc");
        }
        if (trangThai == TrangThai.KHONG_HOAT_DONG) {
            throw new ValidationException(resourceName + " dang khong hoat dong");
        }
    }

    private TrangThai resolveWriteStatus(TrangThai requestedStatus, String fieldName) {
        if (requestedStatus == null) {
            return TrangThai.DANG_HOAT_DONG;
        }
        if (requestedStatus == TrangThai.DA_XOA) {
            throw new ValidationException(fieldName + " khong duoc set la DA_XOA khi tao hoac cap nhat");
        }
        return requestedStatus;
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRequiredText(String value, String fieldName) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            throw new ValidationException(fieldName + " khong duoc de trong");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Boolean resolveSoftDelete(BaseCodeNameEntity entity) {
        if (entity instanceof DanhMuc danhMuc) {
            return danhMuc.getXoaMem();
        }
        if (entity instanceof ThuongHieu thuongHieu) {
            return thuongHieu.getXoaMem();
        }
        if (entity instanceof XuatXu xuatXu) {
            return xuatXu.getXoaMem();
        }
        if (entity instanceof MucDichChay mucDichChay) {
            return mucDichChay.getXoaMem();
        }
        if (entity instanceof ChatLieu chatLieu) {
            return chatLieu.getXoaMem();
        }
        if (entity instanceof DeGiay deGiay) {
            return deGiay.getXoaMem();
        }
        return Boolean.FALSE;
    }
    @Override
    public byte[] exportExcel() {
        Specification<SanPham> specification = Specification.where(AdminSanPhamSpecification.notDeleted());
        List<SanPham> sanPhams = adminSanPhamRepository.findAll(specification);
        Map<String, ProductVariantStatisticsProjection> statisticsByProductId = loadStatisticsMap(sanPhams);

        List<ProductResponse> data = sanPhams.stream()
                .map(sanPham -> adminSanPhamMapper.toProductResponse(sanPham, statisticsByProductId.get(sanPham.getId())))
                .toList();

        String[] headers = {"STT", "Mã SP", "Tên sản phẩm", "Danh mục", "Thương hiệu", "Hành động", "Sẵn có", "Số lượng", "Trạng thái"};

        try {
            return ExcelUtils.exportToExcel("Danh sách sản phẩm", headers, data, item -> new Object[]{
                data.indexOf(item) + 1,
                item.getMaSanPham(),
                item.getTenSanPham(),
                item.getTenDanhMuc(),
                item.getTenThuongHieu(),
                item.getTongBienThe() > 0 ? item.getTongBienThe() + " biến thể" : "Chưa có biến thể",
                "Xem chi tiết",
                item.getTongSoLuongTon(),
                item.getTrangThai() == TrangThai.DANG_HOAT_DONG ? "Đang kinh doanh" : "Ngừng kinh doanh"
            });
        } catch (IOException e) {
            throw new RuntimeException("Lỗi xuất file Excel: " + e.getMessage());
        }
    }
    @Override
    public byte[] downloadTemplate() {
        String[] headers = {
            "Mã sản phẩm", "Tên sản phẩm", "Mô tả ngắn", 
            "Mô tả chi tiết", "Giới tính (NAM/NU/UNISEX)", 
            "Danh mục", "Thương hiệu", "Xuất xứ", 
            "Mục đích chạy", "Cổ giày", "Chất liệu", "Đế giày"
        };
        
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Template Nhập Sản Phẩm");
            Row headerRow = sheet.createRow(0);
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Example row
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("SP_AUTO_001");
            exampleRow.createCell(1).setCellValue("Giày Chạy Bộ Aero v1");
            exampleRow.createCell(2).setCellValue("Giày chạy bộ cao cấp");
            exampleRow.createCell(3).setCellValue("Mô tả chi tiết về sản phẩm...");
            exampleRow.createCell(4).setCellValue("NAM");
            exampleRow.createCell(5).setCellValue("Giày Chạy Bộ");
            exampleRow.createCell(6).setCellValue("AeroStride");
            exampleRow.createCell(7).setCellValue("Việt Nam");
            exampleRow.createCell(8).setCellValue("Chạy Road");
            exampleRow.createCell(9).setCellValue("Cổ Thấp");
            exampleRow.createCell(10).setCellValue("Mesh");
            exampleRow.createCell(11).setCellValue("Cao Su");

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi tạo template: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void importExcel(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            
            // Skip header
            if (rows.hasNext()) rows.next();
            
            // Load caches for rapid lookup
            Map<String, DanhMuc> categoryMap = danhMucRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getTen().toLowerCase(), e -> e, (e1, e2) -> e1));
            Map<String, ThuongHieu> brandMap = thuongHieuRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getTen().toLowerCase(), e -> e, (e1, e2) -> e1));
            Map<String, XuatXu> originMap = xuatXuRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getTen().toLowerCase(), e -> e, (e1, e2) -> e1));
            Map<String, MucDichChay> purposeMap = mucDichChayRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getTen().toLowerCase(), e -> e, (e1, e2) -> e1));
            Map<String, CoGiay> collarMap = coGiayRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getTen().toLowerCase(), e -> e, (e1, e2) -> e1));
            Map<String, ChatLieu> materialMap = chatLieuRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getTen().toLowerCase(), e -> e, (e1, e2) -> e1));
            Map<String, DeGiay> soleMap = deGiayRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getTen().toLowerCase(), e -> e, (e1, e2) -> e1));

            List<SanPham> sanPhamsToSave = new ArrayList<>();

            while (rows.hasNext()) {
                Row row = rows.next();
                String ma = ExcelUtils.getCellValueAsString(row.getCell(0));
                String ten = ExcelUtils.getCellValueAsString(row.getCell(1));
                
                if (!StringUtils.hasText(ten)) continue;

                SanPham sp = new SanPham();
                sp.setMa(StringUtils.hasText(ma) ? ma.toUpperCase() : "SP_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                sp.setTen(ten);
                sp.setMoTaNgan(ExcelUtils.getCellValueAsString(row.getCell(2)));
                sp.setMoTaChiTiet(ExcelUtils.getCellValueAsString(row.getCell(3)));
                
                String genderStr = ExcelUtils.getCellValueAsString(row.getCell(4)).toUpperCase();
                try {
                    sp.setGioiTinhKhachHang(GioiTinhKhachHang.valueOf(genderStr));
                } catch (Exception e) {
                    sp.setGioiTinhKhachHang(GioiTinhKhachHang.UNISEX);
                }

                sp.setDanhMuc(categoryMap.get(ExcelUtils.getCellValueAsString(row.getCell(5)).toLowerCase()));
                sp.setThuongHieu(brandMap.get(ExcelUtils.getCellValueAsString(row.getCell(6)).toLowerCase()));
                sp.setXuatXu(originMap.get(ExcelUtils.getCellValueAsString(row.getCell(7)).toLowerCase()));
                sp.setMucDichChay(purposeMap.get(ExcelUtils.getCellValueAsString(row.getCell(8)).toLowerCase()));
                sp.setCoGiay(collarMap.get(ExcelUtils.getCellValueAsString(row.getCell(9)).toLowerCase()));
                sp.setChatLieu(materialMap.get(ExcelUtils.getCellValueAsString(row.getCell(10)).toLowerCase()));
                sp.setDeGiay(soleMap.get(ExcelUtils.getCellValueAsString(row.getCell(11)).toLowerCase()));
                
                sp.setTrangThai(TrangThai.DANG_HOAT_DONG);
                sp.setXoaMem(false);
                sanPhamsToSave.add(sp);
            }
            
            adminSanPhamRepository.saveAll(sanPhamsToSave);
            
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file Excel: " + e.getMessage());
        }
    }
}
