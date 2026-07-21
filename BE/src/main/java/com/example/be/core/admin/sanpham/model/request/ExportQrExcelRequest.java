package com.example.be.core.admin.sanpham.model.request;

import lombok.Data;
import java.util.List;

@Data
public class ExportQrExcelRequest {
    private String fileName;
    private String sheetName;
    private List<ExportQrExcelRowRequest> rows;
    private List<String> qrDataUrls;
}
