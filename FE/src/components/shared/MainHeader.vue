<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { PATH } from '@/router/routePaths';
import { useAuthStore } from '@/stores/authStore';
import { useCartStore } from '@/stores/cartStore';
import { useToastStore } from '@/stores/toastStore';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const cartStore = useCartStore();
const toastStore = useToastStore();
const router = useRouter();

const favoriteCount = ref(0);

const updateFavoriteCount = () => {
    let favorites = JSON.parse(localStorage.getItem('aerostride_favorites') || '[]');
    favoriteCount.value = favorites.length;
};

const handleFavoriteClick = () => {
    if (favoriteCount.value > 0) {
        router.push(PATH.FAVORITES);
    } else {
        toastStore.showToast('Bạn chưa có sản phẩm yêu thích nào!', 'info');
        router.push(PATH.FAVORITES); // Vẫn cho phép vào xem trang rỗng
    }
};

const handleLogout = async () => {
    await authStore.logout();
    router.push('/');
};

const isMenuOpen = ref(false);
const activeMenu = ref(null);
const isSearchOpen = ref(false);
const isScrolled = ref(false);

const searchQuery = ref('');

const handleSearch = () => {
    if (searchQuery.value.trim()) {
        router.push({ path: PATH.SHOES, query: { keyword: searchQuery.value.trim() } });
    }
};

const navLinks = [
    {
        name: 'Sản phẩm mới',
        id: 'new',
        sub: [
            { label: 'Nổi bật', items: ['Sản phẩm mới nhất', 'Bán chạy nhất', 'Dành riêng cho thành viên', 'Bộ sưu tập giới hạn'] },
            { label: 'Loại giày', items: ['Lifestyle', 'Chạy bộ', 'Bóng rổ', 'Tập luyện', 'Bóng đá'] },
            { label: 'Theo bộ sưu tập', items: ['AeroStride X1', 'Strike V3', 'Glide Pro', 'Air Max'] }
        ]
    },
    {
        name: 'Nam',
        id: 'men',
        sub: [
            { label: 'Loại giày', items: ['Tất cả giày nam', 'Lifestyle', 'Chạy bộ', 'Bóng rổ', 'Bóng đá', 'Tập luyện'] },
            { label: 'Nổi bật', items: ['Hàng mới về', 'Sale 50%', 'Được yêu thích nhất'] }
        ]
    },
    {
        name: 'Nữ',
        id: 'women',
        sub: [
            { label: 'Loại giày', items: ['Tất cả giày nữ', 'Lifestyle', 'Chạy bộ', 'Tập luyện', 'Yoga', 'Tennis'] },
            { label: 'Nổi bật', items: ['Hàng mới về', 'Ưu đãi đặc biệt', 'Phối màu mới'] }
        ]
    },
    { name: 'Mã giảm giá', id: 'vouchers', path: PATH.VOUCHERS, color: '#FF1744' }
];

const isHidden = ref(false);
let lastScrollY = 0;

const handleScroll = () => {
    const currentScrollY = window.scrollY;
    isScrolled.value = currentScrollY > 50;

    if (currentScrollY > lastScrollY && currentScrollY > 100) {
        // Cuộn xuống -> Ẩn
        isHidden.value = true;
        closeMegaMenu();
    } else if (currentScrollY < lastScrollY) {
        // Cuộn lên -> Hiện
        isHidden.value = false;
    }
    lastScrollY = currentScrollY;
};

const handleMouseEnter = () => {
    isHidden.value = false;
};

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
});

const openMegaMenu = (id) => {
    activeMenu.value = id;
    isMenuOpen.value = true;
};

const closeMegaMenu = () => {
    isMenuOpen.value = false;
    activeMenu.value = null;
};
</script>

