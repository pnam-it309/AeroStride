package com.example.be.core.customer.sanpham.repository;

import com.example.be.entity.SanPham;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CustomerSanPhamSpecification {

    private CustomerSanPhamSpecification() {
    }

    public static Specification<SanPham> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("xoaMem")),
                        criteriaBuilder.isFalse(root.get("xoaMem"))
                ),
                criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("trangThai")),
                        criteriaBuilder.notEqual(root.get("trangThai"), TrangThai.DA_XOA)
                )
        );
    }

    private static boolean isValidParam(String param) {
        if (!StringUtils.hasText(param)) return false;
        String trimmed = param.trim();
        return !"null".equalsIgnoreCase(trimmed) && !"undefined".equalsIgnoreCase(trimmed);
    }

    public static Specification<SanPham> hasKeyword(String keyword) {
        if (!isValidParam(keyword)) {
            return null;
        }
        String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("ma")), normalizedKeyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("ten")), normalizedKeyword)
        );
    }

    public static Specification<SanPham> hasThuongHieu(String thuongHieuId) {
        if (!isValidParam(thuongHieuId)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("thuongHieu").get("id"), thuongHieuId.trim());
    }

    public static Specification<SanPham> hasTrangThai(TrangThai trangThai) {
        if (trangThai == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("trangThai"), trangThai);
    }

    public static Specification<SanPham> hasGioiTinhKhachHang(String gioiTinh) {
        if (!isValidParam(gioiTinh)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("gioiTinhKhachHang"), gioiTinh.trim());
    }

    public static Specification<SanPham> hasXuatXu(String xuatXuId) {
        if (!isValidParam(xuatXuId)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("xuatXu").get("id"), xuatXuId.trim());
    }

    public static Specification<SanPham> hasMucDichChay(String mucDichChayId) {
        if (!isValidParam(mucDichChayId)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("mucDichChay").get("id"), mucDichChayId.trim());
    }

    public static Specification<SanPham> hasChatLieu(String chatLieuId) {
        if (!isValidParam(chatLieuId)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("chatLieu").get("id"), chatLieuId.trim());
    }
}
