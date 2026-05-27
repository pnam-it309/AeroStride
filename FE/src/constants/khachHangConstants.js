/**
 * KHACH_HANG_CONSTANTS - Constants cho module Khách hàng
 * Tránh hardcode trong components
 */

// Trạng thái khách hàng
export const TRANG_THAI_KHACH_HANG = {
    DANG_HOAT_DONG: 'DANG_HOAT_DONG',
    NGUNG_HOAT_DONG: 'NGUNG_HOAT_DONG'
};

// Labels cho trạng thái
export const TRANG_THAI_LABELS = {
    [TRANG_THAI_KHACH_HANG.DANG_HOAT_DONG]: 'Đang hoạt động',
    [TRANG_THAI_KHACH_HANG.NGUNG_HOAT_DONG]: 'Ngừng hoạt động'
};

// Filter options
export const TRANG_THAI_FILTER_OPTIONS = [
    { title: 'Tất cả', value: null },
    { title: 'Đang hoạt động', value: TRANG_THAI_KHACH_HANG.DANG_HOAT_DONG },
    { title: 'Ngừng hoạt động', value: TRANG_THAI_KHACH_HANG.NGUNG_HOAT_DONG }
];

// Table headers
export const KHACH_HANG_TABLE_HEADERS = [
    { text: 'STT', width: '50px', align: 'center' },
    { text: 'Mã khách hàng', width: '90px', align: 'center' },
    { text: 'Tên khách hàng', width: '100px', align: 'left' },
    { text: 'Giới tính', width: '90px', align: 'center' },
    { text: 'Thông tin liên hệ', width: '150px', align: 'left' },
    { text: 'Địa chỉ', width: '170px', align: 'left' },
    { text: 'Trạng thái', width: '90px', align: 'center' },
    { text: 'Hành động', width: '100px', align: 'center' }
];

// Breadcrumbs
export const KHACH_HANG_BREADCRUMBS = [
    { title: 'Quản lý tài khoản', disabled: false, href: '#' },
    { title: 'Khách hàng', disabled: true }
];

// Address constants
export const ADDRESS_CONSTANTS = {
    LEGACY_ID_PREFIX: 'root-', // Prefix cho địa chỉ legacy từ bảng KhachHang
    ADDRESS_PARTS_MIN: 4, // Số phần tối thiểu khi parse address string
    REFRESH_DELAY: 800 // Delay cho refresh animation (ms)
};

// Default avatar URL
export const DEFAULT_AVATAR_URL = 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y';

// Invoice table headers
export const INVOICE_TABLE_HEADERS = [
    { text: 'STT', width: '60px' },
    { text: 'Mã sản phẩm' },
    { text: 'Tên sản phẩm' },
    { text: 'Mã biến thể' },
    { text: 'Tên biến thể' },
    { text: 'Số lượng', width: '100px' },
    { text: 'Đơn giá', width: '130px' },
    { text: 'Thành tiền', width: '140px' }
];
