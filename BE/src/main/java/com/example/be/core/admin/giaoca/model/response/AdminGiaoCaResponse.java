package com.example.be.core.admin.giaoca.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class AdminGiaoCaResponse {
    private String id;
    private String maGiaoCa;
    private String nhanVienTrongCaId;
    private String tenNhanVienTrongCa;
    private String nhanVienNhanCaId;
    private String tenNhanVienNhanCa;
    private Long thoiGianVaoCa;
    private Long thoiGianRaCa;
    private BigDecimal tienBanDau;
    private BigDecimal tongDoanhThu;
    private BigDecimal tienThucTe;
    private BigDecimal tienChenhLech;
    private String ghiChu;
    private String trangThai;
}
