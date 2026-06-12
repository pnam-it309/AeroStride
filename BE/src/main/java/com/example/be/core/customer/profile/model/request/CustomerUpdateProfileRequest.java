package com.example.be.core.customer.profile.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerUpdateProfileRequest {
    @NotBlank(message = "Họ và tên không được để trống")
    private String ten;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String sdt;
    
    // Optional fields if user wants to update avatar link later
    private String hinhAnh;
}
