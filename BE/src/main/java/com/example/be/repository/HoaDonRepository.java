package com.example.be.repository;

import com.example.be.entity.HoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, String> {
    List<HoaDon> findAllByTrangThaiAndLoaiDon(OrderStatus trangThai, String loaiDon);
    long countByTrangThaiAndLoaiDon(OrderStatus trangThai, String loaiDon);
}
