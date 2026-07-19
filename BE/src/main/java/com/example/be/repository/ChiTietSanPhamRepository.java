package com.example.be.repository;

import com.example.be.entity.ChiTietSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, String> {

    @Modifying(flushAutomatically = true)
    @Query("UPDATE ChiTietSanPham c SET c.soLuong = c.soLuong - :qty WHERE c.id = :id AND c.soLuong >= :qty")
    int deductStock(@Param("id") String id, @Param("qty") int qty);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE ChiTietSanPham c SET c.soLuong = c.soLuong + :qty WHERE c.id = :id")
    int restoreStock(@Param("id") String id, @Param("qty") int qty);

    long countBySoLuongLessThan(int quantity);
}
