import { defineStore } from 'pinia';

export const useBanHangStore = defineStore('banHang', {
  state: () => ({
    orders: [],
    activeOrderIndex: 0,
    vouchers: [],
    loading: false,
    isProcessing: false,
    
    // Config filters & products
    productSearchKeyword: '',
    productSearchResults: [],
    productSearchLoading: false,
    showProductAutocomplete: false,
    
    // Filters
    filterThuongHieu: 'ALL',
    filterMucDich: 'ALL',
    filterKhoangGia: 'ALL',
    filterMauSac: 'ALL',
    filterKichCo: 'ALL',
    
    filterBrands: [],
    filterPurposes: [],
    filterColors: [],
    filterSizes: [],
    maxProductPrice: 7000000,
    
    // Customer
    customerSearch: '',
    customerResults: [],
    customerLoading: false,
    customerForm: {
      ten: '',
      sdt: '',
      email: '',
      gioiTinh: 'Giới tính',
      ngaySinh: '',
      tongDonHang: 0
    },
    
    // Shipping
    isGiaoHang: false,
    shippingFee: 0,
    surcharge: 0,
    isFreeShip: false,
    onlyChargeIfReturned: false,
    
    recipientName: '',
    recipientPhone: '',
    recipientAddressDetail: '',
    recipientProvince: null,
    recipientDistrict: null,
    recipientWard: null,
    
    // Payment
    checkoutData: {
      paymentMethod: 'CASH',
      vnpayMethod: 'QR',
      receivedAmount: 0,
      note: ''
    }
  }),
  getters: {
    selectedOrder(state) {
      return state.orders[state.activeOrderIndex] || null;
    },
    cartSubtotalAmount(state) {
      const order = this.selectedOrder;
      if (!order) return 0;
      if (order.tongTien !== undefined && order.tongTien !== null) {
        return Number(order.tongTien || 0);
      }
      const items = order.listsHoaDonChiTiet || [];
      return items.reduce((sum, item) => sum + Number(item.thanhTien || 0), 0);
    },
    originalTotalAmount(state) {
      const order = this.selectedOrder;
      const items = order?.listsHoaDonChiTiet || [];
      return items.reduce((sum, item) => {
        const qty = Number(item.soLuong || 0);
        const price = Number(item.donGia || 0);
        const percent = Number(item.phanTramGiam || 0);
        if (percent > 0 && percent < 100) {
          const originalPrice = price / (1 - percent / 100);
          return sum + (originalPrice * qty);
        }
        return sum + (price * qty);
      }, 0);
    },
    productDiscountAmount() {
      return Math.max(0, this.originalTotalAmount - this.cartSubtotalAmount);
    },
    amountAfterProductDiscount() {
      return Math.max(0, this.cartSubtotalAmount);
    },
    discountAmount() {
      const order = this.selectedOrder;
      const raw = this.amountAfterProductDiscount;
      const after = Number(order?.tongTienSauGiam || order?.tongTien || 0);
      return Math.min(raw, Math.max(0, raw - after));
    },
    amountAfterAllDiscounts() {
      return Math.max(0, this.amountAfterProductDiscount - this.discountAmount);
    },
    finalCollectAmount(state) {
      const order = this.selectedOrder;
      const ship = order?.loaiDon === 'ONLINE' ? Number(state.shippingFee || 0) : 0;
      return Math.max(0, this.amountAfterAllDiscounts + ship + Number(state.surcharge || 0));
    },
    remainingBalance(state) {
      const received = Number(state.checkoutData.receivedAmount || 0);
      return Math.max(0, this.finalCollectAmount - received);
    },
    changeAmount(state) {
      const received = Number(state.checkoutData.receivedAmount || 0);
      return Math.max(0, received - this.finalCollectAmount);
    }
  },
  actions: {
    setOrders(orders) {
      this.orders = orders;
    },
    setActiveOrderIndex(index) {
      this.activeOrderIndex = index;
    },
    updateOrder(updatedOrder) {
      const idx = this.orders.findIndex(o => o.id === updatedOrder.id);
      if (idx !== -1) {
        this.orders[idx] = updatedOrder;
      }
    },
    resetFilters() {
      this.filterThuongHieu = 'ALL';
      this.filterMucDich = 'ALL';
      this.filterKhoangGia = 'ALL';
      this.filterMauSac = 'ALL';
      this.filterKichCo = 'ALL';
      this.productSearchKeyword = '';
      this.showProductAutocomplete = false;
    },
    updateProductStock(variantId, newStock) {
      if (!this.productSearchResults || !Array.isArray(this.productSearchResults)) return;
      const idx = this.productSearchResults.findIndex(p => p.id === variantId);
      if (idx !== -1) {
        this.productSearchResults[idx].soLuongTon = Number(newStock);
        this.productSearchResults = [...this.productSearchResults];
      }
    }
  }
});
