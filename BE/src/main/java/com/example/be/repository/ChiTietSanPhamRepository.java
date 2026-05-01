package com.example.be.repository;

import com.example.be.entity.ChiTietSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, String> {
    @Query("""
        SELECT ct
        FROM ChiTietSanPham ct
        LEFT JOIN ct.sanPham sp
        LEFT JOIN sp.danhMuc dm
        LEFT JOIN sp.thuongHieu th
        LEFT JOIN sp.chatLieu cl
        LEFT JOIN sp.deGiay dg
        LEFT JOIN ct.kichThuoc kt
        LEFT JOIN ct.mauSac ms
        WHERE (:keyword IS NULL OR :keyword = '' OR sp.ten LIKE %:keyword% OR ct.maChiTietSanPham LIKE %:keyword%)
          AND (ct.xoaMem IS NULL OR ct.xoaMem = false)
          AND (sp.xoaMem IS NULL OR sp.xoaMem = false)
        """)
    Page<ChiTietSanPham> searchByKeywordLite(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
        SELECT DISTINCT ct
        FROM ChiTietSanPham ct
        LEFT JOIN FETCH ct.sanPham sp
        LEFT JOIN FETCH sp.danhMuc dm
        LEFT JOIN FETCH sp.thuongHieu th
        LEFT JOIN FETCH sp.chatLieu cl
        LEFT JOIN FETCH sp.deGiay dg
        LEFT JOIN FETCH ct.kichThuoc kt
        LEFT JOIN FETCH ct.mauSac ms
        LEFT JOIN FETCH ct.anhChiTietSanPhams imgs
        WHERE (sp.ten LIKE %:keyword% OR ct.maChiTietSanPham LIKE %:keyword%)
          AND (ct.xoaMem IS NULL OR ct.xoaMem = false)
          AND (sp.xoaMem IS NULL OR sp.xoaMem = false)
        """)
    List<ChiTietSanPham> searchByKeyword(@Param("keyword") String keyword);
}
