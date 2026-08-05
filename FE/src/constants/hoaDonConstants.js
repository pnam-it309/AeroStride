/**
 * HOA_DON_CONSTANTS - Các hằng số cho module Hóa đơn (Đồng bộ với BE OrderStatus)
 */

export const ORDER_STATUS = {
    CHO_XAC_NHAN: 'CHO_XAC_NHAN',
    XAC_NHAN: 'XAC_NHAN',
    CHO_GIAO: 'CHO_GIAO',
    DANG_GIAO: 'DANG_GIAO',
    HOAN_THANH: 'HOAN_THANH',
    DA_HUY: 'DA_HUY',
    HOAN_DON: 'HOAN_DON',
    GIAO_THAT_BAI: 'GIAO_THAT_BAI',
    KHACH_KHONG_NHAN: 'KHACH_KHONG_NHAN'
};

export const ORDER_STATUS_ORDINALS = {
    [ORDER_STATUS.CHO_XAC_NHAN]: 0,
    [ORDER_STATUS.XAC_NHAN]: 1,
    [ORDER_STATUS.CHO_GIAO]: 2,
    [ORDER_STATUS.DANG_GIAO]: 3,
    [ORDER_STATUS.HOAN_THANH]: 4,
    [ORDER_STATUS.DA_HUY]: 5,
    [ORDER_STATUS.HOAN_DON]: 6,
    [ORDER_STATUS.GIAO_THAT_BAI]: 7,
    [ORDER_STATUS.KHACH_KHONG_NHAN]: 8
};

export const ORDER_STATUS_LABELS = {
    [ORDER_STATUS.CHO_XAC_NHAN]: 'Chờ xác nhận',
    [ORDER_STATUS.XAC_NHAN]: 'Đã xác nhận',
    [ORDER_STATUS.CHO_GIAO]: 'Chờ giao hàng',
    [ORDER_STATUS.DANG_GIAO]: 'Đang giao hàng',
    [ORDER_STATUS.HOAN_THANH]: 'Hoàn thành',
    [ORDER_STATUS.DA_HUY]: 'Đã hủy',
    [ORDER_STATUS.HOAN_DON]: 'Hoàn đơn',
    [ORDER_STATUS.GIAO_THAT_BAI]: 'Giao thất bại',
    [ORDER_STATUS.KHACH_KHONG_NHAN]: 'Khách không nhận'
};

export const ORDER_STATUS_COLORS = {
    [ORDER_STATUS.CHO_XAC_NHAN]: '#c2410c', // Cam đậm
    [ORDER_STATUS.XAC_NHAN]: '#1e257c', // Xanh navy đậm
    [ORDER_STATUS.CHO_GIAO]: '#9c2a1a', // Đỏ gạch
    [ORDER_STATUS.DANG_GIAO]: '#1e40af', // Xanh lam
    [ORDER_STATUS.HOAN_THANH]: '#166534', // Xanh lá
    [ORDER_STATUS.DA_HUY]: '#991b1b', // Đỏ đậm
    [ORDER_STATUS.HOAN_DON]: '#5b21b6', // Tím
    [ORDER_STATUS.GIAO_THAT_BAI]: '#d97706', // Hổ phách
    [ORDER_STATUS.KHACH_KHONG_NHAN]: '#dc2626' // Đỏ tươi
};

export const ORDER_STATUS_CHIP_CLASSES = {
    [ORDER_STATUS.CHO_XAC_NHAN]: 'status-chip-pending',
    [ORDER_STATUS.XAC_NHAN]: 'status-chip-confirmed',
    [ORDER_STATUS.CHO_GIAO]: 'status-chip-waiting-delivery',
    [ORDER_STATUS.DANG_GIAO]: 'status-chip-delivering',
    [ORDER_STATUS.HOAN_THANH]: 'status-chip-completed',
    [ORDER_STATUS.DA_HUY]: 'status-chip-cancelled',
    [ORDER_STATUS.HOAN_DON]: 'status-chip-refunded',
    [ORDER_STATUS.GIAO_THAT_BAI]: 'status-chip-failed',
    [ORDER_STATUS.KHACH_KHONG_NHAN]: 'status-chip-refused'
};

export const ORDER_STATUS_ICONS = {
    [ORDER_STATUS.CHO_XAC_NHAN]: 'mdi-progress-clock',
    [ORDER_STATUS.XAC_NHAN]: 'mdi-check-circle-outline',
    [ORDER_STATUS.CHO_GIAO]: 'mdi-package-variant-closed',
    [ORDER_STATUS.DANG_GIAO]: 'mdi-truck-fast-outline',
    [ORDER_STATUS.HOAN_THANH]: 'mdi-checkbox-marked-circle-outline',
    [ORDER_STATUS.DA_HUY]: 'mdi-close-circle-outline',
    [ORDER_STATUS.HOAN_DON]: 'mdi-cash-refund',
    [ORDER_STATUS.GIAO_THAT_BAI]: 'mdi-truck-alert-outline',
    [ORDER_STATUS.KHACH_KHONG_NHAN]: 'mdi-account-cancel-outline'
};

export const ORDER_STATUS_FILTER_OPTIONS = [
    { title: 'Tất cả trạng thái', value: null },
    { title: 'Chờ xác nhận', value: ORDER_STATUS.CHO_XAC_NHAN },
    { title: 'Đã xác nhận', value: ORDER_STATUS.XAC_NHAN },
    { title: 'Chờ giao hàng', value: ORDER_STATUS.CHO_GIAO },
    { title: 'Đang giao hàng', value: ORDER_STATUS.DANG_GIAO },
    { title: 'Hoàn thành', value: ORDER_STATUS.HOAN_THANH },
    { title: 'Giao thất bại', value: ORDER_STATUS.GIAO_THAT_BAI },
    { title: 'Khách không nhận', value: ORDER_STATUS.KHACH_KHONG_NHAN },
    { title: 'Đã hủy', value: ORDER_STATUS.DA_HUY },
    { title: 'Hoàn đơn', value: ORDER_STATUS.HOAN_DON }
];
