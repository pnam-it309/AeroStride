package com.example.be.core.admin.sanpham.repository;

import com.example.be.entity.ChiTietSanPham;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * Điều kiện lọc động cho danh sách biến thể (chi tiết sản phẩm) phía admin.
 */
public final class AdminChiTietSanPhamSpecification {

    private AdminChiTietSanPhamSpecification() {
    }

    public static Specification<ChiTietSanPham> notDeleted() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("xoaMem")),
                cb.isFalse(root.get("xoaMem"))
        );
    }

    // Tìm theo mã biến thể hoặc tên sản phẩm gốc
    public static Specification<ChiTietSanPham> hasKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String normalized = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("maChiTietSanPham")), normalized),
                cb.like(cb.lower(root.get("sanPham").get("ten")), normalized)
        );
    }

    public static Specification<ChiTietSanPham> hasSanPham(String sanPhamId) {
        if (!StringUtils.hasText(sanPhamId) || "ALL".equalsIgnoreCase(sanPhamId)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("sanPham").get("id"), sanPhamId.trim());
    }

    public static Specification<ChiTietSanPham> hasMauSac(String mauSacId) {
        if (!StringUtils.hasText(mauSacId)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("mauSac").get("id"), mauSacId.trim());
    }

    public static Specification<ChiTietSanPham> hasKichThuoc(String kichThuocId) {
        if (!StringUtils.hasText(kichThuocId)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("kichThuoc").get("id"), kichThuocId.trim());
    }

    public static Specification<ChiTietSanPham> hasTrangThai(TrangThai trangThai) {
        if (trangThai == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("trangThai"), trangThai);
    }

    public static Specification<ChiTietSanPham> hasGiaBanInRange(BigDecimal minGia, BigDecimal maxGia) {
        if (minGia == null && maxGia == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (minGia != null && maxGia != null) {
                return cb.between(root.get("giaBan"), minGia, maxGia);
            }
            if (minGia != null) {
                return cb.greaterThanOrEqualTo(root.get("giaBan"), minGia);
            }
            return cb.lessThanOrEqualTo(root.get("giaBan"), maxGia);
        };
    }
}
