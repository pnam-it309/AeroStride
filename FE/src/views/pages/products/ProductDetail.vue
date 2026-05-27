<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import PromotionBar from '@/components/shared/PromotionBar.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import CartDrawer from '@/components/shared/CartDrawer.vue';
import { dichVuSanPhamPublic } from '@/services/public/dichVuSanPhamPublic';
import { useCartStore } from '@/stores/cartStore';
import { useToastStore } from '@/stores/toastStore';

const route = useRoute();
const cartStore = useCartStore();
const toastStore = useToastStore();
const loading = ref(true);
const product = ref(null);
const selectedSize = ref(null);
const addingToCart = ref(false);

const fetchProduct = async () => {
    loading.value = true;
    try {
        const data = await dichVuSanPhamPublic.layChiTietSanPham(route.params.id);
        product.value = data;
    } catch (error) {
        console.error('Error fetching product:', error);
    } finally {
        loading.value = false;
    }
};

onMounted(() => {
    fetchProduct();
});

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const sizes = computed(() => {
    if (!product.value?.variants) return [];
    const uniqueSizes = [...new Set(product.value.variants.map((v) => v.tenKichThuoc))];
    return uniqueSizes.sort((a, b) => parseFloat(a) - parseFloat(b));
});

const allImages = computed(() => {
    if (!product.value?.variants) return [];
    const images = [];
    product.value.variants.forEach((v) => {
        if (v.images) {
            v.images.forEach((img) => {
                if (!images.find((i) => i.duongDanAnh === img.duongDanAnh)) {
                    images.push(img);
                }
            });
        }
    });
    return images;
});

// Tìm variant phù hợp với size đã chọn
const selectedVariant = computed(() => {
    if (!product.value?.variants || !selectedSize.value) return null;
    return product.value.variants.find((v) => v.tenKichThuoc === selectedSize.value);
});

// Thêm vào giỏ hàng
const addToCart = () => {
    if (!selectedSize.value) {
        toastStore.showToast('Vui lòng chọn kích thước', 'warning');
        return;
    }
    const variant = selectedVariant.value;
    if (!variant) {
        toastStore.showToast('Sản phẩm không khả dụng', 'error');
        return;
    }

    addingToCart.value = true;
    const firstImage = variant.images?.length
        ? variant.images[0].duongDanAnh
        : allImages.value.length
          ? allImages.value[0].duongDanAnh
          : '';

    const result = cartStore.addToCart({
        idChiTietSanPham: variant.id,
        tenSanPham: product.value.tenSanPham,
        hinhAnh: firstImage,
        tenMauSac: variant.tenMauSac || '',
        tenKichThuoc: variant.tenKichThuoc || '',
        giaBan: variant.giaBan || product.value.giaBanThapNhat,
        soLuong: 1,
        soLuongTonKho: variant.soLuong || 99
    });

    setTimeout(() => {
        addingToCart.value = false;
        if (result.success) {
            toastStore.showToast('Đã thêm vào giỏ hàng!', 'success');
            cartStore.openDrawer();
        } else {
            toastStore.showToast(result.message, 'warning');
        }
    }, 300);
};
</script>

