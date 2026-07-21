<script setup>
import { ref, onMounted, watch, nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import PromotionBar from '@/components/shared/PromotionBar.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import { dichVuSanPhamPublic } from '@/services/public/dichVuSanPhamPublic';
import { useSeoMeta } from '@/composables/useSeoMeta';

const router = useRouter();
const route = useRoute();
const loading = ref(false);
const isFilterVisible = ref(true);

const filters = ref([]);
const products = ref([]);
const totalElements = ref(0);
const currentPage = ref(1);
const pageSize = ref(12);

const searchParams = ref({
    keyword: route.query.keyword || '',
    thuongHieuId: route.query.thuongHieuId || null,
    chatLieuId: route.query.chatLieuId || null,
    xuatXuId: route.query.xuatXuId || null,
    mucDichChayId: route.query.mucDichChayId || null,
    gioiTinhKhachHang: route.query.gioiTinhKhachHang || null,
    sortBy: route.query.sortBy || 'newest'
});

const selectedFilters = ref({
    thuongHieus: [],
    mauSacs: [],
    kichThuocs: [],
    gioiTinhKhachHangs: []
});

const fetchFilters = async () => {
    try {
        const data = await dichVuSanPhamPublic.layBoLoc();
        filters.value = [
            { title: 'Thương hiệu', key: 'thuongHieuId', items: data.thuongHieus },
            { title: 'Chất liệu', key: 'chatLieuId', items: data.chatLieus },
            { title: 'Xuất xứ', key: 'xuatXuId', items: data.xuatXus },
            { title: 'Mục đích', key: 'mucDichChayId', items: data.mucDichChays },
            { 
                title: 'Giới tính', 
                key: 'gioiTinhKhachHang', 
                items: (data.gioiTinhKhachHangs || [])
                    .filter(g => g !== 'TRE_EM')
                    .map((g) => ({ id: g, ten: translateGender(g) })) 
            }
        ];
    } catch (error) {
        console.error('Error fetching filters:', error);
    }
};

const fetchProducts = async () => {
    loading.value = true;
    try {
        const params = {
            ...searchParams.value,
            page: currentPage.value,
            size: pageSize.value
        };
        const response = await dichVuSanPhamPublic.layDanhSachSanPham(params);
        products.value = response.content;
        totalElements.value = response.totalElements;
    } catch (error) {
        console.error('Error fetching products:', error);
    } finally {
        loading.value = false;
    }
};

const handleFilterChange = () => {
    currentPage.value = 1;
    fetchProducts();
};

// SEO
const { setSeoMeta } = useSeoMeta();
const updateSeo = () => {
    setSeoMeta({
        title: `Tất Cả Giày Thể Thao (${totalElements.value} sản phẩm)`,
        description: `Khám phá ${totalElements.value}+ đôi giày thể thao chính hãng tại AeroStride. Lọc theo thương hiệu, màu sắc, kích thước. Giao hàng nhanh, đổi trả miễn phí.`,
        url: window.location.origin + '/shoes'
    });
};

onMounted(() => {
    fetchFilters();
    fetchProducts().then(() => nextTick(updateSeo));
});

watch(
    () => route.query,
    (newQuery) => {
        searchParams.value = { ...searchParams.value, ...newQuery };
        fetchProducts();
    }
);

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const goToDetail = (id) => {
    router.push(`/product/${id}`);
};

const translateGender = (gender) => {
    const map = {
        NAM: 'Nam',
        NU: 'Nữ',
        TRE_EM: 'Trẻ em',
        UNISEX: 'Unisex'
    };
    return map[gender] || gender;
};
</script>

<template>
    <div class="shoe-listing-page bg-white min-vh-100">
        <MainHeader />

        <!-- Separate Promotion Bar -->
        <div class="header-spacing" style="height: 94px"></div>
        <PromotionBar />

        <!-- Sub-header (Title & Global Filters) -->
        <div class="listing-subheader px-12 py-8 mt-0">
            <div class="d-flex align-center justify-space-between">
                <h1 class="text-h4 font-weight-black">Tất cả sản phẩm ({{ totalElements }})</h1>
                <div class="d-flex align-center ga-6">
                    <v-btn variant="text" @click="isFilterVisible = !isFilterVisible" class="font-weight-bold">
                        {{ isFilterVisible ? 'Ẩn bộ lọc' : 'Hiện bộ lọc' }} <v-icon class="ml-2">mdi-filter-variant</v-icon>
                    </v-btn>
                    <v-menu>
                        <template v-slot:activator="{ props }">
                            <v-btn variant="text" v-bind="props" class="font-weight-bold">
                                Sắp xếp theo <v-icon class="ml-2">mdi-chevron-down</v-icon>
                            </v-btn>
                        </template>
                        <v-list>
                            <v-list-item
                                @click="
                                    searchParams.sortBy = 'newest';
                                    fetchProducts();
                                "
                                >Mới nhất</v-list-item
                            >
                            <v-list-item
                                @click="
                                    searchParams.sortBy = 'price_desc';
                                    fetchProducts();
                                "
                                >Giá: Cao - Thấp</v-list-item
                            >
                            <v-list-item
                                @click="
                                    searchParams.sortBy = 'price_asc';
                                    fetchProducts();
                                "
                                >Giá: Thấp - Cao</v-list-item
                            >
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
                            <h3 class="filter-main-label mb-4">Giày</h3>
                        </div>
                        <v-expansion-panels flat variant="accordion" bg-color="transparent">
                            <v-expansion-panel v-for="filter in filters" :key="filter.title" :title="filter.title" class="filter-panel bg-transparent">
                                <v-expansion-panel-text>
                                    <div class="custom-filter-list">
                                        <div 
                                            v-for="item in filter.items" 
                                            :key="item.id || item"
                                            class="custom-filter-item"
                                            :class="{ 'active': searchParams[filter.key] === (item.id || item) }"
                                            @click="searchParams[filter.key] = (searchParams[filter.key] === (item.id || item) ? null : (item.id || item)); handleFilterChange()"
                                        >
                                            <span class="filter-text">{{ item.ten }}</span>
                                            <v-scale-transition>
                                                <v-icon v-if="searchParams[filter.key] === (item.id || item)" size="18" color="white" class="check-icon">mdi-check</v-icon>
                                            </v-scale-transition>
                                        </div>
                                    </div>
                                </v-expansion-panel-text>
                            </v-expansion-panel>
                        </v-expansion-panels>
                        <v-btn
                            variant="text"
                            color="grey"
                            @click="
                                searchParams = { sortBy: 'newest' };
                                handleFilterChange();
                            "
                            class="mt-4 text-none"
                            >Xóa tất cả</v-btn
                        >
                    </v-col>
                </transition>

                <!-- Product Grid -->
                <v-col cols="12" :md="isFilterVisible ? 10 : 12" class="product-grid-col">
                    <v-row v-if="products.length > 0">
                        <v-col v-for="p in products" :key="p.id" cols="12" sm="6" md="4" lg="4">
                            <!-- Click to Navigate -->
                            <div class="product-card-placeholder" @click="goToDetail(p.id)">
                                <!-- Image Placeholder -->
                                <div class="image-box-placeholder mb-4">
                                    <v-img v-if="p.hinhAnh" :src="p.hinhAnh" cover class="fill-height"></v-img>
                                    <v-icon v-else size="64" color="grey-lighten-2">mdi-image-outline</v-icon>
                                </div>

                                <!-- Content -->
                                <div class="product-info">
                                    <span class="promo-label">{{ p.tenThuongHieu }}</span>
                                    <h4 class="product-name">{{ p.tenSanPham }}</h4>

                                    <p class="product-price">{{ formatPrice(p.giaBanThapNhat) }}</p>
                                </div>
                            </div>
                        </v-col>
                    </v-row>
                    <div v-else-if="!loading" class="text-center py-16">
                        <v-icon size="64" color="grey-lighten-1">mdi-package-variant</v-icon>
                        <p class="text-h6 text-grey-darken-1 mt-4">Không tìm thấy sản phẩm nào.</p>
                    </div>

                    <v-pagination
                        v-if="totalElements > pageSize"
                        v-model="currentPage"
                        :length="Math.ceil(totalElements / pageSize)"
                        @update:model-value="fetchProducts"
                        class="mt-12"
                        color="black"
                    ></v-pagination>
                </v-col>
            </v-row>
        </v-container>

        <!-- Global Chat System -->
        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.shoe-listing-page {
    background: #fff;
}

.listing-subheader {
    background: #fff;
    border-bottom: 1px solid #f0f0f0;
}

/* Sidebar Filters */
.filter-sidebar {
    position: sticky;
    top: 120px;
    height: calc(100vh - 120px);
    overflow-y: auto;
    scrollbar-width: none;
    &::-webkit-scrollbar {
        display: none;
    }
}

.filter-main-label {
    font-size: 1.1rem;
    font-weight: 800;
    color: #1e293b;
}
.filter-panel {
    border-top: 1px solid #f0f0f0 !important;
    :deep(.v-expansion-panel-title) {
        font-weight: 750;
        font-size: 0.95rem;
        padding: 15px 0;
    }
    :deep(.v-expansion-panel-text__wrapper) {
        padding: 0 0 15px 0;
    }
}

.filter-item-text {
    font-size: 0.9rem;
    font-weight: 600;
    color: #111;
}

/* Custom Filter UI */
.custom-filter-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.custom-filter-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 14px;
    border-radius: 10px;
    background: #f8fafc;
    border: 1px solid transparent;
    cursor: pointer;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
        background: #f1f5f9;
        transform: translateX(4px);
    }

    &.active {
        background: #111;
        border-color: #111;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        transform: translateX(4px);

        .filter-text {
            color: #fff;
            font-weight: 700;
        }
    }

    .filter-text {
        font-size: 0.9rem;
        font-weight: 600;
        color: #475569;
        transition: color 0.25s ease;
    }
}

