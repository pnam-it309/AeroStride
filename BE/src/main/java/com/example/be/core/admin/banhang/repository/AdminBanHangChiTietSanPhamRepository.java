package com.example.be.core.admin.banhang.repository;

import com.example.be.entity.ChiTietSanPham;
import com.example.be.repository.ChiTietSanPhamRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface AdminBanHangChiTietSanPhamRepository extends ChiTietSanPhamRepository {
    @Query("SELECT ct FROM ChiTietSanPham ct WHERE ct.sanPham.ten LIKE %:keyword% OR ct.maChiTietSanPham LIKE %:keyword%")
    List<ChiTietSanPham> searchByKeyword(@Param("keyword") String keyword);

    @Query("""
        SELECT ct FROM ChiTietSanPham ct
        WHERE (ct.xoaMem IS NULL OR ct.xoaMem = false)
        AND (
            ct.maChiTietSanPham LIKE %:keyword%
            OR ct.sanPham.ten LIKE %:keyword%
            OR ct.sanPham.ma LIKE %:keyword%
        )
        """)
    Page<ChiTietSanPham> searchByKeywordLite(@Param("keyword") String keyword, Pageable pageable);
}
