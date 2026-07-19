package com.example.be.repository;

import com.example.be.entity.LichSuTrangThaiHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.be.entity.HoaDon;
import java.util.List;

@Repository
public interface LichSuTrangThaiHoaDonRepository extends JpaRepository<LichSuTrangThaiHoaDon, String> {
    List<LichSuTrangThaiHoaDon> findAllByHoaDonOrderByNgayTaoDesc(HoaDon hoaDon);
}
