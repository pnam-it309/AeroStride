package com.example.be.repository;

import com.example.be.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, String> {

    Optional<NhanVien> findByTenTaiKhoan(String tenTaiKhoan);

    Optional<NhanVien> findByEmail(String email);

    Optional<NhanVien> findByMa(String ma);

    Optional<NhanVien> findByTenTaiKhoanOrEmailOrSdtOrMa(String tenTaiKhoan, String email, String sdt, String ma);

    boolean existsByTenTaiKhoan(String tenTaiKhoan);
}
