import api from '@/services/apiService';

export const dichVuVnPay = {
  /**
   * Tạo URL thanh toán VNPay từ backend
   * @param {Object} payload - { amount, orderId, orderInfo }
   * @returns {Promise<Object>} - { paymentUrl, status }
   */
  async createPaymentUrl(payload) {
    const response = await api.post('/payment/create', payload);
    return response.data.data;
  },

  /**
   * Xác thực kết quả callback từ VNPay
   * @param {Object} queryParams - Toàn bộ query parameters nhận được từ callback
   * @returns {Promise<Object>} - Kết quả từ Backend (ApiResponse)
   */
  async verifyPaymentCallback(queryParams) {
    const response = await api.get('/payment/vnpay-callback', { params: queryParams });
    return response.data;
  }
};
