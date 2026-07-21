package com.example.be.core.customer.voucher.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVoucherResponse {

    private String id;
    private String ma;
    private String ten;
    private String loaiPhieu;
    private String hinhThuc;
    private Integer phanTramGiamGia;
    private BigDecimal soTienGiam;
    private Integer soLuong;
    private BigDecimal donHangToiThieu;
    private BigDecimal giamToiDa;
    private Long ngayBatDau;
    private Long ngayKetThuc;
    private String trangThai;
}
