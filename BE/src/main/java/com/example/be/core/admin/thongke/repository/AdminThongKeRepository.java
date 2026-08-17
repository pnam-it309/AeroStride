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
                 SUM(CASE WHEN CAST(hd.trangThai AS int) = 5 AND (hd.orderType = com.example.be.infrastructure.constants.OrderType.ONLINE OR (hd.orderType IS NULL AND hd.nhanVien IS NULL AND (hd.loaiDon = 'ONLINE' OR hd.loaiDon IS NULL))) THEN 1 ELSE 0 END),
                 SUM(CASE WHEN CAST(hd.trangThai AS int) = 6 THEN 1 ELSE 0 END),
                 COALESCE(SUM(CASE WHEN CAST(hd.trangThai AS int) = 0 THEN hd.tongTien ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN CAST(hd.trangThai AS int) = 3 THEN hd.tongTien ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN CAST(hd.trangThai AS int) = 5 AND (hd.orderType = com.example.be.infrastructure.constants.OrderType.ONLINE OR (hd.orderType IS NULL AND hd.nhanVien IS NULL AND (hd.loaiDon = 'ONLINE' OR hd.loaiDon IS NULL))) THEN hd.tongTien ELSE 0 END), 0)
            FROM HoaDon hd
            WHERE (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)
            AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)
            """)
    List<Object[]> getOverviewStats(@Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);

    @Query("""
           SELECT
                COALESCE(SUM(CASE WHEN CAST(hd.trangThai AS int) = 4 AND (
                    hd.orderType = com.example.be.infrastructure.constants.OrderType.IN_STORE
                    OR (hd.orderType IS NULL AND hd.loaiDon IN ('TAI_QUAY', 'OFFLINE', 'GIAO_HANG'))
                    OR (hd.orderType IS NULL AND hd.loaiDon IS NULL AND hd.nhanVien IS NOT NULL)
                ) THEN hd.tongTien ELSE 0 END), 0),
                SUM(CASE WHEN CAST(hd.trangThai AS int) = 4 AND (
                    hd.orderType = com.example.be.infrastructure.constants.OrderType.IN_STORE
                    OR (hd.orderType IS NULL AND hd.loaiDon IN ('TAI_QUAY', 'OFFLINE', 'GIAO_HANG'))
                    OR (hd.orderType IS NULL AND hd.loaiDon IS NULL AND hd.nhanVien IS NOT NULL)
                ) THEN 1 ELSE 0 END),
                COALESCE(SUM(CASE WHEN CAST(hd.trangThai AS int) = 4 AND (
                    hd.orderType = com.example.be.infrastructure.constants.OrderType.ONLINE
                    OR (hd.orderType IS NULL AND hd.loaiDon = 'ONLINE')
                    OR (hd.orderType IS NULL AND hd.loaiDon IS NULL AND hd.nhanVien IS NULL)
                ) THEN hd.tongTien ELSE 0 END), 0),
                SUM(CASE WHEN CAST(hd.trangThai AS int) = 4 AND (
                    hd.orderType = com.example.be.infrastructure.constants.OrderType.ONLINE
                    OR (hd.orderType IS NULL AND hd.loaiDon = 'ONLINE')
                    OR (hd.orderType IS NULL AND hd.loaiDon IS NULL AND hd.nhanVien IS NULL)
                ) THEN 1 ELSE 0 END)
           FROM HoaDon hd
           WHERE (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)
           AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)
           """)
    List<Object[]> getOrderTypeStats(@Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);

    // Đơn hàng gần đây
    @Query("""
            SELECT hd.id, hd.maHoaDon, kh.ten, hd.ngayTao, hd.tongTien,
                   CAST(hd.trangThai AS int), hd.orderType
            FROM HoaDon hd
            LEFT JOIN hd.khachHang kh
            ORDER BY hd.ngayTao DESC
            """)
    List<Object[]> getDonHangGanDay(PageRequest pageRequest);

    // Top sản phẩm bán chạy
    @Query(value = """
           SELECT sp.ma_san_pham AS ma,
                  sp.ten_san_pham AS ten,
                  COALESCE(th.ten_thuong_hieu, 'Khác') AS thuongHieu,
                  COALESCE(SUM(hdct.so_luong * hdct.don_gia), 0) AS doanhThu,
                  COALESCE(SUM(hdct.so_luong), 0) AS soLuongBan
           FROM hoa_don_chi_tiet hdct
           JOIN chi_tiet_san_pham ctsp ON hdct.id_chi_tiet_san_pham = ctsp.id
           JOIN san_pham sp ON ctsp.id_san_pham = sp.id
           LEFT JOIN thuong_hieu th ON sp.id_thuong_hieu = th.id
           JOIN hoa_don hd ON hdct.id_hoa_don = hd.id
           WHERE hd.trang_thai = 4
             AND (:tuNgay IS NULL OR hd.ngay_tao >= :tuNgay)
             AND (:denNgay IS NULL OR hd.ngay_tao <= :denNgay)
           GROUP BY sp.id, sp.ma_san_pham, sp.ten_san_pham, th.ten_thuong_hieu
           ORDER BY SUM(hdct.so_luong) DESC
           """, nativeQuery = true)
    List<Object[]> getTopProductsData(
            @Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay, org.springframework.data.domain.Pageable pageable);

    @Query(value = """
           SELECT COALESCE(th.ten_thuong_hieu, 'Khác') AS thuongHieu,
                  COALESCE(SUM(hdct.so_luong * hdct.don_gia), 0) AS doanhThu
           FROM hoa_don_chi_tiet hdct
           JOIN chi_tiet_san_pham ctsp ON hdct.id_chi_tiet_san_pham = ctsp.id
           JOIN san_pham sp ON ctsp.id_san_pham = sp.id
           LEFT JOIN thuong_hieu th ON sp.id_thuong_hieu = th.id
           JOIN hoa_don hd ON hdct.id_hoa_don = hd.id
           WHERE hd.trang_thai = 4
             AND (:tuNgay IS NULL OR hd.ngay_tao >= :tuNgay)
             AND (:denNgay IS NULL OR hd.ngay_tao <= :denNgay)
           GROUP BY th.id, th.ten_thuong_hieu
           HAVING COALESCE(SUM(hdct.so_luong * hdct.don_gia), 0) > 0
           ORDER BY doanhThu DESC
           """, nativeQuery = true)
    List<Object[]> getBrandRevenueData(
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
            SELECT 
                nv.ma_nhan_vien AS maNhanVien,
                nv.ten_nhan_vien AS tenNhanVien,
                COALESCE(SUM(CASE WHEN hd.trang_thai = 4 THEN hd.tong_tien ELSE 0 END), 0) AS tongDoanhThu,
                COALESCE(SUM(CASE WHEN hd.trang_thai = 4 THEN (SELECT COALESCE(SUM(hdct.so_luong), 0) FROM hoa_don_chi_tiet hdct WHERE hdct.id_hoa_don = hd.id) ELSE 0 END), 0) AS tongSanPham,
                COALESCE(COUNT(hd.id), 0) AS tongDonHang
            FROM nhan_vien nv
            LEFT JOIN hoa_don hd ON hd.id_nhan_vien = nv.id
              AND (:tuNgay IS NULL OR hd.ngay_tao >= :tuNgay)
              AND (:denNgay IS NULL OR hd.ngay_tao <= :denNgay)
            WHERE nv.xoa_mem = false OR nv.xoa_mem IS NULL
            GROUP BY nv.id, nv.ma_nhan_vien, nv.ten_nhan_vien
            ORDER BY tongDoanhThu DESC
            """, nativeQuery = true)
    List<Object[]> getEmployeeRevenueStats(
            @Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);

    @Query(value = """
            SELECT 
                COALESCE(SUM(hd.tong_tien), 0) AS doanhThu,
                COUNT(hd.id) AS soDon
            FROM hoa_don hd
            WHERE hd.trang_thai = 4
              AND hd.ngay_tao >= :tuNgay
              AND hd.ngay_tao <= :denNgay
            """, nativeQuery = true)
    List<Object[]> getRevenueCycleStats(@Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);

    @Query(value = """
           SELECT sp.ma_san_pham AS ma,
                  sp.ten_san_pham AS ten,
                  COALESCE(th.ten_thuong_hieu, 'Khác') AS thuongHieu,
                  COALESCE(SUM(hdct.so_luong * hdct.don_gia), 0) AS doanhThu,
                  COALESCE(SUM(hdct.so_luong), 0) AS soLuongBan
           FROM hoa_don_chi_tiet hdct
           JOIN chi_tiet_san_pham ctsp ON hdct.id_chi_tiet_san_pham = ctsp.id
           JOIN san_pham sp ON ctsp.id_san_pham = sp.id
           LEFT JOIN thuong_hieu th ON sp.id_thuong_hieu = th.id
           JOIN hoa_don hd ON hdct.id_hoa_don = hd.id
           WHERE hd.trang_thai = 4
             AND (:tuNgay IS NULL OR hd.ngay_tao >= :tuNgay)
             AND (:denNgay IS NULL OR hd.ngay_tao <= :denNgay)
             AND (:keyword IS NULL OR :keyword = '' OR LOWER(sp.ma_san_pham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(sp.ten_san_pham) LIKE LOWER(CONCAT('%', :keyword, '%')))
           GROUP BY sp.id, sp.ma_san_pham, sp.ten_san_pham, th.ten_thuong_hieu
           """,
           countQuery = """
           SELECT COUNT(DISTINCT sp.id)
           FROM hoa_don_chi_tiet hdct
           JOIN chi_tiet_san_pham ctsp ON hdct.id_chi_tiet_san_pham = ctsp.id
           JOIN san_pham sp ON ctsp.id_san_pham = sp.id
           JOIN hoa_don hd ON hdct.id_hoa_don = hd.id
           WHERE hd.trang_thai = 4
             AND (:tuNgay IS NULL OR hd.ngay_tao >= :tuNgay)
             AND (:denNgay IS NULL OR hd.ngay_tao <= :denNgay)
             AND (:keyword IS NULL OR :keyword = '' OR LOWER(sp.ma_san_pham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(sp.ten_san_pham) LIKE LOWER(CONCAT('%', :keyword, '%')))
           """,
           nativeQuery = true)
    org.springframework.data.domain.Page<Object[]> getProductStatistics(
            @Param("tuNgay") Long tuNgay,
            @Param("denNgay") Long denNgay,
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);

}
