package com.example.be.core.customer.sanpham.model.response;

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
public class CustomerProductFormOptionsResponse {

    private List<CustomerProductOptionResponse> thuongHieus;
    private List<CustomerProductOptionResponse> xuatXus;
    private List<CustomerProductOptionResponse> mucDichChays;
    private List<CustomerProductOptionResponse> coGiays;
    private List<CustomerProductOptionResponse> chatLieus;
    private List<CustomerProductOptionResponse> deGiays;
    private List<CustomerProductOptionResponse> mauSacs;
    private List<CustomerProductOptionResponse> kichThuocs;
    private List<String> gioiTinhKhachHangs;
    private List<String> trangThais;
}
