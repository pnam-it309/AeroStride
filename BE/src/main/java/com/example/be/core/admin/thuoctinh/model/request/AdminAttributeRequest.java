package com.example.be.core.admin.thuoctinh.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminAttributeRequest {

    @Size(max = 50, message = "Ma thuoc tinh khong duoc vuot qua 50 ky tu")
    private String ma;

    @NotBlank(message = "Ten thuoc tinh khong duoc de trong")
    @Size(max = 255, message = "Ten thuoc tinh khong duoc vuot qua 255 ky tu")
    private String ten;

    @Size(max = 255, message = "Thong tin bo sung khong duoc vuot qua 255 ky tu")
    private String moTa;

    private String trangThai;
}
