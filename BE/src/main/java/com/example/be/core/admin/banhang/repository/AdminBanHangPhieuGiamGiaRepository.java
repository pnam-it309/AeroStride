package com.example.be.core.admin.banhang.repository;

import com.example.be.entity.PhieuGiamGia;
import com.example.be.repository.PhieuGiamGiaRepository;
import org.springframework.stereotype.Repository;

import com.example.be.entity.PhieuGiamGia;
import com.example.be.infrastructure.constants.TrangThai;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AdminBanHangPhieuGiamGiaRepository extends PhieuGiamGiaRepository {
    List<PhieuGiamGia> findAllByTrangThaiAndDonHangToiThieuLessThanEqual(TrangThai status, BigDecimal tongTien);
}
