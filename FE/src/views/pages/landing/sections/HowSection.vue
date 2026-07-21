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



const currentIndex = ref(0);
const rotationY = ref(0);
const rotationX = ref(-10);
const rotationZ = ref(0);
let animationFrame = 0;
let lastTick = 0;

const decoratedProducts = computed(() => {
    return props.products.map((p, index) => ({
        id: p.id,
        code: p.title || 'AeroStride Prime',
        category: p.category || p.subtitle || 'PERFORMANCE',
        desc: p.summary || 'Trải nghiệm đỉnh cao công nghệ giày thể thao.',
        specs: [
            { label: 'CHẤT LIỆU', value: p.chatLieu || 'CAO CẤP' },
            { label: 'ĐẾ GIÀY', value: p.deGiay || 'ÊM ÁI' },
            { label: 'SỐ LƯỢNG TỒN', value: `${p.tongSoLuongTon ?? p.raw?.tongSoLuongTon ?? p.soLuong ?? 0} sản phẩm` },
            { label: 'GIÁ CHỈ TỪ', value: (p.giaBanThapNhat ?? p.raw?.giaBanThapNhat) ? new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(p.giaBanThapNhat ?? p.raw?.giaBanThapNhat) : 'Liên hệ' }
        ],
        image: p.imageUrl || '',
        modelUrl: p.modelUrl || ''
    }));
});

const activeProduct = computed(() => decoratedProducts.value[currentIndex.value] || decoratedProducts.value[0]);

const nextProduct = () => {
    currentIndex.value = (currentIndex.value + 1) % decoratedProducts.value.length;
};

const prevProduct = () => {
    currentIndex.value = (currentIndex.value - 1 + decoratedProducts.value.length) % decoratedProducts.value.length;
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

            <v-container fluid class="fill-height px-16 position-relative z-index-10">
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

                    <v-col cols="12" md="6" class="viewer-area position-relative">
                        <div class="main-hud-ring">
                            <div class="ring outer"></div>
                            <div class="ring inner"></div>
                            <div class="compass">
                                <div v-for="i in 12" :key="i" class="tick" :style="{ transform: `rotate(${i * 30}deg)` }"></div>
                            </div>
                        </div>

                        <div class="stage-3d">
                            <div
                                :key="currentIndex"
                                class="shoe-container-3d"
                                :style="{ transform: `rotateX(${rotationX}deg) rotateY(${rotationY}deg) rotateZ(${rotationZ}deg)` }"
                            >
                                <div class="shoe-wrapper">
                                    <model-viewer
                                        :src="activeProduct.modelUrl || '/models/Shoe.glb'"
                                        :poster="activeProduct.image || '/assets/images/products/1.jpg'"
                                        :alt="activeProduct.code"
                                        camera-controls
                                        auto-rotate
                                        rotation-per-second="30deg"
                                        shadow-intensity="1"
                                        environment-image="neutral"
                                        class="shoe-img object-contain"
                                        :style="{ width: '100%', maxWidth: '450px', height: '350px', zIndex: 10, position: 'relative', outline: 'none' }"
                                    ></model-viewer>
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
                                    <span class="current">{{ currentIndex + 1 }}</span
                                    ><span class="total"> / {{ decoratedProducts.length }}</span>
                                </div>
                                <div class="nav-btns">
                                    <v-btn
                                        icon="mdi-chevron-left"
                                        variant="outlined"
                                        rounded="lg"
                                        class="mr-2"
                                        @click="prevProduct"
                                    ></v-btn>
                                    <v-btn icon="mdi-chevron-right" variant="flat" color="black" rounded="lg" @click="nextProduct"></v-btn>
                                </div>
                                <div class="mt-4" style="width: 100%;">
                                    <v-btn block color="blue-darken-4" size="large" rounded="lg" class="mt-4" :to="`/product/${activeProduct.id}`">
                                        MUA NGAY
                                    </v-btn>
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
                <div class="bar-item font-weight-black text-uppercase">AeroStride OS v3.4.2 // {{ activeProduct.category }}</div>
            </div>
        </template>
    </section>
</template>

<style scoped lang="scss">
@import '@/scss/pages/landing/_how-section.scss';
</style>
