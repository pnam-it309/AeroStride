package com.example.be.core.admin.nhanvien.repository;

import com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.repository.NhanVienRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminNhanVienRepository extends JpaRepository<NhanVien, String> {
    boolean existsByEmail(String email);
    boolean existsByTenTaiKhoan(String tenTaiKhoan);
    boolean existsByMa(String ma);
    boolean existsByEmailAndIdNot(String email, String id);
    boolean existsByTenTaiKhoanAndIdNot(String tenTaiKhoan, String id);
    boolean existsByMaAndIdNot(String ma, String id);

    @Query("SELECT nv.ma FROM NhanVien nv")
    List<String> findAllMa();

    @Query("""
        SELECT new com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse(
                nv.id, nv.ma, nv.ten, nv.email, nv.tenTaiKhoan,
                nv.gioiTinh, nv.sdt, nv.ngaySinh, nv.hinhAnh, nv.diaChi,
                nv.trangThai, nv.ngayTao, nv.ngayCapNhat,
                pq.ma, pq.ten, pq.quyenHan)
            FROM NhanVien nv
            LEFT JOIN nv.phanQuyen pq
            ORDER BY nv.ngayTao DESC
        """)
    List<AdminNhanVienResponse> hienThi();

    //Phân trang
    @Query(value = """
        SELECT new com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse(
            nv.id, nv.ma, nv.ten, nv.email, nv.tenTaiKhoan,
            nv.gioiTinh, nv.sdt, nv.ngaySinh, nv.hinhAnh, nv.diaChi,
            nv.trangThai, nv.ngayTao, nv.ngayCapNhat,
            pq.ma, pq.ten, pq.quyenHan)
        FROM NhanVien nv
        LEFT JOIN nv.phanQuyen pq
        ORDER BY nv.ngayTao DESC
        """,
        countQuery = """
        SELECT COUNT(nv) FROM NhanVien nv
        """)
    Page<AdminNhanVienResponse> phanTrang(Pageable pageable);

    //detail
    @Query("""
        SELECT new com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse(
            nv.id, nv.ma, nv.ten, nv.email, nv.tenTaiKhoan,
            nv.gioiTinh, nv.sdt, nv.ngaySinh, nv.hinhAnh, nv.diaChi,
            nv.trangThai, nv.ngayTao, nv.ngayCapNhat,
            pq.ma, pq.ten, pq.quyenHan)
        FROM NhanVien nv
        LEFT JOIN nv.phanQuyen pq
        WHERE nv.id = :id
        """)
    AdminNhanVienResponse detail(@Param("id") String id);

    //search , lọc
    @Query(value = """
    SELECT new com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse(
        nv.id, nv.ma, nv.ten, nv.email, nv.tenTaiKhoan,
        nv.gioiTinh, nv.sdt, nv.ngaySinh, nv.hinhAnh, nv.diaChi,
        nv.trangThai, nv.ngayTao, nv.ngayCapNhat,
        pq.ma, pq.ten, pq.quyenHan)
    FROM NhanVien nv
    LEFT JOIN nv.phanQuyen pq
    WHERE LOWER(nv.ten)   LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR LOWER(nv.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR nv.sdt          LIKE CONCAT('%', :keyword, '%')
       OR LOWER(nv.ma)    LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR LOWER(nv.tenTaiKhoan) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY nv.ngayTao DESC
""",
        countQuery = """
    SELECT COUNT(nv) FROM NhanVien nv
    WHERE LOWER(nv.ten)   LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR LOWER(nv.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR nv.sdt          LIKE CONCAT('%', :keyword, '%')
       OR LOWER(nv.ma)    LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR LOWER(nv.tenTaiKhoan) LIKE LOWER(CONCAT('%', :keyword, '%'))
""")
    Page<AdminNhanVienResponse> searchNhanVien(
        @Param("keyword") String keyword,
        Pageable pageable);

    @Query(value = """
    SELECT new com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse(
        nv.id, nv.ma, nv.ten, nv.email, nv.tenTaiKhoan,
        nv.gioiTinh, nv.sdt, nv.ngaySinh, nv.hinhAnh, nv.diaChi,
        nv.trangThai, nv.ngayTao, nv.ngayCapNhat,
        pq.ma, pq.ten, pq.quyenHan)
    FROM NhanVien nv
    LEFT JOIN nv.phanQuyen pq
    WHERE (:keyword IS NULL OR
           LOWER(nv.ten)   LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           LOWER(nv.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           nv.sdt          LIKE CONCAT('%', :keyword, '%') OR
           LOWER(nv.ma)    LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           LOWER(nv.tenTaiKhoan) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:trangThai IS NULL OR nv.trangThai = :trangThai)
      AND (:gioiTinh IS NULL OR nv.gioiTinh = :gioiTinh)
    ORDER BY nv.ngayTao DESC
""")
    Page<AdminNhanVienResponse> filterAll(
        @Param("keyword") String keyword,
        @Param("trangThai") TrangThai trangThai,
        @Param("gioiTinh") Boolean gioiTinh,
        Pageable pageable);
}
