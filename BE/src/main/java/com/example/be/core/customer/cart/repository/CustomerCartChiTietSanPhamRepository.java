package com.example.be.core.customer.cart.repository;

import com.example.be.entity.ChiTietSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerCartChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, String> {
}
