package com.example.be.core.customer.sanpham.repository;

import com.example.be.entity.SanPham;
import com.example.be.repository.SanPhamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerSanPhamRepository extends SanPhamRepository, JpaSpecificationExecutor<SanPham> {

    @Override
    @EntityGraph(attributePaths = {
            "thuongHieu",
            
            "xuatXu",
            "mucDichChay",
            "coGiay",
            "chatLieu",
            "deGiay"
    })
    Page<SanPham> findAll(@Nullable Specification<SanPham> spec, Pageable pageable);

    @EntityGraph(attributePaths = {
            "thuongHieu",
            
            "xuatXu",
            "mucDichChay",
            "coGiay",
            "chatLieu",
            "deGiay"
    })
    Optional<SanPham> findByIdAndXoaMemFalse(String id);

    /**
     * Tìm sản phẩm theo ID, cho phép xoaMem = null hoặc false (không bị xóa mềm).
     * Fix: findByIdAndXoaMemFalse không match khi xoaMem = null (default).
     */
    @EntityGraph(attributePaths = {
            "thuongHieu",
            "xuatXu",
            "mucDichChay",
            "coGiay",
            "chatLieu",
            "deGiay"
    })
    @Query("SELECT sp FROM SanPham sp WHERE sp.id = :id AND (sp.xoaMem IS NULL OR sp.xoaMem = false)")
    Optional<SanPham> findByIdNotDeleted(@Param("id") String id);
}
