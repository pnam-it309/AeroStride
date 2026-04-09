<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import {
  PlusIcon, ReceiptIcon, UserIcon, TicketIcon,
  TrashIcon, SearchIcon, CashIcon, CreditCardIcon,
  ShoppingCartIcon, XIcon, MinusIcon
} from 'vue-tabler-icons';
import { useNotifications } from '@/services/notificationService';

const { addNotification } = useNotifications();

// State
const loading = ref(false);
const orders = ref([]);
const activeOrderIndex = ref(0);
const products = ref([]);
const productSearch = ref('');
const customerSearch = ref('');
const customers = ref([]);
const vouchers = ref([]);
const isProcessing = ref(false);

const selectedOrder = computed(() => orders.value[activeOrderIndex.value] || null);

// Initialize
const init = async () => {
  loading.value = true;
  try {
    const data = await dichVuDonHang.layDonHangCho();
    orders.value = data || [];
    if (orders.value.length === 0) {
      await createNewOrder();
    }
  } catch (error) {
    console.error('Failed to init POS:', error);
  } finally {
    loading.value = false;
  }
};

// Order Management
const createNewOrder = async () => {
  if (orders.value.length >= 5) {
    addNotification({ title: 'Giới hạn', subtitle: 'Tối đa 5 hóa đơn chờ cùng lúc', color: 'warning' });
    return;
  }
  try {
    const newOrder = await dichVuDonHang.taoDonHang();
    orders.value.push(newOrder);
    activeOrderIndex.value = orders.value.length - 1;
  } catch (error) {
    addNotification({ title: 'Lỗi', subtitle: 'Không thể tạo hóa đơn mới', color: 'error' });
  }
};

const closeOrder = async (orderId, index) => {
  try {
    await dichVuDonHang.xoaDonHang(orderId);
    orders.value.splice(index, 1);
    if (activeOrderIndex.value >= orders.value.length) {
      activeOrderIndex.value = Math.max(0, orders.value.length - 1);
    }
    if (orders.value.length === 0) await createNewOrder();
  } catch (error) {
    addNotification({ title: 'Lỗi', subtitle: 'Không thể hủy hóa đơn', color: 'error' });
  }
};

// Product Search & Add
const onProductSearch = async () => {
  if (!productSearch.value) {
    products.value = [];
    return;
  }
  try {
    const data = await dichVuDonHang.searchSanPham(productSearch.value);
    products.value = data || [];
  } catch (e) { }
};

const addProductToOrder = async (product) => {
  if (!selectedOrder.value) return;
  try {
    const updatedOrder = await dichVuDonHang.addSanPham(selectedOrder.value.id, {
      idChiTietSanPham: product.id,
      soLuong: 1
    });
    const idx = orders.value.findIndex(o => o.id === selectedOrder.value.id);
    if (idx !== -1) orders.value[idx] = updatedOrder;

    productSearch.value = '';
    products.value = [];
    addNotification({ title: 'Thành công', subtitle: `Đã thêm ${product.tenSanPham}`, color: 'success' });
  } catch (error) {
    addNotification({ title: 'Lỗi', subtitle: 'Không thể thêm sản phẩm', color: 'error' });
  }
};

const updateItemQty = async (item, delta) => {
  const newQty = item.soLuong + delta;
  if (newQty < 1) return;
  try {
    const updatedOrder = await dichVuDonHang.updateSoLuong(selectedOrder.value.id, item.id, newQty);
    const idx = orders.value.findIndex(o => o.id === selectedOrder.value.id);
    if (idx !== -1) orders.value[idx] = updatedOrder;
  } catch (error) {
    addNotification({ title: 'Lỗi', subtitle: 'Hết hàng', color: 'error' });
  }
};

const removeItem = async (item) => {
  try {
    await dichVuDonHang.removeSanPham(selectedOrder.value.id, item.id);
    const data = await dichVuDonHang.layDonHangCho();
    orders.value = data || [];
  } catch (error) {
    addNotification({ title: 'Lỗi', subtitle: 'Lỗi xóa sản phẩm', color: 'error' });
  }
};

