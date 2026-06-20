<script setup>
import { computed, defineProps } from 'vue';

const props = defineProps({
    active: Boolean,
    warm: Boolean,
    variants: {
        type: Array,
        default: () => []
    }
});

const formatPrice = (value) => {
    if (!value && value !== 0) return '';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
};

const displayVariants = computed(() => {
    if (!props.variants || props.variants.length === 0) {
        return Array.from({ length: 8 }, (_, i) => ({
            id: i,
            tenSanPham: 'AeroStride Prime',
            tenThuongHieu: 'PREMIUM',
            giaBan: 1200000 + i * 100000,
            tenMauSac: i % 2 === 0 ? 'Đen' : 'Trắng',
            tenKichThuoc: `${39 + i}`,
            hinhAnh: '',
            maMauHex: i % 2 === 0 ? '#1a1a2e' : '#f5f5f5',
            phanTramGiam: 0
        }));
    }
    return props.variants;
});
</script>

<template>
    <section class="snap-section featured-section d-flex align-center overflow-hidden">
        <template v-if="props.active || props.warm">
            <div class="bg-pattern"></div>
            <v-container fluid class="fill-height pa-0 position-relative z-10">
                <v-row no-gutters class="fill-height align-center justify-center">
                    <v-col cols="12" class="px-8 py-8">
                        <div class="section-header text-center mb-6 reveal-container" :class="{ active: props.active }">
                            <span class="section-tag reveal-item delay-1">SẢN PHẨM NỔI BẬT</span>
                            <h2 class="section-title reveal-item delay-2">Bộ Sưu Tập Đặc Sắc</h2>
                            <p class="section-desc reveal-item delay-3">Những thiết kế được yêu thích nhất, kết hợp giữa công nghệ và phong cách</p>
                        </div>

                        <div class="product-grid reveal-container" :class="{ active: props.active }">
                            <v-row>
                                <v-col
                                    v-for="(variant, index) in displayVariants"
                                    :key="variant.id"
                                    cols="6"
                                    md="3"
                                    lg="2"
                                    class="product-col"
                                >
                                    <div
                                        class="product-card"
                                        :style="{ transitionDelay: `${0.05 * index}s` }"
                                    >
                                        <div class="card-image-wrapper">
                                            <div
                                                class="color-swatch"
                                                :style="{ backgroundColor: variant.maMauHex || '#2962FF' }"
                                                v-if="!variant.hinhAnh"
                                            >
                                                <span class="color-label">{{ variant.tenMauSac }}</span>
                                            </div>
                                            <v-img
                                                v-else
                                                :src="variant.hinhAnh"
                                                class="product-image"
                                                contain
                                                loading="lazy"
                                            ></v-img>
                                            <div v-if="variant.phanTramGiam > 0" class="discount-badge">
                                                -{{ formatPrice(variant.phanTramGiam) }}
                                            </div>
                                        </div>
                                        <div class="card-body">
                                            <div class="brand-name">{{ variant.tenThuongHieu }}</div>
                                            <div class="product-name">{{ variant.tenSanPham }}</div>
                                            <div class="variant-info">
                                                <span class="color-name">{{ variant.tenMauSac }}</span>
                                                <span class="size-name">Size {{ variant.tenKichThuoc }}</span>
                                            </div>
                                            <div class="price-row">
                                                <span class="current-price">{{ formatPrice(variant.giaBan) }}</span>
                                            </div>
                                            <v-btn
                                                variant="outlined"
                                                color="blue-darken-4"
                                                size="small"
                                                rounded="pill"
                                                block
                                                class="mt-2 view-btn"
                                                :to="`/san-pham/${variant.idSanPham}`"
                                            >
                                                Xem chi tiết
                                            </v-btn>
                                        </div>
                                    </div>
                                </v-col>
                            </v-row>
                        </div>

                        <div class="text-center mt-8 reveal-container" :class="{ active: props.active }">
                            <v-btn
                                size="large"
                                color="blue-darken-4"
                                rounded="xl"
                                class="px-12 elevation-4 reveal-item delay-4"
                                to="/shoes"
                            >
                                XEM TẤT CẢ
                            </v-btn>
                        </div>
                    </v-col>
                </v-row>
            </v-container>
        </template>
    </section>
</template>

<style scoped lang="scss">
.snap-section {
    height: 100vh;
    width: 100%;
    position: relative;
    overflow-y: auto;
}

