package com.example.be.core.admin.hoadon.repository;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonResponse;
import com.example.be.repository.HoaDonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminHoaDonRepository extends HoaDonRepository {

    @Query(value = """
            SELECT new com.example.be.core.admin.hoadon.model.response.AdminHoaDonResponse(
                hd.id,
                hd.maHoaDon,
                hd.ngayTao,
                kh.ten,
                hd.soDienThoaiNguoiNhan,
                nv.ten,
                hd.loaiDon,
                hd.tongTien,
                CAST(hd.trangThai AS int)
            )
            FROM HoaDon hd
            LEFT JOIN hd.khachHang kh
            LEFT JOIN hd.nhanVien nv
            WHERE (:#{#req.search} IS NULL OR :#{#req.search} = ''
                OR hd.maHoaDon LIKE %:#{#req.search}%
                OR kh.ten LIKE %:#{#req.search}%
                OR hd.soDienThoaiNguoiNhan LIKE %:#{#req.search}%)
            AND (:#{#req.tenKhachHang} IS NULL OR :#{#req.tenKhachHang} = ''
                OR kh.ten LIKE %:#{#req.tenKhachHang}%)
            AND (:#{#req.trangThai} IS NULL OR CAST(hd.trangThai AS int) = :#{#req.trangThai})
            AND (:#{#req.loaiDon} IS NULL OR :#{#req.loaiDon} = ''
                OR hd.loaiDon = :#{#req.loaiDon})
            AND (:#{#req.ngayTao} IS NULL OR :#{#req.ngayTao} = ''
                OR CAST(hd.ngayTao AS date) = CAST(:#{#req.ngayTao} AS date))
            ORDER BY hd.ngayTao DESC
            """)
    Page<AdminHoaDonResponse> getAllHoaDon(Pageable pageable, @Param("req") AdminHoaDonRequest req);
}
