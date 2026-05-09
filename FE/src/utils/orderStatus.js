export const ORDER_STATUS = Object.freeze({
    CHUA_XAC_NHAN: 0,
    CHO_XAC_NHAN: 1,
    XAC_NHAN: 2,
    DANG_GIAO: 3,
    HOAN_THANH: 4,
    DA_HUY: 5,
    HOAN_DON: 6
});

const toEnumKey = (value) => {
    if (value === null || value === undefined || value === '') return null;

    if (typeof value === 'number' && Number.isFinite(value)) {
        switch (value) {
            case 0: return 'CHUA_XAC_NHAN';
            case 1: return 'CHO_XAC_NHAN';
            case 2: return 'XAC_NHAN';
            case 3: return 'DANG_GIAO';
            case 4: return 'HOAN_THANH';
            case 5: return 'DA_HUY';
            case 6: return 'HOAN_DON';
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
        case 'CHUA_XAC_NHAN':
            return {
                key,
                ordinal: 0,
                text: 'Chưa xác nhận',
                color: '#A52A2A',
                chipClass: 'status-chip-unconfirmed',
                icon: 'mdi-help-circle-outline'
            };
        case 'CHO_XAC_NHAN':
            return {
                key,
                ordinal: 1,
                text: 'Chờ xác nhận',
                color: 'orange-darken-3',
                chipClass: 'status-chip-pending',
                icon: 'mdi-progress-clock'
            };
        case 'XAC_NHAN':
            return {
                key,
                ordinal: 2,
                text: 'Đã xác nhận',
                color: 'light-blue-darken-3',
                chipClass: 'status-chip-confirmed',
                icon: 'mdi-check-circle-outline'
            };
        case 'DANG_GIAO':
            return {
                key,
                ordinal: 3,
                text: 'Đang giao',
                color: 'cyan-darken-3',
                chipClass: 'status-chip-delivering',
                icon: 'mdi-truck-fast-outline'
            };
        case 'HOAN_THANH':
            return {
                key,
                ordinal: 4,
                text: 'Đã hoàn thành',
                color: 'green-darken-3',
                chipClass: 'status-chip-completed',
                icon: 'mdi-checkbox-marked-circle-outline'
            };
        case 'DA_HUY':
            return {
                key,
                ordinal: 5,
                text: 'Hủy',
                color: 'error',
                chipClass: 'status-chip-cancelled',
                icon: 'mdi-close-circle-outline'
            };
        case 'HOAN_DON':
            return {
                key,
                ordinal: 6,
                text: 'Hoàn đơn',
                color: 'deep-purple',
                chipClass: 'status-chip-refunded',
                icon: 'mdi-cash-refund'
            };
        default:
            return null;
    }
};

export const getOrderStatusOrdinal = (value) => getOrderStatusMeta(value)?.ordinal ?? null;