// Customer Selection
const onCustomerSearch = async () => {
  if (!customerSearch.value) {
    customers.value = [];
    return;
  }
  const data = await dichVuDonHang.searchKhachHang(customerSearch.value);
  customers.value = data || [];
};

const selectCustomer = async (customer) => {
  try {
    const updated = await dichVuDonHang.setKhachHang(selectedOrder.value.id, customer.id);
    const idx = orders.value.findIndex(o => o.id === selectedOrder.value.id);
    if (idx !== -1) orders.value[idx] = updated;
    customerSearch.value = '';
    customers.value = [];
    fetchVouchers();
  } catch (error) {
    addNotification({ title: 'Lỗi', subtitle: 'Lỗi gắn khách', color: 'error' });
  }
};

const removeCustomer = async () => {
  try {
    const updated = await dichVuDonHang.setKhachHang(selectedOrder.value.id, null);
    const idx = orders.value.findIndex(o => o.id === selectedOrder.value.id);
    if (idx !== -1) orders.value[idx] = updated;
  } catch (e) { }
};

// Voucher Logic
const fetchVouchers = async () => {
  if (!selectedOrder.value) return;
  vouchers.value = await dichVuDonHang.getVouchers(selectedOrder.value.tongTien || 0);
};

const applyVoucher = async (voucherId) => {
  try {
    const updated = await dichVuDonHang.setVoucher(selectedOrder.value.id, voucherId);
    const idx = orders.value.findIndex(o => o.id === selectedOrder.value.id);
    if (idx !== -1) orders.value[idx] = updated;
  } catch (e) {
    addNotification({ title: 'Lỗi', subtitle: 'Lỗi áp mã', color: 'error' });
  }
};

// Checkout
const checkoutData = ref({
  paymentMethod: 'CASH',
  receivedAmount: 0,
  note: ''
});

const handleCheckout = async () => {
  if (checkoutData.value.paymentMethod === 'CASH' && checkoutData.value.receivedAmount < selectedOrder.value.tongTienSauGiam) {
    addNotification({ title: 'Thanh toán', subtitle: 'Tiền khách đưa chưa đủ', color: 'warning' });
    return;
  }

  isProcessing.value = true;
  try {
    const isCash = checkoutData.value.paymentMethod === 'CASH';
    const payload = {
      idKhachHang: selectedOrder.value.idKhachHang,
      idPhieuGiamGia: selectedOrder.value.idPhieuGiamGia,
      tongTien: selectedOrder.value.tongTien,
      phiVanChuyen: 0,
      tongTienSauGiam: selectedOrder.value.tongTienSauGiam,
      loaiDon: 'TAI_QUAY',
      ghiChu: checkoutData.value.note,
      tienMat: isCash ? selectedOrder.value.tongTienSauGiam : 0,
      tienChuyenKhoan: isCash ? 0 : selectedOrder.value.tongTienSauGiam,
      maGiaoDich: ''
    };
    await dichVuDonHang.checkout(selectedOrder.value.id, payload);
    addNotification({ title: 'Thành công', subtitle: 'Đã hoàn tất thanh toán', color: 'success' });
    orders.value.splice(activeOrderIndex.value, 1);
    if (orders.value.length === 0) await createNewOrder();
    activeOrderIndex.value = 0;
  } catch (error) {
    addNotification({ title: 'Lỗi', subtitle: 'Thanh toán thất bại, vui lòng thử lại', color: 'error' });
  } finally {
    isProcessing.value = false;
  }
};

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

onMounted(init);

watch(() => selectedOrder.value?.tongTien, (n) => {
  if (n !== undefined) fetchVouchers();
});

</script>

