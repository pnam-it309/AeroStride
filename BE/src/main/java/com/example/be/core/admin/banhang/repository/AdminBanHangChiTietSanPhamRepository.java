package com.example.be.core.admin.banhang.repository;

import com.example.be.entity.ChiTietSanPham;
import com.example.be.repository.ChiTietSanPhamRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface AdminBanHangChiTietSanPhamRepository extends ChiTietSanPhamRepository {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChiTietSanPham c WHERE c.id = :id")
    Optional<ChiTietSanPham> findByIdWithPessimisticLock(@Param("id") String id);

    // Trừ tồn kho trực tiếp bằng SQL UPDATE (atomic, tránh JPA cache issue)
    // Trả về số dòng bị ảnh hưởng: 1 = thành công, 0 = không đủ hàng
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ChiTietSanPham c SET c.soLuong = c.soLuong - :qty WHERE c.id = :id AND c.soLuong >= :qty")
    int deductStock(@Param("id") String id, @Param("qty") int qty);

    // Cộng trả lại tồn kho trực tiếp bằng SQL UPDATE (atomic)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ChiTietSanPham c SET c.soLuong = c.soLuong + :qty WHERE c.id = :id")
    int restoreStock(@Param("id") String id, @Param("qty") int qty);

    @Query("""
        SELECT ct FROM ChiTietSanPham ct
        WHERE LOWER(ct.sanPham.ten) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(ct.maChiTietSanPham) LIKE LOWER(CONCAT('%', :keyword, '%'))
        """)
    List<ChiTietSanPham> searchByKeyword(@Param("keyword") String keyword);

    @Query("""
        SELECT ct FROM ChiTietSanPham ct
        LEFT JOIN FETCH ct.sanPham sp
        LEFT JOIN FETCH sp.thuongHieu
        LEFT JOIN FETCH sp.chatLieu
        LEFT JOIN FETCH sp.deGiay
        LEFT JOIN FETCH ct.mauSac
        LEFT JOIN FETCH ct.kichThuoc
        WHERE (ct.xoaMem IS NULL OR ct.xoaMem = false)
        AND (:thuongHieu IS NULL OR sp.thuongHieu.ten = :thuongHieu)
        AND (:chatLieu IS NULL OR sp.chatLieu.ten = :chatLieu)
        AND (:xuatXu IS NULL OR sp.xuatXu.ten = :xuatXu)
        AND (:mucDich IS NULL OR sp.mucDichChay.ten = :mucDich)
        AND (:mauSac IS NULL OR ct.mauSac.ten = :mauSac)
        AND (:kichCo IS NULL OR ct.kichThuoc.ten = :kichCo)
        AND (:minGia IS NULL OR ct.giaBan >= :minGia)
        AND (:maxGia IS NULL OR ct.giaBan <= :maxGia)
        AND (
            LOWER(ct.maChiTietSanPham) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(sp.ten) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(sp.ma) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        ORDER BY ct.ngayTao DESC
        """)
    List<ChiTietSanPham> searchForPOS(
        @Param("keyword") String keyword, 
        @Param("thuongHieu") String thuongHieu,
        @Param("chatLieu") String chatLieu,
        @Param("xuatXu") String xuatXu,
        @Param("mucDich") String mucDich,
        @Param("mauSac") String mauSac,
        @Param("kichCo") String kichCo,
        @Param("minGia") java.math.BigDecimal minGia,
        @Param("maxGia") java.math.BigDecimal maxGia);
}
