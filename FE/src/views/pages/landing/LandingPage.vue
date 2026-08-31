<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { PATH } from '@/router/routePaths';
import Preloader from '@/components/common/Preloader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import MainHeader from '@/components/shared/MainHeader.vue';
import FlashSaleSection from './sections/FlashSaleSection.vue';
import { useLandingPage } from '@/composables/useLandingPage';
import { useLandingCatalog } from '@/composables/useLandingCatalog';
import { useSeoMeta } from '@/composables/useSeoMeta';
import { useToastStore } from '@/stores/toastStore';
import shoe1Img from '@/assets/images/products/cat_running.jpg';
import shoe2Img from '@/assets/images/products/cat_training.jpg';
import shoe3Img from '@/assets/images/products/cat_speed.jpg';
import defaultShoeImg from '@/assets/images/products/cat_running.jpg';

const DEFAULT_SHOE_IMAGE = defaultShoeImg || new URL('/src/assets/images/products/cat_running.jpg', import.meta.url).href;

const handleImageError = (e) => {
    const target = e?.target || (e && e.tagName ? e : null);
    if (!target) return;
    if (target.getAttribute('data-fallback') === 'true') return;
    target.setAttribute('data-fallback', 'true');
    target.src = DEFAULT_SHOE_IMAGE;
};

const router = useRouter();
const toastStore = useToastStore();

const { activeSection, handleLogout, handlePreloaderFinish, isLoading, isLoggedIn, LANDING_SECTIONS, isSectionWarm, mouseX, mouseY } =
    useLandingPage();

const { heroProduct, isCatalogLoading, howProducts, topVariantsByQty, problemProducts, landingProducts, featuredVariants, discountedVariants } =
    useLandingCatalog(activeSection);

// SEO Setup
const { setSeoMeta } = useSeoMeta();
onMounted(() => {
    setSeoMeta({
        title: 'Giày Thể Thao Chính Hãng - Mua Sắm Online',
        description:
            'AeroStride - Cửa hàng giày thể thao chính hãng hàng đầu Việt Nam. Mua sắm giày Nike, Adidas, Puma với giá tốt nhất, giao hàng nhanh toàn quốc, đổi trả miễn phí 30 ngày.'
    });
});

// ─── 3D Model Viewer dynamic loading ─────────────────────────────────────────
const viewerReady = ref(false);
let loadStarted = false;

const loadViewer = async () => {
    if (loadStarted) return;
    loadStarted = true;
    try {
        await import('@google/model-viewer');
        viewerReady.value = true;
    } catch (error) {
        loadStarted = false;
        console.error('[LandingPage] model-viewer failed to load:', error);
    }
};

onMounted(() => {
    // Proactively load the 3D model viewer on landing page mount
    loadViewer();
    updateFavoriteIds();
    window.addEventListener('favorites-updated', updateFavoriteIds);
});

onUnmounted(() => {
    window.removeEventListener('favorites-updated', updateFavoriteIds);
});

import { getFavoriteIds, toggleFavorite as toggleFavUtil } from '@/utils/favoritesUtil';

// ─── Favorites state tracking ────────────────────────────────────────────────
const favoriteIds = ref([]);
const updateFavoriteIds = () => {
    favoriteIds.value = getFavoriteIds();
};

const toggleFavorite = (productId, event) => {
    if (event) event.stopPropagation();
    const wasFavorite = favoriteIds.value.includes(productId);
    toggleFavUtil(productId);
    if (wasFavorite) {
        toastStore.showToast('Đã xoá khỏi danh sách yêu thích', 'info');
    } else {
        toastStore.showToast('Đã thêm vào danh sách yêu thích', 'success');
    }
    updateFavoriteIds();
};

const isFavorite = (productId) => favoriteIds.value.includes(productId);

// ─── Image path resolving ────────────────────────────────────────────────────
const isAbsoluteUrl = (v) =>
    typeof v !== 'string' || /^(https?:)?\/\//i.test(v) || v.startsWith('data:') || v.startsWith('blob:') || v.startsWith('/');

const resolveImg = (v) => {
    if (!v) return '';
    if (typeof v !== 'string') return v;
    if (isAbsoluteUrl(v)) return v;
    return dichVuFile.layUrlFile(v.replace(/^\/+/, ''));
};

const formatPrice = (v) => {
    if (!v && v !== 0) return '';
    return new Intl.NumberFormat('vi-VN').format(v) + ' ₫';
};

// ─── Products Tab selection & Mock fallbacks ──────────────────────────────────
const activeTab = ref('MỚI NHẤT');

const newestProducts = computed(() => {
    if (featuredVariants.value?.length) {
        return featuredVariants.value;
    }
    if (landingProducts.value?.length) {
        return landingProducts.value;
    }
    return [];
});

