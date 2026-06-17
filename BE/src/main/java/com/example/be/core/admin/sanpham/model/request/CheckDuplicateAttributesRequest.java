package com.example.be.core.admin.sanpham.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckDuplicateAttributesRequest {
    @NotBlank(message = "Thương hiệu không được để trống")
    private String idThuongHieu;

    @NotBlank(message = "Xuất xứ không được để trống")
    private String idXuatXu;

    @NotBlank(message = "Mục đích chạy không được để trống")
    private String idMucDichChay;

    @NotBlank(message = "Cổ giày không được để trống")
    private String idCoGiay;

    @NotBlank(message = "Chất liệu không được để trống")
    private String idChatLieu;

    @NotBlank(message = "Đế giày không được để trống")
    private String idDeGiay;
}
