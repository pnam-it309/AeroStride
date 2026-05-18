package com.example.be.repository;

import com.example.be.entity.LichLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichLamViecRepository extends JpaRepository<LichLamViec, String> {
}
