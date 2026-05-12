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

const products = [
    {
        code: 'AERO-X1-INFINITE',
        category: 'HYPER-PERFORMANCE',
        desc: 'Hệ thống giày thông minh AeroStride X1 tối ưu hóa hiệu suất chạy bộ trong thời gian thực.',
        specs: [
            { label: 'MASS', value: '220.45g' },
            { label: 'AIR-FLOW', value: '98.2%' },
            { label: 'ENERGY-RETURN', value: '84.5%' },
            { label: 'TRACTION', value: 'MAX' }
        ]
    },
    {
        code: 'AERO-STRIKE-V3',
        category: 'SPEED-ENGINEERED',
        desc: 'Thiết kế cho vận động viên nước rút, V3 tạo lực bật tốt và phản hồi nhanh.',
        specs: [
            { label: 'MASS', value: '195.12g' },
            { label: 'RESPONSE', value: '0.01s' },
            { label: 'CARBON-STIFF', value: 'ULTRA' },
            { label: 'AERO-DRAG', value: '-12%' }
        ]
    },
    {
        code: 'GLIDE-PRO-2.0',
        category: 'ENDURANCE-TECH',
        desc: 'Glide Pro tập trung vào độ êm tối đa cho các chặng đua dài hơi.',
        specs: [
            { label: 'CUSHION', value: '99.9%' },
            { label: 'DROP', value: '8mm' },
            { label: 'STABILITY', value: 'PRO+' },
            { label: 'BREATH', value: 'HIGH' }
        ]
    }
];

const currentIndex = ref(0);
const rotationY = ref(0);
const rotationX = ref(-10);
const rotationZ = ref(0);
let animationFrame = 0;
let lastTick = 0;

const decoratedProducts = computed(() =>
    products.map((product, index) => ({
        ...product,
        image: props.products?.[index]?.imageUrl || ''
    }))
);

const activeProduct = computed(() => decoratedProducts.value[currentIndex.value] || decoratedProducts.value[0]);

const nextProduct = () => {
    currentIndex.value = (currentIndex.value + 1) % products.length;
};

const prevProduct = () => {
    currentIndex.value = (currentIndex.value - 1 + products.length) % products.length;
};

const stopAnimation = () => {
    if (animationFrame) {
        cancelAnimationFrame(animationFrame);
        animationFrame = 0;
    }
};

const animate = (timestamp) => {
    if (!props.active) {
        animationFrame = 0;
        return;
    }

    if (!lastTick) {
        lastTick = timestamp;
    }

    const delta = timestamp - lastTick;
    lastTick = timestamp;

    rotationY.value += delta * 0.035;
    rotationX.value = -10 + Math.sin(timestamp / 1500) * 4;
    rotationZ.value = Math.cos(timestamp / 2500) * 2;

    animationFrame = requestAnimationFrame(animate);
};

const startAnimation = () => {
    if (animationFrame || !props.active) return;
    lastTick = 0;
    animationFrame = requestAnimationFrame(animate);
};

onMounted(startAnimation);

watch(
    () => props.active,
    (isActive) => {
        if (isActive) {
            startAnimation();
        } else {
            stopAnimation();
        }
    },
    { immediate: true }
);

onUnmounted(() => {
    stopAnimation();
});
</script>

