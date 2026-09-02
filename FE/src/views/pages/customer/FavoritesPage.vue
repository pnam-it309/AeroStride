<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useToastStore } from '@/stores/toastStore';
import { dichVuSanPhamPublic } from '@/services/public/dichVuSanPhamPublic';
import { dichVuFile } from '@/services/core/dichVuFile';
import shoe1Img from '@/assets/images/products/cat_running.jpg';
import shoe2Img from '@/assets/images/products/cat_training.jpg';
import shoe3Img from '@/assets/images/products/cat_speed.jpg';
import shoe4Img from '@/assets/images/products/s4.jpg';
import shoe5Img from '@/assets/images/products/s7.jpg';
import shoe6Img from '@/assets/images/products/s11.jpg';
import MainHeader from '@/components/shared/MainHeader.vue';
import MainFooter from '@/components/shared/MainFooter.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';

const FALLBACK_SHOES = [shoe3Img, shoe1Img, shoe2Img, shoe4Img, shoe5Img, shoe6Img];
const DEFAULT_SHOE_IMAGE = shoe3Img;

const getDeterministicFallback = (id) => {
    if (!id) return DEFAULT_SHOE_IMAGE;
    let hash = 0;
    const str = String(id);
    for (let i = 0; i < str.length; i++) {
        hash = (hash << 5) - hash + str.charCodeAt(i);
        hash |= 0;
    }
    const idx = Math.abs(hash) % FALLBACK_SHOES.length;
    return FALLBACK_SHOES[idx];
};

const router = useRouter();
const toastStore = useToastStore();
const favoriteProducts = ref([]);
const loading = ref(true);

const isAbsoluteUrl = (v) =>
    typeof v !== 'string' ||
    /^(https?:)?\/\//i.test(v) ||
    v.startsWith('data:') ||
    v.startsWith('blob:') ||
    (v.startsWith('/') && !v.startsWith('/uploads/'));

const getProductImage = (p) => {
    const raw = p.hinhAnh || p.duongDanAnh || p.images?.[0]?.duongDanAnh;
    if (!raw) return getDeterministicFallback(p.id);
    if (isAbsoluteUrl(raw)) return raw;
    return dichVuFile.layUrlFile(raw.replace(/^\/+/, ''));
};

const getProductPrice = (p) => {
    return p.minPrice ?? p.giaBanThapNhat ?? p.giaBan ?? 0;
};

