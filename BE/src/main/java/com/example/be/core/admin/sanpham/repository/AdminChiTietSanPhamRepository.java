package com.example.be.core.admin.sanpham.repository;

import com.example.be.entity.ChiTietSanPham;
import com.example.be.repository.ChiTietSanPhamRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminChiTietSanPhamRepository extends ChiTietSanPhamRepository, AdminChiTietSanPhamRepositoryCustom {

    @EntityGraph(attributePaths = {"sanPham", "sanPham.thuongHieu", "sanPham.chatLieu", "mauSac", "kichThuoc"})
    Optional<ChiTietSanPham> findByIdAndXoaMemFalse(String id);

    @EntityGraph(attributePaths = {"sanPham", "sanPham.thuongHieu", "sanPham.chatLieu", "mauSac", "kichThuoc"})
    List<ChiTietSanPham> findBySanPhamIdAndXoaMemFalseOrderByNgayTaoDesc(String sanPhamId);

    @EntityGraph(attributePaths = {"sanPham", "sanPham.thuongHieu", "sanPham.chatLieu", "mauSac", "kichThuoc"})
    List<ChiTietSanPham> findBySanPhamIdAndXoaMemFalse(String sanPhamId);
 
    @EntityGraph(attributePaths = {"sanPham", "sanPham.thuongHieu", "sanPham.chatLieu", "mauSac", "kichThuoc", "chiTietDotGiamGias", "chiTietDotGiamGias.dotGiamGia"})
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

    Optional<ChiTietSanPham> findFirstByXoaMemFalseOrderByGiaBanDesc();
}
