package com.example.be.core.admin.sanpham.model.request;

import lombok.Data;

@Data
public class ExportQrZipItemRequest {
    private String baseName;
    private String dataUrl;
}
