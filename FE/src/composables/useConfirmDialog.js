import { ref } from 'vue';

/**
 * Composable để quản lý Confirmation Dialog state
 * Giúp giảm bớt code lặp lại ở các view quản lý (SanPham, KhachHang, NhanVien, etc.)
 */
export function useConfirmDialog() {
    const confirmDialog = ref({
        show: false,
        title: '',
        message: '',
        color: 'primary',
        action: null,
        loading: false
    });

    /**
     * Mở confirmation dialog
     * @param {Object} config - { title, message, color, action }
     */
    const setConfirm = (config = {}) => {
        confirmDialog.value = {
            show: true,
            title: config.title || 'Xác nhận',
            message: config.message || 'Bạn có chắc chắn?',
            color: config.color || 'primary',
            action: config.action || null,
            loading: false
        };
    };

    /**
     * Đóng confirmation dialog
     */
    const clearConfirm = () => {
        confirmDialog.value = {
            show: false,
            title: '',
            message: '',
            color: 'primary',
            action: null,
            loading: false
        };
    };

    /**
     * Thực thi action và đóng dialog
     * @param {Boolean} confirmed - true nếu user chọn "Xác nhận"
     */
    const handleConfirm = async (confirmed) => {
        if (!confirmed) {
            clearConfirm();
            return;
        }

        if (confirmDialog.value.action) {
            confirmDialog.value.loading = true;
            try {
                await confirmDialog.value.action();
            } finally {
                confirmDialog.value.loading = false;
            }
        }
        clearConfirm();
    };

    return {
        confirmDialog,
        setConfirm,
        clearConfirm,
        handleConfirm
    };
}
