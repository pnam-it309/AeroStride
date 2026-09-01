<template>
    <div class="main-footer-wrapper">
        <!-- 1. Brand Showcase Carousel Section (White background, no logo icon) -->
        <section class="brand-showcase-section bg-white pt-10 pb-8 border-t border-b">
            <v-container fluid class="px-4 px-md-8">
                <div class="text-center mb-6">
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
                                    <div class="brand-overlay d-flex align-center justify-center pa-3">
                                        <!-- Brand Name Badge -->
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
                                    <div class="brand-overlay d-flex align-center justify-center pa-3">
                                        <!-- Brand Name Badge -->
                                        <span class="brand-name-badge">{{ brand.name }}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </v-container>
        </section>

        <!-- 2. Dark Modern 4-Column Footer (100% Tiếng Việt) -->
        <footer class="main-footer-body dark-footer py-14">
            <v-container>
                <v-row class="ga-y-8 justify-space-between">
                    <!-- Cột 1: VỀ AEROSTRIDE -->
                    <v-col cols="12" sm="6" md="3" class="footer-col">
                        <h4 class="footer-col-header text-white font-weight-black mb-5 text-uppercase tracking-wider">
                            VỀ AEROSTRIDE
                        </h4>
                        <p class="footer-desc-text text-slate-400 mb-5 leading-relaxed">
                            <strong class="text-white">AeroStride</strong> là hệ thống phân phối giày thể thao chính hãng hàng đầu Việt Nam. Tận tâm mang đến trải nghiệm êm ái, bền bỉ và thời thượng cho mọi vận động viên và tín đồ thời trang.
                        </p>
                        <div class="d-flex align-center ga-2">
                            <v-chip size="small" color="#38bdf8" variant="outlined" class="font-weight-bold">
                                100% CHÍNH HÃNG
                            </v-chip>
                            <v-chip size="small" color="#38bdf8" variant="outlined" class="font-weight-bold">
                                ĐỔI TRẢ 30 NGÀY
                            </v-chip>
                        </div>
                    </v-col>

                    <!-- Cột 2: SẢN PHẨM -->
                    <v-col cols="12" sm="6" md="3" class="footer-col">
                        <h4 class="footer-col-header text-white font-weight-black mb-5 text-uppercase tracking-wider">
                            SẢN PHẨM
                        </h4>
                        <ul class="footer-nav-list list-unstyled d-flex flex-column ga-2.5">
                            <li>
                                <router-link :to="{ path: PATH.SHOES, query: { keyword: 'Chạy Bộ' } }" class="footer-dark-link">
                                    Giày Chạy Bộ Nam / Nữ
                                </router-link>
                            </li>
                            <li>
                                <router-link :to="{ path: PATH.SHOES, query: { keyword: 'Sneaker' } }" class="footer-dark-link">
                                    Giày Sneaker Thể Thao
                                </router-link>
                            </li>
                            <li>
                                <router-link :to="{ path: PATH.SHOES, query: { keyword: 'Bóng Đá' } }" class="footer-dark-link">
                                    Giày Bóng Đá & Futsal
                                </router-link>
                            </li>
                            <li>
                                <router-link :to="{ path: PATH.SHOES, query: { keyword: 'Cầu Lông' } }" class="footer-dark-link">
                                    Giày Cầu Lông & Tennis
                                </router-link>
                            </li>
                            <li>
                                <router-link :to="PATH.SHOES" class="footer-dark-link text-cyan-lighten-3 font-weight-bold">
                                    Tất Cả Sản Phẩm Mới Nhất
                                </router-link>
                            </li>
                        </ul>
                    </v-col>

                    <!-- Cột 3: DỊCH VỤ & HỖ TRỢ -->
                    <v-col cols="12" sm="6" md="3" class="footer-col">
                        <h4 class="footer-col-header text-white font-weight-black mb-5 text-uppercase tracking-wider">
                            DỊCH VỤ & HỖ TRỢ
                        </h4>
                        <ul class="footer-nav-list list-unstyled d-flex flex-column ga-2.5">
                            <li>
                                <router-link :to="PATH.ORDERS" class="footer-dark-link">
                                    Tra cứu đơn hàng của bạn
                                </router-link>
                            </li>
                            <li>
                                <router-link to="/about" class="footer-dark-link">
                                    Giới thiệu về chúng tôi
                                </router-link>
                            </li>
                            <li>
                                <router-link to="/help" class="footer-dark-link">
                                    Chính sách đổi trả 30 ngày
                                </router-link>
                            </li>
                            <li>
                                <router-link to="/help" class="footer-dark-link">
                                    Chính sách bảo mật thông tin
                                </router-link>
                            </li>
                            <li>
                                <router-link to="/help" class="footer-dark-link">
                                    Điều khoản dịch vụ & FAQ
                                </router-link>
                            </li>
                        </ul>
                    </v-col>

                    <!-- Cột 4: LIÊN HỆ -->
                    <v-col cols="12" sm="6" md="3" class="footer-col">
                        <h4 class="footer-col-header text-white font-weight-black mb-5 text-uppercase tracking-wider">
                            LIÊN HỆ
                        </h4>
                        <div class="d-flex flex-column ga-3 text-slate-400 footer-contact-block">
                            <div class="d-flex align-start ga-2.5">
                                <v-icon size="18" color="#38bdf8" class="flex-shrink-0 mt-0.5">mdi-map-marker-outline</v-icon>
                                <span class="text-slate-300">21A Lê Đức Thọ, Mỹ Đình 2, Nam Từ Liêm, Hà Nội</span>
                            </div>
                            <div class="d-flex align-center ga-2.5">
                                <v-icon size="18" color="#38bdf8" class="flex-shrink-0">mdi-email-outline</v-icon>
                                <a href="mailto:support@aerostride.vn" class="footer-dark-link">support@aerostride.vn</a>
                            </div>
                            <div class="d-flex align-center ga-2.5">
                                <v-icon size="18" color="#38bdf8" class="flex-shrink-0">mdi-phone-outline</v-icon>
                                <a href="tel:0986525959" class="text-white font-weight-bold hover:text-cyan">0986 525 959</a>
                            </div>
                            <div class="d-flex align-center ga-2.5">
                                <v-icon size="18" color="#38bdf8" class="flex-shrink-0">mdi-clock-time-four-outline</v-icon>
                                <span>08:00 – 22:00 (Thứ 2 – Chủ Nhật)</span>
                            </div>
                        </div>
                    </v-col>
                </v-row>
            </v-container>
        </footer>

        <!-- 3. Bottom Copyright Bar (Tiếng Việt) -->
        <div class="footer-copyright-bar py-4 text-center border-t border-slate-800">
            <v-container class="pa-0">
                <span class="copyright-text text-slate-400">
                    © 2026 Bản quyền thuộc về: <router-link to="/" class="text-white font-weight-bold text-decoration-none hover:text-cyan">AeroStride.vn</router-link> - Giày Thể Thao Chính Hãng Việt Nam
                </span>
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
    font-family: 'Inter', sans-serif;
}