<template>
    <div class="header-hover-zone" v-if="isHidden" @mouseenter="handleMouseEnter"></div>
    <header class="main-header-system" :class="{ scrolled: isScrolled, 'mega-active': isMenuOpen, 'header-hidden': isHidden }" @mouseenter="handleMouseEnter">
        <!-- Top Utility Bar -->
        <div class="top-utility-bar">
            <div class="container-custom d-flex justify-end gap-6 px-12 w-100">
                <a href="#" class="u-link">Hệ thống cửa hàng</a>
                <span class="divider">|</span>
                <a href="#" class="u-link">Trợ giúp</a>
                <span class="divider">|</span>
                <a href="#" class="u-link">Tham gia cùng chúng tôi</a>
                <span class="divider">|</span>
                <router-link v-if="!authStore.isLoggedIn" :to="PATH.LOGIN" class="u-link d-flex align-center" title="Đăng nhập">
                    <v-icon size="20" color="black" class="mr-1">mdi-account-circle-outline</v-icon>
                </router-link>
                <div v-else class="d-flex align-center cursor-pointer" style="height: 100%">
                    <v-menu location="bottom end" offset="4" transition="slide-y-transition">
                        <template v-slot:activator="{ props }">
                            <div v-bind="props" class="d-flex align-center gap-1">
                                <v-icon size="18" color="black">mdi-account-circle-outline</v-icon>
                                <span class="u-link">{{ authStore.user?.hoTen || authStore.user?.username || 'Thành viên' }}</span>
                            </div>
                        </template>
                        <v-list density="compact" width="180" class="rounded-lg mt-2 border shadow-sm">
                            <v-list-item prepend-icon="mdi-account-outline" title="Tài khoản của tôi" :to="PATH.PROFILE"></v-list-item>
                            <v-list-item prepend-icon="mdi-package-variant-closed" title="Đơn mua" :to="PATH.ORDERS"></v-list-item>
                            <v-divider class="my-1"></v-divider>
                            <v-list-item prepend-icon="mdi-logout" title="Đăng xuất" @click="handleLogout" color="error"></v-list-item>
                        </v-list>
                    </v-menu>
                </div>
            </div>
        </div>

        <!-- Main Navigation Bar -->
        <nav class="main-navbar px-12 py-2" @mouseleave="closeMegaMenu">
            <div class="container-custom d-flex align-center justify-space-between w-100">
                <!-- Logo -->
                <router-link to="/" class="logo-wrap">
                    <LogoClient />
                </router-link>

                <!-- Nav Links -->
                <div class="nav-links-center d-flex align-center">
                    <div v-for="link in navLinks" :key="link.id" class="nav-item-wrap" @mouseenter="openMegaMenu(link.id)">
                        <router-link :to="link.path || PATH.SHOES" class="nav-link" :style="{ color: link.color || 'inherit' }">{{
                            link.name
                        }}</router-link>
                        <div class="active-indicator"></div>
                    </div>
                </div>

                <!-- Actions -->
                <div class="nav-actions d-flex align-center gap-6">
                    <div class="search-bar-wrap" :class="{ expanded: isSearchOpen }">
                        <v-icon size="22" class="search-icon" @click="handleSearch">mdi-magnify</v-icon>
                        <input type="text" placeholder="Tìm kiếm" class="search-input" v-model="searchQuery" @keyup.enter="handleSearch" />
                    </div>
                    <div class="cursor-pointer" style="display: flex; align-items: center;" @click="handleFavoriteClick">
                        <v-badge :content="favoriteCount" color="error" v-if="favoriteCount > 0" offset-x="2" offset-y="2">
                            <v-icon size="24" class="action-icon">mdi-heart-outline</v-icon>
                        </v-badge>
                        <v-icon v-else size="24" class="action-icon">mdi-heart-outline</v-icon>
                    </div>
                    <div class="cursor-pointer" style="display: flex; align-items: center;" @click="cartStore.openDrawer()">
                        <v-badge :content="cartStore.cartCount" color="error" v-if="cartStore.cartCount > 0" offset-x="2" offset-y="2">
                            <v-icon size="24" class="action-icon">mdi-shopping-outline</v-icon>
                        </v-badge>
                        <v-icon v-else size="24" class="action-icon">mdi-shopping-outline</v-icon>
                    </div>
                </div>
            </div>

            <!-- Mega Menu Overlay -->
            <transition name="mega-fade">
                <div v-if="isMenuOpen && navLinks.find((l) => l.id === activeMenu)?.sub" class="mega-menu-content">
                    <v-container class="py-12">
                        <v-row>
                            <v-col v-for="(col, i) in navLinks.find((l) => l.id === activeMenu).sub" :key="i" cols="12" md="3">
                                <h4 class="menu-label mb-6">{{ col.label }}</h4>
                                <ul class="menu-list">
                                    <li v-for="item in col.items" :key="item" class="menu-item" @click="closeMegaMenu">
                                        <router-link :to="PATH.SHOES">{{ item }}</router-link>
                                    </li>
                                </ul>
                            </v-col>
                            <v-col cols="12" md="3">
                                <div class="featured-promo p-6 rounded-lg bg-grey-lighten-4">
                                    <h4 class="menu-label mb-4" style="text-transform: none; font-weight: normal;">Dành riêng cho thành viên</h4>
                                    <p class="text-caption mb-4" style="font-weight: normal;">Sở hữu những mẫu giày mới nhất ngay hôm nay. Đăng ký thành viên hoàn toàn miễn phí.</p>
                                    <v-btn block color="black" rounded="pill" size="small" :to="PATH.SHOES" @click="closeMegaMenu" style="text-transform: none; font-weight: normal;"
                                        >Mua ngay</v-btn
                                    >
                                </div>
                            </v-col>
                        </v-row>
                    </v-container>
                </div>
            </transition>
        </nav>

        <!-- Backdrop for Mega Menu -->
        <div v-if="isMenuOpen" class="mega-backdrop" @mouseenter="closeMegaMenu"></div>
    </header>
