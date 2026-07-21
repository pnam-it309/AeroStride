package com.example.be.core.admin.hoadon.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminLichSuHoaDonResponse {
    private String id;
    private Integer trangThaiCu;
    private Integer trangThaiMoi;
    private String ghiChu;
    private String nguoiThucHien;
    private Long ngayTao;
}
