package com.example.be.core.admin.thongke.repository;

import com.example.be.entity.HoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

public final class AdminThongKeSpecification {

    private AdminThongKeSpecification() {
    }

    public static Specification<HoaDon> hasTrangThai(OrderStatus status) {
        return (root, query, cb) -> cb.equal(root.get("trangThai"), status);
    }

    public static Specification<HoaDon> ngayTaoGreaterOrEqual(Long tuNgay) {
        return (root, query, cb) -> {
            if (tuNgay == null) return null;
            return cb.greaterThanOrEqualTo(root.get("ngayTao"), tuNgay);
        };
    }

    public static Specification<HoaDon> ngayTaoLessOrEqual(Long denNgay) {
        return (root, query, cb) -> {
            if (denNgay == null) return null;
            return cb.lessThanOrEqualTo(root.get("ngayTao"), denNgay);
        };
    }
}
