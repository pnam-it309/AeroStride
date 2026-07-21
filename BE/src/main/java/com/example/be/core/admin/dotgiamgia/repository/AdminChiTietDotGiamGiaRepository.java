package com.example.be.core.admin.dotgiamgia.repository;

import com.example.be.entity.ChiTietDotGiamGia;
import com.example.be.repository.ChiTietDotGiamGiaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminChiTietDotGiamGiaRepository extends ChiTietDotGiamGiaRepository, JpaSpecificationExecutor<ChiTietDotGiamGia> {
    void deleteByDotGiamGiaId(String id);

    @EntityGraph(attributePaths = {"chiTietSanPham", "chiTietSanPham.sanPham", "chiTietSanPham.sanPham.thuongHieu", "chiTietSanPham.sanPham.chatLieu", "chiTietSanPham.mauSac", "chiTietSanPham.kichThuoc", "chiTietSanPham.chiTietDotGiamGias", "chiTietSanPham.chiTietDotGiamGias.dotGiamGia"})
    List<ChiTietDotGiamGia> findByDotGiamGiaId(String id);

    @EntityGraph(attributePaths = {"dotGiamGia", "chiTietSanPham"})
    List<ChiTietDotGiamGia> findAllByChiTietSanPhamIdIn(List<String> chiTietSanPhamIds);
}
