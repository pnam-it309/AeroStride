package com.example.be.repository;

import com.example.be.entity.CuocHoiThoai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CuocHoiThoaiRepository extends JpaRepository<CuocHoiThoai, String> {
}
