/**
 * Hằng số trạng thái Lịch làm việc & Ca làm hệ thống AeroStride
 */

// 1. Trạng thái Đi làm / Chấm công trong Lịch làm việc
export const LICH_LAM_VIEC_STATUS = {
    DUNG_GIO: 'DUNG_GIO',
    DI_MUON: 'DI_MUON',
    CHUA_VAO_CA: 'CHUA_VAO_CA'
};

export const LICH_LAM_VIEC_STATUS_CONFIG = {
    [LICH_LAM_VIEC_STATUS.DUNG_GIO]: {
        label: 'Đúng giờ',
        color: 'success',
        icon: 'mdi-check-circle-outline',
        chipClass: 'status-chip-active'
    },
    [LICH_LAM_VIEC_STATUS.DI_MUON]: {
        label: 'Đi muộn',
        color: 'error',
        icon: 'mdi-clock-alert-outline',
        chipClass: 'status-chip-cancelled'
    },
    [LICH_LAM_VIEC_STATUS.CHUA_VAO_CA]: {
        label: 'Chưa vào ca',
        color: 'grey',
        icon: 'mdi-clock-outline',
        chipClass: 'status-chip-default'
    }
};

export const LICH_LAM_VIEC_STATUS_OPTIONS = [
    { title: 'Tất cả trạng thái', value: 'Tất cả' },
    { title: 'Đúng giờ', value: LICH_LAM_VIEC_STATUS.DUNG_GIO },
    { title: 'Đi muộn', value: LICH_LAM_VIEC_STATUS.DI_MUON },
    { title: 'Chưa vào ca', value: LICH_LAM_VIEC_STATUS.CHUA_VAO_CA }
];

// 2. Trạng thái Ca làm / Bàn giao ca (Giao ca)
export const GIAO_CA_STATUS = {
    OPEN: 'OPEN',
    CLOSED: 'CLOSED'
};

export const GIAO_CA_STATUS_CONFIG = {
    [GIAO_CA_STATUS.OPEN]: {
        label: 'Đang mở',
        color: 'success',
        icon: 'mdi-lock-open-outline',
        chipClass: 'status-chip-active'
    },
    [GIAO_CA_STATUS.CLOSED]: {
        label: 'Đã chốt',
        color: 'grey',
        icon: 'mdi-lock-outline',
        chipClass: 'status-chip-default'
    }
};

export const GIAO_CA_STATUS_OPTIONS = [
    { title: 'Tất cả trạng thái', value: null },
    { title: 'Đang mở', value: GIAO_CA_STATUS.OPEN },
    { title: 'Đã chốt', value: GIAO_CA_STATUS.CLOSED }
];
