package com.example.be.core.customer.order.repository;

import com.example.be.repository.HoaDonRepository;
import com.example.be.entity.HoaDon;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerOrderHoaDonRepository extends HoaDonRepository {
    List<HoaDon> findByKhachHangId(String khachHangId);
    java.util.Optional<HoaDon> findByMaHoaDon(String maHoaDon);
}