<template>
  <v-container fluid class="pos-container pa-0 min-h-screen">
    <v-row no-gutters class="fill-height">
      <!-- MAIN AREA (CART & PRODUCTS) -->
      <v-col cols="12" lg="8" class="main-column pa-6">
        <!-- TABS BAR -->
        <div class="d-flex align-center gap-2 mb-6 overflow-x-auto pb-2">
          <v-btn v-for="(order, idx) in orders" :key="order.id" :variant="activeOrderIndex === idx ? 'flat' : 'tonal'"
            :color="activeOrderIndex === idx ? 'primary' : 'grey-lighten-3'" height="44"
            class="rounded-lg text-none px-4" @click="activeOrderIndex = idx">
            <ReceiptIcon size="18" class="mr-2" />
            Đơn {{ idx + 1 }}
            <XIcon v-if="orders.length > 1" size="14" class="ml-3 hover-error"
              @click.stop="closeOrder(order.id, idx)" />
          </v-btn>
          <v-btn icon color="primary" variant="tonal" size="44" @click="createNewOrder" v-if="orders.length < 5">
            <PlusIcon size="22" />
          </v-btn>
        </div>

        <!-- PRODUCT SEARCH -->
        <div class="position-relative mb-6">
          <v-card class="search-card border shadow-none overflow-visible">
            <v-card-text class="pa-0">
              <v-text-field v-model="productSearch" placeholder="Tìm sản phẩm (Tên, SKU, Barcode)..." variant="plain"
                hide-details class="px-4 py-2" prepend-inner-icon="mdi-magnify" @input="onProductSearch"></v-text-field>
            </v-card-text>
          </v-card>

          <v-list v-if="products.length > 0" class="search-results border rounded-lg shadow-xl">
            <v-list-item v-for="p in products" :key="p.id" class="pa-4 hover-highlight border-b"
              @click="addProductToOrder(p)">
              <div class="d-flex align-center">
                <div class="rounded mr-4 border bg-grey-lighten-4 d-flex align-center justify-center"
                  style="width: 48px; height: 48px">
                  <ShoppingCartIcon size="24" class="text-grey" />
                </div>
                <div class="flex-grow-1">
                  <div class="text-subtitle-1 font-weight-bold">{{ p.tenSanPham }}</div>
                  <div class="text-caption text-medium-emphasis">
                    Màu: {{ p.tenMauSac }} | Size: {{ p.tenKichThuoc }} | Kho: {{ p.soLuongTon }}
                  </div>
                </div>
                <div class="text-right">
                  <div class="text-subtitle-1 font-weight-bold text-primary">{{ formatCurrency(p.giaBan) }}</div>
                  <v-btn size="x-small" color="primary" variant="flat" class="mt-1">Thêm</v-btn>
                </div>
              </div>
            </v-list-item>
          </v-list>
        </div>

        <!-- CART TABLE -->
        <v-card class="cart-card border shadow-none overflow-hidden h-full">
          <v-table class="pos-table">
            <thead>
              <tr class="bg-grey-lighten-4">
                <th class="text-left font-weight-bold py-4">Sản phẩm</th>
                <th class="text-center font-weight-bold py-4">Số lượng</th>
                <th class="text-right font-weight-bold py-4">Đơn giá</th>
                <th class="text-right font-weight-bold py-4">Thành tiền</th>
                <th class="text-center py-4"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!selectedOrder?.listsHoaDonChiTiet?.length">
                <td colspan="5" class="text-center py-16">
                  <ShoppingCartIcon size="64" color="grey" class="mb-4 opacity-20" />
                  <p class="text-h6 text-medium-emphasis">Giỏ hàng đang trống</p>
                </td>
              </tr>
              <tr v-for="item in selectedOrder?.listsHoaDonChiTiet" :key="item.id">
                <td>
                  <div class="d-flex align-center py-2">
                    <div class="rounded mr-3 border bg-grey-lighten-4 d-flex align-center justify-center"
                      style="width: 40px; height: 40px">
                      <ShoppingCartIcon size="18" />
                    </div>
                    <div>
                      <div class="font-weight-bold text-subtitle-2">{{ item.tenSanPham }}</div>
                      <div class="text-caption text-medium-emphasis">Size {{ item.tenKichThuoc }}, {{ item.tenMauSac }}
                      </div>
                    </div>
                  </div>
                </td>
                <td class="text-center">
                  <div class="qty-control d-flex align-center justify-center">
                    <v-btn icon size="x-small" variant="tonal" @click="updateItemQty(item, -1)">
                      <MinusIcon size="14" />
                    </v-btn>
                    <span class="mx-3 font-weight-bold">{{ item.soLuong }}</span>
                    <v-btn icon size="x-small" variant="tonal" @click="updateItemQty(item, 1)">
                      <PlusIcon size="14" />
                    </v-btn>
                  </div>
                </td>
                <td class="text-right">{{ formatCurrency(item.donGia) }}</td>
                <td class="text-right font-weight-bold">{{ formatCurrency(item.thanhTien) }}</td>
                <td class="text-center">
                  <v-btn icon variant="text" color="error" size="small" @click="removeItem(item)">
                    <TrashIcon size="18" />
                  </v-btn>
                </td>
              </tr>
            </tbody>
          </v-table>
        </v-card>
      </v-col>

      <!-- SIDEBAR -->
      <v-col cols="12" lg="4" class="side-column border-l bg-white pa-6">
        <div class="mb-6">
          <div class="d-flex justify-space-between align-center mb-4">
            <h3 class="text-h6 font-weight-bold">Khách hàng</h3>
            <v-chip v-if="selectedOrder?.tenKhachHang" color="primary" variant="flat" size="small" closable
              @click:close="removeCustomer">
              {{ selectedOrder.tenKhachHang }}
            </v-chip>
          </div>

          <div v-if="!selectedOrder?.tenKhachHang">
            <v-text-field v-model="customerSearch" placeholder="SĐT hoặc tên khách..." variant="outlined"
              density="comfortable" prepend-inner-icon="mdi-account-search" hide-details
              @input="onCustomerSearch"></v-text-field>
            <div v-if="customers.length > 0" class="customer-results border rounded-lg mt-2 overflow-hidden shadow-lg">
              <v-list class="pa-0">
                <v-list-item v-for="c in customers" :key="c.id" @click="selectCustomer(c)" class="border-b">
                  <v-list-item-title class="font-weight-bold">{{ c.ten }}</v-list-item-title>
                  <v-list-item-subtitle>{{ c.sdt }}</v-list-item-subtitle>
                </v-list-item>
              </v-list>
            </div>
          </div>
        </div>

        <div class="payment-details bg-grey-lighten-5 rounded-xl pa-5 border-dashed border-2">
          <div class="d-flex justify-space-between mb-4">
            <span class="text-subtitle-1">Tổng tiền hàng:</span>
            <span class="text-subtitle-1 font-weight-bold">{{ formatCurrency(selectedOrder?.tongTien) }}</span>
          </div>

          <v-select v-model="selectedOrder.idPhieuGiamGia" :items="vouchers" item-title="ten" item-value="id"
            label="Mã giảm giá" variant="outlined" density="compact" class="mb-4"
            prepend-inner-icon="mdi-ticket-percent" @update:model-value="applyVoucher"></v-select>

          <div class="d-flex justify-space-between mb-4 text-error">
            <span>Giảm giá:</span>
            <span class="font-weight-bold">- {{ formatCurrency((selectedOrder?.tongTien || 0) -
              (selectedOrder?.tongTienSauGiam || 0)) }}</span>
          </div>

          <v-divider class="my-4 border-dashed"></v-divider>

          <div class="d-flex justify-space-between align-center mb-6">
            <span class="text-h6 font-weight-bold">Tổng thanh toán:</span>
            <span class="text-h5 font-weight-black text-primary">{{ formatCurrency(selectedOrder?.tongTienSauGiam)
            }}</span>
          </div>

          <p class="text-subtitle-2 font-weight-bold mb-3">Thanh toán</p>
          <v-btn-toggle v-model="checkoutData.paymentMethod" mandatory color="primary" class="d-flex gap-2 mb-6"
            style="height: 54px;">
            <v-btn value="CASH" variant="outlined" class="flex-grow-1 rounded-lg">Tiền mặt</v-btn>
            <v-btn value="TRANSFER" variant="outlined" class="flex-grow-1 rounded-lg">Chuyển khoản</v-btn>
          </v-btn-toggle>

          <v-text-field v-if="checkoutData.paymentMethod === 'CASH'" v-model.number="checkoutData.receivedAmount"
            label="Tiền khách đưa" variant="outlined" type="number" class="mb-4"></v-text-field>

          <div v-if="checkoutData.paymentMethod === 'CASH'" class="d-flex justify-space-between mb-6 text-h6">
            <span>Tiền thừa:</span>
            <span class="font-weight-bold text-success">{{ formatCurrency(Math.max(0, checkoutData.receivedAmount -
              (selectedOrder?.tongTienSauGiam || 0))) }}</span>
          </div>

          <v-textarea v-model="checkoutData.note" label="Ghi chú" variant="outlined" rows="2" class="mb-6"></v-textarea>

          <v-btn block color="primary" height="64" class="text-h6 font-weight-bold rounded-xl" :loading="isProcessing"
            :disabled="!selectedOrder?.listsHoaDonChiTiet?.length" @click="handleCheckout">XÁC NHẬN THANH TOÁN</v-btn>
        </div>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
