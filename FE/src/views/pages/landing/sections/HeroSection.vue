<script setup>

import { ref, watch } from 'vue';
import { formatCurrency as formatPrice } from '@/utils/formatters';

const props = defineProps({
    active: Boolean,
    warm: Boolean,
    mouseX: Number,
    mouseY: Number,
    product: Object
});

// @google/model-viewer drags in three.js - roughly 1 MB of the landing chunk.
// LandingPage imports this section statically (every other section is async),
// so a top-level `import '@google/model-viewer'` put that megabyte on the
// critical path of the home page. Loading it on demand keeps the entry lean;
// the <img> below holds the exact same box until the viewer is ready, so the
// swap costs no layout shift.
const viewerReady = ref(false);
let loadStarted = false;

const loadViewer = async () => {
    if (loadStarted) return;
    loadStarted = true;
    try {
        await import('@google/model-viewer');
        viewerReady.value = true;
    } catch (error) {
        // Leave the poster image in place - a failed 3D viewer must not blank
        // out the hero. Reset the flag so a later activation can retry.
        loadStarted = false;
        console.error('[HeroSection] model-viewer failed to load:', error);
    }
};

// Same condition that gates the section's own markup, so nothing downloads
// until this slide is actually reached (or pre-warmed).
watch(
    () => props.active || props.warm,
    (shown) => {
        if (shown) loadViewer();
    },
    { immediate: true }
);
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
                        <div class="hero-price reveal-item delay-3 text-black mb-8 d-flex align-center flex-wrap ga-3">
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
                            <model-viewer v-if="viewerReady" :src="props.product?.modelUrl || '/models/Shoe.glb'"
                                :poster="props.product?.imageUrl || '/assets/images/products/1.jpg'"
                                alt="AeroStride 3D Shoe" camera-controls :auto-rotate="props.active" rotation-per-second="30deg"
                                shadow-intensity="1" environment-image="neutral" class="the-shoe mx-auto hero-shoe-main"
                                style="width: 100%; max-width: 600px; height: 400px; display: block; outline: none;"></model-viewer>
                            <!-- Same poster the viewer itself would show, in the same
                                 600x400 box, so the 3D model swaps in without shifting
                                 anything around it. -->
                            <img v-else :src="props.product?.imageUrl || '/assets/images/products/1.jpg'"
                                alt="AeroStride 3D Shoe" class="the-shoe mx-auto hero-shoe-main"
                                style="width: 100%; max-width: 600px; height: 400px; display: block; object-fit: contain;" />
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
