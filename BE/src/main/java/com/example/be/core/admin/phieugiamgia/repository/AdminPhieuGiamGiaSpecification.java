package com.example.be.core.admin.phieugiamgia.repository;

import com.example.be.entity.PhieuGiamGia;
import com.example.be.infrastructure.constants.HinhThucPhieuGiamGia;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.jpa.domain.Specification;

public final class AdminPhieuGiamGiaSpecification {

    private AdminPhieuGiamGiaSpecification() {}

    public static Specification<PhieuGiamGia> keywordLike(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) return null;
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("ma")), pattern),
                cb.like(cb.lower(root.get("ten")), pattern)
            );
        };
    }

    public static Specification<PhieuGiamGia> hasLoaiPhieu(String loaiPhieu) {
        return (root, query, cb) -> {
            if (loaiPhieu == null || loaiPhieu.trim().isEmpty()) return null;
            return cb.equal(root.get("loaiPhieu"), loaiPhieu);
        };
    }

    public static Specification<PhieuGiamGia> hasHinhThuc(String hinhThuc) {
        return (root, query, cb) -> {
            if (hinhThuc == null || hinhThuc.trim().isEmpty()) return null;
            if (HinhThucPhieuGiamGia.isCongKhai(hinhThuc)) {
                return cb.or(
                    cb.isNull(root.get("hinhThuc")),
                    cb.equal(root.get("hinhThuc"), ""),
                    cb.equal(root.get("hinhThuc"), HinhThucPhieuGiamGia.CONG_KHAI.name())
                );
            }
            return cb.equal(root.get("hinhThuc"), hinhThuc);
        };
    }

    public static Specification<PhieuGiamGia> hasTrangThai(TrangThai trangThai) {
        return (root, query, cb) -> trangThai == null ? null : cb.equal(root.get("trangThai"), trangThai);
    }

    public static Specification<PhieuGiamGia> filterTimeline(String timelineStatus, Long now) {
        return (root, query, cb) -> {
            if (timelineStatus == null || timelineStatus.trim().isEmpty() || now == null) {
                return null;
            }
            if ("SAP_DIEN_RA".equals(timelineStatus)) {
                return cb.greaterThan(root.get("ngayBatDau"), now);
            }
            if ("DANG_HOAT_DONG".equals(timelineStatus)) {
                return cb.and(
                    cb.lessThanOrEqualTo(root.get("ngayBatDau"), now),
                    cb.greaterThanOrEqualTo(root.get("ngayKetThuc"), now),
                    cb.equal(root.get("trangThai"), TrangThai.DANG_HOAT_DONG)
                );
            }
            if ("DA_KET_THUC".equals(timelineStatus)) {
                return cb.or(
                    cb.lessThan(root.get("ngayKetThuc"), now),
                    cb.equal(root.get("trangThai"), TrangThai.NGUNG_HOAT_DONG)
                );
            }
            return null;
        };
    }

    public static Specification<PhieuGiamGia> startDateAfter(Long tuNgay) {
        return (root, query, cb) -> tuNgay == null ? null : cb.greaterThanOrEqualTo(root.get("ngayBatDau"), tuNgay);
    }

    public static Specification<PhieuGiamGia> endDateBefore(Long denNgay) {
        return (root, query, cb) -> denNgay == null ? null : cb.lessThanOrEqualTo(root.get("ngayKetThuc"), denNgay);
    }
}
