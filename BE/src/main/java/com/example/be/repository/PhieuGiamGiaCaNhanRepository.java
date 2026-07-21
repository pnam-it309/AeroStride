package com.example.be.repository;

import com.example.be.entity.PhieuGiamGiaCaNhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuGiamGiaCaNhanRepository extends JpaRepository<PhieuGiamGiaCaNhan, String> {

    // Danh sách phiếu giảm giá cá nhân đã phát cho một khách hàng
    List<PhieuGiamGiaCaNhan> findByKhachHangId(String khachHangId);
}
