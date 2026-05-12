package com.example.be.repository;

import com.example.be.entity.LichLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LichLamViecRepository extends JpaRepository<LichLamViec, String> {
    List<LichLamViec> findByNgayLamBetween(LocalDate start, LocalDate end);
}
