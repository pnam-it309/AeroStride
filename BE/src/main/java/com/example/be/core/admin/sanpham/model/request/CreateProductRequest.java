package com.example.be.core.admin.sanpham.model.request;

import com.example.be.infrastructure.constants.GioiTinhKhachHang;
import com.example.be.infrastructure.constants.TrangThai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateProductRequest {

    @Size(max = 50, message = "maSanPham khong duoc vuot qua 50 ky tu")
    private String maSanPham;

    @NotBlank(message = "tenSanPham khong duoc de trong")
    @Size(max = 255, message = "tenSanPham khong duoc vuot qua 255 ky tu")
    private String tenSanPham;

    @NotBlank(message = "idDanhMuc khong duoc de trong")
    private String idDanhMuc;

    @NotBlank(message = "idThuongHieu khong duoc de trong")
    private String idThuongHieu;

    @NotBlank(message = "idXuatXu khong duoc de trong")
    private String idXuatXu;

    @NotBlank(message = "idMucDichChay khong duoc de trong")
    private String idMucDichChay;

    @NotBlank(message = "idCoGiay khong duoc de trong")
    private String idCoGiay;

    @NotBlank(message = "idChatLieu khong duoc de trong")
    private String idChatLieu;

    @NotBlank(message = "idDeGiay khong duoc de trong")
    private String idDeGiay;

    @NotNull(message = "gioiTinhKhachHang khong duoc de trong")
    private GioiTinhKhachHang gioiTinhKhachHang;

    @Size(max = 1000, message = "hinhAnh khong duoc vuot qua 1000 ky tu")
    private String hinhAnh;

    @Size(max = 1000, message = "moTaNgan khong duoc vuot qua 1000 ky tu")
    private String moTaNgan;

    private String moTaChiTiet;

    private TrangThai trangThai;

    @Valid
    private List<ProductVariantRequest> variants = new ArrayList<>();
}
