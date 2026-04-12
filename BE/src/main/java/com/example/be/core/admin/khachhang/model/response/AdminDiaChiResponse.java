package com.example.be.core.admin.khachhang.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminDiaChiResponse {
    private String id;
    private String tinh;
    private String thanhPho;
    private String phuongXa;
    private String diaChiChiTiet;
    private String tenNguoiNhan;
    private String sdtNguoiNhan;
    private Boolean laMacDinh;
    private String idKhachHang;
    private String tenKhachHang;
}
