package com.example.be.core.admin.sanpham.repository;

import java.util.Collection;
import java.util.List;

public interface AdminChiTietSanPhamRepositoryCustom {
    List<ProductVariantStatisticsProjection> summarizeBySanPhamIds(Collection<String> productIds);
}
