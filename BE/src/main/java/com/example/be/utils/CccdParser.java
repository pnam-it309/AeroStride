package com.example.be.utils;

import com.example.be.core.common.dto.CccdDto;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
public final class CccdParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");

    private CccdParser() {}

    /**
     * Parses the raw QR code string from a Vietnamese CCCD.
     * Raw string format: ID|OldID|FullName|DOB|Gender|Address|DateOfIssue
     * 
     * @param rawData The raw string from the QR code.
     * @return CccdDto containing the parsed information.
     */
    public static CccdDto parse(String rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return null;
        }

        try {
            String[] parts = rawData.split("\\|");
            if (parts.length < 6) {
                log.warn("Invalid CCCD QR code format: {}", rawData);
                return null;
            }

            return CccdDto.builder()
                    .soCccd(parts[0])
                    .soCmndCu(parts[1])
                    .hoTen(parts[2])
                    .ngaySinh(parseDate(parts[3]))
                    .gioiTinh(parts[4])
                    .diaChi(parts[5])
                    .ngayCap(parts.length > 6 ? parseDate(parts[6]) : null)
                    .build();
        } catch (Exception e) {
            log.error("Error parsing CCCD QR code: {}", e.getMessage());
            return null;
        }
    }

    private static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}
