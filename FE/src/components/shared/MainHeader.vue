<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import CartDrawer from '@/components/shared/CartDrawer.vue';
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

// ─── Favorites ───────────────────────────────────────────────────────────────
const favoriteCount = ref(0);
const updateFavoriteCount = () => {
    const favorites = JSON.parse(localStorage.getItem('aerostride_favorites') || '[]');
    favoriteCount.value = favorites.length;
};
const handleFavoriteClick = () => {
    router.push(PATH.FAVORITES);
};

// ─── Auth ─────────────────────────────────────────────────────────────────────
const handleLogout = async () => {
    await authStore.logout();
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
const handleMouseEnter = () => { isHidden.value = false; };

// ─── Nav links ────────────────────────────────────────────────────────────────
const navLinks = [
    { label: 'TRANG CHỦ', path: PATH.LANDING },
    { label: 'SẢN PHẨM', path: PATH.SHOES },
    { label: 'GIỚI THIỆU', path: '/gioi-thieu' },
    { label: 'TIN TỨC', path: '/tin-tuc' },
    { label: 'LIÊN HỆ', path: '/lien-he' },
    { label: 'TRA CỨU', path: PATH.ORDERS },
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

const openSearch = () => { isSearchOpen.value = true; };
const closeSearch = () => {
    isSearchOpen.value = false;
    searchResults.value = [];
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

const formatPrice = (v) => {
    if (!v && v !== 0) return '';
    return new Intl.NumberFormat('vi-VN').format(v) + ' đ';
};

// ─── Lifecycle ────────────────────────────────────────────────────────────────
onMounted(() => {
    window.addEventListener('scroll', handleScroll);
    updateFavoriteCount();
    window.addEventListener('storage', updateFavoriteCount);
    window.addEventListener('favorites-updated', updateFavoriteCount);
});

onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll);
    window.removeEventListener('storage', updateFavoriteCount);
    window.removeEventListener('favorites-updated', updateFavoriteCount);
    clearTimeout(searchTimer);
});
</script>

<template>
    <div class="header-hover-zone" v-if="isHidden" @mouseenter="handleMouseEnter"></div>
    <header
        class="main-header-system"
        :class="{ scrolled: isScrolled, 'header-hidden': isHidden }"
        @mouseenter="handleMouseEnter"
    >
        <!-- Top Utility Bar -->
        <div class="top-utility-bar">
            <div class="container-custom d-flex align-center justify-space-between px-12 w-100">
                <span class="u-text">📞 Hotline: 1900 6789</span>
                <div class="d-flex align-center gap-4">
                    <router-link to="/he-thong-cua-hang" class="u-link">Hệ thống cửa hàng</router-link>
                    <span class="divider">|</span>
                    <router-link to="/tro-giup" class="u-link">Trợ giúp</router-link>
                    <span class="divider">|</span>
                    <router-link v-if="!authStore.isLoggedIn" :to="PATH.LOGIN" class="u-link d-flex align-center gap-1">
                        <v-icon size="16">mdi-account-circle-outline</v-icon>
                        Đăng nhập
                    </router-link>
                    <v-menu v-else location="bottom end" offset="4" transition="slide-y-transition">
                        <template v-slot:activator="{ props: menuProps }">
                            <div v-bind="menuProps" class="d-flex align-center gap-1 u-link cursor-pointer">
                                <v-icon size="16">mdi-account-circle-outline</v-icon>
                                {{ authStore.user?.hoTen || authStore.user?.username || 'Thành viên' }}
                            </div>
                        </template>
                        <v-list density="compact" width="180" class="rounded-lg mt-2 border shadow-sm">
                            <v-list-item prepend-icon="mdi-account-outline" title="Tài khoản" :to="PATH.PROFILE"></v-list-item>
                            <v-list-item prepend-icon="mdi-package-variant-closed" title="Đơn mua" :to="PATH.ORDERS"></v-list-item>
                            <v-divider class="my-1"></v-divider>
                            <v-list-item prepend-icon="mdi-logout" title="Đăng xuất" @click="handleLogout" color="error"></v-list-item>
                        </v-list>
                    </v-menu>
                </div>
            </div>
        </div>

        <!-- Main Nav -->
        <nav class="main-navbar px-12 py-2">
            <div class="container-custom d-flex align-center justify-space-between w-100">
                <!-- Logo -->
                <router-link to="/" class="logo-wrap">
                    <LogoClient />
                </router-link>

                <!-- Nav Links -->
                <nav class="nav-links">
                    <router-link
                        v-for="link in navLinks"
                        :key="link.path"
                        :to="link.path"
                        class="nav-link"
                        :class="{ active: $route.path === link.path || ($route.path.startsWith(link.path) && link.path !== '/') }"
                    >
                        {{ link.label }}
                    </router-link>
                </nav>

                <!-- Actions: search + favorites + cart -->
                <div class="nav-actions d-flex align-center gap-4">
                    <!-- Search -->
                    <div class="search-wrap" :class="{ expanded: isSearchOpen }">
                        <v-icon size="20" class="search-icon" @click="isSearchOpen ? handleSearchSubmit() : openSearch()">
                            mdi-magnify
                        </v-icon>
                        <input
                            v-if="isSearchOpen"
                            ref="searchInputRef"
                            type="text"
                            placeholder="Tìm kiếm sản phẩm..."
                            class="search-input"
                            v-model="searchQuery"
                            @keyup.enter="handleSearchSubmit"
                            @blur="() => { window.setTimeout(closeSearch, 200) }"
                            autofocus
                        />
                        <!-- Dropdown results -->
                        <div v-if="isSearchOpen && (searchResults.length > 0 || isSearchLoading)" class="search-dropdown">
                            <div v-if="isSearchLoading" class="search-loading">
                                <v-progress-circular size="18" width="2" indeterminate color="blue-darken-3"></v-progress-circular>
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
                                    <v-icon v-else size="24" color="grey-lighten-1">mdi-shoe-sneaker</v-icon>
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
                                Xem tất cả kết quả cho "{{ searchQuery }}"
                                <v-icon size="14">mdi-arrow-right</v-icon>
                            </div>
                        </div>
                    </div>

                    <!-- Favorites -->
                    <div class="cursor-pointer d-flex align-center" @click="handleFavoriteClick">
                        <v-badge :content="favoriteCount" color="error" v-if="favoriteCount > 0" offset-x="2" offset-y="2">
                            <v-icon size="22" class="action-icon">mdi-heart-outline</v-icon>
                        </v-badge>
                        <v-icon v-else size="22" class="action-icon">mdi-heart-outline</v-icon>
                    </div>

                    <!-- Cart -->
                    <div class="cursor-pointer d-flex align-center" @click="cartStore.openDrawer()">
                        <v-badge :content="cartStore.cartCount" color="error" v-if="cartStore.cartCount > 0" offset-x="2" offset-y="2">
                            <v-icon size="22" class="action-icon">mdi-shopping-outline</v-icon>
                        </v-badge>
                        <v-icon v-else size="22" class="action-icon">mdi-shopping-outline</v-icon>
                    </div>
                </div>
            </div>
        </nav>
        <!-- Include Cart Drawer globally -->
        <CartDrawer />
    </header>
