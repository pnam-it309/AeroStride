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

export const formatNumberWithDots = (num) => {
    if (num === null || num === undefined || num === '') return '';
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
};

export const parseNumberFromDots = (str) => {
    if (!str) return 0;
    const parsed = parseInt(str.toString().replace(/\./g, ''), 10);
    return isNaN(parsed) ? 0 : parsed;
};

export const formatShortAmount = (num) => {
    if (!num) return '0';
    if (num >= 1000000) return (num / 1000000).toFixed(0) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(0) + 'K';
    return num.toString();
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

export const readMoneyInVietnameseWords = (number) => {
    const num = Math.abs(Number(number || 0));
    if (num === 0) return 'Không đồng';

    const units = ['', ' nghìn', ' triệu', ' tỷ', ' nghìn tỷ', ' triệu tỷ'];
    const digits = ['không', 'một', 'hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín'];

    const readThreeDigits = (n, showZeroHundred) => {
        let hundred = Math.floor(n / 100);
        let ten = Math.floor((n % 100) / 10);
        let unit = n % 10;
        let res = '';

        if (hundred > 0 || showZeroHundred) {
            res += digits[hundred] + ' trăm ';
        }

        if (ten > 1) {
            res += digits[ten] + ' mươi ';
        } else if (ten === 1) {
            res += 'mười ';
        } else if (hundred > 0 && unit > 0) {
            res += 'lẻ ';
        }

        if (unit > 0) {
            if (unit === 1 && ten > 1) {
                res += 'mốt';
            } else if (unit === 5 && ten > 0) {
                res += 'lăm';
            } else {
                res += digits[unit];
            }
        }
        return res.trim();
    };

    let result = '';
    let temp = num;
    let unitIdx = 0;

    while (temp > 0) {
        let chunk = temp % 1000;
        if (chunk > 0) {
            let chunkText = readThreeDigits(chunk, temp >= 1000);
            result = chunkText + units[unitIdx] + (result ? ' ' + result : '');
        }
        temp = Math.floor(temp / 1000);
        unitIdx++;
    }

    result = result.trim();
    if (result) {
        result = result.charAt(0).toUpperCase() + result.slice(1) + ' đồng';
    }
    return number < 0 ? 'Âm ' + result.toLowerCase() : result;
};
