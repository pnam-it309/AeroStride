<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';

import { dichVuDatHang } from '@/services/public/dichVuDatHang';
import { ORDER_STATUS_LABELS, ORDER_STATUS_COLORS, ORDER_STATUS_ICONS } from '@/constants/hoaDonConstants';

const router = useRouter();
const loading = ref(true);
const orders = ref([]);
const activeTab = ref('');
const searchKeyword = ref('');
const isLoggedIn = ref(false);

const trackingForm = ref({ maHoaDon: '', soDienThoai: '' });
const trackingLoading = ref(false);

const handleTrackOrder = async () => {
    if (!trackingForm.value.maHoaDon || !trackingForm.value.soDienThoai) {
        alert('Vui lòng nhập đầy đủ Mã đơn hàng và Số điện thoại');
        return;
    }
    trackingLoading.value = true;
    try {
        const res = await dichVuDatHang.traCuuDonHang(trackingForm.value.maHoaDon.trim(), trackingForm.value.soDienThoai.trim());
        if (res && res.id) {
            router.push(`/my-orders/${res.id}?code=${trackingForm.value.maHoaDon.trim()}&phone=${trackingForm.value.soDienThoai.trim()}`);
        }
    } catch (error) {
        alert(error.response?.data?.message || 'Không tìm thấy đơn hàng hợp lệ');
    } finally {
        trackingLoading.value = false;
    }
};

const tabs = [
    { label: 'Tất cả', value: '', icon: 'mdi-format-list-bulleted' },
    { label: 'Chờ xác nhận', value: 'CHO_XAC_NHAN', icon: 'mdi-progress-clock' },
    { label: 'Đã xác nhận', value: 'XAC_NHAN', icon: 'mdi-check-circle-outline' },
    { label: 'Đang giao', value: 'DANG_GIAO', icon: 'mdi-truck-fast-outline' },
    { label: 'Hoàn thành', value: 'HOAN_THANH', icon: 'mdi-checkbox-marked-circle-outline' },
    { label: 'Đã hủy', value: 'DA_HUY', icon: 'mdi-close-circle-outline' }
];

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const formatDate = (timestamp) => {
    if (!timestamp) return '';
    const d = new Date(timestamp);
    return new Intl.DateTimeFormat('vi-VN', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    }).format(d);
};

const formatDateShort = (timestamp) => {
    if (!timestamp) return '';
    const d = new Date(timestamp);
    const now = new Date();
    const diff = now - d;
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    if (days === 0) {
        const hours = Math.floor(diff / (1000 * 60 * 60));
        if (hours === 0) return 'Vài phút trước';
        return `${hours} giờ trước`;
    }
    if (days === 1) return 'Hôm qua';
    return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
};

const statusIcon = (status) => ORDER_STATUS_ICONS[status] || 'mdi-help-circle-outline';
const statusColor = (status) => ORDER_STATUS_COLORS[status] || '#757575';
const statusLabel = (status) => ORDER_STATUS_LABELS[status] || status;

const stats = ref({ total: 0, completed: 0, cancelled: 0, delivering: 0 });

const fetchOrders = async () => {
    loading.value = true;
    try {
        orders.value = await dichVuDatHang.layDonHangCuaToi(activeTab.value, searchKeyword.value);
    } catch (error) {
        console.error('Error fetching orders:', error);
    } finally {
        loading.value = false;
    }
};

const handleSearch = () => {
    if (isLoggedIn.value) fetchOrders();
};

const fetchStats = async () => {
    try {
        stats.value = await dichVuDatHang.layThongKeDonHang();
    } catch (error) {
        console.error('Error fetching stats:', error);
    }
};

watch(activeTab, () => {
    if (isLoggedIn.value) fetchOrders();
});
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import ReviewModal from '@/components/shared/ReviewModal.vue';

const showReviewModal = ref(false);
const orderToReview = ref(null);

const openReviewModal = (order) => {
    orderToReview.value = order;
    showReviewModal.value = true;
};

const handleReviewSuccess = () => {
    fetchOrders(); // Refresh to update status if needed
};

onMounted(() => {
    isLoggedIn.value = dichVuXacThuc.daDangNhap();
    if (isLoggedIn.value) {
        fetchOrders();
        fetchStats();
    } else {
        loading.value = false;
    }
});

const goToDetail = (id) => {
    router.push(`/my-orders/${id}`);
};
</script>

