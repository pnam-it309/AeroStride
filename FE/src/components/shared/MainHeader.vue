<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue';
import CartDrawer from '@/components/shared/CartDrawer.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { PATH } from '@/router/routePaths';
import { useAuthStore } from '@/stores/authStore';
import { useCartStore } from '@/stores/cartStore';
import { useToastStore } from '@/stores/toastStore';
import { useRouter } from 'vue-router';
import { dichVuSanPhamPublic } from '@/services/public/dichVuSanPhamPublic';
import { dichVuFile } from '@/services/core/dichVuFile';

const authStore = useAuthStore();
const cartStore = useCartStore();
const toastStore = useToastStore();
const router = useRouter();

import { getFavoriteIds } from '@/utils/favoritesUtil';

// ─── Favorites ───────────────────────────────────────────────────────────────
const favoriteCount = ref(0);
const updateFavoriteCount = () => {
    favoriteCount.value = getFavoriteIds().length;
};
const handleFavoriteClick = () => {
    router.push(PATH.FAVORITES);
};

// ─── Auth ─────────────────────────────────────────────────────────────────────
const handleLogout = async () => {
    await authStore.logout();
    toastStore.showToast('Bạn đã đăng xuất tài khoản thành công.', 'info');
    router.push('/');
};

// ─── Scroll hide/show ─────────────────────────────────────────────────────────
const isScrolled = ref(false);
const isHidden = ref(false);
let lastScrollY = 0;
const handleScroll = () => {
    const y = window.scrollY;
    isScrolled.value = y > 50;
    if (y > lastScrollY && y > 100) {
        isHidden.value = true;
        closeSearch();
    } else if (y < lastScrollY) {
        isHidden.value = false;
    }
    lastScrollY = y;
};
const handleMouseEnter = () => {
    isHidden.value = false;
};

// ─── Nav links ────────────────────────────────────────────────────────────────
const navLinks = [
    { label: 'TRANG CHỦ', path: PATH.LANDING },
    { label: 'SẢN PHẨM', path: PATH.SHOES },
    { label: 'GỢI Ý', path: PATH.AI_RECOMMEND },
    { label: 'GIỚI THIỆU', path: '/gioi-thieu' },
    { label: 'TIN TỨC', path: '/tin-tuc' },
    { label: 'LIÊN HỆ', path: '/lien-he' },
    { label: 'TRA CỨU', path: PATH.TRACK_ORDER }
];

// ─── Live search ──────────────────────────────────────────────────────────────
const searchQuery = ref('');
const searchResults = ref([]);
const isSearchLoading = ref(false);
const isSearchOpen = ref(false);
let searchTimer = null;

