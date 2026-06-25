<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import CartDrawer from '@/components/shared/CartDrawer.vue';
import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import { dichVuDatHang } from '@/services/public/dichVuDatHang';
import { dichVuKhachHang } from '@/services/public/dichVuKhachHang';
import { PATH } from '@/router/routePaths';
import { useLocation } from '@/composables/useLocation';

const router = useRouter();
const cartStore = useCartStore();
const authStore = useAuthStore();
const { provinces, districts, wards, loadingLocations, fetchProvinces, fetchDistricts, fetchWards, cleanName } = useLocation();

const loading = ref(false);
const voucherLoading = ref(false);

const shippingInfo = ref({
    tenNguoiNhan: '',
    soDienThoai: '',
    tinhThanh: null,
    quanHuyen: null,
    phuongXa: null,
    diaChi: '',
    isInitializing: false
});

const paymentMethod = ref('COD');
const ghiChu = ref('');

const availableVouchers = ref([]);
const selectedVoucher = ref(null);
const showVoucherDialog = ref(false);

import { watch } from 'vue';

watch(
    () => shippingInfo.value.tinhThanh,
    (newVal) => {
        if (!shippingInfo.value.isInitializing) {
            shippingInfo.value.quanHuyen = null;
            shippingInfo.value.phuongXa = null;
        }
        if (newVal) fetchDistricts(newVal);
    }
);

watch(
    () => shippingInfo.value.quanHuyen,
    (newVal) => {
        if (!shippingInfo.value.isInitializing) {
            shippingInfo.value.phuongXa = null;
        }
        if (newVal) fetchWards(newVal);
    }
);

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const FREE_SHIP_THRESHOLD = 5000000;
const SHIPPING_FEE = 30000;

const shippingFee = computed(() => {
    return cartStore.cartTotal >= FREE_SHIP_THRESHOLD ? 0 : SHIPPING_FEE;
});

const voucherDiscount = computed(() => {
    if (!selectedVoucher.value) return 0;
    const v = selectedVoucher.value;
    if (v.loaiPhieu === 'PHAN_TRAM' && v.phanTramGiamGia) {
        let discount = (cartStore.cartTotal * v.phanTramGiamGia) / 100;
        if (v.giamToiDa && discount > v.giamToiDa) discount = v.giamToiDa;
        return Math.floor(discount);
    }
    return v.soTienGiam || 0;
});

const totalAmount = computed(() => {
    const total = cartStore.cartTotal + shippingFee.value - voucherDiscount.value;
    return total > 0 ? total : 0;
});

const isShippingValid = computed(() => {
    return (
        shippingInfo.value.tenNguoiNhan && shippingInfo.value.tenNguoiNhan.trim() &&
        shippingInfo.value.soDienThoai && shippingInfo.value.soDienThoai.trim() &&
        shippingInfo.value.tinhThanh &&
        shippingInfo.value.quanHuyen &&
        shippingInfo.value.phuongXa &&
        shippingInfo.value.diaChi && shippingInfo.value.diaChi.trim()
    );
});

const remainingForFreeShip = computed(() => Math.max(0, FREE_SHIP_THRESHOLD - cartStore.cartTotal));

const estimatedDelivery = computed(() => {
    const now = new Date();
    const est = new Date(now);
    est.setDate(est.getDate() + (cartStore.cartTotal >= FREE_SHIP_THRESHOLD ? 5 : 7));
    return est.toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
});

