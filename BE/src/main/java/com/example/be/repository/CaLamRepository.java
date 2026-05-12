package com.example.be.repository;

import com.example.be.entity.CaLam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaLamRepository extends JpaRepository<CaLam, String> {
    List<CaLam> findByXoaMemFalse();
}
