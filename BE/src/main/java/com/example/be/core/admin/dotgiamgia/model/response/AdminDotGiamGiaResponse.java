package com.example.be.core.admin.dotgiamgia.model.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class AdminDotGiamGiaResponse {

    private String id;
    private String ma;
    private String ten;
    private String loaiGiamGia;
    private BigDecimal soTienGiam;
    private BigDecimal dieuKienGiamGia;
    private Long ngayBatDau;
    private Long ngayKetThuc;
    private Integer mucUuTien;
    private String trangThai;
    private String moTa;

    public AdminDotGiamGiaResponse(String id, String ma, String ten, String loaiGiamGia, BigDecimal soTienGiam, BigDecimal dieuKienGiamGia, Long ngayBatDau, Long ngayKetThuc, Integer mucUuTien, String trangThai, String moTa) {
        this.id = id;
        this.ma = ma;
        this.ten = ten;
        this.loaiGiamGia = loaiGiamGia;
        this.soTienGiam = soTienGiam;
        this.dieuKienGiamGia = dieuKienGiamGia;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
        this.mucUuTien = mucUuTien;
        this.trangThai = trangThai;
        this.moTa = moTa;
    }
}
