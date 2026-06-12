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

// Form data
const shippingInfo = ref({
    tenNguoiNhan: '',
    soDienThoai: '',
    tinhThanh: null,
    quanHuyen: null,
    phuongXa: null,
    diaChi: ''
});

const paymentMethod = ref('COD');
const ghiChu = ref('');

// Voucher
const availableVouchers = ref([]);
const selectedVoucher = ref(null);
const showVoucherDialog = ref(false);

// Watchers for location
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

// Validation
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
                            if (w) {
                                shippingInfo.value.phuongXa = w.code;
                            }
                        }
                    }
                    // Timeout to reset init flag so manual changes clear the sub-fields
                    setTimeout(() => { shippingInfo.value.isInitializing = false; }, 500);
                }
            }
        } catch (e) {
            console.error('Không thể lấy thông tin người dùng', e);
        }
    }
};

// Fetch vouchers
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

// Checkout
const handleCheckout = async () => {
    if (!isShippingValid.value) {
        alert("Vui lòng điền đầy đủ thông tin giao hàng!");
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
                soLuong: item.soLuong
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
            router.push(`/order-success/${response.data.id}`);
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
    fetchProvinces();
    await fetchUserProfile();
    fetchVouchers();
});
</script>

<template>
    <div class="checkout-page bg-grey-lighten-4 min-vh-100 pb-12">
        <MainHeader />
        <div class="header-spacing" style="height: 104px"></div>

        <v-container style="max-width: 1200px">
            <!-- Breadcrumb -->
            <div class="d-flex align-center mb-8">
                <router-link to="/" class="breadcrumb-link">Trang chủ</router-link>
                <v-icon size="16" class="mx-2 text-grey">mdi-chevron-right</v-icon>
                <router-link :to="PATH.SHOES" class="breadcrumb-link">Sản phẩm</router-link>
                <v-icon size="16" class="mx-2 text-grey">mdi-chevron-right</v-icon>
                <span class="text-body-2 font-weight-bold">Thanh toán</span>
            </div>

            <v-row>
                <!-- Left Column: Shipping & Payment -->
                <v-col cols="12" md="7">
                    <!-- Shipping Info -->
                    <v-card class="checkout-card rounded-xl pa-8 mb-6" elevation="0">
                        <div class="d-flex align-center mb-6">
                            <v-icon size="28" color="blue-darken-4" class="mr-3">mdi-map-marker-account-outline</v-icon>
                            <h2 class="text-h5 font-weight-black text-blue-darken-4">Thông tin giao hàng</h2>
                        </div>

                        <v-row>
                            <v-col cols="12" sm="6">
                                <v-text-field v-model="shippingInfo.tenNguoiNhan" label="Tên người nhận"
                                    variant="outlined" density="comfortable" hide-details="auto"
                                    :rules="[(v) => !!v || 'Vui lòng nhập tên']"></v-text-field>
                            </v-col>
                            <v-col cols="12" sm="6">
                                <v-text-field v-model="shippingInfo.soDienThoai" label="Số điện thoại"
                                    variant="outlined" density="comfortable" hide-details="auto"
                                    :rules="[(v) => !!v || 'Vui lòng nhập SĐT']"></v-text-field>
                            </v-col>
                            <v-col cols="12" sm="6">
                                <v-select v-model="shippingInfo.tinhThanh" :items="provinces" item-title="name" item-value="code" label="Tỉnh/Thành phố"
                                    variant="outlined" density="comfortable" hide-details="auto"
                                    :loading="loadingLocations.provinces"
                                    :rules="[(v) => !!v || 'Vui lòng chọn Tỉnh/Thành']"></v-select>
                            </v-col>
                            <v-col cols="12" sm="6">
                                <v-select v-model="shippingInfo.quanHuyen" :items="districts" item-title="name" item-value="code" label="Quận/Huyện" variant="outlined"
                                    density="comfortable" hide-details="auto"
                                    :loading="loadingLocations.districts"
                                    :disabled="!shippingInfo.tinhThanh"
                                    :rules="[(v) => !!v || 'Vui lòng chọn Quận/Huyện']"></v-select>
                            </v-col>
                            <v-col cols="12" sm="6">
                                <v-select v-model="shippingInfo.phuongXa" :items="wards" item-title="name" item-value="code" label="Phường/Xã" variant="outlined"
                                    density="comfortable" hide-details="auto"
                                    :loading="loadingLocations.wards"
                                    :disabled="!shippingInfo.quanHuyen"
                                    :rules="[(v) => !!v || 'Vui lòng chọn Phường/Xã']"></v-select>
                            </v-col>
                            <v-col cols="12">
                                <v-text-field v-model="shippingInfo.diaChi" label="Địa chỉ chi tiết (số nhà, tên đường)"
                                    variant="outlined" density="comfortable" hide-details="auto"
                                    :rules="[(v) => !!v || 'Vui lòng nhập địa chỉ']"></v-text-field>
                            </v-col>
                            <v-col cols="12">
                                <v-textarea v-model="ghiChu" label="Ghi chú cho đơn hàng" variant="outlined"
                                    density="comfortable" rows="3" hide-details></v-textarea>
                            </v-col>
                        </v-row>
                    </v-card>

                    <!-- Payment Method -->
                    <v-card class="checkout-card rounded-xl pa-8" elevation="0">
                        <div class="d-flex align-center mb-6">
                            <v-icon size="28" color="blue-darken-4" class="mr-3">mdi-credit-card-outline</v-icon>
                            <h2 class="text-h5 font-weight-black text-blue-darken-4">Phương thức thanh toán</h2>
                        </div>

                        <div class="payment-options">
                            <div class="payment-option d-flex align-center pa-5 rounded-xl mb-4"
                                :class="{ selected: paymentMethod === 'COD' }" @click="paymentMethod = 'COD'">
                                <v-icon size="28" :color="paymentMethod === 'COD' ? 'blue-darken-4' : 'grey-lighten-1'" class="mr-2 flex-shrink-0">
                                    {{ paymentMethod === 'COD' ? 'mdi-radiobox-marked' : 'mdi-radiobox-blank' }}
                                </v-icon>
                                <v-icon size="32" class="mx-4 flex-shrink-0" color="blue-darken-3">mdi-cash</v-icon>
                                <div>
                                    <h4 class="font-weight-bold text-body-1" :class="{'text-blue-darken-4': paymentMethod === 'COD'}">Thanh toán khi nhận hàng (COD)</h4>
                                    <p class="text-caption text-grey mb-0">Thanh toán bằng tiền mặt khi shipper giao hàng tận nơi</p>
                                </div>
                            </div>

                            <div class="payment-option d-flex align-center pa-5 rounded-xl mb-4"
                                :class="{ selected: paymentMethod === 'VNPAY' }" @click="paymentMethod = 'VNPAY'">
                                <v-icon size="28" :color="paymentMethod === 'VNPAY' ? 'blue-darken-4' : 'grey-lighten-1'" class="mr-2 flex-shrink-0">
                                    {{ paymentMethod === 'VNPAY' ? 'mdi-radiobox-marked' : 'mdi-radiobox-blank' }}
                                </v-icon>
                                <div class="mx-4 vnpay-logo flex-shrink-0">
                                    <span class="font-weight-black text-blue-darken-3 text-h6">VN<span class="text-red">PAY</span></span>
                                </div>
                                <div>
                                    <h4 class="font-weight-bold text-body-1" :class="{'text-blue-darken-4': paymentMethod === 'VNPAY'}">Thanh toán trực tuyến VNPay</h4>
                                    <p class="text-caption text-grey mb-0">Hỗ trợ Thẻ ATM, Visa, MasterCard, và Quét mã QR</p>
                                </div>
                            </div>
                        </div>
                    </v-card>
                </v-col>

                <!-- Right Column: Order Summary -->
                <v-col cols="12" md="5">
                    <div class="order-summary-sticky">
                        <v-card class="checkout-card rounded-xl pa-6" elevation="0">
                            <h3 class="text-h6 font-weight-black text-blue-darken-4 mb-6 border-bottom pb-4">Tóm tắt đơn hàng</h3>

                            <!-- Product List (Detailed) -->
                            <div class="product-list mb-6">
                                <div v-for="item in cartStore.cartItems" :key="item.idChiTietSanPham"
                                    class="product-item d-flex gap-4 py-4 border-bottom">
                                    <!-- Image -->
                                    <div class="product-img-wrapper position-relative flex-shrink-0">
                                        <v-img :src="item.hinhAnh || 'https://via.placeholder.com/150?text=Sản+Phẩm'" cover width="80" height="80"
                                            class="rounded-lg bg-grey-lighten-4"></v-img>
                                        <span class="product-qty-badge">{{ item.soLuong }}</span>
                                    </div>
                                    <!-- Info -->
                                    <div class="flex-grow-1 d-flex flex-column justify-center">
                                        <p class="text-body-1 font-weight-bold mb-1">{{ item.tenSanPham }}</p>
                                        <p class="text-caption text-grey-darken-1 mb-2">
                                            Màu: {{ item.tenMauSac }} &bull; Size: {{ item.tenKichThuoc }}
                                        </p>
                                        <p class="text-body-1 font-weight-black text-right">{{ formatPrice(item.giaBan *
                                            item.soLuong) }}</p>
                                    </div>
                                </div>
                            </div>

                            <!-- Voucher -->
                            <div class="voucher-section mb-6">
                                <h4 class="text-body-2 font-weight-bold mb-3 text-blue-darken-4">Mã giảm giá</h4>
                                <div v-if="selectedVoucher"
                                    class="voucher-applied d-flex align-center justify-space-between pa-4 rounded-lg">
                                    <div class="d-flex align-center gap-3">
                                        <div class="voucher-icon-small">
                                            <v-icon size="24" color="white">mdi-ticket-percent</v-icon>
                                        </div>
                                        <div>
                                            <span class="text-body-1 font-weight-bold text-blue-darken-4 d-block">{{ selectedVoucher.ten ||
                                                selectedVoucher.ma }}</span>
                                            <span class="text-caption text-blue-darken-2 font-weight-medium">Đã giảm {{
                                                formatPrice(voucherDiscount) }}</span>
                                        </div>
                                    </div>
                                    <v-btn icon variant="text" size="small" color="red" @click="removeVoucher">
                                        <v-icon size="20">mdi-close-circle</v-icon>
                                    </v-btn>
                                </div>
                                <v-btn v-else variant="outlined" block class="voucher-btn text-none rounded-lg"
                                    color="blue-darken-4" @click="showVoucherDialog = true">
                                    <span class="font-weight-bold d-flex align-center flex-grow-1 text-blue-darken-4">
                                        <v-icon class="mr-2" size="22"
                                            color="blue-darken-4">mdi-ticket-percent-outline</v-icon>
                                        Chọn hoặc nhập mã giảm giá
                                    </span>
                                    <v-icon size="20" color="blue-darken-4">mdi-chevron-right</v-icon>
                                </v-btn>
                            </div>

                            <!-- Price Breakdown -->
                            <div class="price-breakdown bg-grey-lighten-4 pa-5 rounded-lg mb-6">
                                <div class="d-flex justify-space-between mb-3">
                                    <span class="text-body-1 text-grey-darken-2">Tạm tính ({{ cartStore.cartCount }} sản
                                        phẩm)</span>
                                    <span class="text-body-1 font-weight-bold">{{ formatPrice(cartStore.cartTotal)
                                    }}</span>
                                </div>
                                <div class="d-flex justify-space-between mb-3">
                                    <span class="text-body-1 text-grey-darken-2">Phí vận chuyển</span>
                                    <span class="text-body-1 font-weight-bold"
                                        :class="{ 'text-blue-darken-3': shippingFee === 0 }">
                                        {{ shippingFee === 0 ? 'Miễn phí' : formatPrice(shippingFee) }}
                                    </span>
                                </div>
                                <div v-if="voucherDiscount > 0" class="d-flex justify-space-between mb-1">
                                    <span class="text-body-1 text-blue-darken-3">Mã giảm giá áp dụng</span>
                                    <span class="text-body-1 font-weight-bold text-blue-darken-3">
                                        -{{ formatPrice(voucherDiscount) }}
                                    </span>
                                </div>
                            </div>

                            <div class="d-flex justify-space-between align-center mb-6">
                                <span class="text-h6 font-weight-bold text-blue-darken-4">Tổng thanh toán</span>
                                <span class="text-h4 font-weight-black text-blue-darken-4">{{ formatPrice(totalAmount)
                                }}</span>
                            </div>

                            <p v-if="cartStore.cartTotal < FREE_SHIP_THRESHOLD"
                                class="text-caption text-center text-grey-darken-1 mb-4">
                                <v-icon size="16" class="mr-1 pb-1">mdi-truck-fast-outline</v-icon>
                                Mua thêm <strong class="text-black">{{ formatPrice(FREE_SHIP_THRESHOLD -
                                    cartStore.cartTotal) }}</strong> để được <strong>Miễn phí vận chuyển</strong>
                            </p>

                            <v-btn color="blue-darken-4" rounded="pill" size="x-large" block
                                class="font-weight-black text-none place-order-btn elevation-2 text-white" :loading="loading"
                                @click="handleCheckout">
                                <span v-if="paymentMethod === 'VNPAY'">Thanh toán bằng VNPay</span>
                                <span v-else>Hoàn tất đặt hàng</span>
                                <v-icon class="ml-2">{{ paymentMethod === 'VNPAY' ? 'mdi-qrcode-scan' : 'mdi-arrow-right-circle' }}</v-icon>
                            </v-btn>
                        </v-card>
                    </div>
                </v-col>
            </v-row>
        </v-container>

        <!-- Voucher Dialog -->
        <v-dialog v-model="showVoucherDialog" max-width="500" class="voucher-dialog">
            <v-card class="rounded-xl">
                <v-card-title class="d-flex align-center justify-space-between pa-6 bg-blue-darken-4 text-white">
                    <h3 class="text-h6 font-weight-black d-flex align-center">
                        <v-icon color="white" class="mr-2">mdi-ticket-percent</v-icon>
                        Mã giảm giá
                    </h3>
                    <v-btn icon variant="text" size="small" color="white" @click="showVoucherDialog = false">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </v-card-title>
                <v-card-text class="pa-6">
                    <div v-if="voucherLoading" class="text-center py-8">
                        <v-progress-circular indeterminate color="black"></v-progress-circular>
                    </div>
                    <div v-else-if="availableVouchers.length === 0" class="text-center py-8">
                        <v-icon size="64" color="grey-lighten-2" class="mb-4">mdi-ticket-outline</v-icon>
                        <p class="text-body-1 text-grey-darken-1 font-weight-medium">Bạn chưa có mã giảm giá nào phù hợp
                            với đơn hàng này.</p>
                    </div>
                    <div v-else>
                        <div v-for="v in availableVouchers" :key="v.id"
                            class="voucher-item d-flex align-center pa-4 rounded-xl mb-4 elevation-1"
                            :class="{ selected: selectedVoucher?.id === v.id }" @click="selectVoucher(v)">
                            <div class="voucher-icon mr-4">
                                <span class="font-weight-black text-white text-h6">{{ v.loaiPhieu === 'PHAN_TRAM' ?
                                    v.phanTramGiamGia + '%' : 'SALE' }}</span>
                            </div>
                            <div class="flex-grow-1">
                                <h4 class="text-body-1 font-weight-black text-black">{{ v.ten || v.ma }}</h4>
                                <p class="text-caption text-grey-darken-1 mb-1 mt-1">
                                    <span v-if="v.loaiPhieu === 'PHAN_TRAM'">Giảm {{ v.phanTramGiamGia }}%{{ v.giamToiDa
                                        ? ` (tối đa ${formatPrice(v.giamToiDa)})` : '' }}</span>
                                    <span v-else>Giảm {{ formatPrice(v.soTienGiam) }} thẳng vào đơn hàng</span>
                                </p>
                                <p v-if="v.donHangToiThieu"
                                    class="text-caption text-orange-darken-3 font-weight-bold mb-0">Đơn tối thiểu {{
                                        formatPrice(v.donHangToiThieu) }}</p>
                            </div>
                            <v-icon v-if="selectedVoucher?.id === v.id" color="blue-darken-4"
                                size="28">mdi-check-circle</v-icon>
                            <v-icon v-else color="grey-lighten-1" size="28">mdi-circle-outline</v-icon>
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
    font-size: 0.9rem;
    font-weight: 600;
    color: #757575;

    &:hover {
        color: #111;
    }
}

