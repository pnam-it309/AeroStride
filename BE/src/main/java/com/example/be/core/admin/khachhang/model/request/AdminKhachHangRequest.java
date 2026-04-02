package com.example.be.core.admin.khachhang.model.request;

import com.example.be.infrastructure.constants.TrangThai;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class AdminKhachHangRequest {
    // crud
    private String ma;
    private String ten;

    private String email;

    private String tenTaiKhoan;
    private String matKhau;

    private Boolean gioiTinh;
    private String sdt;
    private LocalDate ngaySinh;
    private String hinhAnh;
    private String ghiChu;

    private String idDiaChi;


    // search, filter
    private String keyword;        // tìm tên / email / sdt / mã
    private TrangThai trangThai;     // null = tất cả
    private Integer page = 0;
    private Integer size = 10;

}
