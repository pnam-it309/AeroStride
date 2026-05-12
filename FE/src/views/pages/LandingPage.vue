<script setup>
import { PATH } from '@/router/routePaths';
import Logo from '@/layouts/full/logo/Logo.vue';
import Preloader from '@/components/common/Preloader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import { useLandingPage } from '@/composables/useLandingPage';
import { useLandingCatalog } from '@/composables/useLandingCatalog';

import MainHeader from '@/components/shared/MainHeader.vue';
import HeroSection from './landing-sections/HeroSection.vue';
import ProblemSection from './landing-sections/ProblemSection.vue';
import FeaturesSection from './landing-sections/FeaturesSection.vue';
import SocialSection from './landing-sections/SocialSection.vue';
import HowSection from './landing-sections/HowSection.vue';
import FaqSection from './landing-sections/FaqSection.vue';
import CtaSection from './landing-sections/CtaSection.vue';

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

const {
    heroProduct,
    isCatalogLoading,
    howProducts,
    problemProducts
} = useLandingCatalog();
</script>

<template>
    <div class="app-container">
        <transition name="preloader-fade">
            <Preloader v-if="isLoading || isCatalogLoading" @finish="handlePreloaderFinish" />
        </transition>

        <!-- Section Dots Indicator -->
        <div class="section-nav" v-if="!isLoading && !isCatalogLoading">
            <div v-for="(s, i) in LANDING_SECTIONS" :key="i" 
                 class="dot" :class="{ active: activeSection === i }"
                 @click="activeSection = i">
                 <span class="tooltip">{{ s.toUpperCase() }}</span>
            </div>
        </div>

        <div class="landing-scroll-container" @scroll="onScroll" v-if="!isLoading && !isCatalogLoading">
            <!-- Premium Global Header & Mega Menu -->
            <MainHeader v-if="!isLoading && !isCatalogLoading" />

            <!-- Modular Sections -->
            <HeroSection :active="activeSection === 0" :mouseX="mouseX" :mouseY="mouseY" :product="heroProduct" />
            <ProblemSection :active="activeSection === 1" :warm="isSectionWarm(1)" :products="problemProducts" />
            <FeaturesSection :active="activeSection === 2" :warm="isSectionWarm(2)" />
            <SocialSection :active="activeSection === 3" :warm="isSectionWarm(3)" />
            <HowSection :active="activeSection === 4" :warm="isSectionWarm(4)" :products="howProducts" />
            <FaqSection :active="activeSection === 5" />
            <CtaSection :active="activeSection === 6" />

            <footer class="footer-landing py-10 text-center text-grey-darken-1 bg-white border-t">
                <Logo class="mb-4 d-inline-block" style="max-width: 150px" />
                <p>&copy; 2026 AeroStride All rights reserved.</p>
            </footer>
        </div>

        <!-- Global Chat System -->
        <CustomerChat v-if="!isLoading && !isCatalogLoading" />
    </div>
</template>

<style scoped lang="scss">
.landing-scroll-container {
    height: 100vh;
    overflow-y: scroll;
    scroll-snap-type: y mandatory;
    scroll-behavior: smooth;
    overscroll-behavior-y: contain;
    -ms-overflow-style: none;
    scrollbar-width: none;
    &::-webkit-scrollbar { display: none; }
}

.landing-scroll-container :deep(.snap-section) {
    contain: layout paint style;
    content-visibility: auto;
    contain-intrinsic-size: 100vh;
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
        background: #2962FF;
        transform: scale(1.5);
    }
    
    &:hover .tooltip { opacity: 1; transform: translateX(-20px); }
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
    transition: opacity 0.6s cubic-bezier(1, 0, 0, 1), transform 0.6s cubic-bezier(1, 0, 0, 1);
}

.preloader-fade-leave-to {
    opacity: 0;
    transform: translateY(-100%);
}

.footer-landing {
    scroll-snap-align: end;
}
</style>
