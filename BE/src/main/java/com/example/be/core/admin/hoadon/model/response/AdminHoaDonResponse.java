package com.example.be.core.admin.hoadon.model.response;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminHoaDonResponse {
    private String id;
    private String maHoaDon;
    private Long ngayTao;
    private String tenKhachHang;
    private String soDienThoai;
    private String maNhanVien;
    private String tenNhanVien;
    private String loaiDon;
    private BigDecimal phiVanChuyen;
    private BigDecimal tongTien;
    private BigDecimal tongTienSauGiam;
    private Integer trangThai;
    private String ghiChu;
}
