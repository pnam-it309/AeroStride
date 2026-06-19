package com.example.be.core.admin.lichlamviec.repository;

import com.example.be.entity.LichSuHoatDong;
import com.example.be.repository.LichSuHoatDongRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminLichSuHoatDongRepository extends LichSuHoatDongRepository, JpaSpecificationExecutor<LichSuHoatDong> {
    Page<LichSuHoatDong> findAllByOrderByNgayTaoDesc(Pageable pageable);

    @Query("SELECT h FROM LichSuHoatDong h WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(h.hanhDong) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(h.doiTuong) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(h.nguoiTao) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:ngayBatDau IS NULL OR :ngayKetThuc IS NULL OR " +
           "(h.ngayTao >= :ngayBatDau AND h.ngayTao < :ngayKetThuc))")
    Page<LichSuHoatDong> searchActivities(@Param("search") String search,
                                          @Param("ngayBatDau") Long ngayBatDau,
                                          @Param("ngayKetThuc") Long ngayKetThuc,
                                          Pageable pageable);
}
