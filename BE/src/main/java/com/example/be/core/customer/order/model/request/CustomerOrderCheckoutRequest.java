package com.example.be.core.customer.order.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CustomerOrderCheckoutRequest {

    @NotEmpty(message = "Giỏ hàng không được trống")
    @Valid
    private List<CartItem> items;

    @NotBlank(message = "Tên người nhận không được trống")
    private String tenNguoiNhan;

    @NotBlank(message = "Số điện thoại không được trống")
    private String soDienThoai;

    @NotBlank(message = "Địa chỉ không được trống")
    private String diaChi;

    private String tinhThanh;
    private String quanHuyen;
    private String phuongXa;

    private String idPhieuGiamGia;
    private String email;

    // COD, VNPAY
    @NotBlank(message = "Phương thức thanh toán không được trống")
    private String phuongThucThanhToan;

    private String ghiChu;

    @Getter
    @Setter
    public static class CartItem {
        @NotBlank(message = "ID sản phẩm không được trống")
        private String idChiTietSanPham;

        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private Integer soLuong;

        @jakarta.validation.constraints.NotNull(message = "Giá dự kiến không được để trống")
        private BigDecimal giaDuKien;
    }
}
