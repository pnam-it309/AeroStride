package com.example.be.core.admin.thuoctinh.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminAttributeResponse {
    private String id;
    private String ma;
    private String ten;
    private String moTa;
    private String trangThai;
    private Long ngayTao;
    private Long ngayCapNhat;
}
