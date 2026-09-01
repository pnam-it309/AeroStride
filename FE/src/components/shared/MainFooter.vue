<template>
    <div class="main-footer-wrapper">
        <!-- 1. Brand Showcase Carousel Section -->
        <section class="brand-showcase-section bg-white pt-10 pb-6 border-t">
            <v-container fluid class="px-4 px-md-8">
                <div class="text-center mb-8">
                    <h2 class="brand-section-title font-weight-black text-slate-900 text-uppercase tracking-wider">
                        Các Thương Hiệu Có Tại AEROSTRIDE
                    </h2>
                    <div class="title-accent-bar mx-auto mt-2"></div>
                </div>

                <!-- Infinite Draggable Brand Carousel -->
                <div
                    class="brand-marquee-container"
                    ref="marqueeContainer"
                    @mousedown="startDrag"
                    @mouseup="stopDrag"
                    @mousemove="onDrag"
                    @touchstart="onTouchStart"
                    @touchmove="onTouchMove"
                    @touchend="onTouchEnd"
                    @mouseenter="isHovered = true"
                    @mouseleave="handleMouseLeave"
                >
                    <div class="brand-track" :class="{ 'is-paused': isDragging || isHovered }">
                        <!-- Render 2 loops for infinite smooth loop -->
                        <div
                            v-for="(brand, idx) in duplicatedBrands"
                            :key="idx"
                            class="brand-card-item"
                            @click="handleBrandClick(brand.name)"
                        >
                            <div class="brand-card-inner">
                                <img
                                    :src="brand.image"
                                    :alt="brand.name"
                                    class="brand-bg-image"
                                    loading="lazy"
                                />
                                <div class="brand-overlay">
                                    <span class="brand-name-badge">{{ brand.name }}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </v-container>
        </section>

        <!-- 2. Clean Modern 4-Column Footer (Theo mẫu ảnh) -->
        <footer class="main-footer-body bg-white text-slate-800 border-t py-12">
            <v-container>
                <v-row class="ga-y-8 justify-space-between">
                    <!-- Cột 1: Thông Tin Cửa Hàng -->
                    <v-col cols="12" sm="6" md="3">
                        <h3 class="footer-col-title font-weight-bold text-slate-900 mb-4">
                            Thông Tin Cửa Hàng
                        </h3>
                        <p class="text-body-2 text-slate-600 leading-relaxed mb-3">
                            <strong class="text-slate-900">AEROSTRIDE</strong> - Giày Thể Thao Chính Hãng | Giờ mở cửa: Từ 8 giờ sáng đến 22 giờ tối. (Từ thứ 2 đến chủ nhật)
                        </p>
                        <div class="d-flex align-center ga-2 mt-4">
                            <v-avatar size="38" rounded="lg" color="grey-lighten-4" class="elevation-1 pa-1">
                                <img src="@/assets/images/logos/logo.jpg" alt="AeroStride" style="width: 100%; height: 100%; object-fit: contain;" />
                            </v-avatar>
                            <div>
                                <span class="text-subtitle-2 font-weight-black text-slate-900 d-block leading-tight">AEROSTRIDE</span>
                                <span class="text-caption text-primary font-weight-bold tracking-wider">CHÍNH HÃNG 100%</span>
                            </div>
                        </div>
                    </v-col>

                    <!-- Cột 2: Dịch Vụ Khách Hàng -->
                    <v-col cols="12" sm="6" md="2">
                        <h3 class="footer-col-title font-weight-bold text-slate-900 mb-4">
                            Dịch Vụ Khách Hàng
                        </h3>
                        <ul class="footer-nav-list list-unstyled d-flex flex-column ga-2 text-body-2">
                            <li>
                                <router-link :to="PATH.SHOES" class="footer-nav-link">Tìm kiếm</router-link>
                            </li>
                            <li>
                                <router-link to="/about" class="footer-nav-link">Giới thiệu</router-link>
                            </li>
                            <li>
                                <router-link to="/help" class="footer-nav-link">Chính sách đổi trả</router-link>
                            </li>
                            <li>
                                <router-link to="/help" class="footer-nav-link">Chính sách bảo mật</router-link>
                            </li>
                            <li>
                                <router-link to="/help" class="footer-nav-link">Điều khoản dịch vụ</router-link>
                            </li>
                            <li>
                                <router-link to="/contact" class="footer-nav-link">Liên hệ</router-link>
                            </li>
                        </ul>
                    </v-col>

                    <!-- Cột 3: Thông Tin Liên Hệ -->
                    <v-col cols="12" sm="6" md="4">
                        <h3 class="footer-col-title font-weight-bold text-slate-900 mb-4">
                            Thông tin liên hệ
                        </h3>
                        <div class="d-flex flex-column ga-2.5 text-body-2 text-slate-700">
                            <div class="d-flex align-start ga-2.5">
                                <v-icon size="18" color="primary" class="flex-shrink-0 mt-0.5">mdi-map-marker</v-icon>
                                <span>21A Lê Đức Thọ, Phường Mỹ Đình 2, Quận Nam Từ Liêm, Hà Nội</span>
                            </div>
                            <div class="d-flex align-center ga-2.5">
                                <v-icon size="18" color="primary" class="flex-shrink-0">mdi-phone</v-icon>
                                <a href="tel:0986525959" class="text-slate-900 font-weight-bold hover:text-primary">0986 525 959</a>
                            </div>
                            <div class="d-flex align-center ga-2.5">
                                <v-icon size="18" color="primary" class="flex-shrink-0">mdi-cellphone</v-icon>
                                <span>0986 525 959 (Zalo)</span>
                            </div>
                            <div class="d-flex align-center ga-2.5">
                                <v-icon size="18" color="primary" class="flex-shrink-0">mdi-email</v-icon>
                                <a href="mailto:support@aerostride.vn" class="text-slate-700 hover:text-primary">support@aerostride.vn</a>
                            </div>
                        </div>
                    </v-col>

                    <!-- Cột 4: Theo Dõi Chúng Tôi Tại -->
                    <v-col cols="12" sm="6" md="2">
                        <h3 class="footer-col-title font-weight-bold text-slate-900 mb-4">
                            Theo Dõi Chúng Tôi Tại
                        </h3>
                        <div class="social-icon-stack d-flex flex-row flex-md-column ga-3">
                            <a href="tel:0986525959" class="social-circle-btn hotline" title="Hotline gọi ngay">
                                <v-icon size="20" color="white">mdi-phone</v-icon>
                            </a>
                            <a href="https://zalo.me/0986525959" target="_blank" class="social-circle-btn zalo" title="Chat Zalo">
                                <span class="zalo-text">Zalo</span>
                            </a>
                            <a href="https://maps.google.com" target="_blank" class="social-circle-btn map" title="Xem trên Google Maps">
                                <v-icon size="20" color="white">mdi-map-marker</v-icon>
                            </a>
                            <a href="https://facebook.com" target="_blank" class="social-circle-btn messenger" title="Messenger">
                                <v-icon size="20" color="white">mdi-facebook-messenger</v-icon>
                            </a>
                        </div>
                    </v-col>
                </v-row>
            </v-container>
        </footer>

        <!-- 3. Bottom Bar -->
        <div class="footer-copyright-bar bg-slate-100 py-3 text-center text-caption text-slate-500 border-t">
            <v-container>
                © 2026 <strong>AEROSTRIDE</strong>. Tất cả quyền được bảo lưu. Giày Thể Thao Chính Hãng Việt Nam.
            </v-container>
        </div>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { PATH } from '@/router/routePaths';

