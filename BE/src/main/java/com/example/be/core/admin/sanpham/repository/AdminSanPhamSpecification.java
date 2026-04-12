package com.example.be.core.admin.sanpham.repository;

import com.example.be.entity.SanPham;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class AdminSanPhamSpecification {

    private AdminSanPhamSpecification() {
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

    public static Specification<SanPham> hasKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("ma")), normalizedKeyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("ten")), normalizedKeyword)
        );
    }

    public static Specification<SanPham> hasDanhMuc(String danhMucId) {
        if (!StringUtils.hasText(danhMucId)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("danhMuc").get("id"), danhMucId.trim());
    }

    public static Specification<SanPham> hasThuongHieu(String thuongHieuId) {
        if (!StringUtils.hasText(thuongHieuId)) {
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
        if (!StringUtils.hasText(gioiTinh)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("gioiTinhKhachHang"), gioiTinh.trim());
    }

    public static Specification<SanPham> hasXuatXu(String xuatXuId) {
        if (!StringUtils.hasText(xuatXuId)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("xuatXu").get("id"), xuatXuId.trim());
    }

    public static Specification<SanPham> hasMucDichChay(String mucDichChayId) {
        if (!StringUtils.hasText(mucDichChayId)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("mucDichChay").get("id"), mucDichChayId.trim());
    }

    public static Specification<SanPham> hasChatLieu(String chatLieuId) {
        if (!StringUtils.hasText(chatLieuId)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("chatLieu").get("id"), chatLieuId.trim());
    }
}
