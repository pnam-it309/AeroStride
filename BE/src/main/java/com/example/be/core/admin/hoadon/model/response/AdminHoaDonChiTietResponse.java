package com.example.be.core.admin.hoadon.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class AdminHoaDonChiTietResponse {
    private String id;
    private String idCtsp;
    private String tenSanPham;
    private String maSanPham;
    private String maChiTietSanPham;
    private String mauSac;
    private String kichThuoc;
    private Integer soLuong;
    private BigDecimal donGia;       // đơn giá đã chốt trên hóa đơn
    private BigDecimal giaGoc;       // giá gốc niêm yết
    private Integer phanTramGiam;    // % giảm giá (nếu có)
    private String tenDotGiamGia;    // tên đợt giảm giá (nếu có)
    private BigDecimal giaHienTai;   // giá bán hiện tại của sản phẩm (để phát hiện đổi giá)
    private String hinhAnh;
}
