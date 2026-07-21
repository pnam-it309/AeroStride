package com.example.be.repository;

import com.example.be.entity.SanPham;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, String> {
    List<SanPham> findAllByXoaMemFalseAndTrangThai(TrangThai trangThai);
}