/* Cards */
.checkout-card {
    border: 1px solid #e0e0e0;
    background: #fff;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03) !important;
}

/* Payment Options */
.payment-options {
    .payment-option {
        border: 2px solid #eee;
        cursor: pointer;
        transition: all 0.3s ease;

        &:hover {
            border-color: #ccc;
            background: #fafafa;
        }

        &.selected {
            border-color: #111;
            background: #fafafa;
        }

        .vnpay-logo {
            width: 40px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
    }
}

/* Product List */
.product-list {
    .product-item {
        &:last-child {
            border-bottom: none !important;
        }
    }

    .product-img-wrapper {
        width: 80px;
        height: 80px;

        .product-qty-badge {
            position: absolute;
            top: -8px;
            right: -8px;
            background: rgba(0, 0, 0, 0.8);
            color: white;
            font-size: 0.75rem;
            font-weight: 700;
            width: 22px;
            height: 22px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 2;
        }
    }
}

.img-placeholder {
    width: 80px;
    height: 80px;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* Order Summary */
.order-summary-sticky {
    position: sticky;
    top: 130px;
}

/* Voucher */
.voucher-btn {
    border-color: #0d47a1;
    border-style: dashed;
    border-width: 2px;
    height: 56px !important;
    background: #e3f2fd;

    &:hover {
        background: #bbdefb;
    }
}

.voucher-applied {
    background: #e3f2fd;
    border: 1px solid #90caf9;

    .voucher-icon-small {
        width: 40px;
        height: 40px;
        background: #0d47a1;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
    }
}

.voucher-item {
    border: 2px solid transparent;
    cursor: pointer;
    transition: all 0.2s ease;
    background: #fff;

    &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(13, 71, 161, 0.1) !important;
    }

    &.selected {
        border-color: #0d47a1;
        background: #f3f8ff;
    }

    .voucher-icon {
        width: 60px;
        height: 60px;
        background: linear-gradient(135deg, #1976d2, #0d47a1);
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 10px rgba(13, 71, 161, 0.3);
    }
}

.border-bottom {
    border-bottom: 1px solid #eee;
}

/* Place Order Button */
.place-order-btn {
    height: 64px !important;
    font-size: 1.1rem;
    letter-spacing: 0.5px;
    transition: transform 0.2s;

    &:hover {
        transform: scale(1.02);
    }
}
</style>
