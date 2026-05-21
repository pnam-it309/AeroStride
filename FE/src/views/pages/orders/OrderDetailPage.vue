<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import CartDrawer from '@/components/shared/CartDrawer.vue';
import { dichVuDatHang } from '@/services/public/dichVuDatHang';

const route = useRoute();
const router = useRouter();
const order = ref(null);
const loading = ref(true);
const cancelLoading = ref(false);
const showCancelDialog = ref(false);

const statusColorMap = {
    'CHO_XAC_NHAN': 'orange',
    'XAC_NHAN': 'blue',
    'CHO_GIAO': 'cyan',
    'DANG_GIAO': 'indigo',
    'HOAN_THANH': 'green',
    'DA_HUY': 'red',
    'HOAN_DON': 'grey',
};

const statusIconMap = {
    'CHO_XAC_NHAN': 'mdi-clock-outline',
    'XAC_NHAN': 'mdi-check-circle-outline',
    'CHO_GIAO': 'mdi-package-variant',
    'DANG_GIAO': 'mdi-truck-delivery-outline',
    'HOAN_THANH': 'mdi-check-decagram',
    'DA_HUY': 'mdi-close-circle-outline',
    'HOAN_DON': 'mdi-undo-variant',
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

const canCancel = computed(() => {
    return order.value?.trangThai === 'CHO_XAC_NHAN';
});

const fetchOrder = async () => {
    loading.value = true;
    try {
        order.value = await dichVuDatHang.layChiTietDonHang(route.params.id);
    } catch (error) {
        console.error('Error fetching order:', error);
    } finally {
        loading.value = false;
    }
};

const handleCancel = async () => {
    cancelLoading.value = true;
    try {
        await dichVuDatHang.huyDonHang(route.params.id);
        showCancelDialog.value = false;
        await fetchOrder();
    } catch (error) {
        console.error('Error cancelling order:', error);
        alert(error.response?.data?.message || 'Hủy đơn hàng thất bại');
    } finally {
        cancelLoading.value = false;
    }
};

onMounted(() => fetchOrder());
</script>

<template>
    <div class="order-detail-page bg-grey-lighten-4 min-vh-100">
        <MainHeader />
        <div class="header-spacing" style="height: 104px;"></div>

        <v-container class="py-10" style="max-width: 900px">
            <!-- Back Button -->
            <v-btn variant="text" class="font-weight-bold text-none mb-6" @click="router.push('/my-orders')">
                <v-icon class="mr-2">mdi-arrow-left</v-icon> Quay lại đơn hàng
            </v-btn>

            <div v-if="loading" class="text-center py-16">
                <v-progress-circular indeterminate color="black" size="40"></v-progress-circular>
            </div>

            <div v-else-if="order">
                <!-- Order Header -->
                <v-card class="rounded-xl mb-6 pa-0" elevation="0">
                    <div class="order-detail-header px-8 py-6">
                        <div class="d-flex align-center justify-space-between flex-wrap gap-4">
                            <div>
                                <p class="text-caption text-grey mb-1">Mã đơn hàng</p>
                                <h1 class="text-h5 font-weight-black">{{ order.maHoaDon || 'Đang tạo...' }}</h1>
                                <p class="text-caption text-grey mt-1">Đặt ngày {{ formatDate(order.ngayTao) }}</p>
                            </div>
                            <div class="d-flex align-center gap-3">
                                <v-chip
                                    :color="statusColorMap[order.trangThai] || 'grey'"
                                    variant="tonal"
                                    size="large"
                                    class="font-weight-black"
                                >
                                    <v-icon :icon="statusIconMap[order.trangThai]" class="mr-1" size="18"></v-icon>
                                    {{ order.trangThaiDisplay }}
                                </v-chip>
                                <v-btn
                                    v-if="canCancel"
                                    color="red"
                                    variant="outlined"
                                    rounded="pill"
                                    size="small"
                                    class="font-weight-bold text-none"
                                    @click="showCancelDialog = true"
                                >
                                    Hủy đơn
                                </v-btn>
                            </div>
                        </div>
                    </div>
                </v-card>

                <v-row>
                    <v-col cols="12" md="7">
                        <!-- Products -->
                        <v-card class="rounded-xl mb-6 pa-6" elevation="0">
                            <h3 class="text-subtitle-1 font-weight-black mb-5 d-flex align-center">
                                <v-icon class="mr-2" size="20">mdi-package-variant</v-icon>
                                Sản phẩm ({{ order.items?.length || 0 }})
                            </h3>
                            <div v-for="(item, i) in order.items" :key="i" class="detail-product d-flex align-center gap-4 py-4">
                                <div class="detail-thumb">
                                    <v-img v-if="item.hinhAnh" :src="item.hinhAnh" cover width="72" height="72" class="rounded-lg"></v-img>
                                    <div v-else class="thumb-placeholder rounded-lg">
                                        <v-icon size="28" color="grey-lighten-2">mdi-shoe-sneaker</v-icon>
                                    </div>
                                </div>
                                <div class="flex-grow-1">
                                    <p class="text-body-2 font-weight-bold mb-1">{{ item.tenSanPham }}</p>
                                    <p class="text-caption text-grey mb-0">{{ item.tenMauSac }} / {{ item.tenKichThuoc }}</p>
                                    <p class="text-caption text-grey">{{ formatPrice(item.donGia) }} × {{ item.soLuong }}</p>
                                </div>
                                <span class="text-body-2 font-weight-black">{{ formatPrice(item.thanhTien) }}</span>
                            </div>
                        </v-card>

                        <!-- Status Timeline -->
                        <v-card v-if="order.lichSuTrangThai?.length" class="rounded-xl mb-6 pa-6" elevation="0">
                            <h3 class="text-subtitle-1 font-weight-black mb-5 d-flex align-center">
                                <v-icon class="mr-2" size="20">mdi-timeline-clock-outline</v-icon>
                                Lịch sử đơn hàng
                            </h3>
                            <v-timeline density="compact" side="end" class="timeline-custom">
                                <v-timeline-item
                                    v-for="(ls, i) in order.lichSuTrangThai"
                                    :key="i"
                                    :dot-color="statusColorMap[ls.trangThai] || 'grey'"
                                    size="small"
                                >
                                    <div>
                                        <p class="text-body-2 font-weight-bold mb-0">{{ ls.trangThaiDisplay }}</p>
                                        <p class="text-caption text-grey mb-0">{{ formatDate(ls.thoiGian) }}</p>
                                        <p v-if="ls.ghiChu" class="text-caption text-grey-darken-1">{{ ls.ghiChu }}</p>
                                    </div>
                                </v-timeline-item>
                            </v-timeline>
                        </v-card>
                    </v-col>

                    <v-col cols="12" md="5">
                        <!-- Shipping Info -->
                        <v-card class="rounded-xl mb-6 pa-6" elevation="0">
                            <h3 class="text-subtitle-1 font-weight-black mb-4 d-flex align-center">
                                <v-icon class="mr-2" size="20">mdi-truck-outline</v-icon>
                                Thông tin giao hàng
                            </h3>
                            <div class="info-row mb-3">
                                <span class="text-caption text-grey">Người nhận</span>
                                <p class="text-body-2 font-weight-bold mb-0">{{ order.tenNguoiNhan }}</p>
                            </div>
                            <div class="info-row mb-3">
                                <span class="text-caption text-grey">Điện thoại</span>
                                <p class="text-body-2 font-weight-bold mb-0">{{ order.soDienThoaiNguoiNhan }}</p>
                            </div>
                            <div class="info-row mb-3">
                                <span class="text-caption text-grey">Địa chỉ</span>
                                <p class="text-body-2 font-weight-bold mb-0">{{ order.diaChiNguoiNhan }}</p>
                            </div>
                            <div v-if="order.ngayDuKienNhan" class="info-row">
                                <span class="text-caption text-grey">Dự kiến nhận hàng</span>
                                <p class="text-body-2 font-weight-bold mb-0">{{ formatDate(order.ngayDuKienNhan) }}</p>
                            </div>
                        </v-card>

                        <!-- Payment Summary -->
                        <v-card class="rounded-xl pa-6" elevation="0">
                            <h3 class="text-subtitle-1 font-weight-black mb-4 d-flex align-center">
                                <v-icon class="mr-2" size="20">mdi-receipt-text-outline</v-icon>
                                Tóm tắt thanh toán
                            </h3>
                            <div class="d-flex justify-space-between mb-2">
                                <span class="text-body-2 text-grey-darken-1">Tạm tính</span>
                                <span class="text-body-2 font-weight-bold">{{ formatPrice(order.tongTien) }}</span>
                            </div>
                            <div class="d-flex justify-space-between mb-2">
                                <span class="text-body-2 text-grey-darken-1">Phí vận chuyển</span>
                                <span class="text-body-2 font-weight-bold" :class="{ 'text-green': !order.phiVanChuyen || order.phiVanChuyen == 0 }">
                                    {{ !order.phiVanChuyen || order.phiVanChuyen == 0 ? 'Miễn phí' : formatPrice(order.phiVanChuyen) }}
                                </span>
                            </div>
                            <div v-if="order.tienGiam > 0" class="d-flex justify-space-between mb-2">
                                <span class="text-body-2 text-green-darken-2">Giảm giá</span>
                                <span class="text-body-2 font-weight-bold text-green-darken-2">-{{ formatPrice(order.tienGiam) }}</span>
                            </div>
                            <div v-if="order.maVoucher" class="d-flex align-center gap-2 mb-2">
                                <v-icon size="14" color="orange">mdi-ticket-percent</v-icon>
                                <span class="text-caption text-grey">Voucher: {{ order.maVoucher }}</span>
                            </div>
                            <v-divider class="my-3" />
                            <div class="d-flex justify-space-between align-center">
                                <span class="text-body-1 font-weight-black">Tổng cộng</span>
                                <span class="text-h6 font-weight-black">{{ formatPrice(order.tongTienSauGiam) }}</span>
                            </div>
                            <div class="mt-3">
                                <v-chip variant="tonal" size="small" class="font-weight-bold">
                                    <v-icon size="14" class="mr-1">mdi-credit-card-outline</v-icon>
                                    {{ order.phuongThucThanhToan === 'COD' ? 'Thanh toán khi nhận hàng' : order.phuongThucThanhToan }}
                                </v-chip>
                            </div>
                        </v-card>
                    </v-col>
                </v-row>
            </div>
        </v-container>

        <!-- Cancel Confirmation Dialog -->
        <v-dialog v-model="showCancelDialog" max-width="420">
            <v-card class="rounded-xl pa-6">
                <div class="text-center mb-4">
                    <v-icon size="56" color="red-lighten-1" class="mb-4">mdi-alert-circle-outline</v-icon>
                    <h3 class="text-h6 font-weight-black mb-2">Hủy đơn hàng?</h3>
                    <p class="text-body-2 text-grey">Bạn có chắc chắn muốn hủy đơn hàng <strong>{{ order?.maHoaDon }}</strong>? Thao tác này không thể hoàn tác.</p>
                </div>
                <div class="d-flex gap-3 justify-center">
                    <v-btn variant="outlined" rounded="pill" class="font-weight-bold text-none px-8" @click="showCancelDialog = false">
                        Quay lại
                    </v-btn>
                    <v-btn color="red" rounded="pill" class="font-weight-bold text-none px-8" :loading="cancelLoading" @click="handleCancel">
                        Xác nhận hủy
                    </v-btn>
                </div>
            </v-card>
        </v-dialog>

        <CartDrawer />
        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.order-detail-page {
    padding-top: 64px;
}

.order-detail-header {
    background: #fafafa;
    border-bottom: 1px solid #f0f0f0;
}

.v-card {
    border: 1px solid #f0f0f0;
}

.detail-product + .detail-product {
    border-top: 1px solid #f5f5f5;
}

.thumb-placeholder {
    width: 72px;
    height: 72px;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
}

.info-row {
    padding: 4px 0;
}

.timeline-custom {
    :deep(.v-timeline-item__body) {
        padding-top: 2px;
    }
}

.min-vh-100 { min-height: 100vh; }
</style>
