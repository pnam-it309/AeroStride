package com.example.be.core.admin.dotgiamgia.repository;

import com.example.be.core.admin.dotgiamgia.model.response.AdminDotGiamGiaResponse;
import com.example.be.entity.DotGiamGia;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AdminDotGiamGiaRepository extends JpaRepository<DotGiamGia, String> {

    @Query("""
    SELECT new com.example.be.core.admin.dotgiamgia.model.response.AdminDotGiamGiaResponse(
        d.id,
        d.ma,
        d.ten,
        d.loaiGiamGia,
        d.soTienGiam,
        d.dieuKienGiamGia,
        d.ngayBatDau,
        d.ngayKetThuc,
        d.mucUuTien,
        (CASE WHEN d.trangThai = com.example.be.infrastructure.constants.TrangThai.DANG_HOAT_DONG THEN 'DANG_HOAT_DONG' ELSE 'KHONG_HOAT_DONG' END),
        d.moTa
    )
    FROM DotGiamGia d
    WHERE (:keyword IS NULL OR :keyword = ''
        OR LOWER(d.ma) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(d.ten) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
    AND (:trangThai IS NULL OR :trangThai = ''
        OR (:trangThai = 'SAP_DIEN_RA' AND d.ngayBatDau > :now)
        OR (:trangThai = 'DANG_HOAT_DONG' AND d.ngayBatDau <= :now AND d.ngayKetThuc >= :now AND d.trangThai = com.example.be.infrastructure.constants.TrangThai.DANG_HOAT_DONG)
        OR (:trangThai = 'DA_KET_THUC' AND (d.ngayKetThuc < :now OR d.trangThai = com.example.be.infrastructure.constants.TrangThai.KHONG_HOAT_DONG))
    )
    AND (:startDate IS NULL OR d.ngayBatDau >= :startDate)
    AND (:endDate IS NULL OR d.ngayKetThuc <= :endDate)
""")
    Page<AdminDotGiamGiaResponse> phanTrang(
        @Param("keyword") String keyword,
        @Param("trangThai") String trangThai,
        @Param("now") Long now,
        @Param("startDate") Long startDate,
        @Param("endDate") Long endDate,
        Pageable pageable
    );

    @Query("""
    SELECT new com.example.be.core.admin.dotgiamgia.model.response.AdminDotGiamGiaResponse(
        d.id,
        d.ma,
        d.ten,
        d.loaiGiamGia,
        d.soTienGiam,
        d.dieuKienGiamGia,
        d.ngayBatDau,
        d.ngayKetThuc,
        d.mucUuTien,
        CAST(d.trangThai AS String),
        d.moTa
    )
    FROM DotGiamGia d
    WHERE d.id = :id
""")
    AdminDotGiamGiaResponse getDetailById(@Param("id") String id);
}