const resolveImg = (v) => {
    if (!v) return '';
    if (/^(https?:)?\/\//i.test(v) || v?.startsWith('data:') || v?.startsWith('blob:')) return v;
    return dichVuFile.layUrlFile(v.replace(/^\/+/, ''));
};

const doSearch = async (q) => {
    if (!q || q.trim().length < 2) {
        searchResults.value = [];
        return;
    }
    isSearchLoading.value = true;
    try {
        searchResults.value = await dichVuSanPhamPublic.timKiemNhanh(q.trim(), 6);
    } catch {
        searchResults.value = [];
    } finally {
        isSearchLoading.value = false;
    }
};

watch(searchQuery, (val) => {
    clearTimeout(searchTimer);
    if (!val || val.trim().length < 2) {
        searchResults.value = [];
        return;
    }
    searchTimer = setTimeout(() => doSearch(val), 320);
});

const openSearch = () => {
    isSearchOpen.value = true;
};
const closeSearch = () => {
    isSearchOpen.value = false;
    searchResults.value = [];
};
const onSearchBlur = () => {
    setTimeout(() => {
        closeSearch();
    }, 250);
};

const handleSearchSubmit = () => {
    if (searchQuery.value.trim()) {
        router.push({ path: PATH.SHOES, query: { keyword: searchQuery.value.trim() } });
        closeSearch();
    }
};

const handleResultClick = (product) => {
    router.push(`/product/${product.id}`);
    searchQuery.value = '';
    closeSearch();
};

const clearSearch = () => {
    searchQuery.value = '';
};

const formatPrice = (v) => {
    if (!v && v !== 0) return '';
    return new Intl.NumberFormat('vi-VN').format(v) + ' đ';
};

// ─── Mobile Menu ─────────────────────────────────────────────────────────────
const isMobileMenuOpen = ref(false);
const isMobile = ref(false);

const checkMobile = () => {
    isMobile.value = window.innerWidth <= 768;
    if (!isMobile.value) {
        isMobileMenuOpen.value = false;
    }
};

const toggleMobileMenu = () => {
    isMobileMenuOpen.value = !isMobileMenuOpen.value;
};

const closeMobileMenu = () => {
    isMobileMenuOpen.value = false;
};

const handleMobileNavClick = (path) => {
    closeMobileMenu();
    router.push(path);
};

// ─── Mobile Search ────────────────────────────────────────────────────────────
const isMobileSearchOpen = ref(false);
const toggleMobileSearch = () => {
    isMobileSearchOpen.value = !isMobileSearchOpen.value;
    if (isMobileSearchOpen.value) {
        // auto focus after render
        setTimeout(() => {
            document.querySelector('.mobile-search-input')?.focus();
        }, 100);
    }
};

// ─── Lifecycle ────────────────────────────────────────────────────────────────
onMounted(() => {
    window.addEventListener('scroll', handleScroll);
    window.addEventListener('resize', checkMobile);
    checkMobile();
    updateFavoriteCount();
    window.addEventListener('storage', updateFavoriteCount);
    window.addEventListener('favorites-updated', updateFavoriteCount);
});

onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll);
    window.removeEventListener('resize', checkMobile);
    window.removeEventListener('storage', updateFavoriteCount);
    window.removeEventListener('favorites-updated', updateFavoriteCount);
    clearTimeout(searchTimer);
});
</script>

