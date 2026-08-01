package com.example.be.repository;

import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.HoaDon;
import com.example.be.entity.HoaDonChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, String> {
    List<HoaDonChiTiet> findAllByHoaDon(HoaDon hoaDon);
    HoaDonChiTiet findByHoaDonAndChiTietSanPham(HoaDon hoaDon, ChiTietSanPham chiTietSanPham);
    List<HoaDonChiTiet> findAllByHoaDonAndChiTietSanPham(HoaDon hoaDon, ChiTietSanPham chiTietSanPham);

    @Query("SELECT COALESCE(SUM(hdct.soLuong), 0) FROM HoaDonChiTiet hdct " +
           "WHERE hdct.chiTietSanPham.sanPham.id = :sanPhamId " +
           "AND hdct.hoaDon.trangThai = com.example.be.infrastructure.constants.OrderStatus.HOAN_THANH")
    Integer countSoldQuantityBySanPhamId(@Param("sanPhamId") String sanPhamId);
}
