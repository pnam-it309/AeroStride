package com.example.be.repository;

import com.example.be.entity.DotGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface DotGiamGiaRepository extends JpaRepository<DotGiamGia, String>, JpaSpecificationExecutor<DotGiamGia> {
    @Query("SELECT d FROM DotGiamGia d WHERE d.isFlashSale = true AND d.trangThai = com.example.be.infrastructure.constants.TrangThai.DANG_HOAT_DONG AND d.ngayKetThuc >= :now ORDER BY d.ngayBatDau ASC")
    List<DotGiamGia> findActiveOrUpcomingFlashSales(@Param("now") Long now);
}