const categoryCardImages = computed(() => {
    const list = featuredVariants.value || landingProducts.value || [];

    const getValidShoeImg = (item, defaultImg) => {
        if (!item) return defaultImg;
        let raw = item.hinhAnh ?? item.imageUrl ?? item.hinhAnhDaiDien ?? item.urlHinhAnh ?? item.sanPham?.hinhAnh;
        if (!raw && item.variants && item.variants.length > 0) {
            const v = item.variants[0];
            raw = v.hinhAnh || (v.images && v.images.length > 0 ? v.images[0].duongDanAnh || v.images[0].hinhAnh : null);
        }
        const resolved = resolveImg(raw);
        return resolved || defaultImg;
    };

    const runningShoe = list.find((p) => {
        const purpose = (p.tenMucDichChay || p.mucDichChay?.ten || '').toLowerCase();
        return purpose.includes('chạy') || purpose.includes('đường dài') || purpose.includes('chay');
    });

    const trainingShoe = list.find((p) => {
        const purpose = (p.tenMucDichChay || p.mucDichChay?.ten || '').toLowerCase();
        return purpose.includes('tập') || purpose.includes('luyện') || purpose.includes('tap');
    });

    const speedShoe = list.find((p) => {
        const purpose = (p.tenMucDichChay || p.mucDichChay?.ten || '').toLowerCase();
        return purpose.includes('tốc độ') || purpose.includes('toc do') || purpose.includes('speed');
    });

    return {
        running: getValidShoeImg(runningShoe, shoe1Img),
        training: getValidShoeImg(trainingShoe, shoe2Img),
        speed: getValidShoeImg(speedShoe, shoe3Img)
    };
});

const mapToCard = (item, type = 'NEW') => {
    let raw = item.hinhAnh ?? item.imageUrl ?? item.hinhAnhDaiDien ?? item.urlHinhAnh ?? item.sanPham?.hinhAnh;
    if (!raw && item.images && item.images.length > 0) {
        raw = item.images[0]?.duongDanAnh || item.images[0]?.hinhAnh || item.images[0];
    }
    if (!raw && item.variants && item.variants.length > 0) {
        const v = item.variants[0];
        raw = v.hinhAnh || (v.images && v.images.length > 0 ? v.images[0].duongDanAnh || v.images[0].hinhAnh : null);
    }
    if (!raw && item.raw) {
        raw = item.raw.hinhAnh || (item.raw.images && item.raw.images.length > 0 ? item.raw.images[0] : null);
    }
    let resolved = resolveImg(raw);
    if (!resolved) {
        resolved = DEFAULT_SHOE_IMAGE;
    }
    const phanTram = Number(item.phanTramGiam ?? item.giamGia ?? 0);
    const giaBan = Number(item.giaBan ?? item.giaBanThapNhat ?? item.gia ?? 0);
    const rawGiaGoc = item.giaGoc != null ? Number(item.giaGoc) : null;
    const giaGoc = rawGiaGoc && rawGiaGoc > giaBan ? rawGiaGoc : null;

    return {
        id: item.idSanPham ?? item.sanPhamId ?? item.id,
        tenSanPham: item.tenSanPham ?? item.title ?? item.sanPham?.tenSanPham ?? item.tenBienThe ?? 'Sản phẩm',
        tenThuongHieu: item.tenThuongHieu ?? item.subtitle ?? item.sanPham?.tenThuongHieu ?? item.thuongHieu?.ten ?? 'AeroStride',
        hinhAnh: resolved,
        giaBanThapNhat: giaBan,
        giaGoc: giaGoc,
        phanTramGiam: phanTram,
        tenDotGiamGia: item.tenDotGiamGia ?? null,
        badgeType: type
    };
};

const deduplicateProducts = (items) => {
    if (!Array.isArray(items)) return [];
    const seen = new Set();
    const result = [];
    for (const item of items) {
        if (!item) continue;
        const prodId = item.idSanPham || item.sanPhamId || item.raw?.id || item.id;
        const prodName = (item.tenSanPham || item.title || '').trim().toLowerCase();
        const key = prodId ? `id_${prodId}` : `name_${prodName}`;
        if (!seen.has(key)) {
            seen.add(key);
            result.push(item);
        }
    }
    return result;
};

const displayedProducts = computed(() => {
    let source = [];
    let badgeType = 'NEW';

    if (activeTab.value === 'MỚI NHẤT') {
        source = newestProducts.value;
        badgeType = 'NEW';
    } else if (activeTab.value === 'BÁN CHẠY') {
        source = topVariantsByQty.value || [];
        badgeType = 'HOT';
    } else if (activeTab.value === 'ĐANG GIẢM GIÁ') {
        source = discountedVariants.value?.length
            ? discountedVariants.value
            : (featuredVariants.value || []).filter((item) => Number(item.phanTramGiam ?? item.giamGia ?? 0) > 0);
        badgeType = 'DISCOUNT';
    }

    const uniqueProducts = deduplicateProducts(source);
    return uniqueProducts.slice(0, 8).map((item) => mapToCard(item, badgeType));
});

