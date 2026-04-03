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

    Optional<AnhChiTietSanPham> findFirstByChiTietSanPhamIdAndXoaMemFalseOrderByNgayTaoAsc(String variantId);
}
