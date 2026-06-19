<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import CartDrawer from '@/components/shared/CartDrawer.vue';
import { dichVuDatHang } from '@/services/public/dichVuDatHang';
import { ORDER_STATUS_LABELS, ORDER_STATUS_COLORS, ORDER_STATUS_ICONS } from '@/constants/hoaDonConstants';

const route = useRoute();
const router = useRouter();
const order = ref(null);
const loading = ref(true);
const cancelLoading = ref(false);
const showCancelDialog = ref(false);

const statusColor = (status) => ORDER_STATUS_COLORS[status] || '#757575';
const statusIcon = (status) => ORDER_STATUS_ICONS[status] || 'mdi-help-circle-outline';
const statusLabel = (status) => ORDER_STATUS_LABELS[status] || status;

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

const formatDateFull = (timestamp) => {
    if (!timestamp) return '';
    return new Intl.DateTimeFormat('vi-VN', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
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
    <div class="order-detail-page bg-grey-lighten-4 min-vh-100 pb-12">
        <MainHeader />
        <div class="header-spacing" style="height: 104px"></div>

        <v-container class="py-8" style="max-width: 1100px">
            <!-- Back -->
            <v-btn variant="text" class="font-weight-bold text-none mb-6 back-btn" @click="router.push('/my-orders')">
                <v-icon class="mr-2" size="18">mdi-arrow-left</v-icon>
                Quay lại danh sách đơn hàng
            </v-btn>

            <div v-if="loading" class="text-center py-16">
                <v-progress-circular indeterminate color="blue-darken-4" size="48" width="4"></v-progress-circular>
                <p class="text-body-2 text-grey mt-4">Đang tải thông tin đơn hàng...</p>
            </div>

            <div v-else-if="order">
                <!-- Order Header -->
                <v-card class="order-hero-card rounded-xl mb-8" elevation="0">
                    <div class="hero-gradient pa-8">
                        <div class="d-flex align-center justify-space-between flex-wrap gap-4">
                            <div>
                                <p class="text-overline text-blue-lighten-4 mb-1 font-weight-bold tracking-wide">MÃ ĐƠN HÀNG</p>
                                <h1 class="text-h4 font-weight-bold text-white mb-2">{{ order.maHoaDon || 'Đang tạo...' }}</h1>
                                <div class="d-flex align-center flex-wrap gap-4">
                                    <div class="d-flex align-center">
                                        <v-icon size="16" color="blue-lighten-3" class="mr-2">mdi-calendar-clock</v-icon>
                                        <span class="text-body-2 text-blue-lighten-4">Đặt lúc {{ formatDateFull(order.ngayTao) }}</span>
                                    </div>
                                    <div v-if="order.ngayDuKienNhan" class="d-flex align-center">
                                        <v-icon size="16" color="blue-lighten-3" class="mr-2">mdi-truck-fast-outline</v-icon>
                                        <span class="text-body-2 text-blue-lighten-4">Dự kiến nhận: {{ formatDate(order.ngayDuKienNhan) }}</span>
                                    </div>
                                </div>
                            </div>
                            <div class="text-right">
                                <v-chip
                                    :color="statusColor(order.trangThai)"
                                    variant="flat"
                                    size="large"
                                    class="font-weight-bold status-chip px-4"
                                >
                                    <v-icon :icon="statusIcon(order.trangThai)" class="mr-2" size="22"></v-icon>
                                    {{ statusLabel(order.trangThai) }}
                                </v-chip>
                            </div>
                        </div>
                    </div>
                </v-card>

                <v-row class="content-grid">
                    <!-- Left Column -->
                    <v-col cols="12" lg="8" md="7" class="d-flex flex-column gap-6">
                        <!-- Products -->
                        <v-card class="detail-card rounded-xl" elevation="0">
                            <div class="pa-7">
                                <div class="d-flex align-center mb-6">
                                    <div class="card-icon bg-blue-lighten-5 mr-4">
                                        <v-icon color="blue-darken-3" size="22">mdi-package-variant</v-icon>
                                    </div>
                                    <div>
                                        <h3 class="text-subtitle-1 font-weight-bold mb-0">Sản phẩm đã đặt</h3>
                                        <p class="text-caption text-grey mb-0">{{ order.items?.length || 0 }} sản phẩm</p>
                                    </div>
                                </div>
                                <div v-for="(item, i) in order.items" :key="i"
                                    class="detail-product d-flex align-center gap-4 py-4"
                                    :class="{ 'border-top': i > 0 }">
                                    <div class="detail-thumb-wrapper">
                                        <v-img :src="item.hinhAnh || 'https://via.placeholder.com/150?text=Sản+Phẩm'"
                                            cover width="76" height="76" class="rounded-xl"></v-img>
                                    </div>
                                    <div class="flex-grow-1 min-w-0">
                                        <p class="text-body-1 font-weight-bold mb-1 text-truncate">{{ item.tenSanPham }}</p>
                                        <div class="d-flex align-center flex-wrap gap-3">
                                            <v-chip size="x-small" variant="tonal" color="grey" class="font-weight-medium">
                                                <v-icon size="12" class="mr-1">mdi-palette</v-icon>{{ item.tenMauSac }}
                                            </v-chip>
                                            <v-chip size="x-small" variant="tonal" color="grey" class="font-weight-medium">
                                                <v-icon size="12" class="mr-1">mdi-ruler</v-icon>{{ item.tenKichThuoc }}
                                            </v-chip>
                                            <span class="text-caption text-grey">x{{ item.soLuong }}</span>
                                        </div>
                                        <p class="text-caption text-grey mt-1">{{ formatPrice(item.donGia) }} / sản phẩm</p>
                                    </div>
                                    <span class="text-body-1 font-weight-black text-right flex-shrink-0">{{ formatPrice(item.thanhTien) }}</span>
                                </div>
                            </div>
                        </v-card>

                        <!-- Status Timeline -->
                        <v-card v-if="order.lichSuTrangThai?.length" class="detail-card rounded-xl" elevation="0">
                            <div class="pa-7">
                                <div class="d-flex align-center mb-6">
                                    <div class="card-icon bg-indigo-lighten-5 mr-4">
                                        <v-icon color="indigo-darken-3" size="22">mdi-timeline-clock-outline</v-icon>
                                    </div>
                                    <div>
                                        <h3 class="text-subtitle-1 font-weight-bold mb-0">Lịch sử đơn hàng</h3>
                                        <p class="text-caption text-grey mb-0">Các cập nhật mới nhất về đơn hàng</p>
                                    </div>
                                </div>
                                <v-timeline density="compact" side="end" class="timeline-modern">
                                    <v-timeline-item
                                        v-for="(ls, i) in order.lichSuTrangThai"
                                        :key="i"
                                        size="small"
                                        class="timeline-item-custom"
                                    >
                                        <template v-slot:icon>
                                            <div class="timeline-dot" :style="{ background: statusColor(ls.trangThai) }">
                                                <v-icon size="14" color="white">{{ statusIcon(ls.trangThai) }}</v-icon>
                                            </div>
                                        </template>
                                        <div>
                                            <p class="text-body-2 font-weight-bold mb-0">{{ statusLabel(ls.trangThai) }}</p>
                                            <p class="text-caption text-grey mb-1 d-flex align-center">
                                                <v-icon size="12" class="mr-1">mdi-clock-outline</v-icon>
                                                {{ formatDateFull(ls.thoiGian) }}
                                            </p>
                                            <p v-if="ls.ghiChu" class="text-caption text-grey-darken-1 mb-0 bg-grey-lighten-4 pa-3 rounded-lg mt-1">
                                                <v-icon size="12" class="mr-1">mdi-comment-text-outline</v-icon>
                                                {{ ls.ghiChu }}
                                            </p>
                                        </div>
                                    </v-timeline-item>
                                </v-timeline>
                            </div>
                        </v-card>
                    </v-col>

                    <!-- Right Column -->
                    <v-col cols="12" lg="4" md="5" class="d-flex flex-column gap-6">
                        <!-- Shipping Info -->
                        <v-card class="detail-card rounded-xl" elevation="0">
                            <div class="pa-7">
                                <div class="d-flex align-center mb-6">
                                    <div class="card-icon bg-teal-lighten-5 mr-4">
                                        <v-icon color="teal-darken-3" size="22">mdi-truck-outline</v-icon>
                                    </div>
                                    <h3 class="text-subtitle-1 font-weight-bold mb-0">Thông tin giao hàng</h3>
                                </div>
                                <div class="shipping-details">
                                    <div class="shipping-row">
                                        <div class="shipping-label text-caption text-grey mb-1">
                                            <v-icon size="14" class="mr-1">mdi-account-outline</v-icon>Người nhận
                                        </div>
                                        <p class="text-body-2 font-weight-bold mb-0">{{ order.tenNguoiNhan }}</p>
                                    </div>
                                    <div class="shipping-row">
                                        <div class="shipping-label text-caption text-grey mb-1">
                                            <v-icon size="14" class="mr-1">mdi-phone-outline</v-icon>Điện thoại
                                        </div>
                                        <p class="text-body-2 font-weight-bold mb-0">{{ order.soDienThoaiNguoiNhan }}</p>
                                    </div>
                                    <div class="shipping-row">
                                        <div class="shipping-label text-caption text-grey mb-1">
                                            <v-icon size="14" class="mr-1">mdi-map-marker-outline</v-icon>Địa chỉ
                                        </div>
                                        <p class="text-body-2 font-weight-medium mb-0">{{ order.diaChiNguoiNhan }}</p>
                                    </div>
                                    <div v-if="order.ngayDuKienNhan" class="shipping-row">
                                        <div class="shipping-label text-caption text-grey mb-1">
                                            <v-icon size="14" class="mr-1">mdi-calendar-check-outline</v-icon>Dự kiến nhận
                                        </div>
                                        <p class="text-body-2 font-weight-bold text-green mb-0">{{ formatDateFull(order.ngayDuKienNhan) }}</p>
                                    </div>
                                </div>
                            </div>
                        </v-card>

                        <!-- Payment Summary -->
                        <v-card class="detail-card rounded-xl" elevation="0">
                            <div class="pa-7">
                                <div class="d-flex align-center mb-6">
                                    <div class="card-icon bg-orange-lighten-5 mr-4">
                                        <v-icon color="orange-darken-3" size="22">mdi-receipt-text-outline</v-icon>
                                    </div>
                                    <h3 class="text-subtitle-1 font-weight-bold mb-0">Chi tiết thanh toán</h3>
                                </div>
                                <div class="payment-details">
                                    <div class="d-flex justify-space-between mb-3">
                                        <span class="text-body-2 text-grey-darken-1">Tạm tính</span>
                                        <span class="text-body-2 font-weight-medium">{{ formatPrice(order.tongTien) }}</span>
                                    </div>
                                    <div class="d-flex justify-space-between mb-3">
                                        <span class="text-body-2 text-grey-darken-1">Phí vận chuyển</span>
                                        <span class="text-body-2 font-weight-medium" :class="!order.phiVanChuyen || order.phiVanChuyen == 0 ? 'text-green' : ''">
                                            <v-icon v-if="!order.phiVanChuyen || order.phiVanChuyen == 0" size="14" color="green" class="mr-1">mdi-check-circle</v-icon>
                                            {{ !order.phiVanChuyen || order.phiVanChuyen == 0 ? 'Miễn phí' : formatPrice(order.phiVanChuyen) }}
                                        </span>
                                    </div>
                                    <div v-if="order.tienGiam > 0" class="d-flex justify-space-between mb-3">
                                        <span class="text-body-2 text-green-darken-2">
                                            <v-icon size="14" color="green-darken-2" class="mr-1">mdi-ticket-percent</v-icon>Giảm giá
                                        </span>
                                        <span class="text-body-2 font-weight-medium text-green-darken-2">-{{ formatPrice(order.tienGiam) }}</span>
                                    </div>
                                    <v-divider class="my-3" />
                                    <div class="d-flex justify-space-between align-center mb-2">
                                        <span class="text-body-1 font-weight-bold">Tổng cộng</span>
                                        <span class="text-h6 font-weight-black text-blue-darken-4">{{ formatPrice(order.tongTienSauGiam) }}</span>
                                    </div>
                                    <div class="d-flex align-center mt-3">
                                        <v-chip variant="tonal" size="small" class="font-weight-bold payment-chip">
                                            <v-icon size="14" class="mr-1">mdi-credit-card-outline</v-icon>
                                            {{ order.phuongThucThanhToan === 'COD' ? 'Thanh toán khi nhận hàng' : order.phuongThucThanhToan }}
                                        </v-chip>
                                        <v-chip v-if="order.maVoucher" variant="tonal" size="small" color="orange" class="font-weight-bold ml-2">
                                            <v-icon size="14" class="mr-1">mdi-ticket-percent</v-icon>
                                            {{ order.maVoucher }}
                                        </v-chip>
                                    </div>
                                </div>
                            </div>
                        </v-card>

                        <!-- Cancel Button -->
                        <v-card v-if="canCancel" class="detail-card rounded-xl" elevation="0">
                            <div class="pa-7 text-center">
                                <div class="cancel-icon-wrapper mx-auto mb-4">
                                    <v-icon size="32" color="red-lighten-1">mdi-alert-circle-outline</v-icon>
                                </div>
                                <h4 class="text-body-1 font-weight-bold mb-2">Cần hủy đơn hàng?</h4>
                                <p class="text-caption text-grey mb-4">Đơn hàng của bạn đang chờ xác nhận, bạn có thể hủy nếu cần.</p>
                                <v-btn color="red" variant="outlined" rounded="pill"
                                    class="font-weight-bold text-none" block
                                    @click="showCancelDialog = true">
                                    <v-icon class="mr-2" size="18">mdi-close-circle-outline</v-icon>
                                    Hủy đơn hàng
                                </v-btn>
                            </div>
                        </v-card>
                    </v-col>
                </v-row>
            </div>
        </v-container>

        <!-- Cancel Dialog -->
        <v-dialog v-model="showCancelDialog" max-width="440">
            <v-card class="rounded-xl pa-6">
                <div class="text-center mb-6">
                    <div class="cancel-dialog-icon mx-auto mb-4">
                        <v-icon size="40" color="red">mdi-alert-circle-outline</v-icon>
                    </div>
                    <h3 class="text-h6 font-weight-bold mb-2">Xác nhận hủy đơn hàng</h3>
                    <p class="text-body-2 text-grey">
                        Bạn có chắc chắn muốn hủy đơn hàng
                        <strong class="text-black">{{ order?.maHoaDon }}</strong>?
                        <br>Thao tác này không thể hoàn tác.
                    </p>
                </div>
                <div class="d-flex gap-3">
                    <v-btn variant="outlined" rounded="pill" class="font-weight-bold text-none flex-grow-1"
                        @click="showCancelDialog = false">
                        Giữ lại đơn hàng
                    </v-btn>
                    <v-btn color="red" rounded="pill" class="font-weight-bold text-none flex-grow-1 text-white"
                        :loading="cancelLoading" @click="handleCancel">
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

.back-btn {
    transition: all 0.2s;
    &:hover {
        background: #f0f0f0;
    }
}

/* Hero Card */
.order-hero-card {
    overflow: hidden;
}

.hero-gradient {
    background: linear-gradient(135deg, #0d47a1 0%, #1565c0 50%, #1976d2 100%);
    position: relative;

    &::before {
        content: '';
        position: absolute;
        top: -30%;
        right: -10%;
        width: 400px;
        height: 400px;
        background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
        border-radius: 50%;
        pointer-events: none;
    }

    &::after {
        content: '';
        position: absolute;
        bottom: -20%;
        left: -5%;
        width: 250px;
        height: 250px;
        background: radial-gradient(circle, rgba(255,255,255,0.06) 0%, transparent 70%);
        border-radius: 50%;
        pointer-events: none;
    }
}

.tracking-wide {
    letter-spacing: 1.5px;
}

.status-chip {
    backdrop-filter: blur(4px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

/* Detail Card */
.detail-card {
    border: 1px solid #eee;
    background: #fff;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.02) !important;
    transition: box-shadow 0.3s;
    height: fit-content;

    &:hover {
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04) !important;
    }
}

.card-icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

/* Products */
.detail-product {
    transition: all 0.2s;
    border-radius: 12px;
    padding: 12px;

    &:hover {
        background: #f8fbff;
    }

    &.border-top {
        border-top: 1px solid #f0f0f0;
    }
}

.detail-thumb-wrapper {
    width: 76px;
    height: 76px;
    border-radius: 12px;
    overflow: hidden;
    flex-shrink: 0;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* Timeline */
.timeline-modern {
    :deep(.v-timeline-item__body) {
        padding-top: 4px;
    }
}

.timeline-dot {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.9), 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* Shipping */
.shipping-details {
    .shipping-row {
        padding: 10px 12px;
        border-radius: 10px;
        transition: background 0.2s;
        margin-bottom: 4px;

        &:hover {
            background: #f8fbff;
        }

        .shipping-label {
            display: flex;
            align-items: center;
        }
    }
}

/* Payment */
.payment-details {
    background: #f8f9fa;
    padding: 16px;
    border-radius: 12px;
}

.payment-chip {
    background: #e3f2fd !important;
}

/* Cancel section */
.cancel-icon-wrapper {
    width: 56px;
    height: 56px;
    background: #fff5f5;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* Cancel Dialog */
.cancel-dialog-icon {
    width: 64px;
    height: 64px;
    background: #fff5f5;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

.content-grid {
    gap: 0;
}

.gap-6 {
    gap: 24px;
}
</style>
