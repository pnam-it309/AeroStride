package com.example.be.core.admin.giaoca.repository;

import com.example.be.entity.GiaoCa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminGiaoCaRepository extends JpaRepository<GiaoCa, String> {

    @Query("SELECT gc FROM GiaoCa gc WHERE gc.nhanVienTrongCa.id = :nhanVienId AND gc.trangThai = 'OPEN'")
    Optional<GiaoCa> findGiaoCaHienTai(String nhanVienId);

    @Query("SELECT gc FROM GiaoCa gc ORDER BY gc.thoiGianVaoCa DESC")
    List<GiaoCa> findAllOrderByThoiGianVaoCaDesc();
}