const fetchUserProfile = async () => {
    if (authStore.isLoggedIn) {
        try {
            const res = await dichVuKhachHang.layThongTinCaNhan();
            if (res.success && res.data) {
                const profile = res.data;
                shippingInfo.value.tenNguoiNhan = profile.ten || profile.tenTaiKhoan || '';
                shippingInfo.value.soDienThoai = profile.sdt || '';
                if (profile.diaChiChiTiet) shippingInfo.value.diaChi = profile.diaChiChiTiet;

                if (profile.tinhThanh) {
                    shippingInfo.value.isInitializing = true;
                    await fetchProvinces();
                    const p = provinces.value.find(x => cleanName(x.name) === cleanName(profile.tinhThanh));
                    if (p) {
                        shippingInfo.value.tinhThanh = p.code;
                        await fetchDistricts(p.code);
                        const d = districts.value.find(x => cleanName(x.name) === cleanName(profile.quanHuyen));
                        if (d) {
                            shippingInfo.value.quanHuyen = d.code;
                            await fetchWards(d.code);
                            const w = wards.value.find(x => cleanName(x.name) === cleanName(profile.phuongXa));
                            if (w) { shippingInfo.value.phuongXa = w.code; }
                        }
                    }
                    setTimeout(() => { shippingInfo.value.isInitializing = false; }, 500);
                }
            }
        } catch (e) {
            console.error('Không thể lấy thông tin người dùng', e);
        }
    }
};

const fetchVouchers = async () => {
    voucherLoading.value = true;
    try {
        availableVouchers.value = await dichVuDatHang.layVoucherKhaDung(cartStore.cartTotal);
    } catch (error) {
        console.error('Error fetching vouchers:', error);
    } finally {
        voucherLoading.value = false;
    }
};

const selectVoucher = (voucher) => {
    selectedVoucher.value = voucher;
    showVoucherDialog.value = false;
};

const removeVoucher = () => {
    selectedVoucher.value = null;
};

const handleCheckout = async () => {
    if (!isShippingValid.value) {
        return;
    }
    loading.value = true;
    try {
        const p = provinces.value.find(x => x.code === shippingInfo.value.tinhThanh);
        const d = districts.value.find(x => x.code === shippingInfo.value.quanHuyen);
        const w = wards.value.find(x => x.code === shippingInfo.value.phuongXa);

        const checkoutData = {
            items: cartStore.cartItems.map((item) => ({
                idChiTietSanPham: item.idChiTietSanPham,
                soLuong: item.soLuong,
                giaDuKien: item.giaBan || 0
            })),
            tenNguoiNhan: shippingInfo.value.tenNguoiNhan,
            soDienThoai: shippingInfo.value.soDienThoai,
            tinhThanh: p ? p.name : shippingInfo.value.tinhThanh,
            quanHuyen: d ? d.name : shippingInfo.value.quanHuyen || '',
            phuongXa: w ? w.name : shippingInfo.value.phuongXa || '',
            diaChi: shippingInfo.value.diaChi,
            idPhieuGiamGia: selectedVoucher.value?.id || null,
            phuongThucThanhToan: paymentMethod.value,
            ghiChu: ghiChu.value
        };

        const response = await dichVuDatHang.datHang(checkoutData);
        if (response.success) {
            cartStore.clearCart();
            router.push(`${PATH.ORDER_SUCCESS}/${response.data.id}`);
        }
    } catch (error) {
        console.error('Checkout error:', error);
        alert(error.response?.data?.message || 'Đặt hàng thất bại. Vui lòng thử lại.');
    } finally {
        loading.value = false;
    }
};

onMounted(async () => {
    if (cartStore.isEmpty) {
        router.push(PATH.SHOES);
        return;
    }
    await cartStore.syncWithBackend();
    fetchProvinces();
    await fetchUserProfile();
    fetchVouchers();
});
</script>

