/**
 * Common validation rules for Vuetify inputs and programmatic checks.
 */

// Bỏ các ký tự đặc biệt, chỉ cho phép chữ (bao gồm Unicode tiếng Việt), số và khoảng trắng
export const noSpecialChar = (value) => {
    if (!value) return true;
    return /^[\p{L}0-9\s]+$/u.test(value) || 'Không được chứa ký tự đặc biệt';
};

// Kiểm tra độ dài từ 3 đến 255 ký tự
export const lengthBetween3And255 = (value) => {
    if (!value) return true;
    const len = String(value).trim().length;
    return (len >= 3 && len <= 255) || 'Độ dài phải từ 3 đến 255 ký tự';
};

// Đảm bảo tương thích ngược nhưng đồng bộ chuẩn 3 đến 255 ký tự
export const lengthBetween2And255 = lengthBetween3And255;

// Kiểm tra không chứa khoảng trắng ở 2 đầu (Đã bỏ chặn)
export const noOuterWhitespace = () => true;

// Yêu cầu bắt buộc nhập
export const required =
    (fieldName = 'Trường này') =>
    (value) => {
        if (value === null || value === undefined || value === '' || (typeof value === 'string' && !value.trim())) {
            return fieldName === 'Trường này' ? 'Trường này là bắt buộc' : `Vui lòng nhập ${fieldName.toLowerCase()}`;
        }
        return true;
    };

// Tổ hợp các rule chuẩn cho tên (bắt buộc, 3-255 ký tự, không ký tự đặc biệt)
export const getNameRules = (fieldName = 'Trường này') => [required(fieldName), lengthBetween3And255, noSpecialChar];