const noProductsMessage = computed(() => {
    if (activeTab.value === 'MỚI NHẤT') return 'Chưa có sản phẩm mới';
    if (activeTab.value === 'BÁN CHẠY') return 'Chưa có sản phẩm bán chạy';
    if (activeTab.value === 'ĐANG GIẢM GIÁ') return 'Chưa có sản phẩm giảm giá';
    return 'Chưa có sản phẩm nào';
});

const navigateToProduct = (id) => {
    if (!id) return;
    router.push(`/product/${id}`);
};

const scrollToCategories = () => {
    const el = document.getElementById('categories-section');
    if (el) {
        el.scrollIntoView({ behavior: 'smooth' });
    }
};
</script>

<template>
    <div class="landing-page-root">
        <transition name="preloader-fade">
            <Preloader v-if="isLoading" @finish="handlePreloaderFinish" />
        </transition>

        <!-- Global Header -->
        <MainHeader v-if="!isLoading" />

        <div class="main-content-flow" v-if="!isLoading">
            <div class="header-spacing"></div>

            <!-- 1. Hero Section (Mockup spec: 1360px width, E5F0FF background, rounded-28) -->
            <section class="section-container">
                <div class="hero-banner-container">
                    <div class="hero-content-left">
                        <span class="hero-tag">BỘ SƯU TẬP 2026</span>
                        <h1 class="hero-title">Bứt phá giới hạn.<br />Chinh phục mọi cung đường.</h1>
                        <p class="hero-subtitle">
                            Giày thể thao chính hãng, công nghệ đệm tối ưu và phong cách dành riêng cho từng bước chạy.
                        </p>
                        <div class="hero-actions-row">
                            <v-btn class="hero-btn-primary" @click="router.push(PATH.SHOES)"> KHÁM PHÁ NGAY </v-btn>
                            <v-btn class="hero-btn-secondary" @click="scrollToCategories"> XEM BỘ SƯU TẬP </v-btn>
                        </div>
                    </div>

                    <!-- Right Blue Badge Card with Shoe (3D model or image fallback) -->
                    <div class="hero-graphic-right">
                        <div class="blue-badge-card">
                            <span class="badge-brand-text">AERO<br />STRIDE</span>
                        </div>
                        <div class="shoe-model-container">
                            <model-viewer
                                v-if="viewerReady"
                                :src="heroProduct?.raw?.modelUrl || '/models/Shoe.glb'"
                                :poster="heroProduct?.imageUrl || defaultShoeImg"
                                alt="AeroStride 3D Shoe"
                                camera-controls
                                auto-rotate
                                rotation-per-second="30deg"
                                shadow-intensity="1"
                                environment-image="neutral"
                                class="shoe-element-3d"
                            ></model-viewer>
                            <img
                                v-else
                                :src="heroProduct?.imageUrl || defaultShoeImg"
                                alt="AeroStride Shoe"
                                class="fallback-shoe-img"
                            />
                        </div>
                    </div>
                </div>
            </section>

            <!-- 2. Features Bar (Mockup specifications) -->
            <section class="section-container py-8">
                <div class="features-grid-bar">
                    <div class="feature-item">
                        <div class="feature-icon">
                            <v-icon color="#2962ff" size="20">mdi-shield-check-outline</v-icon>
                        </div>
                        <div class="feature-text">
                            <span class="feature-title">100% chính hãng</span>
                            <span class="feature-desc">Cam kết nguồn gốc</span>
                        </div>
                    </div>
                    <div class="feature-item">
                        <div class="feature-icon">
                            <v-icon color="#2962ff" size="20">mdi-lightning-bolt-outline</v-icon>
                        </div>
                        <div class="feature-text">
                            <span class="feature-title">Giao hàng nhanh</span>
                            <span class="feature-desc">Toàn quốc 2–4 ngày</span>
                        </div>
                    </div>
                    <div class="feature-item">
                        <div class="feature-icon">
                            <v-icon color="#2962ff" size="20">mdi-refresh</v-icon>
                        </div>
                        <div class="feature-text">
                            <span class="feature-title">Đổi trả 30 ngày</span>
                            <span class="feature-desc">Dễ dàng, miễn phí</span>
                        </div>
                    </div>
                    <div class="feature-item">
                        <div class="feature-icon">
                            <v-icon color="#2962ff" size="20">mdi-shield-lock-outline</v-icon>
                        </div>
                        <div class="feature-text">
                            <span class="feature-title">Thanh toán an toàn</span>
                            <span class="feature-desc">Bảo mật tuyệt đối</span>
                        </div>
                    </div>
                </div>
            </section>

            <!-- FLASH SALE GIỜ VÀNG COUNTDOWN -->
            <FlashSaleSection />

            <!-- 3. Categories Section ("Mua sắm theo nhu cầu") -->
            <section id="categories-section" class="section-container py-12">
                <div class="section-heading-row mb-6">
                    <h2 class="section-heading">Mua sắm theo nhu cầu</h2>
                    <router-link :to="PATH.SHOES" class="heading-see-all-link">Xem tất cả →</router-link>
                </div>

                <div class="categories-grid-row">
                    <!-- Category 1: CHẠY BỘ -->
                    <div class="category-card card-running" @click="router.push({ path: PATH.SHOES, query: { mucDichChayId: 'md1' } })">
                        <img :src="shoe1Img" alt="Chạy bộ" class="category-shoe-img" />
                        <div class="category-info">
                            <h3 class="category-title">CHẠY BỘ</h3>
                            <span class="category-desc">Hiệu suất tối đa</span>
                        </div>
                        <div class="category-glow"></div>
                    </div>

                    <!-- Category 2: TẬP LUYỆN -->
                    <div class="category-card card-training" @click="router.push({ path: PATH.SHOES, query: { mucDichChayId: 'md3' } })">
                        <img :src="shoe2Img" alt="Tập luyện" class="category-shoe-img" />
                        <div class="category-info">
                            <h3 class="category-title">TẬP LUYỆN</h3>
                            <span class="category-desc">Ổn định từng chuyển động</span>
                        </div>
                        <div class="category-glow"></div>
                    </div>

                    <!-- Category 3: CHẠY TỐC ĐỘ -->
                    <div class="category-card card-speed" @click="router.push({ path: PATH.SHOES, query: { mucDichChayId: 'md2' } })">
                        <img :src="shoe3Img" alt="Chạy tốc độ" class="category-shoe-img" />
                        <div class="category-info">
                            <h3 class="category-title">CHẠY TỐC ĐỘ</h3>
                            <span class="category-desc">Bứt phá giới hạn</span>
                        </div>
                        <div class="category-glow"></div>
                    </div>
                </div>
            </section>

            <!-- 4. Products Section ("Sản phẩm nổi bật") -->
            <section class="section-container py-12">
                <div class="section-heading-row mb-6">
                    <h2 class="section-heading">Sản phẩm nổi bật</h2>
                    <div class="product-filters-tabs">
                        <span class="filter-tab-item" :class="{ active: activeTab === 'MỚI NHẤT' }" @click="activeTab = 'MỚI NHẤT'"
                            >MỚI NHẤT</span
                        >
                        <span class="filter-tab-item" :class="{ active: activeTab === 'BÁN CHẠY' }" @click="activeTab = 'BÁN CHẠY'"
                            >BÁN CHẠY</span
                        >
                        <span
                            class="filter-tab-item"
                            :class="{ active: activeTab === 'ĐANG GIẢM GIÁ' }"
                            @click="activeTab = 'ĐANG GIẢM GIÁ'"
                            >ĐANG GIẢM GIÁ</span
                        >
                    </div>
                </div>

                <!-- Products Grid -->
                <div v-if="displayedProducts.length > 0" class="products-grid-row">
                    <div
                        v-for="product in displayedProducts"
                        :key="product.id"
                        class="product-card-custom"
                        @click="navigateToProduct(product.id)"
                    >
                        <!-- Image Box with Light Blue Background -->
                        <div class="card-image-box">
                            <img
                                :src="product.hinhAnh || DEFAULT_SHOE_IMAGE"
                                :alt="product.tenSanPham"
                                class="product-card-img"
                                referrerpolicy="no-referrer"
                                @error="handleImageError"
                            />

                            <!-- Dynamic Badge per Tab / Discount Status -->
                            <div v-if="product.phanTramGiam > 0" class="sale-tag-badge badge-discount">
                                -{{ Math.round(product.phanTramGiam) }}%
                            </div>
                            <div v-else-if="product.badgeType === 'NEW'" class="sale-tag-badge badge-new">
                                ✨ MỚI
                            </div>
                            <div v-else-if="product.badgeType === 'HOT'" class="sale-tag-badge badge-hot">
                                🔥 BÁN CHẠY
                            </div>
                        </div>

                        <!-- Info Section -->
                        <div class="card-details-box">
                            <div class="brand-favorite-row">
                                <div class="d-flex align-center" style="gap: 6px">
                                    <span class="product-brand-tag">{{ product.tenThuongHieu }}</span>
                                    <span v-if="product.tenDotGiamGia" class="category-sub-chip chip-discount">{{ product.tenDotGiamGia }}</span>
                                    <span v-else-if="product.phanTramGiam > 0" class="category-sub-chip chip-discount">Giảm giá</span>
                                    <span v-else-if="product.badgeType === 'NEW'" class="category-sub-chip chip-new">Mới về</span>
                                    <span v-else-if="product.badgeType === 'HOT'" class="category-sub-chip chip-hot">Bán chạy</span>
                                </div>
                                <div class="favorite-heart-btn" @click.stop="(e) => toggleFavorite(product.id, e)">
                                    <v-icon :color="isFavorite(product.id) ? 'red' : 'grey-darken-1'" size="22">
                                        {{ isFavorite(product.id) ? 'mdi-heart' : 'mdi-heart-outline' }}
                                    </v-icon>
                                </div>
                            </div>
                            <h3 class="product-name-title">{{ product.tenSanPham }}</h3>
                            <div class="price-section-row">
                                <span class="current-price-text" :class="{ 'text-discount-red': product.phanTramGiam > 0 }">
                                    {{ formatPrice(product.giaBanThapNhat) }}
                                </span>
                                <span v-if="product.phanTramGiam > 0 && product.giaGoc" class="old-price-text">
                                    {{ formatPrice(product.giaGoc) }}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
                <div v-else class="no-products-placeholder">
                    <v-icon size="48" color="grey-lighten-1" class="mb-2">mdi-shoe-sneaker</v-icon>
                    <p class="placeholder-text">{{ noProductsMessage }}</p>
                </div>
            </section>

            <!-- 5. Footer (Mockup specification) -->
            <footer class="footer-custom">
                <span class="footer-text-content">
                    AEROSTRIDE &nbsp;&bull;&nbsp; Giày thể thao chính hãng &bull; Hotline 1900 6868 &bull; support@aerostride.vn
                </span>
            </footer>
        </div>

        <!-- Customer Chat Overlay -->
        <CustomerChat v-if="!isLoading" />
    </div>
