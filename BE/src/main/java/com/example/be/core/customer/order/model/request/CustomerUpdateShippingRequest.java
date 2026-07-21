package com.example.be.core.customer.order.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Yêu cầu khách hàng cập nhật thông tin nhận hàng của đơn online.
 * Chỉ áp dụng khi đơn ở trạng thái "Chờ xác nhận".
 * Lưu ý: việc đổi địa chỉ KHÔNG làm thay đổi phí vận chuyển đã chốt.
 */
@Getter
@Setter
public class CustomerUpdateShippingRequest {

    @NotBlank(message = "Tên người nhận không được trống")
    private String tenNguoiNhan;

    @NotBlank(message = "Số điện thoại không được trống")
    private String soDienThoaiNguoiNhan;

    @NotBlank(message = "Địa chỉ nhận hàng không được trống")
    private String diaChiNguoiNhan;

    private String ghiChu;
}
