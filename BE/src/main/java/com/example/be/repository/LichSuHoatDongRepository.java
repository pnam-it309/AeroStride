package com.example.be.repository;

import com.example.be.entity.LichSuHoatDong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichSuHoatDongRepository extends JpaRepository<LichSuHoatDong, String> {
}
