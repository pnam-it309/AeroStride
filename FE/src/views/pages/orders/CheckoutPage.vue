<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import CartDrawer from '@/components/shared/CartDrawer.vue';
import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import { dichVuDatHang } from '@/services/public/dichVuDatHang';
import { PATH } from '@/router/routePaths';

const router = useRouter();
const cartStore = useCartStore();
const authStore = useAuthStore();

const step = ref(1);
const loading = ref(false);
const voucherLoading = ref(false);

// Form data
const shippingInfo = ref({
    tenNguoiNhan: '',
    soDienThoai: '',
    tinhThanh: '',
    quanHuyen: '',
    phuongXa: '',
    diaChi: '',
});

const paymentMethod = ref('COD');
const ghiChu = ref('');

// Voucher
const availableVouchers = ref([]);
const selectedVoucher = ref(null);
const showVoucherDialog = ref(false);

// Provinces data (simple hardcoded for now, can be replaced with GHN API)
const provinces = ref([
    'Hà Nội', 'TP. Hồ Chí Minh', 'Đà Nẵng', 'Hải Phòng', 'Cần Thơ',
    'An Giang', 'Bà Rịa - Vũng Tàu', 'Bắc Giang', 'Bắc Kạn', 'Bạc Liêu',
    'Bắc Ninh', 'Bến Tre', 'Bình Định', 'Bình Dương', 'Bình Phước',
    'Bình Thuận', 'Cà Mau', 'Cao Bằng', 'Đắk Lắk', 'Đắk Nông',
    'Điện Biên', 'Đồng Nai', 'Đồng Tháp', 'Gia Lai', 'Hà Giang',
    'Hà Nam', 'Hà Tĩnh', 'Hải Dương', 'Hậu Giang', 'Hòa Bình',
    'Hưng Yên', 'Khánh Hòa', 'Kiên Giang', 'Kon Tum', 'Lai Châu',
    'Lâm Đồng', 'Lạng Sơn', 'Lào Cai', 'Long An', 'Nam Định',
    'Nghệ An', 'Ninh Bình', 'Ninh Thuận', 'Phú Thọ', 'Phú Yên',
    'Quảng Bình', 'Quảng Nam', 'Quảng Ngãi', 'Quảng Ninh', 'Quảng Trị',
    'Sóc Trăng', 'Sơn La', 'Tây Ninh', 'Thái Bình', 'Thái Nguyên',
    'Thanh Hóa', 'Thừa Thiên Huế', 'Tiền Giang', 'Trà Vinh', 'Tuyên Quang',
    'Vĩnh Long', 'Vĩnh Phúc', 'Yên Bái'
]);

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
        let discount = cartStore.cartTotal * v.phanTramGiamGia / 100;
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
    return shippingInfo.value.tenNguoiNhan.trim() &&
           shippingInfo.value.soDienThoai.trim() &&
           shippingInfo.value.tinhThanh &&
           shippingInfo.value.diaChi.trim();
});

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
    loading.value = true;
    try {
        const checkoutData = {
            items: cartStore.cartItems.map(item => ({
                idChiTietSanPham: item.idChiTietSanPham,
                soLuong: item.soLuong
            })),
            tenNguoiNhan: shippingInfo.value.tenNguoiNhan,
            soDienThoai: shippingInfo.value.soDienThoai,
            tinhThanh: shippingInfo.value.tinhThanh,
            quanHuyen: shippingInfo.value.quanHuyen || '',
            phuongXa: shippingInfo.value.phuongXa || '',
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

onMounted(() => {
    if (cartStore.isEmpty) {
        router.push(PATH.SHOES);
        return;
    }
    fetchVouchers();
});
</script>

<template>
    <div class="checkout-page bg-grey-lighten-4 min-vh-100">
        <MainHeader />
        <div class="header-spacing" style="height: 104px;"></div>

        <v-container class="py-10" style="max-width: 1200px">
            <!-- Breadcrumb -->
            <div class="d-flex align-center mb-8">
                <router-link to="/" class="breadcrumb-link">Trang chủ</router-link>
                <v-icon size="16" class="mx-2 text-grey">mdi-chevron-right</v-icon>
                <router-link :to="PATH.SHOES" class="breadcrumb-link">Sản phẩm</router-link>
                <v-icon size="16" class="mx-2 text-grey">mdi-chevron-right</v-icon>
                <span class="text-body-2 font-weight-bold">Thanh toán</span>
            </div>

            <v-row>
                <!-- Left Column: Steps -->
                <v-col cols="12" md="7">
                    <!-- Step Indicators -->
                    <div class="step-indicators d-flex align-center mb-8 gap-4">
                        <div class="step-indicator" :class="{ active: step >= 1, completed: step > 1 }" @click="step = 1">
                            <div class="step-circle">
                                <v-icon v-if="step > 1" size="18">mdi-check</v-icon>
                                <span v-else>1</span>
                            </div>
                            <span class="step-label">Giao hàng</span>
                        </div>
                        <div class="step-line" :class="{ active: step > 1 }"></div>
                        <div class="step-indicator" :class="{ active: step >= 2, completed: step > 2 }" @click="step >= 2 ? step = 2 : null">
                            <div class="step-circle">
                                <v-icon v-if="step > 2" size="18">mdi-check</v-icon>
                                <span v-else>2</span>
                            </div>
                            <span class="step-label">Thanh toán</span>
                        </div>
                        <div class="step-line" :class="{ active: step > 2 }"></div>
                        <div class="step-indicator" :class="{ active: step >= 3 }">
                            <div class="step-circle"><span>3</span></div>
                            <span class="step-label">Xác nhận</span>
                        </div>
                    </div>

                    <!-- Step 1: Shipping Info -->
                    <v-card v-if="step === 1" class="checkout-card rounded-xl pa-8" elevation="0">
                        <h2 class="text-h5 font-weight-black mb-6">Thông tin giao hàng</h2>
                        
                        <v-row>
                            <v-col cols="12" sm="6">
                                <v-text-field
                                    v-model="shippingInfo.tenNguoiNhan"
                                    label="Tên người nhận"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details="auto"
                                    :rules="[v => !!v || 'Vui lòng nhập tên']"
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12" sm="6">
                                <v-text-field
                                    v-model="shippingInfo.soDienThoai"
                                    label="Số điện thoại"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details="auto"
                                    :rules="[v => !!v || 'Vui lòng nhập SĐT']"
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12" sm="6">
                                <v-autocomplete
                                    v-model="shippingInfo.tinhThanh"
                                    :items="provinces"
                                    label="Tỉnh/Thành phố"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details="auto"
                                ></v-autocomplete>
                            </v-col>
                            <v-col cols="12" sm="6">
                                <v-text-field
                                    v-model="shippingInfo.quanHuyen"
                                    label="Quận/Huyện"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details="auto"
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12" sm="6">
                                <v-text-field
                                    v-model="shippingInfo.phuongXa"
                                    label="Phường/Xã"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details="auto"
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12">
                                <v-text-field
                                    v-model="shippingInfo.diaChi"
                                    label="Địa chỉ chi tiết (số nhà, tên đường)"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details="auto"
                                    :rules="[v => !!v || 'Vui lòng nhập địa chỉ']"
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12">
                                <v-textarea
                                    v-model="ghiChu"
                                    label="Ghi chú cho đơn hàng"
                                    variant="outlined"
                                    density="comfortable"
                                    rows="2"
                                    hide-details
                                ></v-textarea>
                            </v-col>
                        </v-row>

                        <div class="d-flex justify-end mt-8">
                            <v-btn
                                color="black"
                                rounded="pill"
                                size="large"
                                class="font-weight-black text-none px-10"
                                :disabled="!isShippingValid"
                                @click="step = 2"
                            >
                                Tiếp tục
                                <v-icon class="ml-2">mdi-arrow-right</v-icon>
                            </v-btn>
                        </div>
                    </v-card>

                    <!-- Step 2: Payment Method -->
                    <v-card v-if="step === 2" class="checkout-card rounded-xl pa-8" elevation="0">
                        <h2 class="text-h5 font-weight-black mb-6">Phương thức thanh toán</h2>

                        <div class="payment-options">
                            <div
                                class="payment-option d-flex align-center pa-5 rounded-xl mb-4"
                                :class="{ selected: paymentMethod === 'COD' }"
                                @click="paymentMethod = 'COD'"
                            >
                                <v-radio-group v-model="paymentMethod" hide-details class="ma-0 pa-0" style="flex: 0">
                                    <v-radio value="COD" color="black" hide-details></v-radio>
                                </v-radio-group>
                                <v-icon size="28" class="mx-4" color="green-darken-2">mdi-cash</v-icon>
                                <div>
                                    <h4 class="font-weight-bold">Thanh toán khi nhận hàng (COD)</h4>
                                    <p class="text-caption text-grey">Thanh toán bằng tiền mặt khi shipper giao hàng</p>
                                </div>
                            </div>

                            <div
                                class="payment-option d-flex align-center pa-5 rounded-xl mb-4"
                                :class="{ selected: paymentMethod === 'VNPAY' }"
                                @click="paymentMethod = 'VNPAY'"
                            >
                                <v-radio-group v-model="paymentMethod" hide-details class="ma-0 pa-0" style="flex: 0">
                                    <v-radio value="VNPAY" color="black" hide-details></v-radio>
                                </v-radio-group>
                                <v-icon size="28" class="mx-4" color="blue-darken-2">mdi-credit-card</v-icon>
                                <div>
                                    <h4 class="font-weight-bold">Thanh toán qua VNPay</h4>
                                    <p class="text-caption text-grey">Thẻ ATM, Visa, MasterCard, QR Code</p>
                                </div>
                            </div>
                        </div>

                        <div class="d-flex justify-space-between mt-8">
                            <v-btn variant="text" class="font-weight-bold text-none" @click="step = 1">
                                <v-icon class="mr-2">mdi-arrow-left</v-icon> Quay lại
                            </v-btn>
                            <v-btn
                                color="black"
                                rounded="pill"
                                size="large"
                                class="font-weight-black text-none px-10"
                                @click="step = 3"
                            >
                                Tiếp tục
                                <v-icon class="ml-2">mdi-arrow-right</v-icon>
                            </v-btn>
                        </div>
                    </v-card>

                    <!-- Step 3: Review & Confirm -->
                    <v-card v-if="step === 3" class="checkout-card rounded-xl pa-8" elevation="0">
                        <h2 class="text-h5 font-weight-black mb-6">Xác nhận đơn hàng</h2>

                        <!-- Shipping Summary -->
                        <div class="review-section mb-6 pa-5 rounded-xl">
                            <div class="d-flex justify-space-between align-center mb-3">
                                <h4 class="font-weight-bold d-flex align-center">
                                    <v-icon class="mr-2" size="20" color="green">mdi-map-marker-check</v-icon>
                                    Thông tin giao hàng
                                </h4>
                                <v-btn variant="text" size="small" class="text-none font-weight-bold" color="blue" @click="step = 1">Sửa</v-btn>
                            </div>
                            <p class="text-body-2"><strong>{{ shippingInfo.tenNguoiNhan }}</strong> · {{ shippingInfo.soDienThoai }}</p>
                            <p class="text-body-2 text-grey-darken-1">{{ shippingInfo.diaChi }}, {{ shippingInfo.phuongXa }} {{ shippingInfo.quanHuyen }} {{ shippingInfo.tinhThanh }}</p>
                        </div>

                        <!-- Payment Summary -->
                        <div class="review-section mb-6 pa-5 rounded-xl">
                            <div class="d-flex justify-space-between align-center mb-3">
                                <h4 class="font-weight-bold d-flex align-center">
                                    <v-icon class="mr-2" size="20" color="blue">mdi-credit-card-check</v-icon>
                                    Phương thức thanh toán
                                </h4>
                                <v-btn variant="text" size="small" class="text-none font-weight-bold" color="blue" @click="step = 2">Sửa</v-btn>
                            </div>
                            <p class="text-body-2 font-weight-bold">{{ paymentMethod === 'COD' ? 'Thanh toán khi nhận hàng' : 'VNPay' }}</p>
                        </div>

                        <!-- Products -->
                        <div class="review-section mb-6 pa-5 rounded-xl">
                            <h4 class="font-weight-bold mb-4 d-flex align-center">
                                <v-icon class="mr-2" size="20" color="orange">mdi-package-variant</v-icon>
                                Sản phẩm ({{ cartStore.cartCount }})
                            </h4>
                            <div v-for="item in cartStore.cartItems" :key="item.idChiTietSanPham" class="review-item d-flex align-center gap-4 py-3">
                                <div class="review-item-img">
                                    <v-img v-if="item.hinhAnh" :src="item.hinhAnh" cover width="60" height="60" class="rounded-lg"></v-img>
                                    <div v-else class="img-placeholder rounded-lg">
                                        <v-icon size="24" color="grey-lighten-2">mdi-shoe-sneaker</v-icon>
                                    </div>
                                </div>
                                <div class="flex-grow-1">
                                    <p class="text-body-2 font-weight-bold mb-0">{{ item.tenSanPham }}</p>
                                    <p class="text-caption text-grey">{{ item.tenMauSac }} / {{ item.tenKichThuoc }} · x{{ item.soLuong }}</p>
                                </div>
                                <span class="text-body-2 font-weight-black">{{ formatPrice(item.giaBan * item.soLuong) }}</span>
                            </div>
                        </div>

                        <div class="d-flex justify-space-between mt-8">
                            <v-btn variant="text" class="font-weight-bold text-none" @click="step = 2">
                                <v-icon class="mr-2">mdi-arrow-left</v-icon> Quay lại
                            </v-btn>
                            <v-btn
                                color="black"
                                rounded="pill"
                                size="x-large"
                                class="font-weight-black text-none px-12 place-order-btn"
                                :loading="loading"
                                @click="handleCheckout"
                            >
                                <v-icon class="mr-2">mdi-check-circle</v-icon>
                                Đặt hàng
                            </v-btn>
                        </div>
                    </v-card>
                </v-col>

                <!-- Right Column: Order Summary -->
                <v-col cols="12" md="5">
                    <div class="order-summary-sticky">
                        <v-card class="checkout-card rounded-xl pa-6" elevation="0">
                            <h3 class="text-h6 font-weight-black mb-6">Tóm tắt đơn hàng</h3>

                            <!-- Cart Items (compact) -->
                            <div class="summary-items mb-6">
                                <div v-for="item in cartStore.cartItems" :key="item.idChiTietSanPham" class="summary-item d-flex justify-space-between py-2">
                                    <div class="d-flex align-center gap-2">
                                        <span class="text-body-2 text-grey-darken-1">{{ item.tenSanPham }}</span>
                                        <v-chip size="x-small" variant="tonal">x{{ item.soLuong }}</v-chip>
                                    </div>
                                    <span class="text-body-2 font-weight-bold">{{ formatPrice(item.giaBan * item.soLuong) }}</span>
                                </div>
                            </div>

                            <v-divider class="mb-4" />

                            <!-- Voucher -->
                            <div class="voucher-section mb-4">
                                <div v-if="selectedVoucher" class="voucher-applied d-flex align-center justify-space-between pa-3 rounded-lg">
                                    <div class="d-flex align-center gap-2">
                                        <v-icon size="20" color="green">mdi-ticket-percent</v-icon>
                                        <div>
                                            <span class="text-body-2 font-weight-bold">{{ selectedVoucher.ten || selectedVoucher.ma }}</span>
                                            <p class="text-caption text-green-darken-2 mb-0">-{{ formatPrice(voucherDiscount) }}</p>
                                        </div>
                                    </div>
                                    <v-btn icon variant="text" size="x-small" @click="removeVoucher">
                                        <v-icon size="16">mdi-close</v-icon>
                                    </v-btn>
                                </div>
                                <v-btn
                                    v-else
                                    variant="outlined"
                                    block
                                    class="text-none font-weight-bold voucher-btn"
                                    @click="showVoucherDialog = true"
                                >
                                    <v-icon class="mr-2" size="18">mdi-ticket-percent-outline</v-icon>
                                    Chọn mã giảm giá
                                </v-btn>
                            </div>

                            <v-divider class="mb-4" />

                            <!-- Price Breakdown -->
                            <div class="price-breakdown">
                                <div class="d-flex justify-space-between mb-3">
                                    <span class="text-body-2 text-grey-darken-1">Tạm tính</span>
                                    <span class="text-body-2 font-weight-bold">{{ formatPrice(cartStore.cartTotal) }}</span>
                                </div>
                                <div class="d-flex justify-space-between mb-3">
                                    <span class="text-body-2 text-grey-darken-1">Phí vận chuyển</span>
                                    <span class="text-body-2 font-weight-bold" :class="{ 'text-green': shippingFee === 0 }">
                                        {{ shippingFee === 0 ? 'Miễn phí' : formatPrice(shippingFee) }}
                                    </span>
                                </div>
                                <div v-if="voucherDiscount > 0" class="d-flex justify-space-between mb-3">
                                    <span class="text-body-2 text-green-darken-2">Giảm giá voucher</span>
                                    <span class="text-body-2 font-weight-bold text-green-darken-2">-{{ formatPrice(voucherDiscount) }}</span>
                                </div>
                            </div>

                            <v-divider class="my-4" />

                            <div class="d-flex justify-space-between align-center">
                                <span class="text-body-1 font-weight-black">Tổng cộng</span>
                                <span class="text-h5 font-weight-black">{{ formatPrice(totalAmount) }}</span>
                            </div>

                            <p v-if="cartStore.cartTotal < FREE_SHIP_THRESHOLD" class="text-caption text-grey mt-3">
                                <v-icon size="14" class="mr-1">mdi-truck-outline</v-icon>
                                Mua thêm {{ formatPrice(FREE_SHIP_THRESHOLD - cartStore.cartTotal) }} để được miễn phí vận chuyển
                            </p>
                        </v-card>
                    </div>
                </v-col>
            </v-row>
        </v-container>

        <!-- Voucher Dialog -->
        <v-dialog v-model="showVoucherDialog" max-width="500" class="voucher-dialog">
            <v-card class="rounded-xl">
                <v-card-title class="d-flex align-center justify-space-between pa-6">
                    <h3 class="text-h6 font-weight-black">Chọn mã giảm giá</h3>
                    <v-btn icon variant="text" size="small" @click="showVoucherDialog = false">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </v-card-title>
                <v-divider />
                <v-card-text class="pa-6">
                    <div v-if="voucherLoading" class="text-center py-8">
                        <v-progress-circular indeterminate color="black"></v-progress-circular>
                    </div>
                    <div v-else-if="availableVouchers.length === 0" class="text-center py-8">
                        <v-icon size="48" color="grey-lighten-2" class="mb-4">mdi-ticket-outline</v-icon>
                        <p class="text-grey">Không có mã giảm giá khả dụng</p>
                    </div>
                    <div v-else>
                        <div
                            v-for="v in availableVouchers"
                            :key="v.id"
                            class="voucher-item d-flex align-center pa-4 rounded-xl mb-3"
                            :class="{ 'selected': selectedVoucher?.id === v.id }"
                            @click="selectVoucher(v)"
                        >
                            <div class="voucher-icon mr-4">
                                <v-icon size="28" color="orange-darken-2">mdi-ticket-percent</v-icon>
                            </div>
                            <div class="flex-grow-1">
                                <h4 class="text-body-2 font-weight-black">{{ v.ten || v.ma }}</h4>
                                <p class="text-caption text-grey mb-0">
                                    <span v-if="v.loaiPhieu === 'PHAN_TRAM'">Giảm {{ v.phanTramGiamGia }}%{{ v.giamToiDa ? ` (tối đa ${formatPrice(v.giamToiDa)})` : '' }}</span>
                                    <span v-else>Giảm {{ formatPrice(v.soTienGiam) }}</span>
                                    <span v-if="v.donHangToiThieu"> · Đơn tối thiểu {{ formatPrice(v.donHangToiThieu) }}</span>
                                </p>
                            </div>
                            <v-icon v-if="selectedVoucher?.id === v.id" color="green">mdi-check-circle</v-icon>
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
.checkout-page {
    padding-top: 64px;
}

.breadcrumb-link {
    text-decoration: none;
    font-size: 0.85rem;
    font-weight: 600;
    color: #757575;
    &:hover { color: #111; }
}

/* Step Indicators */
.step-indicators {
    .step-indicator {
        display: flex;
        align-items: center;
        gap: 10px;
        cursor: pointer;
        transition: all 0.3s ease;

        .step-circle {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            background: #e0e0e0;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 900;
            font-size: 0.85rem;
            color: #999;
            transition: all 0.3s ease;
        }

        .step-label {
            font-weight: 700;
            font-size: 0.9rem;
            color: #999;
            transition: color 0.3s ease;
        }

        &.active {
            .step-circle { background: #111; color: #fff; }
            .step-label { color: #111; }
        }

        &.completed {
            .step-circle { background: #4CAF50; color: #fff; }
        }
    }

    .step-line {
        flex: 1;
        height: 2px;
        background: #e0e0e0;
        transition: background 0.3s ease;
        &.active { background: #4CAF50; }
    }
}

/* Cards */
.checkout-card {
    border: 1px solid #f0f0f0;
    background: #fff;
}

/* Payment Options */
.payment-option {
    border: 2px solid #f0f0f0;
    cursor: pointer;
    transition: all 0.3s ease;
    &:hover { border-color: #bbb; }
    &.selected { border-color: #111; background: #fafafa; }
}

/* Review Section */
.review-section {
    background: #f9f9f9;
    border: 1px solid #f0f0f0;
}

.review-item + .review-item {
    border-top: 1px solid #f0f0f0;
}

.img-placeholder {
    width: 60px;
    height: 60px;
    background: #f0f0f0;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* Order Summary */
.order-summary-sticky {
    position: sticky;
    top: 130px;
}

.summary-items {
    max-height: 200px;
    overflow-y: auto;
    scrollbar-width: thin;
}

/* Voucher */
.voucher-btn {
    border-color: #e0e0e0;
    border-style: dashed;
    &:hover { border-color: #111; }
}

.voucher-applied {
    background: #E8F5E9;
    border: 1px solid #C8E6C9;
}

.voucher-item {
    border: 1px solid #f0f0f0;
    cursor: pointer;
    transition: all 0.2s ease;
    &:hover { border-color: #bbb; background: #fafafa; }
    &.selected { border-color: #4CAF50; background: #E8F5E9; }
}

.voucher-icon {
    width: 48px;
    height: 48px;
    background: #FFF3E0;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* Place Order Button */
.place-order-btn {
    height: 56px !important;
    font-size: 1rem;
    letter-spacing: 0.5px;
}

.min-vh-100 { min-height: 100vh; }
</style>
