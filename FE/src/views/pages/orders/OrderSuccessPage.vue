<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import CartDrawer from '@/components/shared/CartDrawer.vue';
import { dichVuDatHang } from '@/services/public/dichVuDatHang';
import { PATH } from '@/router/routePaths';

const route = useRoute();
const router = useRouter();
const order = ref(null);
const loading = ref(true);
const showConfetti = ref(true);

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const formatDate = (timestamp) => {
    if (!timestamp) return '';
    return new Intl.DateTimeFormat('vi-VN', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    }).format(new Date(timestamp));
};

const estimatedDelivery = computed(() => {
    if (!order.value?.ngayTao) return 'Đang cập nhật...';
    const est = new Date(order.value.ngayTao);
    est.setDate(est.getDate() + 7);
    return formatDate(est);
});

onMounted(async () => {
    try {
        order.value = await dichVuDatHang.layChiTietDonHang(route.params.id);
    } catch (error) {
        console.error('Error fetching order:', error);
    } finally {
        loading.value = false;
    }
    setTimeout(() => { showConfetti.value = false; }, 5000);
});
</script>

<template>
    <div class="order-success-page bg-white min-vh-100">
        <MainHeader />
        <div class="header-spacing" style="height: 104px"></div>

        <!-- Confetti -->
        <div v-if="showConfetti" class="confetti-container">
            <div
                v-for="n in 60"
                :key="n"
                class="confetti-piece"
                :style="{
                    left: Math.random() * 100 + '%',
                    animationDelay: Math.random() * 3 + 's',
                    animationDuration: Math.random() * 2 + 2 + 's',
                    width: Math.random() * 6 + 6 + 'px',
                    height: Math.random() * 6 + 6 + 'px',
                    backgroundColor: ['#FF6B6B','#4ECDC4','#45B7D1','#FFA07A','#98D8C8','#F7DC6F','#BB8FCE','#1e257c','#1976d2','#4caf50'][
                        Math.floor(Math.random() * 10)
                    ]
                }"
            ></div>
        </div>

        <!-- Success Hero -->
        <div class="success-hero">
            <v-container class="py-12 text-center">
                <div class="success-icon-wrap mb-6">
                    <div class="success-icon">
                        <v-icon size="48" color="white">mdi-check</v-icon>
                    </div>
                    <div class="success-ring ring-1"></div>
                    <div class="success-ring ring-2"></div>
                </div>

                <h1 class="text-h3 font-weight-black text-white mb-3">Đặt hàng thành công!</h1>
                <p class="text-body-1 text-blue-lighten-4 mb-0" style="max-width: 500px; margin: 0 auto;">
                    Cảm ơn bạn đã tin tưởng và mua sắm tại AeroStride. Đơn hàng của bạn đang được xử lý.
                </p>
            </v-container>
        </div>

        <v-container class="py-8" style="max-width: 800px">
            <div v-if="!loading && order">
                <!-- Delivery Progress -->
                <v-card class="progress-card rounded-xl pa-6 mb-6" elevation="0">
                    <div class="d-flex align-center mb-4">
                        <div class="progress-icon mr-3">
                            <v-icon size="22" color="blue-darken-4">mdi-truck-fast-outline</v-icon>
                        </div>
                        <div>
                            <h4 class="text-body-1 font-weight-bold mb-0">Tiến trình giao hàng</h4>
                            <p class="text-caption text-grey mb-0">Dự kiến giao hàng vào <strong class="text-blue-darken-4">{{ estimatedDelivery }}</strong></p>
                        </div>
                    </div>
                    <div class="delivery-timeline">
                        <div class="timeline-step active">
                            <div class="step-dot">
                                <v-icon size="12" color="white">mdi-check</v-icon>
                            </div>
                            <span class="step-label">Đã đặt hàng</span>
                        </div>
                        <div class="timeline-connector active"></div>
                        <div class="timeline-step">
                            <div class="step-dot">2</div>
                            <span class="step-label">Xác nhận</span>
                        </div>
                        <div class="timeline-connector"></div>
                        <div class="timeline-step">
                            <div class="step-dot">3</div>
                            <span class="step-label">Đóng gói</span>
                        </div>
                        <div class="timeline-connector"></div>
                        <div class="timeline-step">
                            <div class="step-dot">
                                <v-icon size="12" color="grey-lighten-1">mdi-truck</v-icon>
                            </div>
                            <span class="step-label">Giao hàng</span>
                        </div>
                        <div class="timeline-connector"></div>
                        <div class="timeline-step">
                            <div class="step-dot">
                                <v-icon size="12" color="grey-lighten-1">mdi-check</v-icon>
                            </div>
                            <span class="step-label">Nhận hàng</span>
                        </div>
                    </div>
                </v-card>

                <!-- Order Details -->
                <v-card class="rounded-xl" elevation="0">
                    <div class="order-summary-header pa-6 d-flex align-center justify-space-between flex-wrap gap-3">
                        <div class="d-flex align-center">
                            <div class="summary-header-icon mr-3">
                                <v-icon size="20" color="white">mdi-receipt-text-outline</v-icon>
                            </div>
                            <div>
                                <p class="text-caption text-blue-lighten-4 mb-0">Mã đơn hàng</p>
                                <h3 class="text-h6 font-weight-bold text-white mb-0">{{ order.maHoaDon || 'Đang tạo...' }}</h3>
                            </div>
                        </div>
                        <v-chip color="orange-darken-2" variant="flat" class="font-weight-bold text-white">
                            <v-icon size="16" class="mr-1">mdi-progress-clock</v-icon>
                            {{ order.trangThaiDisplay }}
                        </v-chip>
                    </div>

                    <div class="pa-6">
                        <v-row>
                            <v-col cols="12" sm="6">
                                <div class="info-block mb-4">
                                    <p class="text-caption text-grey mb-1 d-flex align-center">
                                        <v-icon size="14" class="mr-1">mdi-credit-card-outline</v-icon>
                                        Phương thức thanh toán
                                    </p>
                                    <p class="text-body-2 font-weight-bold mb-0">
                                        {{ order.phuongThucThanhToan === 'COD' ? 'Thanh toán khi nhận hàng (COD)' : order.phuongThucThanhToan }}
                                    </p>
                                </div>
                                <div class="info-block">
                                    <p class="text-caption text-grey mb-1 d-flex align-center">
                                        <v-icon size="14" class="mr-1">mdi-cart-outline</v-icon>
                                        Số lượng sản phẩm
                                    </p>
                                    <p class="text-body-2 font-weight-bold mb-0">{{ order.items?.length || 0 }} sản phẩm</p>
                                </div>
                            </v-col>
                            <v-col cols="12" sm="6">
                                <div class="info-block mb-4">
                                    <p class="text-caption text-grey mb-1 d-flex align-center">
                                        <v-icon size="14" class="mr-1">mdi-currency-usd</v-icon>
                                        Tổng thanh toán
                                    </p>
                                    <p class="text-h6 font-weight-black text-blue-darken-4 mb-0">{{ formatPrice(order.tongTienSauGiam) }}</p>
                                </div>
                                <div class="info-block">
                                    <p class="text-caption text-grey mb-1 d-flex align-center">
                                        <v-icon size="14" class="mr-1">mdi-map-marker-outline</v-icon>
                                        Giao đến
                                    </p>
                                    <p class="text-body-2 font-weight-medium mb-0">{{ order.diaChiNguoiNhan }}</p>
                                </div>
                            </v-col>
                        </v-row>

                        <!-- Products Preview -->
                        <v-divider class="my-4" />
                        <p class="text-caption text-grey mb-3 text-uppercase font-weight-bold tracking-wide">SẢN PHẨM ĐÃ ĐẶT</p>
                        <div v-for="(item, i) in (order.items || []).slice(0, 3)" :key="i"
                            class="d-flex align-center gap-3 py-3"
                            :class="{ 'border-top': i > 0 }">
                            <v-img :src="item.hinhAnh || 'https://via.placeholder.com/150?text=Product'"
                                cover width="48" height="48" class="rounded-lg flex-shrink-0"></v-img>
                            <div class="flex-grow-1 min-w-0">
                                <p class="text-body-2 font-weight-bold mb-0 text-truncate">{{ item.tenSanPham }}</p>
                                <p class="text-caption text-grey mb-0">{{ item.tenMauSac }} / {{ item.tenKichThuoc }} · x{{ item.soLuong }}</p>
                            </div>
                            <span class="text-body-2 font-weight-bold flex-shrink-0">{{ formatPrice(item.thanhTien) }}</span>
                        </div>
                        <p v-if="order.items?.length > 3" class="text-caption text-grey mt-2 text-center">
                            <v-icon size="14" class="mr-1">mdi-plus-circle-outline</v-icon>
                            Còn {{ order.items.length - 3 }} sản phẩm khác
                        </p>
                    </div>
                </v-card>

                <!-- Actions -->
                <div class="d-flex justify-center gap-4 flex-wrap mt-8">
                    <v-btn
                        color="black"
                        rounded="pill"
                        size="x-large"
                        class="font-weight-bold text-none px-10 elevation-2 action-btn"
                        @click="router.push(PATH.ORDERS)"
                    >
                        <v-icon class="mr-2" size="22">mdi-package-variant</v-icon>
                        Theo dõi đơn hàng
                    </v-btn>
                    <v-btn
                        variant="outlined"
                        rounded="pill"
                        size="x-large"
                        class="font-weight-bold text-none px-10 action-btn"
                        @click="router.push(PATH.SHOES)"
                    >
                        <v-icon class="mr-2" size="22">mdi-shopping-outline</v-icon>
                        Mua thêm sản phẩm
                    </v-btn>
                </div>
            </div>

            <div v-else-if="loading" class="text-center py-16">
                <v-progress-circular indeterminate color="blue-darken-4" size="48" width="4"></v-progress-circular>
                <p class="text-body-2 text-grey mt-4">Đang tải thông tin đơn hàng...</p>
            </div>
        </v-container>

        <CartDrawer />
        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.order-success-page {
    padding-top: 64px;
    position: relative;
    overflow: hidden;
}

