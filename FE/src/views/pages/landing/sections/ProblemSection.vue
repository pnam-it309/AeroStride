<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import '@google/model-viewer';

const props = defineProps({
    active: Boolean,
    warm: Boolean,
    products: {
        type: Array,
        default: () => []
    }
});



const activeCard = ref(0);
const hudValues = ref([10, 25, 88]);
const autoPlayProgress = ref(0);
let hudInterval;
let progressInterval;

const decoratedShoes = computed(() => {
    return props.products.map((p, index) => ({
        title: p.title || 'AEROSTRIDE',
        tag: p.category || p.subtitle || 'PREMIUM',
        color: index === 0 ? '#2962FF' : index === 1 ? '#D50000' : '#6200EA',
        bgType: index === 0 ? 'rain' : index === 1 ? 'cubes' : 'orbits',
        rotation:
            index === 0 ? 'rotate(-22deg) translateY(-8px)' : index === 1 ? 'rotate(12deg) translateX(10px)' : 'rotate(0deg) scale(1.05)',
        features: [
            p.chatLieu ? `VẬT LIỆU: ${p.chatLieu.toUpperCase()}` : 'BỀN BỈ',
            p.deGiay ? `ĐẾ GIÀY: ${p.deGiay.toUpperCase()}` : 'ÊM ÁI',
            p.mucDichChay ? `MỤC ĐÍCH: ${p.mucDichChay.toUpperCase()}` : 'TỐI ƯU'
        ],
        image: p.imageUrl || '',
        modelUrl: p.modelUrl || '',
        id: p.id
    }));
});

const currentShoe = computed(() => decoratedShoes.value[activeCard.value] || decoratedShoes.value[0] || {});

const rainParticles = Array.from({ length: 12 }, (_, index) => ({
    id: index,
    left: `${(index * 17 + 9) % 100}%`,
    delay: `${(index * 0.37) % 5}s`
}));

const cubeParticles = Array.from({ length: 8 }, (_, index) => ({
    id: index,
    left: `${(index * 19 + 11) % 90}%`,
    top: `${(index * 13 + 7) % 90}%`,
    delay: `${(index * 0.83) % 10}s`
}));

const orbitRings = Array.from({ length: 4 }, (_, index) => ({
    id: index,
    delay: `${index * 1.5}s`
}));

const nextCard = () => {
    activeCard.value = (activeCard.value + 1) % decoratedShoes.value.length;
    autoPlayProgress.value = 0;
};

const prevCard = () => {
    activeCard.value = (activeCard.value - 1 + decoratedShoes.value.length) % decoratedShoes.value.length;
    autoPlayProgress.value = 0;
};

const startAutoPlay = () => {
    if (progressInterval) return;

    autoPlayProgress.value = 0;
    progressInterval = window.setInterval(() => {
        if (!props.active) return;

        autoPlayProgress.value += 1;
        if (autoPlayProgress.value >= 100) {
            nextCard();
        }
    }, 80);
};

const stopAutoPlay = () => {
    if (!progressInterval) return;
    clearInterval(progressInterval);
    progressInterval = undefined;
};

const startHud = () => {
    if (hudInterval) return;

    hudInterval = window.setInterval(() => {
        hudValues.value = hudValues.value.map(() => Math.floor(Math.random() * 99));
    }, 250);
};

const stopHud = () => {
    if (!hudInterval) return;
    clearInterval(hudInterval);
    hudInterval = undefined;
};

onMounted(() => {
    if (props.active) {
        startHud();
        startAutoPlay();
    }
});

onUnmounted(() => {
    stopHud();
    stopAutoPlay();
});

watch(
    () => props.active,
    (isActive) => {
        if (isActive) {
            startHud();
            startAutoPlay();
        } else {
            stopHud();
            stopAutoPlay();
        }
    },
    { immediate: true }
);
</script>

