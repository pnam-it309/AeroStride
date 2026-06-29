package com.example.be.core.admin.hoadon.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;
import com.example.be.infrastructure.constants.OrderStatus;

@Getter
@Setter
@Builder
public class AdminHoaDonDetailResponse {
    private String id;
    private String maHoaDon;
    private OrderStatus trangThai;
    private String loaiDon;
    private BigDecimal phiVanChuyen;
    private BigDecimal tongTien;
    private BigDecimal tongTienSauGiam;
    private BigDecimal tienNguoiMua;
    private String tenNguoiNhan;
    private String diaChiNguoiNhan;
    private String soDienThoaiNguoiNhan;
    private Long ngayDuKienNhan;
    private String ghiChu;
    private Long ngayTao;
    private Long ngayCapNhat;

    // Hoàn phí
    private Boolean daHoanPhi;     // đã xác nhận hoàn tiền cho khách chưa
    private Boolean canHoanPhi;    // đơn có cần/được xác nhận hoàn phí không (trả trước & đã hủy & chưa hoàn)

    private String maPhieuGiamGia;
    private String tenPhieuGiamGia;

    private String tenKhachHang;
    private String maKhachHang;
    private String soDienThoaiKhachHang;
    private String emailKhachHang;

    private String tenNhanVien;
    private String maNhanVien;

    private List<AdminHoaDonChiTietResponse> listsHoaDonChiTiet;
    private List<AdminLichSuHoaDonResponse> listsLichSuHoaDon;
    private List<AdminGiaoDichThanhToanResponse> listsGiaoDichThanhToan;
}
