package com.example.be.core.admin.khachhang.model.response;

import com.example.be.infrastructure.constants.TrangThai;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminKhachHangResponse {
    private String id;
    private String ma;
    private String ten;
    private String email;
    private String tenTaiKhoan;
    private Boolean gioiTinh;
    private String sdt;
    private LocalDate ngaySinh;
    private String hinhAnh;
    private String ghiChu;
    private TrangThai trangThai;
    private Long ngayTao;
    private Long ngayCapNhat;

    // địa chỉ
    private String tinh;
    private String thanhPho;
    private String phuongXa;
    private String diaChiChiTiet;
    private String tenNguoiNhan;
    private String sdtNguoiNhan;

}
