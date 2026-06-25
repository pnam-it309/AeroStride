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
    <div class="success-page">
        <MainHeader />
        <div style="height: 104px"></div>

        <div class="success-wrapper">
            <div v-if="loading" class="text-center py-16">
                <v-progress-circular indeterminate color="#0d47a1" size="48" width="4"></v-progress-circular>
                <p class="text-body-2 text-grey mt-4">Đang tải thông tin đơn hàng...</p>
            </div>

            <div v-else-if="order" class="success-card">
                <div class="check-circle">
                    <v-icon size="40" color="white">mdi-check</v-icon>
                </div>

                <h1 class="success-title">Thanh toán đã ghi nhận!</h1>

                <p class="order-code"><strong>Mã đơn hàng: #{{ order.maHoaDon }}</strong></p>
                <p class="order-status">Trạng thái: {{ order.trangThaiDisplay }}</p>
                <p class="order-total">Tổng thanh toán: {{ formatPrice(order.tongTienSauGiam) }}</p>

                <p class="order-desc">
                    Thanh toán của bạn đã được ghi nhận. Chúng tôi sẽ xác nhận đơn hàng của bạn trong thời gian sớm nhất.
                </p>

                <button class="home-btn" @click="router.push('/')">Quay về trang chủ</button>
            </div>
        </div>

        <CartDrawer />
        <CustomerChat />
    </div>
</template>

<style scoped>
.success-page {
    min-height: 100vh;
    background: #f5f5f5;
}

.success-wrapper {
    display: flex;
    justify-content: center;
    align-items: flex-start;
    padding: 48px 16px 80px;
}

.success-card {
    background: #fff;
    border-radius: 16px;
    padding: 48px 40px;
    max-width: 480px;
    width: 100%;
    text-align: center;
    box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
}

.check-circle {
    width: 72px;
    height: 72px;
    background: #0d47a1;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 24px;
    box-shadow: 0 8px 24px rgba(13, 71, 161, 0.3);
}

.success-title {
    font-size: 1.5rem;
    font-weight: 800;
    color: #0d47a1;
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
    color: #0d47a1;
    font-weight: 600;
    margin-bottom: 20px;
}

.order-desc {
    font-size: 0.9rem;
    color: #666;
    line-height: 1.6;
    margin-bottom: 32px;
}

.home-btn {
    display: inline-block;
    background: #0d47a1;
    color: #fff;
    border: none;
    border-radius: 8px;
    padding: 14px 40px;
    font-size: 1rem;
    font-weight: 700;
    cursor: pointer;
    transition: background 0.2s;
}

.home-btn:hover {
    background: #093170;
}
</style>
