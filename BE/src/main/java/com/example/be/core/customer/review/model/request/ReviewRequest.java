package com.example.be.core.customer.review.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {
    private String idHoaDon;
    private String idSanPham;
    private String idKhachHang;
    private Integer diemDanhGia;
    private String noiDung;
    private String hinhAnh;
    private String video;
}
