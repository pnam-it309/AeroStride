<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import CartDrawer from '@/components/shared/CartDrawer.vue';
import { dichVuDatHang } from '@/services/public/dichVuDatHang';

const router = useRouter();
const loading = ref(true);
const orders = ref([]);
const activeTab = ref('');

const tabs = [
    { label: 'Tất cả', value: '' },
    { label: 'Chờ xác nhận', value: 'CHO_XAC_NHAN' },
    { label: 'Đã xác nhận', value: 'XAC_NHAN' },
    { label: 'Đang giao', value: 'DANG_GIAO' },
    { label: 'Hoàn thành', value: 'HOAN_THANH' },
    { label: 'Đã hủy', value: 'DA_HUY' },
];

const statusColorMap = {
    'CHO_XAC_NHAN': 'orange',
    'XAC_NHAN': 'blue',
    'CHO_GIAO': 'cyan',
    'DANG_GIAO': 'indigo',
    'HOAN_THANH': 'green',
    'DA_HUY': 'red',
    'HOAN_DON': 'grey',
};

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const formatDate = (timestamp) => {
    if (!timestamp) return '';
    return new Intl.DateTimeFormat('vi-VN', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    }).format(new Date(timestamp));
};

const fetchOrders = async () => {
    loading.value = true;
    try {
        orders.value = await dichVuDatHang.layDonHangCuaToi(activeTab.value);
    } catch (error) {
        console.error('Error fetching orders:', error);
    } finally {
        loading.value = false;
    }
};

watch(activeTab, () => fetchOrders());

onMounted(() => fetchOrders());

const goToDetail = (id) => {
    router.push(`/my-orders/${id}`);
};
</script>

<template>
    <div class="my-orders-page bg-grey-lighten-4 min-vh-100">
        <MainHeader />
        <div class="header-spacing" style="height: 104px;"></div>

        <v-container class="py-10" style="max-width: 900px">
            <h1 class="text-h4 font-weight-black mb-2">Đơn hàng của tôi</h1>
            <p class="text-body-2 text-grey-darken-1 mb-8">Theo dõi tất cả đơn hàng của bạn tại đây</p>

            <!-- Status Tabs -->
            <div class="order-tabs mb-8">
                <v-chip-group v-model="activeTab" mandatory selected-class="active-tab">
                    <v-chip
                        v-for="tab in tabs"
                        :key="tab.value"
                        :value="tab.value"
                        variant="outlined"
                        class="tab-chip font-weight-bold text-none"
                        rounded="pill"
                    >
                        {{ tab.label }}
                    </v-chip>
                </v-chip-group>
            </div>

            <!-- Loading -->
            <div v-if="loading" class="text-center py-16">
                <v-progress-circular indeterminate color="black" size="40"></v-progress-circular>
            </div>

            <!-- Orders List -->
            <div v-else-if="orders.length > 0">
                <TransitionGroup name="order-card" tag="div">
                    <v-card
                        v-for="order in orders"
                        :key="order.id"
                        class="order-card rounded-xl mb-4 pa-0"
                        elevation="0"
                        @click="goToDetail(order.id)"
                    >
                        <!-- Card Header -->
                        <div class="order-card-header d-flex align-center justify-space-between px-6 py-4">
                            <div class="d-flex align-center gap-4">
                                <v-icon size="20" color="grey-darken-1">mdi-receipt-text-outline</v-icon>
                                <div>
                                    <span class="text-body-2 font-weight-black">{{ order.maHoaDon || 'Đang tạo...' }}</span>
                                    <span class="text-caption text-grey ml-3">{{ formatDate(order.ngayTao) }}</span>
                                </div>
                            </div>
                            <v-chip
                                :color="statusColorMap[order.trangThai] || 'grey'"
                                variant="tonal"
                                size="small"
                                class="font-weight-bold"
                            >
                                {{ order.trangThaiDisplay }}
                            </v-chip>
                        </div>

                        <v-divider />

                        <!-- Products Preview -->
                        <div class="px-6 py-4">
                            <div v-for="(item, i) in (order.items || []).slice(0, 2)" :key="i" class="order-product d-flex align-center gap-4 py-2">
                                <div class="product-thumb">
                                    <v-img v-if="item.hinhAnh" :src="item.hinhAnh" cover width="56" height="56" class="rounded-lg"></v-img>
                                    <div v-else class="thumb-placeholder rounded-lg">
                                        <v-icon size="20" color="grey-lighten-2">mdi-shoe-sneaker</v-icon>
                                    </div>
                                </div>
                                <div class="flex-grow-1">
                                    <p class="text-body-2 font-weight-bold mb-0">{{ item.tenSanPham }}</p>
                                    <p class="text-caption text-grey mb-0">{{ item.tenMauSac }} / {{ item.tenKichThuoc }} · x{{ item.soLuong }}</p>
                                </div>
                                <span class="text-body-2 font-weight-bold">{{ formatPrice(item.thanhTien) }}</span>
                            </div>
                            <p v-if="order.items && order.items.length > 2" class="text-caption text-grey mt-2">
                                +{{ order.items.length - 2 }} sản phẩm khác
                            </p>
                        </div>

                        <v-divider />

                        <!-- Card Footer -->
                        <div class="d-flex align-center justify-space-between px-6 py-4">
                            <span class="text-body-2 text-grey-darken-1">{{ order.items?.length || 0 }} sản phẩm</span>
                            <div class="d-flex align-center gap-2">
                                <span class="text-body-2 text-grey-darken-1">Tổng:</span>
                                <span class="text-body-1 font-weight-black">{{ formatPrice(order.tongTienSauGiam) }}</span>
                            </div>
                        </div>
                    </v-card>
                </TransitionGroup>
            </div>

            <!-- Empty State -->
            <div v-else class="empty-state text-center py-16">
                <v-icon size="80" color="grey-lighten-2" class="mb-6">mdi-package-variant-closed</v-icon>
                <h3 class="text-h6 font-weight-bold text-grey-darken-1 mb-2">Chưa có đơn hàng nào</h3>
                <p class="text-body-2 text-grey mb-6">Hãy bắt đầu mua sắm để tạo đơn hàng đầu tiên!</p>
                <v-btn color="black" rounded="pill" class="font-weight-bold text-none" @click="router.push('/shoes')">
                    Khám phá sản phẩm
                </v-btn>
            </div>
        </v-container>

        <CartDrawer />
        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.my-orders-page {
    padding-top: 64px;
}

/* Tabs */
.order-tabs {
    .tab-chip {
        border-color: #e0e0e0;
        font-size: 0.85rem;
        &:hover { border-color: #999; }
    }
}

:deep(.active-tab) {
    background: #111 !important;
    color: #fff !important;
    border-color: #111 !important;
}

/* Order Card */
.order-card {
    border: 1px solid #f0f0f0;
    cursor: pointer;
    transition: all 0.3s ease;
    background: #fff;

    &:hover {
        border-color: #ddd;
        box-shadow: 0 4px 20px rgba(0,0,0,0.06);
        transform: translateY(-2px);
    }
}

.order-card-header {
    background: #fafafa;
}

.thumb-placeholder {
    width: 56px;
    height: 56px;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* Transitions */
.order-card-enter-active { transition: all 0.3s ease-out; }
.order-card-leave-active { transition: all 0.3s ease-in; }
.order-card-enter-from { opacity: 0; transform: translateY(20px); }
.order-card-leave-to { opacity: 0; transform: translateY(-20px); }

.min-vh-100 { min-height: 100vh; }
</style>
