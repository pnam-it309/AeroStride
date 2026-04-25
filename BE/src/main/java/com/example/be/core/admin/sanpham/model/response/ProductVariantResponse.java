package com.example.be.core.admin.sanpham.model.response;

import com.example.be.infrastructure.constants.TrangThai;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private String id;
    private String idSanPham;
    private String maSanPham;
    private String tenSanPham;
    private String tenSanPhamDayDu;
    private String tenThuongHieu;
    private String tenChatLieu;
    private String maChiTietSanPham;
    private String idMauSac;
    private String tenMauSac;
    private String maMauHex;
    private String idKichThuoc;
    private String tenKichThuoc;
    private String giaTriKichThuoc;
    private Integer soLuong;
    private BigDecimal giaNhap;
    private BigDecimal giaBan;
    private TrangThai trangThai;
    private Long ngayTao;
    private Long ngayCapNhat;
    private List<ProductVariantImageResponse> images;
}
