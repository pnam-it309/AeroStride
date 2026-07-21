/**
 * NHAN_VIEN_CONSTANTS - Các hằng số cho module Nhân viên
 */

export const TRANG_THAI_NHAN_VIEN = {
    DANG_HOAT_DONG: 'DANG_HOAT_DONG',
    NGUNG_HOAT_DONG: 'NGUNG_HOAT_DONG'
};

export const TRANG_THAI_FILTER_OPTIONS = [
    { title: 'Tất cả trạng thái', value: null },
    { title: 'Đang hoạt động', value: TRANG_THAI_NHAN_VIEN.DANG_HOAT_DONG },
    { title: 'Ngừng hoạt động', value: TRANG_THAI_NHAN_VIEN.NGUNG_HOAT_DONG }
];

export const NHAN_VIEN_TABLE_HEADERS = [
    { text: 'STT', width: '60px' },
    { text: 'Mã nhân viên', width: '110px' },
    { text: 'Tên nhân viên', width: '140px' },
    { text: 'Tài khoản', width: '160px' },
    { text: 'Giới tính', width: '100px' },
    { text: 'Số điện thoại', width: '120px' },
    { text: 'Địa chỉ', width: '220px' },
    { text: 'Chức vụ', width: '110px' },
    { text: 'Trạng thái', width: '140px' },
    { text: 'Hành động', width: '120px' }
];
