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
    private BigDecimal tongTien;
    private Integer trangThai;

    // BẮT BUỘC: Viết Constructor thủ công để khớp với thứ tự trong Repository
    public AdminHoaDonResponse(String id, String maHoaDon, Long ngayTao, String tenKhachHang,
                               String soDienThoai, String tenNhanVien, String loaiDon,
                               BigDecimal tongTien, Integer trangThai) {
        this.id = id;
        this.maHoaDon = maHoaDon;
        this.ngayTao = ngayTao;
        this.tenKhachHang = tenKhachHang;
        this.soDienThoai = soDienThoai;
        this.tenNhanVien = tenNhanVien;
        this.loaiDon = loaiDon;
        this.tongTien = tongTien;
        this.trangThai = trangThai;
    }
}
