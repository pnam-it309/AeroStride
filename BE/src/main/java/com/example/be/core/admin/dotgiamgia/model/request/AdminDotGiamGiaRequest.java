package com.example.be.core.admin.dotgiamgia.model.request;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

import java.math.BigDecimal;
import com.example.be.infrastructure.constants.TrangThai;
import jakarta.validation.constraints.*;

@Getter
@Setter
public class AdminDotGiamGiaRequest {

    @NotBlank(message = "Mã đợt giảm giá không được để trống")
    @Size(max = 50, message = "Mã đợt giảm giá không được vượt quá 50 ký tự")
    private String ma;

    @NotBlank(message = "Tên đợt giảm giá không được để trống")
    @Size(min = 3, max = 255, message = "Tên đợt giảm giá phải từ 3 đến 255 ký tự")
    @Pattern(regexp = "^[\\p{L}0-9\\s]+$", message = "Tên đợt giảm giá không được chứa ký tự đặc biệt")
    private String ten;

    @NotBlank(message = "Loại giảm giá không được để trống")
    private String loaiGiamGia;

    @NotNull(message = "Số tiền/phần trăm giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Mức giảm giá phải lớn hơn 0")
    private BigDecimal soTienGiam;

    private BigDecimal dieuKienGiamGia;

    private BigDecimal giamToiDa;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private Long ngayBatDau;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private Long ngayKetThuc;

    @Min(value = 0, message = "Mức ưu tiên không được âm")

    @Max(value = 999, message = "Mức ưu tiên không được vượt quá 999")
    private Integer mucUuTien;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String moTa;

    private TrangThai trangThai;
 
    private Boolean isFlashSale;

    private String khungGio;

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 sản phẩm áp dụng")
    private List<String> listIdChiTietSanPham;
}
