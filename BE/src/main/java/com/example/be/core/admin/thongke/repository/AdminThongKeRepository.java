package com.example.be.core.admin.thongke.repository;

import com.example.be.entity.HoaDon;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminThongKeRepository extends JpaRepository<HoaDon, String> {

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

    // Doanh thu nhóm theo ngày (epoch millis -> dùng FUNCTION)
    @Query(value = """
            SELECT
                DATE_FORMAT(FROM_UNIXTIME(hd.ngay_tao / 1000), '%Y-%m-%d') as ngay,
                COALESCE(SUM(hd.tong_tien), 0) as doanhThu,
                COUNT(hd.id) as soDon
            FROM hoa_don hd
            WHERE hd.trang_thai = 3
            AND (:tuNgay IS NULL OR hd.ngay_tao >= :tuNgay)
            AND (:denNgay IS NULL OR hd.ngay_tao <= :denNgay)
            GROUP BY DATE_FORMAT(FROM_UNIXTIME(hd.ngay_tao / 1000), '%Y-%m-%d')
            ORDER BY ngay ASC
            """, nativeQuery = true)
    List<Object[]> getDoanhThuTheoNgay(@Param("tuNgay") Long tuNgay, @Param("denNgay") Long denNgay);

    // Đơn hàng gần đây
    @Query("""
            SELECT hd.id, hd.maHoaDon, kh.ten, hd.ngayTao, hd.tongTien,
                   CAST(hd.trangThai AS int), hd.loaiDon
            FROM HoaDon hd
            LEFT JOIN hd.khachHang kh
            ORDER BY hd.ngayTao DESC
            """)
    List<Object[]> getDonHangGanDay(PageRequest pageRequest);
}
