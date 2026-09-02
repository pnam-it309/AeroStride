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
 * Tuyet doi khong cong don cac dot giam gia ma chon 1 dot tot nhat dang hieu luc.
 */
public final class DiscountPriceUtils {

    private DiscountPriceUtils() {
    }

    public static BigDecimal getEffectiveDiscountValue(ChiTietDotGiamGia rel) {
        if (rel == null || rel.getDotGiamGia() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = rel.getGiaTriGiam() != null
                ? rel.getGiaTriGiam()
                : rel.getDotGiamGia().getSoTienGiam();
        return value != null ? value : BigDecimal.ZERO;
    }

    /** Tinh so tien duoc giam thuc te cho bien the tu mot dot giam gia. */
    public static BigDecimal calculateActualDiscountAmount(BigDecimal basePrice, ChiTietDotGiamGia rel) {
        if (rel == null || rel.getDotGiamGia() == null || basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        DotGiamGia d = rel.getDotGiamGia();
        BigDecimal discountValue = getEffectiveDiscountValue(rel);
        if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discountAmount;
        if (isPercentDiscount(d)) {
            discountAmount = basePrice.multiply(discountValue).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (d.getGiamToiDa() != null && d.getGiamToiDa().compareTo(BigDecimal.ZERO) > 0 && discountAmount.compareTo(d.getGiamToiDa()) > 0) {
                discountAmount = d.getGiamToiDa();
            }
        } else {
            discountAmount = discountValue;
        }
        return discountAmount.min(basePrice);
    }

    /** Chon dot giam gia dang hieu luc mang lai gia tri giam lon nhat cho san pham. */
    public static ChiTietDotGiamGia getBestActiveRelation(BigDecimal originalPrice, Collection<ChiTietDotGiamGia> relations) {
        if (relations == null || relations.isEmpty()) {
            return null;
        }
        long now = System.currentTimeMillis();
        BigDecimal base = originalPrice != null ? originalPrice : BigDecimal.ZERO;
        return relations.stream()
                .filter(Objects::nonNull)
                .filter(rel -> rel.getDotGiamGia() != null)
                .filter(rel -> isActiveNow(rel.getDotGiamGia(), now))
                .max(Comparator
                        .comparing((ChiTietDotGiamGia rel) -> calculateActualDiscountAmount(base, rel))
                        .thenComparing(DiscountPriceUtils::getEffectiveDiscountValue)
                        .thenComparing(rel -> rel.getDotGiamGia().getMucUuTien() != null
                                ? rel.getDotGiamGia().getMucUuTien()
                                : 0))
                .orElse(null);
    }

    public static ChiTietDotGiamGia getBestActiveRelation(Collection<ChiTietDotGiamGia> relations) {
        return getBestActiveRelation(BigDecimal.ZERO, relations);
    }

    /** Lay gia tri giam dang hoat dong tot nhat de hien thi/doi soat. */
    public static BigDecimal getActiveDiscountValue(Collection<ChiTietDotGiamGia> relations) {
        ChiTietDotGiamGia activeRelation = getBestActiveRelation(relations);
        if (activeRelation == null) {
            return BigDecimal.ZERO;
        }
        return getEffectiveDiscountValue(activeRelation);
    }

    /** Lay ten dot giam gia dang hoat dong tot nhat de hien thi tren giao dien. */
    public static String getActiveDiscountName(BigDecimal originalPrice, Collection<ChiTietDotGiamGia> relations) {
        ChiTietDotGiamGia activeRelation = getBestActiveRelation(originalPrice, relations);
        if (activeRelation == null || activeRelation.getDotGiamGia() == null) {
            return null;
        }
        return activeRelation.getDotGiamGia().getTen();
    }

    public static String getActiveDiscountName(Collection<ChiTietDotGiamGia> relations) {
        return getActiveDiscountName(BigDecimal.ZERO, relations);
    }

    /** Lay phan tram giam theo dung so % cua dot giam gia (neu la dang phan tram). */
    public static BigDecimal getActiveDiscountPercent(BigDecimal originalPrice, Collection<ChiTietDotGiamGia> relations) {
        BigDecimal basePrice = originalPrice != null ? originalPrice : BigDecimal.ZERO;
        ChiTietDotGiamGia activeRelation = getBestActiveRelation(basePrice, relations);
        if (activeRelation == null) {
            return BigDecimal.ZERO;
        }
        if (isPercentDiscount(activeRelation.getDotGiamGia())) {
            return getEffectiveDiscountValue(activeRelation);
        }
        if (basePrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountAmount = calculateActualDiscountAmount(basePrice, activeRelation);
            return discountAmount.multiply(BigDecimal.valueOf(100)).divide(basePrice, 0, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal getActiveDiscountPercent(Collection<ChiTietDotGiamGia> relations) {
        return getActiveDiscountPercent(BigDecimal.ZERO, relations);
    }

    /** Tra ve gia sau giam, khong bao gio nho hon 0. */
    public static BigDecimal calculateDiscountedPrice(BigDecimal originalPrice, Collection<ChiTietDotGiamGia> relations) {
        BigDecimal basePrice = originalPrice != null ? originalPrice : BigDecimal.ZERO;
        ChiTietDotGiamGia activeRelation = getBestActiveRelation(basePrice, relations);
        if (activeRelation == null) {
            return basePrice;
        }
        BigDecimal discountAmount = calculateActualDiscountAmount(basePrice, activeRelation);
        BigDecimal discountedPrice = basePrice.subtract(discountAmount);
        return discountedPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : discountedPrice;
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
        return type == null || "PHAN_TRAM".equalsIgnoreCase(type) || "PERCENT".equalsIgnoreCase(type) || "PERCENTAGE".equalsIgnoreCase(type);
    }
}
