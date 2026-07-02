package com.example.be.core.admin.sanpham.model.request;

import com.example.be.infrastructure.constants.TrangThai;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProductVariantImageRequest {

    // Ảnh lưu base64 data URL trực tiếp trong DB nên không giới hạn độ dài.
    private String duongDanAnh;

    @Size(max = 1000, message = "moTa khong duoc vuot qua 1000 ky tu")
    private String moTa;

    private Boolean hinhAnhDaiDien;

    private TrangThai trangThai;
}
