package com.example.be.utils;

import com.example.be.infrastructure.annotations.CodePrefix;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Centralized utility for generating codes (Sequential or Random).
 */
public final class CodeUtils {

    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeUtils() {}

    /**
     * Generates a RANDOM code with prefix based on Class @CodePrefix annotation.
     * Example: SanPham.class (@CodePrefix("SP")) -> SPX7Y2Z
     */
    public static String generateRandom(Class<?> clazz) {
        return generateRandom(clazz.getSimpleName());
    }

    /**
     * Generates a RANDOM code with prefix based on String type.
     */
    public static String generateRandom(String type) {
        String prefix = getPrefix(type);
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 8; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a RANDOM code and ensures it is unique using the provided validator.
     */
    public static String generateRandom(Class<?> clazz, Predicate<String> isDuplicate) {
        String code;
        int attempts = 0;
        do {
            code = generateRandom(clazz);
            attempts++;
            if (attempts > 10) { // Safety break
                code += System.currentTimeMillis() % 1000;
            }
        } while (isDuplicate.test(code) && attempts < 20);
        return code;
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
                    String suffix = ma.substring(prefixLen).trim();
                    if (!suffix.isEmpty()) {
                        int so = Integer.parseInt(suffix);
                        if (so > max) max = so;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%02d", prefix, max + 1);
    }

    /**
     * Generates a SEQUENTIAL code based on the current max value.
     */
    public static String generateNext(String prefix, Integer currentMax) {
        int next = (currentMax == null) ? 1 : currentMax + 1;
        return String.format("%s%05d", prefix, next);
    }

    public static String generateUuidShort() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String getPrefix(Class<?> clazz) {
        if (clazz.isAnnotationPresent(CodePrefix.class)) {
            return clazz.getAnnotation(CodePrefix.class).value();
        }
        
        // Fallback to legacy switch for classes not yet annotated
        return getPrefix(clazz.getSimpleName());
    }

    private static String getPrefix(String className) {
        return switch (className) {
            case "SanPham" -> "SP";
            case "ChiTietSanPham" -> "CT";
            case "HoaDon" -> "HD";
            case "NhanVien" -> "NV";
            case "KhachHang" -> "KH";
            case "PhieuGiamGia" -> "PGG";
            case "DotGiamGia" -> "DG";
            case "ThuocTinh" -> "TT";
            case "MauSac" -> "MS";
            case "KichThuoc" -> "KT";
            case "ChatLieu" -> "CL";
            case "KieuDang" -> "KD";
            case "ThuongHieu" -> "TH";
            case "NhaSanXuat" -> "NSX";
            case "DeGiay" -> "DG";
            case "DanhMuc" -> "DM";
            case "CoGiay" -> "CG";
            case "XuatXu" -> "XX";
            case "MucDich" -> "MD";
            default -> "MA";
        };
    }
}
