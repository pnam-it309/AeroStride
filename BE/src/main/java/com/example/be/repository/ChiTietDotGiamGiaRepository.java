package com.example.be.repository;

import com.example.be.entity.ChiTietDotGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
 
@Repository
public interface ChiTietDotGiamGiaRepository extends JpaRepository<ChiTietDotGiamGia, String> {
    void deleteByDotGiamGiaId(String id);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"chiTietSanPham", "chiTietSanPham.sanPham", "chiTietSanPham.sanPham.thuongHieu", "chiTietSanPham.sanPham.chatLieu", "chiTietSanPham.mauSac", "chiTietSanPham.kichThuoc"})
    List<ChiTietDotGiamGia> findByDotGiamGiaId(String id);
}
