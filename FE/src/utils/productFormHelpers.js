/**
 * Các hàm THUẦN (không đụng reactivity) dùng chung cho form sản phẩm / biến thể.
 * Tách ra từ SanPhamForm.vue để giảm bulk và tái sử dụng. Giữ nguyên hành vi gốc.
 */

// Bỏ dấu tiếng Việt
export const removeAccents = (str) => (str ? str.normalize('NFD').replace(/[̀-ͯ]/g, '') : '');

// Chuẩn hóa chuỗi để tìm kiếm (bỏ dấu, thường hóa, trim)
export const normalizeSearchText = (value) =>
    String(value ?? '')
        .normalize('NFD')
        .replace(/[̀-ͯ]/g, '')
        .toLowerCase()
        .trim();

// Làm sạch chuỗi kích thước: bỏ HTML, ký tự lạ, các từ khóa thừa ("size", "sz"...)
export const cleanSizeString = (str) => {
    let s = (str || '').toString();
    // Bảo mật: loại bỏ HTML tags để chống XSS
    s = s.replace(/<[^>]*>?/gm, '');
    // Chỉ giữ chữ, số, khoảng trắng và . , -
    s = s.replace(/[^a-zA-Z0-9.,\-\s]/g, '');
    // Bỏ các từ khóa thừa
    s = s.replace(/(?:^|\s)(?:kích thước|size|sz)\s*/gi, '');
    // Bỏ chữ "s" đứng ngay trước số (s43 -> 43)
    s = s.replace(/(?:^|\s)s\s*(?=\d)/gi, '');
    // Gộp khoảng trắng thừa
    return s.replace(/\s+/g, ' ').trim();
};

export const formatSizeDisplay = (name) => {
    const norm = cleanSizeString(name).toUpperCase();
    return norm ? `Size ${norm}` : name || '';
};

// Chỉ cho nhập số nguyên 2 chữ số cho size
export const normalizeSizeInput = (value) =>
    String(value || '')
        .replace(/\D/g, '')
        .slice(0, 2);

export const blockNonNumericSizeInput = (event) => {
    const allowedKeys = ['Backspace', 'Delete', 'Tab', 'Escape', 'Enter', 'ArrowLeft', 'ArrowRight', 'Home', 'End'];
    if (allowedKeys.includes(event.key) || event.ctrlKey || event.metaKey) return;
    if (!/^\d$/.test(event.key)) {
        event.preventDefault();
    }
};

// Tạo danh sách hiển thị cho combobox, chèn mục "thêm nhanh" ở đầu và luôn ưu tiên thuộc tính vừa thêm nhanh lên trên đầu
export const getDisplayItems = (originalItems, query, quickAddedIds) => {
    const trimmedQuery = query?.trim();
    const normalizedQuery = normalizeSearchText(trimmedQuery);

    let filtered = originalItems || [];
    if (normalizedQuery) {
        filtered = originalItems.filter((item) => normalizeSearchText(item.ten).includes(normalizedQuery));
    }

    const isItemQuickAdded = (item) => {
        if (!item) return false;
        if (item.isQuickAdded) return true;
        if (quickAddedIds) {
            if (typeof quickAddedIds.has === 'function') {
                return quickAddedIds.has(item.id);
            }
            if (Array.isArray(quickAddedIds)) {
                return quickAddedIds.includes(item.id);
            }
        }
        return false;
    };

    // Tách các mục đã được thêm nhanh lên trên đầu
    const quickAdded = [];
    const regular = [];
    for (const item of filtered) {
        if (isItemQuickAdded(item)) {
            quickAdded.push(item);
        } else {
            regular.push(item);
        }
    }
    const sortedFiltered = [...quickAdded, ...regular];

    if (!trimmedQuery) return sortedFiltered;

    const existsExact = originalItems.some((item) => normalizeSearchText(item.ten) === normalizedQuery);
    if (existsExact) return sortedFiltered;

    return [{ id: `__new__${trimmedQuery}`, ten: trimmedQuery, isNew: true }, ...sortedFiltered];
};

// Lấy giá trị đầu tiên khác rỗng theo danh sách key ưu tiên
export const getNestedValue = (source, keys) => {
    for (const key of keys) {
        const value = source?.[key];
        if (value !== null && value !== undefined && value !== '') {
            return value;
        }
    }
    return '';
};

export const createDraftKey = () => `draft-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

// Chuẩn hóa URL ảnh trả về từ nhiều dạng response upload khác nhau
export const normalizeUploadedFileUrl = (value) => {
    if (!value) return '';
    if (typeof value === 'string') return value;
    return (
        value.fileUrl ||
        value.url ||
        value.secure_url ||
        value.duongDanAnh ||
        value.duongDan ||
        value.path ||
        value.data ||
        value.hinhAnh ||
        value.anh ||
        ''
    );
};
