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

    // Tỷ trọng doanh thu theo thương hiệu sản phẩm
    private List<TyTrongThuongHieu> tyTrongTheoThuongHieu;

    // Top khách hàng mua hàng
    private List<KhachHangThongKe> topKhachHang;

    // Top doanh thu nhân viên
    private List<NhanVienThongKe> topNhanVien;

    // Chu kỳ doanh thu tuần, tháng, năm, hôm nay
    private List<ChuKyDoanhThu> chuKyDoanhThu;


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
    public static class TyTrongThuongHieu {
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NhanVienThongKe {
        private String maNhanVien;
        private String tenNhanVien;
        private BigDecimal tongChi;
        private Long tongSanPham;
        private Long tongDonHang;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChuKyDoanhThu {
        private String tenChuKy;
        private BigDecimal doanhThu;
        private Long soDon;
        private BigDecimal trungBinhDon;
    }

}
