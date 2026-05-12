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
        <div class="hero-bg-animated" :style="{ transform: `translate(${props.mouseX * 0.5}px, ${props.mouseY * 0.5}px)` }"></div>
        <v-container>
            <v-row align="center">
                <v-col cols="12" lg="6" :class="{ 'animate-in-left': props.active }">
                    <h1 class="hero-title text-uppercase font-weight-black text-blue-darken-4">
                        NEXT-GEN <br />
                        <span class="text-blue-accent-4">AEROSTRIDE</span>
                    </h1>
                    <p class="text-h5 text-blue-grey-darken-1 mt-6 mb-10 leading-relaxed">
                        Công nghệ giày tương lai dành cho những người dẫn đầu. 
                        Trải nghiệm sự êm ái vượt giới hạn.
                    </p>
                    <v-btn size="x-large" color="blue-darken-4" rounded="xl" class="px-12 elevation-10">
                        KHÁM PHÁ NGAY
                    </v-btn>
                </v-col>
                <v-col cols="12" lg="6" class="text-center" :class="{ 'animate-in-right': props.active }">
                    <div class="hero-shoe-wrapper">
                        <v-img
                            v-if="props.product?.imageUrl"
                            :src="props.product.imageUrl"
                            eager
                            class="hero-shoe-main"
                        ></v-img>
                        <div class="shoe-glow"></div>
                    </div>
                </v-col>
            </v-row>
        </v-container>
    </section>
</template>

<style scoped lang="scss">
.snap-section {
    height: 100vh;
    width: 100%;
    scroll-snap-align: start;
    scroll-snap-stop: always;
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
    filter: drop-shadow(0 30px 60px rgba(0,0,0,0.2));
    animation: floating 6s ease-in-out infinite;
}

@keyframes floating { 
    0%, 100% { transform: translateY(0) rotate(-5deg); } 
    50% { transform: translateY(-30px) rotate(5deg); } 
}

.shoe-glow {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 300px;
    height: 300px;
    background: #2962FF;
    filter: blur(120px);
    opacity: 0.2;
    z-index: -1;
}

.animate-in-left { animation: slideInLeft 1s cubic-bezier(0.2, 1, 0.3, 1) both; }
.animate-in-right { animation: slideInRight 1s cubic-bezier(0.2, 1, 0.3, 1) both; }

@keyframes slideInLeft { from { opacity: 0; transform: translateX(-100px); } to { opacity: 1; transform: translateX(0); } }
@keyframes slideInRight { from { opacity: 0; transform: translateX(100px); } to { opacity: 1; transform: translateX(0); } }

@media (max-width: 960px) {
    .hero-title { font-size: 3rem; }
    .hero-shoe-main { width: 100%; }
}
</style>
