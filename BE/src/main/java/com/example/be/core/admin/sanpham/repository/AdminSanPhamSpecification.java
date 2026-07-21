package com.example.be.core.admin.sanpham.repository;

import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.SanPham;
import com.example.be.infrastructure.constants.TrangThai;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    // Lọc theo khoảng giá: sản phẩm hợp lệ nếu có ít nhất một biến thể (chưa xóa) với giá bán nằm trong [minGia, maxGia].
    public static Specification<SanPham> hasGiaBanInRange(BigDecimal minGia, BigDecimal maxGia) {
        if (minGia == null && maxGia == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            Subquery<String> subquery = query.subquery(String.class);
            Root<ChiTietSanPham> variant = subquery.from(ChiTietSanPham.class);
            subquery.select(variant.get("id"));

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(variant.get("sanPham").get("id"), root.get("id")));
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.isNull(variant.get("xoaMem")),
                    criteriaBuilder.isFalse(variant.get("xoaMem"))
            ));
            if (minGia != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(variant.get("giaBan"), minGia));
            }
            if (maxGia != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(variant.get("giaBan"), maxGia));
            }
            return criteriaBuilder.exists(subquery.where(predicates.toArray(new Predicate[0])));
        };
    }
}
