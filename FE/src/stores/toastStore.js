import { defineStore } from 'pinia';

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
            this.showToast(message, 'error', 'mdi-alert-circle', timeout);
        },
        
        warning(message, timeout = 4000) {
            this.showToast(message, 'warning', 'mdi-alert', timeout);
        },
        
        info(message, timeout = 3000) {
            this.showToast(message, 'info', 'mdi-information', timeout);
        },
        
        showToast(message, color, icon, timeout) {
            this.message = message;
            this.color = color;
            this.icon = icon;
            this.timeout = timeout;
            this.show = true;
        },
        
        hide() {
            this.show = false;
        }
    }
});
