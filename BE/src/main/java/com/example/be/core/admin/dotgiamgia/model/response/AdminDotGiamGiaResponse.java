package com.example.be.core.admin.dotgiamgia.model.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDotGiamGiaResponse {

    private String id;
    private String ma;
    private String ten;
    private String loaiGiamGia;
    private BigDecimal soTienGiam;
    private BigDecimal dieuKienGiamGia;
    private BigDecimal giamToiDa;
    private Long ngayBatDau;
    private Long ngayKetThuc;
    private Integer mucUuTien;
    private String trangThai;
    private String moTa;
    private Boolean isFlashSale;
    private String khungGio;

}
