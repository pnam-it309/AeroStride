<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
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
const handleMouseEnter = () => {
    isHidden.value = false;
};

// ─── Nav links ────────────────────────────────────────────────────────────────
const navLinks = [
    { label: 'TRANG CHỦ', path: PATH.LANDING },
    { label: 'SẢN PHẨM', path: PATH.SHOES },
    { label: 'GỢI Ý AI', path: PATH.AI_RECOMMEND },
    { label: 'GIỚI THIỆU', path: '/gioi-thieu' },
    { label: 'TIN TỨC', path: '/tin-tuc' },
    { label: 'LIÊN HỆ', path: '/lien-he' },
    { label: 'TRA CỨU', path: PATH.ORDERS }
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
    <header class="main-header-system" :class="{ scrolled: isScrolled, 'header-hidden': isHidden }" @mouseenter="handleMouseEnter">
        <!-- Top Announcement Bar (mockup requirement) -->
        <div class="top-announcement-bar">Hàng chính hãng 100% • Giao hàng miễn phí toàn quốc • Đổi trả trong vòng 30 ngày</div>

        <!-- Main Nav -->
        <nav class="main-navbar">
            <!-- Logo -->
            <div class="logo-wrap">
                <router-link to="/" class="text-logo-brand">AEROSTRIDE</router-link>
            </div>

            <!-- Nav Links -->
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

            <!-- Actions (Search Box + Icons) -->
            <div class="nav-actions">
                <!-- Search Box (mockup layout: 230px, rounded-22, static style) -->
                <div class="search-box-custom">
                    <v-icon size="18" class="search-icon-custom">mdi-magnify</v-icon>
                    <input
                        type="text"
                        placeholder="Tìm kiếm sản phẩm..."
                        class="search-input-custom"
                        v-model="searchQuery"
                        @keyup.enter="handleSearchSubmit"
                        @focus="openSearch"
                        @blur="
                            () => {
                                window.setTimeout(closeSearch, 250);
                            }
                        "
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

                <!-- Account Icon (◯ representation with dynamic drop-down) -->
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

                <!-- Cart Icon (🛒 representation) -->
                <div class="action-icon-btn cursor-pointer" @click="cartStore.openDrawer()">
                    <v-badge :content="cartStore.cartCount" color="primary" v-if="cartStore.cartCount > 0" offset-x="2" offset-y="2">
                        <v-icon size="26" class="action-icon">mdi-shopping-outline</v-icon>
                    </v-badge>
                    <v-icon v-else size="26" class="action-icon">mdi-shopping-outline</v-icon>
                </div>
            </div>
        </nav>
    </header>
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

/* Announcement Bar */
.top-announcement-bar {
    width: 100%;
    height: 36px;
    background: #0a1329;
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: 'Inter', sans-serif;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.5px;
}

/* Main Navbar */
.main-navbar {
    height: 84px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 72px;
    background: #ffffff;
}

/* Brand Text Logo */
.text-logo-brand {
    font-family: 'Outfit', sans-serif;
    font-size: 25px;
    font-weight: 700;
    color: #2962ff;
    text-decoration: none;
    letter-spacing: 0.5px;
    transition: opacity 0.2s ease;

    &:hover {
        opacity: 0.85;
    }
}

/* Navigation Menu */
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
        &::after {
            transform: scaleX(1);
        }
    }

    &.active {
        color: #2962ff;
        &::after {
            transform: scaleX(1);
        }
    }
}

/* Actions area */
.nav-actions {
    display: flex;
    align-items: center;
    gap: 20px;
}

/* Search Box Custom (mockup spec) */
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

.search-icon-custom {
    color: #637085;
}

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

    &::placeholder {
        color: #637085;
    }
}

.clear-icon-custom {
    cursor: pointer;
    opacity: 0.6;
    transition: opacity 0.2s;
    &:hover {
        opacity: 1;
    }
}

/* Search results dropdown */
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

    &:hover {
        background: #f8fafc;
    }
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

    &:hover {
        background: rgba(41, 98, 255, 0.04);
    }
}

/* Icon Buttons */
.action-icon-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 42px;
    height: 42px;
    border-radius: 50%;
    color: #0a1329;
    transition:
        background-color 0.2s ease,
        color 0.2s ease;

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

@media (max-width: 1024px) {
    .main-navbar {
        padding: 0 24px;
    }
    .nav-links {
        gap: 16px;
    }
    .search-box-custom {
        width: 180px;
    }
}

@media (max-width: 768px) {
    .nav-links {
        display: none;
    }
}
</style>
