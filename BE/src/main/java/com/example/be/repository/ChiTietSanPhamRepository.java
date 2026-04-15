package com.example.be.repository;

import com.example.be.entity.ChiTietSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, String> {
    @Query("SELECT ct FROM ChiTietSanPham ct WHERE ct.sanPham.ten LIKE %:keyword% OR ct.maChiTietSanPham LIKE %:keyword%")
    List<ChiTietSanPham> searchByKeyword(@Param("keyword") String keyword);
}
