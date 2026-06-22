package com.example.be.core.customer.landing.service.impl;

import com.example.be.core.customer.landing.model.response.CustomerLandingFeatureItemResponse;
import com.example.be.core.customer.landing.model.response.CustomerLandingProductResponse;
import com.example.be.core.customer.landing.model.response.CustomerLandingVariantResponse;
import com.example.be.core.customer.landing.model.response.CustomerLandingVariantResponse;
import com.example.be.core.customer.landing.repository.CustomerLandingRepository;
import com.example.be.core.customer.landing.service.CustomerLandingService;
import com.example.be.core.customer.sanpham.model.response.CustomerProductVariantStats;
import com.example.be.core.customer.sanpham.repository.CustomerSanPhamAnhChiTietRepository;
import com.example.be.core.customer.sanpham.repository.CustomerSanPhamChiTietRepository;
import com.example.be.core.customer.sanpham.repository.CustomerSanPhamChiTietDotGiamGiaRepository;
import com.example.be.core.customer.sanpham.repository.CustomerSanPhamSpecification;
import com.example.be.entity.AnhChiTietSanPham;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.ChiTietDotGiamGia;
import com.example.be.entity.DotGiamGia;
import com.example.be.entity.SanPham;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.be.core.customer.sanpham.model.response.CustomerProductFormOptionsResponse;
import com.example.be.core.customer.sanpham.service.CustomerSanPhamService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerLandingServiceImpl implements CustomerLandingService {

    private final CustomerLandingRepository landingRepository;
    private final CustomerSanPhamChiTietRepository chiTietSanPhamRepository;
    private final CustomerSanPhamAnhChiTietRepository anhChiTietSanPhamRepository;
    private final CustomerSanPhamChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository;
    private final CustomerSanPhamService sanPhamService;

    @Override
    public List<CustomerLandingProductResponse> getLandingProducts(Integer size) {
        Specification<SanPham> spec = Specification.where(CustomerSanPhamSpecification.notDeleted())
                .and(CustomerSanPhamSpecification.hasTrangThai(TrangThai.DANG_HOAT_DONG));

        Pageable pageable = PageRequest.of(0, size, Sort.by("ngayTao").descending());
        Page<SanPham> page = landingRepository.findAll(spec, pageable);

        List<String> ids = page.getContent().stream().map(SanPham::getId).toList();
        Map<String, CustomerProductVariantStats> stats = ids.isEmpty() ? Map.of() :
                chiTietSanPhamRepository.summarizeBySanPhamIds(ids).stream()
                        .collect(Collectors.toMap(CustomerProductVariantStats::getSanPhamId, s -> s));

        return page.getContent().stream().map(sp -> {
            CustomerProductVariantStats s = stats.get(sp.getId());
            return CustomerLandingProductResponse.builder()
                    .id(sp.getId())
                    .maSanPham(sp.getMa())
                    .tenSanPham(sp.getTen())

                    .tenThuongHieu(sp.getThuongHieu() != null ? sp.getThuongHieu().getTen() : null)
                    .hinhAnh(sp.getHinhAnh())
                    .giaBanThapNhat(s != null ? s.getGiaBanThapNhat() : null)
                    .giaBanCaoNhat(s != null ? s.getGiaBanCaoNhat() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<CustomerLandingVariantResponse> getFeaturedVariants(Integer size) {
        Pageable pageable = PageRequest.of(0, size, Sort.by("ngayTao").descending());
        List<ChiTietSanPham> variants = chiTietSanPhamRepository
                .findByXoaMemFalseAndTrangThai(TrangThai.DANG_HOAT_DONG, pageable);

        if (variants.isEmpty()) return Collections.emptyList();

        List<String> ids = variants.stream().map(ChiTietSanPham::getId).toList();

        List<AnhChiTietSanPham> images = anhChiTietSanPhamRepository
                .findAllByChiTietSanPhamIdInAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(ids);

        Map<String, List<AnhChiTietSanPham>> imageMap = images.stream()
                .filter(img -> img.getChiTietSanPham() != null)
                .collect(Collectors.groupingBy(img -> img.getChiTietSanPham().getId()));

        List<ChiTietDotGiamGia> relations = chiTietDotGiamGiaRepository.findAllByChiTietSanPhamIdIn(ids);
        Map<String, List<ChiTietDotGiamGia>> relationMap = relations.stream()
                .filter(rel -> rel.getChiTietSanPham() != null)
                .collect(Collectors.groupingBy(rel -> rel.getChiTietSanPham().getId()));

        long now = System.currentTimeMillis();

        return variants.stream().map(v -> {
            List<AnhChiTietSanPham> imgs = imageMap.getOrDefault(v.getId(), Collections.emptyList());
            List<String> imgUrls = imgs.stream().map(AnhChiTietSanPham::getDuongDanAnh).toList();
            String primaryImage = imgs.stream()
                    .filter(AnhChiTietSanPham::getHinhAnhDaiDien)
                    .findFirst()
                    .map(AnhChiTietSanPham::getDuongDanAnh)
                    .orElse(imgUrls.isEmpty() ? null : imgUrls.get(0));

            BigDecimal activeDiscount = BigDecimal.ZERO;
            List<ChiTietDotGiamGia> rels = relationMap.getOrDefault(v.getId(), Collections.emptyList());
            if (!rels.isEmpty()) {
                List<DotGiamGia> validCampaigns = new ArrayList<>();
                for (ChiTietDotGiamGia ct : rels) {
                    DotGiamGia d = ct.getDotGiamGia();
                    if (d != null && d.getTrangThai() == TrangThai.DANG_HOAT_DONG
                            && d.getNgayBatDau() != null && d.getNgayKetThuc() != null
                            && d.getNgayBatDau() <= now && now <= d.getNgayKetThuc()
                            && d.getSoTienGiam() != null) {
                        validCampaigns.add(d);
                    }
                }
                if (!validCampaigns.isEmpty()) {
                    validCampaigns.sort((c1, c2) -> c2.getSoTienGiam().compareTo(c1.getSoTienGiam()));
                    activeDiscount = validCampaigns.get(0).getSoTienGiam();
                }
            }

            return CustomerLandingVariantResponse.builder()
                    .id(v.getId())
                    .idSanPham(v.getSanPham() != null ? v.getSanPham().getId() : null)
                    .maSanPham(v.getSanPham() != null ? v.getSanPham().getMa() : null)
                    .tenSanPham(v.getSanPham() != null ? v.getSanPham().getTen() : null)
                    .tenThuongHieu(v.getSanPham() != null && v.getSanPham().getThuongHieu() != null
                            ? v.getSanPham().getThuongHieu().getTen() : null)
                    .maChiTietSanPham(v.getMaChiTietSanPham())
                    .tenMauSac(v.getMauSac() != null ? v.getMauSac().getTen() : null)
                    .maMauHex(v.getMauSac() != null ? v.getMauSac().getMaMauHex() : null)
                    .tenKichThuoc(v.getKichThuoc() != null ? v.getKichThuoc().getTen() : null)
                    .giaTriKichThuoc(v.getKichThuoc() != null ? v.getKichThuoc().getGiaTriKichThuoc() : null)
                    .soLuong(v.getSoLuong())
                    .giaBan(v.getGiaBan())
                    .phanTramGiam(activeDiscount)
                    .hinhAnh(primaryImage)
                    .images(imgUrls)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<List<CustomerLandingFeatureItemResponse>> getLandingFeatures() {
        CustomerProductFormOptionsResponse filters = sanPhamService.getFormOptions();
        List<CustomerLandingFeatureItemResponse> combined = new ArrayList<>();

        if (filters.getThuongHieus() != null) {
            filters.getThuongHieus().stream().limit(3).forEach(b -> 
                combined.add(new CustomerLandingFeatureItemResponse(b.getTen(), "Thương hiệu đẳng cấp", "mdi-tag-heart", "#2962FF", "s", "idThuongHieu", b.getId()))
            );
        }

        if (filters.getChatLieus() != null) {
            filters.getChatLieus().stream().limit(4).forEach(m -> 
                combined.add(new CustomerLandingFeatureItemResponse(m.getTen(), "Chất liệu siêu việt", "mdi-texture", "#00B0FF", "m", "idChatLieu", m.getId()))
            );
        }

        if (filters.getMucDichChays() != null) {
            filters.getMucDichChays().stream().limit(3).forEach(p -> 
                combined.add(new CustomerLandingFeatureItemResponse(p.getTen(), "Thiết kế chuẩn xác", "mdi-run-fast", "#6200EA", "l", "idMucDichChay", p.getId()))
            );
        }

        if (filters.getXuatXus() != null) {
            filters.getXuatXus().stream().limit(2).forEach(o -> 
                combined.add(new CustomerLandingFeatureItemResponse(o.getTen(), "Nguồn gốc xuất xứ", "mdi-earth", "#00E676", "m", "idXuatXu", o.getId()))
            );
        }

        List<CustomerLandingFeatureItemResponse> fallbacks = new ArrayList<>(List.of(
            new CustomerLandingFeatureItemResponse("NIGHT-GLOW", "Phản quang 360 độ.", "mdi-brightness-6", "#FFAB00", "s", null, null),
            new CustomerLandingFeatureItemResponse("QUICK-DRY", "Công nghệ khô nhanh.", "mdi-water-off", "#00B8D4", "m", null, null),
            new CustomerLandingFeatureItemResponse("PRO-STABILITY", "Khung gót vững chãi.", "mdi-shield-check", "#7C4DFF", "l", null, null)
        ));

        while (combined.size() < 12 && !fallbacks.isEmpty()) {
            combined.add(fallbacks.remove(fallbacks.size() - 1));
        }

        List<CustomerLandingFeatureItemResponse> final12 = combined.subList(0, Math.min(12, combined.size()));

        List<List<CustomerLandingFeatureItemResponse>> columns = new ArrayList<>();
        columns.add(buildLoop(final12, 3, 0));
        
        List<CustomerLandingFeatureItemResponse> reversed = new ArrayList<>(final12);
        java.util.Collections.reverse(reversed);
        columns.add(buildLoop(reversed, 3, 0));
        
        columns.add(buildLoop(concat(final12.subList(4, final12.size()), final12.subList(0, 4)), 3, 0));
        columns.add(buildLoop(concat(final12.subList(8, final12.size()), final12.subList(0, 8)), 3, 0));
        columns.add(buildLoop(concat(final12.subList(2, final12.size()), final12.subList(0, 2)), 3, 0));
        columns.add(buildLoop(concat(final12.subList(6, final12.size()), final12.subList(0, 6)), 3, 0));

        return columns;
    }

    private List<CustomerLandingFeatureItemResponse> concat(List<CustomerLandingFeatureItemResponse> a, List<CustomerLandingFeatureItemResponse> b) {
        List<CustomerLandingFeatureItemResponse> res = new ArrayList<>(a);
        res.addAll(b);
        return res;
    }

    private List<CustomerLandingFeatureItemResponse> buildLoop(List<CustomerLandingFeatureItemResponse> items, int repeatCount, int offset) {
        List<CustomerLandingFeatureItemResponse> output = new ArrayList<>();
        for (int i = 0; i < repeatCount; i++) {
            if (offset < items.size()) {
                output.addAll(items.subList(offset, items.size()));
                output.addAll(items.subList(0, offset));
            } else {
                output.addAll(items);
            }
        }
        return output;
    }
}
