package com.example.be.core.admin.sanpham.model.response;

import com.example.be.infrastructure.constants.GioiTinhKhachHang;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    private String id;
    private String maSanPham;
    private String tenSanPham;
    private String idDanhMuc;
    private String tenDanhMuc;
    private String idThuongHieu;
    private String tenThuongHieu;
    private String idXuatXu;
    private String tenXuatXu;
    private String idMucDichChay;
    private String tenMucDichChay;
    private String idCoGiay;
    private String tenCoGiay;
    private String idChatLieu;
    private String tenChatLieu;
    private String idDeGiay;
    private String tenDeGiay;
    private GioiTinhKhachHang gioiTinhKhachHang;
    private String moTaNgan;
    private String moTaChiTiet;
    private String hinhAnh;
    private TrangThai trangThai;
    private Long ngayTao;
    private String nguoiTao;
    private Long ngayCapNhat;
    private String nguoiCapNhat;
    private List<ProductVariantResponse> variants;
}