</template>

<style scoped lang="scss">
.landing-page-root {
    background: #f9fafc;
    min-height: 100vh;
    font-family: 'Inter', sans-serif;
    color: #0a1329;
    overflow-x: hidden;
}

.main-content-flow {
    display: flex;
    flex-direction: column;
    width: 100%;
}

.header-spacing {
    height: 120px; /* 36px announcement + 84px header */

    @media (max-width: 768px) {
        height: 96px; /* announcement (auto ~32px) + 64px navbar */
    }

    @media (max-width: 480px) {
        height: 100px;
    }
}

/* Sections alignment */
.section-container {
    width: 100%;
    max-width: 1440px;
    margin: 0 auto;
    padding: 0 40px;
}

/* 1. Hero Banner Component */
.hero-banner-container {
    background: #e5f0ff;
    border-radius: 28px;
    width: 100%;
    height: 430px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px 0 54px;
    position: relative;
    overflow: hidden;
}

.hero-content-left {
    max-width: 620px;
    z-index: 2;
}

.hero-tag {
    font-size: 14px;
    font-weight: 700;
    color: #2962ff;
    letter-spacing: 1px;
    display: block;
    margin-bottom: 12px;
}

.hero-title {
    font-family: 'Outfit', sans-serif;
    font-size: 48px;
    font-weight: 700;
    line-height: 1.2;
    color: #0a1329;
    margin-bottom: 16px;
    letter-spacing: -0.5px;
}

