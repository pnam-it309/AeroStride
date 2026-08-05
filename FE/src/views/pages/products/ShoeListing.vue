<script setup>
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import { dichVuSanPhamPublic } from '@/services/public/dichVuSanPhamPublic';
import { useSeoMeta } from '@/composables/useSeoMeta';
import { dichVuFile } from '@/services/core/dichVuFile';
import defaultShoeImg from '@/assets/images/products/s4.jpg';

const router = useRouter();
const route = useRoute();
const loading = ref(false);

const filters = ref([]);
const products = ref([]);
const DEFAULT_SHOE_IMAGE = defaultShoeImg || new URL('/src/assets/images/products/s4.jpg', import.meta.url).href;
const imageFallbacks = ref({});

const handleImageError = (id, event) => {
    if (!event || !event.target) return;
    if (event.target.getAttribute('data-fallback') === 'true') return;
    event.target.setAttribute('data-fallback', 'true');
    event.target.src = DEFAULT_SHOE_IMAGE;
    if (id) {
        imageFallbacks.value[id] = DEFAULT_SHOE_IMAGE;
    }
};
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
    sortBy: route.query.sortBy || 'newest',
    minGia: null,
    maxGia: null,
    kichThuoc: null
});

// Additional interactive filters from the wireframe
const priceRange = ref(null); // 'under2', '2to3', '3to4', 'over4'
const priceRangeOptions = [
    { title: '0 - 500.000đ', value: '0to500k' },
    { title: '500.000đ - 1.000.000đ', value: '500kto1M' },
    { title: '1.000.000đ - 2.000.000đ', value: '1Mto2M' },
    { title: '2.000.000đ - 3.000.000đ', value: '2Mto3M' },
    { title: '3.000.000đ - 4.000.000đ', value: '3Mto4M' },
    { title: '4.000.000đ - 5.000.000đ', value: '4Mto5M' },
    { title: '5.000.000đ - 6.000.000đ', value: '5Mto6M' },
    { title: '6.000.000đ - 7.000.000đ', value: '6Mto7M' },
    { title: 'Trên 7.000.000đ', value: 'over7M' }
];
const selectedSize = ref(null);
const sizeList = ['36', '37', '38', '39', '40', '41', '42', '43', '44'];

// Watch price range options to update query parameters
watch(priceRange, (newRange) => {
    if (newRange === '0to500k') {
        searchParams.value.minGia = 0;
        searchParams.value.maxGia = 500000;
    } else if (newRange === '500kto1M') {
        searchParams.value.minGia = 500000;
        searchParams.value.maxGia = 1000000;
    } else if (newRange === '1Mto2M') {
        searchParams.value.minGia = 1000000;
        searchParams.value.maxGia = 2000000;
    } else if (newRange === '2Mto3M') {
        searchParams.value.minGia = 2000000;
        searchParams.value.maxGia = 3000000;
    } else if (newRange === '3Mto4M') {
        searchParams.value.minGia = 3000000;
        searchParams.value.maxGia = 4000000;
    } else if (newRange === '4Mto5M') {
        searchParams.value.minGia = 4000000;
        searchParams.value.maxGia = 5000000;
    } else if (newRange === '5Mto6M') {
        searchParams.value.minGia = 5000000;
        searchParams.value.maxGia = 6000000;
    } else if (newRange === '6Mto7M') {
        searchParams.value.minGia = 6000000;
        searchParams.value.maxGia = 7000000;
    } else if (newRange === 'over7M') {
        searchParams.value.minGia = 7000000;
        searchParams.value.maxGia = null;
    } else {
        searchParams.value.minGia = null;
        searchParams.value.maxGia = null;
    }
    handleFilterChange();
});

// Watch size selection to update query parameters
watch(selectedSize, (newSize) => {
    searchParams.value.kichThuoc = newSize;
    handleFilterChange();
});

// Map backend filter categories dynamically
const brandList = computed(() => {
    const group = filters.value.find(f => f.key === 'thuongHieuId');
    return group ? group.items : [];
});

