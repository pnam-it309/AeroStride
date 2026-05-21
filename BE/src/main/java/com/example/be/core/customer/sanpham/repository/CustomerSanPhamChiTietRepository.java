package com.example.be.core.customer.sanpham.repository;

import com.example.be.core.customer.sanpham.model.response.CustomerProductVariantStats;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.repository.ChiTietSanPhamRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerSanPhamChiTietRepository extends ChiTietSanPhamRepository {

    @EntityGraph(attributePaths = {"sanPham", "sanPham.thuongHieu", "sanPham.chatLieu", "mauSac", "kichThuoc"})
    Optional<ChiTietSanPham> findByIdAndXoaMemFalse(String id);

    @EntityGraph(attributePaths = {"sanPham", "sanPham.thuongHieu", "sanPham.chatLieu", "mauSac", "kichThuoc"})
    List<ChiTietSanPham> findBySanPhamIdAndXoaMemFalseOrderByNgayTaoDesc(String sanPhamId);

    @EntityGraph(attributePaths = {"sanPham", "sanPham.thuongHieu", "sanPham.chatLieu", "mauSac", "kichThuoc"})
    List<ChiTietSanPham> findBySanPhamIdAndXoaMemFalse(String sanPhamId);

    @Query("SELECT new com.example.be.core.customer.sanpham.model.response.CustomerProductVariantStats(" +
           "ct.sanPham.id, COUNT(ct.id), SUM(ct.soLuong), MIN(ct.giaBan), MAX(ct.giaBan)) " +
           "FROM ChiTietSanPham ct WHERE ct.sanPham.id IN :productIds AND ct.xoaMem = false " +
           "GROUP BY ct.sanPham.id")
    List<CustomerProductVariantStats> summarizeBySanPhamIds(@Param("productIds") Collection<String> productIds);
}
