package com.example.be.core.admin.sanpham.model.request;

import com.example.be.infrastructure.constants.TrangThai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductVariantRequest {

    @NotBlank(message = "idMauSac khong duoc de trong")
    private String idMauSac;

    @NotBlank(message = "idKichThuoc khong duoc de trong")
    private String idKichThuoc;

    @Size(max = 50, message = "maChiTietSanPham khong duoc vuot qua 50 ky tu")
    private String maChiTietSanPham;

    @NotNull(message = "soLuong khong duoc de trong")
    @Min(value = 0, message = "soLuong phai lon hon hoac bang 0")
    private Integer soLuong;

    @DecimalMin(value = "0.0", inclusive = true, message = "giaNhap phai lon hon hoac bang 0")
    private BigDecimal giaNhap;

    @NotNull(message = "giaBan khong duoc de trong")
    @DecimalMin(value = "0.0", inclusive = true, message = "giaBan phai lon hon hoac bang 0")
    private BigDecimal giaBan;

    private TrangThai trangThai;

    @Valid
    private List<ProductVariantImageRequest> images = new ArrayList<>();
}
