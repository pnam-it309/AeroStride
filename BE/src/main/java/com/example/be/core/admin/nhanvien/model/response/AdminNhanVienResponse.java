package com.example.be.core.admin.nhanvien.model.response;

import com.example.be.infrastructure.constants.TrangThai;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNhanVienResponse {
    private String id;
    private String ma;
    private String ten;
    private String email;
    private String tenTaiKhoan;
    private Boolean gioiTinh;
    private String sdt;
    private LocalDate ngaySinh;
    private String hinhAnh;
    private String tinh;
    private String thanhPho;
    private String phuongXa;
    private String diaChiChiTiet;
    private TrangThai trangThai;
    private Long ngayTao;
    private Long ngayCapNhat;

    // từ bảng phan_quyen (join)
    private String maPhanQuyen;
    private String tenPhanQuyen;
    private String quyenHan;

}
