package com.example.be.repository;

import com.example.be.entity.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, String> {
    long countByTrangThai(com.example.be.infrastructure.constants.OrderStatus trangThai);
    List<HoaDon> findByTrangThai(com.example.be.infrastructure.constants.OrderStatus trangThai);
}
