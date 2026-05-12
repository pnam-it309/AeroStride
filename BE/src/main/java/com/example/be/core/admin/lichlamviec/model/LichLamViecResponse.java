package com.example.be.core.admin.lichlamviec.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LichLamViecResponse {
    private String id;
    private String nhanVien;
    private String ca;
    private String ngay;
    private String trangThai;
}
