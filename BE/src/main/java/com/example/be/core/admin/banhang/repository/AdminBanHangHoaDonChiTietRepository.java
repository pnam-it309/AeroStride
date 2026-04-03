package com.example.be.core.admin.banhang.repository;

import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.HoaDon;
import com.example.be.entity.HoaDonChiTiet;
import com.example.be.repository.HoaDonChiTietRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminBanHangHoaDonChiTietRepository extends HoaDonChiTietRepository {
    List<HoaDonChiTiet> findAllByHoaDon(HoaDon hoaDon);
    HoaDonChiTiet findByHoaDonAndChiTietSanPham(HoaDon hoaDon, ChiTietSanPham chiTietSanPham);
}
