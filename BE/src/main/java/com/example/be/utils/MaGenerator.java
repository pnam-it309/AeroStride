package com.example.be.utils;

import java.util.Random;

public final class MaGenerator {
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 8;
    private static final Random RANDOM = new Random();

    private MaGenerator() {}

    public static String generate(Class<?> clazz) {
        String prefix = getPrefix(clazz);
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private static String getPrefix(Class<?> clazz) {
        String className = clazz.getSimpleName();
        return switch (className) {
            case "SanPham" -> "SP";
            case "ChiTietSanPham" -> "CT";
            case "HoaDon" -> "HD";
            case "NhanVien" -> "NV";
            case "KhachHang" -> "KH";
            case "PhanQuyen" -> "PQ";
            case "DotGiamGia" -> "DG";
            case "PhieuGiamGia" -> "PG";
            case "ThuongHieu" -> "TH";
            case "DanhMuc" -> "DM";
            case "KichThuoc" -> "KT";
            case "MauSac" -> "MS";
            case "ChatLieu" -> "CL";
            case "DeGiay" -> "DE";
            case "CoGiay" -> "CG";
            case "FormDang" -> "FD";
            case "GioHang" -> "GH";
            case "HinhThucThanhToan" -> "HT";
            default -> "MA";
        };
    }
}
