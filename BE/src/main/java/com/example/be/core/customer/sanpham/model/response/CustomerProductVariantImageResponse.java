package com.example.be.core.customer.sanpham.model.response;

import com.example.be.infrastructure.constants.TrangThai;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProductVariantImageResponse {

    private String id;
    private String duongDanAnh;
    private String moTa;
    private Boolean hinhAnhDaiDien;
    private TrangThai trangThai;
    private Long ngayTao;
    private Long ngayCapNhat;
}
