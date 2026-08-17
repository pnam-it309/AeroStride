import { defineStore } from 'pinia';
import { getBackendErrorMessage, formatUserErrorMessage } from '@/utils/errorUtils';

export const useToastStore = defineStore('toast', {
    state: () => ({
        show: false,
        message: '',
        color: 'success',
        timeout: 3000,
        icon: 'mdi-check-circle'
    }),

    actions: {
        success(message, timeout = 3000) {
            this.showToast(message, 'success', 'mdi-check-circle', timeout);
        },

        error(message, timeout = 5000) {
            const userMsg = typeof message === 'object' && message !== null
                ? getBackendErrorMessage(message, 'Đã xảy ra lỗi. Vui lòng thử lại.', 'ToastStore')
                : formatUserErrorMessage(message, 'Đã xảy ra lỗi. Vui lòng thử lại.');
            this.showToast(userMsg, 'error', 'mdi-alert-circle', timeout);
        },

        warning(message, timeout = 4000) {
            const userMsg = typeof message === 'object' && message !== null
                ? getBackendErrorMessage(message, 'Cảnh báo hệ thống', 'ToastStore')
                : formatUserErrorMessage(message, 'Cảnh báo hệ thống');
            this.showToast(userMsg, 'warning', 'mdi-alert', timeout);
        },

        info(message, timeout = 3000) {
            this.showToast(message, 'info', 'mdi-information', timeout);
        },

        showToast(message, color = 'info', icon, timeout = 3000) {
            const userMsg = typeof message === 'object' && message !== null
                ? getBackendErrorMessage(message, 'Thông báo', 'ToastStore')
                : formatUserErrorMessage(message, 'Thông báo');

            this.message = userMsg;
            // Force primary (dark blue) color for all toasts per user request
            this.color = 'primary';
            this.icon = icon || (color === 'error' ? 'mdi-alert-circle' : color === 'warning' ? 'mdi-alert' : 'mdi-check-circle');
            this.timeout = timeout;
            this.show = true;
        },

        showError(message, timeout = 5000) {
            this.error(message, timeout);
        },

        showSuccess(message, timeout = 3000) {
            this.success(message, timeout);
        },

        showWarning(message, timeout = 4000) {
            this.warning(message, timeout);
        },

        showInfo(message, timeout = 3000) {
            this.info(message, timeout);
        },

        hide() {
            this.show = false;
        }
    }
});
