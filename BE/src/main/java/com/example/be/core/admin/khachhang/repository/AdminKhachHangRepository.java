package com.example.be.core.admin.khachhang.repository;

import com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse;
import com.example.be.entity.KhachHang;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.repository.KhachHangRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminKhachHangRepository extends JpaRepository<KhachHang,String> {

    boolean existsByEmail(String email);
    boolean existsByTenTaiKhoan(String tenTaiKhoan);
    boolean existsByMa(String ma);
    boolean existsByEmailAndIdNot(String email, String id);
    boolean existsByTenTaiKhoanAndIdNot(String tenTaiKhoan, String id);
    boolean existsByMaAndIdNot(String ma, String id);

    @Query("""
    SELECT new com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse(
        kh.id, kh.ma, kh.ten, kh.email, kh.tenTaiKhoan,
        kh.gioiTinh, kh.sdt, kh.ngaySinh, kh.hinhAnh, kh.ghiChu,
        kh.trangThai, kh.ngayTao, kh.ngayCapNhat,
        dc.tinh, dc.thanhPho, dc.phuongXa,
        dc.diaChiChiTiet, dc.tenNguoiNhan, dc.sdtNguoiNhan)
    FROM KhachHang kh
    LEFT JOIN kh.diaChi dc
    """)
    List<AdminKhachHangResponse> hienThi();

    //detail
    @Query("""
    SELECT new com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse(
        kh.id, kh.ma, kh.ten, kh.email, kh.tenTaiKhoan,
        kh.gioiTinh, kh.sdt, kh.ngaySinh, kh.hinhAnh, kh.ghiChu,
        kh.trangThai, kh.ngayTao, kh.ngayCapNhat,
        dc.tinh, dc.thanhPho, dc.phuongXa,
        dc.diaChiChiTiet, dc.tenNguoiNhan, dc.sdtNguoiNhan)
    FROM KhachHang kh
    LEFT JOIN kh.diaChi dc
        WHERE kh.id = ?1
    """)
    AdminKhachHangResponse detail(String id);

    //phân trang
    @Query("""
    SELECT new com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse(
        kh.id, kh.ma, kh.ten, kh.email, kh.tenTaiKhoan,
        kh.gioiTinh, kh.sdt, kh.ngaySinh, kh.hinhAnh, kh.ghiChu,
        kh.trangThai, kh.ngayTao, kh.ngayCapNhat,
        dc.tinh, dc.thanhPho, dc.phuongXa,
        dc.diaChiChiTiet, dc.tenNguoiNhan, dc.sdtNguoiNhan)
    FROM KhachHang kh
    LEFT JOIN kh.diaChi dc
    """)
    Page<AdminKhachHangResponse> phanTrang(Pageable pageable);

    //tìm kiếm
    @Query(value = """
        SELECT new com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse(
            kh.id, kh.ma, kh.ten, kh.email, kh.tenTaiKhoan,
            kh.gioiTinh, kh.sdt, kh.ngaySinh, kh.hinhAnh, kh.ghiChu,
            kh.trangThai, kh.ngayTao, kh.ngayCapNhat,
            dc.tinh, dc.thanhPho, dc.phuongXa,
            dc.diaChiChiTiet, dc.tenNguoiNhan, dc.sdtNguoiNhan)
        FROM KhachHang kh
        LEFT JOIN kh.diaChi dc
        WHERE LOWER(kh.ten)   LIKE LOWER(CONCAT('%',:keyword,'%'))
           OR LOWER(kh.email) LIKE LOWER(CONCAT('%',:keyword,'%'))
           OR kh.sdt          LIKE CONCAT('%',:keyword,'%')
           OR LOWER(kh.ma)    LIKE LOWER(CONCAT('%',:keyword,'%'))
        ORDER BY kh.ngayTao DESC
        """,
        countQuery = """
        SELECT COUNT(kh) FROM KhachHang kh
        WHERE LOWER(kh.ten)   LIKE LOWER(CONCAT('%',:keyword,'%'))
           OR LOWER(kh.email) LIKE LOWER(CONCAT('%',:keyword,'%'))
           OR kh.sdt          LIKE CONCAT('%',:keyword,'%')
           OR LOWER(kh.ma)    LIKE LOWER(CONCAT('%',:keyword,'%'))
        """)
    Page<AdminKhachHangResponse> timKiem(
        @Param("keyword") String keyword,
        Pageable pageable);

    //Lọc theo trạng thái
    @Query("""
SELECT new com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse(
    kh.id, kh.ma, kh.ten, kh.email, kh.tenTaiKhoan,
    kh.gioiTinh, kh.sdt, kh.ngaySinh, kh.hinhAnh, kh.ghiChu,
    kh.trangThai, kh.ngayTao, kh.ngayCapNhat,
    dc.tinh, dc.thanhPho, dc.phuongXa,
    dc.diaChiChiTiet, dc.tenNguoiNhan, dc.sdtNguoiNhan)
FROM KhachHang kh
LEFT JOIN kh.diaChi dc
WHERE (:keyword IS NULL OR
       LOWER(kh.ten) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
       LOWER(kh.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
       kh.sdt LIKE CONCAT('%', :keyword, '%') OR
       LOWER(kh.ma) LIKE LOWER(CONCAT('%', :keyword, '%')))
AND (:trangThai IS NULL OR kh.trangThai = :trangThai)
AND (:gioiTinh IS NULL OR kh.gioiTinh = :gioiTinh)
ORDER BY kh.ngayTao DESC
""")
    Page<AdminKhachHangResponse> filterAll(
        @Param("keyword") String keyword,
        @Param("trangThai") TrangThai trangThai,
        @Param("gioiTinh") Boolean gioiTinh,
        Pageable pageable
    );


}
