package com.example.be.core.admin.hoadon.model.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminHoaDonResponse {
    private String id;
    private String maHoaDon;
    private Long ngayTao;
    private String tenKhachHang;
    private String soDienThoai;
    private String maNhanVien;
    private String tenNhanVien;
    private String loaiDon;
    private BigDecimal phiVanChuyen;
    private BigDecimal phiHoanHang;
    private BigDecimal tongTien;
    private BigDecimal tongTienSauGiam;
    private Integer trangThai;
    private String ghiChu;
    private String diaChiNguoiNhan;
    private List<String> bienThes;
    private List<OrderDetailResponse> details;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderDetailResponse {
        private String tenSanPham;
        private String mauSac;
        private String kichThuoc;
        private Integer soLuong;
        private BigDecimal donGia;
    }
}
