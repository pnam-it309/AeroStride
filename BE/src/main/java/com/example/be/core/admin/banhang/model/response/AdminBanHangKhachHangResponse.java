package com.example.be.core.admin.banhang.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminBanHangKhachHangResponse {
    private String id;
    private String tenKhachHang;
    private String sdt;
    private String email;
}
