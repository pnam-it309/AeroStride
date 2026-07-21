package com.example.be.core.customer.profile.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CustomerProfileResponse {
    private String id;
    private String tenTaiKhoan;
    private String email;
    private String ten;
    private String sdt;
    private String diaChiChiTiet;
    private String phuongXa;
    private String quanHuyen;
    private String tinhThanh;
    private String hinhAnh;
}
