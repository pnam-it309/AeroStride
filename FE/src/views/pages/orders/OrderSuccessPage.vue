<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';

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
    <div class="success-page bg-white min-vh-100">
        <MainHeader />
        <div style="height: 104px"></div>

        <div class="success-wrapper">
            <div v-if="loading" class="text-center py-16">
                <v-progress-circular indeterminate color="#1e257c" size="48" width="4"></v-progress-circular>
                <p class="text-body-2 text-grey mt-4">Đang tải thông tin đơn hàng...</p>
            </div>

            <div v-else-if="order" class="success-block">
                <div class="check-circle">
                    <v-icon size="40" color="white">mdi-check</v-icon>
                </div>

                <h1 class="success-title">Đặt hàng thành công!</h1>

                <p class="order-code"><strong>Mã đơn hàng: #{{ order.maHoaDon }}</strong></p>
                <p class="order-status">Trạng thái: {{ order.trangThaiDisplay }}</p>
                <p class="order-total">Tổng thanh toán: {{ formatPrice(order.tongTienSauGiam) }}</p>

                <p class="order-desc">
                    Đơn hàng của bạn đã được ghi nhận. Chúng tôi sẽ xác nhận và giao hàng trong thời gian sớm nhất.
                </p>

                <v-btn style="background: #1e257c; color: white;" rounded="pill" size="large"
                    class="font-weight-bold text-none px-8" @click="router.push('/')">
                    Quay về trang chủ
                </v-btn>
            </div>
        </div>

        
        <CustomerChat />
    </div>
</template>

<style scoped>
.success-wrapper {
    display: flex;
    justify-content: center;
    align-items: flex-start;
    padding: 48px 16px 80px;
}

.success-block {
    border: 1px solid #e0e0e0;
    background: #fff;
    padding: 48px 40px;
    max-width: 480px;
    width: 100%;
    text-align: center;
}

.check-circle {
    width: 72px;
    height: 72px;
    background: #1e257c;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 24px;
}

.success-title {
    font-size: 1.5rem;
    font-weight: 800;
    color: #1e257c;
    margin-bottom: 20px;
}

.order-code {
    font-size: 1rem;
    color: #111;
    margin-bottom: 8px;
}

.order-status {
    font-size: 0.95rem;
    color: #444;
    margin-bottom: 8px;
}

.order-total {
    font-size: 1rem;
    color: #1e257c;
    font-weight: 600;
    margin-bottom: 20px;
}

.order-desc {
    font-size: 0.9rem;
    color: #666;
    line-height: 1.6;
    margin-bottom: 32px;
}
</style>