/* ── Brand Section ── */
.brand-section-title {
    font-size: clamp(1.2rem, 2.2vw, 1.7rem);
    letter-spacing: 0.08em;
}

.title-accent-bar {
    width: 60px;
    height: 3px;
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
    padding: 6px 0;

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
    animation: seamlessScroll 55s linear infinite; /* Chạy chậm lại, êm ái hơn */
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
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.09);
    border: 1px solid rgba(226, 232, 240, 0.8);
    transition: transform 0.3s ease, box-shadow 0.3s ease;

    &:hover {
        transform: translateY(-5px);
        box-shadow: 0 14px 28px rgba(30, 37, 124, 0.16);
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
    background: linear-gradient(180deg, rgba(0, 0, 0, 0.15) 0%, rgba(0, 0, 0, 0.75) 100%);
}

.brand-name-badge {
    color: #ffffff;
    font-size: 1.15rem;
    font-weight: 900;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.9);
    border: 2px solid rgba(255, 255, 255, 0.85);
    padding: 6px 16px;
    border-radius: 8px;
    background: rgba(0, 0, 0, 0.45);
    backdrop-filter: blur(2px);
}

/* ── Dark Footer Body (Tone màu thanh lịch theo Ảnh 2) ── */
.dark-footer {
    background-color: #1e222b; /* Tone màu than tối như Ảnh 2 */
}

.footer-col-header {
    font-size: 0.98rem;
    font-weight: 800;
    letter-spacing: 0.08em;
    color: #ffffff;
}

.footer-desc-text {
    font-size: 0.92rem;
    line-height: 1.75;
}

.footer-nav-list {
    padding-left: 0;
    margin-bottom: 0;
}

.footer-dark-link {
    color: #94a3b8;
    text-decoration: none;
    font-size: 0.92rem;
    transition: all 0.2s ease;
    display: inline-block;

    &:hover {
        color: #ffffff !important;
        transform: translateX(3px);
    }
}

.footer-contact-block {
    font-size: 0.92rem;
    line-height: 1.7;
}

/* ── Copyright Bar (Đậm hơn đúng như Ảnh 2) ── */
.footer-copyright-bar {
    background-color: #13161c; /* Tone màu tối hơn cho copyright bar */
}

.copyright-text {
    font-size: 0.88rem;
}
</style>
