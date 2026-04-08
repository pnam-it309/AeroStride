import { defineStore } from 'pinia';

export const useLoaderStore = defineStore('loader', {
  state: () => ({
    overlay: false,
    progressBar: false,
    message: 'Đang tải dữ liệu...'
  }),
  actions: {
    showOverlay(msg = 'Hệ thống đang khởi động lại...') {
      this.message = msg;
      this.overlay = true;
    },
    hideOverlay() {
      this.overlay = false;
    },
    startProgress() {
      this.progressBar = true;
    },
    stopProgress() {
      setTimeout(() => {
        this.progressBar = false;
      }, 500); // Small delay for smoothness
    }
  }
});
