package com.example.be.core.admin.khachhang.model.request;

import com.example.be.core.common.dto.PageRequest;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class AdminKhachHangRequest extends PageRequest {
    // crud
    private String ma;

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String ten;

    @NotBlank(message = "Email không được để trống")
    private String email;

    private String tenTaiKhoan;
    private String matKhau;

    @NotNull(message = "Giới tính không được để trống")
    private Boolean gioiTinh;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String sdt;

    private LocalDate ngaySinh;
    private String hinhAnh;
    private String ghiChu;

    private String tinh;
    private String thanhPho;
    private String phuongXa;
    private String diaChiChiTiet;

    private String idDiaChi;


    // search, filter
    private String keyword;        // tìm tên / email / sdt / mã
    private TrangThai trangThai;     // null = tất cả
    
    // filter for stats tab
    private String sdtSearch;
    private Double minTongChiTieu;
    private Double maxTongChiTieu;
    private LocalDate minNgayDonHang;
    private LocalDate maxNgayDonHang;

}