const purposeList = computed(() => {
    const group = filters.value.find(f => f.key === 'mucDichChayId');
    return group ? group.items : [];
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

const onPageChange = () => {
    fetchProducts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
};

const handleFilterChange = () => {
    currentPage.value = 1;
    fetchProducts();
};

const resetFilters = () => {
    searchParams.value = {
        keyword: '',
        thuongHieuId: null,
        chatLieuId: null,
        xuatXuId: null,
        mucDichChayId: null,
        gioiTinhKhachHang: null,
        sortBy: 'newest',
        minGia: null,
        maxGia: null,
        kichThuoc: null
    };
    priceRange.value = null;
    selectedSize.value = null;
    handleFilterChange();
};

// SEO Setup
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
    updateFavoriteIds();
    window.addEventListener('favorites-updated', updateFavoriteIds);
});

onUnmounted(() => {
    window.removeEventListener('favorites-updated', updateFavoriteIds);
});

watch(
    () => route.query,
    (newQuery) => {
        searchParams.value = { ...searchParams.value, ...newQuery };
        fetchProducts();
    }
);

// Image Path Resolver
const isAbsoluteUrl = (v) => 
    typeof v !== 'string' || 
    /^(https?:)?\/\//i.test(v) || 
    v.startsWith('data:') || 
    v.startsWith('blob:') || 
    v.startsWith('/');

const isInvalidImage = (v) => {
    if (!v || typeof v !== 'string') return true;
    const lower = v.toLowerCase();
    return (
        lower.includes('via.placeholder.com') || 
        lower.includes('placeholder.com') || 
        lower.includes('dummyimage.com') ||
        lower.includes('images.unsplash.com')
    );
};

const resolveImg = (v) => {
    if (!v || isInvalidImage(v)) return '';
    if (typeof v !== 'string') return v;
    if (isAbsoluteUrl(v)) return v;
    return dichVuFile.layUrlFile(v.replace(/^\/+/, ''));
};

const getImageUrl = (p) => {
    if (!p) return defaultShoeImg;
    let raw = null;
    if (p.variants && p.variants.length > 0) {
        const v = p.variants[0];
        const vImg = v.hinhAnh || (v.images && v.images.length > 0 ? (v.images[0].duongDanAnh || v.images[0].hinhAnh) : null);
        if (!isInvalidImage(vImg)) {
            raw = vImg;
        }
    }
    if (!raw && p.images && p.images.length > 0) {
        const pImg = p.images[0].duongDanAnh || p.images[0].hinhAnh;
        if (!isInvalidImage(pImg)) {
            raw = pImg;
        }
    }
    if (!raw && !isInvalidImage(p.hinhAnh)) {
        raw = p.hinhAnh;
    }
    const resolved = resolveImg(raw);
    return (resolved && !isInvalidImage(resolved)) ? resolved : defaultShoeImg;
};

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const getProductPrice = (p) => {
    return p.giaBanThapNhat ?? p.giaBan ?? 0;
};

