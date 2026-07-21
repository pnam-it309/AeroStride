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

    /** Id bien the san pham. */
    private String id;
    /** Id san pham cha cua bien the. */
    private String idSanPham;
    private String maSanPham;
    private String tenSanPham;
    private String tenSanPhamDayDu;
    private String tenThuongHieu;
    private String tenChatLieu;
    private String hinhAnh;
    private String maChiTietSanPham;
    private String idMauSac;
    private String tenMauSac;
    private String maMauHex;
    private String idKichThuoc;
    private String tenKichThuoc;
    private String giaTriKichThuoc;
    /** Ton kho hien tai cua bien the. */
    private Integer soLuong;
    /** Gia nhap noi bo. */
    private BigDecimal giaNhap;
    /** Gia ban goc luu tren bien the. */
    private BigDecimal giaGoc;
    /** Gia ban hien thi sau khi tinh dot giam gia dang hoat dong. */
    private BigDecimal giaBan;
    /** Phan tram giam dang ap dung, null/0 neu khong co dot giam gia. */
    private BigDecimal phanTramGiam;
    private TrangThai trangThai;
    private Long ngayTao;
    private Long ngayCapNhat;
    private List<ProductVariantImageResponse> images;
}
