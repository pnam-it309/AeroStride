package com.example.be.core.admin.sanpham.service;

import com.example.be.core.admin.sanpham.model.response.ProductFormOptionsResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import com.example.be.core.admin.sanpham.model.response.ProductOptionResponse;
import com.example.be.core.common.base.BaseCodeNameEntity;
import com.example.be.infrastructure.constants.GioiTinhKhachHang;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductOptionService {

    private final ThuongHieuRepository thuongHieuRepository;
    private final DanhMucRepository danhMucRepository;
    private final XuatXuRepository xuatXuRepository;
    private final MucDichChayRepository mucDichChayRepository;
    private final CoGiayRepository coGiayRepository;
    private final ChatLieuRepository chatLieuRepository;
    private final DeGiayRepository deGiayRepository;
    private final MauSacRepository mauSacRepository;
    private final KichThuocRepository kichThuocRepository;

    @Cacheable(value = "productOptions", key = "'all'")
    public ProductFormOptionsResponse getFormOptions() {
        Sort sortByName = Sort.by(Sort.Direction.ASC, "ten");
        return ProductFormOptionsResponse.builder()
                .danhMucs(mapOptions(danhMucRepository.findAll(sortByName)))
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

    private List<ProductOptionResponse> mapOptions(List<? extends BaseCodeNameEntity> entities) {
        return entities.stream()
                .filter(Objects::nonNull)
                .filter(e -> !Boolean.TRUE.equals(e.getXoaMem()))
                .filter(e -> e.getTrangThai() == TrangThai.DANG_HOAT_DONG)
                .map(e -> ProductOptionResponse.builder()
                        .id(e.getId())
                        .ma(e.getMa())
                        .ten(e.getTen())
                        .build())
                .toList();
    }

    @CacheEvict(value = "productOptions", allEntries = true)
    public void evictCache() {
        // This method will be called when metadata changes to clear the cache
    }
}