.pos-container {
  height: calc(100vh - 80px); /* Tối đa hóa chiều cao màn hình */
  overflow: hidden;
  background: #f1f5f9;
}

.main-column {
  height: 100%;
  overflow-y: auto;
  border-radius: 0;
  background: #f8fafc;
}

.side-column {
  height: 100%;
  overflow-y: auto;
  background: white;
  box-shadow: -4px 0 15px rgba(0, 0, 0, 0.03);
}

.gap-2 {
  gap: 8px;
}

.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 100;
  background: white;
  max-height: 450px;
  overflow-y: auto;
}

.search-card {
  border-radius: 12px !important;
  transition: all 0.3s ease;
}

.search-card:focus-within {
  border-color: rgb(var(--v-theme-primary)) !important;
  box-shadow: 0 0 0 3px rgba(var(--v-theme-primary), 0.1) !important;
}

.qty-control {
  background: #f1f5f9;
  border-radius: 50px;
  padding: 2px 4px;
  display: inline-flex !important;
  align-items: center;
  min-width: 100px;
  justify-content: space-between;
}

.qty-control span {
  font-size: 0.9rem;
  font-weight: 700;
  min-width: 24px;
  text-align: center;
}

.cart-card {
  border-radius: 16px !important;
  border: 1px solid #e2e8f0 !important;
}

.payment-details {
  border-radius: 20px !important;
  background: #f8fafc;
}

.hover-error:hover {
  color: rgb(var(--v-theme-error)) !important;
  background: rgba(var(--v-theme-error), 0.1);
}

/* Tối ưu hóa bảng POS */
.pos-table :deep(th) {
  text-transform: none !important;
  color: #64748b !important;
  font-weight: 700 !important;
  font-size: 0.8rem !important;
  background: #f8fafc !important;
}

/* Custom Scrollbar */
.main-column::-webkit-scrollbar,
.side-column::-webkit-scrollbar,
.search-results::-webkit-scrollbar {
  width: 5px;
}

.main-column::-webkit-scrollbar-thumb,
.side-column::-webkit-scrollbar-thumb,
.search-results::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}

.main-column::-webkit-scrollbar-track,
.side-column::-webkit-scrollbar-track {
  background: transparent;
}
</style>
