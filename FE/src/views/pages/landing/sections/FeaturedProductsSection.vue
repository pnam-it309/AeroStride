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
                                        class="premium-product-card"
                                        :style="{ transitionDelay: `${0.05 * index}s` }"
                                    >
                                        <div class="card-image-wrap">
                                            <div class="premium-badges">
                                                <div class="badge-featured">HOT</div>
                                                <div v-if="variant.phanTramGiam > 0" class="badge-sale">
                                                    -{{ variant.phanTramGiam }}%
                                                </div>
                                            </div>
                                            
                                            <div
                                                class="premium-placeholder"
                                                :style="{ backgroundColor: variant.maMauHex || '#2962FF' }"
                                                v-if="!variant.hinhAnh"
                                            >
                                                <v-icon size="40" color="white" style="opacity: 0.5;">mdi-shoe-sneaker</v-icon>
                                            </div>
                                            <img
                                                v-else
                                                :src="variant.hinhAnh"
                                                class="premium-img"
                                                :alt="variant.tenSanPham"
                                            />
                                            
                                            <!-- Hover Actions -->
                                            <div class="premium-hover-overlay">
                                                <div class="action-buttons">
                                                    <v-btn icon class="hover-btn eye-btn" size="40" :to="`/san-pham/${variant.idSanPham}`">
                                                        <v-icon size="20">mdi-eye-outline</v-icon>
                                                    </v-btn>
                                                    <v-btn icon class="hover-btn cart-btn" size="40" :to="`/san-pham/${variant.idSanPham}`">
                                                        <v-icon size="20">mdi-cart-outline</v-icon>
                                                    </v-btn>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="card-info-wrap">
                                            <div class="premium-brand">{{ variant.tenThuongHieu || 'AeroStride' }}</div>
                                            <h3 class="premium-title">{{ variant.tenSanPham }}</h3>
                                            
                                            <div class="premium-price-row mt-2 mb-3">
                                                <span class="price-main">{{ formatPrice(variant.giaBan) }}</span>
                                                <span v-if="variant.phanTramGiam > 0" class="price-old">
                                                    {{ formatPrice(variant.giaBan / (1 - variant.phanTramGiam / 100)) }}
                                                </span>
                                            </div>
                                            
                                            <div class="premium-attributes mt-auto">
                                                <div class="attr-pill" v-if="variant.tenMauSac">
                                                    <span class="color-dot" :style="{ backgroundColor: variant.maMauHex || '#000' }"></span>
                                                    {{ variant.tenMauSac }}
                                                </div>
                                                <div class="attr-pill" v-if="variant.tenKichThuoc">
                                                    <v-icon size="12" class="mr-1" color="grey-darken-1">mdi-ruler</v-icon>
                                                    {{ variant.tenKichThuoc }}
                                                </div>
                                            </div>
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
.featured-section {
    background-color: #f8fafc;
    position: relative;
    padding: 60px 0;
}

.bg-pattern {
    position: absolute;
    inset: 0;
    background-image: radial-gradient(#e2e8f0 1px, transparent 1px);
    background-size: 30px 30px;
    opacity: 0.5;
    z-index: 0;
}

.section-tag {
    display: inline-block;
    padding: 6px 16px;
    background: rgba(41, 98, 255, 0.1);
    color: #2962ff;
    border-radius: 20px;
    font-size: 0.8rem;
    font-weight: 800;
    letter-spacing: 1px;
    margin-bottom: 12px;
}

.section-title {
    font-size: 2.5rem;
    font-weight: 900;
    color: #1e293b;
    margin-bottom: 8px;
    line-height: 1.2;
}

.section-desc {
    font-size: 1.1rem;
    color: #64748b;
    max-width: 600px;
    margin: 0 auto;
}

.premium-product-card {
    background: #ffffff;
    border-radius: 20px;
    border: 1px solid rgba(226, 232, 240, 0.8);
    overflow: hidden;
    cursor: pointer;
    opacity: 0;
    transform: translateY(30px);
    transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
    display: flex;
    flex-direction: column;
    height: 100%;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);

    .active & {
        opacity: 1;
        transform: translateY(0);
    }

    &:hover {
        transform: translateY(-8px);
        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
        border-color: rgba(41, 98, 255, 0.3);

        .premium-img {
            transform: scale(1.08) rotate(-2deg);
        }
        .premium-hover-overlay {
            opacity: 1;
        }
        .action-buttons {
            transform: translateY(0);
        }
        .premium-title {
            color: #2962ff;
        }
    }
}

.card-image-wrap {
    position: relative;
    height: 250px;
    background: #f8fafc;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
}

.premium-img {
    width: 85%;
    height: 85%;
    object-fit: contain;
    transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
    filter: drop-shadow(0 15px 25px rgba(0, 0, 0, 0.15));
}

.premium-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0.9;
}

.premium-badges {
    position: absolute;
    top: 16px;
    left: 16px;
    z-index: 2;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.badge-featured, .badge-sale {
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 0.7rem;
    font-weight: 800;
    letter-spacing: 0.5px;
    text-transform: uppercase;
    box-shadow: 0 4px 10px rgba(0,0,0,0.1);
    backdrop-filter: blur(8px);
}

.badge-featured {
    background: rgba(41, 98, 255, 0.9);
    color: #fff;
}

.badge-sale {
    background: rgba(211, 47, 47, 0.9);
    color: #fff;
}

.premium-hover-overlay {
    position: absolute;
    inset: 0;
    background: rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(4px);
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.4s ease;
    z-index: 3;
}

.action-buttons {
    display: flex;
    gap: 12px;
    transform: translateY(20px);
    transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.hover-btn {
    background: #ffffff !important;
    color: #1e293b !important;
    box-shadow: 0 10px 25px rgba(0,0,0,0.1) !important;
    transition: all 0.3s ease !important;

    &:hover {
        background: #2962ff !important;
        color: #ffffff !important;
        transform: scale(1.1);
    }
}

.card-info-wrap {
    padding: 20px;
    display: flex;
    flex-direction: column;
    flex-grow: 1;
}

.premium-brand {
    font-size: 0.75rem;
    font-weight: 800;
    color: #2962ff;
    letter-spacing: 1px;
    text-transform: uppercase;
    margin-bottom: 4px;
}

.premium-title {
    font-size: 1.05rem;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    transition: color 0.3s ease;
}

.premium-price-row {
    display: flex;
    align-items: baseline;
    gap: 10px;
}

.price-main {
    font-size: 1.25rem;
    font-weight: 900;
    color: #1e293b;
}

.price-old {
    font-size: 0.85rem;
    font-weight: 600;
    color: #94a3b8;
    text-decoration: line-through;
}

.premium-attributes {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.attr-pill {
    display: flex;
    align-items: center;
    background: #f1f5f9;
    padding: 6px 12px;
    border-radius: 8px;
    font-size: 0.75rem;
    font-weight: 600;
    color: #475569;
    border: 1px solid #e2e8f0;
}

.color-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    margin-right: 6px;
    box-shadow: inset 0 2px 4px rgba(0,0,0,0.1);
    border: 1px solid rgba(0,0,0,0.05);
}

@media (max-width: 960px) {
    .section-title { font-size: 1.8rem; }
    .card-image-wrap { height: 180px; }
    .premium-title { font-size: 0.9rem; }
    .price-main { font-size: 1.1rem; }
}
</style>