</template>

<style scoped lang="scss">
.main-header-system {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    z-index: 1000;
    background: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(12px);
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
        background: #ffffff;
        backdrop-filter: none;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    }

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

/* Top Bar */
.top-utility-bar {
    height: 34px;
    background: #f5f5f5;
    border-bottom: 1px solid #ebebeb;
    display: flex;
    align-items: center;

    .u-text {
        font-size: 0.78rem;
        color: #555;
    }

    .u-link {
        font-size: 0.78rem;
        color: #333;
        text-decoration: none;
        font-weight: 500;
        cursor: pointer;
        &:hover { color: #000; }
    }

    .divider {
        color: #ccc;
        font-size: 0.7rem;
    }
}

/* Main Nav */
.main-navbar {
    background: #fff;
    position: relative;
}

/* Nav Links */
.nav-links {
    display: flex;
    align-items: center;
    gap: 2px;
}

.nav-link {
    position: relative;
    padding: 8px 14px;
    font-size: 0.82rem;
    font-weight: 700;
    letter-spacing: 0.8px;
    color: #333;
    text-decoration: none;
    white-space: nowrap;
    transition: color 0.2s ease;

    &::after {
        content: '';
        position: absolute;
        bottom: 2px;
        left: 14px;
        right: 14px;
        height: 2px;
        background: #111;
        transform: scaleX(0);
        transition: transform 0.25s ease;
        transform-origin: center;
    }

    &:hover {
        color: #111;
        &::after { transform: scaleX(1); }
    }

    &.active {
        color: #111;
        font-weight: 900;
        &::after { transform: scaleX(1); }
    }
}

/* Actions */
.nav-actions {
    .action-icon {
        cursor: pointer;
        color: #111;
        &:hover { opacity: 0.7; }
    }
}

/* Search */
.search-wrap {
    position: relative;
    display: flex;
    align-items: center;
    background: #f5f5f5;
    border-radius: 100px;
    padding: 6px 12px;
    min-width: 36px;
    transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);

    &.expanded {
        width: 260px;
        background: #ebebeb;
    }

    .search-icon {
        color: #666;
        cursor: pointer;
        flex-shrink: 0;
    }

    .search-input {
        border: none;
        background: transparent;
        outline: none;
        font-size: 0.88rem;
        font-weight: 500;
        width: 100%;
        color: #111;
        margin-left: 8px;
    }
}

.search-dropdown {
    position: absolute;
    top: calc(100% + 8px);
    right: 0;
    left: 0;
    width: 340px;
    background: #fff;
    border-radius: 14px;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.12);
    border: 1px solid #f0f0f0;
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
    padding: 10px 14px;
    cursor: pointer;
    transition: background 0.15s;

    &:hover { background: #f8fafc; }
}

.result-img-wrap {
    width: 44px;
    height: 44px;
    border-radius: 8px;
    background: #f5f5f5;
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
    padding: 4px;
}

.result-info {
    flex: 1;
    min-width: 0;
}

.result-name {
    font-size: 0.85rem;
    font-weight: 700;
    color: #1e293b;
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
    color: #94a3b8;
    font-weight: 600;
}

.result-price {
    font-size: 0.78rem;
    font-weight: 800;
    color: #d32f2f;
}

.search-footer {
    padding: 10px 14px;
    font-size: 0.8rem;
    font-weight: 700;
    color: #2962ff;
    cursor: pointer;
    border-top: 1px solid #f0f0f0;
    display: flex;
    align-items: center;
    gap: 4px;

    &:hover { background: #f0f4ff; }
}

@media (max-width: 1200px) {
    .top-utility-bar { display: none; }
    .brand-tabs { display: none; }
}
</style>
