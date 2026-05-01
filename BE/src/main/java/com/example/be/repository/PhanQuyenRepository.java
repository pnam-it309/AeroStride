package com.example.be.repository;

import com.example.be.entity.PhanQuyen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhanQuyenRepository extends JpaRepository<PhanQuyen, String> {

    Optional<PhanQuyen> findByMaIgnoreCase(String ma);
}
