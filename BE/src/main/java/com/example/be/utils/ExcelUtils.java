package com.example.be.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

public class ExcelUtils {

    public static <T> byte[] exportToExcel(String sheetName, String[] headers, List<T> data, Function<T, Object[]> rowMapper) throws IOException {
        // Use SXSSFWorkbook (Streaming POI) for high throughput and low memory footprint
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet(sheetName);

            // Track max column lengths for instant column sizing without slow AWT font measuring
            int[] maxColLengths = new int[headers.length];

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                maxColLengths[i] = headers[i] != null ? headers[i].length() : 10;
            }

            // Create data rows
            int rowIdx = 1;
            for (T item : data) {
                Row row = sheet.createRow(rowIdx++);
                Object[] values = rowMapper.apply(item);
                for (int i = 0; i < values.length && i < headers.length; i++) {
                    Cell cell = row.createCell(i);
                    if (values[i] == null) {
                        cell.setCellValue("");
                    } else if (values[i] instanceof Number) {
                        double numVal = ((Number) values[i]).doubleValue();
                        cell.setCellValue(numVal);
                        maxColLengths[i] = Math.max(maxColLengths[i], String.valueOf((long) numVal).length());
                    } else if (values[i] instanceof Boolean) {
                        String boolText = (Boolean) values[i] ? "Nam" : "Nữ";
                        cell.setCellValue(boolText);
                        maxColLengths[i] = Math.max(maxColLengths[i], boolText.length());
                    } else {
                        String text = values[i].toString();
                        cell.setCellValue(text);
                        maxColLengths[i] = Math.max(maxColLengths[i], Math.min(text.length(), 50));
                    }
                }
            }

            // Set column widths in O(1) arithmetic instead of heavy Java AWT font rendering
            for (int i = 0; i < headers.length; i++) {
                int charWidth = Math.min(50, Math.max(maxColLengths[i] + 4, 12));
                sheet.setColumnWidth(i, charWidth * 256);
            }

            workbook.write(out);
            workbook.dispose(); // clean up temporary files
            return out.toByteArray();
        }
    }

    public static byte[] createTemplate(String sheetName, String[] headers, Object[] sampleData) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(50); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.AQUA.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                int defaultWidth = Math.min(40, Math.max(headers[i].length() + 6, 15));
                sheet.setColumnWidth(i, defaultWidth * 256);
            }

            // Create sample data row
            if (sampleData != null && sampleData.length > 0) {
                Row sampleRow = sheet.createRow(1);
                for (int i = 0; i < sampleData.length && i < headers.length; i++) {
                    Cell cell = sampleRow.createCell(i);
                    if (sampleData[i] != null) {
                        if (sampleData[i] instanceof Number) {
                            cell.setCellValue(((Number) sampleData[i]).doubleValue());
                        } else {
                            cell.setCellValue(sampleData[i].toString());
                        }
                    }
                }
            }

            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        }
    }

    public static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // Handle whole numbers without decimal point if possible
                double numericValue = cell.getNumericCellValue();
                if (numericValue == (long) numericValue) {
                    return String.valueOf((long) numericValue);
                }
                return String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}

