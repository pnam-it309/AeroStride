package com.example.be.core.admin.giaoca.model.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class AdminMoCaRequest {
    private String nhanVienId;
    private BigDecimal tienBanDau;
}
