<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import PromotionBar from '@/components/shared/PromotionBar.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import CartDrawer from '@/components/shared/CartDrawer.vue';
import { dichVuSanPhamPublic } from '@/services/public/dichVuSanPhamPublic';
import { useCartStore } from '@/stores/cartStore';
import { useToastStore } from '@/stores/toastStore';
import { useSeoMeta } from '@/composables/useSeoMeta';

const route = useRoute();
const cartStore = useCartStore();
const toastStore = useToastStore();
const loading = ref(true);
const product = ref(null);
const selectedColor = ref(null);
const selectedSize = ref(null);
const selectedQuantity = ref(1);
const activeSlide = ref(0);
const isFavorite = ref(false);
const addingToCart = ref(false);
const recommendedProducts = ref([]);

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

const fetchRecommendations = async () => {
    try {
        const data = await dichVuSanPhamPublic.layDanhSachSanPham({ page: 1, size: 4 });
        recommendedProducts.value = data.content || [];
    } catch (error) {
        console.error('Error fetching recommendations:', error);
    }
};

const { setProductSeo } = useSeoMeta();

onMounted(() => {
    fetchProduct();
    fetchRecommendations();
});

// Cập nhật SEO và trạng thái Yêu thích khi product data load xong
watch(product, (newProduct) => {
    if (newProduct) {
        setProductSeo(newProduct);
        let favorites = JSON.parse(localStorage.getItem('aerostride_favorites') || '[]');
        isFavorite.value = favorites.includes(newProduct.id);
    }
});

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const colors = computed(() => {
    if (!product.value?.variants) return [];
    return [...new Set(product.value.variants.map((v) => v.tenMauSac))].filter(Boolean);
});

const sizes = computed(() => {
    if (!product.value?.variants) return [];
    let filteredVariants = product.value.variants;
    if (selectedColor.value) {
        filteredVariants = filteredVariants.filter((v) => v.tenMauSac === selectedColor.value);
    }
    const uniqueSizes = [...new Set(filteredVariants.map((v) => v.tenKichThuoc))].filter(Boolean);
    return uniqueSizes.sort((a, b) => parseFloat(a) - parseFloat(b));
});

