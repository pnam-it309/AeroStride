package com.example.be.core.admin.sanpham.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionResponse {

    private String id;
    private String ma;
    private String ten;
    private String moTa;
}
