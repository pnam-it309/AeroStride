package com.example.be.core.admin.sanpham.repository;

import java.math.BigDecimal;

public interface ProductVariantStatisticsProjection {

    String getSanPhamId();

    Long getTongBienThe();

    Long getTongSoLuongTon();

    BigDecimal getGiaBanThapNhat();

    BigDecimal getGiaBanCaoNhat();
}
