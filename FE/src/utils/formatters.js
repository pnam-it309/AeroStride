import { format } from 'date-fns';
import { vi } from 'date-fns/locale';

export const formatCurrency = (value) => {
    if (isNaN(value)) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
    }).format(value);
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
