<script setup>

import '@google/model-viewer';
import { formatCurrency as formatPrice } from '@/utils/formatters';

const props = defineProps({
    active: Boolean,
    warm: Boolean,
    mouseX: Number,
    mouseY: Number,
    product: Object
});
</script>

<template>
    <section class="snap-section hero-section d-flex align-center">
        <template v-if="props.active || props.warm">
            <div class="hero-bg-animated"
                :style="{ transform: `translate(${props.mouseX * 0.5}px, ${props.mouseY * 0.5}px)` }"></div>
            <div class="hero-diagonal-band"></div>
            <v-container>
                <v-row align="center">
                    <v-col cols="12" lg="5" offset-lg="1" :class="{ 'reveal-container': true, active: props.active }"
                        class="position-relative ps-lg-8">
                        <h1 class="hero-title reveal-item delay-1">
                            {{ props.product?.tenSanPham || 'AeroStride' }}
                        </h1>
                        <p class="hero-subtitle reveal-item delay-2 text-grey-darken-3 mt-4">
                            {{ props.product?.tenThuongHieu || 'PREMIUM' }}
                        </p>
                        <div class="hero-price reveal-item delay-3 text-black mb-8 d-flex align-center flex-wrap gap-3">
                            <span>{{ props.product?.giaBanThapNhat ? formatPrice(props.product.giaBanThapNhat) : '1.200.000 ₫' }}</span>
                            <span v-if="props.product?.tongSoLuongTon != null" class="text-subtitle-1 font-weight-bold px-3 py-1 rounded-pill"
                                  :style="props.product.tongSoLuongTon > 0 ? 'background: rgba(46, 125, 50, 0.15); color: #2e7d32; font-size: 0.95rem;' : 'background: rgba(198, 40, 40, 0.15); color: #c62828; font-size: 0.95rem;'">
                                {{ props.product.tongSoLuongTon > 0 ? `Còn ${props.product.tongSoLuongTon} sản phẩm` : 'Hết hàng' }}
                            </span>
                        </div>
                        <v-btn size="x-large" rounded="xl" class="px-12 hero-btn reveal-item delay-4"
                            :to="props.product?.id ? `/product/${props.product.id}` : '/shoes'">
                            KHÁM PHÁ NGAY
                        </v-btn>
                    </v-col>
                    <v-col cols="12" lg="6" class="text-center reveal-container" :class="{ active: props.active }">
                        <div class="hero-shoe-wrapper reveal-item delay-4">
                            <model-viewer :src="props.product?.modelUrl || '/models/Shoe.glb'"
                                :poster="props.product?.imageUrl || '/assets/images/products/1.jpg'"
                                alt="AeroStride 3D Shoe" camera-controls auto-rotate rotation-per-second="30deg"
                                shadow-intensity="1" environment-image="neutral" class="the-shoe mx-auto hero-shoe-main"
                                style="width: 100%; max-width: 600px; height: 400px; display: block; outline: none;"></model-viewer>
                        </div>
                    </v-col>
                </v-row>
            </v-container>
        </template>
    </section>
</template>

<style scoped lang="scss">
@import '@/scss/pages/landing/_hero-section.scss';
</style>
