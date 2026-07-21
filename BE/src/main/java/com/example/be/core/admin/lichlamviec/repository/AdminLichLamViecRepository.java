package com.example.be.core.admin.lichlamviec.repository;

import com.example.be.entity.LichLamViec;
import com.example.be.repository.LichLamViecRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AdminLichLamViecRepository extends LichLamViecRepository, JpaSpecificationExecutor<LichLamViec> {
    List<LichLamViec> findByNgayLamBetween(LocalDate start, LocalDate end);
}
