package com.example.be.core.customer.order.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Yêu cầu khách hàng cập nhật số lượng sản phẩm trong đơn online.
 * Chỉ áp dụng cho đơn thanh toán tiền mặt (COD) ở trạng thái "Chờ xác nhận".
 * Giỏ hàng không được rỗng: phải còn ít nhất 1 biến thể với số lượng tối thiểu là 1.
 */
@Getter
@Setter
public class CustomerUpdateItemsRequest {

    @NotEmpty(message = "Đơn hàng phải còn ít nhất 1 sản phẩm")
    @Valid
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        @NotBlank(message = "ID sản phẩm không được trống")
        private String idChiTietSanPham;

        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private Integer soLuong;
    }
}
