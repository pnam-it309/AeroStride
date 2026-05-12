<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { PATH } from '@/router/routePaths';
import { useAuthStore } from '@/stores/authStore';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

const handleLogout = async () => {
    await authStore.logout();
    router.push('/');
};

const isMenuOpen = ref(false);
const activeMenu = ref(null);
const isSearchOpen = ref(false);
const isScrolled = ref(false);

const navLinks = [
    { name: 'New & Featured', id: 'new', sub: [
        { label: 'Featured', items: ['New Arrivals', 'Bestsellers', 'Member Exclusive', 'Custom Shoes'] },
        { label: 'Shoes', items: ['All Shoes', 'Lifestyle', 'Running', 'Basketball', 'Training'] },
        { label: 'Shop By Icon', items: ['AeroStride X1', 'Strike V3', 'Glide Pro', 'Air Max'] }
    ]},
    { name: 'Men', id: 'men', sub: [
        { label: 'Shoes', items: ['All Shoes', 'Lifestyle', 'Running', 'Basketball', 'Football'] },
        { label: 'Clothing', items: ['All Clothing', 'Tops & T-Shirts', 'Hoodies', 'Jackets', 'Shorts'] },
        { label: 'Accessories', items: ['Bags', 'Socks', 'Hats'] }
    ]},
    { name: 'Women', id: 'women', sub: [
        { label: 'Shoes', items: ['All Shoes', 'Lifestyle', 'Running', 'Training', 'Yoga'] },
        { label: 'Clothing', items: ['All Clothing', 'Sports Bras', 'Leggings', 'Hoodies', 'Jackets'] },
        { label: 'Accessories', items: ['Bags', 'Socks', 'Hats'] }
    ]},
    { name: 'Sale', id: 'sale', color: '#FF1744' }
];

const handleScroll = () => {
    isScrolled.value = window.scrollY > 50;
};

