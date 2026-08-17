package com.example.be.infrastructure.batch.mapper;

import com.example.be.entity.CaLam;
import com.example.be.entity.LichLamViec;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.batch.ExcelRowMapper;
import com.example.be.repository.CaLamRepository;
import com.example.be.repository.NhanVienRepository;
import com.example.be.utils.ExcelUtils;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class LichLamViecRowMapper implements ExcelRowMapper<LichLamViec> {

    private final NhanVienRepository nhanVienRepository;
    private final CaLamRepository caLamRepository;

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
    };

    @Override
    public LichLamViec mapRow(Row row) throws Exception {
        if (row == null) return null;

        String maNhanVien = ExcelUtils.getCellValueAsString(row.getCell(0)).trim();
        String ngayLamStr = ExcelUtils.getCellValueAsString(row.getCell(2)).trim();
        String tenCa = ExcelUtils.getCellValueAsString(row.getCell(3)).trim();

        if (maNhanVien.isEmpty() && tenCa.isEmpty()) {
            return null;
        }

        NhanVien nv = nhanVienRepository.findByMa(maNhanVien).orElse(null);
        if (nv == null) {
            return null; // Skip if employee not found
        }

        CaLam caLam = caLamRepository.findByXoaMemFalse().stream()
                .filter(c -> c.getTenCa() != null && c.getTenCa().equalsIgnoreCase(tenCa))
                .findFirst()
                .orElse(null);
        if (caLam == null) {
            return null; // Skip if shift not found
        }

        if (ngayLamStr.contains(" ")) {
            ngayLamStr = ngayLamStr.split(" ")[0];
        }

        LocalDate ngayLam = parseDate(ngayLamStr);
        if (ngayLam == null) {
            return null;
        }

        return LichLamViec.builder()
                .nhanVien(nv)
                .caLam(caLam)
                .ngayLam(ngayLam)
                .trangThaiLich(LichLamViec.TrangThaiLichLamViec.DA_XAC_NHAN)
                .build();
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
