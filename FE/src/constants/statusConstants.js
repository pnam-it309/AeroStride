/**
 * Hệ thống hằng số trạng thái chung cho toàn bộ ứng dụng AeroStride
 * Sử dụng hằng số này để tránh hardcode chuỗi string trong code.
 */

export const SYSTEM_STATUS = {
    // Trạng thái hoạt động chung
    ACTIVE: 'DANG_HOAT_DONG',
    INACTIVE: 'NGUNG_HOAT_DONG',
    
    // Alias hỗ trợ các module cũ hoặc logic khác
    HOAT_DONG: 'DANG_HOAT_DONG',
    NGUNG_HOAT_DONG: 'NGUNG_HOAT_DONG',
    
    // Trạng thái hiển thị (Label)
    LABELS: {
        DANG_HOAT_DONG: 'Đang hoạt động',
        NGUNG_HOAT_DONG: 'Ngừng hoạt động',
        ACTIVE: 'Đang hoạt động',
        INACTIVE: 'Ngừng hoạt động'
    },
    
    // Màu sắc Vuetify tương ứng
    COLORS: {
        DANG_HOAT_DONG: 'success',
        NGUNG_HOAT_DONG: 'warning',
        ACTIVE: 'success',
        INACTIVE: 'warning'
    }
};

export const STATUS_OPTIONS = [
    { title: 'Tất cả', value: null },
    { title: 'Đang hoạt động', value: SYSTEM_STATUS.ACTIVE },
    { title: 'Ngừng hoạt động', value: SYSTEM_STATUS.INACTIVE }
];
