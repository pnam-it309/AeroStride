package com.example.be.core.admin.thongke.repository;

import com.example.be.entity.HoaDon;
import com.example.be.repository.HoaDonRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminThongKeRepository extends HoaDonRepository, 
        JpaSpecificationExecutor<HoaDon>, AdminThongKeRepositoryCustom {

    // Tổng doanh thu (chỉ đơn DELIVERED - ordinal 3)
    @Query("SELECT COALESCE(SUM(hd.tongTien), 0) FROM HoaDon hd WHERE CAST(hd.trangThai AS int) = 3" +
           " AND (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)" +
           " AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)")
    java.math.BigDecimal sumDoanhThu(@Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);

    // Đếm theo trạng thái
    @Query("SELECT COUNT(hd) FROM HoaDon hd WHERE CAST(hd.trangThai AS int) = :trangThai" +
           " AND (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)" +
           " AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)")
    Long countByTrangThai(@Param("trangThai") int trangThai, @Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);

    // Tổng số đơn
    @Query("SELECT COUNT(hd) FROM HoaDon hd" +
           " WHERE (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)" +
           " AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)")
    Long countTongDon(@Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);

    // Đơn hàng gần đây
    @Query("""
            SELECT hd.id, hd.maHoaDon, kh.ten, hd.ngayTao, hd.tongTien,
                   CAST(hd.trangThai AS int), hd.loaiDon
            FROM HoaDon hd
            LEFT JOIN hd.khachHang kh
            ORDER BY hd.ngayTao DESC
            """)
    List<Object[]> getDonHangGanDay(PageRequest pageRequest);

    // Top sản phẩm bán chạy
    @Query("""
           SELECT sp.ten,
                  COALESCE(SUM(hdct.soLuong * hdct.donGia), 0),
                  COALESCE(SUM(hdct.soLuong), 0)
           FROM HoaDonChiTiet hdct
           JOIN hdct.chiTietSanPham ctsp
           JOIN ctsp.sanPham sp
           JOIN hdct.hoaDon hd
           WHERE CAST(hd.trangThai AS int) = 4 -- HOAN_THANH
           AND (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)
           AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)
           GROUP BY sp.id, sp.ten
           ORDER BY SUM(hdct.soLuong) DESC
           """)
    List<Object[]> getTopProductsData(
            @Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay, org.springframework.data.domain.Pageable pageable);

    // Doanh thu theo danh mục
    @Query("""
           SELECT dm.ten,
                  COALESCE(SUM(hdct.soLuong * hdct.donGia), 0)
           FROM HoaDonChiTiet hdct
           JOIN hdct.chiTietSanPham ctsp
           JOIN ctsp.sanPham sp
           JOIN sp.danhMuc dm
           JOIN hdct.hoaDon hd
           WHERE CAST(hd.trangThai AS int) = 4 -- HOAN_THANH
           AND (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)
           AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)
           GROUP BY dm.id, dm.ten
           ORDER BY SUM(hdct.soLuong * hdct.donGia) DESC
           """)
    List<Object[]> getSalesByCategoryData(
            @Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);
}