<template>
    <div class="my-orders-page bg-white min-vh-100">
        <MainHeader />
        <div class="header-spacing" style="height: 104px"></div>

        <!-- Header -->
        <div class="orders-header">
            <v-container style="max-width: 1000px">
                <div class="pa-8">
                    <div class="d-flex align-center justify-space-between flex-wrap gap-4">
                        <div class="d-flex align-center">
                            <div class="header-icon mr-4">
                                <v-icon size="28" color="white">mdi-package-variant-closed</v-icon>
                            </div>
                            <div>
                                <h1 class="text-h4 font-weight-bold mb-1" style="color: #ffffff !important;">Đơn hàng của tôi</h1>
                                <p class="text-body-2 mb-0" style="color: rgba(255,255,255,0.9) !important;">Theo dõi và quản lý tất cả đơn hàng của bạn</p>
                            </div>
                        </div>
                        <v-btn variant="outlined" color="white" rounded="pill" class="font-weight-bold text-none" style="color: white !important;"
                            @click="router.push('/shoes')">
                            <v-icon class="mr-2" size="18">mdi-shopping-outline</v-icon>
                            Mua sắm ngay
                        </v-btn>
                    </div>
                </div>
            </v-container>
        </div>

        <!-- Stats (Logged In Only) -->
        <v-container v-if="isLoggedIn" style="max-width: 1000px" class="mt-n8 position-relative z-index-1">
            <v-row class="stats-row">
                <v-col cols="3" class="pa-2">
                    <div class="stats-block text-center pa-4 rounded-xl elevation-2">
                        <p class="text-h5 font-weight-bold mb-0" style="color: #1e257c;">{{ stats.total }}</p>
                        <p class="text-caption text-grey mb-0">Tổng đơn</p>
                    </div>
                </v-col>
                <v-col cols="3" class="pa-2">
                    <div class="stats-block text-center pa-4 rounded-xl elevation-2">
                        <p class="text-h5 font-weight-bold mb-0" style="color: #1e257c;">{{ stats.completed }}</p>
                        <p class="text-caption text-grey mb-0">Hoàn thành</p>
                    </div>
                </v-col>
                <v-col cols="3" class="pa-2">
                    <div class="stats-block text-center pa-4 rounded-xl elevation-2">
                        <p class="text-h5 font-weight-bold mb-0" style="color: #1e257c;">{{ stats.delivering }}</p>
                        <p class="text-caption text-grey mb-0">Đang giao</p>
                    </div>
                </v-col>
                <v-col cols="3" class="pa-2">
                    <div class="stats-block text-center pa-4 rounded-xl elevation-2">
                        <p class="text-h5 font-weight-bold mb-0" style="color: #1e257c;">{{ stats.cancelled }}</p>
                        <p class="text-caption text-grey mb-0">Đã hủy</p>
                    </div>
                </v-col>
            </v-row>
        </v-container>

        <!-- Tracking Form (Guest Only) -->
        <v-container v-if="!isLoggedIn" style="max-width: 800px" class="mt-n8 position-relative z-index-1">
            <v-card class="elevation-4 rounded-xl pa-8 text-center" style="border-top: 4px solid #1e257c;">
                <v-icon size="48" color="#1e257c" class="mb-4">mdi-magnify-scan</v-icon>
                <h2 class="text-h5 font-weight-bold mb-2">Tra cứu đơn hàng</h2>
                <p class="text-body-2 text-grey-darken-1 mb-8">
                    Nhập mã đơn hàng và số điện thoại đã dùng để tra cứu trạng thái đơn hàng của bạn.
                </p>
                <v-form @submit.prevent="handleTrackOrder" class="mx-auto" style="max-width: 500px;">
                    <v-text-field v-model="trackingForm.maHoaDon" label="Mã đơn hàng (VD: HD...)"
                        variant="outlined" density="comfortable" class="mb-4" hide-details="auto"
                        prepend-inner-icon="mdi-barcode" />
                    <v-text-field v-model="trackingForm.soDienThoai" label="Số điện thoại"
                        variant="outlined" density="comfortable" class="mb-6" hide-details="auto"
                        prepend-inner-icon="mdi-phone-outline" />
                    <v-btn type="submit" size="large" rounded="pill" block class="text-none font-weight-bold"
                        style="background: #1e257c; color: white !important;" :loading="trackingLoading">
                        <v-icon size="20" class="mr-2">mdi-magnify</v-icon>Tra cứu ngay
                    </v-btn>
                </v-form>
                <p class="text-caption text-grey mt-6">Hoặc <a href="/login" class="font-weight-bold" style="color: #1e257c;">Đăng nhập</a> để quản lý toàn bộ đơn hàng</p>
            </v-card>
        </v-container>

        <!-- Main Content (Logged In) -->
        <v-container v-if="isLoggedIn" class="py-6" style="max-width: 1000px">
            <!-- Status Tabs and Search -->
            <div class="mb-6 pb-4" style="border-bottom: 1px solid #e0e0e0;">
                <div class="d-flex align-center justify-space-between flex-wrap gap-4">
                    <div class="order-tabs">
                        <v-chip-group v-model="activeTab" mandatory selected-class="active-tab" show-arrows>
                            <v-chip
                                v-for="tab in tabs"
                                :key="tab.value"
                                :value="tab.value"
                                variant="outlined"
                                class="tab-chip font-weight-bold text-none"
                                rounded="pill"
                                filter
                            >
                                <v-icon size="16" class="mr-1">{{ tab.icon }}</v-icon>
                                {{ tab.label }}
                            </v-chip>
                        </v-chip-group>
                    </div>
                    <div style="width: 320px; min-width: 250px;">
                        <v-text-field
                            v-model="searchKeyword"
                            label="Mã đơn hàng, mã/tên sản phẩm"
                            variant="outlined"
                            density="compact"
                            hide-details
                            prepend-inner-icon="mdi-magnify"
                            clearable
                            @click:clear="handleSearch"
                            @keyup.enter="handleSearch"
                        ></v-text-field>
                    </div>
                </div>
            </div>

            <!-- Loading -->
            <div v-if="loading" class="text-center py-16">
                <v-progress-circular indeterminate color="#1e257c" size="48" width="4"></v-progress-circular>
                <p class="text-body-2 text-grey mt-4">Đang tải đơn hàng...</p>
            </div>

            <!-- Orders List -->
            <div v-else-if="orders.length > 0" class="orders-list custom-scrollbar">
                <div v-for="order in orders"
                    :key="order.id"
                    class="order-row pa-5 mb-5 rounded-xl"
                    @click="goToDetail(order.id)">
                    <!-- Header -->
                    <div class="d-flex align-center justify-space-between mb-4">
                        <div class="d-flex align-center">
                            <div class="order-status-dot mr-3" :style="{ background: statusColor(order.trangThai) }"></div>
                            <div>
                                <div class="d-flex align-center gap-2 mb-1">
                                    <span class="text-body-2 font-weight-bold">{{ order.maHoaDon || 'Đang tạo...' }}</span>
                                    <v-chip
                                        :color="statusColor(order.trangThai)"
                                        variant="tonal"
                                        size="x-small"
                                        class="font-weight-bold"
                                    >
                                        <v-icon size="12" class="mr-1">{{ statusIcon(order.trangThai) }}</v-icon>
                                        {{ statusLabel(order.trangThai) }}
                                    </v-chip>
                                </div>
                                <div class="d-flex align-center text-caption text-grey">
                                    <v-icon size="12" class="mr-1">mdi-calendar-outline</v-icon>
                                    {{ formatDateShort(order.ngayTao) }}
                                    <span class="mx-1">•</span>
                                    <v-icon size="12" class="mr-1">mdi-shopping-outline</v-icon>
                                    {{ order.items?.length || 0 }} sản phẩm
                                </div>
                            </div>
                        </div>
                        <v-icon size="20" color="grey-lighten-1" class="flex-shrink-0">mdi-chevron-right</v-icon>
                    </div>

                    <!-- Products Preview -->
                    <div class="product-preview d-flex gap-3 mb-3">
                        <div v-for="(item, i) in (order.items || []).slice(0, 3)" :key="i" class="preview-thumb-wrapper">
                            <v-img v-if="item.hinhAnh" :src="item.hinhAnh" cover width="52" height="52" class="rounded-lg"></v-img>
                            <div v-else class="thumb-placeholder rounded-lg">
                                <v-icon size="18" color="grey-lighten-2">mdi-shoe-sneaker</v-icon>
                            </div>
                            <span class="preview-overlay" v-if="i === 2 && order.items.length > 3">+{{ order.items.length - 3 }}</span>
                        </div>
                        <div v-if="order.items?.length === 1" class="preview-info d-flex flex-column justify-center">
                            <p class="text-body-2 font-weight-bold mb-0 text-truncate" style="max-width: 200px">{{ order.items[0].tenSanPham }}</p>
                            <p class="text-caption text-grey mb-0">{{ order.items[0].tenMauSac }} / {{ order.items[0].tenKichThuoc }} · x{{ order.items[0].soLuong }}</p>
                        </div>
                    </div>

                    <hr style="border: none; border-top: 1px solid #f0f0f0; margin: 12px 0;" />

                    <!-- Footer -->
                    <div class="d-flex align-center justify-space-between mt-3">
                        <div class="d-flex align-center">
                            <v-icon size="14" color="grey" class="mr-1">mdi-currency-usd</v-icon>
                            <span class="text-body-2 text-grey-darken-1">Tổng:</span>
                        </div>
                        <span class="text-body-1 font-weight-bold" style="color: #1e257c;">{{ formatPrice(order.tongTienSauGiam) }}</span>
                    </div>

                    <!-- Actions -->
                    <div class="d-flex justify-end mt-4 gap-2 border-t pt-3" style="border-top: 1px solid #f0f0f0;">
                        <v-btn 
                            v-if="order.trangThai === 'HOAN_THANH'" 
                            color="#1e257c" 
                            variant="outlined" 
                            size="small" 
                            rounded="pill" 
                            class="text-none font-weight-bold px-4" 
                            @click.stop="openReviewModal(order)"
                        >
                            <v-icon size="16" class="mr-1">mdi-star-outline</v-icon>Đánh giá
                        </v-btn>
                        <v-btn 
                            color="#1e257c" 
                            variant="flat" 
                            size="small" 
                            rounded="pill" 
                            class="text-none font-weight-bold px-4" 
                            @click.stop="goToDetail(order.id)"
                        >
                            Xem chi tiết
                        </v-btn>
                    </div>
                </div>
            </div>

            <!-- Empty State -->
            <div v-else class="text-center py-16">
                <div class="pa-12">
                    <div class="empty-icon-large mx-auto mb-6">
                        <v-icon size="64" color="grey-lighten-3">mdi-package-variant-closed</v-icon>
                    </div>
                    <h3 class="text-h6 font-weight-bold text-grey-darken-1 mb-2">
                        {{ activeTab ? 'Không có đơn hàng nào' : 'Chưa có đơn hàng nào' }}
                    </h3>
                    <p class="text-body-2 text-grey mb-6">
                        {{ activeTab ? 'Không tìm thấy đơn hàng nào ở trạng thái này.' : 'Hãy bắt đầu mua sắm để tạo đơn hàng đầu tiên!' }}
                    </p>
                    <v-btn style="background: #1e257c; color: white;" rounded="pill" size="large" class="font-weight-bold text-none px-8"
                        @click="router.push('/shoes')">
                        <v-icon class="mr-2">mdi-cart-outline</v-icon>
                        Khám phá sản phẩm
                    </v-btn>
                </div>
            </div>
        </v-container>

        
        <CustomerChat />

        <ReviewModal
            v-model:show="showReviewModal"
            :order="orderToReview"
            @review-success="handleReviewSuccess"
        />
    </div>