const getOldPrice = (p) => {
    const price = getProductPrice(p);
    if (!price || !p.phanTramGiam || p.phanTramGiam >= 100) return null;
    return price / (1 - p.phanTramGiam / 100);
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

// Real-time synchronization of favorites count with the header
const favoriteIds = ref([]);
const updateFavoriteIds = () => {
    favoriteIds.value = JSON.parse(localStorage.getItem('aerostride_favorites') || '[]');
};

const toggleFavorite = (productId, event) => {
    if (event) event.stopPropagation();
    let favorites = JSON.parse(localStorage.getItem('aerostride_favorites') || '[]');
    if (favorites.includes(productId)) {
        favorites = favorites.filter(id => id !== productId);
        localStorage.setItem('aerostride_favorites', JSON.stringify(favorites));
    } else {
        favorites.push(productId);
        localStorage.setItem('aerostride_favorites', JSON.stringify(favorites));
    }
    window.dispatchEvent(new Event('favorites-updated'));
    updateFavoriteIds();
};

const isFavorite = (productId) => favoriteIds.value.includes(productId);

// Sorting Label computed helper
const activeSortLabel = computed(() => {
    if (searchParams.value.sortBy === 'newest') return 'Nổi bật';
    if (searchParams.value.sortBy === 'price_asc') return 'Giá: Thấp - Cao';
    if (searchParams.value.sortBy === 'price_desc') return 'Giá: Cao - Thấp';
    return 'Nổi bật';
});
</script>

<template>
    <div class="shoe-listing-page bg-layout min-vh-100">
        <!-- Global Header -->
        <MainHeader />

        <div class="header-spacing"></div>

        <v-container fluid class="main-catalog-container py-8 px-16">
            <!-- Breadcrumbs -->
            <div class="breadcrumbs-row mb-2">
                <span class="crumb-link" @click="router.push('/')">Trang chủ</span>
                <span class="crumb-sep">/</span>
                <span class="crumb-active">Giày thể thao</span>
            </div>

            <!-- Page Title and Count & Sort Section -->
            <div class="d-flex align-center justify-space-between mb-6">
                <div class="title-details">
                    <h1 class="page-catalog-title">Giày thể thao</h1>
                    <span class="product-count-label">{{ totalElements }} sản phẩm</span>
                </div>

                <!-- Custom Sort Dropdown -->
                <v-menu offset-y>
                    <template v-slot:activator="{ props }">
                        <div class="custom-sort-card-btn" v-bind="props">
                            <span>Sắp xếp: {{ activeSortLabel }}</span>
                            <v-icon size="16" class="ml-2">mdi-chevron-down</v-icon>
                        </div>
                    </template>
                    <v-list class="sort-dropdown-menu">
                        <v-list-item
                            @click="
                                searchParams.sortBy = 'newest';
                                handleFilterChange();
                            "
                            class="sort-menu-item"
                        >
                            <v-list-item-title>Nổi bật</v-list-item-title>
                        </v-list-item>
                        <v-list-item
                            @click="
                                searchParams.sortBy = 'price_asc';
                                handleFilterChange();
                            "
                            class="sort-menu-item"
                        >
                            <v-list-item-title>Giá: Thấp - Cao</v-list-item-title>
                        </v-list-item>
                        <v-list-item
                            @click="
                                searchParams.sortBy = 'price_desc';
                                handleFilterChange();
                            "
                            class="sort-menu-item"
                        >
                            <v-list-item-title>Giá: Cao - Thấp</v-list-item-title>
                        </v-list-item>
                    </v-list>
                </v-menu>
            </div>

            <v-row class="mt-2">
                <!-- Left Sidebar Filters (Mockup specs: 250px, border-radius-18, background white) -->
                <v-col cols="12" md="3" lg="2.5" class="pr-6">
                    <div class="sidebar-filter-panel">
                        <!-- Filters Header -->
                        <div class="filter-header-row mb-6">
                            <span class="filter-main-title">BỘ LỌC</span>
                            <span class="filter-reset-action" @click="resetFilters">Đặt lại</span>
                        </div>

                        <!-- Brand Combobox Group -->
                        <div class="filter-section-group mb-4">
                            <h4 class="filter-group-title">THƯƠNG HIỆU</h4>
                            <v-select
                                v-model="searchParams.thuongHieuId"
                                :items="brandList"
                                item-title="ten"
                                item-value="id"
                                placeholder="Tất cả thương hiệu"
                                variant="outlined"
                                density="compact"
                                clearable
                                hide-details
                                class="custom-filter-select"
                                @update:model-value="handleFilterChange"
                            ></v-select>
                        </div>

                        <!-- Price Range Combobox Group -->
                        <div class="filter-section-group mb-6">
                            <h4 class="filter-group-title">KHOẢNG GIÁ</h4>
                            <v-select
                                v-model="priceRange"
                                :items="priceRangeOptions"
                                item-title="title"
                                item-value="value"
                                placeholder="Tất cả mức giá"
                                variant="outlined"
                                density="compact"
                                clearable
                                hide-details
                                class="custom-filter-select"
                            ></v-select>
                        </div>

                        <!-- Purpose Checkboxes Group -->
                        <div class="filter-section-group mb-6">
                            <h4 class="filter-group-title">MỤC ĐÍCH</h4>
                            <div class="checkboxes-list">
                                <label 
                                    v-for="purpose in purposeList" 
                                    :key="purpose.id" 
                                    class="checkbox-item-row"
                                >
                                    <input 
                                        type="checkbox" 
                                        :checked="searchParams.mucDichChayId === purpose.id" 
                                        @change="searchParams.mucDichChayId = (searchParams.mucDichChayId === purpose.id ? null : purpose.id); handleFilterChange()"
                                        class="custom-check-input"
                                    />
                                    <span class="checkbox-label-text">{{ purpose.ten }}</span>
                                </label>
                                <div v-if="purposeList.length === 0" class="empty-list-indicator">
                                    Không có mục đích
                                </div>
                            </div>
                        </div>

                        <!-- Shoe Sizes Grid -->
                        <div class="filter-section-group">
                            <h4 class="filter-group-title">KÍCH CỠ</h4>
                            <div class="sizes-boxes-grid">
                                <div 
                                    v-for="size in sizeList" 
                                    :key="size"
                                    class="size-box-cell"
                                    :class="{ active: selectedSize === size }"
                                    @click="selectedSize = (selectedSize === size ? null : size)"
                                >
                                    {{ size }}
                                </div>
                            </div>
                        </div>
                    </div>
                </v-col>

                <!-- Right Products Grid (Mockup specs: 3 columns layout, 310px width, rounded-18 card) -->
                <v-col cols="12" md="9" lg="9.5">
                    <v-row v-if="products.length > 0" class="products-list-row">
                        <v-col 
                            v-for="p in products" 
                            :key="p.id" 
                            cols="12" 
                            sm="4" 
                            md="4"
                            lg="3"
                            class="product-col-item"
                        >
                            <div class="product-item-card" @click="goToDetail(p.id)">
                                <!-- Image box in card (286x238 layout specification, F2F7FC background, 14px border-radius) -->
                                <div class="card-image-wrapper">
                                    <img 
                                        :src="getImageUrl(p)" 
                                        :alt="p.tenSanPham" 
                                        class="card-shoe-img" 
                                        referrerpolicy="no-referrer"
                                        @error="handleImageError"
                                    />
                                    <!-- Badges -->
                                    <div v-if="p.phanTramGiam > 0" class="badge-label-new">
                                        -{{ p.phanTramGiam }}%
                                    </div>
                                    <div v-else class="badge-label-new">
                                        MỚI
                                    </div>

                                    <!-- Favorite Button -->
                                    <div class="favorite-overlay-btn" @click.stop="(e) => toggleFavorite(p.id, e)">
                                        <v-icon 
                                            :color="isFavorite(p.id) ? 'red' : 'grey-darken-1'"
                                            size="20"
                                        >
                                            {{ isFavorite(p.id) ? 'mdi-heart' : 'mdi-heart-outline' }}
                                        </v-icon>
                                    </div>
                                </div>

                                <!-- Card Details Info -->
                                <div class="card-info-wrapper">
                                    <span class="product-brand-badge">{{ p.tenThuongHieu || 'AEROSTRIDE' }}</span>
                                    <h4 class="product-name-title">{{ p.tenSanPham }}</h4>
                                    
                                    <div class="price-row-block">
                                        <span class="current-price-label">{{ formatPrice(getProductPrice(p)) }}</span>
                                        <span v-if="p.phanTramGiam > 0 && getOldPrice(p)" class="old-price-label">
                                            {{ formatPrice(getOldPrice(p)) }}
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </v-col>
                    </v-row>

                    <!-- Loading / Empty States -->
                    <div v-else-if="!loading" class="empty-state-card text-center py-16">
                        <v-icon size="64" color="grey-lighten-1">mdi-package-variant</v-icon>
                        <p class="text-h6 text-grey-darken-1 mt-4">Không tìm thấy sản phẩm nào.</p>
                    </div>

                    <!-- Custom Pagination layout (← 1 2 3 4 … 12 →) -->
                    <div class="pagination-wrapper mt-12" v-if="totalElements > pageSize">
                        <v-pagination
                            v-model="currentPage"
                            :length="Math.ceil(totalElements / pageSize)"
                            @update:model-value="onPageChange"
                            color="#2962FF"
                            class="custom-nav-pagination"
                        ></v-pagination>
                    </div>
                </v-col>
            </v-row>
        </v-container>

        <!-- Customer Chat Overlay -->
        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.shoe-listing-page {
    background: #F9FAFC;
    min-height: 100vh;
    font-family: 'Inter', sans-serif;
    color: #0A1329;
}

.header-spacing {
    height: 120px; /* 36px announcement bar + 84px header */
}

/* Breadcrumbs styles */
.breadcrumbs-row {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: #667387;
}

.crumb-link {
    cursor: pointer;
    transition: color 0.2s;

    &:hover {
        color: #2962FF;
    }
}

.crumb-sep {
    color: #667387;
    opacity: 0.6;
}

.crumb-active {
    font-weight: 500;
}

/* Page title components */
.title-details {
    display: flex;
    flex-direction: column;
}

.page-catalog-title {
    font-family: 'Outfit', sans-serif;
    font-size: 36px;
    font-weight: 700;
    line-height: 1.2;
    color: #0A1329;
}

.product-count-label {
    font-size: 14px;
    color: #667387;
    margin-top: 4px;
}

/* Custom Sort button card */
.custom-sort-card-btn {
    background: #ffffff;
    border-radius: 10px;
    padding: 10px 18px;
    font-size: 13px;
    font-weight: 600;
    color: #0A1329;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(0,0,0,0.02);
    border: 1px solid rgba(229, 235, 245, 0.8);
    display: flex;
    align-items: center;
    transition: all 0.2s;

    &:hover {
        border-color: #2962FF;
        color: #2962FF;
    }
}

.sort-dropdown-menu {
    border-radius: 8px;
    box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05);
    border: 1px solid rgba(229, 235, 245, 0.8);
}

