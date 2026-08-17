package com.example.be.infrastructure.batch.mapper;

import com.example.be.entity.PhieuGiamGia;
import com.example.be.infrastructure.batch.ExcelRowMapper;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.constants.LoaiPhieuGiamGia;
import com.example.be.utils.ExcelUtils;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class PhieuGiamGiaRowMapper implements ExcelRowMapper<PhieuGiamGia> {

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
    };

    @Override
    public PhieuGiamGia mapRow(Row row) throws Exception {
        if (row == null) return null;

        String ma = ExcelUtils.getCellValueAsString(row.getCell(0)).trim();
        String ten = ExcelUtils.getCellValueAsString(row.getCell(1)).trim();
        if (ma.isEmpty() && ten.isEmpty()) {
            return null; // Skip empty row
        }

        PhieuGiamGia p = new PhieuGiamGia();
        p.setMa(ma);
        p.setTen(ten);
        p.setLoaiPhieu(ExcelUtils.getCellValueAsString(row.getCell(2)).trim());

        String valStr = ExcelUtils.getCellValueAsString(row.getCell(3)).trim();
        if (!valStr.isEmpty()) {
            if (LoaiPhieuGiamGia.isPhanTram(p.getLoaiPhieu())) {
                p.setPhanTramGiamGia((int) Double.parseDouble(valStr));
            } else {
                p.setSoTienGiam(new BigDecimal(valStr));
            }
        }

        String minOrderStr = ExcelUtils.getCellValueAsString(row.getCell(4)).trim();
        if (!minOrderStr.isEmpty()) {
            p.setDonHangToiThieu(new BigDecimal(minOrderStr));
        }

        String qtyStr = ExcelUtils.getCellValueAsString(row.getCell(5)).trim();
        if (!qtyStr.isEmpty()) {
            p.setSoLuong((int) Double.parseDouble(qtyStr));
        }

        String hinhThuc = ExcelUtils.getCellValueAsString(row.getCell(6)).trim();
        p.setHinhThuc(hinhThuc.isEmpty() ? "CONG_KHAI" : hinhThuc);

        String startDateStr = ExcelUtils.getCellValueAsString(row.getCell(7)).trim();
        String endDateStr = ExcelUtils.getCellValueAsString(row.getCell(8)).trim();

        if (!startDateStr.isEmpty()) {
            LocalDate startDate = parseDate(startDateStr);
            if (startDate != null) {
                p.setNgayBatDau(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
        }

        if (!endDateStr.isEmpty()) {
            LocalDate endDate = parseDate(endDateStr);
            if (endDate != null) {
                p.setNgayKetThuc(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1L);
            }
        }

        p.setTrangThai(TrangThai.DANG_HOAT_DONG);
        return p;
    }

    private LocalDate parseDate(String text) {
        for (DateTimeFormatter dtf : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(text, dtf);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
