<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import { useNotifications } from '@/services/notificationService';

// Import Components
import OrderTabs from './components/OrderTabs.vue';
import ProductPicker from './components/ProductPicker.vue';
import CartTable from './components/CartTable.vue';
import CustomerSelector from './components/CustomerSelector.vue';
import CheckoutPanel from './components/CheckoutPanel.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';

const { addNotification } = useNotifications();

// State
const loading = ref(false);
const orders = ref([]);
const activeOrderIndex = ref(0);
const vouchers = ref([]);
const isProcessing = ref(false);

const checkoutData = ref({
    paymentMethod: 'CASH',
    receivedAmount: 0,
    note: ''
});

// Confirmation Logic
const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    color: 'primary',
    action: null,
    loading: false
});

const selectedOrder = computed(() => orders.value[activeOrderIndex.value] || null);

// Lifecycle
onMounted(async () => {
    loading.value = true;
    try {
        const data = await dichVuDonHang.layDonHangCho();
        orders.value = data || [];
        if (orders.value.length === 0) await createNewOrder();
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể kết nối máy chủ', color: 'error' });
    } finally {
        loading.value = false;
    }
});

// Watchers
watch(() => selectedOrder.value?.id, (id) => {
    if (id) fetchVouchers();
});

// Logic: Hóa đơn
const createNewOrder = async () => {
    if (orders.value.length >= 5) {
        addNotification({ title: 'Giới hạn', subtitle: 'Tối đa 5 hóa đơn chờ', color: 'warning' });
        return;
    }
    try {
        const newOrder = await dichVuDonHang.taoDonHang();
        orders.value.push(newOrder);
        activeOrderIndex.value = orders.value.length - 1;
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tạo đơn mới', color: 'error' });
    }
};

