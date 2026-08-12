package com.example.be.core.customer.sanpham.model.request;

import com.example.be.core.common.dto.PageRequest;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
public class CustomerSearchProductRequest extends PageRequest {

    private String keyword;
    private String thuongHieuId;
    private TrangThai trangThai;
    private String gioiTinhKhachHang;
    private String xuatXuId;
    private String mucDichChayId;
    private List<String> mucDichChayIds;
    private String chatLieuId;
    private BigDecimal minGia;
    private BigDecimal maxGia;
    private String kichThuoc;

    public CustomerSearchProductRequest() {
        setPage(0);
        setSize(12);
        setSortBy("ngayTao");
        setSortDirection("desc");
    }

    /**
     * Returns the raw/original sortBy value before transformation (e.g. "price_asc", "newest").
     */
    public String getRawSortBy() {
        return super.getSortBy();
    }

    @Override
    public String getSortBy() {
        String sort = super.getSortBy();
        if ("newest".equals(sort)) {
            super.setSortDirection("desc");
            return "ngayTao";
        } else if ("price_asc".equals(sort) || "price_desc".equals(sort)) {
            // Price sorting is handled at the service layer after query
            // Use ngayTao as the DB sort to get all products, then re-sort by price in Java
            super.setSortDirection("desc");
            return "ngayTao"; 
        }
        return sort;
    }
}
