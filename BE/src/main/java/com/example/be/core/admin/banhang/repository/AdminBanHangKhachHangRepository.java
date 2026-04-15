package com.example.be.core.admin.banhang.repository;

import com.example.be.entity.KhachHang;
import com.example.be.repository.KhachHangRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminBanHangKhachHangRepository extends KhachHangRepository {
    @Query("SELECT k FROM KhachHang k WHERE k.ten LIKE %:keyword% OR k.sdt LIKE %:keyword% OR k.email LIKE %:keyword%")
    List<KhachHang> searchByKeyword(@Param("keyword") String keyword);
}
