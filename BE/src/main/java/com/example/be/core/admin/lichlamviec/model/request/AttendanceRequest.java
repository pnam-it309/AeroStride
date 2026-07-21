package com.example.be.core.admin.lichlamviec.model.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {
    private String nhanVienId;
    private String ngay;
    private String gioVao;
    private String gioRa;
    private String ghiChu;
}
