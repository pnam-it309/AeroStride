package com.example.be.core.admin.khachhang.repository;

import com.example.be.entity.DiaChi;
import com.example.be.repository.DiaChiRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;

public interface AdminDiaChiRepository extends DiaChiRepository, JpaSpecificationExecutor<DiaChi> {

    @EntityGraph(attributePaths = {"khachHang"})
    List<DiaChi> findByKhachHangId(String khachHangId);

    DiaChi findByKhachHangIdAndLaMacDinhTrue(String khId);
}
