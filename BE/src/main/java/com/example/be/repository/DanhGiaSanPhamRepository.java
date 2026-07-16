package com.example.be.repository;

import com.example.be.entity.DanhGiaSanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DanhGiaSanPhamRepository extends JpaRepository<DanhGiaSanPham, String> {
    
    Page<DanhGiaSanPham> findBySanPhamIdAndTrangThaiDanhGia(String idSanPham, DanhGiaSanPham.TrangThaiDanhGia trangThai, Pageable pageable);
    
    List<DanhGiaSanPham> findByKhachHangId(String idKhachHang);
    
    boolean existsByHoaDonIdAndSanPhamId(String idHoaDon, String idSanPham);
    
    Page<DanhGiaSanPham> findAll(Pageable pageable);
}
