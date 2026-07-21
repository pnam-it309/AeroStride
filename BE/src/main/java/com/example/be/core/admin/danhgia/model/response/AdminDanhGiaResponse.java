package com.example.be.core.admin.danhgia.model.response;

import com.example.be.entity.DanhGiaSanPham;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDanhGiaResponse {
    private String id;
    private String tenKhachHang;
    private String soDienThoai;
    private String tenSanPham;
    private String hinhAnhSanPham;
    private Integer diemDanhGia;
    private String noiDung;
    private String hinhAnhDanhGia;
    private String videoDanhGia;
    private DanhGiaSanPham.TrangThaiDanhGia trangThai;
    private Long ngayTao;

    public AdminDanhGiaResponse(DanhGiaSanPham entity) {
        this.id = entity.getId();
        if (entity.getKhachHang() != null) {
            this.tenKhachHang = entity.getKhachHang().getTen();
            this.soDienThoai = entity.getKhachHang().getSdt();
        }
        if (entity.getSanPham() != null) {
            this.tenSanPham = entity.getSanPham().getTen();
            this.hinhAnhSanPham = entity.getSanPham().getHinhAnh();
        }
        this.diemDanhGia = entity.getDiemDanhGia();
        this.noiDung = entity.getNoiDung();
        this.hinhAnhDanhGia = entity.getHinhAnh();
        this.videoDanhGia = entity.getVideo();
        this.trangThai = entity.getTrangThai();
        this.ngayTao = entity.getNgayTao();
    }
}
