package com.example.be.core.customer.sanpham.repository;

import com.example.be.entity.AnhChiTietSanPham;
import com.example.be.repository.AnhChiTietSanPhamRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CustomerSanPhamAnhChiTietRepository extends AnhChiTietSanPhamRepository {

    List<AnhChiTietSanPham> findByChiTietSanPhamIdAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(String variantId);

    List<AnhChiTietSanPham> findAllByChiTietSanPhamIdInAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(List<String> variantIds);

    @Query("SELECT ct.sanPham.id, act.duongDanAnh FROM AnhChiTietSanPham act " +
           "JOIN act.chiTietSanPham ct " +
           "WHERE ct.sanPham.id IN :sanPhamIds AND (act.xoaMem = false OR act.xoaMem IS NULL) AND (ct.xoaMem = false OR ct.xoaMem IS NULL) " +
           "ORDER BY act.hinhAnhDaiDien DESC, act.ngayTao ASC")
    List<Object[]> findFirstVariantImagesBySanPhamIds(@Param("sanPhamIds") Collection<String> sanPhamIds);
}


