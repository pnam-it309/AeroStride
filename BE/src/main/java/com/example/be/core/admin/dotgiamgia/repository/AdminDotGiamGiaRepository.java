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
        CAST(d.trangThai AS String),
        d.moTa
    )
    FROM DotGiamGia d
    WHERE (:keyword IS NULL OR :keyword = ''
        OR LOWER(d.ma) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(d.ten) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
""")
    Page<AdminDotGiamGiaResponse> phanTrang(
        @Param("keyword") String keyword,
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
