package com.example.be.core.admin.dotgiamgia.model.request;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

import java.math.BigDecimal;

import com.example.be.infrastructure.constants.TrangThai;

@Getter
@Setter
public class AdminDotGiamGiaRequest {

    private String ma;
    private String ten;

    private String loaiGiamGia;
    private BigDecimal soTienGiam;
    private BigDecimal dieuKienGiamGia;

    private Long ngayBatDau;
    private Long ngayKetThuc;

    private Integer mucUuTien;

    private TrangThai trangThai;
 
    private List<String> listIdChiTietSanPham;
}
