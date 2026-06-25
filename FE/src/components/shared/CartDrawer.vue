<script setup>
import { computed, onMounted } from 'vue';
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
    return cartStore.cartTotal >= FREE_SHIP_THRESHOLD ? 0 : SHIPPING_FEE;
});

const shippingProgress = computed(() => {
    if (cartStore.cartTotal >= FREE_SHIP_THRESHOLD) return 100;
    return Math.min((cartStore.cartTotal / FREE_SHIP_THRESHOLD) * 100, 99);
});

const remainingForFreeShip = computed(() => {
    return Math.max(0, FREE_SHIP_THRESHOLD - cartStore.cartTotal);
});

const handleQuantityChange = (id, delta) => {
    const item = cartStore.items.find((i) => i.idChiTietSanPham === id);
    if (item) {
        cartStore.updateQuantity(id, item.soLuong + delta);
    }
};

const handleCheckout = () => {
    cartStore.closeDrawer();
    if (!authStore.isLoggedIn) {
        router.push(PATH.LOGIN);
    } else {
        router.push(PATH.CHECKOUT);
    }
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
                    <v-chip size="small" color="black" variant="flat" class="ml-2 font-weight-bold">{{ cartStore.cartCount }}</v-chip>
                </h3>
                <v-btn icon variant="text" size="small" @click="cartStore.closeDrawer()" class="close-btn">
                    <v-icon>mdi-close</v-icon>
                </v-btn>
            </div>

            <v-divider />

            <!-- Free Shipping Progress -->
            <div v-if="!cartStore.isEmpty && cartStore.cartTotal < FREE_SHIP_THRESHOLD" class="shipping-progress px-6 py-4">
                <div class="d-flex align-center mb-2">
                    <v-icon size="16" color="orange-darken-2" class="mr-2">mdi-truck-fast-outline</v-icon>
                    <span class="text-caption font-weight-medium text-grey-darken-1">
                        Mua thêm <strong class="text-orange-darken-3">{{ formatPrice(remainingForFreeShip) }}</strong> để được
                        <strong class="text-orange-darken-3">miễn phí vận chuyển</strong>
                    </span>
                </div>
                <v-progress-linear :model-value="shippingProgress" height="6" color="orange" rounded class="shipping-bar"></v-progress-linear>
            </div>
            <div v-else-if="!cartStore.isEmpty" class="shipping-progress-free px-6 py-3">
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
                                            <span class="quantity-value mx-3 text-body-2 font-weight-bold">{{ item.soLuong }}</span>
                                            <v-btn
                                                icon
                                                variant="outlined"
                                                size="x-small"
                                                :disabled="item.soLuong >= item.soLuongTonKho"
                                                @click="handleQuantityChange(item.idChiTietSanPham, 1)"
                                                class="qty-btn"
                                            >
                                                <v-icon size="14">mdi-plus</v-icon>
                                            </v-btn>
                                        </div>
                                        <div class="d-flex align-center gap-2">
                                            <span class="text-body-2 font-weight-black text-black">{{ formatPrice(item.giaBan * item.soLuong) }}</span>
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
                            <span class="text-body-1 font-weight-bold">{{ formatPrice(cartStore.cartTotal) }}</span>
                        </div>
                        <div class="d-flex justify-space-between align-center mb-2">
                            <span class="text-body-2 text-grey-darken-1">Phí vận chuyển</span>
                            <span class="text-body-2 font-weight-medium" :class="shippingFee === 0 ? 'text-green' : 'text-grey-darken-1'">
                                {{ shippingFee === 0 ? 'Miễn phí' : formatPrice(shippingFee) }}
                            </span>
                        </div>
                        <v-divider class="my-2" />
                        <div class="d-flex justify-space-between align-center">
                            <span class="text-body-1 font-weight-bold text-black">Tổng cộng</span>
                            <span class="text-h6 font-weight-black text-black">{{ formatPrice(cartStore.cartTotal + shippingFee) }}</span>
                        </div>
                    </div>

                    <v-btn
                        block
                        size="large"
                        color="black"
                        rounded="pill"
                        class="font-weight-bold text-none checkout-btn elevation-2 mb-3"
                        @click="handleCheckout"
                    >
                        <v-icon class="mr-2" size="20">mdi-lock-outline</v-icon>
                        Thanh toán ngay · {{ formatPrice(cartStore.cartTotal + shippingFee) }}
                    </v-btn>
                    <v-btn
                        block
                        size="large"
                        variant="outlined"
                        rounded="pill"
                        class="font-weight-bold text-none"
                        @click="cartStore.closeDrawer(); router.push(PATH.SHOES)"
                    >
                        Tiếp tục mua sắm
                    </v-btn>
                </div>
            </template>
        </div>
    </v-navigation-drawer>
</template>

<style scoped lang="scss">
.cart-drawer {
    :deep(.v-navigation-drawer__content) {
        display: flex;
        flex-direction: column;
    }
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
        background: #000;
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

.quantity-value {
    min-width: 24px;
    text-align: center;
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

.checkout-btn {
    letter-spacing: 0.3px;
    font-size: 0.95rem;
    height: 52px !important;
    transition: transform 0.2s, box-shadow 0.2s;

    &:hover {
        transform: translateY(-1px);
        box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15) !important;
    }
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
