package com.example.be.core.customer.review.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {
    private Long idHoaDon;
    private Long idSanPham;
    private Long idKhachHang; // Có thể lấy từ token nhưng tạm truyền từ client
    private Integer diemDanhGia;
    private String noiDung;
    private String hinhAnh; // JSON array string
    private String video;
}
