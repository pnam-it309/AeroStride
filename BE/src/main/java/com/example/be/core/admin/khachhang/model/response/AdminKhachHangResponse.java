package com.example.be.core.admin.khachhang.model.response;

import com.example.be.infrastructure.constants.TrangThai;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

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
    
    // Summary address for list view - named to match frontend
    private String diaChiChiTiet;
    
    // Full list for detail view
    private List<AdminDiaChiResponse> addresses;

    // Constructor for JPQL (13 args)
    public AdminKhachHangResponse(String id, String ma, String ten, String email, String tenTaiKhoan, 
                                 Boolean gioiTinh, String sdt, LocalDate ngaySinh, String hinhAnh, 
                                 String ghiChu, TrangThai trangThai, Long ngayTao, Long ngayCapNhat) {
        this.id = id;
        this.ma = ma;
        this.ten = ten;
        this.email = email;
        this.tenTaiKhoan = tenTaiKhoan;
        this.gioiTinh = gioiTinh;
        this.sdt = sdt;
        this.ngaySinh = ngaySinh;
        this.hinhAnh = hinhAnh;
        this.ghiChu = ghiChu;
        this.trangThai = trangThai;
        this.ngayTao = ngayTao;
        this.ngayCapNhat = ngayCapNhat;
    }

    // Constructor for JPQL with address (14 args)
    public AdminKhachHangResponse(String id, String ma, String ten, String email, String tenTaiKhoan, 
                                 Boolean gioiTinh, String sdt, LocalDate ngaySinh, String hinhAnh, 
                                 String ghiChu, TrangThai trangThai, Long ngayTao, Long ngayCapNhat,
                                 String diaChiChiTiet) {
        this(id, ma, ten, email, tenTaiKhoan, gioiTinh, sdt, ngaySinh, hinhAnh, ghiChu, trangThai, ngayTao, ngayCapNhat);
        this.diaChiChiTiet = diaChiChiTiet;
    }
}