.hero-subtitle {
    font-size: 17px;
    font-weight: 400;
    line-height: 1.5;
    color: #637085;
    margin-bottom: 32px;
}

.hero-actions-row {
    display: flex;
    align-items: center;
    gap: 16px;
}

.hero-btn-primary {
    background: #2962ff !important;
    color: #ffffff !important;
    font-weight: 600;
    font-size: 14px;
    border-radius: 24px;
    height: 48px !important;
    padding: 0 32px !important;
    box-shadow: 0 10px 20px rgba(41, 98, 255, 0.15);
    letter-spacing: 0.5px;
    text-transform: none;
    transition:
        transform 0.2s ease,
        box-shadow 0.2s ease;

    &:hover {
        transform: translateY(-2px);
        box-shadow: 0 12px 24px rgba(41, 98, 255, 0.25);
    }
}

.hero-btn-secondary {
    background: #ffffff !important;
    color: #0a1329 !important;
    font-weight: 600;
    font-size: 14px;
    border-radius: 24px;
    height: 48px !important;
    padding: 0 32px !important;
    border: 1px solid rgba(0, 0, 0, 0.05) !important;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.03);
    letter-spacing: 0.5px;
    text-transform: none;
    transition:
        transform 0.2s ease,
        background-color 0.2s;

    &:hover {
        transform: translateY(-2px);
        background: #f8fafc !important;
    }
}

.hero-graphic-right {
    position: relative;
    width: 600px;
    height: 380px;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 2;
}