<template>
    <section class="snap-section showcase-360-section overflow-hidden">
        <template v-if="props.active || props.warm">
            <div class="hud-background">
                <div class="grid-line"></div>
                <div class="circles">
                    <div class="circle c1"></div>
                    <div class="circle c2"></div>
                    <div class="circle c3"></div>
                </div>
            </div>

            <v-container fluid class="fill-height px-16 relative z-10">
                <v-row align="center" class="fill-height">
                    <v-col cols="12" md="3" class="tech-hud">
                        <div class="hud-box" :class="{ active: props.active }">
                            <div class="box-header">
                                <v-icon size="16">mdi-cube-outline</v-icon>
                                <span>STRUCTURE ANALYSIS</span>
                            </div>
                            <div class="box-content">
                                <div v-for="(spec, i) in activeProduct.specs" :key="i" class="spec-row">
                                    <span class="label">{{ spec.label }}</span>
                                    <div class="dots flex-grow-1"></div>
                                    <span class="value">{{ spec.value }}</span>
                                </div>
                            </div>
                        </div>

                        <div class="hud-box mt-8" :class="{ active: props.active }" style="transition-delay: 0.1s">
                            <div class="box-header">
                                <v-icon size="16">mdi-sine-wave</v-icon>
                                <span>ENGINE DYNAMICS</span>
                            </div>
                            <div class="box-content text-center py-4">
                                <div class="cube-icon">
                                    <v-icon size="60" color="#2962FF">mdi-cube-scan</v-icon>
                                </div>
                                <div class="mt-4 text-caption font-weight-bold opacity-50">{{ activeProduct.category }}</div>
                            </div>
                        </div>
                    </v-col>

                    <v-col cols="12" md="6" class="viewer-area relative">
                        <div class="main-hud-ring">
                            <div class="ring outer"></div>
                            <div class="ring inner"></div>
                            <div class="compass">
                                <div v-for="i in 12" :key="i" class="tick" :style="{ transform: `rotate(${i * 30}deg)` }"></div>
                            </div>
                        </div>

                        <div class="stage-3d">
                            <div :key="currentIndex" class="shoe-container-3d" :style="{ transform: `rotateX(${rotationX}deg) rotateY(${rotationY}deg) rotateZ(${rotationZ}deg)` }">
                                <div class="shoe-wrapper">
                                    <v-img v-if="activeProduct.image" :src="activeProduct.image" class="shoe-img" contain loading="lazy"></v-img>
                                    <div class="shoe-glow"></div>
                                </div>

                                <div class="hud-3d-rings">
                                    <div class="h-ring r1"></div>
                                    <div class="h-ring r2"></div>
                                </div>
                            </div>

                            <div class="ground-shadow"></div>
                        </div>

                        <div class="interaction-hint">
                            <span class="text">3D SPATIAL SCANNER: {{ activeProduct.code }}</span>
                            <div class="pulse-dot"></div>
                        </div>
                    </v-col>

                    <v-col cols="12" md="3" class="lore-content">
                        <div class="lore-container" :class="{ active: props.active }">
                            <div class="category">
                                <div class="dot"></div>
                                <span>AEROSTRIDE: ENDFIELD-TECH</span>
                            </div>
                            <h2 class="title">{{ activeProduct.code }}</h2>
                            <p class="description">{{ activeProduct.desc }}</p>

                            <div class="nav-controls mt-10">
                                <div class="page-num">
                                    <span class="current">{{ currentIndex + 1 }}</span><span class="total"> / {{ products.length }}</span>
                                </div>
                                <div class="nav-btns">
                                    <v-btn icon="mdi-chevron-left" variant="outlined" rounded="lg" class="mr-2" @click="prevProduct"></v-btn>
                                    <v-btn icon="mdi-chevron-right" variant="flat" color="black" rounded="lg" @click="nextProduct"></v-btn>
                                </div>
                            </div>
                        </div>
                    </v-col>
                </v-row>
            </v-container>

            <div class="bottom-hud-bar">
                <div class="bar-item">STATUS: CALIBRATED</div>
                <div class="bar-item">SPATIAL: ACTIVE</div>
                <div class="bar-item">ASSET_ID: #AS-{{ currentIndex + 101 }}</div>
                <div class="flex-grow-1"></div>
                <div class="bar-item font-weight-black uppercase">AeroStride OS v3.4.2 // {{ activeProduct.category }}</div>
            </div>
        </template>
    </section>
</template>

<style scoped lang="scss">
.showcase-360-section {
    height: 100vh;
    width: 100%;
    scroll-snap-align: start;
    scroll-snap-stop: always;
    background: #fdfdfd;
    position: relative;
    font-family: 'Inter', sans-serif;
}

.relative { position: relative; }
.z-10 { z-index: 10; }

.hud-background {
    position: absolute;
    inset: 0;

    .grid-line {
        position: absolute;
        top: 50%;
        left: 0;
        width: 100%;
        height: 1px;
        background: linear-gradient(to right, transparent, rgba(0,0,0,0.05), transparent);
    }

    .circles {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        .circle {
            border-radius: 50%;
            border: 1px dashed rgba(0,0,0,0.05);
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
        }
        .c1 { width: 400px; height: 400px; }
        .c2 { width: 600px; height: 600px; animation: rotate 30s linear infinite; }
        .c3 { width: 800px; height: 800px; animation: rotate 50s linear infinite reverse; }
    }
}

.stage-3d {
    width: 600px;
    height: 600px;
    perspective: 1500px;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
}

.shoe-container-3d {
    width: 500px;
    height: 350px;
    transform-style: preserve-3d;
    display: flex;
    align-items: center;
    justify-content: center;
}

.shoe-wrapper {
    position: relative;
    transform-style: preserve-3d;
    filter: drop-shadow(0 30px 60px rgba(0,0,0,0.15));
}

