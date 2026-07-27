package com.example.be.core.admin.banhang.repository;

import com.example.be.entity.HoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.repository.HoaDonRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminBanHangHoaDonRepository extends HoaDonRepository {
    @Query("SELECT h FROM HoaDon h WHERE h.trangThai = :trangThai AND (h.loaiDon IN ('TAI_QUAY', 'GIAO_HANG', 'ONLINE') OR h.loaiDon IS NULL) ORDER BY h.ngayTao ASC")
    List<HoaDon> findAllPendingPOSOrders(@Param("trangThai") OrderStatus trangThai);

    @Query("SELECT COUNT(h) FROM HoaDon h WHERE h.trangThai = :trangThai AND (h.loaiDon IN ('TAI_QUAY', 'GIAO_HANG', 'ONLINE') OR h.loaiDon IS NULL)")
    long countPendingPOSOrders(@Param("trangThai") OrderStatus trangThai);

    List<HoaDon> findAllByTrangThaiAndLoaiDon(OrderStatus trangThai, String loaiDon);
    long countByTrangThaiAndLoaiDon(OrderStatus trangThai, String loaiDon);
}
