package com.example.be.core.admin.sanpham.service;

import com.example.be.core.admin.sanpham.model.request.ExportQrExcelRequest;
import com.example.be.core.admin.sanpham.model.request.ExportQrZipRequest;

public interface ExportQrService {
    byte[] exportQrZip(ExportQrZipRequest request) throws Exception;
    byte[] exportQrExcel(ExportQrExcelRequest request) throws Exception;
}
