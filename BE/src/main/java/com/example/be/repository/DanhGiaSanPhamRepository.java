package com.example.be.repository;

import com.example.be.entity.DanhGiaSanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DanhGiaSanPhamRepository extends JpaRepository<DanhGiaSanPham, String> {
    
    Page<DanhGiaSanPham> findBySanPham_IdAndTrangThai(String idSanPham, DanhGiaSanPham.TrangThaiDanhGia trangThai, Pageable pageable);
    
    List<DanhGiaSanPham> findByKhachHang_Id(String idKhachHang);
    
    boolean existsByHoaDon_IdAndSanPham_Id(String idHoaDon, String idSanPham);
    
    Page<DanhGiaSanPham> findAll(Pageable pageable);
}

