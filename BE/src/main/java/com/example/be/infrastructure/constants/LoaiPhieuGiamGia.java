package com.example.be.infrastructure.constants;

public enum LoaiPhieuGiamGia {
    PHAN_TRAM,
    TIEN_MAT;

    public static boolean isPhanTram(String loaiPhieu) {
        if (loaiPhieu == null || loaiPhieu.isBlank()) {
            return false;
        }
        String normalized = loaiPhieu.trim().toUpperCase();
        return PHAN_TRAM.name().equals(normalized)
                || "PERCENTAGE".equals(normalized)
                || "PERCENT".equals(normalized)
                || "PHẦN TRĂM".equals(normalized);
    }

    public static boolean isTienMat(String loaiPhieu) {
        if (loaiPhieu == null || loaiPhieu.isBlank()) {
            return false;
        }
        String normalized = loaiPhieu.trim().toUpperCase();
        return TIEN_MAT.name().equals(normalized)
                || "FIXED_AMOUNT".equals(normalized)
                || "FIXED".equals(normalized)
                || "TIỀN MẶT".equals(normalized);
    }
}
