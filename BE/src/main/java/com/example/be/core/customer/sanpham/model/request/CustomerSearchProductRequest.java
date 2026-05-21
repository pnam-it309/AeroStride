package com.example.be.core.customer.sanpham.model.request;

import com.example.be.core.common.dto.PageRequest;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class CustomerSearchProductRequest extends PageRequest {

    private String keyword;
    private String danhMucId;
    private String thuongHieuId;
    private TrangThai trangThai;
    private String gioiTinhKhachHang;
    private String xuatXuId;
    private String mucDichChayId;
    private String chatLieuId;

    public CustomerSearchProductRequest() {
        setPage(1);
        setSize(12);
        setSortBy("ngayTao");
        setSortDirection("desc");
    }
}
