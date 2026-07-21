package com.example.be.core.admin.dotgiamgia.model.request;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

import java.math.BigDecimal;
import com.example.be.infrastructure.constants.TrangThai;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class AdminDotGiamGiaRequest {

    @NotBlank(message = "Mã đợt giảm giá không được để trống")
    private String ma;

    @NotBlank(message = "Tên đợt giảm giá không được để trống")
    private String ten;

    @NotBlank(message = "Loại giảm giá không được để trống")
    private String loaiGiamGia;

    @NotNull(message = "Số tiền/phần trăm giảm không được để trống")
    private BigDecimal soTienGiam;

    @NotNull(message = "Điều kiện giảm giá không được để trống")
    private BigDecimal dieuKienGiamGia;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private Long ngayBatDau;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private Long ngayKetThuc;

    private Integer mucUuTien;
    private String moTa;

    private TrangThai trangThai;
 
    private List<String> listIdChiTietSanPham;
}