const router = useRouter();
const marqueeContainer = ref(null);
const isHovered = ref(false);
const isDragging = ref(false);
let startX = 0;
let scrollLeft = 0;

const brands = [
    {
        name: 'KAMITO',
        image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&q=80&w=500'
    },
    {
        name: 'ADIDAS',
        image: 'https://images.unsplash.com/photo-1587563871167-1ee9c731aefb?auto=format&fit=crop&q=80&w=500'
    },
    {
        name: 'NIKE',
        image: 'https://images.unsplash.com/photo-1552346154-21d32810aba3?auto=format&fit=crop&q=80&w=500'
    },
    {
        name: 'MOLTEN',
        image: 'https://images.unsplash.com/photo-1519766304817-4f37bda74a29?auto=format&fit=crop&q=80&w=500'
    },
    {
        name: 'ĐỘNG LỰC',
        image: 'https://images.unsplash.com/photo-1579338559194-a162d19bf842?auto=format&fit=crop&q=80&w=500'
    },
    {
        name: 'PUMA',
        image: 'https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&q=80&w=500'
    },
    {
        name: 'MIZUNO',
        image: 'https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?auto=format&fit=crop&q=80&w=500'
    },
    {
        name: 'ASICS',
        image: 'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&q=80&w=500'
    }
];

// Duplicate for infinite marquee loop
const duplicatedBrands = computed(() => [...brands, ...brands, ...brands]);

const handleBrandClick = (brandName) => {
    if (isDragging.value) return;
    router.push({
        path: PATH.SHOES,
        query: { keyword: brandName }
    });
};

// Mouse Drag-to-scroll Handlers
const startDrag = (e) => {
    isDragging.value = true;
    startX = e.pageX - marqueeContainer.value.offsetLeft;
    scrollLeft = marqueeContainer.value.scrollLeft;
};

