package com.example.be.core.customer.landing.service.impl;

import com.example.be.core.customer.landing.model.response.CustomerLandingProductResponse;
import com.example.be.core.customer.landing.repository.CustomerLandingRepository;
import com.example.be.core.customer.landing.service.CustomerLandingService;
import com.example.be.core.customer.sanpham.model.response.CustomerProductVariantStats;
import com.example.be.core.customer.sanpham.repository.CustomerSanPhamChiTietRepository;
import com.example.be.core.customer.sanpham.repository.CustomerSanPhamSpecification;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerLandingServiceImpl implements CustomerLandingService {

    private final CustomerLandingRepository landingRepository;
    private final CustomerSanPhamChiTietRepository chiTietSanPhamRepository;

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
}