<template>
    <div class="header-hover-zone" v-if="isHidden" @mouseenter="handleMouseEnter"></div>
    <header class="main-header-system" :class="{ scrolled: isScrolled, 'header-hidden': isHidden }" @mouseenter="handleMouseEnter">
        <!-- Top Announcement Bar -->
        <div class="top-announcement-bar">
            <span class="announcement-text">Hàng chính hãng 100% • Giao hàng miễn phí toàn quốc • Đổi trả trong vòng 30 ngày</span>
        </div>

        <!-- Main Nav -->
        <nav class="main-navbar">
            <!-- Logo -->
            <div class="logo-wrap">
                <LogoClient class="header-logo" />
            </div>

            <!-- Desktop Nav Links -->
            <div class="nav-links">
                <router-link
                    v-for="link in navLinks"
                    :key="link.label"
                    :to="link.path"
                    class="nav-link"
                    :class="{ active: $route.path === link.path || ($route.path.startsWith(link.path) && link.path !== '/') }"
                >
                    {{ link.label }}
                </router-link>
            </div>

            <!-- Desktop Actions (Search Box + Icons) -->
            <div class="nav-actions">
                <!-- Desktop Search Box -->
                <div class="search-box-custom desktop-search">
                    <v-icon size="18" class="search-icon-custom">mdi-magnify</v-icon>
                    <input
                        type="text"
                        placeholder="Tìm kiếm sản phẩm..."
                        class="search-input-custom"
                        v-model="searchQuery"
                        @keyup.enter="handleSearchSubmit"
                        @focus="openSearch"
                        @blur="onSearchBlur"
                    />
                    <v-icon v-if="searchQuery" size="16" class="clear-icon-custom" color="grey-darken-1" @mousedown.prevent="clearSearch">
                        mdi-close-circle
                    </v-icon>
                    <!-- Dropdown results -->
                    <div v-if="isSearchOpen && (searchResults.length > 0 || isSearchLoading)" class="search-dropdown-custom">
                        <div v-if="isSearchLoading" class="search-loading">
                            <v-progress-circular size="16" width="2" indeterminate color="blue-darken-3"></v-progress-circular>
                            <span>Đang tìm...</span>
                        </div>
                        <div
                            v-for="item in searchResults"
                            :key="item.id"
                            class="search-result-item"
                            @mousedown.prevent="handleResultClick(item)"
                        >
                            <div class="result-img-wrap">
                                <img v-if="item.hinhAnh" :src="item.hinhAnh" :alt="item.tenSanPham" class="result-img" />
                                <v-icon v-else size="20" color="grey-lighten-1">mdi-shoe-sneaker</v-icon>
                            </div>
                            <div class="result-info">
                                <div class="result-name">{{ item.tenSanPham }}</div>
                                <div class="result-meta">
                                    <span class="result-brand">{{ item.tenThuongHieu }}</span>
                                    <span class="result-price" v-if="item.giaBanThapNhat">
                                        {{ formatPrice(item.giaBanThapNhat) }}
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="search-footer" v-if="searchResults.length > 0" @mousedown.prevent="handleSearchSubmit">
                            Xem tất cả kết quả
                            <v-icon size="12">mdi-arrow-right</v-icon>
                        </div>
                    </div>
                </div>

                <!-- Heart Icon (Favorites) -->
                <div class="action-icon-btn cursor-pointer" @click="handleFavoriteClick">
                    <v-badge :content="favoriteCount" color="primary" v-if="favoriteCount > 0" offset-x="2" offset-y="2">
                        <v-icon size="26" class="action-icon">mdi-heart-outline</v-icon>
                    </v-badge>
                    <v-icon v-else size="26" class="action-icon">mdi-heart-outline</v-icon>
                </div>

                <!-- Account Icon -->
                <v-menu v-if="authStore.isLoggedIn" location="bottom end" offset="4" transition="slide-y-transition">
                    <template v-slot:activator="{ props: menuProps }">
                        <div v-bind="menuProps" class="action-icon-btn cursor-pointer">
                            <v-icon size="26" class="action-icon">mdi-account-outline</v-icon>
                        </div>
                    </template>
                    <v-list density="compact" width="180" class="rounded-lg mt-2 border elevation-1">
                        <v-list-item prepend-icon="mdi-account-outline" title="Tài khoản" :to="PATH.PROFILE"></v-list-item>
                        <v-list-item prepend-icon="mdi-package-variant-closed" title="Đơn mua" :to="PATH.ORDERS"></v-list-item>
                        <v-divider class="my-1"></v-divider>
                        <v-list-item prepend-icon="mdi-logout" title="Đăng xuất" @click="handleLogout" color="error"></v-list-item>
                    </v-list>
                </v-menu>
                <div v-else class="action-icon-btn cursor-pointer" @click="router.push(PATH.LOGIN)">
                    <v-icon size="26" class="action-icon">mdi-account-outline</v-icon>
                </div>

                <!-- Cart Icon -->
                <div class="action-icon-btn cursor-pointer" @click="cartStore.openDrawer()">
                    <v-badge :content="cartStore.cartCount" color="primary" v-if="cartStore.cartCount > 0" offset-x="2" offset-y="2">
                        <v-icon size="26" class="action-icon">mdi-shopping-outline</v-icon>
                    </v-badge>
                    <v-icon v-else size="26" class="action-icon">mdi-shopping-outline</v-icon>
                </div>

                <!-- Mobile: Search icon + Hamburger (shown only on mobile) -->
                <div class="mobile-only-actions">
                    <div class="action-icon-btn cursor-pointer" @click="toggleMobileSearch">
                        <v-icon size="24" class="action-icon">{{ isMobileSearchOpen ? 'mdi-close' : 'mdi-magnify' }}</v-icon>
                    </div>
                    <div class="action-icon-btn cursor-pointer hamburger-btn" @click="toggleMobileMenu" aria-label="Menu">
                        <v-icon size="24" class="action-icon">{{ isMobileMenuOpen ? 'mdi-close' : 'mdi-menu' }}</v-icon>
                    </div>
                </div>
            </div>
        </nav>

        <!-- Mobile Search Bar (expandable, below navbar) -->
        <transition name="mobile-search-slide">
            <div v-if="isMobileSearchOpen" class="mobile-search-bar">
                <div class="mobile-search-inner">
                    <v-icon size="18" color="#637085">mdi-magnify</v-icon>
                    <input
                        type="text"
                        placeholder="Tìm kiếm sản phẩm..."
                        class="mobile-search-input"
                        v-model="searchQuery"
                        @keyup.enter="handleSearchSubmit(); isMobileSearchOpen = false"
                        @focus="openSearch"
                        @blur="onSearchBlur"
                    />
                    <v-icon v-if="searchQuery" size="16" color="grey-darken-1" @mousedown.prevent="clearSearch" style="cursor:pointer">
                        mdi-close-circle
                    </v-icon>
                </div>
                <!-- Mobile search dropdown -->
                <div v-if="isSearchOpen && (searchResults.length > 0 || isSearchLoading)" class="mobile-search-dropdown">
                    <div v-if="isSearchLoading" class="search-loading">
                        <v-progress-circular size="16" width="2" indeterminate color="blue-darken-3"></v-progress-circular>
                        <span>Đang tìm...</span>
                    </div>
                    <div
                        v-for="item in searchResults"
                        :key="item.id"
                        class="search-result-item"
                        @mousedown.prevent="handleResultClick(item); isMobileSearchOpen = false"
                    >
                        <div class="result-img-wrap">
                            <img v-if="item.hinhAnh" :src="item.hinhAnh" :alt="item.tenSanPham" class="result-img" />
                            <v-icon v-else size="20" color="grey-lighten-1">mdi-shoe-sneaker</v-icon>
                        </div>
                        <div class="result-info">
                            <div class="result-name">{{ item.tenSanPham }}</div>
                            <div class="result-meta">
                                <span class="result-brand">{{ item.tenThuongHieu }}</span>
                                <span class="result-price" v-if="item.giaBanThapNhat">{{ formatPrice(item.giaBanThapNhat) }}</span>
                            </div>
                        </div>
                    </div>
                    <div class="search-footer" v-if="searchResults.length > 0" @mousedown.prevent="handleSearchSubmit(); isMobileSearchOpen = false">
                        Xem tất cả kết quả
                        <v-icon size="12">mdi-arrow-right</v-icon>
                    </div>
                </div>
            </div>
        </transition>
    </header>

    <!-- Mobile Navigation Drawer Overlay -->
    <transition name="mobile-drawer-fade">
        <div v-if="isMobileMenuOpen" class="mobile-nav-overlay" @click.self="closeMobileMenu">
            <transition name="mobile-drawer-slide">
                <div v-if="isMobileMenuOpen" class="mobile-nav-drawer">
                    <!-- Drawer Header -->
                    <div class="mobile-drawer-header">
                        <LogoClient class="header-logo" />
                        <button class="drawer-close-btn" @click="closeMobileMenu" aria-label="Đóng menu">
                            <v-icon size="22">mdi-close</v-icon>
                        </button>
                    </div>

                    <!-- Nav Links List -->
                    <nav class="mobile-nav-list">
                        <button
                            v-for="link in navLinks"
                            :key="link.label"
                            class="mobile-nav-item"
                            :class="{ active: $route.path === link.path || ($route.path.startsWith(link.path) && link.path !== '/') }"
                            @click="handleMobileNavClick(link.path)"
                        >
                            {{ link.label }}
                        </button>
                    </nav>

                    <!-- Drawer Footer Actions -->
                    <div class="mobile-drawer-footer">
                        <button class="mobile-footer-btn" @click="handleMobileNavClick(PATH.FAVORITES)">
                            <v-icon size="20">mdi-heart-outline</v-icon>
                            <span>Yêu thích</span>
                            <v-badge v-if="favoriteCount > 0" :content="favoriteCount" color="primary" inline></v-badge>
                        </button>
                        <button class="mobile-footer-btn" @click="authStore.isLoggedIn ? handleMobileNavClick(PATH.PROFILE) : handleMobileNavClick(PATH.LOGIN)">
                            <v-icon size="20">mdi-account-outline</v-icon>
                            <span>{{ authStore.isLoggedIn ? 'Tài khoản' : 'Đăng nhập' }}</span>
                        </button>
                        <button class="mobile-footer-btn" @click="closeMobileMenu(); cartStore.openDrawer()">
                            <v-icon size="20">mdi-shopping-outline</v-icon>
                            <span>Giỏ hàng</span>
                            <v-badge v-if="cartStore.cartCount > 0" :content="cartStore.cartCount" color="primary" inline></v-badge>
                        </button>
                    </div>
                </div>
            </transition>
        </div>
    </transition>

    <!-- Include Cart Drawer globally -->
    <CartDrawer />
