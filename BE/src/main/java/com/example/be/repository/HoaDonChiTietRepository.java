package com.example.be.repository;

import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.HoaDon;
import com.example.be.entity.HoaDonChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, String> {
    List<HoaDonChiTiet> findAllByHoaDon(HoaDon hoaDon);
    HoaDonChiTiet findByHoaDonAndChiTietSanPham(HoaDon hoaDon, ChiTietSanPham chiTietSanPham);
}
