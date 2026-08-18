package com.example.be.core.admin.danhgia.repository;

import com.example.be.core.admin.danhgia.model.request.AdminDanhGiaFilterRequest;
import com.example.be.entity.DanhGiaSanPham;
import com.example.be.entity.HoaDon;
import com.example.be.entity.KhachHang;
import com.example.be.entity.SanPham;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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
                Join<DanhGiaSanPham, KhachHang> khachHangJoin = root.join("khachHang", JoinType.LEFT);
                Join<DanhGiaSanPham, SanPham> sanPhamJoin = root.join("sanPham", JoinType.LEFT);
                Join<DanhGiaSanPham, HoaDon> hoaDonJoin = root.join("hoaDon", JoinType.LEFT);

                Predicate searchTenKh = cb.like(cb.lower(khachHangJoin.get("ten")), keyword);
                Predicate searchSdt = cb.like(cb.lower(khachHangJoin.get("sdt")), keyword);
                Predicate searchTenNguoiNhan = cb.like(cb.lower(hoaDonJoin.get("tenNguoiNhan")), keyword);
                Predicate searchSdtNguoiNhan = cb.like(cb.lower(hoaDonJoin.get("soDienThoaiNguoiNhan")), keyword);
                Predicate searchTenSp = cb.like(cb.lower(sanPhamJoin.get("ten")), keyword);
                Predicate searchNoiDung = cb.like(cb.lower(root.get("noiDung")), keyword);

                predicates.add(cb.or(searchTenKh, searchSdt, searchTenNguoiNhan, searchSdtNguoiNhan, searchTenSp, searchNoiDung));
            }

            if (request.getTrangThai() != null) {
                predicates.add(cb.equal(root.get("trangThai"), request.getTrangThai()));
            }

            if (request.getDiemDanhGia() != null && request.getDiemDanhGia() > 0) {
                predicates.add(cb.equal(root.get("diemDanhGia"), request.getDiemDanhGia()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
