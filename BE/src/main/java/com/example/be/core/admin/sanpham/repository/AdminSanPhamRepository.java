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

    boolean existsByMaIgnoreCaseAndXoaMemFalse(String ma);

    boolean existsByMaIgnoreCaseAndXoaMemFalseAndIdNot(String ma, String id);

    boolean existsByTenIgnoreCaseAndXoaMemFalse(String ten);

    boolean existsByTenIgnoreCaseAndXoaMemFalseAndIdNot(String ten, String id);

    Optional<SanPham> findByTenIgnoreCaseAndXoaMemFalse(String ten);

    boolean existsByMa(String ma);

    Optional<SanPham> findFirstByThuongHieuIdAndXuatXuIdAndMucDichChayIdAndCoGiayIdAndChatLieuIdAndDeGiayIdAndXoaMemFalse(
            String idThuongHieu, String idXuatXu, String idMucDichChay, String idCoGiay, String idChatLieu, String idDeGiay
    );
}
