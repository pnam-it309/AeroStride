package com.example.be.infrastructure.constants;

public enum OrderStatus {
    CHUA_XAC_NHAN,  // 0: Chưa xác nhận
    CHO_XAC_NHAN,   // 1: Chờ xác nhận
    XAC_NHAN,       // 2: Đã xác nhận
    DANG_GIAO,      // 3: Đang giao
    HOAN_THANH,     // 4: Đã hoàn thành
    DA_HUY,         // 5: Hủy
    HOAN_DON        // 6: Hoàn đơn
}
