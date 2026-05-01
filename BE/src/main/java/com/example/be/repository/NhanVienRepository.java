package com.example.be.repository;

import com.example.be.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, String> {

    Optional<NhanVien> findByTenTaiKhoan(String tenTaiKhoan);

    Optional<NhanVien> findByEmail(String email);

    Optional<NhanVien> findByTenTaiKhoanOrEmail(String tenTaiKhoan, String email);

    boolean existsByTenTaiKhoan(String tenTaiKhoan);

    boolean existsByEmail(String email);

    List<NhanVien> findByResetStatus(NhanVien.ResetStatus resetStatus);

    List<NhanVien> id(String id);
}
