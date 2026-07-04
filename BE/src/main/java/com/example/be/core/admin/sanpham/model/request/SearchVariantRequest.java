package com.example.be.core.admin.sanpham.model.request;

import com.example.be.core.common.dto.PageRequest;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString(callSuper = true)
public class SearchVariantRequest extends PageRequest {

    private String keyword;
    private String sanPhamId;
    private String mauSacId;
    private String kichThuocId;
    private TrangThai trangThai;
    private BigDecimal minGia;
    private BigDecimal maxGia;

    public SearchVariantRequest() {
        setPage(0);
        setSize(10);
        setSortBy("ngayTao");
        setSortDirection("desc");
    }
}
