package com.example.be.core.admin.danhgia.repository;

import com.example.be.core.admin.danhgia.model.request.AdminDanhGiaFilterRequest;
import com.example.be.entity.DanhGiaSanPham;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AdminDanhGiaSpecification {

    public static Specification<DanhGiaSanPham> filter(AdminDanhGiaFilterRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String keyword = "%" + request.getKeyword().trim().toLowerCase() + "%";
                Predicate searchTenKh = cb.like(cb.lower(root.join("khachHang").get("tenKhachHang")), keyword);
                Predicate searchSdt = cb.like(cb.lower(root.join("khachHang").get("soDienThoai")), keyword);
                Predicate searchTenSp = cb.like(cb.lower(root.join("sanPham").get("tenSanPham")), keyword);
                predicates.add(cb.or(searchTenKh, searchSdt, searchTenSp));
            }

            if (request.getTrangThai() != null) {
                predicates.add(cb.equal(root.get("trangThai"), request.getTrangThai()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
