/**
 * Variant Utility Functions
 * Shared helper functions for variant/product management
 */

/**
 * Normalize uploaded file URL from various response formats
 * @param {string|object} value - URL or object containing URL
 * @returns {string} Normalized URL
 */
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

/**
 * Get nested value from object using array of keys
 * @param {object} source - Source object
 * @param {string[]} keys - Array of keys to try
 * @returns {any} First non-null/undefined/empty value found
 */
export const getNestedValue = (source, keys) => {
    for (const key of keys) {
        const value = source?.[key];
        if (value !== null && value !== undefined && value !== '') {
            return value;
        }
    }
    return '';
};

/**
 * Normalize search text for comparison (remove accents, lowercase, trim)
 * @param {string} value - Text to normalize
 * @returns {string} Normalized text
 */
export const normalizeSearchText = (value) =>
    String(value ?? '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .trim();

/**
 * Combobox filter function for v-combobox custom-filter
 * @param {string} itemTitle - Item title
 * @param {string} queryText - Search query
 * @param {object} item - Item object
 * @returns {boolean} Whether item matches query
 */
export const comboboxFilter = (itemTitle, queryText, item) => {
    const normalizedQuery = normalizeSearchText(queryText);
    if (!normalizedQuery) {
        return true;
    }

    const normalizedTitle = normalizeSearchText(item?.raw?.ten || itemTitle || item?.value);
    return normalizedTitle.includes(normalizedQuery);
};

/**
 * Standard combobox props for consistent behavior
 */
export const comboboxProps = {
    clearable: true,
    autoSelectFirst: false
};

// ─── Variant field extractors ─────────────────────────────────────────────────

/**
 * Extract SKU value from variant with various field name fallbacks
 * @param {object} variant
 * @returns {string}
 */
export const getVariantSkuValue = (variant = {}) =>
    variant.maChiTietSanPham || variant.sku || variant.maSku || variant.maBienThe || variant.ma || '';

/**
 * Extract color ID from variant with various nested structures
 * @param {object} variant
 * @returns {string}
 */
export const getVariantColorIdValue = (variant = {}) =>
    getNestedValue(variant, ['idMauSac', 'mauSacId']) ||
    getNestedValue(variant?.mauSac, ['id', 'value', 'ma']) ||
    getNestedValue(variant?.color, ['id', 'value', 'ma']) ||
    '';

/**
 * Extract color label from variant with various nested structures
 * @param {object} variant
 * @returns {string}
 */
export const getVariantColorLabelValue = (variant = {}) =>
    getNestedValue(variant, ['tenMauSac', 'mauSac', 'mau']) ||
    getNestedValue(variant?.mauSac, ['ten', 'name', 'label', 'title']) ||
    getNestedValue(variant?.color, ['ten', 'name', 'label', 'title']) ||
    '';

/**
 * Extract size ID from variant with various nested structures
 * @param {object} variant
 * @returns {string}
 */
export const getVariantSizeIdValue = (variant = {}) =>
    getNestedValue(variant, ['idKichThuoc', 'kichThuocId', 'sizeId']) ||
    getNestedValue(variant?.kichThuoc, ['id', 'value', 'ma']) ||
    getNestedValue(variant?.size, ['id', 'value', 'ma']) ||
    '';

/**
 * Extract size label from variant with various nested structures
 * @param {object} variant
 * @returns {string}
 */
export const getVariantSizeLabelValue = (variant = {}) =>
    getNestedValue(variant, ['tenKichThuoc', 'kichThuoc', 'size']) ||
    getNestedValue(variant?.kichThuoc, ['ten', 'name', 'label', 'title', 'giaTriKichThuoc']) ||
    getNestedValue(variant?.size, ['ten', 'name', 'label', 'title']) ||
    '';

/**
 * Get variant thumbnail URL from various possible image structures
 * Covers: urlAnh, images[], hinhAnhs[], anhChiTietSanPhams[], hinhAnh (string/array)
 * @param {object} variant - Variant object
 * @param {string} [fallback=''] - Fallback URL if no image found
 * @returns {string} Image URL
 */
export const getVariantThumbnailUrl = (variant, fallback = '') => {
    if (!variant) return fallback;

    return (
        normalizeUploadedFileUrl(
            variant.urlAnh ||
            variant.images?.find((img) => img?.hinhAnhDaiDien || img?.anhDaiDien || img?.laAnhChinh)?.duongDanAnh ||
            variant.images?.[0]?.duongDanAnh ||
            variant.hinhAnhs?.find((img) => img?.hinhAnhDaiDien)?.duongDanAnh ||
            variant.hinhAnhs?.[0]?.duongDanAnh ||
            variant.anhChiTietSanPhams?.find((img) => img?.hinhAnhDaiDien)?.duongDanAnh ||
            variant.anhChiTietSanPhams?.[0]?.duongDanAnh ||
            variant.hinhAnh?.[0]?.duongDanAnh ||
            variant.hinhAnh?.[0]?.url ||
            variant.hinhAnh ||
            variant.duongDanAnh ||
            variant.imageUrl ||
            variant.anh
        ) || fallback
    );
};

/**
 * Generate the next SKU suffix for a product
 * @param {string} prefix - Product code prefix (e.g. "SP001")
 * @param {string[]} existingSkus - Array of existing SKU strings
 * @returns {string} Next SKU (e.g. "SP001-4")
 */
export const generateNextSku = (prefix, existingSkus = []) => {
    if (!prefix) return '';

    const fullPrefix = prefix + '-';
    let maxSuffix = 0;

    existingSkus.forEach((sku) => {
        if (sku && sku.startsWith(fullPrefix)) {
            const parts = sku.split('-');
            const num = parseInt(parts[parts.length - 1], 10);
            if (!isNaN(num) && num > maxSuffix) {
                maxSuffix = num;
            }
        }
    });

    return `${prefix}-${maxSuffix + 1}`;
};
