<script setup>
import { computed, defineProps, onMounted, onUnmounted, ref, watch } from 'vue';

const props = defineProps({
    active: Boolean,
    warm: Boolean,
    products: {
        type: Array,
        default: () => []
    }
});

const shoes = [
    {
        title: 'NEON VELOCITY',
        tag: 'PERFORMANCE',
        color: '#2962FF',
        bgType: 'rain',
        rotation: 'rotate(-22deg) translateY(-8px)',
        features: ['AIR-UNIT', 'RESPONSIVE', 'LIGHTWEIGHT']
    },
    {
        title: 'PULSE CORE',
        tag: 'DYNAMICS',
        color: '#D50000',
        bgType: 'cubes',
        rotation: 'rotate(12deg) translateX(10px)',
        features: ['GRIP-SOLE', 'DURABLE', 'ENERGY']
    },
    {
        title: 'ZENITH AIR',
        tag: 'LIFESTYLE',
        color: '#6200EA',
        bgType: 'orbits',
        rotation: 'rotate(0deg) scale(1.05)',
        features: ['3D-MESH', 'STYLISH', 'ZEN-COMFORT']
    }
];

const activeCard = ref(0);
const hudValues = ref([10, 25, 88]);
const autoPlayProgress = ref(0);
let hudInterval;
let progressInterval;

const decoratedShoes = computed(() =>
    shoes.map((shoe, index) => ({
        ...shoe,
        image: props.products?.[index]?.imageUrl || ''
    }))
);

const currentShoe = computed(() => decoratedShoes.value[activeCard.value] || decoratedShoes.value[0]);

const rainParticles = Array.from({ length: 12 }, (_, index) => ({
    id: index,
    left: `${((index * 17) + 9) % 100}%`,
    delay: `${(index * 0.37) % 5}s`
}));

const cubeParticles = Array.from({ length: 8 }, (_, index) => ({
    id: index,
    left: `${((index * 19) + 11) % 90}%`,
    top: `${((index * 13) + 7) % 90}%`,
    delay: `${(index * 0.83) % 10}s`
}));

const orbitRings = Array.from({ length: 4 }, (_, index) => ({
    id: index,
    delay: `${index * 1.5}s`
}));

const nextCard = () => {
    activeCard.value = (activeCard.value + 1) % shoes.length;
    autoPlayProgress.value = 0;
};

