/**
 * Quy trình xử lý trạng thái chung cho hệ thống Admin AeroStride
 * Đồng bộ logic giữa các module: Nhân viên, Khách hàng, Sản phẩm, Giảm giá...
 */

import { SYSTEM_STATUS } from '@/constants/statusConstants';

export const STATUS = {
    ACTIVE: SYSTEM_STATUS.ACTIVE,
    INACTIVE: SYSTEM_STATUS.INACTIVE
};

/**
 * Kiểm tra trạng thái có đang hoạt động hay không
 * Hỗ trợ nhiều định dạng: String ('DANG_HOAT_DONG', 'ACTIVE'), Number (0), Boolean
 */
export const isActiveStatus = (status) => {
    if (status === null || status === undefined) return false;

    // Nếu là Number (Backend cũ trả về 0 là active)
    if (typeof status === 'number') return status === 0;

    // Nếu là Boolean
    if (typeof status === 'boolean') return status === true;

    const normalized = String(status).toUpperCase();
    return (
        normalized === SYSTEM_STATUS.ACTIVE ||
        normalized === 'ACTIVE' ||
        normalized === 'HOAT_DONG' ||
        normalized === '0' ||
        normalized === 'TRUE'
    );
};

/**
 * Trả về nhãn hiển thị cho trạng thái
 */
export const getStatusLabel = (status) => {
    return isActiveStatus(status) ? SYSTEM_STATUS.LABELS.ACTIVE : SYSTEM_STATUS.LABELS.INACTIVE;
};

/**
 * Trả về class CSS hoặc màu sắc cho Chip trạng thái
 */
export const getStatusColor = (status) => {
    return isActiveStatus(status) ? SYSTEM_STATUS.COLORS.ACTIVE : SYSTEM_STATUS.COLORS.INACTIVE;
};