.sort-menu-item {
    font-size: 13px;
    font-weight: 500;
    color: #0A1329;
    cursor: pointer;

    &:hover {
        background-color: #F2F7FC;
        color: #2962FF;
    }
}

/* Sidebar Filters (specs: 250px width, border-radius-18, background white) */
.sidebar-filter-panel {
    background: #ffffff;
    border-radius: 18px;
    padding: 24px;
    border: 1px solid rgba(229, 235, 245, 0.6);
    box-shadow: 0 4px 15px rgba(0,0,0,0.01);
}

.filter-header-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #F0F4FA;
    padding-bottom: 14px;
}

.filter-main-title {
    font-size: 15px;
    font-weight: 700;
    color: #0A1329;
    letter-spacing: 0.5px;
}

.filter-reset-action {
    font-size: 12px;
    font-weight: 600;
    color: #2962FF;
    cursor: pointer;
    transition: opacity 0.2s;

    &:hover {
        opacity: 0.8;
    }
}

.filter-group-title {
    font-size: 12px;
    font-weight: 700;
    color: #0A1329;
    margin-bottom: 12px;
    letter-spacing: 0.5px;
}

.checkboxes-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.checkbox-item-row {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
}

.custom-check-input {
    width: 16px;
    height: 16px;
    cursor: pointer;
    accent-color: #2962FF;
}

