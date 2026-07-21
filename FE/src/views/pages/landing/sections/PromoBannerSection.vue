<script setup>
import { computed } from 'vue';
import { dichVuFile } from '@/services/core/dichVuFile';

const props = defineProps({
    active: Boolean,
    warm: Boolean,
    products: {
        type: Array,
        default: () => []
    }
});

const isAbsoluteUrl = (v) => /^(https?:)?\/\//i.test(v) || v?.startsWith('data:') || v?.startsWith('blob:');
const resolveImg = (v) => {
    if (!v) return '';
    if (isAbsoluteUrl(v)) return v;
    return dichVuFile.layUrlFile(v.replace(/^\/+/, ''));
};

const GRADIENTS = [
    'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)',
    'linear-gradient(135deg, #7f0000 0%, #b71c1c 50%, #d50000 100%)',
    'linear-gradient(135deg, #1b5e20 0%, #2e7d32 50%, #388e3c 100%)',
    'linear-gradient(135deg, #311b92 0%, #4527a0 50%, #512da8 100%)',
];
const ACCENTS = ['#64b5f6', '#ff5252', '#69f0ae', '#ce93d8'];

const displayBanners = computed(() => {
    if (!props.products || props.products.length === 0) return [];
    return props.products.slice(0, 3).map((p, i) => ({
        id: p.id,
        tenSanPham: p.tenSanPham || p.title,
        tenThuongHieu: p.tenThuongHieu || p.subtitle || 'PREMIUM',
        gia: p.giaBanThapNhat ?? p.raw?.giaBanThapNhat ?? p.gia ?? p.raw?.giaBan ?? null,
        soLuong: p.tongSoLuongTon ?? p.raw?.tongSoLuongTon ?? p.soLuong ?? p.raw?.soLuong ?? 0,
        hinhAnh: resolveImg(p.hinhAnh || p.imageUrl),
        link: `/product/${p.id}`,
        bg: GRADIENTS[i % GRADIENTS.length],
        accent: ACCENTS[i % ACCENTS.length]
    }));
});

const formatPrice = (v) => {
    if (!v && v !== 0) return 'Liên hệ';
    return new Intl.NumberFormat('vi-VN').format(v) + ' đ';
};
</script>

<template>
    <section class="snap-section promo-banner-section">
        <template v-if="props.active || props.warm">
            <v-container fluid class="fill-height pa-6">
                <v-row no-gutters class="fill-height align-center justify-center">
                    <v-col cols="12" class="px-4">
                        <!-- Header -->
                        <div class="section-header reveal-container mb-8" :class="{ active: props.active }">
                            <div class="d-flex align-center justify-space-between">
                                <div>
                                    <span class="section-tag reveal-item delay-1">Sản phẩm đặc sắc</span>
                                    <h2 class="section-title reveal-item delay-2">Nổi Bật Tháng Này</h2>
                                </div>
                                <v-btn variant="text" color="blue-darken-3" class="reveal-item delay-2 see-all-btn"
                                    to="/shoes">
                                    Xem tất cả <v-icon>mdi-arrow-right</v-icon>
                                </v-btn>
                            </div>
                        </div>

                        <!-- Banners từ DB -->
                        <div class="banner-grid reveal-container" :class="{ active: props.active }">
                            <v-row>
                                <v-col v-for="(banner, index) in displayBanners" :key="banner.id" cols="12" md="4"
                                    class="banner-col">
                                    <router-link :to="banner.link" class="banner-card"
                                        :style="{ background: banner.bg, '--accent': banner.accent, transitionDelay: `${0.1 * index}s` }">
                                        <div class="banner-tag">{{ banner.tenThuongHieu }}</div>
                                        <div class="banner-image-wrap" v-if="banner.hinhAnh">
                                            <img :src="banner.hinhAnh" :alt="banner.tenSanPham" class="banner-img" />
                                        </div>
                                        <div class="banner-icon" v-else>👟</div>
                                        <h3 class="banner-title">{{ banner.tenSanPham }}</h3>
                                        <div class="banner-meta d-flex align-center justify-space-between mt-2 mb-4">
                                            <div class="banner-price-box">
                                                <span class="price-label text-caption d-block"
                                                    style="color: rgba(255,255,255,0.7); font-size: 0.75rem;">Giá
                                                    từ</span>
                                                <span class="banner-price font-weight-bold"
                                                    style="font-size: 1.1rem; color: #fff;">
                                                    {{ banner.gia != null ? formatPrice(banner.gia) : 'Liên hệ' }}
                                                </span>
                                            </div>
                                            <div class="banner-stock-box text-right">
                                                <span class="stock-label text-caption d-block"
                                                    style="color: rgba(255,255,255,0.7); font-size: 0.75rem;">Số lượng
                                                    tồn</span>
                                                <span
                                                    class="banner-stock px-2 py-1 rounded-pill text-caption font-weight-bold d-inline-block"
                                                    :style="banner.soLuong > 0 ? 'background: rgba(46, 125, 50, 0.25); color: #81c784; border: 1px solid rgba(129, 199, 132, 0.4);' : 'background: rgba(198, 40, 40, 0.25); color: #e57373; border: 1px solid rgba(229, 115, 115, 0.4);'">
                                                    {{ banner.soLuong > 0 ? `${banner.soLuong} sản phẩm` : 'Hết hàng' }}
                                                </span>
                                            </div>
                                        </div>
                                        <div class="banner-cta">
                                            Khám phá ngay <v-icon size="16">mdi-arrow-right</v-icon>
                                        </div>
                                        <div class="banner-glow"></div>
                                    </router-link>
                                </v-col>
                            </v-row>
                        </div>

                        <!-- Empty state -->
                        <div v-if="displayBanners.length === 0" class="text-center py-10 text-grey">
                            <v-icon size="48" class="mb-2">mdi-image-off-outline</v-icon>
                            <p>Đang tải sản phẩm...</p>
                        </div>
                    </v-col>
                </v-row>
            </v-container>
        </template>
    </section>