</template>

<style scoped lang="scss">
.main-header-system {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    z-index: 1000;
    background: #ffffff;
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);

    &.header-hidden {
        transform: translateY(-100%);
    }
}

.header-hover-zone {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 20px;
    z-index: 1001;
}

/* ── Announcement Bar ────────────────────────────────────────────── */
.top-announcement-bar {
    width: 100%;
    height: 36px;
    background: #0a1329;
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 16px;
    overflow: hidden;
}

.announcement-text {
    font-family: 'Inter', sans-serif;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.5px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    text-align: center;
}

/* ── Main Navbar ──────────────────────────────────────────────────── */
.main-navbar {
    height: 84px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 72px;
    background: #ffffff;
}

/* Header Logo */
.logo-wrap {
    display: flex;
    align-items: center;
    flex-shrink: 0;
}

:deep(.header-logo) {
    display: inline-flex;
    align-items: center;
}

/* ── Desktop Nav Links ────────────────────────────────────────────── */
.nav-links {
    display: flex;
    align-items: center;
    gap: 32px;
}

.nav-link {
    font-family: 'Inter', sans-serif;
    font-size: 13px;
    font-weight: 600;
    color: #0a1329;
    text-decoration: none;
    letter-spacing: 0.5px;
    transition: color 0.2s ease;
    padding: 8px 0;
    position: relative;

    &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 0;
        width: 100%;
        height: 2px;
        background: #2962ff;
        transform: scaleX(0);
        transition: transform 0.25s ease;
        transform-origin: center;
    }

    &:hover {
        color: #2962ff;
        &::after { transform: scaleX(1); }
    }

    &.active {
        color: #2962ff;
        &::after { transform: scaleX(1); }
    }
}

