<script setup>
import { onMounted, defineAsyncComponent } from 'vue';
import { PATH } from '@/router/routePaths';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import Preloader from '@/components/common/Preloader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import { useLandingPage } from '@/composables/useLandingPage';
import { useLandingCatalog } from '@/composables/useLandingCatalog';
import { useSeoMeta } from '@/composables/useSeoMeta';

import MainHeader from '@/components/shared/MainHeader.vue';
import HeroSection from './sections/HeroSection.vue';


// Lazy load sections below the fold
const PromoBannerSection = defineAsyncComponent(() => import('./sections/PromoBannerSection.vue'));
const MonthlyProductsSection = defineAsyncComponent(() => import('./sections/MonthlyProductsSection.vue'));
const ProblemSection = defineAsyncComponent(() => import('./sections/ProblemSection.vue'));
const FeaturesSection = defineAsyncComponent(() => import('./sections/FeaturesSection.vue'));
const SocialSection = defineAsyncComponent(() => import('./sections/SocialSection.vue'));
const HowSection = defineAsyncComponent(() => import('./sections/HowSection.vue'));
const FaqSection = defineAsyncComponent(() => import('./sections/FaqSection.vue'));
const CtaSection = defineAsyncComponent(() => import('./sections/CtaSection.vue'));

const {
    activeSection,
    handleLogout,
    handlePreloaderFinish,
    isLoading,
    isLoggedIn,
    LANDING_SECTIONS,
    isSectionWarm,
    mouseX,
    mouseY,
    onScroll
} = useLandingPage();

const { heroProduct, isCatalogLoading, howProducts, topVariantsByQty, problemProducts, landingProducts } = useLandingCatalog(activeSection);

// SEO
const { setSeoMeta } = useSeoMeta();
onMounted(() => {
    setSeoMeta({
        title: 'Giày Thể Thao Chính Hãng - Mua Sắm Online',
        description: 'AeroStride - Cửa hàng giày thể thao chính hãng hàng đầu Việt Nam. Mua sắm giày Nike, Adidas, Puma với giá tốt nhất, giao hàng nhanh toàn quốc, đổi trả miễn phí 30 ngày.'
    });
});

const scrollToSection = (index) => {
    activeSection.value = index;
    const container = document.querySelector('.landing-scroll-container');
    if (container) {
        container.scrollTo({
            top: index * window.innerHeight,
            behavior: 'smooth'
        });
    }
};
</script>

<template>
    <div class="app-container">
        <transition name="preloader-fade">
            <Preloader v-if="isLoading" @finish="handlePreloaderFinish" />
        </transition>

        <!-- Section Dots Indicator -->
        <div class="section-nav" v-if="!isLoading">
            <div v-for="(s, i) in LANDING_SECTIONS" :key="i" class="dot" :class="{ active: activeSection === i }"
                @click="scrollToSection(i)">
                <span class="tooltip">{{ s }}</span>
            </div>
        </div>

        <div class="landing-scroll-container" v-if="!isLoading" @scroll="onScroll">
            <!-- Premium Global Header & Mega Menu -->
            <MainHeader v-if="!isLoading" />

            <!-- Modular Sections -->
            <HeroSection :active="activeSection === 0" :warm="isSectionWarm(0)" :mouseX="mouseX" :mouseY="mouseY" :product="heroProduct" />
            <PromoBannerSection :active="activeSection === 1" :warm="isSectionWarm(1)" :products="landingProducts" />
            <MonthlyProductsSection
                :active="activeSection === 2"
                :warm="isSectionWarm(2)"
                :variants="topVariantsByQty"
                title="SẢN PHẨM NỔI BẬT"
                subtitle="Được yêu thích nhất tháng này"
            />
            <ProblemSection :active="activeSection === 3" :warm="isSectionWarm(3)" :products="problemProducts" />
            <FeaturesSection :active="activeSection === 4" :warm="isSectionWarm(4)" />
            <SocialSection :active="activeSection === 5" :warm="isSectionWarm(5)" />
            <HowSection :active="activeSection === 6" :warm="isSectionWarm(6)" :products="howProducts" />
            <FaqSection :active="activeSection === 7" :warm="isSectionWarm(7)" />
            <CtaSection :active="activeSection === 8" :warm="isSectionWarm(8)" />

            <footer class="footer-landing py-10 text-center text-grey-darken-1 bg-white border-t">
                <LogoClient class="mb-4 d-inline-block" style="max-width: 150px" />
                <p>&copy; 2026 AeroStride All rights reserved.</p>
            </footer>
        </div>

        <!-- Cart Drawer -->
        

        <!-- Global Chat System -->
        <CustomerChat v-if="!isLoading" />
    </div>
</template>

<style scoped lang="scss">
.landing-scroll-container {
    height: 100vh;
    overflow-y: scroll;
    scroll-behavior: smooth;
    overscroll-behavior-y: contain;
    -ms-overflow-style: none;
    scrollbar-width: none;
    /* Native CSS Scroll Snapping for smooth performance */
    scroll-snap-type: y mandatory;
    -webkit-overflow-scrolling: touch;

    &::-webkit-scrollbar {
        display: none;
    }
}

.landing-scroll-container :deep(.snap-section) {
    /* Performance optimizations */
    contain: content;
    scroll-snap-align: start;
    scroll-snap-stop: always;
    height: 100vh;
    position: relative; /* Remove sticky to prevent GPU overlap lag */
    background: #ffffff;
}

.navbar {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    z-index: 1000;
}

/* Section Navigation Dots */
.section-nav {
    position: fixed;
    right: 40px;
    top: 50%;
    transform: translateY(-50%);
    z-index: 2000;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: rgba(41, 98, 255, 0.2);
    cursor: pointer;
    transition: all 0.3s ease;
    position: relative;

    &.active {
        background: #2962ff;
        transform: scale(1.5);
    }

    &:hover .tooltip {
        opacity: 1;
        transform: translateX(-20px);
    }
}

.tooltip {
    position: absolute;
    right: 30px;
    top: -5px;
    background: #1e293b;
    color: white;
    padding: 4px 12px;
    border-radius: 4px;
    font-size: 10px;
    white-space: nowrap;
    opacity: 0;
    pointer-events: none;
    transition: all 0.3s ease;
}

.preloader-fade-leave-active {
    transition:
        opacity 0.6s cubic-bezier(1, 0, 0, 1),
        transform 0.6s cubic-bezier(1, 0, 0, 1);
}

.preloader-fade-leave-to {
    opacity: 0;
    transform: translateY(-100%);
}

.footer-landing {
    position: relative;
    z-index: 100;
    scroll-snap-align: end;
}
</style>
