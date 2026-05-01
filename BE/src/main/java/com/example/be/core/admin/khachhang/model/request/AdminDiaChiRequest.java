package com.example.be.core.admin.khachhang.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDiaChiRequest {
    private String tinh;
    private String thanhPho;
    private String phuongXa;
    private String diaChiChiTiet;
    private String tenNguoiNhan;
    private String sdtNguoiNhan;
    private String idKhachHang;
    private Boolean laMacDinh;
}
