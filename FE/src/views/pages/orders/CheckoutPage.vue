<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';

import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import apiService from '@/services/apiService';
import { dichVuDatHang } from '@/services/public/dichVuDatHang';
import { dichVuKhachHang } from '@/services/public/dichVuKhachHang';
import { dichVuVnPay } from '@/views/modules/banhang/composables/dichVuVnPay';
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
    email: '',
    tinhThanh: null,
    quanHuyen: null,
    phuongXa: null,
    diaChi: '',
    isInitializing: false
});

const paymentMethod = ref('COD');
const ghiChu = ref('');

const vnpayDialog = ref({
    show: false,
    loading: false,
    checking: false,
    orderId: '',
    orderCode: '',
    amount: 0,
    qrUrl: '',
    paymentUrl: '',
    pollInterval: null
});

const closeVnPayDialog = () => {
    if (vnpayDialog.value.pollInterval) {
        clearInterval(vnpayDialog.value.pollInterval);
        vnpayDialog.value.pollInterval = null;
    }
    vnpayDialog.value.show = false;
};

const checkOnlineOrderStatus = async (orderId) => {
    try {
        const order = await dichVuDatHang.layChiTietDonHang(orderId);
        if (order && (order.trangThai === 'XAC_NHAN' || order.trangThaiDisplay === 'Đã xác nhận')) {
            closeVnPayDialog();
            cartStore.clearCart();
            router.push(`${PATH.ORDER_SUCCESS}/${orderId}`);
        }
    } catch (e) {
        console.error('Poll order status error:', e);
    }
};

const onConfirmPaidOnline = async () => {
    vnpayDialog.value.checking = true;
    try {
        await dichVuDatHang.xacNhanDonHangQr(vnpayDialog.value.orderId);
        closeVnPayDialog();
        cartStore.clearCart();
        router.push(`${PATH.ORDER_SUCCESS}/${vnpayDialog.value.orderId}`);
    } catch (error) {
        alert('Xác nhận thanh toán thất bại hoặc chưa nhận được tiền.');
    } finally {
        vnpayDialog.value.checking = false;
    }
};

const openVnPayGateway = () => {
    if (vnpayDialog.value.paymentUrl) {
        window.open(vnpayDialog.value.paymentUrl, '_blank', 'width=800,height=600');
    }
};

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

const FREE_SHIP_THRESHOLD = ref(500000);
const baseShippingFee = ref(30000);
const calculatedShippingFee = ref(30000);

const fetchShippingConfig = async () => {
    try {
        const response = await apiService.get('/config/shipping');
        if (response.data?.success) {
            FREE_SHIP_THRESHOLD.value = response.data.data.freeShipThreshold;
            baseShippingFee.value = response.data.data.baseFee;
            calculatedShippingFee.value = response.data.data.baseFee;
        }
    } catch (e) {
        console.error('Lỗi khi lấy cấu hình phí vận chuyển', e);
    }
};

const calculateDistanceFee = async () => {
    // Mock distance based on province selection since no map API is used
    let distance = 5; // Default < 5km (Nội thành Hà Nội)
    if (shippingInfo.value.tinhThanh) {
        const p = provinces.value.find(x => x.code === shippingInfo.value.tinhThanh);
        if (p && !p.name.includes('Hà Nội')) {
            distance = 25; // Mock Ngoại tỉnh
        }
    }
    try {
        const response = await apiService.get('/config/shipping/calculate', { params: { distance } });
        if (response.data?.success) {
            calculatedShippingFee.value = response.data.data.fee;
        }
    } catch (e) {
        console.error('Lỗi khi tính phí vận chuyển theo khoảng cách', e);
    }
};

watch(() => shippingInfo.value.tinhThanh, () => {
    calculateDistanceFee();
});

const shippingFee = computed(() => {
    return cartStore.cartTotal >= FREE_SHIP_THRESHOLD.value ? 0 : calculatedShippingFee.value;
});

// cartStore.cartTotal đã là tổng SAU đợt giảm giá (server trả về giaBan đã giảm).
// Tính tổng theo GIÁ GỐC và số tiền đợt giảm giá để hiển thị minh bạch trên phần tạm tính.
const originalSubtotal = computed(() =>
    cartStore.cartItems.reduce((sum, item) => sum + ((item.giaGoc || item.giaBan || 0) * item.soLuong), 0)
);
const campaignDiscount = computed(() => Math.max(0, originalSubtotal.value - cartStore.cartTotal));

