package com.example.be.core.customer.review.model.response;

import com.example.be.entity.DanhGiaSanPham;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerReviewResponse {
    private String id;
    private Integer diemDanhGia;
    private String noiDung;
    private String hinhAnh;
    private String video;
    private String trangThai;
    private Long ngayTao;
    private String tenKhachHang;
    private String avatarKhachHang;
    private CustomerDto khachHang;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerDto {
        private String id;
        private String ten;
        private String anhDaiDien;
    }

    public CustomerReviewResponse(DanhGiaSanPham entity) {
        this.id = entity.getId();
        this.diemDanhGia = entity.getDiemDanhGia();
        this.noiDung = entity.getNoiDung();
        this.hinhAnh = entity.getHinhAnh();
        this.video = entity.getVideo();
        this.trangThai = entity.getTrangThai() != null ? entity.getTrangThai().name() : null;
        this.ngayTao = entity.getNgayTao();
        
        if (entity.getKhachHang() != null) {
            this.tenKhachHang = entity.getKhachHang().getTen();
            this.avatarKhachHang = entity.getKhachHang().getHinhAnh();
            this.khachHang = CustomerDto.builder()
                    .id(entity.getKhachHang().getId())
                    .ten(entity.getKhachHang().getTen())
                    .anhDaiDien(entity.getKhachHang().getHinhAnh())
                    .build();
        } else if (entity.getHoaDon() != null && entity.getHoaDon().getTenNguoiNhan() != null) {
            this.tenKhachHang = entity.getHoaDon().getTenNguoiNhan();
            this.avatarKhachHang = null;
            this.khachHang = null;
        } else {
            this.tenKhachHang = "Khách hàng AeroStride";
            this.avatarKhachHang = null;
            this.khachHang = null;
        }
    }
}