const stopDrag = () => {
    setTimeout(() => {
        isDragging.value = false;
    }, 50);
};

const handleMouseLeave = () => {
    isHovered.value = false;
    stopDrag();
};

const onDrag = (e) => {
    if (!isDragging.value) return;
    e.preventDefault();
    const x = e.pageX - marqueeContainer.value.offsetLeft;
    const walk = (x - startX) * 1.5;
    marqueeContainer.value.scrollLeft = scrollLeft - walk;
};

// Touch Handlers for Mobile
let touchStartX = 0;
let touchScrollLeft = 0;

const onTouchStart = (e) => {
    isDragging.value = true;
    touchStartX = e.touches[0].pageX - marqueeContainer.value.offsetLeft;
    touchScrollLeft = marqueeContainer.value.scrollLeft;
};

const onTouchMove = (e) => {
    if (!isDragging.value) return;
    const x = e.touches[0].pageX - marqueeContainer.value.offsetLeft;
    const walk = (x - touchStartX) * 1.5;
    marqueeContainer.value.scrollLeft = touchScrollLeft - walk;
};

const onTouchEnd = () => {
    setTimeout(() => {
        isDragging.value = false;
    }, 50);
};
</script>

<style scoped lang="scss">
.main-footer-wrapper {
    font-family: inherit;
}

/* ── Brand Section ── */
.brand-section-title {
    font-size: clamp(1.25rem, 2.2vw, 1.75rem);
    letter-spacing: 0.05em;
}

.title-accent-bar {
    width: 60px;
    height: 3px;
    background: #1e257c;
    border-radius: 2px;
}

.brand-marquee-container {
    overflow-x: auto;
    overflow-y: hidden;
    white-space: nowrap;
    cursor: grab;
    user-select: none;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    &::-webkit-scrollbar {
        display: none;
    }
    padding: 8px 0;

    &:active {
        cursor: grabbing;
    }
}

.brand-track {
    display: inline-flex;
    gap: 16px;
    animation: marqueeScroll 35s linear infinite;

    &.is-paused {
        animation-play-state: paused;
    }
}

@keyframes marqueeScroll {
    0% {
        transform: translateX(0);
    }
    100% {
        transform: translateX(calc(-100% / 3));
    }
}

.brand-card-item {
    flex: 0 0 200px;
    width: 200px;
    height: 180px;
    border-radius: 12px;
    overflow: hidden;
    position: relative;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    transition: transform 0.3s ease, box-shadow 0.3s ease;

    &:hover {
        transform: translateY(-4px);
        box-shadow: 0 10px 20px rgba(0, 0, 0, 0.15);
    }

    @media (max-width: 600px) {
        flex: 0 0 150px;
        width: 150px;
        height: 140px;
    }
}

.brand-card-inner {
    width: 100%;
    height: 100%;
    position: relative;
}

.brand-bg-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    transition: transform 0.4s ease;

    .brand-card-item:hover & {
        transform: scale(1.08);
    }
}

.brand-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, rgba(0, 0, 0, 0.1) 0%, rgba(0, 0, 0, 0.75) 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 12px;
}

.brand-name-badge {
    color: #ffffff;
    font-size: 1.1rem;
    font-weight: 900;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.8), 0 0 12px rgba(0, 0, 0, 0.5);
    border: 2px solid rgba(255, 255, 255, 0.85);
    padding: 6px 14px;
    border-radius: 8px;
    background: rgba(0, 0, 0, 0.4);
    backdrop-filter: blur(2px);
}

/* ── Footer 4 Columns ── */
.footer-col-title {
    font-size: 1.05rem;
}

.footer-nav-list {
    padding-left: 0;
    margin-bottom: 0;
}

.footer-nav-link {
    color: #475569;
    text-decoration: none;
    transition: all 0.2s ease;
    display: inline-block;

    &:hover {
        color: #1e257c !important;
        transform: translateX(3px);
    }
}

/* ── Social Stack Buttons (Theo mẫu ảnh) ── */
.social-circle-btn {
    width: 42px;
    height: 42px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    text-decoration: none;
    box-shadow: 0 3px 8px rgba(0, 0, 0, 0.15);
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
        transform: scale(1.12);
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);
    }

    &.hotline {
        background-color: #ef4444; /* Đỏ */
    }

    &.zalo {
        background-color: #0068ff; /* Xanh Zalo */
        color: white;
        font-weight: 900;
        font-size: 0.75rem;
    }

    &.map {
        background-color: #f59e0b; /* Cam */
    }

    &.messenger {
        background-color: #ec4899; /* Hồng tím Messenger */
    }
}

.zalo-text {
    font-weight: 800;
    letter-spacing: -0.5px;
}
</style>
