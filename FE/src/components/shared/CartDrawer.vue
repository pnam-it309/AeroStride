<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import { dichVuFile } from '@/services/core/dichVuFile';
import { PATH } from '@/router/routePaths';

const router = useRouter();
const cartStore = useCartStore();
const authStore = useAuthStore();

onMounted(() => {
    cartStore.syncWithBackend();
});

const resolveImg = (v) => {
    if (!v) return '';
    if (/^(https?:)?\/\//i.test(v) || v?.startsWith('data:') || v?.startsWith('blob:')) return v;
    return dichVuFile.layUrlFile(v.replace(/^\/+/, ''));
};

const FREE_SHIP_THRESHOLD = 5000000;
const SHIPPING_FEE = 30000;

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const shippingFee = computed(() => {
    return cartStore.cartTotal >= FREE_SHIP_THRESHOLD ? 0 : null;
});

const shippingProgress = computed(() => {
    if (cartStore.cartTotal >= FREE_SHIP_THRESHOLD) return 100;
    return Math.min((cartStore.cartTotal / FREE_SHIP_THRESHOLD) * 100, 99);
});

const remainingForFreeShip = computed(() => {
    return Math.max(0, FREE_SHIP_THRESHOLD - cartStore.cartTotal);
});

const stockAlertModal = ref({
    show: false,
    title: '',
    message: ''
});

const showStockAlert = (title, message) => {
    stockAlertModal.value = {
        show: true,
        title,
        message
    };
};

const handleQuantityChange = async (id, delta) => {
    const item = cartStore.items.find((i) => i.idChiTietSanPham === id);
    if (item) {
        const newQty = item.soLuong + delta;
        if (delta > 0 && item.soLuongTonKho && newQty > item.soLuongTonKho) {
            showStockAlert(
                'Vượt quá số lượng tồn kho',
                `Sản phẩm "${item.tenSanPham || 'giày'}" hiện chỉ còn tối đa ${item.soLuongTonKho} sản phẩm trong kho.`
            );
            return;
        }
        const res = await cartStore.updateQuantity(id, newQty);
        if (res && !res.success) {
            showStockAlert('Không thể cập nhật số lượng', res.message || 'Số lượng vượt quá tồn kho hiện có.');
        }
    }
};

const handleQuantityInput = async (item, eventTargetValue) => {
    let num = parseInt(eventTargetValue, 10);
    if (isNaN(num) || num <= 0) {
        await cartStore.updateQuantity(item.idChiTietSanPham, 1);
        return;
    }
    if (item.soLuongTonKho && num > item.soLuongTonKho) {
        showStockAlert(
            'Vượt quá số lượng tồn kho',
            `Sản phẩm "${item.tenSanPham || 'giày'}" hiện chỉ còn tối đa ${item.soLuongTonKho} sản phẩm trong kho.`
        );
        await cartStore.updateQuantity(item.idChiTietSanPham, item.soLuongTonKho);
        return;
    }
    const res = await cartStore.updateQuantity(item.idChiTietSanPham, num);
    if (res && !res.success) {
        showStockAlert('Không thể cập nhật số lượng', res.message || 'Số lượng vượt quá tồn kho hiện có.');
    }
};

const handleCheckout = () => {
    cartStore.closeDrawer();
    router.push(PATH.CHECKOUT);
};
</script>

<template>
    <v-navigation-drawer v-model="cartStore.isDrawerOpen" temporary location="right" width="440" class="cart-drawer">
        <div class="d-flex flex-column h-100">
            <!-- Header -->
            <div class="cart-header d-flex align-center justify-space-between px-6 py-4">
                <h3 class="text-h6 font-weight-black d-flex align-center">
                    <v-icon class="mr-2" size="22" color="black">mdi-bag-outline</v-icon>
                    Giỏ hàng
                    <span class="cart-badge-count ml-2">{{ cartStore.cartCount }}</span>
                </h3>
                <v-btn icon variant="text" size="small" @click="cartStore.closeDrawer()" class="close-btn">
                    <v-icon>mdi-close</v-icon>
                </v-btn>
            </div>

            <v-divider />

            <!-- Free Shipping Progress -->
            <div v-if="authStore.isLoggedIn && !cartStore.isEmpty && cartStore.cartTotal < FREE_SHIP_THRESHOLD" class="shipping-progress px-6 py-4">
                <div class="d-flex align-center mb-2">
                    <v-icon size="16" color="orange-darken-2" class="mr-2">mdi-truck-fast-outline</v-icon>
                    <span class="text-caption font-weight-medium text-grey-darken-1">
                        Mua thêm <strong class="text-orange-darken-3">{{ formatPrice(remainingForFreeShip) }}</strong> để được
                        <strong class="text-orange-darken-3">miễn phí vận chuyển</strong>
                    </span>
                </div>
                <v-progress-linear :model-value="shippingProgress" height="6" color="orange" rounded class="shipping-bar"></v-progress-linear>
            </div>
            <div v-else-if="authStore.isLoggedIn && !cartStore.isEmpty" class="shipping-progress-free px-6 py-3">
                <div class="d-flex align-center">
                    <v-icon size="16" color="green" class="mr-2">mdi-check-circle</v-icon>
                    <span class="text-caption font-weight-bold text-green">Bạn đã được miễn phí vận chuyển!</span>
                </div>
            </div>

            <!-- Cart Items -->
            <div class="cart-items-container flex-grow-1" v-if="!cartStore.isEmpty">
                <!-- Loading skeleton when syncing with backend -->
                <div v-if="cartStore.isSyncing && !cartStore.cartItems[0]?.tenSanPham" class="px-4 py-2">
                    <div v-for="n in cartStore.cartCount" :key="n" class="cart-item px-4 py-4 mb-1">
                        <div class="d-flex gap-4">
                            <v-skeleton-loader type="image" width="90" height="90" class="rounded-xl"></v-skeleton-loader>
                            <div class="flex-grow-1">
                                <v-skeleton-loader type="text" class="mb-2"></v-skeleton-loader>
                                <v-skeleton-loader type="text" width="60%" class="mb-2"></v-skeleton-loader>
                                <v-skeleton-loader type="text" width="40%"></v-skeleton-loader>
                            </div>
                        </div>
                    </div>
                </div>
                <TransitionGroup v-else name="cart-item" tag="div">
                    <div v-for="item in cartStore.cartItems" :key="item.idChiTietSanPham" class="cart-item px-6 py-4">
                        <div class="d-flex gap-4">
                            <!-- Product Image -->
                            <div class="cart-item-image position-relative">
                                <v-img v-if="item.hinhAnh" :src="resolveImg(item.hinhAnh)" cover class="rounded-xl" width="90" height="90" lazy-src="https://via.placeholder.com/90?text=..."></v-img>
                                <div v-else class="image-placeholder rounded-xl">
                                    <v-icon size="36" color="grey-lighten-2">mdi-shoe-sneaker</v-icon>
                                </div>
                                <span class="qty-circle">{{ item.soLuong }}</span>
                            </div>

                            <!-- Product Info -->
                            <div class="flex-grow-1 d-flex flex-column">
                                <h4 class="cart-item-name text-body-1 font-weight-bold mb-1">{{ item.tenSanPham }}</h4>
                                <p class="text-caption text-grey-darken-1 mb-2 d-flex align-center">
                                    <v-icon size="12" class="mr-1">mdi-palette-outline</v-icon>{{ item.tenMauSac }}
                                    <span class="mx-1">•</span>
                                    <v-icon size="12" class="mr-1">mdi-ruler</v-icon>{{ item.tenKichThuoc }}
                                </p>
                                <div class="mt-auto">
                                    <p class="cart-item-price text-body-1 font-weight-bold mb-2">{{ formatPrice(item.giaBan) }}</p>

                                    <!-- Quantity Controls -->
                                    <div class="d-flex align-center justify-space-between">
                                        <div class="quantity-controls d-flex align-center">
                                            <v-btn
                                                icon
                                                variant="outlined"
                                                size="x-small"
                                                :disabled="item.soLuong <= 1"
                                                @click="handleQuantityChange(item.idChiTietSanPham, -1)"
                                                class="qty-btn"
                                            >
                                                <v-icon size="14">mdi-minus</v-icon>
                                            </v-btn>
                                            <input
                                                type="number"
                                                class="quantity-input text-center font-weight-bold text-body-2 mx-2"
                                                :value="item.soLuong"
                                                @change="handleQuantityInput(item, $event.target.value)"
                                                @blur="handleQuantityInput(item, $event.target.value)"
                                            />
                                            <v-btn
                                                icon
                                                variant="outlined"
                                                size="x-small"
                                                @click="handleQuantityChange(item.idChiTietSanPham, 1)"
                                                class="qty-btn"
                                            >
                                                <v-icon size="14">mdi-plus</v-icon>
                                            </v-btn>
                                        </div>
                                        <div class="d-flex align-center gap-2">
                                            <span class="text-body-2 font-weight-black item-total-price">{{ formatPrice(item.giaBan * item.soLuong) }}</span>
                                            <v-btn
                                                icon
                                                variant="text"
                                                size="x-small"
                                                color="grey-lighten-1"
                                                @click="cartStore.removeFromCart(item.idChiTietSanPham)"
                                                class="delete-btn"
                                            >
                                                <v-icon size="18">mdi-trash-can-outline</v-icon>
                                            </v-btn>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </TransitionGroup>
            </div>

            <!-- Empty State -->
            <div v-else class="empty-cart d-flex flex-column align-center justify-center flex-grow-1 px-6">
                <div class="empty-icon-wrap mb-6">
                    <v-icon size="72" color="grey-lighten-3">mdi-bag-outline</v-icon>
                </div>
                <h4 class="text-h6 font-weight-bold text-grey-darken-1 mb-2">Giỏ hàng trống</h4>
                <p class="text-body-2 text-grey text-center mb-6">Hãy khám phá bộ sưu tập giày mới nhất của chúng tôi!</p>
                <v-btn
                    color="black"
                    rounded="pill"
                    size="large"
                    class="font-weight-bold text-none px-8"
                    @click="cartStore.closeDrawer(); router.push(PATH.SHOES)"
                >
                    <v-icon class="mr-2" size="20">mdi-shopping-outline</v-icon>
                    Khám phá sản phẩm
                </v-btn>
            </div>

            <!-- Footer -->
            <template v-if="!cartStore.isEmpty">
                <div class="cart-footer pa-6">
                    <div class="price-summary mb-4">
                        <div class="d-flex justify-space-between align-center mb-2">
                            <span class="text-body-2 text-grey-darken-1">Tạm tính</span>
                            <span class="text-body-1 font-weight-bold total-summary-price">{{ formatPrice(cartStore.cartTotal) }}</span>
                        </div>
                        <div class="d-flex justify-space-between align-center mb-2" v-if="authStore.isLoggedIn && shippingFee === 0">
                            <span class="text-body-2 text-grey-darken-1">Phí vận chuyển</span>
                            <span class="text-body-2 font-weight-medium text-green">Miễn phí</span>
                        </div>
                        <v-divider class="my-2" />
                        <div class="d-flex justify-space-between align-center">
                            <span class="text-body-1 font-weight-bold text-black">Tổng cộng</span>
                            <span class="text-h6 font-weight-black total-summary-price">{{ formatPrice(cartStore.cartTotal + (shippingFee || 0)) }}</span>
                        </div>
                    </div>

                    <v-btn
                        block
                        size="large"
                        variant="flat"
                        rounded="pill"
                        class="font-weight-bold text-none checkout-btn-custom mb-3"
                        @click="handleCheckout"
                    >
                        <v-icon class="mr-2" size="20">mdi-lock-outline</v-icon>
                        Thanh toán ngay · {{ formatPrice(cartStore.cartTotal + (shippingFee || 0)) }}
                    </v-btn>
                    <v-btn
                        block
                        size="large"
                        variant="flat"
                        rounded="pill"
                        class="font-weight-bold text-none continue-btn-custom"
                        @click="cartStore.closeDrawer(); router.push(PATH.SHOES)"
                    >
                        Tiếp tục mua sắm
                    </v-btn>
                </div>
            </template>
        </div>

        <!-- Stock Alert Modal -->
        <v-dialog v-model="stockAlertModal.show" max-width="450">
            <v-card class="rounded-2xl pa-4 text-center">
                <div class="d-flex justify-center mt-2 mb-3">
                    <v-avatar color="amber-lighten-4" size="64">
                        <v-icon color="amber-darken-3" size="36">mdi-alert-circle-outline</v-icon>
                    </v-avatar>
                </div>
                <v-card-title class="text-h6 font-weight-black pt-0 pb-2">{{ stockAlertModal.title }}</v-card-title>
                <v-card-text class="text-body-2 text-grey-darken-2 px-4 pb-4">
                    {{ stockAlertModal.message }}
                </v-card-text>
                <v-card-actions class="justify-center pb-2">
                    <v-btn color="black" variant="flat" rounded="pill" class="px-8 font-weight-bold text-none"
                        @click="stockAlertModal.show = false">
                        Đã hiểu
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-navigation-drawer>
</template>

<style scoped lang="scss">
.cart-drawer {
    :deep(.v-navigation-drawer__content) {
        display: flex;
        flex-direction: column;
    }
}

.cart-badge-count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: #ff1744;
    color: #ffffff;
    font-size: 0.75rem;
    font-weight: 800;
    min-width: 20px;
    height: 20px;
    border-radius: 10px;
    padding: 0 6px;
    line-height: 1;
}

