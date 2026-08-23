package com.example.be.repository;

import com.example.be.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, String> {

    Optional<KhachHang> findByTenTaiKhoan(String tenTaiKhoan);

    Optional<KhachHang> findByTenTaiKhoanIgnoreCase(String tenTaiKhoan);

    Optional<KhachHang> findFirstBySdt(String sdt);

    Optional<KhachHang> findFirstByEmail(String email);

    Optional<KhachHang> findFirstByEmailIgnoreCase(String email);

    Optional<KhachHang> findByTenTaiKhoanOrEmailOrSdtOrMa(String tenTaiKhoan, String email, String sdt, String ma);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByTenTaiKhoanIgnoreCase(String tenTaiKhoan);

    @Query("SELECT k FROM KhachHang k WHERE k.ngaySinh IS NOT NULL AND MONTH(k.ngaySinh) = :month AND DAY(k.ngaySinh) = :day AND (k.xoaMem IS NULL OR k.xoaMem = false)")
    List<KhachHang> findCustomersWithBirthdayToday(@Param("month") int month, @Param("day") int day);
}
