package com.example.be.core.admin.khachhang.repository;

import com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse;
import com.example.be.entity.KhachHang;
import com.example.be.infrastructure.constants.TrangThai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminKhachHangRepository extends JpaRepository<KhachHang, String> {

    boolean existsByEmail(String email);
    boolean existsByTenTaiKhoan(String tenTaiKhoan);
    boolean existsByMa(String ma);
    boolean existsByEmailAndIdNot(String email, String id);
    boolean existsByTenTaiKhoanAndIdNot(String tenTaiKhoan, String id);
    boolean existsByMaAndIdNot(String ma, String id);

    @Query("SELECT k.ma FROM KhachHang k")
    List<String> findAllMa();

    // ── Lấy tất cả (không phân trang) ────────────────────────────────────
    @Query("""
        SELECT new com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse(
            kh.id, kh.ma, kh.ten, kh.email, kh.tenTaiKhoan,
            kh.gioiTinh, kh.sdt, kh.ngaySinh, kh.hinhAnh, kh.ghiChu,
            kh.trangThai, kh.ngayTao, kh.ngayCapNhat,
            CONCAT(
                COALESCE(dc.diaChiChiTiet, ''), ', ',
                COALESCE(dc.phuongXa, ''), ', ',
                COALESCE(dc.thanhPho, ''), ', ',
                COALESCE(dc.tinh, '')
            ),
            SUM(CASE WHEN hd.trangThai = 6 THEN hd.tongTienSauGiam ELSE 0 END),
            MAX(hd.ngayTao)
        )
        FROM KhachHang kh
        LEFT JOIN kh.diaChi dc
        LEFT JOIN HoaDon hd ON hd.khachHang = kh
        GROUP BY kh.id, kh.ma, kh.ten, kh.email, kh.tenTaiKhoan,
                 kh.gioiTinh, kh.sdt, kh.ngaySinh, kh.hinhAnh, kh.ghiChu,
                 kh.trangThai, kh.ngayTao, kh.ngayCapNhat,
                 dc.diaChiChiTiet, dc.phuongXa, dc.thanhPho, dc.tinh
        ORDER BY kh.ngayTao DESC
    """)
    List<AdminKhachHangResponse> hienThi();

    // ── Chi tiết theo ID ──────────────────────────────────────────────────
    @Query("""
        SELECT new com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse(
            kh.id, kh.ma, kh.ten, kh.email, kh.tenTaiKhoan,
            kh.gioiTinh, kh.sdt, kh.ngaySinh, kh.hinhAnh, kh.ghiChu,
            kh.trangThai, kh.ngayTao, kh.ngayCapNhat,
            CONCAT(
                COALESCE(dc.diaChiChiTiet, ''), ', ',
                COALESCE(dc.phuongXa, ''), ', ',
                COALESCE(dc.thanhPho, ''), ', ',
                COALESCE(dc.tinh, '')
            ),
            SUM(CASE WHEN hd.trangThai = 6 THEN hd.tongTienSauGiam ELSE 0 END),
            MAX(hd.ngayTao)
        )
        FROM KhachHang kh
        LEFT JOIN kh.diaChi dc
        LEFT JOIN HoaDon hd ON hd.khachHang = kh
        WHERE kh.id = :id
        GROUP BY kh.id, kh.ma, kh.ten, kh.email, kh.tenTaiKhoan,
                 kh.gioiTinh, kh.sdt, kh.ngaySinh, kh.hinhAnh, kh.ghiChu,
                 kh.trangThai, kh.ngayTao, kh.ngayCapNhat,
                 dc.diaChiChiTiet, dc.phuongXa, dc.thanhPho, dc.tinh
    """)
    AdminKhachHangResponse detail(@Param("id") String id);

    /**
     * Tìm kiếm + lọc + phân trang — 1 query duy nhất.
     */
    @Query(value = """
        SELECT new com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse(
            kh.id, kh.ma, kh.ten, kh.email, kh.tenTaiKhoan,
            kh.gioiTinh, kh.sdt, kh.ngaySinh, kh.hinhAnh, kh.ghiChu,
            kh.trangThai, kh.ngayTao, kh.ngayCapNhat,
            CONCAT(
                COALESCE(dc.diaChiChiTiet, ''), ', ',
                COALESCE(dc.phuongXa, ''), ', ',
                COALESCE(dc.thanhPho, ''), ', ',
                COALESCE(dc.tinh, '')
            ),
            SUM(CASE WHEN hd.trangThai = 6 THEN hd.tongTienSauGiam ELSE 0 END),
            MAX(hd.ngayTao)
        )
        FROM KhachHang kh
        LEFT JOIN kh.diaChi dc
        LEFT JOIN HoaDon hd ON hd.khachHang = kh
        WHERE (:keyword IS NULL OR
               LOWER(kh.ten) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(kh.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               kh.sdt LIKE CONCAT('%', :keyword, '%') OR
               LOWER(kh.ma) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:trangThai IS NULL OR kh.trangThai = :trangThai)
          AND (:gioiTinh IS NULL OR kh.gioiTinh = :gioiTinh)
        GROUP BY kh.id, kh.ma, kh.ten, kh.email, kh.tenTaiKhoan,
                 kh.gioiTinh, kh.sdt, kh.ngaySinh, kh.hinhAnh, kh.ghiChu,
                 kh.trangThai, kh.ngayTao, kh.ngayCapNhat,
                 dc.diaChiChiTiet, dc.phuongXa, dc.thanhPho, dc.tinh
        ORDER BY kh.ngayTao DESC
    """)
    Page<AdminKhachHangResponse> filterAll(
            @Param("keyword") String keyword,
            @Param("trangThai") TrangThai trangThai,
            @Param("gioiTinh") Boolean gioiTinh,
            Pageable pageable
    );
}
