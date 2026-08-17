package com.example.be.core.customer.sanpham.repository;

import com.example.be.entity.ChiTietDotGiamGia;
import com.example.be.repository.ChiTietDotGiamGiaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerSanPhamChiTietDotGiamGiaRepository extends ChiTietDotGiamGiaRepository {

    @EntityGraph(attributePaths = {"dotGiamGia", "chiTietSanPham"})
    List<ChiTietDotGiamGia> findAllByChiTietSanPhamIdIn(List<String> chiTietSanPhamIds);

    @EntityGraph(attributePaths = {"dotGiamGia", "chiTietSanPham", "chiTietSanPham.sanPham", "chiTietSanPham.sanPham.thuongHieu", "chiTietSanPham.mauSac", "chiTietSanPham.kichThuoc"})
    List<ChiTietDotGiamGia> findByDotGiamGiaId(String dotGiamGiaId);
}

