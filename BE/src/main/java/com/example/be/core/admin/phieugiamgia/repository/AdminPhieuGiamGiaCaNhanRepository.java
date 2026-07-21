package com.example.be.core.admin.phieugiamgia.repository;

import com.example.be.entity.PhieuGiamGiaCaNhan;
import com.example.be.repository.PhieuGiamGiaCaNhanRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface AdminPhieuGiamGiaCaNhanRepository extends PhieuGiamGiaCaNhanRepository {

    @EntityGraph(attributePaths = {"khachHang", "phieuGiamGia"})
    List<PhieuGiamGiaCaNhan> findByPhieuGiamGiaId(String phieuGiamGiaId);

    @EntityGraph(attributePaths = {"khachHang", "phieuGiamGia"})
    List<PhieuGiamGiaCaNhan> findAllByPhieuGiamGiaIdIn(List<String> phieuGiamGiaIds);
}

