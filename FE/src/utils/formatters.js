import { format } from 'date-fns';
import { vi } from 'date-fns/locale';

export const formatCurrency = (value) => {
    if (isNaN(value)) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(value);
};

export const formatNumber = (value) => {
    if (value === null || value === undefined || value === '') return '0';
    return new Intl.NumberFormat('vi-VN').format(Number(value));
};

export const formatDate = (date, formatStr = 'dd/MM/yyyy') => {
    if (!date) return '';
    try {
        const d = typeof date === 'number' ? new Date(date) : new Date(date);
        return format(d, formatStr, { locale: vi });
    } catch (e) {
        return '';
    }
};

export const formatDateTime = (date) => {
    return formatDate(date, 'HH:mm dd/MM/yyyy');
};

/**
 * Safely parse a value to number with fallback
 * @param {any} value - Value to parse
 * @param {number} fallback - Fallback if not a finite number
 * @returns {number}
 */
export const toNumber = (value, fallback = 0) => {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
};
