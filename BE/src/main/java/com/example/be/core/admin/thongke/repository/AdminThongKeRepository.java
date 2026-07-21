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

    @Query("""
            SELECT 
                 COALESCE(SUM(CASE WHEN CAST(hd.trangThai AS int) = 4 THEN hd.tongTien ELSE 0 END), 0),
                 COUNT(hd),
                 SUM(CASE WHEN CAST(hd.trangThai AS int) = 4 THEN 1 ELSE 0 END),
                 SUM(CASE WHEN CAST(hd.trangThai AS int) = 0 THEN 1 ELSE 0 END),
                 SUM(CASE WHEN CAST(hd.trangThai AS int) = 3 THEN 1 ELSE 0 END),
                 SUM(CASE WHEN CAST(hd.trangThai AS int) = 5 THEN 1 ELSE 0 END),
                 SUM(CASE WHEN CAST(hd.trangThai AS int) = 6 THEN 1 ELSE 0 END),
                 COALESCE(SUM(CASE WHEN CAST(hd.trangThai AS int) = 0 THEN hd.tongTien ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN CAST(hd.trangThai AS int) = 3 THEN hd.tongTien ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN CAST(hd.trangThai AS int) = 5 THEN hd.tongTien ELSE 0 END), 0)
            FROM HoaDon hd
            WHERE (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)
            AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)
            """)
    List<Object[]> getOverviewStats(@Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);

    @Query("""
           SELECT
                COALESCE(SUM(CASE WHEN hd.loaiDon IN ('TAI_QUAY', 'OFFLINE') AND CAST(hd.trangThai AS int) = 4 THEN hd.tongTien ELSE 0 END), 0),
                SUM(CASE WHEN hd.loaiDon IN ('TAI_QUAY', 'OFFLINE') AND CAST(hd.trangThai AS int) = 4 THEN 1 ELSE 0 END),
                COALESCE(SUM(CASE WHEN hd.loaiDon = 'ONLINE' AND CAST(hd.trangThai AS int) = 4 THEN hd.tongTien ELSE 0 END), 0),
                SUM(CASE WHEN hd.loaiDon = 'ONLINE' AND CAST(hd.trangThai AS int) = 4 THEN 1 ELSE 0 END)
           FROM HoaDon hd
           WHERE (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)
           AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)
           """)
    List<Object[]> getOrderTypeStats(@Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);

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
           SELECT sp.ma,
                  sp.ten,
                  th.ten,
                  COALESCE(SUM(hdct.soLuong * hdct.donGia), 0),
                  COALESCE(SUM(hdct.soLuong), 0)
           FROM HoaDonChiTiet hdct
           JOIN hdct.chiTietSanPham ctsp
           JOIN ctsp.sanPham sp
           LEFT JOIN sp.thuongHieu th
           JOIN hdct.hoaDon hd
           WHERE CAST(hd.trangThai AS int) = 4
           AND (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)
           AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)
           GROUP BY sp.id, sp.ma, sp.ten, th.ten
           ORDER BY SUM(hdct.soLuong) DESC
           """)
    List<Object[]> getTopProductsData(
            @Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay, org.springframework.data.domain.Pageable pageable);

    @Query("""
           SELECT COALESCE(md.ten, 'Khác'),
                  COALESCE(SUM(hdct.soLuong * hdct.donGia), 0)
           FROM HoaDonChiTiet hdct
           JOIN hdct.chiTietSanPham ctsp
           JOIN ctsp.sanPham sp
           LEFT JOIN sp.mucDichChay md
           JOIN hdct.hoaDon hd
           WHERE CAST(hd.trangThai AS int) = 4
           AND (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)
           AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)
           GROUP BY md.ten
           HAVING COALESCE(SUM(hdct.soLuong * hdct.donGia), 0) > 0
           ORDER BY SUM(hdct.soLuong * hdct.donGia) DESC
           """)
    List<Object[]> getCategoryRevenueData(
            @Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);

    @Query(value = """
            SELECT 
                COALESCE(kh.ten_nguoi_dung, hd.ten_nguoi_nhan, 'Khách lẻ') AS tenKhachHang,
                COALESCE(SUM(CASE WHEN hd.trang_thai = 4 THEN hd.tong_tien ELSE 0 END), 0) AS tongChi,
                COALESCE(SUM(CASE WHEN hd.trang_thai = 4 THEN (SELECT COALESCE(SUM(hdct.so_luong), 0) FROM hoa_don_chi_tiet hdct WHERE hdct.id_hoa_don = hd.id) ELSE 0 END), 0) AS tongSanPham,
                COALESCE(SUM(CASE WHEN hd.trang_thai = 4 THEN 1 ELSE 0 END), 0) AS donThanhCong,
                COALESCE(SUM(CASE WHEN hd.trang_thai = 6 THEN 1 ELSE 0 END), 0) AS donHoan
            FROM hoa_don hd
            LEFT JOIN khach_hang kh ON hd.id_khach_hang = kh.id
            WHERE (:tuNgay IS NULL OR hd.ngay_tao >= :tuNgay)
              AND (:denNgay IS NULL OR hd.ngay_tao <= :denNgay)
            GROUP BY kh.id, kh.ten_nguoi_dung, hd.ten_nguoi_nhan
            HAVING COALESCE(SUM(CASE WHEN hd.trang_thai = 4 THEN hd.tong_tien ELSE 0 END), 0) > 0
            ORDER BY tongChi DESC
            """, nativeQuery = true)
    List<Object[]> getCustomerPurchaseStats(
            @Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);

    @Query(value = """
           SELECT sp.ma as ma,
                  sp.ten as ten,
                  th.ten as thuongHieu,
                  SUM(hdct.soLuong * hdct.donGia) as doanhThu,
                  SUM(hdct.soLuong) as soLuongBan
           FROM HoaDonChiTiet hdct
           JOIN hdct.chiTietSanPham ctsp
           JOIN ctsp.sanPham sp
           LEFT JOIN sp.thuongHieu th
           JOIN hdct.hoaDon hd
           WHERE CAST(hd.trangThai AS int) = 4
             AND (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)
             AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)
             AND (:keyword IS NULL OR LOWER(sp.ma) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(sp.ten) LIKE LOWER(CONCAT('%', :keyword, '%')))
           GROUP BY sp.id, sp.ma, sp.ten, th.ten
           """,
           countQuery = """
           SELECT COUNT(DISTINCT sp.id)
           FROM HoaDonChiTiet hdct
           JOIN hdct.chiTietSanPham ctsp
           JOIN ctsp.sanPham sp
           JOIN hdct.hoaDon hd
           WHERE CAST(hd.trangThai AS int) = 4
             AND (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)
             AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)
             AND (:keyword IS NULL OR LOWER(sp.ma) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(sp.ten) LIKE LOWER(CONCAT('%', :keyword, '%')))
           """)
    org.springframework.data.domain.Page<Object[]> getProductStatistics(
            @Param("tuNgay") Long tuNgay,
            @Param("denNgay") Long denNgay,
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);

}
