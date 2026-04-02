package com.example.be.core.admin.sanpham.model.request;

import com.example.be.infrastructure.constants.TrangThai;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProductVariantImageRequest {

    @Size(max = 1000, message = "duongDanAnh khong duoc vuot qua 1000 ky tu")
    private String duongDanAnh;

    @Size(max = 1000, message = "moTa khong duoc vuot qua 1000 ky tu")
    private String moTa;

    private Boolean hinhAnhDaiDien;

    private TrangThai trangThai;
}
