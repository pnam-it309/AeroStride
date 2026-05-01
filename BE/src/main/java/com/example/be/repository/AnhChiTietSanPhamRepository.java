package com.example.be.repository;

import com.example.be.entity.AnhChiTietSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnhChiTietSanPhamRepository extends JpaRepository<AnhChiTietSanPham, String> {
}
