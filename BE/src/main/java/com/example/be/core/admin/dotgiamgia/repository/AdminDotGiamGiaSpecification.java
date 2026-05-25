package com.example.be.core.admin.dotgiamgia.repository;

import com.example.be.entity.DotGiamGia;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.jpa.domain.Specification;

public final class AdminDotGiamGiaSpecification {

    private AdminDotGiamGiaSpecification() {
    }

    public static Specification<DotGiamGia> keywordLike(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) return null;
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("ten")), pattern),
                cb.like(cb.lower(root.get("ma")), pattern)
            );
        };
    }

    public static Specification<DotGiamGia> filterTrangThai(String status, Long now) {
        return (root, query, cb) -> {
            if (status == null || status.trim().isEmpty() || now == null) {
                return null;
            }
            if ("SAP_DIEN_RA".equals(status)) {
                return cb.greaterThan(root.get("ngayBatDau"), now);
            }
            if ("DANG_HOAT_DONG".equals(status)) {
                return cb.and(
                    cb.lessThanOrEqualTo(root.get("ngayBatDau"), now),
                    cb.greaterThanOrEqualTo(root.get("ngayKetThuc"), now),
                    cb.equal(root.get("trangThai"), TrangThai.DANG_HOAT_DONG)
                );
            }
            if ("DA_KET_THUC".equals(status)) {
                return cb.or(
                    cb.lessThan(root.get("ngayKetThuc"), now),
                    cb.equal(root.get("trangThai"), TrangThai.NGUNG_HOAT_DONG)
                );
            }
            return null;
        };
    }

    public static Specification<DotGiamGia> startDateAfter(Long startDate) {
        return (root, query, cb) -> startDate == null ? null : cb.greaterThanOrEqualTo(root.get("ngayBatDau"), startDate);
    }

    public static Specification<DotGiamGia> endDateBefore(Long endDate) {
        return (root, query, cb) -> endDate == null ? null : cb.lessThanOrEqualTo(root.get("ngayKetThuc"), endDate);
    }
}
