import api from '../apiService';

export const dichVuThanhToan = {
  // Lấy tất cả phương thức thanh toán
  async layPhuongThucThanhToan() {
    // const response = await api.get('/payment/methods');
    // return response.data.data;
    return new Promise(resolve => setTimeout(() => resolve([
      { id: 1, name: 'VNPay', description: 'Cổng thanh toán VNPay', icon: 'mdi-credit-card', status: 'active' },
      { id: 2, name: 'Tiền mặt', description: 'Thanh toán bằng tiền mặt', icon: 'mdi-cash', status: 'active' }
    ]), 500));
  },

  // Lấy giao dịch thanh toán
  async layGiaoDichThanhToan(params) {
    // const response = await api.get('/payment/transactions', { params });
    // return response.data.data;
    return new Promise(resolve => setTimeout(() => resolve({
      content: [
        { id: 1, transactionCode: 'TXN-001', customerName: 'Nguyễn Văn A', amount: 1500000, paymentMethod: 'VNPay', status: 'success', createdAt: new Date().toISOString() },
        { id: 2, transactionCode: 'TXN-002', customerName: 'Trần Thị B', amount: 500000, paymentMethod: 'Tiền mặt', status: 'success', createdAt: new Date(Date.now() - 86400000).toISOString() }
      ],
      totalElements: 2
    }), 500));
  },

  // Xử lý thanh toán
  async xuLyThanhToan(paymentData) {
    const response = await api.post('/payment/process', paymentData);
    return response.data.data;
  },

  // Hoàn tiền
  async hoanTien(paymentId, refundData) {
    const response = await api.post(`/payment/${paymentId}/refund`, refundData);
    return response.data.data;
  },

  // Lấy thống kê thanh toán
  async layThongKeThanhToan() {
    // const response = await api.get('/payment/statistics');
    // return response.data.data;
    return new Promise(resolve => setTimeout(() => resolve({
      totalRevenue: 25000000,
      todayRevenue: 1500000,
      successfulPayments: 15,
      failedPayments: 1,
      refundAmount: 0
    }), 500));
  }
};
