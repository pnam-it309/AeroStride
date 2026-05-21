<script setup>
import { ref, onMounted } from 'vue';
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

onMounted(async () => {
    try {
        order.value = await dichVuDatHang.layChiTietDonHang(route.params.id);
    } catch (error) {
        console.error('Error fetching order:', error);
    } finally {
        loading.value = false;
    }
    setTimeout(() => { showConfetti.value = false; }, 4000);
});
</script>

<template>
    <div class="order-success-page bg-white min-vh-100">
        <MainHeader />
        <div class="header-spacing" style="height: 104px;"></div>

        <!-- Confetti Animation -->
        <div v-if="showConfetti" class="confetti-container">
            <div v-for="n in 50" :key="n" class="confetti-piece" :style="{
                left: Math.random() * 100 + '%',
                animationDelay: Math.random() * 3 + 's',
                animationDuration: (Math.random() * 2 + 2) + 's',
                backgroundColor: ['#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8', '#F7DC6F', '#BB8FCE', '#2962FF'][Math.floor(Math.random() * 8)]
            }"></div>
        </div>

        <v-container class="py-16 text-center" style="max-width: 700px" v-if="!loading">
            <!-- Success Icon -->
            <div class="success-icon-wrap mb-8">
                <div class="success-icon">
                    <v-icon size="56" color="white">mdi-check</v-icon>
                </div>
                <div class="success-ring ring-1"></div>
                <div class="success-ring ring-2"></div>
            </div>

            <h1 class="text-h4 font-weight-black mb-3">Đặt hàng thành công! 🎉</h1>
            <p class="text-body-1 text-grey-darken-1 mb-8">
                Cảm ơn bạn đã mua sắm tại AeroStride. Đơn hàng của bạn đang được xử lý.
            </p>

            <!-- Order Info Card -->
            <v-card v-if="order" class="order-info-card rounded-xl pa-8 text-left mb-10" elevation="0">
                <div class="d-flex align-center justify-space-between mb-6">
                    <div>
                        <p class="text-caption text-grey mb-1">Mã đơn hàng</p>
                        <h3 class="text-h6 font-weight-black">{{ order.maHoaDon || 'Đang tạo...' }}</h3>
                    </div>
                    <v-chip color="orange" variant="tonal" class="font-weight-bold">
                        {{ order.trangThaiDisplay }}
                    </v-chip>
                </div>

                <v-divider class="mb-4" />

                <v-row class="mb-2">
                    <v-col cols="6">
                        <p class="text-caption text-grey mb-1">Phương thức thanh toán</p>
                        <p class="text-body-2 font-weight-bold">{{ order.phuongThucThanhToan === 'COD' ? 'Thanh toán khi nhận hàng' : order.phuongThucThanhToan }}</p>
                    </v-col>
                    <v-col cols="6">
                        <p class="text-caption text-grey mb-1">Tổng thanh toán</p>
                        <p class="text-body-1 font-weight-black">{{ formatPrice(order.tongTienSauGiam) }}</p>
                    </v-col>
                </v-row>
                <v-row>
                    <v-col cols="12">
                        <p class="text-caption text-grey mb-1">Giao đến</p>
                        <p class="text-body-2 font-weight-bold">{{ order.diaChiNguoiNhan }}</p>
                    </v-col>
                </v-row>
            </v-card>

            <!-- Actions -->
            <div class="d-flex justify-center gap-4 flex-wrap">
                <v-btn
                    color="black"
                    rounded="pill"
                    size="large"
                    class="font-weight-black text-none px-10"
                    @click="router.push(PATH.ORDERS)"
                >
                    <v-icon class="mr-2">mdi-package-variant</v-icon>
                    Xem đơn hàng
                </v-btn>
                <v-btn
                    variant="outlined"
                    rounded="pill"
                    size="large"
                    class="font-weight-bold text-none px-10"
                    @click="router.push(PATH.SHOES)"
                >
                    Tiếp tục mua sắm
                </v-btn>
            </div>
        </v-container>

        <v-container v-else class="text-center py-16">
            <v-progress-circular indeterminate color="black" size="48"></v-progress-circular>
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

/* Success Icon Animation */
.success-icon-wrap {
    position: relative;
    display: inline-block;
    width: 120px;
    height: 120px;
}

.success-icon {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, #4CAF50, #2E7D32);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 2;
    animation: scaleIn 0.5s ease-out;
}

.success-ring {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    border-radius: 50%;
    border: 2px solid rgba(76, 175, 80, 0.3);

    &.ring-1 {
        width: 100px;
        height: 100px;
        animation: ringPulse 2s ease-out infinite;
    }

    &.ring-2 {
        width: 120px;
        height: 120px;
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

/* Order Info Card */
.order-info-card {
    border: 1px solid #f0f0f0;
    background: #fafafa;
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
    width: 10px;
    height: 10px;
    border-radius: 2px;
    animation: confettiFall linear forwards;
}

@keyframes confettiFall {
    0% { transform: translateY(0) rotate(0deg); opacity: 1; }
    100% { transform: translateY(100vh) rotate(720deg); opacity: 0; }
}

.min-vh-100 { min-height: 100vh; }
</style>
