package com.example.be.core.customer.cart.model.response;
import lombok.*;
import java.math.BigDecimal;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerCartItemResponse {
    private String idChiTietSanPham;
    private String maChiTietSanPham;
    private String tenSanPham;
    private String hinhAnh;
    private String tenMauSac;
    private String tenKichThuoc;
    private BigDecimal giaBan;
    private BigDecimal giaGoc;
    private Integer phanTramGiam;
    private Integer soLuong;
    private Integer soLuongTonKho;
    private Boolean isAvailable;
    private String message;
}