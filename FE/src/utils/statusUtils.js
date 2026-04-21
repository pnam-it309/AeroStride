/**
 * Quy trình xử lý trạng thái chung cho hệ thống Admin AeroStride
 * Đồng bộ logic giữa các module: Nhân viên, Khách hàng, Sản phẩm, Giảm giá...
 */

export const STATUS = {
    ACTIVE: 'DANG_HOAT_DONG',
    INACTIVE: 'KHONG_HOAT_DONG'
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
        normalized === STATUS.ACTIVE || 
        normalized === 'ACTIVE' || 
        normalized === 'HOAT_DONG' || 
        normalized === '0' ||
        normalized === 'TRUE'
    );
};

/**
 * Trả về nhãn hiển thị trạng thái tiếng Việt
 */
export const getStatusLabel = (status) => {
    return isActiveStatus(status) ? 'Hoạt động' : 'Ngừng hoạt động';
};

/**
 * Trả về class CSS hoặc màu sắc cho Chip trạng thái
 */
export const getStatusColor = (status) => {
    return isActiveStatus(status) ? 'success' : 'warning';
};
