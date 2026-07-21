package com.example.be.core.admin.phieugiamgia.model.request;

import com.example.be.core.common.dto.PageRequest;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class AdminPhieuGiamGiaRequest extends PageRequest {

    @NotBlank(message = "Mã phiếu giảm giá không được để trống")
    private String ma;

    @NotBlank(message = "Tên phiếu giảm giá không được để trống")
    private String ten;

    @NotBlank(message = "Loại phiếu không được để trống")
    private String loaiPhieu;

    @NotBlank(message = "Hình thức không được để trống")
    private String hinhThuc;

    private Integer phanTramGiamGia;
    private BigDecimal soTienGiam;

    @NotNull(message = "Số lượng không được để trống")
    private Integer soLuong;

    @NotNull(message = "Đơn hàng tối thiểu không được để trống")
    private BigDecimal donHangToiThieu;
    private BigDecimal giamToiDa;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private Long ngayBatDau;

    @NotNull(message = "Ngày kết thúc không được để trống")
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
