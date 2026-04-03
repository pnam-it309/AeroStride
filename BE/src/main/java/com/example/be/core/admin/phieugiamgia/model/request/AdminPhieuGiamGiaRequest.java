package com.example.be.core.admin.phieugiamgia.model.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdminPhieuGiamGiaRequest {

    private String ma;
    private String ten;

    private String loaiPhieu;
    private Integer phanTramGiamGia;
    private BigDecimal soTienGiam;

    private Integer soLuong;

    private BigDecimal donHangToiThieu;
    private BigDecimal giamToiDa;

    private Long ngayBatDau;
    private Long ngayKetThuc;

    private String ghiChu;
}
