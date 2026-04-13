package com.example.be.core.admin.phieugiamgia.repository;

import com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse;
import com.example.be.entity.PhieuGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminPhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, String> {

    @Query("""
SELECT new com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse(
    p.id,
    p.ma,
    p.ten,
    p.loaiPhieu,
    p.hinhThuc,
    p.phanTramGiamGia,
    p.soTienGiam,
    p.soLuong,
    p.donHangToiThieu,
    p.giamToiDa,
    p.ngayBatDau,
    p.ngayKetThuc,
    p.ghiChu,
    CAST(p.trangThai AS String)
)
FROM PhieuGiamGia p
""")
    List<AdminPhieuGiamGiaResponse> hienThi();

    @Query("""
    SELECT new com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse(
        p.id, p.ma, p.ten, p.loaiPhieu, p.hinhThuc, p.phanTramGiamGia, p.soTienGiam,
        p.soLuong, p.donHangToiThieu, p.giamToiDa, p.ngayBatDau, p.ngayKetThuc, p.ghiChu, CAST(p.trangThai AS String)
    )
    FROM PhieuGiamGia p WHERE p.id = :id
""")
    AdminPhieuGiamGiaResponse detail(String id);

    @Query("""
        SELECT new com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse(
            p.id, p.ma, p.ten, p.loaiPhieu, p.hinhThuc, p.phanTramGiamGia, p.soTienGiam,
            p.soLuong, p.donHangToiThieu, p.giamToiDa,
            p.ngayBatDau, p.ngayKetThuc, p.ghiChu, CAST(p.trangThai AS String)
        )
        FROM PhieuGiamGia p
        WHERE (:keyword IS NULL OR :keyword = ''
               OR p.ma LIKE %:keyword%
               OR p.ten LIKE %:keyword%)
    """)
    Page<AdminPhieuGiamGiaResponse> phanTrang(
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
