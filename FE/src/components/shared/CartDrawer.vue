<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import { PATH } from '@/router/routePaths';

const router = useRouter();
const cartStore = useCartStore();
const authStore = useAuthStore();

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

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
    <v-navigation-drawer v-model="cartStore.isDrawerOpen" temporary location="right" width="420" class="cart-drawer">
        <!-- Header -->
        <div class="cart-header d-flex align-center justify-space-between px-6 py-5">
            <h3 class="text-h6 font-weight-black d-flex align-center">
                <v-icon class="mr-2" size="22">mdi-bag-outline</v-icon>
                Giỏ hàng ({{ cartStore.cartCount }})
            </h3>
            <v-btn icon variant="text" size="small" @click="cartStore.closeDrawer()">
                <v-icon>mdi-close</v-icon>
            </v-btn>
        </div>

        <v-divider />

        <!-- Cart Items -->
        <div class="cart-items-container" v-if="!cartStore.isEmpty">
            <TransitionGroup name="cart-item" tag="div">
                <div v-for="item in cartStore.cartItems" :key="item.idChiTietSanPham" class="cart-item px-6 py-4">
                    <div class="d-flex gap-4">
                        <!-- Product Image -->
                        <div class="cart-item-image">
                            <v-img v-if="item.hinhAnh" :src="item.hinhAnh" cover class="rounded-lg" width="90" height="90"></v-img>
                            <div v-else class="image-placeholder rounded-lg">
                                <v-icon size="32" color="grey-lighten-2">mdi-shoe-sneaker</v-icon>
                            </div>
                        </div>

                        <!-- Product Info -->
                        <div class="flex-grow-1">
                            <h4 class="cart-item-name text-body-2 font-weight-black mb-1">{{ item.tenSanPham }}</h4>
                            <p class="cart-item-variant text-caption text-grey-darken-1 mb-2">
                                {{ item.tenMauSac }} / {{ item.tenKichThuoc }}
                            </p>
                            <p class="cart-item-price text-body-2 font-weight-bold">{{ formatPrice(item.giaBan) }}</p>

                            <!-- Quantity Controls -->
                            <div class="d-flex align-center justify-space-between mt-2">
                                <div class="quantity-controls d-flex align-center">
                                    <v-btn
                                        icon
                                        variant="outlined"
                                        size="x-small"
                                        :disabled="item.soLuong <= 1"
                                        @click="handleQuantityChange(item.idChiTietSanPham, -1)"
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
                                    >
                                        <v-icon size="14">mdi-plus</v-icon>
                                    </v-btn>
                                </div>
                                <v-btn
                                    icon
                                    variant="text"
                                    size="x-small"
                                    color="grey"
                                    @click="cartStore.removeFromCart(item.idChiTietSanPham)"
                                >
                                    <v-icon size="18">mdi-delete-outline</v-icon>
                                </v-btn>
                            </div>
                        </div>
                    </div>
                </div>
            </TransitionGroup>
        </div>

        <!-- Empty State -->
        <div v-else class="empty-cart d-flex flex-column align-center justify-center py-16 px-6">
            <v-icon size="80" color="grey-lighten-2" class="mb-6">mdi-bag-outline</v-icon>
            <h4 class="text-h6 font-weight-bold text-grey-darken-1 mb-2">Giỏ hàng trống</h4>
            <p class="text-body-2 text-grey text-center mb-6">Hãy khám phá và thêm sản phẩm yêu thích vào giỏ hàng!</p>
            <v-btn
                color="black"
                rounded="pill"
                class="font-weight-bold text-none"
                @click="
                    cartStore.closeDrawer();
                    router.push(PATH.SHOES);
                "
            >
                Khám phá sản phẩm
            </v-btn>
        </div>

        <!-- Footer -->
        <template v-slot:append v-if="!cartStore.isEmpty">
            <div class="cart-footer px-6 py-5">
                <div class="d-flex justify-space-between align-center mb-4">
                    <span class="text-body-1 font-weight-bold text-grey-darken-1">Tạm tính</span>
                    <span class="text-h6 font-weight-black">{{ formatPrice(cartStore.cartTotal) }}</span>
                </div>
                <p class="text-caption text-grey mb-4">Phí vận chuyển sẽ được tính ở bước thanh toán</p>
                <v-btn
                    block
                    size="large"
                    color="black"
                    rounded="pill"
                    class="font-weight-black text-none checkout-btn mb-3"
                    @click="handleCheckout"
                >
                    Thanh toán · {{ formatPrice(cartStore.cartTotal) }}
                </v-btn>
                <v-btn
                    block
                    size="large"
                    variant="outlined"
                    rounded="pill"
                    class="font-weight-bold text-none"
                    @click="
                        cartStore.closeDrawer();
                        router.push(PATH.SHOES);
                    "
                >
                    Tiếp tục mua sắm
                </v-btn>
            </div>
        </template>
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
    h3 {
        letter-spacing: -0.3px;
    }
}

.cart-items-container {
    flex: 1;
    overflow-y: auto;
    scrollbar-width: thin;
    &::-webkit-scrollbar {
        width: 4px;
    }
    &::-webkit-scrollbar-thumb {
        background: #e0e0e0;
        border-radius: 4px;
    }
}

.cart-item {
    border-bottom: 1px solid #f5f5f5;
    transition: all 0.3s ease;

    &:hover {
        background: #fafafa;
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
    .v-btn {
        border-color: #e0e0e0;
        &:hover {
            border-color: #000;
        }
    }
}

.cart-footer {
    border-top: 1px solid #f0f0f0;
    background: #fff;
}

.checkout-btn {
    letter-spacing: 0.5px;
    font-size: 0.95rem;
    height: 52px !important;
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
