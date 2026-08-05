package com.example.be.core.customer.profile.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerAddressRequest {
    private String tenNguoiNhan;
    private String sdtNguoiNhan;
    private String tinh;
    private String thanhPho;
    private String phuongXa;
    private String diaChiChiTiet;
    private Boolean laMacDinh;
}
