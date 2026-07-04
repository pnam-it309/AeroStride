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
</style>
