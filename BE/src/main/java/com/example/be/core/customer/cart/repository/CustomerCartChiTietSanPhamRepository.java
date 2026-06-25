package com.example.be.core.customer.cart.repository;

import com.example.be.entity.ChiTietSanPham;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerCartChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, String> {

    @EntityGraph(attributePaths = {"sanPham", "mauSac", "kichThuoc"})
    List<ChiTietSanPham> findAllById(Iterable<String> ids);
}