.blue-badge-card {
    background: #2962ff;
    border-radius: 28px;
    width: 600px;
    height: 380px;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    padding-left: 35px;
    box-shadow: 0 15px 35px rgba(41, 98, 255, 0.2);
}

.badge-brand-text {
    font-family: 'Outfit', sans-serif;
    font-size: 48px;
    font-weight: 700;
    color: #ffffff;
    line-height: 1.1;
    letter-spacing: 2px;
}

.shoe-model-container {
    position: absolute;
    width: 440px;
    height: 300px;
    top: 50%;
    left: auto;
    right: -40px;
    transform: translateY(-50%);
    pointer-events: auto;
}

.shoe-element-3d,
.fallback-shoe-img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    outline: none;
    filter: drop-shadow(0 20px 30px rgba(0, 0, 0, 0.15));
}

/* 2. Features Bar */
.features-grid-bar {
    width: 100%;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    border-bottom: 1px solid #e5f0ff;
    padding-bottom: 24px;
}

.feature-item {
    display: flex;
    align-items: center;
    gap: 16px;
}

.feature-icon {
    font-size: 26px;
    font-weight: 700;
    color: #2962ff;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(41, 98, 255, 0.08);
    border-radius: 50%;
}

.feature-text {
    display: flex;
    flex-direction: column;
}

.feature-title {
    font-size: 14px;
    font-weight: 600;
    color: #0a1329;
}

.feature-desc {
    font-size: 12px;
    color: #637085;
    margin-top: 2px;
}

/* Heading structures */
.section-heading-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.section-heading {
    font-family: 'Outfit', sans-serif;
    font-size: 30px;
    font-weight: 700;
    color: #0a1329;
}

.heading-see-all-link {
    color: #2962ff;
    font-size: 14px;
    font-weight: 600;
    text-decoration: none;
    transition: opacity 0.2s;

    &:hover {
        opacity: 0.8;
    }
}

/* 3. Categories Grid */
.categories-grid-row {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 30px;
}

.category-card {
    height: 220px;
    border-radius: 24px;
    padding: 28px;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    position: relative;
    overflow: hidden;
    cursor: pointer;
    transition:
        transform 0.4s cubic-bezier(0.16, 1, 0.3, 1),
        box-shadow 0.4s ease;

    .category-info {
        z-index: 2;
        color: #ffffff;
    }

    .category-title {
        font-family: 'Outfit', sans-serif;
        font-size: 24px;
        font-weight: 700;
        letter-spacing: 0.5px;
    }

    .category-desc {
        font-size: 13px;
        font-weight: 400;
        opacity: 0.85;
        margin-top: 4px;
        display: block;
    }

    .category-glow {
        position: absolute;
        inset: 0;
        background: linear-gradient(to top, rgba(0, 0, 0, 0.4) 0%, transparent 60%);
        z-index: 1;
        transition: opacity 0.3s;
    }

    .category-shoe-img {
        position: absolute;
        top: -15px;
        right: -25px;
        width: 220px;
        height: 175px;
        object-fit: cover;
        border-radius: 20px;
        transform: rotate(-12deg) scale(0.95);
        transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.4s ease;
        filter: drop-shadow(0 12px 25px rgba(0, 0, 0, 0.3));
        z-index: 1;
        opacity: 0.92;
    }

    &:hover {
        transform: translateY(-8px);
        box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);

        .category-shoe-img {
            transform: rotate(-5deg) scale(1.08) translateY(-6px);
            opacity: 1;
            filter: drop-shadow(0 18px 30px rgba(0, 0, 0, 0.4));
        }

        .category-glow {
            background: linear-gradient(to top, rgba(0, 0, 0, 0.5) 0%, rgba(255, 255, 255, 0.05) 100%);
        }
    }
}

.card-running {
    background: #2962ff;
}

.card-training {
    background: #12ad8f;
}

.card-speed {
    background: #8c40f5;
}

/* 4. Products Tabbed Grid */
.product-filters-tabs {
    display: flex;
    align-items: center;
    gap: 24px;
}

.filter-tab-item {
    font-size: 13px;
    font-weight: 600;
    color: #637085;
    cursor: pointer;
    padding: 6px 0;
    position: relative;
    transition: color 0.2s;

    &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 0;
        width: 100%;
        height: 2px;
        background: #2962ff;
        transform: scaleX(0);
        transition: transform 0.25s ease;
        transform-origin: center;
    }

    &.active {
        color: #2962ff;
        &::after {
            transform: scaleX(1);
        }
    }

    &:hover {
        color: #2962ff;
    }
}

.products-grid-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 30px;
}

.no-products-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 24px;
    background: #ffffff;
    border-radius: 20px;
    border: 1.5px dashed rgba(41, 98, 255, 0.15);
    width: 100%;
    text-align: center;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.01);

    .placeholder-text {
        font-size: 15px;
        color: #637085;
        font-weight: 600;
        margin-top: 12px;
    }
}

