<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import PromotionBar from '@/components/shared/PromotionBar.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import apiService from '@/services/apiService';

const router = useRouter();
const loading = ref(false);
const isFilterVisible = ref(true);

const filters = [
    { title: 'Gender', items: ['Men', 'Women', 'Unisex'] },
    { title: 'Colour', items: ['Black', 'White', 'Blue', 'Red', 'Grey'] },
    { title: 'Brand', items: ['AeroStride', 'Performance', 'Elite'] },
    { title: 'Sports', items: ['Running', 'Basketball', 'Training', 'Lifestyle'] },
    { title: 'Technology', items: ['X1-Sensor', 'Air-Nano', 'Carbon-Strike'] }
];

const products = ref([]);

const fetchProducts = async () => {
    loading.value = true;
    try {
        const response = await apiService.get('/customer/san-pham/hien-thi', {
            params: { size: 12 }
        });
        if (response.data.success) {
            products.value = response.data.data.content;
        }
    } catch (error) {
        console.error('Error fetching products:', error);
    } finally {
        loading.value = false;
    }
};

onMounted(() => {
    fetchProducts();
});

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const goToDetail = (id) => {
    router.push(`/product/${id}`);
};
</script>

<template>
    <div class="shoe-listing-page bg-white min-vh-100">
        <MainHeader />
        
        <!-- Separate Promotion Bar -->
        <div class="header-spacing" style="height: 104px;"></div>
        <PromotionBar />

        <!-- Sub-header (Title & Global Filters) -->
        <div class="listing-subheader px-12 py-8 mt-0">
            <div class="d-flex align-center justify-space-between">
                <h1 class="text-h4 font-weight-black">Tất cả sản phẩm ({{ products.length }})</h1>
                <div class="d-flex align-center gap-6">
                    <v-btn variant="text" @click="isFilterVisible = !isFilterVisible" class="font-weight-bold">
                        {{ isFilterVisible ? 'Hide Filters' : 'Show Filters' }} <v-icon class="ml-2">mdi-filter-variant</v-icon>
                    </v-btn>
                    <v-menu>
                        <template v-slot:activator="{ props }">
                            <v-btn variant="text" v-bind="props" class="font-weight-bold">
                                Sort By <v-icon class="ml-2">mdi-chevron-down</v-icon>
                            </v-btn>
                        </template>
                        <v-list>
                            <v-list-item>Newest</v-list-item>
                            <v-list-item>Price: High-Low</v-list-item>
                            <v-list-item>Price: Low-High</v-list-item>
                        </v-list>
                    </v-menu>
                </div>
            </div>
        </div>

        <v-container fluid class="px-12 pb-16">
            <v-row>
                <!-- Sidebar Filters -->
                <transition name="sidebar-slide">
                    <v-col v-if="isFilterVisible" cols="12" md="2" class="filter-sidebar">
                        <div class="filter-group mb-8">
                            <h3 class="filter-main-label mb-4">Shoes</h3>
                        </div>
                        <v-expansion-panels flat variant="accordion">
                            <v-expansion-panel v-for="filter in filters" :key="filter.title" :title="filter.title" class="filter-panel">
                                <v-expansion-panel-text>
                                    <div v-for="item in filter.items" :key="item" class="d-flex align-center mb-2">
                                        <v-checkbox-btn :label="item" color="black" density="compact"></v-checkbox-btn>
                                        <span class="filter-item-text ml-2">{{ item }}</span>
                                    </div>
                                </v-expansion-panel-text>
                            </v-expansion-panel>
                        </v-expansion-panels>
                    </v-col>
                </transition>

                <!-- Product Grid -->
                <v-col cols="12" :md="isFilterVisible ? 10 : 12" class="product-grid-col">
                    <v-row>
                        <v-col v-for="p in products" :key="p.id" cols="12" sm="6" md="4" lg="4">
                            <!-- Click to Navigate -->
                            <div class="product-card-placeholder" @click="goToDetail(p.id)">
                                <!-- Image Placeholder -->
                                <div class="image-box-placeholder mb-4">
                                    <v-img v-if="p.hinhAnh" :src="p.hinhAnh" cover class="fill-height"></v-img>
                                    <v-icon v-else size="64" color="grey-lighten-2">mdi-image-outline</v-icon>
                                    <div class="corner-border tl"></div>
                                    <div class="corner-border br"></div>
                                </div>
                                
                                <!-- Content -->
                                <div class="product-info">
                                    <span class="promo-label">{{ p.tenThuongHieu }}</span>
                                    <h4 class="product-name">{{ p.tenSanPham }}</h4>
                                    <p class="product-category">{{ p.tenDanhMuc }}</p>
                                    <p class="product-price">{{ formatPrice(p.giaBanThapNhat) }}</p>
                                </div>
                            </div>
                        </v-col>
                    </v-row>
                </v-col>
            </v-row>
        </v-container>

        <!-- Global Chat System -->
        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.shoe-listing-page {
    padding-top: 64px;
}

.listing-subheader {
    background: #fff; border-bottom: 1px solid #f0f0f0;
}

/* Sidebar Filters */
.filter-sidebar {
    position: sticky; top: 120px; height: calc(100vh - 120px); overflow-y: auto;
    scrollbar-width: none; &::-webkit-scrollbar { display: none; }
}

.filter-main-label { font-size: 1.1rem; font-weight: 900; color: #000; }
.filter-panel {
    border-top: 1px solid #f0f0f0 !important;
    :deep(.v-expansion-panel-title) { font-weight: 750; font-size: 0.95rem; padding: 15px 0; }
    :deep(.v-expansion-panel-text__wrapper) { padding: 0 0 15px 0; }
}

.filter-item-text { font-size: 0.9rem; font-weight: 600; color: #111; }

/* Product Card Placeholder */
.product-card-placeholder {
    cursor: pointer; transition: transform 0.3s ease;
    &:hover {
        .image-box-placeholder { background: #eeeeee; }
        .product-name { color: #2962FF; }
    }
}

.image-box-placeholder {
    width: 100%; aspect-ratio: 1; background: #f6f6f6;
    display: flex; align-items: center; justify-content: center;
    position: relative; overflow: hidden;
    transition: background 0.3s ease;

    .corner-border {
        position: absolute; width: 20px; height: 20px; border: 1px solid rgba(0,0,0,0.05);
        &.tl { top: 10px; left: 10px; border-right: none; border-bottom: none; }
        &.br { bottom: 10px; right: 10px; border-left: none; border-top: none; }
    }
}

.product-info {
    .promo-label { color: #FF3D00; font-size: 0.85rem; font-weight: 900; display: block; margin-bottom: 4px; }
    .product-name { font-size: 1rem; font-weight: 900; color: #000; margin-bottom: 2px; }
    .product-category { color: #757575; font-size: 0.9rem; font-weight: 600; margin-bottom: 4px; }
    .product-price { font-size: 1rem; font-weight: 800; color: #111; }
}

/* Transitions */
.sidebar-slide-enter-active, .sidebar-slide-leave-active { transition: all 0.4s ease; }
.sidebar-slide-enter-from, .sidebar-slide-leave-to { opacity: 0; transform: translateX(-30px); margin-left: -200px; }

.min-vh-100 { min-height: 100vh; }
</style>