</template>

<style scoped>
.my-orders-page {
    padding-top: 64px;
    min-height: 100vh;
}

.orders-header {
    background: linear-gradient(135deg, #1e257c 0%, #3b429f 100%);
    box-shadow: 0 4px 20px rgba(30, 37, 124, 0.15);
}

.header-icon {
    width: 52px;
    height: 52px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    backdrop-filter: blur(10px);
}

.stats-row .stats-block {
    border: 1px solid rgba(0,0,0,0.03);
    background: #fff;
    box-shadow: 0 4px 15px rgba(0,0,0,0.03) !important;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.stats-row .stats-block:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 25px rgba(30, 37, 124, 0.08) !important;
}

.order-tabs .tab-chip {
    border-color: #e0e0e0;
    font-size: 0.85rem;
    transition: all 0.3s ease;
    padding: 0 16px;
}
.order-tabs .tab-chip:hover {
    border-color: #1e257c;
    background: #f5f7ff;
    transform: translateY(-1px);
}

:deep(.active-tab) {
    background: linear-gradient(135deg, #1e257c 0%, #3b429f 100%) !important;
    color: #fff !important;
    border-color: transparent !important;
    box-shadow: 0 4px 12px rgba(30, 37, 124, 0.2);
}

.order-row {
    border: 1px solid rgba(0,0,0,0.04);
    cursor: pointer;
    transition: all 0.3s ease;
    background: #fff;
    box-shadow: 0 4px 12px rgba(0,0,0,0.02);
}
.order-row:hover {
    border-color: #1e257c;
    box-shadow: 0 8px 24px rgba(30, 37, 124, 0.08);
    transform: translateY(-2px);
}

.order-status-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex-shrink: 0;
}

.product-preview .preview-thumb-wrapper {
    position: relative;
}
.product-preview .preview-thumb-wrapper .preview-overlay {
    position: absolute;
    inset: 0;
    background: rgba(0,0,0,0.5);
    color: white;
    font-size: 0.75rem;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
}

.thumb-placeholder {
    width: 52px;
    height: 52px;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
}

.preview-info {
    min-width: 0;
}

.empty-icon-large {
    width: 100px;
    height: 100px;
    background: #fafafa;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 2px dashed #e0e0e0;
}

.orders-list {
    max-height: calc(100vh - 400px);
    min-height: 300px;
    overflow-y: auto;
    padding-right: 8px;
}

.custom-scrollbar::-webkit-scrollbar {
    width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
    background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
    background-color: #d1d5db;
    border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
    background-color: #9ca3af;
}
</style>
