package com.example.be.core.admin.banhang.repository;

import com.example.be.entity.HoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.OrderType;
import com.example.be.repository.HoaDonRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminBanHangHoaDonRepository extends HoaDonRepository {
    /**
     * Lấy hóa đơn chờ được tạo từ màn POS.
     * loaiDon là kênh nhận hàng và có thể đổi từ TAI_QUAY sang GIAO_HANG, vì vậy
     * không được dùng trường này để nhận diện nguồn tạo hóa đơn.
     * Nhánh orderType null hỗ trợ các hóa đơn POS cũ được tạo trước khi có phân loại nguồn đơn.
     */
    @Query("""
        SELECT h FROM HoaDon h
        WHERE h.trangThai = :trangThai
          AND (
              h.orderType = :orderType
              OR (h.orderType IS NULL AND (h.nhanVien IS NOT NULL OR h.loaiDon = 'TAI_QUAY'))
          )
          AND (:idNhanVien IS NULL OR h.nhanVien.id = :idNhanVien)
        ORDER BY h.ngayTao ASC
        """)
    List<HoaDon> findAllPendingPOSOrders(
            @Param("trangThai") OrderStatus trangThai,
            @Param("orderType") OrderType orderType,
            @Param("idNhanVien") String idNhanVien
    );

    @Query("""
        SELECT COUNT(h) FROM HoaDon h
        WHERE h.trangThai = :trangThai
          AND (
              h.orderType = :orderType
              OR (h.orderType IS NULL AND (h.nhanVien IS NOT NULL OR h.loaiDon = 'TAI_QUAY'))
          )
          AND (:idNhanVien IS NULL OR h.nhanVien.id = :idNhanVien)
        """)
    long countPendingPOSOrders(
            @Param("trangThai") OrderStatus trangThai,
            @Param("orderType") OrderType orderType,
            @Param("idNhanVien") String idNhanVien
    );

    List<HoaDon> findAllByTrangThaiAndLoaiDon(OrderStatus trangThai, String loaiDon);
    long countByTrangThaiAndLoaiDon(OrderStatus trangThai, String loaiDon);
}
