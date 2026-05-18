/**
 * Frontend utility for generating suggested codes.
 * Matches the logic in Backend CodeUtils.
 */

const PREFIXES = {
    SanPham: 'SP',
    ChiTietSanPham: 'CT',
    HoaDon: 'HD',
    NhanVien: 'NV',
    KhachHang: 'KH',
    PhieuGiamGia: 'PGG',
    DotGiamGia: 'DG'
};

const CHARS = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ';

/**
 * Generates a random code similar to backend.
 */
export const generateRandomCode = (type) => {
    const prefix = PREFIXES[type] || 'MA';
    let result = prefix;
    for (let i = 0; i < 8; i++) {
        result += CHARS.charAt(Math.floor(Math.random() * CHARS.length));
    }
    return result;
};

/**
 * Suggests a SKU-like code for product variants.
 */
export const suggestVariantCode = (productCode, colorCode, sizeName) => {
    if (!productCode) return '';
    const parts = [productCode];
    if (colorCode) parts.push(colorCode);
    if (sizeName) parts.push(sizeName.toString().replace(/\s+/g, ''));
    return parts.join('-').toUpperCase();
};
