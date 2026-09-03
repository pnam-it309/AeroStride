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

    @Query(value = """
           SELECT ct.id_san_pham, act.duong_dan_anh
           FROM anh_chi_tiet_san_pham act
           INNER JOIN chi_tiet_san_pham ct ON act.id_chi_tiet_san_pham = ct.id
           INNER JOIN (
               SELECT act_sub.id_chi_tiet_san_pham, MIN(act_sub.id) AS min_act_id
               FROM anh_chi_tiet_san_pham act_sub
               WHERE (act_sub.xoa_mem = false OR act_sub.xoa_mem IS NULL)
               GROUP BY act_sub.id_chi_tiet_san_pham
           ) sub ON act.id = sub.min_act_id
           WHERE ct.id_san_pham IN (:sanPhamIds)
             AND (ct.xoa_mem = false OR ct.xoa_mem IS NULL)
           """, nativeQuery = true)
    List<Object[]> findFirstVariantImagesBySanPhamIds(@Param("sanPhamIds") Collection<String> sanPhamIds);
}
