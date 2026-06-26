package com.example.be.utils;

import com.example.be.entity.ChiTietDotGiamGia;
import com.example.be.entity.DotGiamGia;
import com.example.be.infrastructure.constants.TrangThai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/**
 * Ham dung chung de tinh gia ban thuc te cua bien the khi co dot giam gia.
 * BE dung chung logic nay cho ca luong san pham va luong ban hang tai quay.
 */
public final class DiscountPriceUtils {

    private DiscountPriceUtils() {
    }

    /** Lay gia tri giam dang hoat dong tot nhat de hien thi/doi soat. */
    public static BigDecimal getActiveDiscountValue(Collection<ChiTietDotGiamGia> relations) {
        ChiTietDotGiamGia activeRelation = getBestActiveRelation(relations);
        if (activeRelation == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = activeRelation.getGiaTriGiam() != null
                ? activeRelation.getGiaTriGiam()
                : activeRelation.getDotGiamGia().getSoTienGiam();
        return value != null ? value : BigDecimal.ZERO;
    }

    /** Lay phan tram giam neu dot giam gia la dang phan tram. */
    public static BigDecimal getActiveDiscountPercent(Collection<ChiTietDotGiamGia> relations) {
        ChiTietDotGiamGia activeRelation = getBestActiveRelation(relations);
        if (activeRelation == null || !isPercentDiscount(activeRelation.getDotGiamGia())) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = activeRelation.getGiaTriGiam() != null
                ? activeRelation.getGiaTriGiam()
                : activeRelation.getDotGiamGia().getSoTienGiam();
        return value != null ? value : BigDecimal.ZERO;
    }

    /** Tra ve gia sau giam, khong bao gio nho hon 0. */
    public static BigDecimal calculateDiscountedPrice(BigDecimal originalPrice, Collection<ChiTietDotGiamGia> relations) {
        BigDecimal basePrice = originalPrice != null ? originalPrice : BigDecimal.ZERO;
        ChiTietDotGiamGia activeRelation = getBestActiveRelation(relations);
        if (activeRelation == null) {
            return basePrice;
        }

        BigDecimal discountValue = activeRelation.getGiaTriGiam() != null
                ? activeRelation.getGiaTriGiam()
                : activeRelation.getDotGiamGia().getSoTienGiam();
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            return basePrice;
        }

        BigDecimal discountAmount = isPercentDiscount(activeRelation.getDotGiamGia())
                ? basePrice.multiply(discountValue).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : discountValue;

        BigDecimal discountedPrice = basePrice.subtract(discountAmount);
        return discountedPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : discountedPrice;
    }

    /** Chon dot giam gia dang hieu luc co uu tien/gia tri cao nhat. */
    private static ChiTietDotGiamGia getBestActiveRelation(Collection<ChiTietDotGiamGia> relations) {
        if (relations == null || relations.isEmpty()) {
            return null;
        }
        long now = System.currentTimeMillis();
        return relations.stream()
                .filter(Objects::nonNull)
                .filter(rel -> rel.getDotGiamGia() != null)
                .filter(rel -> isActiveNow(rel.getDotGiamGia(), now))
                .max(Comparator
                        .comparing((ChiTietDotGiamGia rel) -> rel.getDotGiamGia().getMucUuTien() != null
                                ? rel.getDotGiamGia().getMucUuTien()
                                : 0)
                        .thenComparing(rel -> {
                            BigDecimal value = rel.getGiaTriGiam() != null
                                    ? rel.getGiaTriGiam()
                                    : rel.getDotGiamGia().getSoTienGiam();
                            return value != null ? value : BigDecimal.ZERO;
                        }))
                .orElse(null);
    }

    private static boolean isActiveNow(DotGiamGia discount, long now) {
        return discount.getTrangThai() == TrangThai.DANG_HOAT_DONG
                && discount.getNgayBatDau() != null
                && discount.getNgayKetThuc() != null
                && discount.getNgayBatDau() <= now
                && now <= discount.getNgayKetThuc()
                && discount.getSoTienGiam() != null;
    }

    private static boolean isPercentDiscount(DotGiamGia discount) {
        String type = discount.getLoaiGiamGia();
        return type == null || "PHAN_TRAM".equalsIgnoreCase(type) || "PERCENT".equalsIgnoreCase(type);
    }
}
