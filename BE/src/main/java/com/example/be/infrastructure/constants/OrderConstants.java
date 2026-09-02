package com.example.be.infrastructure.constants;

/**
 * Hằng số cho phân loại đơn hàng và phương thức nhận hàng.
 */
public final class OrderConstants {

    private OrderConstants() {
        // Prevent instantiation
    }

    // Legacy loai_don values
    public static final String LOAI_DON_TAI_QUAY = "TAI_QUAY";
    public static final String LOAI_DON_GIAO_HANG = "GIAO_HANG";
    public static final String LOAI_DON_ONLINE = "ONLINE";
    public static final String LOAI_DON_OFFLINE = "OFFLINE";

    // Text descriptions
    public static final String DESC_SHIP_AT_STORE = "Xác nhận đơn giao hàng tại quầy";
    public static final String DESC_PAY_AT_STORE = "Thanh toán tại quầy thành công";
    public static final String DESC_ONLINE_ORDER = "Khách hàng đặt hàng trực tuyến";
    public static final String DESC_VNPAY_PAID = "Thanh toán thành công qua VNPay";
}