onMounted(() => {
    window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll);
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
    <header class="main-header-system" :class="{ 'scrolled': isScrolled, 'mega-active': isMenuOpen }">
        <!-- Top Utility Bar -->
        <div class="top-utility-bar">
            <div class="container-custom d-flex justify-end gap-6 px-12">
                <a href="#" class="u-link">Find a Store</a>
                <span class="divider">|</span>
                <a href="#" class="u-link">Help</a>
                <span class="divider">|</span>
                <a href="#" class="u-link">Join Us</a>
                <span class="divider">|</span>
                <router-link v-if="!authStore.isLoggedIn" :to="PATH.LOGIN" class="u-link d-flex align-center" title="Sign In">
                    <v-icon size="20" color="black" class="mr-1">mdi-account-circle-outline</v-icon>
                </router-link>
                <div v-else class="d-flex align-center cursor-pointer" style="height: 100%">
                    <v-menu location="bottom end" transition="slide-y-transition">
                        <template v-slot:activator="{ props }">
                            <div v-bind="props" class="d-flex align-center gap-1">
                                <v-icon size="18" color="black">mdi-account-circle-outline</v-icon>
                                <span class="u-link font-weight-black">{{ authStore.user?.username || 'Member' }}</span>
                            </div>
                        </template>
                        <v-list density="compact" width="180" class="rounded-lg mt-2 border shadow-sm">
                            <v-list-item prepend-icon="mdi-account-outline" title="My Profile" :to="PATH.PROFILE"></v-list-item>
                            <v-list-item prepend-icon="mdi-package-variant-closed" title="Orders" :to="PATH.ORDERS"></v-list-item>
                            <v-divider class="my-1"></v-divider>
                            <v-list-item prepend-icon="mdi-logout" title="Sign Out" @click="handleLogout" color="error"></v-list-item>
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
                    <div v-for="link in navLinks" :key="link.id" 
                         class="nav-item-wrap"
                         @mouseenter="openMegaMenu(link.id)">
                        <router-link :to="PATH.SHOES" class="nav-link" :style="{ color: link.color || 'inherit' }">{{ link.name }}</router-link>
                        <div class="active-indicator"></div>
                    </div>
                </div>

                <!-- Actions -->
                <div class="nav-actions d-flex align-center gap-6">
                    <div class="search-bar-wrap" :class="{ 'expanded': isSearchOpen }">
                        <v-icon size="22" class="search-icon" @click="isSearchOpen = true">mdi-magnify</v-icon>
                        <input type="text" placeholder="Search" class="search-input" />
                    </div>
                    <v-icon size="24" class="action-icon">mdi-heart-outline</v-icon>
                    <v-icon size="24" class="action-icon">mdi-bag-outline</v-icon>
                </div>
            </div>

            <!-- Mega Menu Overlay -->
            <transition name="mega-fade">
                <div v-if="isMenuOpen && navLinks.find(l => l.id === activeMenu)?.sub" class="mega-menu-content">
                    <v-container class="py-12">
                        <v-row>
                            <v-col v-for="(col, i) in navLinks.find(l => l.id === activeMenu).sub" :key="i" cols="12" md="3">
                                <h4 class="menu-label mb-6">{{ col.label }}</h4>
                                <ul class="menu-list">
                                    <li v-for="item in col.items" :key="item" class="menu-item" @click="closeMegaMenu">
                                        <router-link :to="PATH.SHOES">{{ item }}</router-link>
                                    </li>
                                </ul>
                            </v-col>
                            <v-col cols="12" md="3">
                                <div class="featured-promo p-6 rounded-lg bg-grey-lighten-4">
                                    <h4 class="menu-label mb-4">MEMBER EXCLUSIVE</h4>
                                    <p class="text-caption mb-4">Get the latest AeroStride X1 before anyone else. Join us now for free.</p>
                                    <v-btn block color="black" rounded="pill" size="small" :to="PATH.SHOES" @click="closeMegaMenu">SHOP NOW</v-btn>
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
    position: fixed; top: 0; left: 0; width: 100%; z-index: 9999;
    background: rgba(255, 255, 255, 0.6);
    backdrop-filter: blur(12px);
    border-bottom: 1px solid rgba(255, 255, 255, 0.3);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    
    &:hover {
        background: #ffffff;
        backdrop-filter: none;
        box-shadow: 0 4px 20px rgba(0,0,0,0.08);
    }

    &.mega-active { background: #ffffff !important; box-shadow: 0 4px 30px rgba(0,0,0,0.05); }
}

.top-utility-bar {
    height: 36px; background: #f5f5f5; display: flex; align-items: center;
    .u-link { font-size: 0.7rem; font-weight: 700; color: #111; text-decoration: none; &:hover { opacity: 0.7; } }
    .divider { font-size: 0.7rem; color: #ccc; margin: 0 10px; }
}

.main-navbar {
    position: relative; background: #fff; border-bottom: 1px solid transparent;
    transition: all 0.3s ease;
}

.nav-links-center {
    position: absolute; left: 50%; transform: translateX(-50%);
    gap: 30px;
}

.nav-item-wrap {
    position: relative; padding: 15px 0;
    .nav-link { font-size: 0.95rem; font-weight: 700; color: #111; text-decoration: none; }
    .active-indicator { position: absolute; bottom: 0; left: 0; width: 0; height: 2px; background: #000; transition: width 0.3s ease; }
    &:hover .active-indicator { width: 100%; }
}

/* Actions & Search */
.nav-actions {
    .action-icon { cursor: pointer; color: #111; &:hover { opacity: 0.7; } }
}

.search-bar-wrap {
    background: #f5f5f5; border-radius: 100px; display: flex; align-items: center; padding: 6px 12px;
    width: 180px; transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    .search-icon { color: #666; margin-right: 8px; }
    .search-input { border: none; background: transparent; outline: none; font-size: 0.9rem; font-weight: 600; width: 100%; color: #111; }
    &:hover, &.expanded { background: #e5e5e5; width: 250px; }
}

/* Mega Menu */
.mega-menu-content {
    position: absolute; top: 100%; left: 0; width: 100%; background: #ffffff;
    border-top: 1px solid #f0f0f0; box-shadow: 0 40px 80px rgba(0,0,0,0.1);
    z-index: 1000; overflow: hidden;
}

.menu-label { font-size: 0.9rem; font-weight: 900; color: #111; text-transform: uppercase; letter-spacing: 1px; }
.menu-list { list-style: none; padding: 0;
    .menu-item { margin-bottom: 8px; cursor: pointer;
        a { font-size: 0.9rem; color: #757575; text-decoration: none; font-weight: 600; &:hover { color: #000; } }
    }
}

.mega-backdrop {
    position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.3); z-index: -1; backdrop-filter: blur(4px);
}

/* Transitions */
.mega-fade-enter-active, .mega-fade-leave-active { transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1); }
.mega-fade-enter-from, .mega-fade-leave-to { opacity: 0; transform: translateY(-10px); }

@media (max-width: 1200px) {
    .nav-links-center { display: none; }
    .top-utility-bar { display: none; }
}
</style>
