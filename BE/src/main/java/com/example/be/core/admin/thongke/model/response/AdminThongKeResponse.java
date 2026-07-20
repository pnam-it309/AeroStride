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
    private Long donHangHoan;
    private Long tongKhachHang;
    private Long tongSanPham;
    private BigDecimal doanhThuChoXacNhan;
    private BigDecimal doanhThuDangGiao;
    private BigDecimal doanhThuDaHuy;
    private BigDecimal doanhThuTaiQuay;
    private BigDecimal doanhThuTrucTuyen;
    private Long donTaiQuay;
    private Long donTrucTuyen;
    private BigDecimal giaTriTrungBinh;
    private Long sanPhamSapHet;

    // Doanh thu theo ngày (cho biểu đồ)
    private List<DoanhThuNgay> doanhThuTheoNgay;

    // Đơn hàng gần đây
    private List<DonHangGanDay> donHangGanDay;

    // Top sản phẩm bán chạy
    private List<SanPhamBanChay> topSanPhamBanChay;

    // Tỷ trọng doanh thu theo danh mục sản phẩm
    private List<TyTrongDanhMuc> tyTrongTheoDanhMuc;

    // Top khách hàng mua hàng
    private List<KhachHangThongKe> topKhachHang;


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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SanPhamBanChay {
        private String maSanPham;
        private String name;
        private String thuongHieu;
        private BigDecimal revenue;
        private Long quantity;
        private Double growth;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TyTrongDanhMuc {
        private String name;
        private BigDecimal revenue;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KhachHangThongKe {
        private String tenKhachHang;
        private BigDecimal tongChi;
        private Long tongSanPham;
        private Long donThanhCong;
        private Long donHoan;
    }

}
