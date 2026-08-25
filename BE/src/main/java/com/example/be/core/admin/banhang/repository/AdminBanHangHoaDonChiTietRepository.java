package com.example.be.core.admin.banhang.repository;

import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.HoaDon;
import com.example.be.entity.HoaDonChiTiet;
import com.example.be.repository.HoaDonChiTietRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminBanHangHoaDonChiTietRepository extends HoaDonChiTietRepository {
    List<HoaDonChiTiet> findAllByHoaDon(HoaDon hoaDon);
    HoaDonChiTiet findByHoaDonAndChiTietSanPham(HoaDon hoaDon, ChiTietSanPham chiTietSanPham);

    @Query("""
        SELECT d FROM HoaDonChiTiet d
        JOIN FETCH d.chiTietSanPham ct
        LEFT JOIN FETCH ct.sanPham sp
        LEFT JOIN FETCH ct.mauSac ms
        LEFT JOIN FETCH ct.kichThuoc kt
        WHERE d.hoaDon.id IN :hoaDonIds
    """)
    List<HoaDonChiTiet> findAllByHoaDonIdInWithDetails(@Param("hoaDonIds") List<String> hoaDonIds);
}

