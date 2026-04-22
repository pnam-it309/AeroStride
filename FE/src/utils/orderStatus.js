export const ORDER_STATUS = Object.freeze({
    PENDING_PAYMENT: 0,
    PROCESSING: 1,
    SHIPPED: 2,
    DELIVERED: 3,
    CANCELLED: 4,
    REFUNDED: 5
});

const toEnumKey = (value) => {
    if (value === null || value === undefined || value === '') return null;

    if (typeof value === 'number' && Number.isFinite(value)) {
        switch (value) {
            case 0:
                return 'PENDING_PAYMENT';
            case 1:
                return 'PROCESSING';
            case 2:
                return 'SHIPPED';
            case 3:
                return 'DELIVERED';
            case 4:
                return 'CANCELLED';
            case 5:
                return 'REFUNDED';
            default:
                return null;
        }
    }

    const s = String(value).trim().toUpperCase();

    // Support BE enum name or FE legacy numeric strings
    if (s in ORDER_STATUS) return s;

    const numericValue = Number(s);
    if (Number.isFinite(numericValue)) return toEnumKey(numericValue);

    return null;
};

export const getOrderStatusMeta = (value) => {
    const key = toEnumKey(value);
    if (!key) return null;

    switch (key) {
        case 'PENDING_PAYMENT':
            return {
                key,
                ordinal: 0,
                text: 'Chờ thanh toán',
                color: 'warning',
                chipClass: 'status-chip-pending',
                icon: 'mdi-cash-clock'
            };
        case 'PROCESSING':
            return {
                key,
                ordinal: 1,
                text: 'Đang xử lý',
                color: 'info',
                chipClass: 'status-chip-confirmed',
                icon: 'mdi-progress-clock'
            };
        case 'SHIPPED':
            return {
                key,
                ordinal: 2,
                text: 'Đã gửi hàng',
                color: 'primary',
                chipClass: 'status-chip-waiting-delivery',
                icon: 'mdi-truck-fast-outline'
            };
        case 'DELIVERED':
            return {
                key,
                ordinal: 3,
                text: 'Đã giao',
                color: 'success',
                chipClass: 'status-chip-completed',
                icon: 'mdi-checkbox-marked-circle-outline'
            };
        case 'CANCELLED':
            return {
                key,
                ordinal: 4,
                text: 'Đã hủy',
                color: 'error',
                chipClass: 'status-chip-cancelled',
                icon: 'mdi-close-circle-outline'
            };
        case 'REFUNDED':
            return {
                key,
                ordinal: 5,
                text: 'Hoàn tiền',
                color: 'orange',
                chipClass: 'status-chip-delivering',
                icon: 'mdi-cash-refund'
            };
        default:
            return null;
    }
};

export const getOrderStatusOrdinal = (value) => getOrderStatusMeta(value)?.ordinal ?? null;

