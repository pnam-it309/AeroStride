package com.example.be.utils;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Centralized utility for generating codes (Sequential or Random).
 */
public final class CodeUtils {

    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeUtils() {}

    /**
     * Generates a RANDOM code with prefix based on Class.
     * Example: SanPham.class -> SPX7Y2Z
     */
    public static String generateRandom(Class<?> clazz) {
        String prefix = getPrefix(clazz);
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 8; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a SEQUENTIAL code.
     * Example: prefix="NV", existing=["NV01"] -> "NV02"
     */
    public static String generateSequential(String prefix, List<String> existingCodes) {
        if (existingCodes == null || existingCodes.isEmpty()) {
            return String.format("%s01", prefix);
        }
        int max = 0;
        int prefixLen = prefix.length();
        for (String ma : existingCodes) {
            if (ma != null && ma.startsWith(prefix)) {
                try {
                    int so = Integer.parseInt(ma.substring(prefixLen).trim());
                    if (so > max) max = so;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%02d", prefix, max + 1);
    }

    public static String generateUuidShort() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String getPrefix(Class<?> clazz) {
        String className = clazz.getSimpleName();
        return switch (className) {
            case "SanPham" -> "SP";
            case "ChiTietSanPham" -> "CT";
            case "HoaDon" -> "HD";
            case "NhanVien" -> "NV";
            case "KhachHang" -> "KH";
            case "PhieuGiamGia" -> "PG";
            case "DotGiamGia" -> "DG";
            default -> "MA";
        };
    }
}
