package com.example.be.core.admin.phieugiamgia.repository;

import com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse;
import com.example.be.entity.PhieuGiamGia;
import com.example.be.infrastructure.constants.TrangThai;
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
            (CASE WHEN p.trangThai = com.example.be.infrastructure.constants.TrangThai.DANG_HOAT_DONG THEN 'DANG_HOAT_DONG' ELSE 'KHONG_HOAT_DONG' END)
        )
        FROM PhieuGiamGia p
        """)
    List<AdminPhieuGiamGiaResponse> hienThi();

    @Query("""
            SELECT new com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse(
                p.id, p.ma, p.ten, p.loaiPhieu, p.hinhThuc, p.phanTramGiamGia, p.soTienGiam,
                p.soLuong, p.donHangToiThieu, p.giamToiDa, p.ngayBatDau, p.ngayKetThuc, p.ghiChu, 
                (CASE WHEN p.trangThai = com.example.be.infrastructure.constants.TrangThai.DANG_HOAT_DONG THEN 'DANG_HOAT_DONG' ELSE 'KHONG_HOAT_DONG' END)
            )
            FROM PhieuGiamGia p WHERE p.id = :id
        """)
    AdminPhieuGiamGiaResponse detail(String id);

    @Query("""
            SELECT new com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse(
                p.id, p.ma, p.ten, p.loaiPhieu, p.hinhThuc, p.phanTramGiamGia, p.soTienGiam,
                p.soLuong, p.donHangToiThieu, p.giamToiDa,
                p.ngayBatDau, p.ngayKetThuc, p.ghiChu, 
                (CASE WHEN p.trangThai = com.example.be.infrastructure.constants.TrangThai.DANG_HOAT_DONG THEN 'DANG_HOAT_DONG' ELSE 'KHONG_HOAT_DONG' END)
            )
            FROM PhieuGiamGia p
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR p.ma LIKE %:keyword%
                   OR p.ten LIKE %:keyword%)
            AND (:loaiPhieu IS NULL OR p.loaiPhieu = :loaiPhieu)
            AND (:hinhThuc IS NULL 
                 OR (:hinhThuc = 'CONG_KHAI' AND (p.hinhThuc IS NULL OR p.hinhThuc = 'CONG_KHAI' OR p.hinhThuc = ''))
                 OR (p.hinhThuc = :hinhThuc))
            AND (:trangThai IS NULL OR p.trangThai = :trangThai)
            AND (:timelineStatus IS NULL OR :timelineStatus = ''
                 OR (:timelineStatus = 'SAP_DIEN_RA' AND p.ngayBatDau > :now)
                 OR (:timelineStatus = 'DANG_HOAT_DONG' AND p.ngayBatDau <= :now AND p.ngayKetThuc >= :now AND p.trangThai = com.example.be.infrastructure.constants.TrangThai.DANG_HOAT_DONG)
                 OR (:timelineStatus = 'DA_KET_THUC' AND (p.ngayKetThuc < :now OR p.trangThai = com.example.be.infrastructure.constants.TrangThai.KHONG_HOAT_DONG)))
            AND (:tuNgay IS NULL OR p.ngayBatDau >= :tuNgay)
            AND (:denNgay IS NULL OR p.ngayKetThuc <= :denNgay)
        """)
    Page<AdminPhieuGiamGiaResponse> phanTrang(
        @Param("keyword") String keyword,
        @Param("loaiPhieu") String loaiPhieu,
        @Param("hinhThuc") String hinhThuc,
        @Param("trangThai") TrangThai trangThai,
        @Param("timelineStatus") String timelineStatus,
        @Param("now") Long now,
        @Param("tuNgay") Long tuNgay,
        @Param("denNgay") Long denNgay,
        Pageable pageable
    );
}
