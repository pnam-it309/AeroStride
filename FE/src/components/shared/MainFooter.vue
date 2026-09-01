<template>
    <div class="main-footer-wrapper">
        <!-- 1. Brand Showcase Carousel Section (Seamless Infinite Loop) -->
        <section class="brand-showcase-section bg-white pt-12 pb-8 border-t">
            <v-container fluid class="px-4 px-md-8">
                <div class="text-center mb-8">
                    <h2 class="brand-section-title font-weight-black text-slate-900 text-uppercase tracking-wider">
                        Các Thương Hiệu Có Tại AEROSTRIDE
                    </h2>
                    <div class="title-accent-bar mx-auto mt-3"></div>
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
                    <div class="brand-track-group">
                        <!-- First Track -->
                        <div class="brand-track" :class="{ 'is-paused': isDragging || isHovered }">
                            <div
                                v-for="(brand, idx) in brands"
                                :key="'track1-' + idx"
                                class="brand-card-item"
                                @click="handleBrandClick(brand)"
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

                        <!-- Second Track for Perfect Seamless Loop -->
                        <div class="brand-track" :class="{ 'is-paused': isDragging || isHovered }" aria-hidden="true">
                            <div
                                v-for="(brand, idx) in brands"
                                :key="'track2-' + idx"
                                class="brand-card-item"
                                @click="handleBrandClick(brand)"
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
                </div>
            </v-container>
        </section>

        <!-- 2. Clean Modern 3-Column Footer (Phóng to chữ & Giãn dòng thoáng đãng) -->
        <footer class="main-footer-body bg-white text-slate-800 border-t py-16">
            <v-container>
                <v-row class="ga-y-10 justify-space-between">
                    <!-- Cột 1: Thông Tin Cửa Hàng -->
                    <v-col cols="12" sm="6" md="4" class="footer-col">
                        <h3 class="footer-col-title font-weight-black text-slate-900 mb-5">
                            Thông Tin Cửa Hàng
                        </h3>
                        <p class="footer-text-large leading-loose text-slate-600 mb-6">
                            <strong class="text-slate-900 font-weight-bold">AEROSTRIDE</strong> - Hệ thống phân phối giày thể thao chính hãng hàng đầu Việt Nam. Giờ mở cửa: <strong class="text-slate-900">8:00 - 22:00</strong> tất cả các ngày trong tuần (Thứ 2 - Chủ Nhật).
                        </p>
                        
                        <!-- Logo with italic blue gradient brand name -->
                        <div class="footer-brand-badge-box d-flex align-center ga-3 pa-3 rounded-xl border bg-slate-50">
                            <div class="logo-box flex-shrink-0">
                                <img src="@/assets/images/logos/logoclient.jpg" alt="AeroStride" class="footer-logo-img" />
                            </div>
                            <div class="d-flex flex-column">
                                <span class="footer-brand-name-italic">AEROSTRIDE</span>
                                <span class="footer-brand-sub">AUTHENTIC SPORT SHOES</span>
                            </div>
                        </div>
                    </v-col>

                    <!-- Cột 2: Dịch Vụ Khách Hàng -->
                    <v-col cols="12" sm="6" md="3" class="footer-col">
                        <h3 class="footer-col-title font-weight-black text-slate-900 mb-5">
                            Dịch Vụ Khách Hàng
                        </h3>
                        <ul class="footer-nav-list list-unstyled d-flex flex-column ga-3">
                            <li>
                                <router-link :to="PATH.SHOES" class="footer-nav-link footer-text-large">
                                    <v-icon size="14" class="mr-2" color="#1e257c">mdi-chevron-right</v-icon>Tìm kiếm sản phẩm
                                </router-link>
                            </li>
                            <li>
                                <router-link to="/about" class="footer-nav-link footer-text-large">
                                    <v-icon size="14" class="mr-2" color="#1e257c">mdi-chevron-right</v-icon>Giới thiệu về chúng tôi
                                </router-link>
                            </li>
                            <li>
                                <router-link to="/help" class="footer-nav-link footer-text-large">
                                    <v-icon size="14" class="mr-2" color="#1e257c">mdi-chevron-right</v-icon>Chính sách đổi trả 30 ngày
                                </router-link>
                            </li>
                            <li>
                                <router-link to="/help" class="footer-nav-link footer-text-large">
                                    <v-icon size="14" class="mr-2" color="#1e257c">mdi-chevron-right</v-icon>Chính sách bảo mật
                                </router-link>
                            </li>
                            <li>
                                <router-link to="/help" class="footer-nav-link footer-text-large">
                                    <v-icon size="14" class="mr-2" color="#1e257c">mdi-chevron-right</v-icon>Điều khoản dịch vụ
                                </router-link>
                            </li>
                            <li>
                                <router-link to="/contact" class="footer-nav-link footer-text-large">
                                    <v-icon size="14" class="mr-2" color="#1e257c">mdi-chevron-right</v-icon>Liên hệ & Hỗ trợ CSKH
                                </router-link>
                            </li>
                        </ul>
                    </v-col>

                    <!-- Cột 3: Thông Tin Liên Hệ -->
                    <v-col cols="12" sm="12" md="5" class="footer-col">
                        <h3 class="footer-col-title font-weight-black text-slate-900 mb-5">
                            Thông Tin Liên Hệ
                        </h3>
                        <div class="d-flex flex-column ga-4 footer-text-large text-slate-700">
                            <div class="d-flex align-start ga-3 leading-relaxed">
                                <v-icon size="22" color="#1e257c" class="flex-shrink-0 mt-1">mdi-map-marker-radius</v-icon>
                                <span><strong class="text-slate-900">Trụ sở & Cửa hàng chính:</strong> 21A Lê Đức Thọ, Phường Mỹ Đình 2, Quận Nam Từ Liêm, Hà Nội</span>
                            </div>
                            <div class="d-flex align-center ga-3">
                                <v-icon size="22" color="#1e257c" class="flex-shrink-0">mdi-phone-in-talk</v-icon>
                                <span>Hotline tư vấn: <a href="tel:0986525959" class="text-primary font-weight-black hover:underline">0986 525 959</a></span>
                            </div>
                            <div class="d-flex align-center ga-3">
                                <v-icon size="22" color="#1e257c" class="flex-shrink-0">mdi-cellphone-message</v-icon>
                                <span>Zalo CSKH 24/7: <strong class="text-slate-900 font-weight-bold">0986 525 959</strong></span>
                            </div>
                            <div class="d-flex align-center ga-3">
                                <v-icon size="22" color="#1e257c" class="flex-shrink-0">mdi-email-fast-outline</v-icon>
                                <span>Email tiếp nhận: <a href="mailto:support@aerostride.vn" class="text-slate-800 hover:text-primary font-weight-medium">support@aerostride.vn</a></span>
                            </div>
                        </div>
                    </v-col>
                </v-row>
            </v-container>
        </footer>

        <!-- 3. Bottom Bar -->
        <div class="footer-copyright-bar bg-slate-100 py-4 text-center text-body-2 text-slate-600 border-t">
            <v-container>
                © 2026 <strong class="text-slate-900">AEROSTRIDE</strong>. Tất cả quyền được bảo lưu. Giày Thể Thao Chính Hãng Việt Nam.
            </v-container>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { PATH } from '@/router/routePaths';
