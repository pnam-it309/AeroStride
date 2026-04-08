package com.example.be.core.admin.thongke.model.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminThongKeResponse {

    // Tổng quan
    private BigDecimal tongDoanhThu;
    private Long tongDonHang;
    private Long donHangHoanThanh;
    private Long donHangChoXacNhan;
    private Long donHangDangGiao;
    private Long donHangDaHuy;
    private Long tongKhachHang;
    private Long sanPhamSapHet;

    // Doanh thu theo ngày (cho biểu đồ)
    private List<DoanhThuNgay> doanhThuTheoNgay;

    // Đơn hàng gần đây
    private List<DonHangGanDay> donHangGanDay;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoanhThuNgay {
        private String ngay;
        private BigDecimal doanhThu;
        private Long soDon;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DonHangGanDay {
        private String id;
        private String maHoaDon;
        private String tenKhachHang;
        private Long ngayTao;
        private BigDecimal tongTien;
        private Integer trangThai;
        private String loaiDon;
    }
}
