package com.example.be.core.admin.banhang.repository;

import com.example.be.entity.ChiTietSanPham;
import com.example.be.repository.ChiTietSanPhamRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminBanHangChiTietSanPhamRepository extends ChiTietSanPhamRepository {
    @Query("SELECT ct FROM ChiTietSanPham ct WHERE ct.sanPham.ten LIKE %:keyword% OR ct.maChiTietSanPham LIKE %:keyword%")
    List<ChiTietSanPham> searchByKeyword(@Param("keyword") String keyword);
}
