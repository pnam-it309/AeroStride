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
    private String nhanVienId;
    private String maNhanVien;
    private String ca;
    private String caId;
    private String ngay;
    private String trangThai;
    private Boolean tangCa;
    private String gioBatDauTangCa;
    private String gioKetThucTangCa;
    private String gioVao;
    private String gioRa;
    private String ghiChu;
}
