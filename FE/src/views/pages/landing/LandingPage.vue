    <script setup>
    import { ref, onMounted, onUnmounted, watch, computed } from 'vue';
    import { useRouter } from 'vue-router';
    import { PATH } from '@/router/routePaths';
    import Preloader from '@/components/common/Preloader.vue';
    import CustomerChat from '@/components/shared/CustomerChat.vue';
    import MainHeader from '@/components/shared/MainHeader.vue';
    import { useLandingPage } from '@/composables/useLandingPage';
    import { useLandingCatalog } from '@/composables/useLandingCatalog';
    import { useSeoMeta } from '@/composables/useSeoMeta';
    import { useToastStore } from '@/stores/toastStore';
    import { dichVuFile } from '@/services/core/dichVuFile';

    const router = useRouter();
    const toastStore = useToastStore();

    const {
        activeSection,
        handleLogout,
        handlePreloaderFinish,
        isLoading,
        isLoggedIn,
        LANDING_SECTIONS,
        isSectionWarm,
        mouseX,
        mouseY
    } = useLandingPage();

    const { heroProduct, isCatalogLoading, howProducts, topVariantsByQty, problemProducts, landingProducts, featuredVariants } = useLandingCatalog(activeSection);

    // SEO Setup
    const { setSeoMeta } = useSeoMeta();
    onMounted(() => {
        setSeoMeta({
            title: 'Giày Thể Thao Chính Hãng - Mua Sắm Online',
            description: 'AeroStride - Cửa hàng giày thể thao chính hãng hàng đầu Việt Nam. Mua sắm giày Nike, Adidas, Puma với giá tốt nhất, giao hàng nhanh toàn quốc, đổi trả miễn phí 30 ngày.'
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

    // ─── Favorites state tracking ────────────────────────────────────────────────
    const favoriteIds = ref([]);
    const updateFavoriteIds = () => {
        favoriteIds.value = JSON.parse(localStorage.getItem('aerostride_favorites') || '[]');
    };

    const toggleFavorite = (productId, event) => {
        if (event) event.stopPropagation();
        let favorites = JSON.parse(localStorage.getItem('aerostride_favorites') || '[]');
        if (favorites.includes(productId)) {
            favorites = favorites.filter(id => id !== productId);
            localStorage.setItem('aerostride_favorites', JSON.stringify(favorites));
            toastStore.showToast('Đã xoá khỏi danh sách yêu thích', 'info');
        } else {
            favorites.push(productId);
            localStorage.setItem('aerostride_favorites', JSON.stringify(favorites));
            toastStore.showToast('Đã thêm vào danh sách yêu thích', 'success');
        }
        window.dispatchEvent(new Event('favorites-updated'));
        updateFavoriteIds();
    };

    const isFavorite = (productId) => favoriteIds.value.includes(productId);

    // ─── Image path resolving ────────────────────────────────────────────────────
    const isAbsoluteUrl = (v) => 
        typeof v !== 'string' || 
        /^(https?:)?\/\//i.test(v) || 
        v.startsWith('data:') || 
        v.startsWith('blob:') || 
        v.startsWith('/');

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

    const mapToCard = (item) => ({
    id:
        item.idSanPham ??
        item.sanPhamId ??
        item.id,

    tenSanPham:
        item.tenSanPham ??
        item.title ??
        item.sanPham?.tenSanPham ??
        item.tenBienThe ??
        'Sản phẩm',

    tenThuongHieu:
        item.tenThuongHieu ??
        item.subtitle ??
        item.sanPham?.tenThuongHieu ??
        item.thuongHieu?.ten ??
        'AeroStride',

    hinhAnh: resolveImg(
        item.hinhAnh ??
        item.imageUrl ??
        item.hinhAnhDaiDien ??
        item.urlHinhAnh ??
        item.sanPham?.hinhAnh
    ),

    giaBanThapNhat:
        item.giaBanThapNhat ??
        item.giaBan ??
        item.gia ??
        0,

    phanTramGiam: Number(
        item.phanTramGiam ??
        item.giamGia ??
        0
    )
});

    const newestProducts = computed(() => {
    if (landingProducts.value?.length) {
        return landingProducts.value;
    }

    return featuredVariants.value || [];
});

const displayedProducts = computed(() => {
    let source = [];

    if (activeTab.value === 'MỚI NHẤT') {
        source = newestProducts.value;
    } else if (activeTab.value === 'BÁN CHẠY') {
        source = topVariantsByQty.value || [];
    } else if (activeTab.value === 'ĐANG GIẢM GIÁ') {
        source = (featuredVariants.value || []).filter(
            item => Number(item.phanTramGiam ?? item.giamGia ?? 0) > 0
        );
    }

    return source.slice(0, 4).map(mapToCard);
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
                            <h1 class="hero-title">
                                Bứt phá giới hạn.<br/>Chinh phục mọi cung đường.
                            </h1>
                            <p class="hero-subtitle">
                                Giày thể thao chính hãng, công nghệ đệm tối ưu và phong cách dành riêng cho từng bước chạy.
                            </p>
                            <div class="hero-actions-row">
                                <v-btn class="hero-btn-primary" @click="router.push(PATH.SHOES)">
                                    KHÁM PHÁ NGAY
                                </v-btn>
                                <v-btn class="hero-btn-secondary" @click="scrollToCategories">
                                    XEM BỘ SƯU TẬP
                                </v-btn>
                            </div>
                        </div>

                        <!-- Right Blue Badge Card with Shoe (3D model or image fallback) -->
                        <div class="hero-graphic-right">
                            <div class="blue-badge-card">
                                <span class="badge-brand-text">AERO<br/>STRIDE</span>
                            </div>
                            <div class="shoe-model-container">
                                <model-viewer 
                                    v-if="viewerReady" 
                                    :src="heroProduct?.raw?.modelUrl || '/models/Shoe.glb'"
                                    :poster="heroProduct?.imageUrl || '/assets/images/products/1.jpg'"
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
                                    :src="heroProduct?.imageUrl || '/assets/images/products/1.jpg'" 
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
                            <span class="feature-icon">✓</span>
                            <div class="feature-text">
                                <span class="feature-title">100% chính hãng</span>
                                <span class="feature-desc">Cam kết nguồn gốc</span>
                            </div>
                        </div>
                        <div class="feature-item">
                            <span class="feature-icon">⚡</span>
                            <div class="feature-text">
                                <span class="feature-title">Giao hàng nhanh</span>
                                <span class="feature-desc">Toàn quốc 2–4 ngày</span>
                            </div>
                        </div>
                        <div class="feature-item">
                            <span class="feature-icon">↻</span>
                            <div class="feature-text">
                                <span class="feature-title">Đổi trả 30 ngày</span>
                                <span class="feature-desc">Dễ dàng, miễn phí</span>
                            </div>
                        </div>
                        <div class="feature-item">
                            <span class="feature-icon">♢</span>
                            <div class="feature-text">
                                <span class="feature-title">Thanh toán an toàn</span>
                                <span class="feature-desc">Bảo mật tuyệt đối</span>
                            </div>
                        </div>
                    </div>
                </section>

                <!-- 3. Categories Section ("Mua sắm theo nhu cầu") -->
                <section id="categories-section" class="section-container py-12">
                    <div class="section-heading-row mb-6">
                        <h2 class="section-heading">Mua sắm theo nhu cầu</h2>
                        <router-link :to="PATH.SHOES" class="heading-see-all-link">Xem tất cả →</router-link>
                    </div>

                    <div class="categories-grid-row">
                        <!-- Category 1: CHẠY BỘ -->
                        <div 
                            class="category-card card-running"
                            @click="router.push({ path: PATH.SHOES, query: { mucDichChayId: 'md1' } })"
                        >
                            <div class="category-info">
                                <h3 class="category-title">CHẠY BỘ</h3>
                                <span class="category-desc">Hiệu suất tối đa</span>
                            </div>
                            <div class="category-glow"></div>
                        </div>

                        <!-- Category 2: TẬP LUYỆN -->
                        <div 
                            class="category-card card-training"
                            @click="router.push({ path: PATH.SHOES, query: { mucDichChayId: 'md3' } })"
                        >
                            <div class="category-info">
                                <h3 class="category-title">TẬP LUYỆN</h3>
                                <span class="category-desc">Ổn định từng chuyển động</span>
                            </div>
                            <div class="category-glow"></div>
                        </div>

                        <!-- Category 3: CHẠY TỐC ĐỘ -->
                        <div 
                            class="category-card card-speed"
                            @click="router.push({ path: PATH.SHOES, query: { mucDichChayId: 'md2' } })"
                        >
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
                            <span 
                                class="filter-tab-item" 
                                :class="{ active: activeTab === 'MỚI NHẤT' }"
                                @click="activeTab = 'MỚI NHẤT'"
                            >MỚI NHẤT</span>
                            <span 
                                class="filter-tab-item" 
                                :class="{ active: activeTab === 'BÁN CHẠY' }"
                                @click="activeTab = 'BÁN CHẠY'"
                            >BÁN CHẠY</span>
                            <span 
                                class="filter-tab-item" 
                                :class="{ active: activeTab === 'ĐANG GIẢM GIÁ' }"
                                @click="activeTab = 'ĐANG GIẢM GIÁ'"
                            >ĐANG GIẢM GIÁ</span>
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
                                <img v-if="product.hinhAnh" :src="product.hinhAnh" :alt="product.tenSanPham" class="product-card-img" />
                                <v-icon v-else size="48" color="grey-lighten-2">mdi-shoe-sneaker</v-icon>

                                <!-- Sale percent badge -->
                                <div v-if="product.phanTramGiam > 0" class="sale-tag-badge">
                                    -{{ product.phanTramGiam }}%
                                </div>
                            </div>

                            <!-- Info Section -->
                            <div class="card-details-box">
                                <div class="brand-favorite-row">
                                    <span class="product-brand-tag">{{ product.tenThuongHieu }}</span>
                                    <div class="favorite-heart-btn" @click.stop="(e) => toggleFavorite(product.id, e)">
                                        <v-icon 
                                            :color="isFavorite(product.id) ? 'red' : 'grey-darken-1'"
                                            size="22"
                                        >
                                            {{ isFavorite(product.id) ? 'mdi-heart' : 'mdi-heart-outline' }}
                                        </v-icon>
                                    </div>
                                </div>
                                <h3 class="product-name-title">{{ product.tenSanPham }}</h3>
                                <div class="price-section-row">
                                    <span class="current-price-text">{{ formatPrice(product.giaBanThapNhat) }}</span>
                                    <span v-if="product.phanTramGiam > 0" class="old-price-text">
                                        {{ formatPrice(product.giaBanThapNhat / (1 - product.phanTramGiam / 100)) }}
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
        background: #F9FAFC;
        min-height: 100vh;
        font-family: 'Inter', sans-serif;
        color: #0A1329;
        overflow-x: hidden;
    }

    .main-content-flow {
        display: flex;
        flex-direction: column;
        width: 100%;
    }

    .header-spacing {
        height: 120px; /* 36px announcement + 84px header */
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
        background: #E5F0FF;
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
        color: #2962FF;
        letter-spacing: 1px;
        display: block;
        margin-bottom: 12px;
    }

    .hero-title {
        font-family: 'Outfit', sans-serif;
        font-size: 48px;
        font-weight: 700;
        line-height: 1.2;
        color: #0A1329;
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
        background: #2962FF !important;
        color: #ffffff !important;
        font-weight: 600;
        font-size: 14px;
        border-radius: 24px;
        height: 48px !important;
        padding: 0 32px !important;
        box-shadow: 0 10px 20px rgba(41, 98, 255, 0.15);
        letter-spacing: 0.5px;
        text-transform: none;
        transition: transform 0.2s ease, box-shadow 0.2s ease;

        &:hover {
            transform: translateY(-2px);
            box-shadow: 0 12px 24px rgba(41, 98, 255, 0.25);
        }
    }

    .hero-btn-secondary {
        background: #ffffff !important;
        color: #0A1329 !important;
        font-weight: 600;
        font-size: 14px;
        border-radius: 24px;
        height: 48px !important;
        padding: 0 32px !important;
        border: 1px solid rgba(0, 0, 0, 0.05) !important;
        box-shadow: 0 4px 10px rgba(0, 0, 0, 0.03);
        letter-spacing: 0.5px;
        text-transform: none;
        transition: transform 0.2s ease, background-color 0.2s;

        &:hover {
            transform: translateY(-2px);
            background: #F8FAFC !important;
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
        background: #2962FF;
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

    .shoe-element-3d, .fallback-shoe-img {
        width: 100%;
        height: 100%;
        object-fit: contain;
        outline: none;
        filter: drop-shadow(0 20px 30px rgba(0,0,0,0.15));
    }

    /* 2. Features Bar */
    .features-grid-bar {
        width: 100%;
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 20px;
        border-bottom: 1px solid #E5F0FF;
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
        color: #2962FF;
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
        color: #0A1329;
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
        color: #0A1329;
    }

    .heading-see-all-link {
        color: #2962FF;
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
        transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.4s ease;

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
            background: linear-gradient(to top, rgba(0,0,0,0.4) 0%, transparent 60%);
            z-index: 1;
            transition: opacity 0.3s;
        }

        &:hover {
            transform: translateY(-8px);
            box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
            
            .category-glow {
                background: linear-gradient(to top, rgba(0,0,0,0.5) 0%, rgba(255,255,255,0.05) 100%);
            }
        }
    }

    .card-running {
        background: #2962FF;
    }

    .card-training {
        background: #12AD8F;
    }

    .card-speed {
        background: #8C40F5;
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
            background: #2962FF;
            transform: scaleX(0);
            transition: transform 0.25s ease;
            transform-origin: center;
        }

        &.active {
            color: #2962FF;
            &::after {
                transform: scaleX(1);
            }
        }

        &:hover {
            color: #2962FF;
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
        box-shadow: 0 4px 15px rgba(0,0,0,0.01);

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
        box-shadow: 0 4px 15px rgba(0,0,0,0.02);
        border: 1px solid rgba(229, 235, 245, 0.5);
        transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s ease;

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
        background: #F0F5FC;
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
        background: #FF3D00;
        color: #ffffff;
        font-size: 10px;
        font-weight: 800;
        padding: 3px 8px;
        border-radius: 8px;
        box-shadow: 0 3px 6px rgba(255, 61, 0, 0.2);
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
        color: #2962FF;
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
        color: #0A1329;
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
        color: #0A1329;
    }

    .old-price-text {
        font-size: 12px;
        color: #637085;
        text-decoration: line-through;
        font-weight: 500;
    }

    /* 5. Footer styling */
    .footer-custom {
        background: #0A1329;
        height: 80px;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        margin-top: 60px;
        border-top: 1px solid rgba(255,255,255,0.05);
    }

    .footer-text-content {
        color: #ffffff;
        font-size: 13px;
        font-weight: 400;
        opacity: 0.85;
    }

    /* Page transitions */
    .preloader-fade-leave-active {
        transition: opacity 0.6s cubic-bezier(1, 0, 0, 1), transform 0.6s cubic-bezier(1, 0, 0, 1);
    }

    .preloader-fade-leave-to {
        opacity: 0;
        transform: translateY(-100%);
    }

    /* Responsiveness overrides */
    @media (max-width: 1200px) {
        .products-grid-row {
            grid-template-columns: repeat(3, 1fr);
        }
        .categories-grid-row {
            gap: 20px;
        }
        .hero-banner-container {
            height: auto;
            flex-direction: column;
            padding: 40px 30px;
            gap: 30px;
        }
        .hero-content-left {
            max-width: 100%;
            text-align: center;
        }
        .hero-actions-row {
            justify-content: center;
        }
        .hero-graphic-right {
            width: 100%;
            height: 300px;
        }
        .blue-badge-card {
            width: 100%;
            height: 250px;
            padding-left: 30px;
            justify-content: center;
            padding-left: 0;
        }
        .shoe-model-container {
            width: 400px;
            height: 280px;
            transform: translate(-50%, -55%);
        }
    }

    @media (max-width: 960px) {
        .features-grid-bar {
            grid-template-columns: repeat(2, 1fr);
            gap: 24px;
        }
        .categories-grid-row {
            grid-template-columns: 1fr;
        }
        .products-grid-row {
            grid-template-columns: repeat(2, 1fr);
        }
        .section-heading-row {
            flex-direction: column;
            align-items: flex-start;
            gap: 12px;
        }
        .product-filters-tabs {
            width: 100%;
            justify-content: space-between;
        }
    }

    @media (max-width: 600px) {
        .section-container {
            padding: 0 16px;
        }
        .features-grid-bar {
            grid-template-columns: 1fr;
        }
        .hero-title {
            font-size: 32px;
        }
        .hero-subtitle {
            font-size: 15px;
        }
        .products-grid-row {
            grid-template-columns: 1fr;
        }
    }
    </style>
