package com.example.be.core.admin.hoadon.repository;

import com.example.be.repository.HoaDonRepository;
import com.example.be.entity.HoaDon;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminHoaDonRepository extends HoaDonRepository, JpaSpecificationExecutor<HoaDon>, AdminHoaDonRepositoryCustom {

    /**
     * Lấy chi tiết hóa đơn đầy đủ thông tin để hiển thị trên UI Admin
     */
    @EntityGraph(attributePaths = {
        "khachHang",
        "nhanVien",
        "nhanVien.phanQuyen",
        "listsHoaDonChiTiet",
        "listsHoaDonChiTiet.chiTietSanPham",
        "listsHoaDonChiTiet.chiTietSanPham.sanPham",
        "listsHoaDonChiTiet.chiTietSanPham.mauSac",
        "listsHoaDonChiTiet.chiTietSanPham.kichThuoc",
        "listsHoaDonChiTiet.chiTietSanPham.anhChiTietSanPhams"
    })
    Optional<HoaDon> findDetailById(@Param("id") String id);

    @Query("SELECT h FROM HoaDon h WHERE h.loaiDon = :loaiDon AND h.ngayTao >= :startDate AND h.ngayTao <= :endDate ORDER BY h.ngayTao DESC")
    List<HoaDon> findByLoaiDonAndNgayTaoBetween(String loaiDon, Long startDate, Long endDate);

    @Query("SELECT SUM(h.tongTien) FROM HoaDon h WHERE h.giaoCa.id = :giaoCaId AND (h.trangThai = 4 OR h.trangThai = 5)") // 4: Đã giao, 5: Hoàn thành
    BigDecimal calculateDoanhThuByGiaoCaId(String giaoCaId);

    /**
     * Lấy thông tin hóa đơn tối ưu cho việc in ấn (Invoice Print)
     * Đã bao gồm JOIN FETCH tất cả các quan hệ cần thiết để tránh N+1 trong template
     */
    @EntityGraph(attributePaths = {
        "khachHang",
        "nhanVien",
        "listsHoaDonChiTiet",
        "listsHoaDonChiTiet.chiTietSanPham",
        "listsHoaDonChiTiet.chiTietSanPham.sanPham",
        "listsHoaDonChiTiet.chiTietSanPham.mauSac",
        "listsHoaDonChiTiet.chiTietSanPham.kichThuoc",
        "listsGiaoDichThanhToan",
        "listsGiaoDichThanhToan.phuongThucThanhToan"
    })
    Optional<HoaDon> findForPrintById(@Param("id") String id);
}