/* ── Actions area ─────────────────────────────────────────────────── */
.nav-actions {
    display: flex;
    align-items: center;
    gap: 20px;
}

/* Mobile-only elements hidden on desktop */
.mobile-only-actions {
    display: none;
}

/* ── Search Box (desktop) ─────────────────────────────────────────── */
.search-box-custom {
    position: relative;
    width: 230px;
    height: 44px;
    background: #f5f7fa;
    border-radius: 22px;
    display: flex;
    align-items: center;
    padding: 0 16px;
    border: 1px solid transparent;
    transition: all 0.3s ease;

    &:focus-within {
        background: #ffffff;
        border-color: #2962ff;
        box-shadow: 0 4px 12px rgba(41, 98, 255, 0.08);
    }
}

.search-icon-custom { color: #637085; }

.search-input-custom {
    border: none;
    background: transparent;
    outline: none;
    width: 100%;
    margin-left: 8px;
    font-family: 'Inter', sans-serif;
    font-size: 13px;
    font-weight: 400;
    color: #0a1329;

    &::placeholder { color: #637085; }
}

.clear-icon-custom {
    cursor: pointer;
    opacity: 0.6;
    transition: opacity 0.2s;
    &:hover { opacity: 1; }
}

/* ── Search Dropdown ──────────────────────────────────────────────── */
.search-dropdown-custom {
    position: absolute;
    top: calc(100% + 8px);
    right: 0;
    width: 340px;
    background: #ffffff;
    border-radius: 16px;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
    border: 1px solid rgba(0, 0, 0, 0.05);
    overflow: hidden;
    z-index: 2000;
}

.search-loading {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 14px 16px;
    font-size: 0.85rem;
    color: #888;
}

.search-result-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 16px;
    cursor: pointer;
    transition: background 0.15s;

    &:hover { background: #f8fafc; }
}

.result-img-wrap {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    background: #f5f7fa;
    overflow: hidden;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
}

.result-img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    padding: 2px;
}

.result-info {
    flex: 1;
    min-width: 0;
}

.result-name {
    font-size: 0.85rem;
    font-weight: 700;
    color: #0a1329;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.result-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 2px;
}

.result-brand {
    font-size: 0.72rem;
    color: #637085;
    font-weight: 600;
}

.result-price {
    font-size: 0.78rem;
    font-weight: 800;
    color: #2962ff;
}

.search-footer {
    padding: 12px 16px;
    font-size: 0.8rem;
    font-weight: 700;
    color: #2962ff;
    cursor: pointer;
    border-top: 1px solid #f5f7fa;
    display: flex;
    align-items: center;
    justify-content: space-between;

    &:hover { background: rgba(41, 98, 255, 0.04); }
}

/* ── Icon Buttons ─────────────────────────────────────────────────── */
.action-icon-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 42px;
    height: 42px;
    border-radius: 50%;
    color: #0a1329;
    transition: background-color 0.2s ease, color 0.2s ease;

    &:hover {
        background-color: #f5f7fa;
        color: #2962ff;
    }
}

