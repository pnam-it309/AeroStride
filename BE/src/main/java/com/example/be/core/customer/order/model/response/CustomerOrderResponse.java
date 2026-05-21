package com.example.be.core.customer.order.model.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerOrderResponse {

    private String id;
    private String maHoaDon;
    private String trangThai;
    private String trangThaiDisplay;
    private BigDecimal tongTien;
    private BigDecimal phiVanChuyen;
    private BigDecimal tongTienSauGiam;
    private BigDecimal tienGiam;
    private String diaChiNguoiNhan;
    private String soDienThoaiNguoiNhan;
    private String tenNguoiNhan;
    private String ghiChu;
    private String loaiDon;
    private String phuongThucThanhToan;
    private Long ngayTao;
    private Long ngayCapNhat;
    private Long ngayDuKienNhan;

    // Voucher info
    private String tenVoucher;
    private String maVoucher;

    private List<OrderItemResponse> items;
    private List<OrderStatusHistory> lichSuTrangThai;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemResponse {
        private String id;
        private String tenSanPham;
        private String hinhAnh;
        private String tenMauSac;
        private String tenKichThuoc;
        private BigDecimal donGia;
        private Integer soLuong;
        private BigDecimal thanhTien;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderStatusHistory {
        private String trangThai;
        private String trangThaiDisplay;
        private Long thoiGian;
        private String ghiChu;
    }

    // Helper to map OrderStatus enum to display string
    public static String mapTrangThaiDisplay(String trangThai) {
        return switch (trangThai) {
            case "CHO_XAC_NHAN" -> "Chờ xác nhận";
            case "XAC_NHAN" -> "Đã xác nhận";
            case "CHO_GIAO" -> "Chờ giao hàng";
            case "DANG_GIAO" -> "Đang giao hàng";
            case "HOAN_THANH" -> "Hoàn thành";
            case "DA_HUY" -> "Đã hủy";
            case "HOAN_DON" -> "Hoàn đơn";
            default -> trangThai;
        };
    }
}
