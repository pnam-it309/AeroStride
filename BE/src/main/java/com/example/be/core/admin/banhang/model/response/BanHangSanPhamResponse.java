package com.example.be.core.admin.banhang.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class BanHangSanPhamResponse {
    /** Id bien the san pham de them vao gio hang tai quay. */
    private String id;
    /** Ten san pham cha hien thi trong ket qua tim kiem POS. */
    private String tenSanPham;
    /** SKU/ma vach cua bien the, dung cho tim kiem va quet QR/barcode. */
    private String maChiTietSanPham;
    private String maSanPham;

    private String tenThuongHieu;
    private String tenChatLieu;
    private String tenDeGiay;
    private String tenMauSac;
    private String tenKichThuoc;
    private Integer soLuongTon;
    /** Gia ban goc truoc dot giam gia. */
    private BigDecimal giaGoc;
    /** Gia POS dang ap dung sau khi tinh dot giam gia hop le. */
    private BigDecimal giaBan;
    /** Phan tram giam dang ap dung, dung de hien badge tren FE. */
    private BigDecimal phanTramGiam;
    private String hinhAnh;
}
