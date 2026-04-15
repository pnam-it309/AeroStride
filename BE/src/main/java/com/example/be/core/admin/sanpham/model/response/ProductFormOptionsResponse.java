package com.example.be.core.admin.sanpham.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFormOptionsResponse {

    private List<ProductOptionResponse> danhMucs;
    private List<ProductOptionResponse> thuongHieus;
    private List<ProductOptionResponse> xuatXus;
    private List<ProductOptionResponse> mucDichChays;
    private List<ProductOptionResponse> coGiays;
    private List<ProductOptionResponse> chatLieus;
    private List<ProductOptionResponse> deGiays;
    private List<ProductOptionResponse> mauSacs;
    private List<ProductOptionResponse> kichThuocs;
    private List<String> gioiTinhKhachHangs;
    private List<String> trangThais;
}
