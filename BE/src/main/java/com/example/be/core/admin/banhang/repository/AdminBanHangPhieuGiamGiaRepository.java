package com.example.be.core.admin.banhang.repository;

import com.example.be.entity.PhieuGiamGia;
import com.example.be.repository.PhieuGiamGiaRepository;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminBanHangPhieuGiamGiaRepository extends PhieuGiamGiaRepository {
    List<PhieuGiamGia> findAllByTrangThai(TrangThai status);
}