.product-card-custom {
    background: #ffffff;
    border-radius: 20px;
    padding: 12px;
    cursor: pointer;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
    border: 1px solid rgba(229, 235, 245, 0.5);
    transition:
        transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
        box-shadow 0.3s ease;

    &:hover {
        transform: translateY(-6px);
        box-shadow: 0 15px 30px rgba(41, 98, 255, 0.06);
        border-color: rgba(41, 98, 255, 0.15);

        .product-card-img {
            transform: scale(1.06) rotate(-2deg);
        }
    }
}

.card-image-box {
    background: #f0f5fc;
    border-radius: 16px;
    width: 100%;
    aspect-ratio: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;
}

.product-card-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
    filter: drop-shadow(0 8px 12px rgba(0, 0, 0, 0.08));
}

.sale-tag-badge {
    position: absolute;
    top: 12px;
    left: 12px;
    color: #ffffff;
    font-size: 11px;
    font-weight: 800;
    padding: 3px 8px;
    border-radius: 8px;
    letter-spacing: 0.3px;

    &.badge-discount {
        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
        box-shadow: 0 4px 10px rgba(239, 68, 68, 0.35);
    }

    &.badge-new {
        background: linear-gradient(135deg, #059669 0%, #10b981 100%);
        box-shadow: 0 4px 10px rgba(16, 185, 129, 0.35);
    }

    &.badge-hot {
        background: linear-gradient(135deg, #ea580c 0%, #f97316 100%);
        box-shadow: 0 4px 10px rgba(249, 115, 22, 0.35);
    }
}

.category-sub-chip {
    font-size: 9px;
    font-weight: 700;
    padding: 2px 6px;
    border-radius: 4px;
    text-transform: uppercase;
    letter-spacing: 0.3px;

    &.chip-discount {
        background: #fee2e2;
        color: #dc2626;
    }

    &.chip-new {
        background: #d1fae5;
        color: #059669;
    }

    &.chip-hot {
        background: #ffedd5;
        color: #ea580c;
    }
}

.text-discount-red {
    color: #dc2626 !important;
}

.card-details-box {
    padding: 12px 4px 4px 4px;
}

.brand-favorite-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;
}

.product-brand-tag {
    font-size: 11px;
    font-weight: 700;
    color: #2962ff;
    letter-spacing: 0.5px;
    text-transform: uppercase;
}

.favorite-heart-btn {
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    transition: background-color 0.2s;

    &:hover {
        background-color: rgba(239, 68, 68, 0.08);
    }
}

.product-name-title {
    font-size: 15px;
    font-weight: 600;
    color: #0a1329;
    line-height: 1.35;
    height: 40px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin-bottom: 8px;
}

.price-section-row {
    display: flex;
    align-items: center;
    gap: 8px;
}

.current-price-text {
    font-size: 16px;
    font-weight: 700;
    color: #0a1329;
}

.old-price-text {
    font-size: 12px;
    color: #637085;
    text-decoration: line-through;
    font-weight: 500;
}

/* 5. Footer styling */
.footer-custom {
    background: #0a1329;
    height: 80px;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    margin-top: 60px;
    border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.footer-text-content {
    color: #ffffff;
    font-size: 13px;
    font-weight: 400;
    opacity: 0.85;
}

/* Page transitions */
.preloader-fade-leave-active {
    transition:
        opacity 0.6s cubic-bezier(1, 0, 0, 1),
        transform 0.6s cubic-bezier(1, 0, 0, 1);
}

.preloader-fade-leave-to {
    opacity: 0;
    transform: translateY(-100%);
}

/* ── Responsive ─────────────────────────────────────────────────── */

/* Tablet landscape: hero stacks, 3-col products */
@media (max-width: 1200px) {
    .products-grid-row { grid-template-columns: repeat(3, 1fr); gap: 20px; }
    .hero-banner-container {
        height: auto;
        flex-direction: column;
        padding: 32px 28px 24px;
        gap: 20px;
    }
    .hero-content-left { max-width: 100%; text-align: center; }
    .hero-actions-row { justify-content: center; }
    .hero-title { font-size: 38px; }
    .hero-graphic-right { width: 100%; height: 220px; }
    .blue-badge-card { width: 100%; height: 220px; justify-content: flex-start; padding-left: 28px; }
    .badge-brand-text { font-size: 38px; }
    .shoe-model-container { width: 320px; height: 210px; right: -10px; left: auto; transform: translateY(-50%); }
}

/* Tablet portrait */
@media (max-width: 960px) {
    .section-container { padding: 0 20px; }
    .features-grid-bar { grid-template-columns: repeat(2, 1fr); gap: 16px; padding-bottom: 20px; }
    .feature-icon { width: 34px; height: 34px; font-size: 18px; }
    .feature-title { font-size: 13px; }
    .feature-desc { font-size: 11px; }
    .feature-item { gap: 10px; }
    .categories-grid-row { grid-template-columns: repeat(2, 1fr); gap: 14px; }
    .category-card { height: 160px; padding: 18px; }
    .category-card .category-title { font-size: 18px; }
    .category-card .category-desc { font-size: 12px; }
    .category-card .category-shoe-img { width: 150px; height: 120px; }
    .products-grid-row { grid-template-columns: repeat(2, 1fr); gap: 14px; }
    .section-heading { font-size: 22px; }
    .section-heading-row { flex-direction: column; align-items: flex-start; gap: 10px; }
    .product-filters-tabs { gap: 20px; }
    .product-card-custom { padding: 10px; }
    .card-image-box { border-radius: 12px; }
    .card-details-box { padding: 10px 4px 4px; }
    .product-name-title { font-size: 13px; height: 36px; }
    .current-price-text { font-size: 14px; }
}

/* Mobile */
@media (max-width: 600px) {
    .section-container { padding: 0 14px; }

    /* Hero: compact layout */
    .hero-banner-container { padding: 22px 18px 18px; border-radius: 18px; gap: 16px; }
    .hero-tag { font-size: 11px; margin-bottom: 8px; }
    .hero-title { font-size: 22px; line-height: 1.25; margin-bottom: 10px; }
    .hero-subtitle { font-size: 13px; margin-bottom: 16px; line-height: 1.5; }
    .hero-actions-row { flex-direction: row; gap: 8px; justify-content: center; }
    .hero-btn-primary,
    .hero-btn-secondary {
        height: 38px !important;
        padding: 0 16px !important;
        font-size: 12px;
        flex: 1;
        max-width: 160px;
    }
    /* Hero graphic: small strip */
    .hero-graphic-right { height: 140px; width: 100%; }
    .blue-badge-card { height: 140px; border-radius: 16px; justify-content: flex-start; padding-left: 20px; }
    .badge-brand-text { font-size: 24px; letter-spacing: 1px; }
    .shoe-model-container { width: 190px; height: 130px; right: -5px; left: auto; transform: translateY(-50%); }

    /* Features: 2 columns, very compact */
    .features-grid-bar { grid-template-columns: repeat(2, 1fr); gap: 10px 12px; padding-bottom: 16px; }
    .feature-item { gap: 8px; }
    .feature-icon { width: 28px; height: 28px; font-size: 14px; flex-shrink: 0; }
    .feature-title { font-size: 12px; }
    .feature-desc { font-size: 10px; }

    /* Categories: 1 col, shorter */
    .categories-grid-row { grid-template-columns: 1fr; gap: 10px; }
    .category-card { height: 120px; padding: 16px; border-radius: 16px; }
    .category-card .category-title { font-size: 16px; }
    .category-card .category-desc { font-size: 11px; }
    .category-card .category-shoe-img { width: 120px; height: 100px; top: -8px; right: -15px; }

    /* Section headings */
    .section-heading { font-size: 18px; }
    .heading-see-all-link { font-size: 12px; }
    .product-filters-tabs { gap: 14px; }
    .filter-tab-item { font-size: 11px; }

    /* Products: 2 columns, compact cards */
    .products-grid-row { grid-template-columns: repeat(2, 1fr); gap: 10px; }
    .product-card-custom { padding: 8px; border-radius: 14px; }
    .card-image-box { border-radius: 10px; }
    .card-details-box { padding: 8px 2px 2px; }
    .product-brand-tag { font-size: 10px; }
    .product-name-title { font-size: 12px; height: 32px; margin-bottom: 6px; }
    .current-price-text { font-size: 13px; }
    .old-price-text { font-size: 10px; }
    .favorite-heart-btn { width: 24px; height: 24px; }
    .sale-tag-badge { font-size: 9px; padding: 2px 6px; top: 8px; left: 8px; }

    /* Footer */
    .footer-custom { height: auto; padding: 16px 14px; margin-top: 32px; }
    .footer-text-content { font-size: 11px; line-height: 1.7; }
}

/* Very small phones */
@media (max-width: 380px) {
    .section-container { padding: 0 10px; }
    .hero-title { font-size: 19px; }
    .hero-btn-primary, .hero-btn-secondary { font-size: 11px; padding: 0 12px !important; }
    .hero-graphic-right { height: 120px; }
    .blue-badge-card { height: 120px; justify-content: flex-start; padding-left: 14px; }
    .badge-brand-text { font-size: 20px; }
    .shoe-model-container { width: 150px; height: 110px; right: -5px; left: auto; transform: translateY(-50%); }
    .products-grid-row { gap: 8px; }
    .product-name-title { font-size: 11px; }
}
</style>
