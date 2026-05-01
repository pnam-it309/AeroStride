package com.example.be.repository;

import com.example.be.entity.DiaChi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface DiaChiRepository extends JpaRepository<DiaChi, String> {
    @org.springframework.data.jpa.repository.Query("SELECT d.maDiaChi FROM DiaChi d")
    java.util.List<String> findAllMa();
}