const prevCard = () => {
    activeCard.value = (activeCard.value - 1 + shoes.length) % shoes.length;
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

                <div class="gradient-glow" :style="{ background: `radial-gradient(circle at center, ${currentShoe.color}0a 0%, transparent 70%)` }"></div>
            </div>

            <v-container fluid class="pa-0 fill-height position-relative z-index-10">
                <v-row no-gutters class="fill-height align-center">
                    <v-col cols="12" class="position-relative">
                        <div class="shoe-focus-container">
                            <div class="shoe-item">
                                <div class="shoe-meta-fixed">
                                    <span class="shoe-tag" :style="{ color: currentShoe.color }">{{ currentShoe.tag }}</span>
                                    <h2 class="shoe-name">{{ currentShoe.title }}</h2>
                                    <v-btn variant="outlined" :color="currentShoe.color" class="mt-4 px-8 py-4 font-weight-black explore-btn" rounded="lg">
                                        KHAM PHA CHI TIET
                                        <v-icon end>mdi-arrow-right</v-icon>
                                    </v-btn>
                                </div>

                                <div class="tech-specs-fixed">
                                    <div class="spec-group" :style="{ borderColor: `${currentShoe.color}4d` }">
                                        <div class="spec-item">
                                            <span class="label">ENGINE</span>
                                            <span class="val">{{ activeCard === 1 ? 'HYPER' : 'AIR' }} V{{ activeCard + 1 }}</span>
                                        </div>
                                        <div class="spec-divider"></div>
                                        <div class="spec-item">
                                            <span class="label">NEXT_SLIDE</span>
                                            <div class="auto-progress-bar">
                                                <div class="progress-fill" :style="{ width: autoPlayProgress + '%', background: currentShoe.color }"></div>
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
                                            :style="{ animationDelay: `${idx * 0.2}s`, top: `${15 + idx * 30}%`, left: activeCard === 1 ? '115%' : '-25%' }"
                                        >
                                            <div class="node-line" :style="{ background: currentShoe.color, height: '3px', opacity: 1 }"></div>
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

                                    <v-img v-if="currentShoe.image" :src="currentShoe.image" class="the-shoe" :style="{ transform: currentShoe.rotation }" loading="lazy"></v-img>
                                    <div class="shadow-floor" :style="{ background: `radial-gradient(ellipse at center, ${currentShoe.color}26 0%, transparent 70%)` }"></div>
                                    <div class="shoe-aura" :style="{ boxShadow: `0 0 160px ${currentShoe.color}1a` }"></div>
                                </div>
                            </div>
                        </div>

                        <div class="controls-overlay">
                            <v-btn icon size="x-large" variant="plain" color="blue-darken-4" class="nav-btn" @click="prevCard">
                                <v-icon size="48">mdi-chevron-left</v-icon>
                            </v-btn>

                            <div class="step-indicator">
                                <span v-for="n in 3" :key="n" class="step-line" :class="{ active: activeCard === n - 1 }" @click="activeCard = n - 1"></span>
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
.snap-section { height: 100vh; width: 100%; scroll-snap-align: start; scroll-snap-stop: always; position: relative; background: #ffffff; perspective: 1000px; }
.digital-bg { position: absolute; inset: 0; z-index: 1; overflow: hidden; }
.bg-scene { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; }
.ripple-ring { position: absolute; border: 2px solid; border-radius: 50%; opacity: 0; animation: ripple-out 6s linear infinite; }
@keyframes ripple-out { 0% { width: 0; height: 0; opacity: 0; } 10% { opacity: 0.3; } 80% { opacity: 0.1; } 100% { width: 1000px; height: 1000px; opacity: 0; } }
.grid-layer { position: absolute; width: 200%; height: 200%; top: -50%; left: -50%; background-image: linear-gradient(rgba(41, 98, 255, 0.04) 2px, transparent 2px), linear-gradient(90deg, rgba(41, 98, 255, 0.04) 2px, transparent 2px); background-size: 80px 80px; transform: perspective(1000px) rotateX(65deg); animation: grid-move 15s linear infinite; }
@keyframes grid-move { 0% { transform: perspective(1000px) rotateX(65deg) translateY(0); } 100% { transform: perspective(1000px) rotateX(65deg) translateY(80px); } }
.particle-rain { position: absolute; width: 2px; height: 60px; opacity: 0.15; animation: rain-drop 4s linear infinite; }
@keyframes rain-drop { 0% { transform: translateY(-100vh); opacity: 0; } 50% { opacity: 0.3; } 100% { transform: translateY(100vh); opacity: 0; } }
.floating-cube { position: absolute; width: 50px; height: 50px; border: 2px solid; opacity: 0.1; animation: cube-float 15s linear infinite; }
@keyframes cube-float { 0% { transform: rotate(0deg) translate(0, 0); } 50% { transform: rotate(180deg) translate(40px, -40px); } 100% { transform: rotate(360deg) translate(0, 0); } }
.shoe-focus-container { height: 80vh; display: flex; align-items: center; justify-content: center; position: relative; perspective: 1500px; }
.shoe-item { position: absolute; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.shoe-visual-wrapper-center { position: relative; transform-style: preserve-3d; }
.the-shoe { width: 500px; filter: drop-shadow(0 40px 80px rgba(0,0,0,0.12)); animation: shoe-hover 6s ease-in-out infinite; z-index: 5; }
@keyframes shoe-hover { 0%, 100% { transform: translateY(0) rotate(-15deg); } 50% { transform: translateY(-20px) rotate(-10deg); } }
.micro-details { position: absolute; inset: 0; pointer-events: none; z-index: 10; }
.feature-node { position: absolute; display: flex; align-items: center; gap: 20px; animation: node-float 4s ease-in-out infinite; }
.node-line { width: 100px; }
.node-text { padding: 6px 18px; font-size: 0.8rem; font-weight: 950; border: 3px solid; border-radius: 8px; background: white; color: #1e293b; letter-spacing: 2px; box-shadow: 0 10px 20px rgba(0,0,0,0.08); }
.node-hud { font-size: 0.65rem; font-family: monospace; margin-top: 6px; letter-spacing: 1px; }
@keyframes node-float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-15px); } }
.shadow-floor { position: absolute; bottom: -10px; left: 50%; transform: translateX(-50%) rotateX(75deg); width: 80%; height: 40px; filter: blur(30px); z-index: 0; }
.shoe-aura { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 70%; height: 70%; border-radius: 50%; z-index: 1; }
.shoe-meta-fixed { position: absolute; top: 12%; left: 8%; text-align: left; z-index: 20; }
.tech-specs-fixed { position: absolute; top: 12%; right: 8%; z-index: 20; }
.shoe-name { font-size: 5rem; font-weight: 950; letter-spacing: -2px; color: #1e293b; line-height: 1; margin-top: 10px; }
.shoe-tag { font-size: 0.85rem; font-weight: 900; letter-spacing: 6px; }
.explore-btn { border-width: 2px; letter-spacing: 2px; transition: transform 0.2s ease, box-shadow 0.2s ease; &:hover { transform: translateY(-3px); box-shadow: 0 10px 20px rgba(0,0,0,0.1); } }
.spec-group { display: flex; gap: 30px; background: white; padding: 12px 35px; border-radius: 100px; border: 3px solid rgba(0,0,0,0.06); box-shadow: 0 10px 30px rgba(0,0,0,0.04); }
.spec-item { display: flex; flex-direction: column; align-items: flex-start; .label { font-size: 0.65rem; color: #94a3b8; font-weight: 900; letter-spacing: 2px; } .val { font-size: 0.9rem; font-weight: 950; } }
.spec-divider { width: 2px; height: 30px; background: rgba(0,0,0,0.08); align-self: center; }
.auto-progress-bar { width: 80px; height: 5px; background: rgba(0,0,0,0.05); border-radius: 10px; overflow: hidden; margin-top: 5px; }
.progress-fill { height: 100%; transition: width 0.08s linear; }
.controls-overlay { position: absolute; bottom: 50px; left: 0; width: 100%; display: flex; align-items: center; justify-content: space-between; padding: 0 100px; z-index: 30; }
.nav-btn { transition: transform 0.2s ease, opacity 0.2s ease; opacity: 0.6; &:hover { opacity: 1; transform: scale(1.08); } }
.step-line { display: inline-block; width: 40px; height: 5px; background: rgba(0,0,0,0.05); margin: 0 6px; cursor: pointer; transition: all 0.3s ease; &.active { background: #2962FF; width: 65px; } }

@media (max-width: 960px) {
    .shoe-meta-fixed,
    .tech-specs-fixed,
    .controls-overlay { position: static; }
    .shoe-focus-container { height: auto; min-height: 70vh; }
    .shoe-name { font-size: 3rem; }
    .the-shoe { width: 100%; max-width: 420px; }
    .shoe-item { position: relative; flex-direction: column; }
    .controls-overlay { padding: 24px; justify-content: center; gap: 24px; }
}
</style>