.cart-header {
    background: #fff;
    border-bottom: 1px solid #f5f5f5;

    .close-btn {
        &:hover {
            background: #f5f5f5;
        }
    }
}

.shipping-progress {
    background: linear-gradient(135deg, #fff8e1, #fff3e0);
    border-bottom: 1px solid #ffe0b2;

    .shipping-bar {
        :deep(.v-progress-linear__background) {
            background: #ffe0b2 !important;
        }
    }
}

.shipping-progress-free {
    background: linear-gradient(135deg, #e8f5e9, #f1f8e9);
    border-bottom: 1px solid #c8e6c9;
}

.cart-items-container {
    overflow-y: auto;
    scrollbar-width: thin;
    background: #fafafa;

    &::-webkit-scrollbar {
        width: 4px;
    }
    &::-webkit-scrollbar-thumb {
        background: #e0e0e0;
        border-radius: 4px;
    }
}

.cart-item {
    background: #fff;
    margin: 8px 12px;
    border-radius: 16px;
    border: 1px solid #f0f0f0;
    transition: all 0.3s ease;

    &:hover {
        border-color: #e0e0e0;
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
    }

    .qty-circle {
        position: absolute;
        top: -6px;
        right: -6px;
        background: #d32f2f;
        color: #fff;
        font-size: 0.65rem;
        font-weight: 700;
        width: 20px;
        height: 20px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 2;
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
    }
}

.image-placeholder {
    width: 90px;
    height: 90px;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
}

.cart-item-name {
    line-height: 1.3;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.quantity-controls {
    .qty-btn {
        border-color: #e0e0e0;
        background: #fff;
        &:hover {
            border-color: #000;
        }
    }
}

.quantity-input {
    width: 44px;
    border: 1px solid transparent;
    border-radius: 4px;
    outline: none;
    background: transparent;
    appearance: textfield;
    -moz-appearance: textfield;
    transition: all 0.2s ease;
    &:focus {
        border-color: #000;
        background: #f8f8f8;
    }
}
.quantity-input::-webkit-outer-spin-button,
.quantity-input::-webkit-inner-spin-button {
    -webkit-appearance: none;
    margin: 0;
}

.delete-btn {
    opacity: 0.5;
    transition: all 0.2s;

    &:hover {
        opacity: 1;
        color: #d32f2f !important;
    }
}

.cart-footer {
    border-top: 1px solid #f0f0f0;
    background: #fff;
    box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.04);
}

.price-summary {
    background: #fafafa;
    border-radius: 12px;
    padding: 16px;
}

.checkout-btn-custom {
    background-color: #e8f5e9 !important;
    border: 1.5px solid #4caf50 !important;
    color: #1b5e20 !important;
    height: 52px !important;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
        background-color: #c8e6c9 !important;
        border-color: #388e3c !important;
        color: #1b5e20 !important;
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(27, 94, 32, 0.15);
    }
}

.continue-btn-custom {
    background-color: #f0f1ff !important;
    border: 1.5px solid #1e257c !important;
    color: #1e257c !important;
    height: 52px !important;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
        background-color: #e2e4ff !important;
        border-color: #1a206a !important;
        color: #1a206a !important;
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(30, 37, 124, 0.15);
    }
}

.cart-item-price,
.item-total-price,
.total-summary-price {
    color: #1e257c !important;
}

.empty-icon-wrap {
    width: 120px;
    height: 120px;
    background: #fafafa;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 2px dashed #e0e0e0;
}

/* Transitions */
.cart-item-enter-active {
    transition: all 0.3s ease-out;
}
.cart-item-leave-active {
    transition: all 0.3s ease-in;
}
.cart-item-enter-from {
    opacity: 0;
    transform: translateX(30px);
}
.cart-item-leave-to {
    opacity: 0;
    transform: translateX(-30px);
}
.cart-item-move {
    transition: transform 0.3s ease;
}
</style>
