package com.example.be.core.admin.phieugiamgia.model.request;

import com.example.be.core.common.dto.PageRequest;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdminPhieuGiamGiaRequest extends PageRequest {

    private String ma;
    private String ten;

    private String loaiPhieu;
    private String hinhThuc;
    private Integer phanTramGiamGia;
    private BigDecimal soTienGiam;

    private Integer soLuong;

    private BigDecimal donHangToiThieu;
    private BigDecimal giamToiDa;

    private Long ngayBatDau;
    private Long ngayKetThuc;

    private String ghiChu;

    private java.util.List<String> listIdKhachHang;

    // Search fields
    private String keyword;
    private TrangThai trangThai;
    private String timelineStatus;
    private String tuNgay;
    private String denNgay;
}
