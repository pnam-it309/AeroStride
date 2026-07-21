package com.example.be.core.admin.banhang.repository;

import com.example.be.entity.KhachHang;
import com.example.be.repository.KhachHangRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminBanHangKhachHangRepository extends KhachHangRepository {
    @Query("SELECT k FROM KhachHang k WHERE k.ten LIKE %:keyword% OR k.sdt LIKE %:keyword% OR k.email LIKE %:keyword%")
    List<KhachHang> searchByKeyword(@Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);

    Optional<KhachHang> findFirstBySdt(String sdt);

    Optional<KhachHang> findFirstByEmail(String email);

    boolean existsByMa(String ma);
}
