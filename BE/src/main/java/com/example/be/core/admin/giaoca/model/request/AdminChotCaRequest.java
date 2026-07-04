package com.example.be.core.admin.giaoca.model.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class AdminChotCaRequest {
    private String nhanVienNhanCaId; // Có thể null nếu chốt ca cuối ngày không ai nhận
    private BigDecimal tienThucTe;
    private String ghiChu;
}
