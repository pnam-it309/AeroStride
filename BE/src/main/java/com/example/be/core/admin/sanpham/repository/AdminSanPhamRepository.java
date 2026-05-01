package com.example.be.core.admin.sanpham.repository;

import com.example.be.entity.SanPham;
import com.example.be.repository.SanPhamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminSanPhamRepository extends SanPhamRepository, JpaSpecificationExecutor<SanPham> {

    @Override
    @EntityGraph(attributePaths = {
            "thuongHieu",
            "danhMuc",
            "xuatXu",
            "mucDichChay",
            "coGiay",
            "chatLieu",
            "deGiay"
    })
    Page<SanPham> findAll(@Nullable Specification<SanPham> spec, Pageable pageable);

    @EntityGraph(attributePaths = {
            "thuongHieu",
            "danhMuc",
            "xuatXu",
            "mucDichChay",
            "coGiay",
            "chatLieu",
            "deGiay"
    })
    Optional<SanPham> findByIdAndXoaMemFalse(String id);

    boolean existsByMaIgnoreCaseAndXoaMemFalse(String ma);

    boolean existsByMaIgnoreCaseAndXoaMemFalseAndIdNot(String ma, String id);
}
