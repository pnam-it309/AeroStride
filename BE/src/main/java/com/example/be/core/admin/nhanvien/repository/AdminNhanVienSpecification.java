package com.example.be.core.admin.nhanvien.repository;

import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.jpa.domain.Specification;

public final class AdminNhanVienSpecification {

    private AdminNhanVienSpecification() {}

    public static Specification<NhanVien> hasTrangThai(TrangThai status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("trangThai"), status);
    }

    public static Specification<NhanVien> hasGioiTinh(Boolean gioiTinh) {
        return (root, query, cb) -> gioiTinh == null ? null : cb.equal(root.get("gioiTinh"), gioiTinh);
    }

    public static Specification<NhanVien> keywordLike(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("ten")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("sdt")), pattern),
                cb.like(cb.lower(root.get("ma")), pattern),
                cb.like(cb.lower(root.get("tenTaiKhoan")), pattern)
            );
        };
    }
}