<template>
    <div class="product-detail-page bg-white min-vh-100">
        <MainHeader />

        <div class="header-spacing" style="height: 104px"></div>
        <PromotionBar />

        <v-container class="mt-12" v-if="product">
            <v-row>
                <!-- Left: Image Gallery -->
                <v-col cols="12" md="7" class="image-gallery">
                    <v-row v-if="allImages.length > 0">
                        <v-col v-for="(img, i) in allImages" :key="i" cols="6" class="mb-4">
                            <v-img :src="img.duongDanAnh" cover class="rounded-lg bg-grey-lighten-4 aspect-square"></v-img>
                        </v-col>
                    </v-row>
                    <div v-else class="image-placeholder-large">
                        <v-icon size="64" color="grey-lighten-2">mdi-image-outline</v-icon>
                    </div>
                </v-col>

                <!-- Right: Product Info -->
                <v-col cols="12" md="5">
                    <div class="sticky-info-panel px-md-8">
                        <div class="header-info mb-8">
                            <h1 class="product-title text-h4 font-weight-black mb-1">{{ product.tenSanPham }}</h1>
                            <p class="product-cat text-subtitle-1 font-weight-bold grey--text">
                                {{ product.tenDanhMuc }} - {{ product.tenThuongHieu }}
                            </p>
                            <div class="product-price mt-4 text-h6 font-weight-black">{{ formatPrice(product.giaBanThapNhat) }}</div>
                        </div>

                        <!-- Size Selection -->
                        <div class="size-selection-section mb-10">
                            <div class="d-flex justify-space-between align-center mb-4">
                                <span class="font-weight-black">Chọn kích thước</span>
                                <a href="#" class="size-guide">Bảng size</a>
                            </div>
                            <v-row class="g-2" v-if="sizes.length > 0">
                                <v-col v-for="size in sizes" :key="size" cols="4">
                                    <div class="size-box" :class="{ active: selectedSize === size }" @click="selectedSize = size">
                                        {{ size }}
                                    </div>
                                </v-col>
                            </v-row>
                            <p v-else class="text-grey">Không có sẵn kích thước nào.</p>
                        </div>

                        <!-- Action Buttons -->
                        <div class="action-buttons d-flex flex-column gap-4">
                            <v-btn
                                block
                                size="x-large"
                                color="black"
                                rounded="pill"
                                class="font-weight-black py-6"
                                :loading="addingToCart"
                                @click="addToCart"
                            >
                                <v-icon class="mr-2">mdi-bag-plus-outline</v-icon>
                                Thêm vào giỏ hàng
                            </v-btn>
                            <v-btn block size="x-large" variant="outlined" rounded="pill" class="font-weight-black py-6">
                                Yêu thích <v-icon class="ml-2">mdi-heart-outline</v-icon>
                            </v-btn>
                        </div>

                        <!-- Description -->
                        <div class="product-description mt-12 pt-12 border-t">
                            <p class="desc-text">{{ product.moTa }}</p>
                            <ul class="mt-6 pl-4 spec-list">
                                <li>Thương hiệu: {{ product.tenThuongHieu }}</li>
                                <li>Xuất xứ: {{ product.tenXuatXu }}</li>
                                <li>Mã sản phẩm: {{ product.maSanPham }}</li>
                            </ul>
                        </div>

                        <!-- Expansion Panels -->
                        <div class="mt-8">
                            <v-expansion-panels flat variant="accordion">
                                <v-expansion-panel title="Giao hàng & Trả hàng" class="border-t">
                                    <v-expansion-panel-text class="text-caption">
                                        Miễn phí giao hàng tiêu chuẩn cho đơn hàng trên 5.000.000₫. Miễn phí đổi trả trong vòng 30 ngày.
                                    </v-expansion-panel-text>
                                </v-expansion-panel>
                                <v-expansion-panel title="Đánh giá (0)" class="border-t">
                                    <v-expansion-panel-text>
                                        <div class="d-flex align-center mb-2">
                                            <v-rating model-value="0" density="compact" color="black" half-increments readonly></v-rating>
                                            <span class="ml-2 font-weight-bold">Chưa có đánh giá</span>
                                        </div>
                                    </v-expansion-panel-text>
                                </v-expansion-panel>
                            </v-expansion-panels>
                        </div>
                    </div>
                </v-col>
            </v-row>
        </v-container>
        <v-container v-else-if="loading" class="text-center py-16">
            <v-progress-circular indeterminate color="black"></v-progress-circular>
        </v-container>

        <!-- Cart Drawer -->
        <CartDrawer />
        <!-- Global Chat System -->
        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.product-detail-page {
    padding-top: 64px;
}

.image-placeholder-large {
    width: 100%;
    aspect-ratio: 1;
    background: #f6f6f6;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    border-radius: 4px;
    .index-label {
        position: absolute;
        bottom: 20px;
        left: 20px;
        font-size: 0.65rem;
        font-weight: 900;
        color: #ccc;
        letter-spacing: 2px;
    }
}

.sticky-info-panel {
    position: sticky;
    top: 140px;
}

.product-cat {
    color: #111;
}
.product-price {
    color: #111;
}

.size-guide {
    font-size: 0.9rem;
    color: #757575;
    text-decoration: none;
    font-weight: 600;
    border-bottom: 1px solid transparent;
    &:hover {
        border-bottom-color: #757575;
    }
}

.size-box {
    border: 1px solid #e5e5e5;
    border-radius: 4px;
    padding: 12px;
    text-align: center;
    font-size: 0.95rem;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.2s ease;
    &:hover {
        border-color: #000;
    }
    &.active {
        border-color: #000;
        background: #000;
        color: #fff;
    }
}

.desc-text {
    font-size: 1.05rem;
    line-height: 1.8;
    color: #111;
}
.spec-list {
    list-style: disc;
    li {
        font-size: 0.9rem;
        margin-bottom: 8px;
        color: #111;
        font-weight: 500;
    }
}

:deep(.v-expansion-panel-title) {
    font-weight: 800;
    font-size: 1.1rem;
    padding: 24px 0;
}
:deep(.v-expansion-panel-text__wrapper) {
    padding: 0 0 24px 0;
}

.gap-4 {
    gap: 16px;
}

.aspect-square {
    aspect-ratio: 1;
}

@media (max-width: 960px) {
    .sticky-info-panel {
        position: relative;
        top: 0;
        padding: 0;
        margin-top: 40px;
    }
}
</style>