.checkbox-label-text {
    font-size: 13px;
    color: #667387;
    transition: color 0.2s;

    &:hover {
        color: #0A1329;
    }
}

.empty-list-indicator {
    font-size: 12px;
    color: #a0aec0;
    font-style: italic;
}

/* Shoe Sizes Grid selector (Mockup specifications) */
.sizes-boxes-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
}

.size-box-cell {
    height: 38px;
    border: 1px solid #E5EBF5;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    font-weight: 500;
    color: #667387;
    cursor: pointer;
    background: #ffffff;
    transition: all 0.2s;

    &:hover {
        border-color: #2962FF;
        color: #2962FF;
        background: #F2F7FC;
    }

    &.active {
        background: #2962FF;
        border-color: #2962FF;
        color: #ffffff;
        font-weight: 700;
    }
}

/* Right Products Grid (310px width, 372px height, border-radius-18 card) */
.products-list-row {
    margin: 0 !important;
}

.product-col-item {
    display: flex;
    justify-content: center;
    padding: 5px !important;
}
.product-item-card {
    background: #ffffff;
    border-radius: 18px;
    padding: 11px 10px;
    width: 100%;
    max-width: none;
    min-height: 334px;
    height: auto;
    cursor: pointer;
    box-shadow: 0 4px 15px rgba(0,0,0,0.01);
    border: 1px solid rgba(229, 235, 245, 0.5);
    display: flex;
    flex-direction: column;
    transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s ease;
    margin: 0 auto;

    &:hover {
        transform: translateY(-6px);
        box-shadow: 0 15px 30px rgba(41, 98, 255, 0.05);
        border-color: rgba(41, 98, 255, 0.12);

        .card-shoe-img {
            transform: scale(1.05) rotate(-2deg);
        }
    }
}

