package com.example.be.repository;

import com.example.be.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, String> {

    Optional<KhachHang> findByTenTaiKhoan(String tenTaiKhoan);

    Optional<KhachHang> findByTenTaiKhoanOrEmailOrSdtOrMa(String tenTaiKhoan, String email, String sdt, String ma);

    boolean existsByEmail(String email);
}
