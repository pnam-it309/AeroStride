package com.example.be.core.admin.khachhang.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDiaChiRequest {
    private String tinh;
    private String thanhPho;
    private String phuongXa;

    @Size(max = 255, message = "Địa chỉ chi tiết không được vượt quá 255 ký tự")
    private String diaChiChiTiet;

    @Size(min = 2, max = 100, message = "Tên người nhận phải từ 2 đến 100 ký tự")
    private String tenNguoiNhan;

    @Pattern(regexp = "^(0[3|5|7|8|9])[0-9]{8}$", message = "Số điện thoại người nhận không hợp lệ (gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09)")
    private String sdtNguoiNhan;

    private String idKhachHang;
    private Boolean laMacDinh;
}