<template>
    <section class="snap-section problem-section d-flex align-center overflow-hidden">
        <template v-if="props.active || props.warm">
            <div class="digital-bg">
                <div v-if="currentShoe.bgType === 'rain'" class="bg-scene scene-rain">
                    <div class="grid-layer"></div>
                    <div
                        v-for="particle in rainParticles"
                        :key="particle.id"
                        class="particle-rain"
                        :style="{ left: particle.left, animationDelay: particle.delay, background: currentShoe.color }"
                    ></div>
                </div>

                <div v-else-if="currentShoe.bgType === 'cubes'" class="bg-scene scene-cubes">
                    <div
                        v-for="particle in cubeParticles"
                        :key="particle.id"
                        class="floating-cube"
                        :style="{ left: particle.left, top: particle.top, animationDelay: particle.delay, borderColor: currentShoe.color }"
                    ></div>
                </div>

                <div v-else class="bg-scene scene-orbits">
                    <div
                        v-for="ring in orbitRings"
                        :key="ring.id"
                        class="ripple-ring"
                        :style="{ animationDelay: ring.delay, borderColor: currentShoe.color }"
                    ></div>
                </div>

                <div
                    class="gradient-glow"
                    :style="{ background: `radial-gradient(circle at center, ${currentShoe.color}0a 0%, transparent 70%)` }"
                ></div>
            </div>

            <v-container fluid class="pa-0 fill-height position-relative z-index-10">
                <v-row no-gutters class="fill-height align-center">
                    <v-col cols="12" class="position-relative">
                        <div class="shoe-focus-container">
                            <div class="shoe-item">
                                <div class="shoe-meta-fixed reveal-container" :class="{ active: props.active }">
                                    <span class="shoe-tag reveal-item delay-1" :style="{ color: currentShoe.color }">{{
                                        currentShoe.tag
                                    }}</span>
                                    <h2 class="shoe-name reveal-item delay-2">{{ currentShoe.title }}</h2>
                                    <v-btn
                                        variant="outlined"
                                        :color="currentShoe.color"
                                        class="mt-4 px-8 py-4 font-weight-black explore-btn reveal-item delay-3"
                                        rounded="lg"
                                    >
                                        KHAM PHA CHI TIET
                                        <v-icon end>mdi-arrow-right</v-icon>
                                    </v-btn>
                                </div>

                                <div class="tech-specs-fixed reveal-container" :class="{ active: props.active }">
                                    <div class="spec-group reveal-item delay-4" :style="{ borderColor: `${currentShoe.color}4d` }">
                                        <div class="spec-item">
                                            <span class="label">ENGINE</span>
                                            <span class="val">{{ activeCard === 1 ? 'HYPER' : 'AIR' }} V{{ activeCard + 1 }}</span>
                                        </div>
                                        <div class="spec-divider"></div>
                                        <div class="spec-item">
                                            <span class="label">NEXT_SLIDE</span>
                                            <div class="auto-progress-bar">
                                                <div
                                                    class="progress-fill"
                                                    :style="{ width: autoPlayProgress + '%', background: currentShoe.color }"
                                                ></div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div class="shoe-visual-wrapper-center">
                                    <div class="micro-details">
                                        <div
                                            v-for="(feat, idx) in currentShoe.features"
                                            :key="idx"
                                            class="feature-node"
                                            :style="{
                                                animationDelay: `${idx * 0.2}s`,
                                                top: `${15 + idx * 30}%`,
                                                left: activeCard === 1 ? '115%' : '-25%'
                                            }"
                                        >
                                            <div
                                                class="node-line"
                                                :style="{ background: currentShoe.color, height: '3px', opacity: 1 }"
                                            ></div>
                                            <div class="node-content">
                                                <div class="node-text" :style="{ borderColor: currentShoe.color, borderWidth: '3px' }">
                                                    {{ feat }}
                                                </div>
                                                <div class="node-hud" :style="{ color: currentShoe.color, fontWeight: '950' }">
                                                    SYNC_VAL // {{ hudValues[idx] }}%
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <model-viewer
                                        :src="currentShoe.modelUrl || 'https://modelviewer.dev/shared-assets/models/Shoe.glb'"
                                        :poster="currentShoe.image || '/assets/images/products/1.jpg'"
                                        :alt="currentShoe.title"
                                        camera-controls
                                        auto-rotate
                                        rotation-per-second="30deg"
                                        shadow-intensity="1"
                                        environment-image="neutral"
                                        class="the-shoe object-contain"
                                        :style="{ transform: currentShoe.rotation, width: '100%', maxWidth: '500px', height: '400px', zIndex: 10, position: 'relative', outline: 'none' }"
                                    ></model-viewer>
                                    <div
                                        class="shadow-floor"
                                        :style="{
                                            background: `radial-gradient(ellipse at center, ${currentShoe.color}26 0%, transparent 70%)`
                                        }"
                                    ></div>
                                    <div class="shoe-aura" :style="{ boxShadow: `0 0 160px ${currentShoe.color}1a` }"></div>
                                </div>
                            </div>
                        </div>

                        <div class="controls-overlay">
                            <v-btn icon size="x-large" variant="plain" color="blue-darken-4" class="nav-btn" @click="prevCard">
                                <v-icon size="48">mdi-chevron-left</v-icon>
                            </v-btn>

                            <div class="step-indicator">
                                <span
                                    v-for="n in 3"
                                    :key="n"
                                    class="step-line"
                                    :class="{ active: activeCard === n - 1 }"
                                    @click="activeCard = n - 1"
                                ></span>
                            </div>

                            <v-btn icon size="x-large" variant="plain" color="blue-darken-4" class="nav-btn" @click="nextCard">
                                <v-icon size="48">mdi-chevron-right</v-icon>
                            </v-btn>
                        </div>
                    </v-col>
                </v-row>
            </v-container>
        </template>
    </section>
</template>

<style scoped lang="scss">
@import '@/scss/pages/landing/_problem-section.scss';
</style>
