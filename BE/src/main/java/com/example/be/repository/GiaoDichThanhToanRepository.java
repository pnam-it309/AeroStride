package com.example.be.repository;

import com.example.be.entity.GiaoDichThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.be.entity.HoaDon;
import java.util.List;

@Repository
public interface GiaoDichThanhToanRepository extends JpaRepository<GiaoDichThanhToan, String> {
    List<GiaoDichThanhToan> findAllByHoaDon(HoaDon hoaDon);
}
