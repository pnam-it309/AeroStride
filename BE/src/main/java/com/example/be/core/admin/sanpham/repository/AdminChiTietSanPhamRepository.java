package com.example.be.core.admin.sanpham.repository;

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
public interface AdminChiTietSanPhamRepository extends ChiTietSanPhamRepository {

    @EntityGraph(attributePaths = {"sanPham", "mauSac", "kichThuoc"})
    Optional<ChiTietSanPham> findByIdAndXoaMemFalse(String id);

    @EntityGraph(attributePaths = {"sanPham", "mauSac", "kichThuoc"})
    List<ChiTietSanPham> findBySanPhamIdAndXoaMemFalseOrderByNgayTaoDesc(String sanPhamId);

    @EntityGraph(attributePaths = {"sanPham", "mauSac", "kichThuoc"})
    List<ChiTietSanPham> findBySanPhamIdAndXoaMemFalse(String sanPhamId);
 
    @EntityGraph(attributePaths = {"sanPham", "mauSac", "kichThuoc"})
    List<ChiTietSanPham> findAllByXoaMemFalse();

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

    @Query("""
            select
                c.sanPham.id as sanPhamId,
                count(c.id) as tongBienThe,
                coalesce(sum(c.soLuong), 0) as tongSoLuongTon,
                min(c.giaBan) as giaBanThapNhat,
                max(c.giaBan) as giaBanCaoNhat
            from ChiTietSanPham c
            where c.sanPham.id in :productIds
              and c.xoaMem = false
            group by c.sanPham.id
            """)
    List<ProductVariantStatisticsProjection> summarizeBySanPhamIds(@Param("productIds") Collection<String> productIds);
}
