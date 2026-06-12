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
    CHO_XAC_NHAN: 'orange',
    XAC_NHAN: 'blue',
    CHO_GIAO: 'cyan',
    DANG_GIAO: 'indigo',
    HOAN_THANH: 'green',
    DA_HUY: 'red',
    HOAN_DON: 'grey'
};

const statusIconMap = {
    CHO_XAC_NHAN: 'mdi-clock-outline',
    XAC_NHAN: 'mdi-check-circle-outline',
    CHO_GIAO: 'mdi-package-variant',
    DANG_GIAO: 'mdi-truck-delivery-outline',
    HOAN_THANH: 'mdi-check-decagram',
    DA_HUY: 'mdi-close-circle-outline',
    HOAN_DON: 'mdi-undo-variant'
};

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const formatDate = (timestamp) => {
    if (!timestamp) return '';
    return new Intl.DateTimeFormat('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
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
        <div class="header-spacing" style="height: 104px"></div>

        <v-container class="py-8" style="max-width: 1000px">
            <!-- Back Button -->
            <v-btn variant="text" class="font-weight-bold text-none mb-6" @click="router.push('/my-orders')">
                <v-icon class="mr-2">mdi-arrow-left</v-icon> Quay lại đơn hàng
            </v-btn>

            <div v-if="loading" class="text-center py-16">
                <v-progress-circular indeterminate color="black" size="40"></v-progress-circular>
            </div>

            <div v-else-if="order">
                <!-- Order Header -->
                <v-card class="rounded-xl mb-6 pa-0 gradient-header" elevation="0">
                    <div class="order-detail-header px-8 py-6 position-relative z-index-1">
                        <div class="d-flex align-center justify-space-between flex-wrap gap-4">
                            <div>
                                <p class="text-caption text-blue-lighten-4 mb-1 text-uppercase letter-spacing-1 font-weight-medium">Mã đơn hàng</p>
                                <h1 class="text-h5 font-weight-bold text-white mb-2">{{ order.maHoaDon || 'Đang tạo...' }}</h1>
                                <div class="d-flex align-center">
                                    <v-icon size="16" color="blue-lighten-3" class="mr-2">mdi-calendar-clock</v-icon>
                                    <span class="text-body-2 text-blue-lighten-4">Đặt lúc {{ formatDate(order.ngayTao) }}</span>
                                </div>
                            </div>
                            <div class="d-flex flex-column align-end gap-3">
                                <v-chip
                                    :color="order.trangThai === 'DA_HUY' ? 'red-lighten-1' : 'white'"
                                    :variant="order.trangThai === 'DA_HUY' ? 'flat' : 'elevated'"
                                    size="large"
                                    class="font-weight-bold elevation-2"
                                    :class="{'text-blue-darken-4': order.trangThai !== 'DA_HUY'}"
                                >
                                    <v-icon :icon="statusIconMap[order.trangThai]" class="mr-2" size="24"></v-icon>
                                    {{ order.trangThaiDisplay }}
                                </v-chip>
                                <v-btn
                                    v-if="canCancel"
                                    color="white"
                                    variant="outlined"
                                    rounded="pill"
                                    class="font-weight-medium text-none mt-2 btn-hover-scale"
                                    @click="showCancelDialog = true"
                                >
                                    <v-icon class="mr-2" size="18">mdi-close-circle</v-icon>
                                    Hủy đơn hàng
                                </v-btn>
                            </div>
                        </div>
                    </div>
                </v-card>

                <v-row class="bento-grid">
                    <v-col cols="12" lg="8" md="7" class="d-flex flex-column gap-6">
                        <!-- Products -->
                        <v-card class="premium-card rounded-xl mb-6 pa-6 h-100 d-flex flex-column" elevation="0">
                            <div class="d-flex align-center mb-6">
                                <div class="icon-blob bg-blue-lighten-5 mr-4">
                                    <v-icon color="blue-darken-3" size="24">mdi-package-variant</v-icon>
                                </div>
                                <h3 class="text-subtitle-1 font-weight-bold text-blue-darken-4 mb-0">
                                    Sản phẩm ({{ order.items?.length || 0 }})
                                </h3>
                            </div>
                            <div v-for="(item, i) in order.items" :key="i" class="detail-product d-flex align-center gap-4 py-4">
                                <div class="detail-thumb">
                                    <v-img :src="item.hinhAnh || 'https://via.placeholder.com/150?text=Sản+Phẩm'" cover width="72" height="72" class="rounded-lg"></v-img>
                                </div>
                                <div class="flex-grow-1">
                                    <p class="text-body-2 font-weight-medium mb-1">{{ item.tenSanPham }}</p>
                                    <p class="text-caption text-grey mb-0">{{ item.tenMauSac }} / {{ item.tenKichThuoc }}</p>
                                    <p class="text-caption text-grey">{{ formatPrice(item.donGia) }} × {{ item.soLuong }}</p>
                                </div>
                                <span class="text-body-2 font-weight-bold">{{ formatPrice(item.thanhTien) }}</span>
                            </div>
                        </v-card>

                        <!-- Status Timeline -->
                        <v-card v-if="order.lichSuTrangThai?.length" class="premium-card rounded-xl mb-6 pa-6 h-100" elevation="0">
                            <div class="d-flex align-center mb-6">
                                <div class="icon-blob bg-indigo-lighten-5 mr-4">
                                    <v-icon color="indigo-darken-3" size="24">mdi-timeline-clock-outline</v-icon>
                                </div>
                                <h3 class="text-subtitle-1 font-weight-bold text-indigo-darken-4 mb-0">
                                    Lịch sử đơn hàng
                                </h3>
                            </div>
                            <v-timeline density="compact" side="end" class="timeline-custom">
                                <v-timeline-item
                                    v-for="(ls, i) in order.lichSuTrangThai"
                                    :key="i"
                                    :dot-color="statusColorMap[ls.trangThai] || 'grey'"
                                    size="small"
                                >
                                    <div>
                                        <p class="text-body-2 font-weight-medium mb-0">{{ ls.trangThaiDisplay }}</p>
                                        <p class="text-caption text-grey mb-0">{{ formatDate(ls.thoiGian) }}</p>
                                        <p v-if="ls.ghiChu" class="text-caption text-grey-darken-1">{{ ls.ghiChu }}</p>
                                    </div>
                                </v-timeline-item>
                            </v-timeline>
                        </v-card>
                    </v-col>

                    <v-col cols="12" lg="4" md="5" class="d-flex flex-column gap-6">
                        <!-- Shipping Info -->
                        <v-card class="premium-card rounded-xl mb-6 pa-6 h-100 d-flex flex-column" elevation="0">
                            <div class="d-flex align-center mb-6">
                                <div class="icon-blob bg-teal-lighten-5 mr-4">
                                    <v-icon color="teal-darken-3" size="24">mdi-truck-outline</v-icon>
                                </div>
                                <h3 class="text-subtitle-1 font-weight-bold text-teal-darken-4 mb-0">
                                    Thông tin giao hàng
                                </h3>
                            </div>
                            <div class="info-row mb-3">
                                <span class="text-caption text-grey">Người nhận</span>
                                <p class="text-body-2 font-weight-medium mb-0">{{ order.tenNguoiNhan }}</p>
                            </div>
                            <div class="info-row mb-3">
                                <span class="text-caption text-grey">Điện thoại</span>
                                <p class="text-body-2 font-weight-medium mb-0">{{ order.soDienThoaiNguoiNhan }}</p>
                            </div>
                            <div class="info-row mb-3">
                                <span class="text-caption text-grey">Địa chỉ</span>
                                <p class="text-body-2 font-weight-medium mb-0">{{ order.diaChiNguoiNhan }}</p>
                            </div>
                            <div v-if="order.ngayDuKienNhan" class="info-row">
                                <span class="text-caption text-grey">Dự kiến nhận hàng</span>
                                <p class="text-body-2 font-weight-medium mb-0">{{ formatDate(order.ngayDuKienNhan) }}</p>
                            </div>
                        </v-card>

                        <!-- Payment Summary -->
                        <v-card class="premium-card rounded-xl pa-6 mb-6 h-100 d-flex flex-column" elevation="0">
                            <div class="d-flex align-center mb-6">
                                <div class="icon-blob bg-orange-lighten-5 mr-4">
                                    <v-icon color="orange-darken-3" size="24">mdi-receipt-text-outline</v-icon>
                                </div>
                                <h3 class="text-subtitle-1 font-weight-bold text-orange-darken-4 mb-0">
                                    Tóm tắt thanh toán
                                </h3>
                            </div>
                            <div class="d-flex justify-space-between mb-2">
                                <span class="text-body-2 text-grey-darken-1">Tạm tính</span>
                                <span class="text-body-2 font-weight-medium">{{ formatPrice(order.tongTien) }}</span>
                            </div>
                            <div class="d-flex justify-space-between mb-2">
                                <span class="text-body-2 text-grey-darken-1">Phí vận chuyển</span>
                                <span
                                    class="text-body-2 font-weight-medium"
                                    :class="{ 'text-green': !order.phiVanChuyen || order.phiVanChuyen == 0 }"
                                >
                                    {{ !order.phiVanChuyen || order.phiVanChuyen == 0 ? 'Miễn phí' : formatPrice(order.phiVanChuyen) }}
                                </span>
                            </div>
                            <div v-if="order.tienGiam > 0" class="d-flex justify-space-between mb-2">
                                <span class="text-body-2 text-green-darken-2">Giảm giá</span>
                                <span class="text-body-2 font-weight-medium text-green-darken-2">-{{ formatPrice(order.tienGiam) }}</span>
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
                    <p class="text-body-2 text-grey">
                        Bạn có chắc chắn muốn hủy đơn hàng <strong>{{ order?.maHoaDon }}</strong
                        >? Thao tác này không thể hoàn tác.
                    </p>
                </div>
                <div class="d-flex gap-3 justify-center">
                    <v-btn variant="outlined" rounded="pill" class="font-weight-bold text-none px-8" @click="showCancelDialog = false">
                        Quay lại
                    </v-btn>
                    <v-btn
                        color="red"
                        rounded="pill"
                        class="font-weight-bold text-none px-8"
                        :loading="cancelLoading"
                        @click="handleCancel"
                    >
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

.premium-card {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    border: 1px solid rgba(0, 0, 0, 0.05);
    background: #ffffff;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03) !important;

    &:hover {
        transform: translateY(-4px);
        box-shadow: 0 12px 30px rgba(0, 0, 0, 0.08) !important;
        border-color: rgba(13, 71, 161, 0.1);
    }
}

.gradient-header {
    background: linear-gradient(135deg, #0d47a1 0%, #1976d2 100%);
    box-shadow: 0 10px 30px rgba(13, 71, 161, 0.2) !important;
    position: relative;
    overflow: hidden;

    &::before {
        content: '';
        position: absolute;
        top: 0;
        right: 0;
        width: 300px;
        height: 300px;
        background: radial-gradient(circle, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0) 70%);
        transform: translate(30%, -30%);
        border-radius: 50%;
    }
}

.z-index-1 {
    z-index: 1;
}

.letter-spacing-1 {
    letter-spacing: 1px;
}

.icon-blob {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.detail-product {
    transition: background 0.2s ease;
    border-radius: 12px;
    padding: 12px;

    &:hover {
        background: #f8fbff;
    }
}

.detail-product + .detail-product {
    border-top: 1px dashed #f0f0f0;
    border-radius: 0;
    padding: 16px 12px;
}

.info-row {
    padding: 10px 12px;
    border-radius: 8px;
    transition: background 0.2s;

    &:hover {
        background: #f8fbff;
    }
}

.timeline-custom {
    :deep(.v-timeline-item__body) {
        padding-top: 2px;
    }
    :deep(.v-timeline-item__dot) {
        box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.8);
    }
}

.btn-hover-scale {
    transition: transform 0.2s;
    &:hover {
        transform: scale(1.05);
    }
}

.min-vh-100 {
    min-height: 100vh;
}
</style>