</template>

<style scoped lang="scss">
.main-header-system {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    z-index: 1000;
    background: rgba(255, 255, 255, 0.6);
    backdrop-filter: blur(12px);
    border-bottom: 1px solid rgba(255, 255, 255, 0.3);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
        background: #ffffff;
        backdrop-filter: none;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    }

    &.mega-active {
        background: #ffffff !important;
        box-shadow: 0 4px 30px rgba(0, 0, 0, 0.05);
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
    height: 25px;
    z-index: 1001; /* Above header so it triggers even when header is hidden */
}

.top-utility-bar {
    height: 36px;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    .u-link {
        font-size: 0.8rem;
        font-weight: normal;
        color: #111;
        text-decoration: none;
        &:hover {
            opacity: 0.7;
        }
    }
    .divider {
        font-size: 0.7rem;
        color: #ccc;
        margin: 0 10px;
    }
}

.main-navbar {
    position: relative;
    background: #fff;
    border-bottom: 1px solid transparent;
    transition: all 0.3s ease;
}

.nav-links-center {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    gap: 30px;
}

.nav-item-wrap {
    position: relative;
    padding: 15px 0;
    .nav-link {
        font-size: 0.95rem;
        font-weight: normal;
        color: #111;
        text-decoration: none;
    }
    .active-indicator {
        position: absolute;
        bottom: 0;
        left: 0;
        width: 0;
        height: 2px;
        background: #000;
        transition: width 0.3s ease;
    }
    &:hover .active-indicator {
        width: 100%;
    }
}

/* Actions & Search */
.nav-actions {
    .action-icon {
        cursor: pointer;
        color: #111;
        &:hover {
            opacity: 0.7;
        }
    }
}

.search-bar-wrap {
    background: #f5f5f5;
    border-radius: 100px;
    display: flex;
    align-items: center;
    padding: 6px 12px;
    width: 180px;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    .search-icon {
        color: #666;
        margin-right: 8px;
    }
    .search-input {
        border: none;
        background: transparent;
        outline: none;
        font-size: 0.9rem;
        font-weight: 600;
        width: 100%;
        color: #111;
    }
    &:hover,
    &.expanded {
        background: #e5e5e5;
        width: 250px;
    }
}

/* Mega Menu */
.mega-menu-content {
    position: absolute;
    top: 100%;
    left: 0;
    width: 100%;
    background: #ffffff;
    border-top: 1px solid #f0f0f0;
    box-shadow: 0 40px 80px rgba(0, 0, 0, 0.1);
    z-index: 1000;
    overflow: hidden;
}

.menu-label {
    font-size: 0.9rem;
    font-weight: 900;
    color: #111;
    text-transform: uppercase;
    letter-spacing: 1px;
}
.menu-list {
    list-style: none;
    padding: 0;
    .menu-item {
        margin-bottom: 8px;
        cursor: pointer;
        a {
            font-size: 0.9rem;
            color: #757575;
            text-decoration: none;
            font-weight: 600;
            &:hover {
                color: #000;
            }
        }
    }
}

.mega-backdrop {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.3);
    z-index: -1;
    backdrop-filter: blur(4px);
}

/* Transitions */
.mega-fade-enter-active,
.mega-fade-leave-active {
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.mega-fade-enter-from,
.mega-fade-leave-to {
    opacity: 0;
    transform: translateY(-10px);
}

@media (max-width: 1200px) {
    .nav-links-center {
        display: none;
    }
    .top-utility-bar {
        display: none;
    }
}
</style>
