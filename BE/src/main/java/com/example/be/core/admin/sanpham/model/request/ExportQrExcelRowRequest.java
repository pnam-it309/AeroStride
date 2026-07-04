package com.example.be.core.admin.sanpham.model.request;

import lombok.Data;

@Data
public class ExportQrExcelRowRequest {
    private String productName;
    private String sku;
    private String color;
    private String size;
}
