package com.example.be.core.admin.hoadon.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateHoaDonRequest {
    private String tenNguoiNhan;
    private String soDienThoaiNguoiNhan;
    private String diaChiNguoiNhan;
    private String ghiChu;
    private String idKhachHang;
    private String idNhanVien;
}
