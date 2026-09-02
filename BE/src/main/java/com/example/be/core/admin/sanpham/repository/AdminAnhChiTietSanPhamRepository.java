package com.example.be.core.admin.sanpham.repository;

import com.example.be.entity.AnhChiTietSanPham;
import com.example.be.repository.AnhChiTietSanPhamRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminAnhChiTietSanPhamRepository extends AnhChiTietSanPhamRepository {

    Optional<AnhChiTietSanPham> findByIdAndXoaMemFalse(String id);

    List<AnhChiTietSanPham> findByChiTietSanPhamIdAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(String variantId);

    List<AnhChiTietSanPham> findByChiTietSanPhamIdAndXoaMemFalse(String variantId);

    List<AnhChiTietSanPham> findAllByChiTietSanPhamIdInAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(List<String> variantIds);

    Optional<AnhChiTietSanPham> findFirstByChiTietSanPhamIdAndXoaMemFalseOrderByNgayTaoAsc(String variantId);

    @org.springframework.data.jpa.repository.Query("SELECT a.chiTietSanPham.id AS variantId, a.duongDanAnh AS url, a.hinhAnhDaiDien AS isMain " +
            "FROM AnhChiTietSanPham a WHERE a.chiTietSanPham.id IN :variantIds AND a.xoaMem = false " +
            "ORDER BY a.hinhAnhDaiDien DESC, a.ngayTao ASC")
    List<com.example.be.core.admin.sanpham.model.response.VariantThumbnailProjection> findThumbnailProjectionsByVariantIds(
            @org.springframework.data.repository.query.Param("variantIds") List<String> variantIds
    );
}
