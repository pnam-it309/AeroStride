package com.example.be.repository;

import com.example.be.entity.PhuongThucThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhuongThucThanhToanRepository extends JpaRepository<PhuongThucThanhToan, String> {
    PhuongThucThanhToan findByMa(String ma);
}
