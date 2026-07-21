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

    /** Mau sac duoc chon cho bien the san pham. */
    @NotBlank(message = "idMauSac khong duoc de trong")
    private String idMauSac;

    /** Kich thuoc duoc chon cho bien the san pham. */
    @NotBlank(message = "idKichThuoc khong duoc de trong")
    private String idKichThuoc;

    /** Ma SKU/ma vach cua bien the, dung cho quet QR/barcode tai man ban hang. */
    @Size(max = 50, message = "maChiTietSanPham khong duoc vuot qua 50 ky tu")
    private String maChiTietSanPham;

    /** So luong ton kho ban dau hoac so luong cap nhat cua bien the. */
    @NotNull(message = "soLuong khong duoc de trong")
    @Min(value = 0, message = "soLuong phai lon hon hoac bang 0")
    private Integer soLuong;

    /** Gia nhap dung de quan tri loi nhuan, khong hien thi cho khach tai POS. */
    @DecimalMin(value = "0.0", inclusive = true, message = "giaNhap phai lon hon hoac bang 0")
    private BigDecimal giaNhap;

    /** Gia ban goc cua bien the; dot giam gia se tinh gia hien thi rieng. */
    @NotNull(message = "giaBan khong duoc de trong")
    @DecimalMin(value = "0.0", inclusive = true, message = "giaBan phai lon hon hoac bang 0")
    private BigDecimal giaBan;

    /** Trang thai kinh doanh cua bien the: dang ban hoac ngung hoat dong. */
    private TrangThai trangThai;

    /** Danh sach anh cua bien the, gom anh dai dien va anh phu. */
    @Valid
    private List<ProductVariantImageRequest> images = new ArrayList<>();
}
