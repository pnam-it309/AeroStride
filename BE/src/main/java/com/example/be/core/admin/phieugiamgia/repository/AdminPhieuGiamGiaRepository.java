package com.example.be.core.admin.phieugiamgia.repository;

import com.example.be.entity.PhieuGiamGia;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.repository.PhieuGiamGiaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AdminPhieuGiamGiaRepository extends PhieuGiamGiaRepository, JpaSpecificationExecutor<PhieuGiamGia> {
    List<PhieuGiamGia> findByTrangThaiAndNgayKetThucLessThan(TrangThai trangThai, Long now);
    boolean existsByMa(String ma);
}
