package com.example.be.core.customer.order.repository;

import com.example.be.repository.ChiTietSanPhamRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerOrderChiTietSanPhamRepository extends ChiTietSanPhamRepository {

    // Trừ tồn kho atomic: chỉ trừ khi còn đủ hàng. Trả về 1 = thành công, 0 = không đủ hàng.
    @Modifying(flushAutomatically = true)
    @Query("UPDATE ChiTietSanPham c SET c.soLuong = c.soLuong - :qty WHERE c.id = :id AND c.soLuong >= :qty")
    int deductStock(@Param("id") String id, @Param("qty") int qty);

    // Hoàn trả tồn kho atomic.
    @Modifying(flushAutomatically = true)
    @Query("UPDATE ChiTietSanPham c SET c.soLuong = c.soLuong + :qty WHERE c.id = :id")
    int restoreStock(@Param("id") String id, @Param("qty") int qty);
}
