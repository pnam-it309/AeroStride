package com.example.be.core.admin.lichlamviec.repository;

import com.example.be.entity.LichSuHoatDong;
import com.example.be.repository.LichSuHoatDongRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminLichSuHoatDongRepository extends LichSuHoatDongRepository, JpaSpecificationExecutor<LichSuHoatDong> {
    Page<LichSuHoatDong> findAllByOrderByNgayTaoDesc(Pageable pageable);
}
