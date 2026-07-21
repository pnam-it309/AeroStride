package com.example.be.core.customer.sanpham.repository;

import com.example.be.entity.AnhChiTietSanPham;
import com.example.be.repository.AnhChiTietSanPhamRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerSanPhamAnhChiTietRepository extends AnhChiTietSanPhamRepository {

    List<AnhChiTietSanPham> findByChiTietSanPhamIdAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(String variantId);

    List<AnhChiTietSanPham> findAllByChiTietSanPhamIdInAndXoaMemFalseOrderByHinhAnhDaiDienDescNgayTaoAsc(List<String> variantIds);
}