/* Product Card Placeholder */
.product-card-placeholder {
    cursor: pointer;
    transition: transform 0.3s ease;
    &:hover {
        .image-box-placeholder {
            transform: scale(1.02);
        }
        .product-name {
            color: #2962ff;
        }
    }
}

.image-box-placeholder {
    width: 100%;
    aspect-ratio: 1;
    background: transparent;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;
    transition: transform 0.3s ease;
}

.product-info {
    .promo-label {
        color: #ff3d00;
        font-size: 0.85rem;
        font-weight: 800;
        display: block;
        margin-bottom: 4px;
    }
    .product-name {
        font-size: 1rem;
        font-weight: 700;
        color: #1e293b;
        margin-bottom: 2px;
    }
    .product-category {
        color: #757575;
        font-size: 0.9rem;
        font-weight: 600;
        margin-bottom: 4px;
    }
    .product-price {
        font-size: 1.05rem;
        font-weight: 800;
        color: #111;
    }
}

/* Transitions */
.sidebar-slide-enter-active,
.sidebar-slide-leave-active {
    transition: all 0.4s ease;
}
.sidebar-slide-enter-from,
.sidebar-slide-leave-to {
    opacity: 0;
    transform: translateX(-30px);
    margin-left: -200px;
}

.min-vh-100 {
    min-height: 100vh;
}
</style>
