package com.example.be.core.customer.sanpham.model.request;

import com.example.be.core.common.dto.PageRequest;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
public class CustomerSearchProductRequest extends PageRequest {

    private String keyword;
    private String thuongHieuId;
    private List<String> thuongHieuIds;
    private TrangThai trangThai;
    private String gioiTinhKhachHang;
    private String xuatXuId;
    private List<String> xuatXuIds;
    private String mucDichChayId;
    private List<String> mucDichChayIds;
    private String chatLieuId;
    private List<String> chatLieuIds;
    private BigDecimal minGia;
    private BigDecimal maxGia;
    private String kichThuoc;
    private List<String> kichThuocs;

    public CustomerSearchProductRequest() {
        setPage(0);
        setSize(12);
        setSortBy("ngayTao");
        setSortDirection("desc");
    }

    private List<String> parseStringList(List<String> list, String singleVal) {
        List<String> result = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            for (String item : list) {
                if (item != null && !item.isBlank()) {
                    if (item.contains(",")) {
                        result.addAll(Arrays.stream(item.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
                    } else {
                        result.add(item.trim());
                    }
                }
            }
        }
        if (singleVal != null && !singleVal.isBlank()) {
            if (singleVal.contains(",")) {
                result.addAll(Arrays.stream(singleVal.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
            } else if (!result.contains(singleVal.trim())) {
                result.add(singleVal.trim());
            }
        }
        return result.isEmpty() ? null : result;
    }

    public List<String> getEffectiveMucDichChayIds() {
        return parseStringList(this.mucDichChayIds, this.mucDichChayId);
    }

    public List<String> getEffectiveKichThuocs() {
        return parseStringList(this.kichThuocs, this.kichThuoc);
    }

    public List<String> getEffectiveThuongHieuIds() {
        return parseStringList(this.thuongHieuIds, this.thuongHieuId);
    }

    public List<String> getEffectiveChatLieuIds() {
        return parseStringList(this.chatLieuIds, this.chatLieuId);
    }

    public List<String> getEffectiveXuatXuIds() {
        return parseStringList(this.xuatXuIds, this.xuatXuId);
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
