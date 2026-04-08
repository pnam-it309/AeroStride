package com.example.be.core.admin.phieugiamgia.model.response;

import lombok.*;

import java.math.BigDecimal;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminPhieuGiamGiaResponse {

    private String id;
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
