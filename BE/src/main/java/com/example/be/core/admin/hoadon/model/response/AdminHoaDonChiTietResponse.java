package com.example.be.core.admin.hoadon.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class AdminHoaDonChiTietResponse {
    private String id;
    private String idCtsp;
    private String tenSanPham;
    private String maSanPham;
    private String mauSac;
    private String kichThuoc;
    private Integer soLuong;
    private BigDecimal donGia;
    private String hinhAnh;
}