const formatPrice = (price) => {
    if (!price || isNaN(price)) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const handleImageError = (e, id) => {
    const target = e?.target || (e && e.tagName ? e : null);
    if (!target) return;
    if (target.getAttribute('data-fallback') === 'true') return;
    target.setAttribute('data-fallback', 'true');
    target.src = getDeterministicFallback(id);
};

import { getFavoriteIds, setFavoriteIds, removeFavorite as removeFavUtil } from '@/utils/favoritesUtil';

const fetchFavorites = async (silent = false) => {
    if (!silent) loading.value = true;
    try {
        const favoriteIds = getFavoriteIds();
        if (favoriteIds.length === 0) {
            favoriteProducts.value = [];
            loading.value = false;
            return;
        }

        const promises = favoriteIds.map((id) =>
            dichVuSanPhamPublic.layChiTietSanPham(id).catch(() => null)
        );
        const results = await Promise.all(promises);

        const validProducts = [];
        const validIds = [];

        results.forEach((p, index) => {
            if (p && p.id && (p.tenSanPham || p.ten)) {
                validProducts.push(p);
                validIds.push(p.id);
            }
        });

        favoriteProducts.value = validProducts;

        // Chỉ cập nhật lại nếu có sản phẩm bị xóa khỏi database
        if (validIds.length > 0 && validIds.length !== favoriteIds.length) {
            setFavoriteIds(validIds);
        }
    } catch (error) {
        console.error('Error fetching favorites:', error);
    } finally {
        loading.value = false;
    }
};

const onFavoritesUpdated = () => {
    fetchFavorites(true);
};

onMounted(() => {
    fetchFavorites();
    window.addEventListener('favorites-updated', onFavoritesUpdated);
});

onUnmounted(() => {
    window.removeEventListener('favorites-updated', onFavoritesUpdated);
});

const goToDetail = (id) => {
    router.push(`/product/${id}`);
};

const removeFavorite = (id, event) => {
    if (event) event.stopPropagation();
    removeFavUtil(id);
    toastStore.showToast('Đã xoá khỏi danh sách yêu thích', 'info');
    fetchFavorites(true);
};
</script>

<template>
    <div class="favorites-page bg-grey-lighten-4 min-vh-100 d-flex flex-column">
        <MainHeader />
        <div class="header-spacing" style="height: 104px"></div>

        <v-container class="py-10 flex-grow-1">
            <div class="d-flex align-center justify-space-between mb-6">
                <div>
                    <h1 class="text-h4 font-weight-black text-grey-darken-4 mb-1">Danh Sách Yêu Thích</h1>
                    <p class="text-body-1 text-grey-darken-1">
                        Những mẫu giày bạn đã thả tim sẽ được lưu giữ tại đây ({{ favoriteProducts.length }} sản phẩm).
                    </p>
                </div>
                <v-btn
                    v-if="favoriteProducts.length > 0"
                    variant="outlined"
                    color="primary"
                    prepend-icon="mdi-shopping-outline"
                    rounded="pill"
                    class="font-weight-bold"
                    @click="router.push('/shoes')"
                >
                    Xem thêm mẫu khác
                </v-btn>
            </div>

            <!-- Loading State -->
            <v-row v-if="loading">
                <v-col v-for="i in 4" :key="i" cols="12" sm="6" md="4" lg="3">
                    <v-skeleton-loader type="image, article" class="rounded-xl"></v-skeleton-loader>
                </v-col>
            </v-row>

            <!-- Product Grid -->
            <v-row v-else-if="favoriteProducts.length > 0">
                <v-col v-for="p in favoriteProducts" :key="p.id" cols="12" sm="6" md="4" lg="3">
                    <div class="product-fav-card bg-white rounded-xl pa-3 position-relative elevation-1" @click="goToDetail(p.id)">
                        <!-- Nút Xoá Yêu Thích -->
                        <button
                            type="button"
                            class="fav-remove-btn"
                            title="Bỏ yêu thích"
                            @click="(e) => removeFavorite(p.id, e)"
                        >
                            <v-icon color="red" size="20">mdi-heart</v-icon>
                        </button>

                        <!-- Box Ảnh Sản Phẩm -->
                        <div class="product-img-box rounded-lg bg-grey-lighten-4 mb-3 overflow-hidden d-flex align-center justify-center">
                            <img
                                :src="getProductImage(p)"
                                :alt="p.tenSanPham"
                                class="fav-shoe-img"
                                referrerpolicy="no-referrer"
                                loading="lazy"
                                decoding="async"
                                @error="(e) => handleImageError(e, p.id)"
                            />
                        </div>

                        <!-- Thông tin sản phẩm -->
                        <div class="px-1 text-left">
                            <div class="text-caption font-weight-bold text-uppercase text-primary mb-1">
                                {{ p.tenThuongHieu || 'AEROSTRIDE' }}
                            </div>
                            <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-4 mb-2 text-truncate" :title="p.tenSanPham">
                                {{ p.tenSanPham }}
                            </h3>
                            <div class="d-flex align-center justify-space-between">
                                <span class="text-h6 font-weight-black text-black">
                                    {{ formatPrice(getProductPrice(p)) }}
                                </span>
                                <v-btn
                                    size="small"
                                    color="#1e257c"
                                    variant="tonal"
                                    rounded="pill"
                                    class="px-3 text-caption font-weight-bold"
                                    @click.stop="goToDetail(p.id)"
                                >
                                    Xem ngay
                                </v-btn>
                            </div>
                        </div>
                    </div>
                </v-col>
            </v-row>

            <!-- Empty State -->
            <div v-else class="text-center py-16 bg-white rounded-xl elevation-1 mt-4">
                <v-icon size="88" color="grey-lighten-2" class="mb-4">mdi-heart-broken-outline</v-icon>
                <h2 class="text-h5 font-weight-black text-grey-darken-3 mb-2">Chưa có sản phẩm yêu thích nào</h2>
                <p class="text-body-1 text-grey-darken-1 mb-6 max-w-500 mx-auto">
                    Bạn chưa thả tim mẫu giày nào. Hãy khám phá ngay bộ sưu tập giày thể thao AeroStride để chọn cho mình đôi giày ưng ý nhất!
                </p>
                <v-btn color="#1e257c" size="large" rounded="pill" class="px-8 font-weight-bold text-white elevation-2" @click="router.push('/shoes')">
                    Khám phá sản phẩm ngay
                </v-btn>
            </div>
        </v-container>

        <!-- Main Footer -->
        <MainFooter class="mt-12" />

        <CustomerChat />
    </div>
</template>

<style scoped>
.product-fav-card {
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    border: 1px solid rgba(0, 0, 0, 0.05);
}

.product-fav-card:hover {
    transform: translateY(-6px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1) !important;
}

.fav-remove-btn {
    position: absolute;
    top: 14px;
    right: 14px;
    z-index: 5;
    width: 34px;
    height: 34px;
    border-radius: 50%;
    background: white;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    border: none;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.2s ease, background-color 0.2s ease;
}

.fav-remove-btn:hover {
    transform: scale(1.15);
    background-color: #fff0f0;
}

.product-img-box {
    aspect-ratio: 1;
    width: 100%;
}

.fav-shoe-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.4s ease;
}

.product-fav-card:hover .fav-shoe-img {
    transform: scale(1.06);
}

.max-w-500 {
    max-width: 500px;
}
</style>
