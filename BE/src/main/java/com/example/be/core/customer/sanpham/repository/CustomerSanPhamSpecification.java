package com.example.be.core.customer.sanpham.repository;

import com.example.be.entity.SanPham;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.List;

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
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("ma")), normalizedKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("ten")), normalizedKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("thuongHieu").get("ten")), normalizedKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("mucDichChay").get("ten")), normalizedKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("chatLieu").get("ten")), normalizedKeyword)
            );
        };
    }

    public static Specification<SanPham> hasThuongHieu(String thuongHieuId) {
        if (!isValidParam(thuongHieuId)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("thuongHieu").get("id"), thuongHieuId.trim());
    }

    public static Specification<SanPham> hasThuongHieuIn(List<String> thuongHieuIds) {
        if (thuongHieuIds == null || thuongHieuIds.isEmpty()) {
            return null;
        }
        List<String> validIds = thuongHieuIds.stream()
                .filter(CustomerSanPhamSpecification::isValidParam)
                .map(String::trim)
                .toList();
        if (validIds.isEmpty()) {
            return null;
        }
        return (root, query, criteriaBuilder) -> root.get("thuongHieu").get("id").in(validIds);
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

    public static Specification<SanPham> hasXuatXuIn(List<String> xuatXuIds) {
        if (xuatXuIds == null || xuatXuIds.isEmpty()) {
            return null;
        }
        List<String> validIds = xuatXuIds.stream()
                .filter(CustomerSanPhamSpecification::isValidParam)
                .map(String::trim)
                .toList();
        if (validIds.isEmpty()) {
            return null;
        }
        return (root, query, criteriaBuilder) -> root.get("xuatXu").get("id").in(validIds);
    }

    public static Specification<SanPham> hasMucDichChay(String mucDichChayId) {
        if (!isValidParam(mucDichChayId)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("mucDichChay").get("id"), mucDichChayId.trim());
    }

    public static Specification<SanPham> hasMucDichChayIn(List<String> mucDichChayIds) {
        if (mucDichChayIds == null || mucDichChayIds.isEmpty()) {
            return null;
        }
        // Filter out invalid values
        List<String> validIds = mucDichChayIds.stream()
                .filter(id -> isValidParam(id))
                .map(String::trim)
                .toList();
        if (validIds.isEmpty()) {
            return null;
        }
        return (root, query, criteriaBuilder) -> root.get("mucDichChay").get("id").in(validIds);
    }

    public static Specification<SanPham> hasChatLieu(String chatLieuId) {
        if (!isValidParam(chatLieuId)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("chatLieu").get("id"), chatLieuId.trim());
    }

    public static Specification<SanPham> hasChatLieuIn(List<String> chatLieuIds) {
        if (chatLieuIds == null || chatLieuIds.isEmpty()) {
            return null;
        }
        List<String> validIds = chatLieuIds.stream()
                .filter(CustomerSanPhamSpecification::isValidParam)
                .map(String::trim)
                .toList();
        if (validIds.isEmpty()) {
            return null;
        }
        return (root, query, criteriaBuilder) -> root.get("chatLieu").get("id").in(validIds);
    }

    /**
     * Filter sản phẩm có ít nhất 1 biến thể với giaBan >= minGia
     */
    public static Specification<SanPham> hasMinGia(BigDecimal minGia) {
        if (minGia == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            Subquery<String> subquery = query.subquery(String.class);
            Root<?> variant = subquery.from(com.example.be.entity.ChiTietSanPham.class);
            subquery.select(variant.get("sanPham").get("id"));
            subquery.where(
                    criteriaBuilder.equal(variant.get("sanPham").get("id"), root.get("id")),
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(variant.get("xoaMem")),
                            criteriaBuilder.isFalse(variant.get("xoaMem"))
                    ),
                    criteriaBuilder.greaterThanOrEqualTo(variant.get("giaBan"), minGia)
            );
            return criteriaBuilder.exists(subquery);
        };
    }

    /**
     * Filter sản phẩm có ít nhất 1 biến thể với giaBan <= maxGia
     */
    public static Specification<SanPham> hasMaxGia(BigDecimal maxGia) {
        if (maxGia == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            Subquery<String> subquery = query.subquery(String.class);
            Root<?> variant = subquery.from(com.example.be.entity.ChiTietSanPham.class);
            subquery.select(variant.get("sanPham").get("id"));
            subquery.where(
                    criteriaBuilder.equal(variant.get("sanPham").get("id"), root.get("id")),
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(variant.get("xoaMem")),
                            criteriaBuilder.isFalse(variant.get("xoaMem"))
                    ),
                    criteriaBuilder.lessThanOrEqualTo(variant.get("giaBan"), maxGia)
            );
            return criteriaBuilder.exists(subquery);
        };
    }

    /**
     * Filter sản phẩm có ít nhất 1 biến thể với kích thước tên hoặc giá trị (e.g. "40", "Size 40", ID)
     */
    public static Specification<SanPham> hasKichThuoc(String kichThuocTen) {
        if (!isValidParam(kichThuocTen)) {
            return null;
        }
        String clean = kichThuocTen.trim().toLowerCase();
        String numOnly = clean.replaceAll("[^0-9.]", "");
        return (root, query, criteriaBuilder) -> {
            Subquery<String> subquery = query.subquery(String.class);
            Root<?> variant = subquery.from(com.example.be.entity.ChiTietSanPham.class);
            subquery.select(variant.get("sanPham").get("id"));

            List<jakarta.persistence.criteria.Predicate> orPreds = new java.util.ArrayList<>();
            orPreds.add(criteriaBuilder.equal(criteriaBuilder.lower(variant.get("kichThuoc").get("ten")), clean));
            orPreds.add(criteriaBuilder.like(criteriaBuilder.lower(variant.get("kichThuoc").get("ten")), "%" + clean + "%"));
            orPreds.add(criteriaBuilder.equal(criteriaBuilder.lower(variant.get("kichThuoc").get("giaTriKichThuoc")), clean));
            orPreds.add(criteriaBuilder.equal(criteriaBuilder.lower(variant.get("kichThuoc").get("id")), clean));
            orPreds.add(criteriaBuilder.equal(criteriaBuilder.lower(variant.get("kichThuoc").get("ma")), clean));
            if (!numOnly.isEmpty()) {
                orPreds.add(criteriaBuilder.like(criteriaBuilder.lower(variant.get("kichThuoc").get("ten")), "%" + numOnly + "%"));
                orPreds.add(criteriaBuilder.equal(criteriaBuilder.lower(variant.get("kichThuoc").get("giaTriKichThuoc")), numOnly));
            }

            subquery.where(
                    criteriaBuilder.equal(variant.get("sanPham").get("id"), root.get("id")),
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(variant.get("xoaMem")),
                            criteriaBuilder.isFalse(variant.get("xoaMem"))
                    ),
                    criteriaBuilder.or(orPreds.toArray(new jakarta.persistence.criteria.Predicate[0]))
            );
            return criteriaBuilder.exists(subquery);
        };
    }

    /**
     * Filter sản phẩm có ít nhất 1 biến thể với bất kỳ kích thước nào trong danh sách (OR)
     */
    public static Specification<SanPham> hasKichThuocIn(List<String> kichThuocs) {
        if (kichThuocs == null || kichThuocs.isEmpty()) {
            return null;
        }
        List<String> validSizes = kichThuocs.stream()
                .filter(CustomerSanPhamSpecification::isValidParam)
                .map(s -> s.trim().toLowerCase())
                .toList();
        if (validSizes.isEmpty()) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            Subquery<String> subquery = query.subquery(String.class);
            Root<?> variant = subquery.from(com.example.be.entity.ChiTietSanPham.class);
            subquery.select(variant.get("sanPham").get("id"));

            List<jakarta.persistence.criteria.Predicate> sizePredicates = new java.util.ArrayList<>();
            for (String size : validSizes) {
                String num = size.replaceAll("[^0-9.]", "");
                List<jakarta.persistence.criteria.Predicate> orPreds = new java.util.ArrayList<>();
                orPreds.add(criteriaBuilder.equal(criteriaBuilder.lower(variant.get("kichThuoc").get("ten")), size));
                orPreds.add(criteriaBuilder.like(criteriaBuilder.lower(variant.get("kichThuoc").get("ten")), "%" + size + "%"));
                orPreds.add(criteriaBuilder.equal(criteriaBuilder.lower(variant.get("kichThuoc").get("giaTriKichThuoc")), size));
                orPreds.add(criteriaBuilder.equal(criteriaBuilder.lower(variant.get("kichThuoc").get("id")), size));
                orPreds.add(criteriaBuilder.equal(criteriaBuilder.lower(variant.get("kichThuoc").get("ma")), size));
                if (!num.isEmpty()) {
                    orPreds.add(criteriaBuilder.like(criteriaBuilder.lower(variant.get("kichThuoc").get("ten")), "%" + num + "%"));
                    orPreds.add(criteriaBuilder.equal(criteriaBuilder.lower(variant.get("kichThuoc").get("giaTriKichThuoc")), num));
                }
                sizePredicates.add(criteriaBuilder.or(orPreds.toArray(new jakarta.persistence.criteria.Predicate[0])));
            }

            subquery.where(
                    criteriaBuilder.equal(variant.get("sanPham").get("id"), root.get("id")),
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(variant.get("xoaMem")),
                            criteriaBuilder.isFalse(variant.get("xoaMem"))
                    ),
                    criteriaBuilder.or(sizePredicates.toArray(new jakarta.persistence.criteria.Predicate[0]))
            );
            return criteriaBuilder.exists(subquery);
        };
    }
}
