package com.example.be.core.admin.sanpham.model.request;

import lombok.Data;
import java.util.List;

@Data
public class ExportQrZipRequest {
    private String fileName;
    private List<ExportQrZipItemRequest> items;
}