import { dichVuGioiThieu } from '@/services/public/dichVuGioiThieu';

const router = useRouter();
const marqueeContainer = ref(null);
const isHovered = ref(false);
const isDragging = ref(false);
let startX = 0;
let scrollLeft = 0;
let dragDistance = 0;

const brands = ref([
    {
        id: '',
        name: 'NIKE',
        image: 'https://images.unsplash.com/photo-1552346154-21d32810aba3?auto=format&fit=crop&q=80&w=500'
    },
    {
        id: '',
        name: 'ADIDAS',
        image: 'https://images.unsplash.com/photo-1587563871167-1ee9c731aefb?auto=format&fit=crop&q=80&w=500'
    },
    {
        id: '',
        name: 'PUMA',
        image: 'https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&q=80&w=500'
    },
    {
        id: '',
        name: 'KAMITO',
        image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&q=80&w=500'
    },
    {
        id: '',
        name: 'MIZUNO',
        image: 'https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?auto=format&fit=crop&q=80&w=500'
    },
    {
        id: '',
        name: 'ASICS',
        image: 'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&q=80&w=500'
    },
    {
        id: '',
        name: 'MOLTEN',
        image: 'https://images.unsplash.com/photo-1519766304817-4f37bda74a29?auto=format&fit=crop&q=80&w=500'
    },
    {
        id: '',
        name: 'ĐỘNG LỰC',
        image: 'https://images.unsplash.com/photo-1579338559194-a162d19bf842?auto=format&fit=crop&q=80&w=500'
    }
]);

const fetchBrandsFromDb = async () => {
    try {
        const res = await dichVuGioiThieu.layDanhSachThuongHieu();
        if (res?.data && Array.isArray(res.data) && res.data.length > 0) {
            brands.value = res.data;
        }
    } catch (e) {
        console.error('Lỗi tải danh sách thương hiệu từ DB:', e);
    }
};

