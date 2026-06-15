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
                COALESCE(SUM(CASE WHEN CAST(hd.trangThai AS int) = 3 THEN hd.tongTien ELSE 0 END), 0),
                COUNT(hd),
                SUM(CASE WHEN CAST(hd.trangThai AS int) = 4 THEN 1 ELSE 0 END),
                SUM(CASE WHEN CAST(hd.trangThai AS int) = 0 THEN 1 ELSE 0 END),
                SUM(CASE WHEN CAST(hd.trangThai AS int) = 3 THEN 1 ELSE 0 END),
                SUM(CASE WHEN CAST(hd.trangThai AS int) = 5 THEN 1 ELSE 0 END)
           FROM HoaDon hd
           WHERE (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)
           AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)
           """)
    List<Object[]> getOverviewStats(@Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);

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
           WHERE CAST(hd.trangThai AS int) = 4
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
           WHERE CAST(hd.trangThai AS int) = 4
           AND (:tuNgay IS NULL OR hd.ngayTao >= :tuNgay)
           AND (:denNgay IS NULL OR hd.ngayTao <= :denNgay)
           GROUP BY dm.id, dm.ten
           ORDER BY SUM(hdct.soLuong * hdct.donGia) DESC
           """)
    List<Object[]> getSalesByCategoryData(
            @Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);
}
