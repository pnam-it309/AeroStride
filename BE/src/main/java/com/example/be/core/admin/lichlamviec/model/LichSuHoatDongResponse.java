package com.example.be.core.admin.lichlamviec.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LichSuHoatDongResponse {
    private String id;
    private String nguoiThucHien;
    private String hanhDong;
    private String doiTuong;
    private String ngay;
}
