package com.example.be.core.customer.landing.repository;

import com.example.be.entity.SanPham;
import com.example.be.repository.SanPhamRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerLandingRepository extends SanPhamRepository, JpaSpecificationExecutor<SanPham> {

    @Override
    @EntityGraph(attributePaths = {"thuongHieu"})
    org.springframework.data.domain.Page<SanPham> findAll(
            @org.springframework.lang.Nullable org.springframework.data.jpa.domain.Specification<SanPham> spec,
            org.springframework.data.domain.Pageable pageable
    );
}