</template>

<style scoped lang="scss">
.snap-section {
    min-height: 100vh;
    width: 100%;
    position: relative;
    display: flex;
    align-items: center;
    background: #f8fafc;
}


.see-all-btn {
    font-weight: 700;
    font-size: 0.85rem;
}

.banner-grid {
    max-width: 1400px;
    margin: 0 auto;
}

.banner-col {
    padding: 8px !important;
}

.banner-card {
    display: block;
    border-radius: 20px;
    padding: 28px 24px;
    min-height: 280px;
    text-decoration: none;
    position: relative;
    overflow: hidden;
    opacity: 0;
    transform: translateY(24px);
    transition: all 0.6s cubic-bezier(0.2, 1, 0.3, 1);
    cursor: pointer;

    .active & {
        opacity: 1;
        transform: translateY(0);
    }

    &:hover {
        transform: translateY(-6px) scale(1.01);
        box-shadow: 0 24px 60px rgba(0, 0, 0, 0.25);

        .banner-glow {
            opacity: 1;
        }

        .banner-cta {
            gap: 8px;
        }

        .banner-img {
            transform: scale(1.05);
        }
    }
}

.banner-tag {
    font-size: 0.65rem;
    font-weight: 900;
    letter-spacing: 3px;
    color: var(--accent);
    margin-bottom: 10px;
    padding: 3px 10px;
    border: 1px solid var(--accent);
    border-radius: 20px;
    display: inline-block;
    text-transform: uppercase;
}

.banner-image-wrap {
    width: 100%;
    height: 130px;
    overflow: hidden;
    border-radius: 10px;
    margin-bottom: 12px;
    background: rgba(255, 255, 255, 0.05);
}

.banner-img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    transition: transform 0.4s ease;
    padding: 8px;
}

.banner-icon {
    font-size: 2.5rem;
    margin-bottom: 10px;
    display: block;
}

.banner-title {
    font-size: 1.3rem;
    font-weight: 700;
    color: #ffffff;
    margin-bottom: 6px;
    letter-spacing: normal;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.banner-price {
    font-size: 0.9rem;
    font-weight: 700;
    color: rgba(255, 255, 255, 0.75);
    margin-bottom: 14px;
}

.banner-cta {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 0.82rem;
    font-weight: 800;
    color: var(--accent);
    transition: gap 0.2s ease;
}

.banner-glow {
    position: absolute;
    inset: 0;
    background: radial-gradient(ellipse at 80% 20%, rgba(255, 255, 255, 0.06) 0%, transparent 60%);
    opacity: 0;
    transition: opacity 0.4s ease;
    pointer-events: none;
}



@media (max-width: 960px) {
    .banner-card {
        min-height: 220px;
        padding: 20px 16px;
    }

    .banner-title {
        font-size: 1.1rem;
    }

    .section-title {
        font-size: 1.5rem;
    }

    .banner-image-wrap {
        height: 90px;
    }
}
</style>
