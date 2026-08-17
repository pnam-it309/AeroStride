package com.example.be.infrastructure.constants;

import com.example.be.entity.GiaoDichThanhToan;
import com.example.be.entity.HoaDon;

/**
 * Hằng số và tiện ích xử lý phương thức / loại giao dịch thanh toán.
 */
public final class PaymentConstants {

    private PaymentConstants() {
        // Prevent instantiation
    }

    // Transaction & Payment Method Types
    public static final String METHOD_COD = "COD";
    public static final String METHOD_TIEN_MAT = "TIEN_MAT";
    public static final String METHOD_VNPAY = "VNPAY";
    public static final String METHOD_CHUYEN_KHOAN = "CHUYEN_KHOAN";
    public static final String METHOD_ONLINE = "ONLINE";
    public static final String TYPE_THANH_TOAN = "THANH_TOAN";

    /**
     * Kiểm tra xem một chuỗi có đại diện cho phương thức tiền mặt / COD hay không.
     */
    public static boolean isCashOrCod(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toUpperCase();
        return normalized.contains(METHOD_COD)
                || normalized.contains(METHOD_TIEN_MAT)
                || normalized.contains("TIỀN MẶT");
    }

    /**
     * Kiểm tra xem một bản ghi giao dịch có phải là tiền mặt / COD hay không.
     */
    public static boolean isCashOrCod(GiaoDichThanhToan gd) {
        if (gd == null) {
            return false;
        }
        if (isCashOrCod(gd.getLoaiGiaoDich())) {
            return true;
        }
        if (gd.getPhuongThucThanhToan() != null && isCashOrCod(gd.getPhuongThucThanhToan().getTen())) {
            return true;
        }
        return false;
    }

    /**
     * Kiểm tra xem đơn hàng có bất kỳ giao dịch thanh toán tiền mặt (COD) nào hay không.
     */
    public static boolean isCashOrder(HoaDon hoaDon) {
        if (hoaDon == null || hoaDon.getListsGiaoDichThanhToan() == null || hoaDon.getListsGiaoDichThanhToan().isEmpty()) {
            return false;
        }
        for (GiaoDichThanhToan gd : hoaDon.getListsGiaoDichThanhToan()) {
            if (isCashOrCod(gd)) {
                return true;
            }
        }
        return false;
    }
}
