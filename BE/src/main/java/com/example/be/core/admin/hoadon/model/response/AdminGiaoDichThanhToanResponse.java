package com.example.be.core.admin.hoadon.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class AdminGiaoDichThanhToanResponse {
    private String id;
    private String tenPhuongThuc;
    private BigDecimal soTien;
    private String loaiGiaoDich;
    private String maGiaoDichNgoai;
    private String ghiChu;
    private Long ngayTao;
    private Integer trangThai;
    private String nguoiXacNhan;
}
