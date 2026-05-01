package com.example.be.core.admin.khachhang.repository;

import com.example.be.entity.DiaChi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AdminDiaChiRepository extends JpaRepository<DiaChi, String> {
    @Query("SELECT DISTINCT d FROM DiaChi d LEFT JOIN FETCH d.khachHang WHERE d.khachHang.id = :khachHangId OR d.id IN (SELECT k.diaChi.id FROM KhachHang k WHERE k.id = :khachHangId)")
    List<DiaChi> findByKhachHangId(@Param("khachHangId") String khachHangId);

    @Query("SELECT d FROM DiaChi d WHERE d.khachHang.id = :khId AND d.laMacDinh = true")
    DiaChi findDefaultByKhachHangId(@Param("khId") String khId);
}