.card-image-wrapper {
    background: #F2F7FC;
    border-radius: 14px;
    width: 100%;
    height: 211px;
    position: relative;
    overflow: hidden;
    display: block;
}

.card-shoe-img {
    width: 100%;
    height: 100%;
    max-height: 211px;
    object-fit: cover;
    display: block;
    transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.badge-label-new {
    position: absolute;
    top: 14px;
    left: 14px;
    color: #2962FF;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.5px;
    text-transform: uppercase;
}

.favorite-overlay-btn {
    position: absolute;
    top: 14px;
    right: 14px;
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    transition: background-color 0.2s;

    &:hover {
        background-color: rgba(239, 68, 68, 0.08);
    }
}

.card-info-wrapper {
    padding: 14px 4px 4px 4px;
    display: flex;
    flex-direction: column;
    flex-grow: 1;
}

.product-brand-badge {
    font-size: 11px;
    font-weight: 700;
    color: #2962FF;
    letter-spacing: 0.5px;
    text-transform: uppercase;
    margin-bottom: 4px;
}

.product-name-title {
    font-size: 15px;
    font-weight: 600;
    color: #0A1329;
    line-height: 1.3;
    height: 20px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-bottom: 8px;
}

.price-row-block {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: auto;
}

.current-price-label {
    font-size: 16px;
    font-weight: 700;
    color: #0A1329;
}

.old-price-label {
    font-size: 12px;
    color: #667387;
    text-decoration: line-through;
    font-weight: 500;
}

/* Pagination wrapper */
.pagination-wrapper {
    display: flex;
    justify-content: center;
    width: 100%;
}

.custom-nav-pagination {
    :deep(.v-pagination__item) {
        border-radius: 8px;
        font-weight: 600;
        font-size: 13px;
    }
}

.custom-filter-select {
    margin-top: 6px;
    :deep(.v-field) {
        border-radius: 8px !important;
        font-size: 13px !important;
        background-color: #F8FAFC !important;
        box-shadow: none !important;

        .v-field__outline {
            --v-field-border-width: 1px !important;
            --v-field-border-opacity: 0.15 !important;
            border-color: #CBD5E1 !important;
        }

        &.v-field--focused {
            .v-field__outline {
                --v-field-border-opacity: 1 !important;
                border-color: #2962FF !important;
            }
        }
    }
    :deep(.v-select__selection-text) {
        color: #0A1329 !important;
        font-weight: 500 !important;
    }
}

.empty-state-card {
    background: #ffffff;
    border-radius: 18px;
    border: 1px dashed rgba(229, 235, 245, 1);
}

/* Responsiveness overrides */
@media (max-width: 960px) {
    .main-catalog-container {
        padding: 16px;
    }
    .sidebar-filter-panel {
        margin-bottom: 24px;
    }
}
</style>
