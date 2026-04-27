export const ORDER_STATUS = Object.freeze({
    CHO_THANH_TOAN: 0,
    CHO_XAC_NHAN: 1,
    CHO_GIAO_HANG: 2,
    DANG_VAN_CHUYEN: 3,
    DA_GIAO_HANG: 4,
    DA_THANH_TOAN: 5,
    HOAN_THANH: 6,
    DA_HUY: 7
});

const toEnumKey = (value) => {
    if (value === null || value === undefined || value === '') return null;

    if (typeof value === 'number' && Number.isFinite(value)) {
        switch (value) {
            case 0: return 'CHO_THANH_TOAN';
            case 1: return 'CHO_XAC_NHAN';
            case 2: return 'CHO_GIAO_HANG';
            case 3: return 'DANG_VAN_CHUYEN';
            case 4: return 'DA_GIAO_HANG';
            case 5: return 'DA_THANH_TOAN';
            case 6: return 'HOAN_THANH';
            case 7: return 'DA_HUY';
            default: return null;
        }
    }

    const s = String(value).trim().toUpperCase();
    if (s in ORDER_STATUS) return s;

    const numericValue = Number(s);
    if (Number.isFinite(numericValue)) return toEnumKey(numericValue);

    return null;
};

export const getOrderStatusMeta = (value) => {
    const key = toEnumKey(value);
    if (!key) return null;

    switch (key) {
        case 'CHO_THANH_TOAN':
            return {
                key,
                ordinal: 0,
                text: 'Chờ thanh toán',
                color: 'warning',
                chipClass: 'status-chip-pending',
                icon: 'mdi-cash-clock'
            };
        case 'CHO_XAC_NHAN':
            return {
                key,
                ordinal: 1,
                text: 'Chờ xác nhận',
                color: 'info',
                chipClass: 'status-chip-confirmed',
                icon: 'mdi-progress-clock'
            };
        case 'CHO_GIAO_HANG':
            return {
                key,
                ordinal: 2,
                text: 'Chờ giao hàng',
                color: 'primary',
                chipClass: 'status-chip-waiting-delivery',
                icon: 'mdi-truck-delivery-outline'
            };
        case 'DANG_VAN_CHUYEN':
            return {
                key,
                ordinal: 3,
                text: 'Đang vận chuyển',
                color: 'cyan',
                chipClass: 'status-chip-delivering',
                icon: 'mdi-truck-fast-outline'
            };
        case 'DA_GIAO_HANG':
            return {
                key,
                ordinal: 4,
                text: 'Đã giao hàng',
                color: 'indigo',
                chipClass: 'status-chip-delivered',
                icon: 'mdi-package-variant-closed-check'
            };
        case 'DA_THANH_TOAN':
            return {
                key,
                ordinal: 5,
                text: 'Đã thanh toán',
                color: 'teal',
                chipClass: 'status-chip-paid',
                icon: 'mdi-currency-usd'
            };
        case 'HOAN_THANH':
            return {
                key,
                ordinal: 6,
                text: 'Hoàn thành',
                color: 'success',
                chipClass: 'status-chip-completed',
                icon: 'mdi-checkbox-marked-circle-outline'
            };
        case 'DA_HUY':
            return {
                key,
                ordinal: 7,
                text: 'Đã hủy',
                color: 'error',
                chipClass: 'status-chip-cancelled',
                icon: 'mdi-close-circle-outline'
            };
        default:
            return null;
    }
};

export const getOrderStatusOrdinal = (value) => getOrderStatusMeta(value)?.ordinal ?? null;