watch(selectedColor, () => {
    selectedSize.value = null; // Reset size when color changes
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

watch(allImages, (newImages) => {
    if (newImages && newImages.length > 0) {
        activeSlide.value = 0;
    }
}, { immediate: true });

// Tìm variant phù hợp với color và size đã chọn
const selectedVariant = computed(() => {
    if (!product.value?.variants || !selectedColor.value || !selectedSize.value) return null;
    return product.value.variants.find((v) => v.tenMauSac === selectedColor.value && v.tenKichThuoc === selectedSize.value);
});

const displayPrice = computed(() => {
    if (selectedVariant.value && selectedVariant.value.giaBan) return selectedVariant.value.giaBan;
    if (product.value?.giaBanThapNhat) return product.value.giaBanThapNhat;
    // Fallback if giaBanThapNhat is missing, find min price from variants
    if (product.value?.variants?.length > 0) {
        const prices = product.value.variants.map(v => v.giaBan).filter(p => p > 0);
        if (prices.length > 0) return Math.min(...prices);
    }
    return 0;
});

const maxQuantity = computed(() => {
    if (selectedVariant.value) return selectedVariant.value.soLuong || 0;
    if (product.value?.variants?.length > 0) {
        if (selectedColor.value) {
            return product.value.variants
                .filter(v => v.tenMauSac === selectedColor.value)
                .reduce((sum, v) => sum + (v.soLuong || 0), 0);
        }
        return product.value.variants.reduce((sum, v) => sum + (v.soLuong || 0), 0);
    }
    return 0;
});

watch(maxQuantity, (newMax) => {
    if (selectedQuantity.value > newMax) {
        selectedQuantity.value = newMax > 0 ? newMax : 1;
    }
});

// Thêm vào giỏ hàng
const addToCart = async () => {
    if (!product.value) return;

    if (!selectedColor.value) {
        toastStore.showToast('Vui lòng chọn màu sắc', 'warning');
        return;
    }

    if (!selectedSize.value) {
        toastStore.showToast('Vui lòng chọn kích thước', 'warning');
        return;
    }

    const variant = selectedVariant.value;
    if (!variant) {
        toastStore.showToast('Phiên bản này hiện không có sẵn', 'warning');
        return;
    }

    if (selectedQuantity.value > variant.soLuong) {
        toastStore.showToast(`Chỉ còn ${variant.soLuong} sản phẩm trong kho`, 'warning');
        return;
    }

    addingToCart.value = true;
    try {
        const result = await cartStore.addToCart({
            idChiTietSanPham: variant.id,
            soLuong: selectedQuantity.value,
            // Truyền metadata để drawer hiển thị ngay (không cần chờ sync)
            tenSanPham: product.value?.ten || product.value?.tenSanPham || '',
            hinhAnh: variant.images?.[0]?.duongDanAnh || product.value?.hinhAnh || '',
            tenMauSac: variant.tenMauSac || selectedColor.value || '',
            tenKichThuoc: variant.tenKichThuoc || selectedSize.value || '',
            giaBan: variant.giaBan || displayPrice.value || 0,
            soLuongTonKho: variant.soLuong || 0
        });
        if (result?.success) {
            toastStore.showToast('Đã thêm vào giỏ hàng', 'success');
            cartStore.openDrawer();
        } else {
            toastStore.showToast(result?.message || 'Không thể thêm vào giỏ hàng', 'warning');
        }
    } catch (e) {
        toastStore.showToast('Có lỗi xảy ra, vui lòng thử lại', 'error');
    } finally {
        addingToCart.value = false;
    }
};

const toggleFavorite = () => {
    isFavorite.value = !isFavorite.value;
    let favorites = JSON.parse(localStorage.getItem('aerostride_favorites') || '[]');
    if (isFavorite.value) {
        if (!favorites.includes(product.value.id)) {
            favorites.push(product.value.id);
        }
        toastStore.showToast('Đã thêm vào danh sách yêu thích', 'success');
    } else {
        favorites = favorites.filter(id => id !== product.value.id);
        toastStore.showToast('Đã huỷ yêu thích', 'info');
    }
    localStorage.setItem('aerostride_favorites', JSON.stringify(favorites));
    window.dispatchEvent(new Event('favorites-updated'));
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
                <v-col cols="12" md="6" lg="5" class="image-gallery">
                    <div v-if="allImages.length > 0">
                        <!-- Main Auto Slider -->
                        <div class="rounded-lg bg-grey-lighten-4 mb-4" style="aspect-ratio: 1; max-height: 600px; overflow: hidden;">
                            <v-carousel
                                v-model="activeSlide"
                                cycle
                                interval="3000"
                                hide-delimiters
                                show-arrows="hover"
                                height="100%"
                            >
                                <v-carousel-item
                                    v-for="(img, i) in allImages"
                                    :key="i"
                                    :src="img.duongDanAnh"
                                    cover
                                ></v-carousel-item>
                            </v-carousel>
                        </div>

                        <!-- Thumbnail Strip -->
                        <v-row class="g-2">
                            <v-col v-for="(img, i) in allImages" :key="i" cols="3" sm="2" class="mb-2">
                                <v-card class="rounded-lg bg-grey-lighten-4 overflow-hidden"
                                    :elevation="activeSlide === i ? 4 : 0"
                                    :style="activeSlide === i ? 'border: 2px solid #e53935;' : 'border: 1px solid #eee; cursor: pointer;'"
                                    @click="activeSlide = i">
                                    <v-img :src="img.duongDanAnh" cover class="aspect-square"></v-img>
                                </v-card>
                            </v-col>
                        </v-row>
                    </div>
                    <div v-else class="image-placeholder-large">
                        <v-icon size="64" color="grey-lighten-2">mdi-image-outline</v-icon>
                    </div>
                </v-col>

                <!-- Right: Product Info -->
                <v-col cols="12" md="6" lg="7">
                    <div class="sticky-info-panel px-md-8">
                        <div class="header-info mb-8">
                            <h1 class="product-title text-h4 font-weight-black mb-1">{{ product.tenSanPham }}</h1>
                            <p class="product-cat text-subtitle-1 font-weight-bold grey--text">
                                {{ product.tenThuongHieu }}
                            </p>
                            <div class="product-price mt-4 text-h5 font-weight-black text-blue-darken-4">{{
                                formatPrice(displayPrice) }}</div>
                        </div>

                        <!-- Description Info moved to top -->
                        <div class="product-description mb-8">
                            <p class="desc-text text-body-1">{{ product.moTa }}</p>
                            <div class="d-flex flex-wrap gap-4 mt-3">
                                <v-chip size="small" variant="flat" color="grey-lighten-4"
                                    class="font-weight-bold"><v-icon start size="14">mdi-domain</v-icon> {{
                                    product.tenThuongHieu }}</v-chip>
                                <v-chip size="small" variant="flat" color="grey-lighten-4"
                                    class="font-weight-bold"><v-icon start size="14">mdi-earth</v-icon> Xuất xứ: {{
                                    product.tenXuatXu }}</v-chip>
                                <v-chip size="small" variant="flat" color="grey-lighten-4"
                                    class="font-weight-bold"><v-icon start size="14">mdi-barcode</v-icon> Mã: {{
                                    product.maSanPham }}</v-chip>
                            </div>
                        </div>

                        <!-- Color Selection -->
                        <div class="color-selection-section mb-6" v-if="colors.length > 0">
                            <div class="d-flex justify-space-between align-center mb-4">
                                <span class="font-weight-black">Chọn màu sắc</span>
                            </div>
                            <v-row class="g-2">
                                <v-col v-for="color in colors" :key="color" cols="4">
                                    <div class="size-box" :class="{ active: selectedColor === color }"
                                        @click="selectedColor = color">
                                        {{ color }}
                                    </div>
                                </v-col>
                            </v-row>
                        </div>

                        <!-- Size Selection -->
                        <div class="size-selection-section mb-10">
                            <div class="d-flex justify-space-between align-center mb-4">
                                <span class="font-weight-black">Chọn kích thước</span>
                                <a href="#" class="size-guide">Bảng kích thước</a>
                            </div>
                            <v-row class="g-2" v-if="sizes.length > 0">
                                <v-col v-for="size in sizes" :key="size" cols="4">
                                    <div class="size-box" :class="{ active: selectedSize === size }"
                                        @click="selectedSize = size">
                                        {{ size.replace('Size ', '') }}
                                    </div>
                                </v-col>
                            </v-row>
                            <p v-else class="text-grey">Vui lòng chọn màu sắc để xem các kích thước.</p>
                        </div>

                        <!-- Quantity Selection -->
                        <div class="quantity-selection-section mb-10">
                            <div class="d-flex align-center justify-space-between mb-4">
                                <span class="font-weight-black">Số lượng</span>
                                <span class="text-caption text-grey" v-if="maxQuantity > 0">
                                    Còn lại: {{ maxQuantity }}
                                </span>
                            </div>
                            <div class="d-flex align-center border rounded-lg py-2 px-4" style="width: 140px;">
                                <v-btn icon="mdi-minus" variant="plain" density="compact"
                                    :disabled="selectedQuantity <= 1" @click="selectedQuantity--"></v-btn>
                                <span class="font-weight-bold flex-grow-1 text-center">{{ selectedQuantity }}</span>
                                <v-btn icon="mdi-plus" variant="plain" density="compact"
                                    :disabled="selectedQuantity >= maxQuantity" @click="selectedQuantity++"></v-btn>
                            </div>
                        </div>

                        <!-- Action Buttons -->
                        <div class="action-buttons d-flex flex-column gap-4">
                            <v-btn block size="x-large" color="black" rounded="pill" class="font-weight-black py-6"
                                :loading="addingToCart" @click="addToCart">
                                <v-icon class="mr-2">mdi-bag-plus-outline</v-icon>
                                Thêm vào giỏ hàng
                            </v-btn>
                            <v-btn block size="x-large" variant="outlined" rounded="pill" class="font-weight-black py-6"
                                :class="isFavorite ? 'text-red' : ''"
                                :color="isFavorite ? 'red' : 'black'" @click="toggleFavorite">
                                {{ isFavorite ? 'Đã yêu thích' : 'Yêu thích' }}
                                <v-icon class="ml-2" :color="isFavorite ? 'red' : ''">{{ isFavorite ? 'mdi-heart' :
                                    'mdi-heart-outline' }}</v-icon>
                            </v-btn>
                        </div>

                        <!-- Shipping & Returns Beautiful Alert -->
                        <div class="shipping-returns mt-8">
                            <v-alert color="blue-lighten-5" border="start" border-color="blue-darken-3"
                                icon="mdi-truck-fast-outline" class="rounded-lg" elevation="0">
                                <div class="font-weight-bold text-blue-darken-4 mb-1">Giao hàng & Trả hàng</div>
                                <div class="text-caption text-blue-darken-4">Miễn phí giao hàng tiêu chuẩn cho đơn hàng
                                    trên 5.000.000₫. Miễn phí đổi trả trong vòng 30 ngày.</div>
                            </v-alert>
                        </div>
                    </div>
                </v-col>
            </v-row>
            
            <!-- Recommended Products Section -->
            <div class="recommended-section mt-16 pt-8 border-top" v-if="recommendedProducts.length > 0">
                <h2 class="text-h4 font-weight-black mb-8 text-center">Có Thể Bạn Cũng Thích</h2>
                <v-row>
                    <v-col v-for="p in recommendedProducts" :key="p.id" cols="12" sm="6" md="4" lg="3">
                        <div class="cursor-pointer" @click="$router.push(`/product/${p.id}`)">
                            <div class="rounded-xl bg-grey-lighten-4 mb-4 overflow-hidden d-flex align-center justify-center" style="aspect-ratio: 1">
                                <v-img :src="p.hinhAnh || ''" cover class="fill-height"></v-img>
                            </div>
                            
                            <div class="px-1 text-left">
                                <div class="text-caption font-weight-bold text-uppercase text-grey-darken-2 mb-1">{{ p.tenThuongHieu || 'NIKE' }}</div>
                                <h3 class="text-subtitle-1 font-weight-black text-black mb-1 text-truncate">{{ p.tenSanPham }}</h3>
                                <div class="text-subtitle-1 font-weight-black text-black">{{ formatPrice(p.giaBanThapNhat) }}</div>
                            </div>
                        </div>
                    </v-col>
                </v-row>
            </div>
            
        </v-container>
        <v-container v-else-if="loading" class="text-center py-16">
            <v-progress-circular indeterminate color="black"></v-progress-circular>
        </v-container>

        <!-- Landing style Footer -->
        <footer class="footer-landing py-10 text-center text-grey-darken-1 bg-white border-t mt-16">
            <LogoClient class="mb-4 d-inline-block" style="max-width: 150px" />
            <p>&copy; 2026 AeroStride. All rights reserved.</p>
        </footer>

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
