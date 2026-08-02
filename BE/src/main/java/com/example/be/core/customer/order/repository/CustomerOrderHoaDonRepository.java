package com.example.be.core.customer.order.repository;

import com.example.be.repository.HoaDonRepository;
import com.example.be.entity.HoaDon;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerOrderHoaDonRepository extends HoaDonRepository {
    List<HoaDon> findByKhachHangId(String khachHangId);
    java.util.Optional<HoaDon> findByMaHoaDon(String maHoaDon);
    List<HoaDon> findAllBySoDienThoaiNguoiNhanOrderByNgayTaoDesc(String soDienThoaiNguoiNhan);

    @Query("SELECT h FROM HoaDon h WHERE h.soDienThoaiNguoiNhan = :sdt OR (h.khachHang IS NOT NULL AND h.khachHang.sdt = :sdt) ORDER BY h.ngayTao DESC")
    List<HoaDon> findAllBySoDienThoaiOrderByNgayTaoDesc(@Param("sdt") String sdt);
}
