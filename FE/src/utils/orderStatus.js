import {
    ORDER_STATUS,
    ORDER_STATUS_ORDINALS,
    ORDER_STATUS_LABELS,
    ORDER_STATUS_COLORS,
    ORDER_STATUS_CHIP_CLASSES,
    ORDER_STATUS_ICONS
} from '@/constants/hoaDonConstants';

const toEnumKey = (value) => {
    if (value === null || value === undefined || value === '') return null;

    if (typeof value === 'number' && Number.isFinite(value)) {
        const entries = Object.entries(ORDER_STATUS_ORDINALS);
        const match = entries.find(([_, val]) => val === value);
        return match ? match[0] : null;
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

    return {
        key,
        ordinal: ORDER_STATUS_ORDINALS[key],
        text: ORDER_STATUS_LABELS[key],
        color: ORDER_STATUS_COLORS[key],
        chipClass: ORDER_STATUS_CHIP_CLASSES[key],
        icon: ORDER_STATUS_ICONS[key]
    };
};

export const getOrderStatusOrdinal = (value) => getOrderStatusMeta(value)?.ordinal ?? null;
