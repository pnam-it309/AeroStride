package com.example.be.core.admin.dotgiamgia.model.request;

import com.example.be.core.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDotGiamGiaSearchRequest extends PageRequest {
    private String keyword;
    private com.example.be.infrastructure.constants.TrangThai trangThai;
}
