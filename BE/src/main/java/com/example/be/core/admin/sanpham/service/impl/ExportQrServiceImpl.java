package com.example.be.core.admin.sanpham.service.impl;

import com.example.be.core.admin.sanpham.model.request.ExportQrExcelRequest;
import com.example.be.core.admin.sanpham.model.request.ExportQrExcelRowRequest;
import com.example.be.core.admin.sanpham.model.request.ExportQrZipItemRequest;
import com.example.be.core.admin.sanpham.model.request.ExportQrZipRequest;
import com.example.be.core.admin.sanpham.service.ExportQrService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportQrServiceImpl implements ExportQrService {

    @Override
    public byte[] exportQrZip(ExportQrZipRequest request) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Set<String> usedFileNames = new HashSet<>();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            List<ExportQrZipItemRequest> items = request.getItems();
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    ExportQrZipItemRequest item = items.get(i);
                    String baseName = sanitizeFileName(item.getBaseName(), "qr_" + (i + 1));
                    String fileName = baseName + ".png";
                    int dupIndex = 1;
                    while (usedFileNames.contains(fileName)) {
                        dupIndex++;
                        fileName = baseName + "_" + dupIndex + ".png";
                    }
                    usedFileNames.add(fileName);

                    ZipEntry zipEntry = new ZipEntry(fileName);
                    zos.putNextEntry(zipEntry);
                    zos.write(decodeBase64(item.getDataUrl()));
                    zos.closeEntry();
                }
            }
        }
        return baos.toByteArray();
    }

    @Override
    public byte[] exportQrExcel(ExportQrExcelRequest request) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            String sheetName = request.getSheetName() != null ? request.getSheetName() : "QR Code";
            Sheet sheet = workbook.createSheet(sheetName);

            // Set column widths
            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 28 * 256);
            sheet.setColumnWidth(2, 16 * 256);
            sheet.setColumnWidth(3, 14 * 256);
            sheet.setColumnWidth(4, 14 * 256);
            sheet.setColumnWidth(5, 18 * 256);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"STT", "San pham", "Ma SKU", "Mau sac", "Kich thuoc", "QR Code"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Fill data
            List<ExportQrExcelRowRequest> rows = request.getRows();
            List<String> qrDataUrls = request.getQrDataUrls();

            if (rows != null) {
                CreationHelper helper = workbook.getCreationHelper();
                Drawing<?> drawing = sheet.createDrawingPatriarch();

                for (int i = 0; i < rows.size(); i++) {
                    ExportQrExcelRowRequest rowData = rows.get(i);
                    Row row = sheet.createRow(i + 1);
                    row.setHeightInPoints(78); // matched customHeight from FE

                    row.createCell(0).setCellValue(i + 1);
                    row.createCell(1).setCellValue(rowData.getProductName());
                    row.createCell(2).setCellValue(rowData.getSku());
                    row.createCell(3).setCellValue(rowData.getColor());
                    row.createCell(4).setCellValue(rowData.getSize());

                    // Add image
                    if (qrDataUrls != null && i < qrDataUrls.size() && qrDataUrls.get(i) != null) {
                        byte[] imageBytes = decodeBase64(qrDataUrls.get(i));
                        if (imageBytes.length > 0) {
                            int pictureIdx = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
                            ClientAnchor anchor = helper.createClientAnchor();
                            anchor.setCol1(5);
                            anchor.setRow1(i + 1);
                            anchor.setCol2(6);
                            anchor.setRow2(i + 2);
                            anchor.setDx1(60000);
                            anchor.setDy1(60000);
                            anchor.setDx2(-60000);
                            anchor.setDy2(-60000);
                            
                            drawing.createPicture(anchor, pictureIdx);
                        }
                    }
                }
            }
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private String sanitizeFileName(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        String normalized = value.trim().replaceAll("[<>:\"/\\\\|?*\\x00-\\x1F]", "_").replaceAll("\\s+", "_");
        if (normalized.length() > 80) {
            normalized = normalized.substring(0, 80);
        }
        return normalized.isEmpty() ? fallback : normalized;
    }

    private byte[] decodeBase64(String dataUrl) {
        if (dataUrl == null || dataUrl.isEmpty()) {
            return new byte[0];
        }
        String base64 = dataUrl;
        if (dataUrl.contains(",")) {
            base64 = dataUrl.split(",")[1];
        }
        return Base64.getDecoder().decode(base64);
    }
}
