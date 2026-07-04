package com.example.be.core.admin.sanpham.repository;

import com.example.be.entity.ChiTietSanPham;
import com.example.be.repository.ChiTietSanPhamRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminChiTietSanPhamRepository extends ChiTietSanPhamRepository, AdminChiTietSanPhamRepositoryCustom, JpaSpecificationExecutor<ChiTietSanPham> {

    @EntityGraph(attributePaths = {"sanPham", "sanPham.thuongHieu", "sanPham.chatLieu", "mauSac", "kichThuoc"})
    Optional<ChiTietSanPham> findByIdAndXoaMemFalse(String id);

    @EntityGraph(attributePaths = {"sanPham", "sanPham.thuongHieu", "sanPham.chatLieu", "mauSac", "kichThuoc"})
    List<ChiTietSanPham> findBySanPhamIdAndXoaMemFalseOrderByNgayTaoDesc(String sanPhamId);

    @EntityGraph(attributePaths = {"sanPham", "sanPham.thuongHieu", "sanPham.chatLieu", "mauSac", "kichThuoc"})
    List<ChiTietSanPham> findBySanPhamIdAndXoaMemFalse(String sanPhamId);
 
    @EntityGraph(attributePaths = {"sanPham", "sanPham.thuongHieu", "sanPham.chatLieu", "mauSac", "kichThuoc"})
    List<ChiTietSanPham> findAllByXoaMemFalse();

    @org.springframework.data.jpa.repository.Query("SELECT ct FROM ChiTietSanPham ct " +
            "JOIN ct.sanPham sp " +
            "LEFT JOIN sp.thuongHieu th " +
            "WHERE ct.xoaMem = false AND ct.trangThai = com.example.be.infrastructure.constants.TrangThai.DANG_HOAT_DONG " +
            "AND (sp.ten LIKE %:keyword% OR th.ten LIKE %:keyword% OR :keyword IS NULL) " +
            "AND (ct.giaBan <= :maxPrice OR :maxPrice IS NULL) " +
            "AND (ct.giaBan >= :minPrice OR :minPrice IS NULL) " +
            "ORDER BY ct.giaBan ASC")
    @EntityGraph(attributePaths = {"sanPham", "sanPham.thuongHieu", "mauSac", "kichThuoc"})
    List<ChiTietSanPham> searchVariantsForAi(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("minPrice") java.math.BigDecimal minPrice,
            @org.springframework.data.repository.query.Param("maxPrice") java.math.BigDecimal maxPrice,
            org.springframework.data.domain.Pageable pageable
    );

    Optional<ChiTietSanPham> findBySanPhamIdAndMauSacIdAndKichThuocIdAndXoaMemFalse(String sanPhamId, String mauSacId, String kichThuocId);

    boolean existsBySanPhamIdAndMauSacIdAndKichThuocIdAndXoaMemFalse(String sanPhamId, String mauSacId, String kichThuocId);

    boolean existsBySanPhamIdAndMauSacIdAndKichThuocIdAndXoaMemFalseAndIdNot(
            String sanPhamId,
            String mauSacId,
            String kichThuocId,
            String id
    );

    boolean existsByMaChiTietSanPhamIgnoreCaseAndXoaMemFalse(String maChiTietSanPham);

    boolean existsByMaChiTietSanPhamIgnoreCaseAndXoaMemFalseAndIdNot(String maChiTietSanPham, String id);

    Optional<ChiTietSanPham> findFirstByXoaMemFalseOrderByGiaBanDesc();
}
