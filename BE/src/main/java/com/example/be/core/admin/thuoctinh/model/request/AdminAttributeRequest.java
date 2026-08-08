package com.example.be.core.admin.thuoctinh.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminAttributeRequest {

    @Size(max = 50, message = "Ma thuoc tinh khong duoc vuot qua 50 ky tu")
    private String ma;

    @NotBlank(message = "Ten thuoc tinh khong duoc de trong")
    @Size(min = 2, max = 255, message = "Ten thuoc tinh phai tu 2 den 255 ky tu")
    private String ten;

    @Size(max = 255, message = "Mo ta khong duoc vuot qua 255 ky tu")
    private String moTa;

    private String trangThai;

    @Pattern(regexp = "^$|^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Ma mau Hex khong hop le (VD: #FFFFFF)")
    @Size(max = 7, message = "Ma mau Hex khong duoc vuot qua 7 ky tu")
    private String maMauHex;

    @Size(max = 10, message = "Gia tri kich thuoc khong duoc vuot qua 10 ky tu")
    private String giaTriKichThuoc;
}
