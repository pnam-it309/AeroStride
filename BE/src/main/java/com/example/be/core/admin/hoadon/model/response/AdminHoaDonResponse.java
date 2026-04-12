package com.example.be.core.admin.hoadon.model.response;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class AdminHoaDonResponse {
    private String id;
    private String maHoaDon;
    private Long ngayTao;
    private String tenKhachHang;
    private String soDienThoai;
    private String tenNhanVien;
    private String loaiDon;
    private BigDecimal phiVanChuyen;
    private BigDecimal tongTien;
    private BigDecimal tongTienSauGiam;
    private Integer trangThai;
    private String ghiChu;

    public AdminHoaDonResponse(String id, String maHoaDon, Long ngayTao, String tenKhachHang,
                               String soDienThoai, String tenNhanVien, String loaiDon,
                               BigDecimal phiVanChuyen, BigDecimal tongTien, BigDecimal tongTienSauGiam,
                               Integer trangThai, String ghiChu) {
        this.id = id;
        this.maHoaDon = maHoaDon;
        this.ngayTao = ngayTao;
        this.tenKhachHang = tenKhachHang;
        this.soDienThoai = soDienThoai;
        this.tenNhanVien = tenNhanVien;
        this.loaiDon = loaiDon;
        this.phiVanChuyen = phiVanChuyen;
        this.tongTien = tongTien;
        this.tongTienSauGiam = tongTienSauGiam;
        this.trangThai = trangThai;
        this.ghiChu = ghiChu;
    }
}