<template>
    <div class="checkout-page bg-grey-lighten-4 min-vh-100 pb-16">
        <MainHeader />
        <div class="header-spacing" style="height: 104px"></div>

        <v-container style="max-width: 1200px">
            <!-- Breadcrumb -->
            <div class="d-flex align-center mb-6 pt-4">
                <router-link to="/" class="breadcrumb-link">Trang chủ</router-link>
                <v-icon size="14" class="mx-2 text-grey">mdi-chevron-right</v-icon>
                <router-link :to="PATH.SHOES" class="breadcrumb-link">Sản phẩm</router-link>
                <v-icon size="14" class="mx-2 text-grey">mdi-chevron-right</v-icon>
                <span class="text-body-1 font-weight-bold text-black">Thanh toán</span>
            </div>

            <!-- Page Title -->
            <div class="d-flex align-center mb-8">
                <div class="section-icon mr-3">
                    <v-icon size="18" color="white">mdi-credit-card-check-outline</v-icon>
                </div>
                <div>
                    <h1 class="text-h4 font-weight-black mb-1">Thanh toán đơn hàng</h1>
                    <p class="text-body-2 text-grey-darken-1 mb-0">Kiểm tra và xác nhận thông tin đặt hàng</p>
                </div>
            </div>

            <v-row>
                <!-- Left Column -->
                <v-col cols="12" md="7">
                    <!-- Shipping Info -->
                    <v-card class="checkout-card rounded-xl mb-6" elevation="0">
                        <div class="card-accent-line"></div>
                        <div class="pa-8">
                            <div class="d-flex align-center mb-6">
                                <div class="step-number mr-4 font-weight-black">1</div>
                                <div>
                                    <h2 class="text-h5 font-weight-bold mb-1">Thông tin giao hàng</h2>
                                    <p class="text-caption text-grey mb-0">Nhập địa chỉ nơi bạn muốn nhận hàng</p>
                                </div>
                            </div>

                            <v-row>
                                <v-col cols="12" sm="6">
                                    <v-text-field v-model="shippingInfo.tenNguoiNhan" label="Tên người nhận"
                                        variant="outlined" density="comfortable" hide-details="auto"
                                        prepend-inner-icon="mdi-account-outline"
                                        :rules="[(v) => !!v || 'Vui lòng nhập tên']"></v-text-field>
                                </v-col>
                                <v-col cols="12" sm="6">
                                    <v-text-field v-model="shippingInfo.soDienThoai" label="Số điện thoại"
                                        variant="outlined" density="comfortable" hide-details="auto"
                                        prepend-inner-icon="mdi-phone-outline"
                                        :rules="[(v) => !!v || 'Vui lòng nhập SĐT']"></v-text-field>
                                </v-col>
                                <v-col cols="12" sm="4">
                                    <v-select v-model="shippingInfo.tinhThanh" :items="provinces" item-title="name" item-value="code" label="Tỉnh/Thành phố"
                                        variant="outlined" density="comfortable" hide-details="auto"
                                        prepend-inner-icon="mdi-map-marker-outline"
                                        :loading="loadingLocations.provinces"
                                        :rules="[(v) => !!v || 'Vui lòng chọn Tỉnh/Thành']"></v-select>
                                </v-col>
                                <v-col cols="12" sm="4">
                                    <v-select v-model="shippingInfo.quanHuyen" :items="districts" item-title="name" item-value="code" label="Quận/Huyện"
                                        variant="outlined" density="comfortable" hide-details="auto"
                                        :disabled="!shippingInfo.tinhThanh"
                                        :loading="loadingLocations.districts"
                                        :rules="[(v) => !!v || 'Vui lòng chọn Quận/Huyện']"></v-select>
                                </v-col>
                                <v-col cols="12" sm="4">
                                    <v-select v-model="shippingInfo.phuongXa" :items="wards" item-title="name" item-value="code" label="Phường/Xã"
                                        variant="outlined" density="comfortable" hide-details="auto"
                                        :disabled="!shippingInfo.quanHuyen"
                                        :loading="loadingLocations.wards"
                                        :rules="[(v) => !!v || 'Vui lòng chọn Phường/Xã']"></v-select>
                                </v-col>
                                <v-col cols="12">
                                    <v-text-field v-model="shippingInfo.diaChi" label="Địa chỉ chi tiết (số nhà, tên đường)"
                                        variant="outlined" density="comfortable" hide-details="auto"
                                        prepend-inner-icon="mdi-home-outline"
                                        :rules="[(v) => !!v || 'Vui lòng nhập địa chỉ']"></v-text-field>
                                </v-col>
                                <v-col cols="12">
                                    <v-textarea v-model="ghiChu" label="Ghi chú cho đơn hàng (không bắt buộc)"
                                        variant="outlined" density="comfortable" rows="2" hide-details
                                        prepend-inner-icon="mdi-note-text-outline"></v-textarea>
                                </v-col>
                            </v-row>
                        </div>
                    </v-card>

                    <!-- Payment Method -->
                    <v-card class="checkout-card rounded-xl mb-6" elevation="0">
                        <div class="card-accent-line accent-green"></div>
                        <div class="pa-8">
                            <div class="d-flex align-center mb-6">
                                <div class="step-number step-2 mr-4 font-weight-black">2</div>
                                <div>
                                    <h2 class="text-h5 font-weight-bold mb-1">Phương thức thanh toán</h2>
                                    <p class="text-caption text-grey mb-0">Chọn cách bạn muốn thanh toán cho đơn hàng</p>
                                </div>
                            </div>

                            <div class="payment-options">
                                <div class="payment-option d-flex align-center pa-5 rounded-xl mb-4"
                                    :class="{ selected: paymentMethod === 'COD' }" @click="paymentMethod = 'COD'">
                                    <div class="radio-indicator mr-4">
                                        <div v-if="paymentMethod === 'COD'" class="radio-inner"></div>
                                    </div>
                                    <div class="payment-icon-wrapper mr-4">
                                        <v-icon size="28" color="blue-darken-4">mdi-cash</v-icon>
                                    </div>
                                    <div class="flex-grow-1">
                                        <h4 class="font-weight-bold text-body-1 mb-1">Thanh toán khi nhận hàng (COD)</h4>
                                        <p class="text-caption text-grey mb-0">Thanh toán bằng tiền mặt khi nhận hàng</p>
                                    </div>
                                    <v-chip v-if="paymentMethod === 'COD'" size="x-small" color="green" variant="flat" class="font-weight-bold">ĐÃ CHỌN</v-chip>
                                </div>

                                <div class="payment-option d-flex align-center pa-5 rounded-xl"
                                    :class="{ selected: paymentMethod === 'VNPAY' }" @click="paymentMethod = 'VNPAY'">
                                    <div class="radio-indicator mr-4">
                                        <div v-if="paymentMethod === 'VNPAY'" class="radio-inner"></div>
                                    </div>
                                    <div class="payment-icon-wrapper mr-4">
                                        <span class="vnpay-logo font-weight-black"><span class="text-blue-darken-4">VN</span><span class="text-red">PAY</span></span>
                                    </div>
                                    <div class="flex-grow-1">
                                        <h4 class="font-weight-bold text-body-1 mb-1">Thanh toán trực tuyến VNPay</h4>
                                        <p class="text-caption text-grey mb-0">Hỗ trợ Thẻ ATM, Visa, MasterCard, QR Code</p>
                                    </div>
                                    <v-chip v-if="paymentMethod === 'VNPAY'" size="x-small" color="green" variant="flat" class="font-weight-bold">ĐÃ CHỌN</v-chip>
                                </div>
                            </div>
                        </div>
                    </v-card>

                    <!-- Estimated Delivery -->
                    <v-card class="checkout-card rounded-xl" elevation="0">
                        <div class="card-accent-line accent-orange"></div>
                        <div class="pa-8">
                            <div class="d-flex align-center mb-4">
                                <div class="step-number step-3 mr-4 font-weight-black">3</div>
                                <div>
                                    <h2 class="text-h5 font-weight-bold mb-1">Thông tin giao hàng dự kiến</h2>
                                </div>
                            </div>
                            <div class="delivery-estimate pa-5 rounded-xl">
                                <div class="d-flex align-center">
                                    <div class="delivery-icon mr-4">
                                        <v-icon size="32" color="blue-darken-4">mdi-truck-fast-outline</v-icon>
                                    </div>
                                    <div>
                                        <p class="text-body-2 font-weight-bold mb-1">Dự kiến giao hàng vào</p>
                                        <p class="text-h6 font-weight-black text-blue-darken-4 mb-1">{{ estimatedDelivery }}</p>
                                        <p class="text-caption text-grey-darken-1 mb-0 d-flex align-center">
                                            <v-icon size="14" class="mr-1">mdi-clock-outline</v-icon>
                                            Thời gian giao hàng từ 8:00 - 20:00 tất cả các ngày trong tuần
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </v-card>
                </v-col>

                <!-- Right Column: Order Summary -->
                <v-col cols="12" md="5">
                    <div class="order-summary-sticky">
                        <v-card class="checkout-card rounded-xl" elevation="0">
                            <div class="pa-6">
                                <div class="d-flex align-center mb-6">
                                    <div class="summary-icon mr-3">
                                        <v-icon size="20" color="white">mdi-receipt-text-outline</v-icon>
                                    </div>
                                    <h3 class="text-h6 font-weight-bold mb-0">Tóm tắt đơn hàng</h3>
                                </div>

                                <!-- Product List -->
                                <div class="product-list mb-6">
                                    <div v-for="item in cartStore.cartItems" :key="item.idChiTietSanPham"
                                        class="product-item d-flex gap-4 py-4">
                                        <div class="product-img-wrapper position-relative flex-shrink-0">
                                            <v-img :src="item.hinhAnh || 'https://via.placeholder.com/150?text=Sản+Phẩm'" cover width="72" height="72"
                                                class="rounded-xl bg-grey-lighten-4"></v-img>
                                            <span class="product-qty-badge">{{ item.soLuong }}</span>
                                        </div>
                                        <div class="flex-grow-1 d-flex flex-column">
                                            <p class="text-body-2 font-weight-bold mb-1">{{ item.tenSanPham }}</p>
                                            <p class="text-caption text-grey-darken-1 mb-1">
                                                {{ item.tenMauSac }} / {{ item.tenKichThuoc }}
                                            </p>
                                            <div class="mt-auto d-flex justify-space-between align-center">
                                                <span class="text-caption text-grey">Đơn giá: {{ formatPrice(item.giaBan) }}</span>
                                                <span class="text-body-2 font-weight-black">{{ formatPrice(item.giaBan * item.soLuong) }}</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <v-divider class="mb-4" />

                                <!-- Voucher -->
                                <div class="voucher-section mb-4">
                                    <div v-if="selectedVoucher"
                                        class="voucher-applied d-flex align-center justify-space-between pa-4 rounded-xl mb-3">
                                        <div class="d-flex align-center gap-3">
                                            <div class="voucher-icon-small">
                                                <v-icon size="20" color="white">mdi-ticket-percent</v-icon>
                                            </div>
                                            <div>
                                                <span class="text-body-2 font-weight-bold text-blue-darken-4 d-block">{{ selectedVoucher.ten || selectedVoucher.ma }}</span>
                                                <span class="text-caption text-blue-darken-2 font-weight-medium">Giảm {{ formatPrice(voucherDiscount) }}</span>
                                            </div>
                                        </div>
                                        <v-btn icon variant="text" size="small" color="red-lighten-1" @click="removeVoucher">
                                            <v-icon size="18">mdi-close-circle</v-icon>
                                        </v-btn>
                                    </div>
                                    <v-btn v-else variant="outlined" block class="voucher-btn text-none rounded-xl"
                                        color="blue-darken-4" @click="showVoucherDialog = true">
                                        <span class="font-weight-bold d-flex align-center flex-grow-1">
                                            <v-icon class="mr-2" size="20" color="blue-darken-4">mdi-ticket-percent-outline</v-icon>
                                            Chọn hoặc nhập mã giảm giá
                                        </span>
                                        <v-icon size="18" color="blue-darken-4">mdi-chevron-right</v-icon>
                                    </v-btn>
                                </div>

                                <!-- Price Breakdown -->
                                <div class="price-breakdown pa-4 rounded-xl mb-4">
                                    <div class="d-flex justify-space-between mb-3">
                                        <span class="text-body-2 text-grey-darken-1">Tạm tính ({{ cartStore.cartCount }} sản phẩm)</span>
                                        <span class="text-body-2 font-weight-bold">{{ formatPrice(cartStore.cartTotal) }}</span>
                                    </div>
                                    <div class="d-flex justify-space-between mb-3">
                                        <span class="text-body-2 text-grey-darken-1">Phí vận chuyển</span>
                                        <span class="text-body-2 font-weight-medium"
                                            :class="{ 'text-green': shippingFee === 0 }">
                                            <v-icon v-if="shippingFee === 0" size="14" color="green" class="mr-1">mdi-check-circle</v-icon>
                                            {{ shippingFee === 0 ? 'Miễn phí' : formatPrice(shippingFee) }}
                                        </span>
                                    </div>
                                    <div v-if="voucherDiscount > 0" class="d-flex justify-space-between mb-1">
                                        <span class="text-body-2 text-blue-darken-3">
                                            <v-icon size="14" color="blue-darken-3" class="mr-1">mdi-ticket-percent</v-icon>
                                            Giảm giá
                                        </span>
                                        <span class="text-body-2 font-weight-bold text-blue-darken-3">
                                            -{{ formatPrice(voucherDiscount) }}
                                        </span>
                                    </div>
                                </div>

                                <!-- Total -->
                                <div class="d-flex justify-space-between align-center mb-4 pa-4 bg-blue-darken-4 rounded-xl">
                                    <span class="text-body-1 font-weight-bold text-white">Tổng thanh toán</span>
                                    <span class="text-h5 font-weight-black text-white">{{ formatPrice(totalAmount) }}</span>
                                </div>

                                <!-- Free ship notice -->
                                <p v-if="cartStore.cartTotal < FREE_SHIP_THRESHOLD"
                                    class="text-caption text-center text-grey-darken-1 mb-4 px-2">
                                    <v-icon size="14" class="mr-1 pb-1">mdi-truck-fast-outline</v-icon>
                                    Mua thêm <strong class="text-orange-darken-3">{{ formatPrice(remainingForFreeShip) }}</strong> để được
                                    <strong class="text-orange-darken-3">Miễn phí vận chuyển</strong>
                                </p>

                                <v-btn color="black" rounded="pill" size="x-large" block
                                    class="font-weight-bold text-none place-order-btn elevation-2 text-white"
                                    :loading="loading" :disabled="!isShippingValid"
                                    @click="handleCheckout">
                                    <v-icon class="mr-2">{{ paymentMethod === 'VNPAY' ? 'mdi-qrcode-scan' : 'mdi-lock-outline' }}</v-icon>
                                    {{ paymentMethod === 'VNPAY' ? 'Thanh toán bằng VNPay' : 'Hoàn tất đặt hàng' }}
                                </v-btn>
                            </div>
                        </v-card>
                    </div>
                </v-col>
            </v-row>
        </v-container>

        <!-- Voucher Dialog -->
        <v-dialog v-model="showVoucherDialog" max-width="520" class="voucher-dialog">
            <v-card class="rounded-xl">
                <div class="voucher-dialog-header pa-6">
                    <div class="d-flex align-center justify-space-between">
                        <div class="d-flex align-center">
                            <v-icon size="24" color="white" class="mr-3">mdi-ticket-percent</v-icon>
                            <h3 class="text-h6 font-weight-bold text-white mb-0">Chọn mã giảm giá</h3>
                        </div>
                        <v-btn icon variant="text" size="small" color="white" @click="showVoucherDialog = false">
                            <v-icon>mdi-close</v-icon>
                        </v-btn>
                    </div>
                </div>
                <v-card-text class="pa-6">
                    <div v-if="voucherLoading" class="text-center py-10">
                        <v-progress-circular indeterminate color="blue-darken-4" size="40"></v-progress-circular>
                        <p class="text-body-2 text-grey mt-4">Đang tải mã giảm giá...</p>
                    </div>
                    <div v-else-if="availableVouchers.length === 0" class="text-center py-10">
                        <div class="empty-voucher-icon mx-auto mb-4">
                            <v-icon size="48" color="grey-lighten-2">mdi-ticket-outline</v-icon>
                        </div>
                        <p class="text-body-1 text-grey-darken-1 font-weight-medium mb-2">Không có mã giảm giá</p>
                        <p class="text-caption text-grey">Bạn chưa có mã giảm giá nào phù hợp với đơn hàng này.</p>
                    </div>
                    <div v-else class="voucher-list">
                        <div v-for="v in availableVouchers" :key="v.id"
                            class="voucher-item d-flex pa-4 rounded-xl mb-3"
                            :class="{ selected: selectedVoucher?.id === v.id }"
                            @click="selectVoucher(v)">
                            <div class="voucher-badge mr-4">
                                <span class="voucher-badge-text">{{ v.loaiPhieu === 'PHAN_TRAM' ? v.phanTramGiamGia + '%' : 'Giảm' }}</span>
                            </div>
                            <div class="flex-grow-1">
                                <div class="d-flex align-center justify-space-between mb-1">
                                    <h4 class="text-body-2 font-weight-bold">{{ v.ten || v.ma }}</h4>
                                    <v-icon v-if="selectedVoucher?.id === v.id" color="blue-darken-4" size="22">mdi-check-circle</v-icon>
                                    <div v-else class="unselected-radio"></div>
                                </div>
                                <p class="text-caption text-grey-darken-1 mb-1">
                                    <span v-if="v.loaiPhieu === 'PHAN_TRAM'">Giảm {{ v.phanTramGiamGia }}%{{ v.giamToiDa ? ` (Tối đa ${formatPrice(v.giamToiDa)})` : '' }}</span>
                                    <span v-else>Giảm {{ formatPrice(v.soTienGiam) }} trực tiếp</span>
                                </p>
                                <p v-if="v.donHangToiThieu" class="text-caption text-orange-darken-3 font-weight-bold mb-0">
                                    <v-icon size="12" class="mr-1">mdi-cart-outline</v-icon>
                                    Đơn tối thiểu {{ formatPrice(v.donHangToiThieu) }}
                                </p>
                            </div>
                        </div>
                    </div>
                </v-card-text>
            </v-card>
        </v-dialog>

        <CartDrawer />
        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.breadcrumb-link {
    text-decoration: none;
    font-size: 0.85rem;
    font-weight: 500;
    color: #757575;
    transition: color 0.2s;
    &:hover { color: #111; }
}

.section-icon {
    width: 40px;
    height: 40px;
    background: linear-gradient(135deg, #0d47a1, #1976d2);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(13, 71, 161, 0.25);
}

.checkout-card {
    border: 1px solid #e8e8e8;
    background: #fff;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.02) !important;
    position: relative;
    overflow: hidden;
    transition: box-shadow 0.3s;

    &:hover {
        box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04) !important;
    }
}

.card-accent-line {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, #0d47a1, #1976d2);
    &.accent-green {
        background: linear-gradient(90deg, #2e7d32, #4caf50);
    }
    &.accent-orange {
        background: linear-gradient(90deg, #e65100, #ff9800);
    }
}

.step-number {
    width: 44px;
    height: 44px;
    background: linear-gradient(135deg, #0d47a1, #1976d2);
    color: white;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.1rem;
    box-shadow: 0 4px 12px rgba(13, 71, 161, 0.25);
    flex-shrink: 0;
}

.step-2 {
    background: linear-gradient(135deg, #1b5e20, #388e3c);
    box-shadow: 0 4px 12px rgba(27, 94, 32, 0.25);
}

.step-3 {
    background: linear-gradient(135deg, #e65100, #f57c00);
    box-shadow: 0 4px 12px rgba(230, 81, 0, 0.25);
}

.payment-options {
    .payment-option {
        border: 2px solid #eee;
        cursor: pointer;
        transition: all 0.3s ease;
        background: #fff;
        position: relative;
        overflow: hidden;

        &::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            width: 4px;
            height: 100%;
            background: transparent;
            transition: background 0.3s;
        }

        &:hover {
            border-color: #ccc;
            background: #fafafa;
        }

        &.selected {
            border-color: #0d47a1;
            background: #f3f8ff;
            &::before { background: #0d47a1; }
        }
    }
}

.radio-indicator {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    border: 2px solid #ccc;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    transition: all 0.3s;

    .radio-inner {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        background: #0d47a1;
        animation: radioPop 0.2s ease;
    }
}

@keyframes radioPop {
    0% { transform: scale(0); }
    50% { transform: scale(1.2); }
    100% { transform: scale(1); }
}

.payment-icon-wrapper {
    width: 48px;
    height: 48px;
    background: #e3f2fd;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.vnpay-logo {
    font-size: 1.1rem;
    letter-spacing: -1px;
}

.delivery-estimate {
    background: linear-gradient(135deg, #e3f2fd, #f3f8ff);
    border: 1px solid #bbdefb;
}

.delivery-icon {
    width: 56px;
    height: 56px;
    background: white;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 8px rgba(13, 71, 161, 0.1);
}

.summary-icon {
    width: 36px;
    height: 36px;
    background: linear-gradient(135deg, #0d47a1, #1976d2);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.order-summary-sticky {
    position: sticky;
    top: 130px;
}

.product-list {
    .product-item {
        border-bottom: 1px solid #f5f5f5;
        &:last-child { border-bottom: none; }
    }
    .product-img-wrapper {
        width: 72px;
        height: 72px;
        .product-qty-badge {
            position: absolute;
            top: -6px;
            right: -6px;
            background: #000;
            color: white;
            font-size: 0.65rem;
            font-weight: 700;
            min-width: 20px;
            height: 20px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 0 4px;
            z-index: 2;
            box-shadow: 0 2px 6px rgba(0,0,0,0.2);
        }
    }
}

.voucher-btn {
    border-color: #0d47a1;
    border-style: dashed;
    border-width: 2px;
    height: 52px !important;
    background: #eef5ff;
    transition: all 0.3s;
    &:hover {
        background: #ddeeff;
    }
}

.voucher-applied {
    background: linear-gradient(135deg, #e3f2fd, #f3f8ff);
    border: 1px solid #90caf9;
    border-radius: 12px;
    .voucher-icon-small {
        width: 36px;
        height: 36px;
        background: #0d47a1;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
    }
}

.price-breakdown {
    background: #f8f9fa;
}

.place-order-btn {
    height: 56px !important;
    font-size: 1rem;
    letter-spacing: 0.3px;
    transition: all 0.2s;
    &:hover {
        transform: translateY(-1px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15) !important;
    }
    &:disabled {
        opacity: 0.6;
    }
}

/* Voucher Dialog */
.voucher-dialog-header {
    background: linear-gradient(135deg, #0d47a1, #1976d2);
}

.empty-voucher-icon {
    width: 80px;
    height: 80px;
    background: #f5f5f5;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

.voucher-list {
    max-height: 420px;
    overflow-y: auto;
}

.voucher-item {
    border: 2px solid #eee;
    cursor: pointer;
    transition: all 0.2s ease;
    background: #fff;
    &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 16px rgba(13, 71, 161, 0.08);
    }
    &.selected {
        border-color: #0d47a1;
        background: #f3f8ff;
    }
}

.voucher-badge {
    width: 56px;
    height: 56px;
    background: linear-gradient(135deg, #0d47a1, #1976d2);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: 0 4px 10px rgba(13, 71, 161, 0.3);
    .voucher-badge-text {
        color: white;
        font-weight: 700;
        font-size: 0.85rem;
    }
}

.unselected-radio {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    border: 2px solid #ddd;
}
</style>
