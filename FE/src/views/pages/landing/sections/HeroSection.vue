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
            <div class="hero-bg-animated" :style="{ transform: `translate(${props.mouseX * 0.5}px, ${props.mouseY * 0.5}px)` }"></div>
            <v-container>
                <v-row align="center">
                    <v-col cols="12" lg="6" :class="{ 'reveal-container': true, active: props.active }">
                        <h1 class="hero-title reveal-item delay-1 text-black">
                            {{ props.product?.tenSanPham || 'AeroStride' }}
                        </h1>
                        <p class="hero-subtitle reveal-item delay-2 text-grey-darken-3">
                            {{ props.product?.tenThuongHieu || 'PREMIUM' }}
                        </p>
                        <div class="hero-price reveal-item delay-3 text-black mb-8">
                            {{ props.product?.giaBanThapNhat ? formatPrice(props.product.giaBanThapNhat) : '1.200.000 ₫' }}
                        </div>
                        <v-btn
                            size="x-large"
                            color="blue-darken-4"
                            rounded="xl"
                            class="px-12 elevation-10 reveal-item delay-4"
                            :to="props.product?.id ? `/product/${props.product.id}` : '/shoes'"
                        >
                            KHÁM PHÁ NGAY
                        </v-btn>
                    </v-col>
                    <v-col cols="12" lg="6" class="text-center reveal-container" :class="{ active: props.active }">
                        <div class="hero-shoe-wrapper reveal-item delay-4">
                            <img
                                :src="props.product?.imageUrl || '/assets/images/products/1.jpg'"
                                alt="AeroStride Shoe"
                                class="the-shoe mx-auto hero-shoe-main"
                                style="width: 100%; max-width: 600px; height: auto; object-fit: contain;"
                            />
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
