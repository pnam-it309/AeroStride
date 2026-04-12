package com.example.be.core.admin.khachhang.repository;

import com.example.be.entity.DiaChi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AdminDiaChiRepository extends JpaRepository<DiaChi, String> {
    List<DiaChi> findByKhachHangId(String khachHangId);

    @Query("SELECT d FROM DiaChi d WHERE d.khachHang.id = :khId AND d.laMacDinh = true")
    DiaChi findDefaultByKhachHangId(@Param("khId") String khId);
}