/* Hero */
.success-hero {
    background: linear-gradient(135deg, #0d47a1 0%, #1565c0 50%, #1976d2 100%);
    position: relative;
    overflow: hidden;

    &::before {
        content: '';
        position: absolute;
        top: -50%;
        right: -15%;
        width: 500px;
        height: 500px;
        background: radial-gradient(circle, rgba(255,255,255,0.08) 0%, transparent 70%);
        border-radius: 50%;
    }
}

/* Success Icon */
.success-icon-wrap {
    position: relative;
    display: inline-block;
    width: 100px;
    height: 100px;
}

.success-icon {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 70px;
    height: 70px;
    background: linear-gradient(135deg, #4caf50, #2e7d32);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 2;
    animation: scaleIn 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    box-shadow: 0 8px 24px rgba(76, 175, 80, 0.3);
}

.success-ring {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    border-radius: 50%;
    border: 2px solid rgba(255, 255, 255, 0.3);

    &.ring-1 {
        width: 90px;
        height: 90px;
        animation: ringPulse 2s ease-out infinite;
    }
    &.ring-2 {
        width: 100px;
        height: 100px;
        animation: ringPulse 2s ease-out infinite 0.5s;
    }
}

@keyframes scaleIn {
    from { transform: translate(-50%, -50%) scale(0); }
    to { transform: translate(-50%, -50%) scale(1); }
}

@keyframes ringPulse {
    0% { transform: translate(-50%, -50%) scale(0.8); opacity: 1; }
    100% { transform: translate(-50%, -50%) scale(1.3); opacity: 0; }
}

/* Progress Card */
.progress-card {
    border: 1px solid #eee;
    background: #fff;
    box-shadow: 0 2px 12px rgba(0,0,0,0.02) !important;
}

.progress-icon {
    width: 44px;
    height: 44px;
    background: #e3f2fd;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.delivery-timeline {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 0;

    .timeline-step {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 6px;
        position: relative;

        .step-dot {
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background: #e0e0e0;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 0.75rem;
            font-weight: 700;
            color: #999;
            transition: all 0.3s;
        }

        .step-label {
            font-size: 0.65rem;
            font-weight: 600;
            color: #999;
            white-space: nowrap;
            transition: color 0.3s;
        }

        &.active {
            .step-dot {
                background: #0d47a1;
                color: white;
                box-shadow: 0 4px 12px rgba(13, 71, 161, 0.3);
            }
            .step-label {
                color: #0d47a1;
            }
        }
    }

    .timeline-connector {
        flex: 1;
        height: 3px;
        background: #e0e0e0;
        margin: 0 4px;
        border-radius: 2px;
        margin-bottom: 26px;

        &.active {
            background: #0d47a1;
        }
    }
}

/* Order Summary */
.order-summary-header {
    background: linear-gradient(135deg, #0d47a1, #1565c0);
    border-radius: 12px 12px 0 0;
}

.summary-header-icon {
    width: 40px;
    height: 40px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.info-block {
    background: #f8f9fa;
    padding: 12px 16px;
    border-radius: 10px;
}

.tracking-wide {
    letter-spacing: 1.2px;
}

.border-top {
    border-top: 1px solid #f0f0f0;
}

.action-btn {
    height: 56px !important;
    transition: all 0.2s;
    &:hover {
        transform: translateY(-2px);
    }
}

/* Confetti */
.confetti-container {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 9998;
    overflow: hidden;
}

.confetti-piece {
    position: absolute;
    top: -10px;
    border-radius: 2px;
    animation: confettiFall linear forwards;
}

@keyframes confettiFall {
    0% { transform: translateY(0) rotate(0deg); opacity: 1; }
    100% { transform: translateY(100vh) rotate(720deg); opacity: 0; }
}
</style>
