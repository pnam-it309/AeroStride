/**
 * useAddressMapping - Composable for mapping address codes to names
 * Tránh duplicate code trong KhachHang.vue và KhachHangForm.vue
 */
import { ADDRESS_CONSTANTS } from '@/constants/khachHangConstants';

export function useAddressMapping() {
    /**
     * Chuyển đổi address form từ code sang name trước khi gửi lên server
     * @param {Object} form - Address form object with tinh, thanhPho, phuongXa as codes
     * @param {Array} provinces - List of provinces
     * @param {Array} districts - List of districts
     * @param {Array} wards - List of wards
     * @returns {Object} - Payload with names instead of codes
     */
    const mapCodesToNames = (form, provinces, districts, wards) => {
        const payload = { ...form };

        const p = provinces.find((x) => x.code === form.tinh);
        const d = districts.find((x) => x.code === form.thanhPho);
        const w = wards.find((x) => x.code === form.phuongXa);

        if (p) payload.tinh = p.name;
        if (d) payload.thanhPho = d.name;
        if (w) payload.phuongXa = w.name;

        return payload;
    };

    /**
     * Kiểm tra xem address ID có phải là ID giả (legacy) không
     * @param {String} id - Address ID
     * @returns {Boolean}
     */
    const isLegacyAddressId = (id) => {
        return id && String(id).startsWith(ADDRESS_CONSTANTS.LEGACY_ID_PREFIX);
    };

    /**
     * Tạo legacy address ID từ customer ID
     * @param {String} customerId - Customer ID
     * @returns {String}
     */
    const createLegacyAddressId = (customerId) => {
        return ADDRESS_CONSTANTS.LEGACY_ID_PREFIX + customerId;
    };

    /**
     * Parse address string thành các phần (tinh, thanhPho, phuongXa, diaChiChiTiet)
     * @param {String} addressString - Full address string
     * @returns {Object} - { tinh, thanhPho, phuongXa, diaChiChiTiet }
     */
    const parseAddressString = (addressString) => {
        if (!addressString || !addressString.includes(',')) {
            return { tinh: null, thanhPho: null, phuongXa: null, diaChiChiTiet: addressString || '' };
        }

        const parts = addressString.split(',').map((p) => p.trim());

        if (parts.length >= ADDRESS_CONSTANTS.ADDRESS_PARTS_MIN) {
            // Định dạng: [Số nhà], [Phường], [Quận], [Tỉnh]
            return {
                tinh: parts[parts.length - 1],
                thanhPho: parts[parts.length - 2],
                phuongXa: parts[parts.length - 3],
                diaChiChiTiet: parts.slice(0, parts.length - 3).join(', ')
            };
        }

        return { tinh: null, thanhPho: null, phuongXa: null, diaChiChiTiet: addressString };
    };

    return {
        mapCodesToNames,
        isLegacyAddressId,
        createLegacyAddressId,
        parseAddressString
    };
}
