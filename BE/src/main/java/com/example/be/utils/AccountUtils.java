package com.example.be.utils;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.UUID;

/**
 * Centralized utility for account-related helpers.
 * Eliminates duplicate implementations across NhanVien and KhachHang service layers.
 */
public final class AccountUtils {

    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AccountUtils() {}

    /**
     * Generates a random temporary password of the given length.
     * Uses SecureRandom for cryptographic safety.
     */
    public static String taoMatKhauNgauNhien(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Removes Vietnamese diacritics and converts 'đ'/'Đ' to 'd'/'D'.
     */
    public static String xoaDau(String s) {
        if (s == null || s.isBlank()) return "";
        String result = Normalizer.normalize(s, Normalizer.Form.NFD);
        result = result.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return result.replace("đ", "d").replace("Đ", "D");
    }

    /**
     * Derives a base username from a full Vietnamese name.
     * Example: "Nguyễn Văn An" → "anvn"
     */
    public static String taoTenTaiKhoanTuHoTen(String hoTen) {
        if (hoTen == null || hoTen.isBlank()) return "user";
        String[] parts = hoTen.trim().split("\\s+");
        String ten = xoaDau(parts[parts.length - 1]).toLowerCase();
        StringBuilder vietTat = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            vietTat.append(xoaDau(String.valueOf(parts[i].charAt(0))).toLowerCase());
        }
        return ten + vietTat;
    }

    /**
     * Generates a unique USER_ prefixed username using UUID.
     */
    public static String taoTenTaiKhoanUuid() {
        return "USER_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }


    /**
     * Parses a date string (yyyy-MM-dd) to a Long timestamp (epoch milliseconds).
     * @param dateStr The date string to parse.
     * @param endOfDay If true, returns the last millisecond of the day.
     */
    public static Long parseDateToLong(String dateStr, boolean endOfDay) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr.trim());
            java.time.ZoneId zone = java.time.ZoneId.systemDefault();
            if (endOfDay) {
                return date.atTime(23, 59, 59, 999).atZone(zone).toInstant().toEpochMilli();
            }
            return date.atStartOfDay(zone).toInstant().toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }
}