const campaignDiscountPercent = computed(() => {
    if (originalSubtotal.value === 0) return 0;
    return Math.round((campaignDiscount.value / originalSubtotal.value) * 100);
});

const calcVoucherDiscount = (v) => {
    if (!v) return 0;
    if (v.loaiPhieu === 'PHAN_TRAM' && v.phanTramGiamGia) {
        let discount = (cartStore.cartTotal * v.phanTramGiamGia) / 100;
        if (v.giamToiDa && discount > v.giamToiDa) discount = v.giamToiDa;
        return Math.floor(discount);
    }
    return v.soTienGiam || 0;
};

const voucherDiscount = computed(() => calcVoucherDiscount(selectedVoucher.value));

const bestVoucher = computed(() => {
    if (!availableVouchers.value?.length) return null;
    let best = null;
    let bestValue = 0;
    for (const v of availableVouchers.value) {
        const d = calcVoucherDiscount(v);
        if (d > bestValue) {
            bestValue = d;
            best = v;
        }
    }
    return best;
});

const bestVoucherDiscount = computed(() => calcVoucherDiscount(bestVoucher.value));

const isBestSelected = computed(
    () => bestVoucher.value && selectedVoucher.value && bestVoucher.value.id === selectedVoucher.value.id
);

const totalAmount = computed(() => {
    const total = cartStore.cartTotal + shippingFee.value - voucherDiscount.value;
    return total > 0 ? total : 0;
});