const closeOrder = (orderId, index) => {
    confirmDialog.value = {
        show: true,
        title: 'Xác nhận xóa hóa đơn',
        message: 'Bạn có chắc chắn muốn xóa hóa đơn chờ này không? Dữ liệu giỏ hàng sẽ bị mất.',
        color: 'error',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                await dichVuDonHang.xoaDonHang(orderId);
                orders.value.splice(index, 1);
                activeOrderIndex.value = Math.max(0, orders.value.length - 1);
                if (orders.value.length === 0) await createNewOrder();
                confirmDialog.value.show = false;
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: 'Không thể xóa hóa đơn', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

// Logic: Sản phẩm
const onAddProduct = (product) => {
    confirmDialog.value = {
        show: true,
        title: 'Xác nhận thêm sản phẩm',
        message: `Bạn có chắc chắn muốn thêm [${product.tenSanPham}] vào giỏ hàng?`,
        color: 'success',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                const updated = await dichVuDonHang.addSanPham(selectedOrder.value.id, {
                    idChiTietSanPham: product.id,
                    soLuong: 1
                });
                updateOrderInList(updated);
                addNotification({ title: 'Thêm thành công', subtitle: product.tenSanPham, color: 'success' });
                confirmDialog.value.show = false;
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: 'Sản phẩm không đủ số lượng hoặc lỗi hệ thống', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

const onUpdateQty = async (item, delta) => {
    const newQty = item.soLuong + delta;
    if (newQty < 1) return;
    try {
        const updated = await dichVuDonHang.updateSoLuong(selectedOrder.value.id, item.id, newQty);
        updateOrderInList(updated);
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: 'Số lượng vượt quá tồn kho', color: 'error' });
    }
};

const onRemoveItem = (item) => {
    confirmDialog.value = {
        show: true,
        title: 'Xác nhận xóa sản phẩm',
        message: `Bạn có chắc chắn muốn xóa [${item.tenSanPham}] khỏi giỏ hàng?`,
        color: 'warning',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                await dichVuDonHang.removeSanPham(selectedOrder.value.id, item.id);
                const data = await dichVuDonHang.layDonHangCho();
                orders.value = data || [];
                confirmDialog.value.show = false;
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: 'Không thể xóa sản phẩm', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

// Logic: Khách hàng & Voucher
const onSelectCustomer = async (customer) => {
    try {
        const updated = await dichVuDonHang.setKhachHang(selectedOrder.value.id, customer.id);
        updateOrderInList(updated);
        fetchVouchers();
    } catch (e) {}
};

const onRemoveCustomer = async () => {
    try {
        const updated = await dichVuDonHang.setKhachHang(selectedOrder.value.id, null);
        updateOrderInList(updated);
    } catch (e) {}
};

const fetchVouchers = async () => {
    if (!selectedOrder.value) return;
    vouchers.value = await dichVuDonHang.getVouchers(selectedOrder.value.tongTien || 0);
};

const onApplyVoucher = async (voucherId) => {
    try {
        const updated = await dichVuDonHang.setVoucher(selectedOrder.value.id, voucherId);
        updateOrderInList(updated);
    } catch (e) {}
};

// Logic: Thanh toán
const onCheckout = () => {
    if (checkoutData.value.paymentMethod === 'CASH' && checkoutData.value.receivedAmount < selectedOrder.value.tongTienSauGiam) {
        addNotification({ title: 'Cảnh báo', subtitle: 'Khách chưa đưa đủ tiền', color: 'warning' });
        return;
    }

    confirmDialog.value = {
        show: true,
        title: 'Xác nhận thanh toán',
        message: `Bạn xác nhận thanh toán hóa đơn với tổng tiền [${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedOrder.value.tongTienSauGiam)}]?`,
        color: 'primary',
        action: async () => {
            confirmDialog.value.loading = true;
            isProcessing.value = true;
            try {
                const isCash = checkoutData.value.paymentMethod === 'CASH';
                const payload = {
                    idKhachHang: selectedOrder.value.idKhachHang,
                    idPhieuGiamGia: selectedOrder.value.idPhieuGiamGia,
                    tongTien: selectedOrder.value.tongTien,
                    tongTienSauGiam: selectedOrder.value.tongTienSauGiam,
                    loaiDon: 'TAI_QUAY',
                    ghiChu: checkoutData.value.note,
                    tienMat: isCash ? selectedOrder.value.tongTienSauGiam : 0,
                    tienChuyenKhoan: isCash ? 0 : selectedOrder.value.tongTienSauGiam
                };
                await dichVuDonHang.checkout(selectedOrder.value.id, payload);
                addNotification({ title: 'Thành công', subtitle: 'Hóa đơn đã được thanh toán', color: 'success' });
                
                orders.value.splice(activeOrderIndex.value, 1);
                if (orders.value.length === 0) await createNewOrder();
                activeOrderIndex.value = 0;
                
                // Reset checkout data
                checkoutData.value.receivedAmount = 0;
                checkoutData.value.note = '';
                confirmDialog.value.show = false;
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: 'Thanh toán thất bại', color: 'error' });
            } finally {
                isProcessing.value = false;
                confirmDialog.value.loading = false;
            }
        }
    };
};

// Helpers
const updateOrderInList = (updated) => {
    const idx = orders.value.findIndex(o => o.id === updated.id);
    if (idx !== -1) orders.value[idx] = updated;
};
</script>

<template>
    <v-container fluid class="pos-wrapper pa-0">
        <v-row no-gutters class="fill-height">
            <!-- Cột trái: Quản lý giỏ hàng & Sản phẩm -->
            <v-col cols="12" lg="8" class="main-panel border-r pa-6">
                <!-- Tabs & Quick Actions -->
                <OrderTabs 
                    :orders="orders" 
                    :active-index="activeOrderIndex"
                    @select="idx => activeOrderIndex = idx"
                    @create="createNewOrder"
                    @close="closeOrder"
                />

                <!-- Search Bar -->
                <div class="mb-6">
                    <ProductPicker 
                        :active-order-id="selectedOrder?.id"
                        @add-product="onAddProduct"
                    />
                </div>

                <!-- Shopping Cart Table -->
                <div class="cart-container">
                    <CartTable 
                        :items="selectedOrder?.listsHoaDonChiTiet"
                        @update-qty="onUpdateQty"
                        @remove="onRemoveItem"
                    />
                </div>
            </v-col>

            <!-- Cột phải: Khách hàng & Thanh toán -->
            <v-col cols="12" lg="4" class="side-panel pa-6 bg-white shadow-lg">
                <div v-if="selectedOrder" class="checkout-wrapper d-flex flex-column h-100">
                    <!-- Customer Section -->
                    <div class="mb-6">
                        <CustomerSelector 
                            :selected-customer-name="selectedOrder.tenKhachHang"
                            :active-order-id="selectedOrder.id"
                            @select="onSelectCustomer"
                            @remove="onRemoveCustomer"
                        />
                    </div>

                    <v-divider class="mb-6"></v-divider>

                    <!-- Checkout & Sum -->
                    <CheckoutPanel 
                        :order="selectedOrder"
                        :vouchers="vouchers"
                        :checkout-data="checkoutData"
                        :loading="isProcessing"
                        @apply-voucher="onApplyVoucher"
                        @checkout="onCheckout"
                    />
                </div>
                
                <div v-else class="fill-height d-flex align-center justify-center text-grey">
                    <v-progress-circular indeterminate color="primary"></v-progress-circular>
                </div>
            </v-col>
        </v-row>

        <!-- Confirmation Dialog -->
        <AdminConfirm 
            v-model:show="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :color="confirmDialog.color"
            :loading="confirmDialog.loading"
            @confirm="confirmDialog.action"
        />
    </v-container>
</template>

<style scoped>
.pos-wrapper {
    height: calc(100vh - 64px);
    overflow: hidden;
    background-color: #f1f5f9;
}

.main-panel {
    height: 100%;
    overflow-y: auto;
    background-color: #f8fafc;
}

.side-panel {
    height: 100%;
    overflow-y: auto;
    background-color: white;
    z-index: 2;
}

.cart-container {
    height: calc(100% - 200px);
}

.checkout-wrapper {
    min-height: 100%;
}

/* Custom transitions and shadows */
.shadow-lg {
    box-shadow: -10px 0 25px -5px rgba(0, 0, 0, 0.05) !important;
}

@media (max-width: 1264px) {
    .pos-wrapper {
        height: auto;
        overflow-y: auto;
    }
    .main-panel, .side-panel {
        height: auto;
    }
}
</style>



