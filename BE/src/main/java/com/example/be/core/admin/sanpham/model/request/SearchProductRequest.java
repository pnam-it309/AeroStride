package com.example.be.core.admin.sanpham.model.request;

import com.example.be.core.common.dto.PageRequest;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchProductRequest extends PageRequest {

    private String keyword;
    private String danhMucId;
    private String thuongHieuId;
    private TrangThai trangThai;
    private String gioiTinhKhachHang;
    private String xuatXuId;
    private String mucDichChayId;
    private String chatLieuId;

    public SearchProductRequest() {
        setPage(1);
        setSize(10);
        setSortBy("ngayTao");
        setSortDirection("desc");
    }
}