.bg-pattern {
    position: absolute;
    inset: 0;
    background:
        radial-gradient(ellipse at 20% 50%, rgba(41, 98, 255, 0.03) 0%, transparent 50%),
        radial-gradient(ellipse at 80% 50%, rgba(98, 0, 234, 0.03) 0%, transparent 50%);
    z-index: 0;
}

.z-10 {
    z-index: 10;
}

.section-header {
    .section-tag {
        font-size: 0.75rem;
        font-weight: 900;
        color: #2962ff;
        letter-spacing: 4px;
        display: block;
        margin-bottom: 8px;
    }
    .section-title {
        font-size: 2.5rem;
        font-weight: 950;
        color: #1e293b;
        letter-spacing: -1px;
    }
    .section-desc {
        font-size: 1rem;
        color: #64748b;
        max-width: 500px;
        margin: 0 auto;
    }
}

.product-grid {
    max-width: 1400px;
    margin: 0 auto;
}

.product-col {
    padding: 8px !important;
}

.product-card {
    background: #ffffff;
    border-radius: 16px;
    overflow: hidden;
    border: 1px solid rgba(0, 0, 0, 0.06);
    transition: all 0.4s cubic-bezier(0.2, 1, 0.3, 1);
    opacity: 0;
    transform: translateY(20px);

    .active & {
        opacity: 1;
        transform: translateY(0);
        transition-delay: inherit;
    }

    &:hover {
        transform: translateY(-6px);
        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
        border-color: rgba(41, 98, 255, 0.15);
    }
}

.card-image-wrapper {
    position: relative;
    width: 100%;
    padding-top: 100%;
    background: #f8fafc;
    overflow: hidden;
}

.color-swatch {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.4s ease;

    .product-card:hover & {
        transform: scale(1.05);
    }

    .color-label {
        color: rgba(255, 255, 255, 0.8);
        font-weight: 700;
        font-size: 0.85rem;
        letter-spacing: 2px;
        text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    }
}

.product-image {
    position: absolute;
    inset: 0;
    padding: 16px;
    transition: transform 0.4s ease;

    .product-card:hover & {
        transform: scale(1.08);
    }
}

.discount-badge {
    position: absolute;
    top: 8px;
    left: 8px;
    background: #d50000;
    color: white;
    font-size: 0.65rem;
    font-weight: 900;
    padding: 3px 8px;
    border-radius: 6px;
}

.card-body {
    padding: 12px;
}

.brand-name {
    font-size: 0.6rem;
    font-weight: 900;
    color: #2962ff;
    letter-spacing: 2px;
    margin-bottom: 2px;
    text-transform: uppercase;
}

.product-name {
    font-size: 0.85rem;
    font-weight: 800;
    color: #1e293b;
    margin-bottom: 4px;
    line-height: 1.2;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.variant-info {
    display: flex;
    gap: 8px;
    font-size: 0.7rem;
    color: #94a3b8;
    margin-bottom: 6px;

    .color-name,
    .size-name {
        font-weight: 600;
    }
}

.price-row {
    display: flex;
    align-items: center;
    gap: 6px;

    .current-price {
        font-size: 0.95rem;
        font-weight: 950;
        color: #1e293b;
    }
}

.view-btn {
    font-size: 0.7rem;
    font-weight: 800;
    letter-spacing: 0.5px;
    transition: all 0.2s ease;

    &:hover {
        background: #1e293b !important;
        color: white !important;
        border-color: #1e293b !important;
    }
}

/* Reveal Animation System */
.reveal-item {
    opacity: 0;
    transform: translateY(30px);
    transition: all 0.8s cubic-bezier(0.2, 1, 0.3, 1);
    will-change: transform, opacity;
}

.active .reveal-item {
    opacity: 1;
    transform: translateY(0);
}

.delay-1 {
    transition-delay: 0.1s;
}
.delay-2 {
    transition-delay: 0.2s;
}
.delay-3 {
    transition-delay: 0.3s;
}
.delay-4 {
    transition-delay: 0.5s;
}

@media (max-width: 960px) {
    .section-title {
        font-size: 1.8rem;
    }
    .product-col {
        padding: 4px !important;
    }
    .card-body {
        padding: 8px;
    }
    .product-name {
        font-size: 0.75rem;
    }
}
</style>
