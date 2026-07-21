package com.example.be.core.admin.danhgia.model.request;

import com.example.be.core.common.dto.PageRequest;
import com.example.be.entity.DanhGiaSanPham;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AdminDanhGiaFilterRequest extends PageRequest {
    private String keyword;
    private DanhGiaSanPham.TrangThaiDanhGia trangThai;
}
