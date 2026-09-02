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
    @jakarta.validation.constraints.Size(min = 2, max = 255, message = "Tên khách hàng phải từ 2 đến 255 ký tự")
    @jakarta.validation.constraints.Pattern(
        regexp = "^[\\p{L}0-9\\s]+$",
        message = "Tên khách hàng không được chứa ký tự đặc biệt"
    )
    private String ten;

    @jakarta.validation.constraints.Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    private String tenTaiKhoan;
    private String matKhau;

    private Boolean gioiTinh;

    @NotBlank(message = "Số điện thoại không được để trống")
    @jakarta.validation.constraints.Pattern(
        regexp = "^(0[3|5|7|8|9])[0-9]{8}$",
        message = "Số điện thoại không hợp lệ (phải bắt đầu bằng 03, 05, 07, 08, 09 và gồm 10 chữ số)"
    )
    private String sdt;

    private LocalDate ngaySinh;
    private String hinhAnh;

    @jakarta.validation.constraints.Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String ghiChu;

    private String tinh;
    private String thanhPho;
    private String phuongXa;

    @jakarta.validation.constraints.Size(max = 255, message = "Địa chỉ chi tiết không được vượt quá 255 ký tự")
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