.shoe-img {
    width: 450px;
    height: auto;
    transform: translateZ(50px);
}

.hud-3d-rings {
    position: absolute;
    width: 100%;
    height: 100%;
    transform-style: preserve-3d;
    .h-ring { position: absolute; border: 1px solid rgba(41, 98, 255, 0.2); border-radius: 50%; top: 50%; left: 50%; }
    .r1 { width: 500px; height: 500px; transform: translate(-50%, -50%) rotateX(90deg); }
    .r2 { width: 550px; height: 550px; transform: translate(-50%, -50%) rotateY(90deg); border-style: dashed; }
}

.ground-shadow {
    position: absolute;
    bottom: 100px;
    left: 50%;
    transform: translateX(-50%);
    width: 300px;
    height: 60px;
    background: radial-gradient(ellipse at center, rgba(0,0,0,0.1) 0%, transparent 70%);
    filter: blur(10px);
}

.hud-box {
    background: rgba(255, 255, 255, 0.8);
    border: 1px solid rgba(0,0,0,0.08);
    padding: 20px;
    border-radius: 4px;
    opacity: 0;
    transform: translateX(-30px);
    transition: all 0.8s ease;
    &.active { opacity: 1; transform: translateX(0); }
    .box-header { display: flex; align-items: center; gap: 8px; font-size: 0.75rem; font-weight: 900; color: #666; margin-bottom: 18px; border-bottom: 1px solid rgba(0,0,0,0.05); padding-bottom: 10px; text-transform: uppercase; }
}
.spec-row {
    display: flex;
    align-items: center;
    margin-bottom: 10px;
    font-size: 0.85rem;
    .label { color: #999; font-weight: 700; }
    .dots { border-bottom: 1px dotted rgba(0,0,0,0.1); margin: 0 10px; height: 12px; }
    .value { font-weight: 900; color: #000; font-family: monospace; }
}

.main-hud-ring {
    position: absolute;
    width: 550px;
    height: 550px;
    .ring { position: absolute; border-radius: 50%; border: 1px solid rgba(0,0,0,0.05); top: 50%; left: 50%; transform: translate(-50%, -50%); }
    .outer { width: 100%; height: 100%; border-style: double; border-width: 4px; }
    .inner { width: 85%; height: 85%; border-style: dashed; }
    .compass {
        position: absolute;
        width: 100%;
        height: 100%;
        .tick { position: absolute; width: 2px; height: 15px; background: rgba(0,0,0,0.1); left: 50%; transform-origin: 0 275px; }
    }
}

.interaction-hint {
    position: absolute;
    bottom: 20px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    align-items: center;
    gap: 10px;
    .text { font-size: 0.65rem; font-weight: 900; color: #999; letter-spacing: 3px; }
    .pulse-dot { width: 8px; height: 8px; background: #2962FF; border-radius: 50%; animation: pulse 1.5s infinite; }
}

.lore-content {
    .lore-container {
        opacity: 0;
        transform: translateX(30px);
        transition: all 0.8s ease;
        &.active { opacity: 1; transform: translateX(0); }
    }
    .category {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 0.75rem;
        font-weight: 900;
        color: #666;
        letter-spacing: 2px;
        margin-bottom: 20px;
        .dot { width: 4px; height: 4px; background: #000; }
    }
    .title { font-size: 3.2rem; font-weight: 950; color: #000; line-height: 1; margin-bottom: 24px; letter-spacing: -2px; }
    .description { font-size: 1rem; color: #444; line-height: 1.8; font-weight: 500; min-height: 100px; }
}

.nav-controls {
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-top: 1px solid rgba(0,0,0,0.1);
    padding-top: 30px;
    .page-num {
        font-size: 1.2rem;
        font-weight: 900;
        .current { color: #000; }
        .total { color: #ccc; }
    }
}

.bottom-hud-bar {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    background: #000;
    color: #fff;
    display: flex;
    padding: 10px 40px;
    font-size: 0.65rem;
    letter-spacing: 1px;
    z-index: 20;
    .bar-item { margin-right: 30px; display: flex; align-items: center; gap: 5px; opacity: 0.6; }
}

@keyframes rotate { from { transform: translate(-50%, -50%) rotate(0deg); } to { transform: translate(-50%, -50%) rotate(360deg); } }
@keyframes pulse { 0% { transform: scale(1); opacity: 1; } 100% { transform: scale(2.5); opacity: 0; } }

@media (max-width: 960px) {
    .tech-hud { display: none; }
    .stage-3d { width: 100%; }
}
</style>