const isShippingValid = computed(() => {
    return (
        shippingInfo.value.tenNguoiNhan && shippingInfo.value.tenNguoiNhan.trim() &&
        shippingInfo.value.soDienThoai && shippingInfo.value.soDienThoai.trim() &&
        shippingInfo.value.email && shippingInfo.value.email.trim() && /.+@.+\..+/.test(shippingInfo.value.email.trim()) &&
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
                shippingInfo.value.email = profile.email || '';
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
        await cartStore.syncWithBackend();
        if (cartStore.isEmpty) {
            alert('Giỏ hàng của bạn đã thay đổi. Vui lòng kiểm tra lại.');
            router.push(PATH.SHOES);
            return;
        }
        if (selectedVoucher.value) {
            await fetchVouchers();
            const stillValid = availableVouchers.value.some(v => v.id === selectedVoucher.value.id);
            if (!stillValid) {
                const removed = selectedVoucher.value;
                selectedVoucher.value = null;
                alert(`Phiếu giảm giá "${removed.ten || removed.ma}" không còn hiệu lực và đã được gỡ bỏ. Vui lòng kiểm tra lại đơn hàng trước khi thanh toán.`);
                return;
            }
        }

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
            email: shippingInfo.value.email,
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
            const createdOrder = response.data;
            if (paymentMethod.value === 'VNPAY') {
                try {
                    const payload = {
                        amount: totalAmount.value,
                        orderId: createdOrder.id,
                        orderInfo: 'Thanh toan hoa don ' + (createdOrder.maHoaDon || createdOrder.id),
                        returnUrl: `${window.location.origin}/order-success/${createdOrder.id}`
                    };
                    const vnpData = await dichVuVnPay.createPaymentUrl(payload);
                    if (vnpData && vnpData.paymentUrl) {
                        vnpayDialog.value = {
                            show: true,
                            loading: false,
                            checking: false,
                            orderId: createdOrder.id,
                            orderCode: createdOrder.maHoaDon || createdOrder.id,
                            amount: totalAmount.value,
                            qrUrl: `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(vnpData.paymentUrl)}`,
                            paymentUrl: vnpData.paymentUrl,
                            pollInterval: setInterval(() => checkOnlineOrderStatus(createdOrder.id), 3000)
                        };
                        loading.value = false;
                        return;
                    }
                } catch (e) {
                    console.error('Lỗi tạo mã QR VNPay:', e);
                }
            }
            cartStore.clearCart();
            router.push(`${PATH.ORDER_SUCCESS}/${createdOrder.id}`);
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
    fetchShippingConfig();
    await fetchUserProfile();
    fetchVouchers();
});
</script>

<template>
    <div class="checkout-page bg-white min-vh-100 pb-16">
        <MainHeader />
        <div class="header-spacing" style="height: 124px"></div>

        <v-container style="max-width: 1480px">
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
                    <v-icon size="18" style="color: #ffffff !important;">mdi-credit-card-check-outline</v-icon>
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
                    <div class="section-block mb-6">
                        <div class="pa-8">
                            <div class="d-flex align-center mb-6">
                                <div class="step-number mr-4 font-weight-bold">
                                    <v-icon color="white">mdi-map-marker-outline</v-icon>
                                </div>
                                <div>
                                    <h2 class="text-h5 font-weight-bold mb-1">Thông tin giao hàng</h2>
                                    <p class="text-caption text-grey mb-0">Nhập địa chỉ nơi bạn muốn nhận hàng</p>
                                </div>
                            </div>

                            <v-row>
                                <v-col cols="12" sm="6">
                                    <v-text-field v-model="shippingInfo.tenNguoiNhan" label="Tên người nhận"
                                        variant="outlined" hide-details="auto"
                                        prepend-inner-icon="mdi-account-outline"
                                        :rules="[(v) => !!v || 'Vui lòng nhập tên']"></v-text-field>
                                </v-col>
                                <v-col cols="12" sm="6">
                                    <v-text-field v-model="shippingInfo.soDienThoai" label="Số điện thoại"
                                        variant="outlined" hide-details="auto"
                                        prepend-inner-icon="mdi-phone-outline"
                                        :rules="[(v) => !!v || 'Vui lòng nhập SĐT']"></v-text-field>
                                </v-col>
                                <v-col cols="12">
                                    <v-text-field v-model="shippingInfo.email" label="Email nhận thông báo đơn hàng"
                                        variant="outlined" hide-details="auto"
                                        prepend-inner-icon="mdi-email-outline"
                                        :rules="[
                                            (v) => !!v || 'Vui lòng nhập Email',
                                            (v) => /.+@.+\..+/.test(v) || 'Email không hợp lệ'
                                        ]"></v-text-field>
                                </v-col>
                                <v-col cols="12" sm="4">
                                    <v-select v-model="shippingInfo.tinhThanh" :items="provinces" item-title="name" item-value="code" label="Tỉnh/Thành phố"
                                        variant="outlined" hide-details="auto"
                                        prepend-inner-icon="mdi-map-marker-outline"
                                        :loading="loadingLocations.provinces"
                                        :rules="[(v) => !!v || 'Vui lòng chọn Tỉnh/Thành']"></v-select>
                                </v-col>
                                <v-col cols="12" sm="4">
                                    <v-select v-model="shippingInfo.quanHuyen" :items="districts" item-title="name" item-value="code" label="Quận/Huyện"
                                        variant="outlined" hide-details="auto"
                                        :disabled="!shippingInfo.tinhThanh"
                                        :loading="loadingLocations.districts"
                                        :rules="[(v) => !!v || 'Vui lòng chọn Quận/Huyện']"></v-select>
                                </v-col>
                                <v-col cols="12" sm="4">
                                    <v-select v-model="shippingInfo.phuongXa" :items="wards" item-title="name" item-value="code" label="Phường/Xã"
                                        variant="outlined" hide-details="auto"
                                        :disabled="!shippingInfo.quanHuyen"
                                        :loading="loadingLocations.wards"
                                        :rules="[(v) => !!v || 'Vui lòng chọn Phường/Xã']"></v-select>
                                </v-col>
                                <v-col cols="12">
                                    <v-text-field v-model="shippingInfo.diaChi" label="Địa chỉ chi tiết (số nhà, tên đường)"
                                        variant="outlined" hide-details="auto"
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
                    </div>

                    <!-- Payment Method -->
                    <div class="section-block mb-6">
                        <div class="pa-8">
                            <div class="d-flex align-center mb-6">
                                <div class="step-number mr-4 font-weight-bold">
                                    <v-icon color="white">mdi-credit-card-outline</v-icon>
                                </div>
                                <div>
                                    <h2 class="text-h5 font-weight-bold mb-1">Phương thức thanh toán</h2>
                                    <p class="text-caption text-grey mb-0">Chọn cách bạn muốn thanh toán cho đơn hàng</p>
                                </div>
                            </div>

                            <div class="payment-options">
                                <div class="payment-option d-flex align-center pa-5 mb-4"
                                    :class="{ selected: paymentMethod === 'COD' }" @click="paymentMethod = 'COD'">
                                    <div class="radio-indicator mr-4">
                                        <div v-if="paymentMethod === 'COD'" class="radio-inner"></div>
                                    </div>
                                    <div class="flex-grow-1">
                                        <h4 class="font-weight-bold text-body-1 mb-1">Thanh toán khi nhận hàng (COD)</h4>
                                        <p class="text-caption text-grey mb-0">Thanh toán bằng tiền mặt khi nhận hàng</p>
                                    </div>
                                    <v-chip v-if="paymentMethod === 'COD'" size="x-small" color="#1e257c" variant="flat" class="font-weight-bold text-white">ĐÃ CHỌN</v-chip>
                                </div>

                                <div class="payment-option d-flex align-center pa-5"
                                    :class="{ selected: paymentMethod === 'VNPAY' }" @click="paymentMethod = 'VNPAY'">
                                    <div class="radio-indicator mr-4">
                                        <div v-if="paymentMethod === 'VNPAY'" class="radio-inner"></div>
                                    </div>
                                    <div class="flex-grow-1">
                                        <h4 class="font-weight-bold text-body-1 mb-1">Thanh toán trực tuyến VNPay</h4>
                                        <p class="text-caption text-grey mb-0">Hỗ trợ Thẻ ATM, Visa, MasterCard, QR Code</p>
                                    </div>
                                    <v-chip v-if="paymentMethod === 'VNPAY'" size="x-small" color="#1e257c" variant="flat" class="font-weight-bold text-white">ĐÃ CHỌN</v-chip>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Estimated Delivery -->
                    <div class="section-block">
                        <div class="pa-8">
                            <div class="d-flex align-center mb-4">
                                <div class="step-number mr-4 font-weight-bold">
                                    <v-icon color="white">mdi-truck-fast-outline</v-icon>
                                </div>
                                <div>
                                    <h2 class="text-h5 font-weight-bold mb-1">Thông tin giao hàng dự kiến</h2>
                                </div>
                            </div>
                            <div class="delivery-estimate pa-5">
                                <div class="d-flex align-center">
                                    <div class="delivery-icon mr-4">
                                        <v-icon size="32" color="#1e257c">mdi-truck-fast-outline</v-icon>
                                    </div>
                                    <div>
                                        <p class="text-body-2 font-weight-bold mb-1">Dự kiến giao hàng vào</p>
                                        <p class="text-h6 font-weight-bold mb-1" style="color: #1e257c;">{{ estimatedDelivery }}</p>
                                        <p class="text-caption text-grey-darken-1 mb-0 d-flex align-center">
                                            <v-icon size="14" class="mr-1">mdi-clock-outline</v-icon>
                                            Thời gian giao hàng từ 8:00 - 20:00 tất cả các ngày trong tuần
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </v-col>

                <!-- Right Column: Order Summary -->
                <v-col cols="12" md="5">
                    <div class="order-summary-sticky">
                        <div class="section-block">
                            <div class="pa-6">
                                <div class="d-flex align-center mb-6">
                                    <div class="summary-icon mr-3">
                                        <v-icon size="20" style="color: #ffffff !important;">mdi-receipt-text-outline</v-icon>
                                    </div>
                                    <h3 class="text-h6 font-weight-bold mb-0">Tóm tắt đơn hàng</h3>
                                </div>

                                <!-- Product List -->
                                <div class="product-list mb-6">
                                    <div v-for="item in cartStore.cartItems" :key="item.idChiTietSanPham"
                                        class="product-item d-flex gap-4 py-4">
                                        <div class="product-img-wrapper position-relative flex-shrink-0">
                                            <v-img :src="item.hinhAnh || 'https://via.placeholder.com/150?text=Sản+Phẩm'" cover width="72" height="72"
                                                class="bg-grey-lighten-4"></v-img>
                                            <span class="product-qty-badge">{{ item.soLuong }}</span>
                                            <div v-if="item.phanTramGiam > 0" class="cart-discount-badge">
                                                -{{ item.phanTramGiam }}%
                                            </div>
                                        </div>
                                        <div class="flex-grow-1 d-flex flex-column">
                                            <p class="text-body-2 font-weight-bold mb-1">{{ item.tenSanPham }}</p>
                                            <p class="text-caption text-grey-darken-1 mb-1">
                                                {{ item.tenMauSac }} / {{ item.tenKichThuoc }}
                                            </p>
                                            <div class="mt-auto d-flex flex-column gap-1">
                                                <div v-if="item.phanTramGiam > 0" class="text-caption text-error font-weight-medium mb-1">Đợt giảm giá: -{{ item.phanTramGiam }}%</div>
                                                <div class="d-flex justify-space-between align-center">
                                                    <span class="text-caption text-grey">Đơn giá: {{ formatPrice(item.giaBan) }}</span>
                                                    <span class="text-body-2 font-weight-bold">{{ formatPrice(item.giaBan * item.soLuong) }}</span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <hr class="my-4" style="border: none; border-top: 1px solid #e0e0e0;" />

                                <!-- Voucher -->
                                <div class="voucher-section mb-4">
                                    <div v-if="selectedVoucher"
                                        class="voucher-applied d-flex align-center justify-space-between pa-4 mb-3">
                                        <div class="d-flex align-center gap-3">
                                            <div class="voucher-icon-small">
                                                <v-icon size="20" color="white">mdi-ticket-percent</v-icon>
                                            </div>
                                            <div>
                                                <span class="text-body-2 font-weight-bold d-block" style="color: #1e257c;">{{ selectedVoucher.ten || selectedVoucher.ma || selectedVoucher.maPhieu || selectedVoucher.maPhieuGiamGia || 'Mã Voucher' }}</span>
                                                <span class="text-caption" style="color: #1e257c;">Giảm {{ formatPrice(voucherDiscount) }}</span>
                                            </div>
                                        </div>
                                        <v-btn icon variant="text" size="small" color="grey" @click="removeVoucher">
                                            <v-icon size="18">mdi-close-circle</v-icon>
                                        </v-btn>
                                    </div>
                                    <v-btn v-else variant="outlined" block class="voucher-btn text-none"
                                        color="#1e257c" @click="showVoucherDialog = true">
                                        <span class="font-weight-bold d-flex align-center flex-grow-1">
                                            <v-icon class="mr-2" size="20">mdi-ticket-percent-outline</v-icon>
                                            Chọn hoặc nhập mã giảm giá
                                        </span>
                                        <v-icon size="18">mdi-chevron-right</v-icon>
                                    </v-btn>

                                    <div v-if="bestVoucher && !isBestSelected && bestVoucherDiscount > 0"
                                        class="best-voucher-suggest d-flex align-center justify-space-between pa-3 mt-3"
                                        style="border: 1px dashed #1e257c; background: #f5f7ff;">
                                        <div class="d-flex align-center gap-2">
                                            <v-icon size="18" style="color: #1e257c;">mdi-lightbulb-on-outline</v-icon>
                                            <div>
                                                <span class="text-caption font-weight-bold d-block" style="color: #1e257c;">
                                                    Gợi ý tốt nhất: {{ bestVoucher.ten || bestVoucher.ma }}
                                                </span>
                                                <span class="text-caption text-grey-darken-1">Tiết kiệm {{ formatPrice(bestVoucherDiscount) }}</span>
                                            </div>
                                        </div>
                                        <v-btn size="small" variant="flat" style="background: #1e257c; color: white;" rounded="pill"
                                            class="text-none font-weight-bold" @click="selectVoucher(bestVoucher)">
                                            Áp dụng
                                        </v-btn>
                                    </div>
                                </div>

                                <!-- Price Breakdown -->
                                <div class="price-breakdown pa-4 mb-4">
                                    <div class="d-flex justify-space-between mb-3">
                                        <span class="text-body-2 text-grey-darken-1">Tạm tính ({{ cartStore.cartCount }} sản phẩm)</span>
                                        <span class="text-body-2 font-weight-bold">{{ formatPrice(originalSubtotal) }}</span>
                                    </div>
                                    <div v-if="campaignDiscount > 0" class="d-flex justify-space-between mb-3">
                                        <span class="text-body-2 text-error">Đợt giảm giá</span>
                                        <span class="text-body-2 font-weight-bold text-error">-{{ campaignDiscountPercent }}%</span>
                                    </div>
                                    <div class="d-flex justify-space-between mb-3">
                                        <span class="text-body-2 text-grey-darken-1 d-flex align-center">
                                            Phí vận chuyển
                                            <svg width="45" height="15" viewBox="0 0 45 15" fill="none"
                                                xmlns="http://www.w3.org/2000/svg"
                                                style="display: inline-block; vertical-align: middle; margin-left: 6px;">
                                                <path d="M1 2.5 L7 2.5 L4.5 6.5 L7 6.5 L3.5 10.5 L1 10.5 L3.5 6.5 L1 6.5 Z"
                                                    fill="#0C2A46" />
                                                <path d="M5.5 2.5 L11.5 2.5 L9 6.5 L11.5 6.5 L8 10.5 L5.5 10.5 L8 6.5 L5.5 6.5 Z"
                                                    fill="#FA6400" />
                                                <text x="13.5" y="11" fill="#FA6400" font-family="'Inter', sans-serif"
                                                    font-weight="900" font-style="italic" font-size="10.5"
                                                    letter-spacing="-0.5px">GHN</text>
                                            </svg>
                                        </span>
                                        <span class="text-body-2 font-weight-medium"
                                            :class="{ 'text-blue-darken-4': shippingFee === 0 }">
                                            <v-icon v-if="shippingFee === 0" size="14" color="#1e257c" class="mr-1">mdi-check-circle</v-icon>
                                            {{ shippingFee === 0 ? 'Miễn phí' : formatPrice(shippingFee) }}
                                        </span>
                                    </div>
                                    <div v-if="voucherDiscount > 0" class="d-flex justify-space-between mb-1">
                                        <span class="text-body-2" style="color: #1e257c;">
                                            <v-icon size="14" class="mr-1" style="color: #1e257c;">mdi-ticket-percent</v-icon>
                                            Giảm giá
                                        </span>
                                        <span class="text-body-2 font-weight-bold" style="color: #1e257c;">
                                            -{{ formatPrice(voucherDiscount) }}
                                        </span>
                                    </div>
                                </div>

                                <!-- Total -->
                                <div class="d-flex justify-space-between align-center mb-4 pa-4 total-bar">
                                    <span class="text-body-1 font-weight-bold total-label">Tổng thanh toán</span>
                                    <span class="text-h5 font-weight-bold total-label">{{ formatPrice(totalAmount) }}</span>
                                </div>

                                <!-- Free ship notice -->
                                <p v-if="cartStore.cartTotal < FREE_SHIP_THRESHOLD"
                                    class="text-caption text-center text-grey-darken-1 mb-4 px-2">
                                    <v-icon size="14" class="mr-1 pb-1">mdi-truck-fast-outline</v-icon>
                                    Mua thêm <strong style="color: #1e257c;">{{ formatPrice(remainingForFreeShip) }}</strong> để được
                                    <strong style="color: #1e257c;">Miễn phí vận chuyển</strong>
                                </p>

                                <v-btn style="background: #1e257c; color: white;" rounded="pill" size="x-large" block
                                    class="font-weight-bold text-none place-order-btn"
                                    :loading="loading" :disabled="!isShippingValid"
                                    @click="handleCheckout">
                                    <v-icon class="mr-2">{{ paymentMethod === 'VNPAY' ? 'mdi-qrcode-scan' : 'mdi-lock-outline' }}</v-icon>
                                    {{ paymentMethod === 'VNPAY' ? 'Thanh toán bằng VNPay' : 'Hoàn tất đặt hàng' }}
                                </v-btn>
                            </div>
                        </div>
                    </div>
                </v-col>
            </v-row>
        </v-container>

        <!-- Voucher Dialog -->
        <v-dialog v-model="showVoucherDialog" max-width="520" class="voucher-dialog">
            <div class="modal-content">
                <div class="pa-6" style="background: #1e257c;">
                    <div class="d-flex align-center justify-space-between">
                        <div class="d-flex align-center">
                            <v-icon size="24" class="mr-3" style="color: #ffffff !important;">mdi-ticket-percent</v-icon>
                            <h3 class="text-h6 font-weight-bold mb-0" style="color: #e0f7fa !important;">Chọn mã giảm giá</h3>
                        </div>
                        <v-btn icon variant="text" size="small" @click="showVoucherDialog = false" style="color: #ffffff !important;">
                            <v-icon>mdi-close</v-icon>
                        </v-btn>
                    </div>
                </div>
                <div class="pa-6 bg-white">
                    <div v-if="voucherLoading" class="text-center py-10">
                        <v-progress-circular indeterminate color="#1e257c" size="40"></v-progress-circular>
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
                            class="voucher-item d-flex pa-4 mb-3"
                            :class="{ selected: selectedVoucher?.id === v.id }"
                            @click="selectVoucher(v)">
                            <div class="voucher-badge mr-4 px-2">
                                <span class="voucher-badge-text text-center" style="font-size: 0.75rem; white-space: nowrap;">{{ v.ma || v.maPhieu || v.maPhieuGiamGia || 'Mã' }}</span>
                            </div>
                            <div class="flex-grow-1">
                                <div class="d-flex align-center justify-space-between mb-1">
                                    <h4 class="text-body-2 font-weight-bold">{{ v.ten || v.ma || v.maPhieu || v.maPhieuGiamGia || 'Mã Voucher' }}</h4>
                                    <v-icon v-if="selectedVoucher?.id === v.id" style="color: #1e257c;" size="22">mdi-check-circle</v-icon>
                                    <div v-else class="unselected-radio"></div>
                                </div>
                                <p class="text-caption text-grey-darken-1 mb-1">
                                    <span v-if="v.loaiPhieu === 'PHAN_TRAM'">
                                        Giảm {{ v.phanTramGiamGia }}%
                                        <span v-if="v.giamToiDa" class="font-weight-medium" style="color: #ef4444;">(Tối đa {{ formatPrice(v.giamToiDa) }})</span>
                                    </span>
                                    <span v-else>Giảm {{ formatPrice(v.soTienGiam) }} trực tiếp</span>
                                </p>
                                <p v-if="v.donHangToiThieu" class="text-caption font-weight-bold mb-0" style="color: #1e257c;">
                                    <v-icon size="12" class="mr-1" style="color: #1e257c;">mdi-cart-outline</v-icon>
                                    Đơn tối thiểu {{ formatPrice(v.donHangToiThieu) }}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </v-dialog>

        <!-- VNPay QR Dialog for Online Checkout -->
        <v-dialog v-model="vnpayDialog.show" max-width="450" persistent>
            <v-card class="rounded-xl overflow-hidden pb-4">
                <v-card-text class="pt-6 text-center d-flex flex-column align-center">
                    <div class="vnpay-logo-wrapper mb-4">
                        <v-img src="https://vnpay.vn/assets/images/logo-icon/logo-primary.svg" width="60" />
                    </div>

                    <h3 class="text-h6 font-weight-bold mb-1">Thanh toán VNPay</h3>
                    <p class="text-subtitle-2 text-grey-darken-1 mb-6">Mã đơn: {{ vnpayDialog.orderCode }}</p>

                    <div class="pa-2 bg-white rounded-lg elevation-2 mb-4 d-inline-block">
                        <v-img :src="vnpayDialog.qrUrl" width="220" height="220" />
                    </div>
                    <div class="text-h5 font-weight-bold text-error mb-1">
                        {{ new Intl.NumberFormat('vi-VN', {
                            style: 'currency', currency: 'VND'
                        }).format(vnpayDialog.amount) }}
                    </div>
                    <div class="text-caption text-grey-darken-1 mb-6 px-4 text-center">
                        Sử dụng ứng dụng ngân hàng hoặc ví VNPay để quét mã.
                    </div>

                    <div class="w-100 px-2">
                        <v-btn variant="outlined" color="#005BAA" block class="mb-3 rounded-lg font-weight-bold" height="44"
                            @click="openVnPayGateway">
                            Mở cổng thanh toán VNPay
                        </v-btn>

                        <v-btn block color="#005BAA" class="mb-3 rounded-lg text-white font-weight-bold" height="48"
                            :loading="vnpayDialog.checking" @click="onConfirmPaidOnline">
                            XÁC NHẬN ĐÃ THANH TOÁN
                        </v-btn>

                        <v-btn variant="text" color="grey-darken-1" block class="rounded-lg font-weight-medium" height="40"
                            @click="closeVnPayDialog">
                            Đóng và thanh toán sau
                        </v-btn>
                    </div>
                </v-card-text>
            </v-card>
        </v-dialog>

        <CustomerChat />
    </div>
</template>

<style scoped>
:deep(.v-field) {
    font-size: 0.95rem;
}
</style>
