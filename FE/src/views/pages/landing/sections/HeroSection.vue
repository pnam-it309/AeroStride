<script setup>
import { defineProps } from 'vue';
const props = defineProps({
    active: Boolean,
    mouseX: Number,
    mouseY: Number,
    product: Object
});
</script>

<template>
    <section class="snap-section hero-section d-flex align-center">
        <template v-if="props.active || props.warm">
            <div class="hero-bg-animated" :style="{ transform: `translate(${props.mouseX * 0.5}px, ${props.mouseY * 0.5}px)` }"></div>
            <div class="hero-diagonal-band"></div>
            <v-container>
                <v-row align="center">
                    <v-col cols="12" lg="5" offset-lg="1" :class="{ 'reveal-container': true, active: props.active }" class="position-relative ps-lg-8">
                        <h1 class="hero-title reveal-item delay-1">
                            {{ props.product?.tenSanPham || 'AeroStride' }}
                        </h1>
                        <p class="hero-subtitle reveal-item delay-2 text-grey-darken-3 mt-4">
                            {{ props.product?.tenThuongHieu || 'PREMIUM' }}
                        </p>
                        <div class="hero-price reveal-item delay-3 text-black mb-8">
                            {{ props.product?.giaBanThapNhat ? formatPrice(props.product.giaBanThapNhat) : '1.200.000 ₫' }}
                        </div>
                        <v-btn
                            size="x-large"
                            rounded="xl"
                            class="px-12 hero-btn reveal-item delay-4"
                            :to="props.product?.id ? `/product/${props.product.id}` : '/shoes'"
                        >
                            KHÁM PHÁ NGAY
                        </v-btn>
                    </v-col>
                    <v-col cols="12" lg="6" class="text-center reveal-container" :class="{ active: props.active }">
                        <div class="hero-shoe-wrapper reveal-item delay-4">
                            <model-viewer
                                :src="props.product?.modelUrl || '/models/Shoe.glb'"
                                :poster="props.product?.imageUrl || '/assets/images/products/1.jpg'"
                                alt="AeroStride 3D Shoe"
                                camera-controls
                                auto-rotate
                                rotation-per-second="30deg"
                                shadow-intensity="1"
                                environment-image="neutral"
                                class="the-shoe mx-auto hero-shoe-main"
                                style="width: 100%; max-width: 600px; height: 400px; display: block; outline: none;"
                            ></model-viewer>
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
    overflow: hidden;
}

.hero-bg-animated {
    position: absolute;
    width: 120%;
    height: 120%;
    background: radial-gradient(circle, rgba(41, 98, 255, 0.05) 0%, transparent 70%);
    z-index: 0;
}

.hero-title {
    font-size: 5.5rem;
    line-height: 0.9;
}

.hero-shoe-main {
    width: 600px;
    filter: drop-shadow(0 30px 60px rgba(0, 0, 0, 0.2));
    animation: floating 6s ease-in-out infinite;
}

@keyframes floating {
    0%,
    100% {
        transform: translateY(0) rotate(-5deg);
    }
    50% {
        transform: translateY(-30px) rotate(5deg);
    }
}

.shoe-glow {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 300px;
    height: 300px;
    background: #2962ff;
    filter: blur(120px);
    opacity: 0.2;
    z-index: -1;
}

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

.hero-shoe-main {
    width: 600px;
    filter: drop-shadow(0 30px 60px rgba(0, 0, 0, 0.2));
    animation: floating 6s ease-in-out infinite;
    transition: transform 1s cubic-bezier(0.2, 1, 0.3, 1);
}

@media (max-width: 960px) {
    .hero-title {
        font-size: 3rem;
    }
    .hero-shoe-main {
        width: 100%;
    }
}
</style>
