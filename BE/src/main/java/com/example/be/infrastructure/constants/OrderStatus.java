package com.example.be.infrastructure.constants;

public enum OrderStatus {
    CHO_XAC_NHAN,   // 0: Chờ xác nhận
    XAC_NHAN,       // 1: Đã xác nhận
    CHO_GIAO,       // 2: Chờ giao
    DANG_GIAO,      // 3: Đang giao
    HOAN_THANH,     // 4: Đã hoàn thành
    DA_HUY,         // 5: Hủy
    HOAN_DON        // 6: Hoàn đơn
}
