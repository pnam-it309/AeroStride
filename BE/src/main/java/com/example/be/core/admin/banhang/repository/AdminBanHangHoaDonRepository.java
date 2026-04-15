package com.example.be.core.admin.banhang.repository;

import com.example.be.entity.HoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.repository.HoaDonRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminBanHangHoaDonRepository extends HoaDonRepository {
    List<HoaDon> findAllByTrangThaiAndLoaiDon(OrderStatus trangThai, String loaiDon);
    long countByTrangThaiAndLoaiDon(OrderStatus trangThai, String loaiDon);
}