const handleBrandClick = (brand) => {
    if (dragDistance > 8) return;
    const query = {};
    if (brand?.id) {
        query.thuongHieuId = brand.id;
    }
    if (brand?.name) {
        query.keyword = brand.name;
    }
    router.push({
        path: PATH.SHOES,
        query
    });
};

onMounted(() => {
    fetchBrandsFromDb();
});

// Mouse Drag-to-scroll Handlers
const startDrag = (e) => {
    isDragging.value = true;
    dragDistance = 0;
    startX = e.pageX - (marqueeContainer.value?.offsetLeft || 0);
    scrollLeft = marqueeContainer.value?.scrollLeft || 0;
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
    if (!isDragging.value || !marqueeContainer.value) return;
    e.preventDefault();
    const x = e.pageX - marqueeContainer.value.offsetLeft;
    const walk = (x - startX) * 1.5;
    dragDistance = Math.abs(walk);
    marqueeContainer.value.scrollLeft = scrollLeft - walk;
};

// Touch Handlers for Mobile
let touchStartX = 0;
let touchScrollLeft = 0;

const onTouchStart = (e) => {
    isDragging.value = true;
    dragDistance = 0;
    touchStartX = e.touches[0].pageX - (marqueeContainer.value?.offsetLeft || 0);
    touchScrollLeft = marqueeContainer.value?.scrollLeft || 0;
};

const onTouchMove = (e) => {
    if (!isDragging.value || !marqueeContainer.value) return;
    const x = e.touches[0].pageX - marqueeContainer.value.offsetLeft;
    const walk = (x - touchStartX) * 1.5;
    dragDistance = Math.abs(walk);
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
    font-size: clamp(1.35rem, 2.5vw, 1.95rem);
    letter-spacing: 0.06em;
}

.title-accent-bar {
    width: 70px;
    height: 4px;
    background: #1e257c;
    border-radius: 3px;
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
    padding: 10px 0;

    &:active {
        cursor: grabbing;
    }
}

.brand-track-group {
    display: flex;
    width: max-content;
}

.brand-track {
    display: flex;
    gap: 18px;
    padding-right: 18px;
    flex-shrink: 0;
    animation: seamlessScroll 25s linear infinite;
    will-change: transform;

    &.is-paused {
        animation-play-state: paused;
    }
}

@keyframes seamlessScroll {
    from {
        transform: translateX(0);
    }
    to {
        transform: translateX(-100%);
    }
}

.brand-card-item {
    flex: 0 0 210px;
    width: 210px;
    height: 190px;
    border-radius: 14px;
    overflow: hidden;
    position: relative;
    cursor: pointer;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
    transition: transform 0.3s ease, box-shadow 0.3s ease;

    &:hover {
        transform: translateY(-4px);
        box-shadow: 0 12px 24px rgba(0, 0, 0, 0.16);
    }

    @media (max-width: 600px) {
        flex: 0 0 155px;
        width: 155px;
        height: 145px;
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
    font-size: 1.15rem;
    font-weight: 900;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.8), 0 0 12px rgba(0, 0, 0, 0.5);
    border: 2px solid rgba(255, 255, 255, 0.85);
    padding: 6px 16px;
    border-radius: 8px;
    background: rgba(0, 0, 0, 0.4);
    backdrop-filter: blur(2px);
}

/* ── Footer 3 Columns (Phóng to & Giãn dòng thoáng) ── */
.footer-col-title {
    font-size: 1.25rem;
    letter-spacing: -0.01em;
}

.footer-text-large {
    font-size: 1.02rem; /* Phóng to chữ */
    line-height: 1.85; /* Giãn dòng thoáng đãng */
}

.footer-nav-list {
    padding-left: 0;
    margin-bottom: 0;
}

.footer-nav-link {
    color: #475569;
    text-decoration: none;
    transition: all 0.2s ease;
    display: inline-flex;
    align-items: center;

    &:hover {
        color: #1e257c !important;
        transform: translateX(4px);
    }
}

/* Footer Logo Badge Box */
.footer-brand-badge-box {
    border-color: #e2e8f0;
}

.logo-box {
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.footer-logo-img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    mix-blend-mode: multiply;
}

.footer-brand-name-italic {
    font-size: 1.35rem;
    font-weight: 900;
    font-style: italic;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    background: linear-gradient(135deg, #1e257c 0%, #2563eb 60%, #38bdf8 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    line-height: 1.1;
}

.footer-brand-sub {
    font-size: 0.65rem;
    font-weight: 800;
    letter-spacing: 0.15em;
    color: #64748b;
}
</style>
