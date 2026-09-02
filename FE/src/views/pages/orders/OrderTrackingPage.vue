<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import MainFooter from '@/components/shared/MainFooter.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import { dichVuDatHang } from '@/services/public/dichVuDatHang';
import { PATH } from '@/router/routePaths';
import { useNotifications } from '@/services/notificationService';
import { useAuthStore } from '@/stores/authStore';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const { addNotification } = useNotifications();

const trackingForm = ref({ maHoaDon: '', soDienThoai: '' });
const trackingLoading = ref(false);

onMounted(() => {
    const code = route.query.code || route.query.maHoaDon;
    const phone = route.query.phone || route.query.sdt;
    if (code) {
        trackingForm.value.maHoaDon = String(code).trim();
    }
    if (phone) {
        trackingForm.value.soDienThoai = String(phone).trim();
    }
    if (code || phone) {
        handleTrackOrder();
    }
});

const handleTrackOrder = async () => {
    const code = trackingForm.value.maHoaDon ? trackingForm.value.maHoaDon.trim() : '';
    const phone = trackingForm.value.soDienThoai ? trackingForm.value.soDienThoai.trim() : '';

    if (!code && !phone) {
        addNotification({
            title: 'Cảnh báo',
            subtitle: 'Vui lòng nhập Mã đơn hàng hoặc Số điện thoại để tra cứu',
            color: 'warning'
        });
        return;
    }

    trackingLoading.value = true;
    try {
        const res = await dichVuDatHang.traCuuDonHang(code, phone);
        if (res && res.id) {
            const query = {};
            if (res.maHoaDon) query.code = res.maHoaDon;
            if (res.soDienThoaiNguoiNhan) query.phone = res.soDienThoaiNguoiNhan;
            router.push({ path: `/my-orders/${res.id}`, query });
        } else {
            addNotification({
                title: 'Thông báo',
                subtitle: 'Không tìm thấy thông tin đơn hàng phù hợp',
                color: 'error'
            });
        }
    } catch (error) {
        addNotification({
            title: 'Lỗi',
            subtitle: error.response?.data?.message || 'Không tìm thấy đơn hàng hợp lệ',
            color: 'error'
        });
    } finally {
        trackingLoading.value = false;
    }
};
</script>

<template>
    <div class="order-tracking-page bg-slate-50 min-vh-100 d-flex flex-column">
        <MainHeader />
        <div style="height: 104px"></div>

        <!-- Banner Header -->
        <div class="tracking-banner py-10" style="background: linear-gradient(135deg, #0f172a 0%, #1e257c 100%);">
            <v-container style="max-width: 900px" class="text-center text-white">
                <v-icon size="48" color="white" class="mb-3">mdi-magnify-scan</v-icon>
                <h1 class="text-h4 font-weight-bold mb-2 text-white">Tra Cứu Đơn Hàng</h1>
                <p class="text-body-1 text-slate-200 mb-0">
                    Dễ dàng theo dõi hành trình đơn hàng của bạn nhanh chóng và chính xác
                </p>
            </v-container>
        </div>

        <!-- Tracking Form Box -->
        <v-container style="max-width: 700px" class="mt-n8 mb-12 flex-grow-1 position-relative z-index-1">
            <v-card class="elevation-6 rounded-2xl pa-8 bg-white border" style="border-top: 5px solid #1e257c !important">
                <div v-if="authStore.isLoggedIn" class="d-flex align-center justify-space-between flex-wrap ga-3 pa-4 mb-6 rounded-xl border" style="background: #eef2ff; border-color: #c7d2fe !important">
                    <div class="d-flex align-center ga-3">
                        <v-icon color="#1e257c" size="28">mdi-account-check</v-icon>
                        <div>
                            <div class="text-subtitle-2 font-weight-bold" style="color: #1e257c">Bạn đang đăng nhập tài khoản</div>
                            <div class="text-caption text-grey-darken-2">Xem danh sách tất cả đơn hàng đã mua tại Đơn hàng của tôi.</div>
                        </div>
                    </div>
                    <v-btn :to="PATH.ORDERS" variant="flat" style="background: #1e257c; color: white !important" class="text-none font-weight-bold" rounded="pill">
                        Đến Đơn hàng của tôi
                    </v-btn>
                </div>

                <div class="text-center mb-6">
                    <p class="text-body-2 text-slate-600">
                        Vui lòng nhập <strong>Mã đơn hàng</strong> (VD: HD12345) hoặc <strong>Số điện thoại</strong> đã dùng khi đặt hàng.
                    </p>
                </div>

                <v-form @submit.prevent="handleTrackOrder" class="mx-auto" style="max-width: 520px">
                    <div class="mb-4">
                        <label class="text-subtitle-2 font-weight-bold text-slate-800 mb-2 d-block">Mã đơn hàng</label>
                        <v-text-field
                            v-model="trackingForm.maHoaDon"
                            placeholder="Nhập mã đơn hàng (VD: HD...)"
                            variant="outlined"
                            density="comfortable"
                            hide-details="auto"
                            prepend-inner-icon="mdi-barcode"
                            maxlength="30"
                            color="primary"
                            bg-color="white"
                        />
                    </div>

                    <div class="mb-6">
                        <label class="text-subtitle-2 font-weight-bold text-slate-800 mb-2 d-block">Số điện thoại nhận hàng</label>
                        <v-text-field
                            v-model="trackingForm.soDienThoai"
                            placeholder="Nhập số điện thoại (10 chữ số)"
                            variant="outlined"
                            density="comfortable"
                            hide-details="auto"
                            prepend-inner-icon="mdi-phone-outline"
                            maxlength="10"
                            color="primary"
                            bg-color="white"
                            :rules="[(v) => !v || /^0[3|5|7|8|9][0-9]{8}$/.test(v.trim()) || 'SĐT 10 số không hợp lệ (VD: 0912345678)']"
                        />
                    </div>

                    <v-btn
                        type="submit"
                        size="x-large"
                        rounded="pill"
                        block
                        class="text-none font-weight-bold shadow-md"
                        style="background: #1e257c; color: white !important"
                        :loading="trackingLoading"
                    >
                        <v-icon size="22" class="mr-2">mdi-magnify</v-icon>
                        Tra cứu đơn hàng ngay
                    </v-btn>
                </v-form>

                <div class="text-center mt-8 pt-6 border-t">
                    <p class="text-body-2 text-slate-600 mb-0">
                        Đã có tài khoản? 
                        <a :href="PATH.LOGIN" class="font-weight-bold hover:underline" style="color: #1e257c">Đăng nhập</a> 
                        để xem toàn bộ lịch sử 
                        <a :href="PATH.ORDERS" class="font-weight-bold hover:underline" style="color: #1e257c">Đơn mua của tôi</a>.
                    </p>
                </div>
            </v-card>
        </v-container>

        <CustomerChat />
        <MainFooter />
    </div>
</template>

<style scoped>
.order-tracking-page {
    font-family: inherit;
}
.shadow-md {
    box-shadow: 0 4px 14px rgba(30, 37, 124, 0.25) !important;
}
</style>
