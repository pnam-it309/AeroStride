package com.example.be.repository;

import com.example.be.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, String> {

    Optional<KhachHang> findByTenTaiKhoan(String tenTaiKhoan);

    Optional<KhachHang> findByEmail(String email);

    Optional<KhachHang> findByTenTaiKhoanOrEmail(String tenTaiKhoan, String email);

    boolean existsByTenTaiKhoan(String tenTaiKhoan);

    boolean existsByEmail(String email);

    @Query("SELECT k FROM KhachHang k WHERE k.ten LIKE %:keyword% OR k.sdt LIKE %:keyword% OR k.email LIKE %:keyword%")
    List<KhachHang> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT k FROM KhachHang k WHERE k.diaChi.id = :diaChiId")
    List<KhachHang> findByDiaChiId(@Param("diaChiId") String diaChiId);
}
