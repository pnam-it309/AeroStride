package com.example.be.infrastructure.constants;

public enum OrderStatus {
    CHO_XAC_NHAN,     // 0: Chờ xác nhận
    XAC_NHAN,         // 1: Đã xác nhận
    CHO_GIAO,         // 2: Chờ giao
    DANG_GIAO,        // 3: Đang giao
    HOAN_THANH,       // 4: Đã hoàn thành
    DA_HUY,           // 5: Hủy
    HOAN_DON,         // 6: Hoàn đơn
    GIAO_THAT_BAI,    // 7: Giao hàng thất bại
    KHACH_KHONG_NHAN; // 8: Khách không nhận

    public boolean canTransitionTo(OrderStatus nextStatus) {
        if (this == nextStatus) return true;
        
        return switch (this) {
            case CHO_XAC_NHAN -> nextStatus == XAC_NHAN || nextStatus == DA_HUY;
            case XAC_NHAN -> nextStatus == CHO_GIAO || nextStatus == DA_HUY;
            case CHO_GIAO -> nextStatus == DANG_GIAO || nextStatus == DA_HUY;
            case DANG_GIAO -> nextStatus == HOAN_THANH || nextStatus == GIAO_THAT_BAI || nextStatus == KHACH_KHONG_NHAN || nextStatus == HOAN_DON;
            case GIAO_THAT_BAI, KHACH_KHONG_NHAN -> nextStatus == HOAN_DON || nextStatus == HOAN_THANH;
            case HOAN_THANH -> nextStatus == HOAN_DON;
            case DA_HUY, HOAN_DON -> false;
        };
    }
}

