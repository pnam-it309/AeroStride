package com.example.be.infrastructure.constants;

public enum HinhThucPhieuGiamGia {
    CONG_KHAI,
    CA_NHAN;

    public static boolean isCongKhai(String hinhThuc) {
        if (hinhThuc == null || hinhThuc.isBlank()) {
            return true; // Mặc định nếu chưa gán hình thức thì coi là công khai
        }
        String normalized = hinhThuc.trim().toUpperCase();
        return CONG_KHAI.name().equals(normalized) || "CÔNG KHAI".equals(normalized);
    }

    public static boolean isCaNhan(String hinhThuc) {
        if (hinhThuc == null || hinhThuc.isBlank()) {
            return false;
        }
        String normalized = hinhThuc.trim().toUpperCase();
        return CA_NHAN.name().equals(normalized) || "CÁ NHÂN".equals(normalized);
    }
}