.action-icon {
    font-size: 26px !important;
    width: 26px !important;
    height: 26px !important;
}

/* ── Mobile Search Bar ────────────────────────────────────────────── */
.mobile-search-bar {
    background: #ffffff;
    border-top: 1px solid rgba(0, 0, 0, 0.06);
    padding: 10px 16px 12px;
}

.mobile-search-inner {
    display: flex;
    align-items: center;
    gap: 10px;
    background: #f5f7fa;
    border-radius: 22px;
    padding: 0 16px;
    height: 42px;
    border: 1px solid transparent;
    transition: all 0.2s;

    &:focus-within {
        background: #fff;
        border-color: #2962ff;
        box-shadow: 0 4px 12px rgba(41, 98, 255, 0.08);
    }
}

.mobile-search-input {
    flex: 1;
    border: none;
    background: transparent;
    outline: none;
    font-family: 'Inter', sans-serif;
    font-size: 14px;
    color: #0a1329;

    &::placeholder { color: #637085; }
}

.mobile-search-dropdown {
    margin-top: 8px;
    background: #ffffff;
    border-radius: 16px;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
    border: 1px solid rgba(0, 0, 0, 0.05);
    overflow: hidden;
}

/* ── Mobile Drawer Overlay ────────────────────────────────────────── */
.mobile-nav-overlay {
    position: fixed;
    inset: 0;
    background: rgba(10, 19, 41, 0.5);
    z-index: 1200;
    backdrop-filter: blur(2px);
}

.mobile-nav-drawer {
    position: fixed;
    top: 0;
    right: 0;
    width: min(320px, 85vw);
    height: 100%;
    background: #ffffff;
    display: flex;
    flex-direction: column;
    box-shadow: -8px 0 32px rgba(0, 0, 0, 0.12);
    overflow-y: auto;
    overscroll-behavior: contain;
}

.mobile-drawer-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 20px 16px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.drawer-close-btn {
    background: #f5f7fa;
    border: none;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #0a1329;
    transition: background 0.2s;

    &:hover { background: #e8edf5; }
}

.mobile-nav-list {
    display: flex;
    flex-direction: column;
    padding: 12px 0;
    flex: 1;
}

.mobile-nav-item {
    background: none;
    border: none;
    text-align: left;
    padding: 14px 24px;
    font-family: 'Inter', sans-serif;
    font-size: 14px;
    font-weight: 600;
    color: #0a1329;
    letter-spacing: 0.5px;
    cursor: pointer;
    transition: background 0.15s, color 0.15s;
    border-left: 3px solid transparent;

    &:hover {
        background: #f5f7fa;
        color: #2962ff;
        border-left-color: #2962ff;
    }

    &.active {
        background: rgba(41, 98, 255, 0.05);
        color: #2962ff;
        border-left-color: #2962ff;
    }
}

.mobile-drawer-footer {
    padding: 16px;
    border-top: 1px solid rgba(0, 0, 0, 0.06);
    display: flex;
    gap: 8px;
}

.mobile-footer-btn {
    flex: 1;
    background: #f5f7fa;
    border: 1px solid rgba(229, 235, 245, 0.8);
    border-radius: 12px;
    padding: 12px 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    font-family: 'Inter', sans-serif;
    font-size: 11px;
    font-weight: 600;
    color: #0a1329;
    transition: all 0.2s;

    &:hover {
        background: rgba(41, 98, 255, 0.06);
        border-color: rgba(41, 98, 255, 0.2);
        color: #2962ff;
    }
}

/* ── Transition Animations ────────────────────────────────────────── */
.mobile-search-slide-enter-active,
.mobile-search-slide-leave-active {
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    overflow: hidden;
}
.mobile-search-slide-enter-from,
.mobile-search-slide-leave-to {
    opacity: 0;
    max-height: 0;
}
.mobile-search-slide-enter-to,
.mobile-search-slide-leave-from {
    opacity: 1;
    max-height: 200px;
}

.mobile-drawer-fade-enter-active,
.mobile-drawer-fade-leave-active {
    transition: opacity 0.3s ease;
}
.mobile-drawer-fade-enter-from,
.mobile-drawer-fade-leave-to { opacity: 0; }
.mobile-drawer-fade-enter-to,
.mobile-drawer-fade-leave-from { opacity: 1; }

.mobile-drawer-slide-enter-active,
.mobile-drawer-slide-leave-active {
    transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.mobile-drawer-slide-enter-from,
.mobile-drawer-slide-leave-to { transform: translateX(100%); }
.mobile-drawer-slide-enter-to,
.mobile-drawer-slide-leave-from { transform: translateX(0); }

/* ── Responsive Breakpoints ───────────────────────────────────────── */
@media (max-width: 1200px) {
    .main-navbar { padding: 0 40px; }
    .nav-links { gap: 20px; }
}

@media (max-width: 1024px) {
    .main-navbar { padding: 0 24px; }
    .nav-links { gap: 14px; }
    .nav-link { font-size: 12px; }
    .search-box-custom { width: 180px; }
}

@media (max-width: 900px) {
    .nav-links { gap: 10px; }
    .nav-link { font-size: 11px; letter-spacing: 0; }
    .search-box-custom { width: 150px; }
    .nav-actions { gap: 12px; }
}

/* Tablet & Mobile: hide desktop search + nav links, show hamburger */
@media (max-width: 768px) {
    .main-navbar {
        padding: 0 16px;
        height: 64px;
    }

    /* Hide desktop-only elements */
    .nav-links { display: none; }
    .desktop-search { display: none; }

    /* Hide per-icon action buttons that are already in footer drawer */
    .nav-actions > .action-icon-btn:not(.hamburger-btn) {
        display: none;
    }

    /* Show mobile-specific controls */
    .mobile-only-actions {
        display: flex;
        align-items: center;
        gap: 4px;
    }

    /* Announcement bar: shorter text wraps on very small screens */
    .top-announcement-bar {
        height: auto;
        min-height: 32px;
        padding: 6px 12px;
    }

    .announcement-text {
        font-size: 11px;
        white-space: normal;
        line-height: 1.4;
    }
}

@media (max-width: 480px) {
    .announcement-text { font-size: 10px; }
    .main-navbar { padding: 0 12px; }
}
</style>
