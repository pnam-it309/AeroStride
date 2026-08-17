package com.example.be.repository;

import com.example.be.entity.PhieuGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, String> {
    List<PhieuGiamGia> findAllByTrangThai(com.example.be.infrastructure.constants.TrangThai trangThai);
}
