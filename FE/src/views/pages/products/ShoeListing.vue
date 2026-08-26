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

const handleImageError = (e, productId) => {
    const target = e?.target || (e && e.tagName ? e : null);
    if (!target) return;
    if (target.getAttribute('data-fallback') === 'true') return;
    target.setAttribute('data-fallback', 'true');
    target.src = DEFAULT_SHOE_IMAGE;
    if (productId) {
        imageFallbacks.value[productId] = DEFAULT_SHOE_IMAGE;
    }
};
const totalElements = ref(0);
const currentPage = ref(1);
const pageSize = ref(12);

const cleanSearchValue = (v) => {
    if (v === null || v === undefined || v === '' || v === 'null' || v === 'undefined') {
        return null;
    }
    return v;
};

const searchParams = ref({
    keyword: route.query.keyword || '',
    thuongHieuId: route.query.thuongHieuId || null,
    chatLieuId: route.query.chatLieuId || null,
    xuatXuId: route.query.xuatXuId || null,
    mucDichChayIds: [],
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
const selectedSizes = ref([]);
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

// Watch size selection to update query parameters (multi-select: support multiple sizes)
watch(
    selectedSizes,
    (newSizes) => {
        searchParams.value.kichThuocs = newSizes.length > 0 ? [...newSizes] : [];
        searchParams.value.kichThuoc = newSizes.length > 0 ? newSizes.join(',') : null;
        handleFilterChange();
    },
    { deep: true }
);

// Map backend filter categories dynamically
const brandList = computed(() => {
    const group = filters.value.find((f) => f.key === 'thuongHieuId');
    return group ? group.items : [];
});

const purposeList = computed(() => {
    const group = filters.value.find((f) => f.key === 'mucDichChayId');
    return group ? group.items : [];
});

const fetchFilters = async () => {
    try {
        const data = await dichVuSanPhamPublic.layBoLoc();
        filters.value = [
            { title: 'Thương hiệu', key: 'thuongHieuId', items: data?.thuongHieus || [] },
            { title: 'Chất liệu', key: 'chatLieuId', items: data?.chatLieus || [] },
            { title: 'Xuất xứ', key: 'xuatXuId', items: data?.xuatXus || [] },
            { title: 'Mục đích', key: 'mucDichChayId', items: data?.mucDichChays || [] },
            {
                title: 'Giới tính',
                key: 'gioiTinhKhachHang',
                items: (data?.gioiTinhKhachHangs || []).filter((g) => g !== 'TRE_EM').map((g) => ({ id: g, ten: translateGender(g) }))
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
            page: currentPage.value - 1,
            size: pageSize.value
        };

        Object.keys(searchParams.value).forEach((key) => {
            const val = searchParams.value[key];
            // Handle arrays (mucDichChayIds, kichThuocs, etc.)
            if (Array.isArray(val)) {
                if (val.length > 0) {
                    params[key] = val.join(',');
                }
                return;
            }
            const cleanVal = cleanSearchValue(val);
            if (cleanVal !== null) {
                params[key] = cleanVal;
            }
        });

        const response = await dichVuSanPhamPublic.layDanhSachSanPham(params);
        products.value = response?.content || [];
        totalElements.value = response?.totalElements || 0;
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
        mucDichChayIds: [],
        gioiTinhKhachHang: null,
        sortBy: 'newest',
        minGia: null,
        maxGia: null,
        kichThuoc: null,
        kichThuocs: []
    };
    priceRange.value = null;
    selectedSizes.value = [];
    currentPage.value = 1;
    router.replace({ path: route.path, query: {} });
    fetchProducts();
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
    if (route.query.kichThuoc) {
        selectedSizes.value = String(route.query.kichThuoc).split(',').map((s) => s.trim()).filter(Boolean);
    }
    fetchFilters();
    fetchProducts().then(() => nextTick(updateSeo));
    updateFavoriteIds();
    window.addEventListener('favorites-updated', updateFavoriteIds);
});

onUnmounted(() => {
    window.removeEventListener('favorites-updated', updateFavoriteIds);
});

watch(totalElements, () => {
    updateSeo();
});

watch(
    () => route.query,
    (newQuery) => {
        searchParams.value = { ...searchParams.value, ...newQuery };
        if (newQuery.kichThuoc) {
            selectedSizes.value = String(newQuery.kichThuoc).split(',').map((s) => s.trim()).filter(Boolean);
        }
        fetchProducts();
    }
);

// Image Path Resolver
const isAbsoluteUrl = (v) =>
    typeof v !== 'string' ||
    /^(https?:)?\/\//i.test(v) ||
    v.startsWith('data:') ||
    v.startsWith('blob:') ||
    (v.startsWith('/') && !v.startsWith('/uploads/'));

const isInvalidImage = (v) => {
    if (!v || typeof v !== 'string') return true;
    const lower = v.toLowerCase();
    return lower.includes('via.placeholder.com') || lower.includes('placeholder.com') || lower.includes('dummyimage.com');
};

const resolveImg = (v) => {
    if (!v || isInvalidImage(v)) return '';
    if (typeof v !== 'string') return v;
    if (isAbsoluteUrl(v)) return v;
    return dichVuFile.layUrlFile(v);
};

const getImageUrl = (p) => {
    if (!p) return DEFAULT_SHOE_IMAGE;
    if (p.id && imageFallbacks.value[p.id]) {
        return imageFallbacks.value[p.id];
    }
    let raw = null;
    if (p.hinhAnh && !isInvalidImage(p.hinhAnh)) {
        raw = p.hinhAnh;
    }
    if (!raw && p.variants && p.variants.length > 0) {
        for (const v of p.variants) {
            const vImg = v.hinhAnh || (v.images && v.images.length > 0 ? v.images[0].duongDanAnh || v.images[0].hinhAnh : null);
            if (vImg && !isInvalidImage(vImg)) {
                raw = vImg;
                break;
            }
        }
    }
    if (!raw && p.images && p.images.length > 0) {
        for (const img of p.images) {
            const pImg = img.duongDanAnh || img.hinhAnh;
            if (pImg && !isInvalidImage(pImg)) {
                raw = pImg;
                break;
            }
        }
    }
    const resolved = resolveImg(raw);
    return resolved && !isInvalidImage(resolved) ? resolved : DEFAULT_SHOE_IMAGE;
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

import { getFavoriteIds, toggleFavorite as toggleFavUtil } from '@/utils/favoritesUtil';

// Real-time synchronization of favorites count with the header
const favoriteIds = ref([]);
const updateFavoriteIds = () => {
    favoriteIds.value = getFavoriteIds();
};

const toggleFavorite = (productId, event) => {
    if (event) event.stopPropagation();
    toggleFavUtil(productId);
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

// ── Mobile filter drawer ──────────────────────────────────────────────────────
const isMobileFilterOpen = ref(false);

const activeFilterCount = computed(() => {
    let count = 0;
    if (searchParams.value.thuongHieuId) count++;
    if (priceRange.value) count++;
    if (searchParams.value.mucDichChayIds && searchParams.value.mucDichChayIds.length > 0) count++;
    if (selectedSizes.value.length > 0) count++;
    return count;
});

const closeMobileFilter = () => {
    isMobileFilterOpen.value = false;
};
</script>

<template>
    <div class="shoe-listing-page bg-layout min-vh-100">
        <!-- Global Header -->
        <MainHeader />

        <div class="header-spacing"></div>

        <v-container fluid class="main-catalog-container py-8">
            <!-- Breadcrumbs -->
            <div class="breadcrumbs-row mb-2">
                <span class="crumb-link" @click="router.push('/')">Trang chủ</span>
                <span class="crumb-sep">/</span>
                <span class="crumb-active">Giày thể thao</span>
            </div>

            <!-- Page Title + Sort row -->
            <div class="title-sort-row mb-6">
                <div class="title-details">
                    <h1 class="page-catalog-title">Giày thể thao</h1>
                    <span class="product-count-label">{{ totalElements }} sản phẩm</span>
                </div>

                <div class="title-row-actions">
                    <!-- Mobile: Filter trigger button -->
                    <button class="mobile-filter-btn" @click="isMobileFilterOpen = true">
                        <v-icon size="18">mdi-filter-variant</v-icon>
                        <span>Bộ lọc</span>
                        <span v-if="activeFilterCount > 0" class="filter-badge">{{ activeFilterCount }}</span>
                    </button>

                    <!-- Sort Dropdown -->
                    <v-menu offset-y>
                        <template v-slot:activator="{ props }">
                            <div class="custom-sort-card-btn" v-bind="props">
                                <span>Sắp xếp: {{ activeSortLabel }}</span>
                                <v-icon size="16" class="ml-2">mdi-chevron-down</v-icon>
                            </div>
                        </template>
                        <v-list class="sort-dropdown-menu">
                            <v-list-item @click="searchParams.sortBy = 'newest'; handleFilterChange();" class="sort-menu-item">
                                <v-list-item-title>Nổi bật</v-list-item-title>
                            </v-list-item>
                            <v-list-item @click="searchParams.sortBy = 'price_asc'; handleFilterChange();" class="sort-menu-item">
                                <v-list-item-title>Giá: Thấp - Cao</v-list-item-title>
                            </v-list-item>
                            <v-list-item @click="searchParams.sortBy = 'price_desc'; handleFilterChange();" class="sort-menu-item">
                                <v-list-item-title>Giá: Cao - Thấp</v-list-item-title>
                            </v-list-item>
                        </v-list>
                    </v-menu>
                </div>
            </div>

            <v-row class="mt-2">
                <!-- Left Sidebar Filters — desktop only -->
                <v-col cols="12" md="3" lg="2" class="pr-4 desktop-filter-col">
                    <div class="sidebar-filter-panel">
                        <div class="filter-header-row mb-6">
                            <span class="filter-main-title">BỘ LỌC</span>
                            <span class="filter-reset-action" @click="resetFilters">Đặt lại</span>
                        </div>

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

                        <div class="filter-section-group mb-6">
                            <h4 class="filter-group-title">MỤC ĐÍCH</h4>
                            <div class="checkboxes-list">
                                <label v-for="purpose in purposeList" :key="purpose.id" class="checkbox-item-row">
                                    <input
                                        type="checkbox"
                                        :checked="searchParams.mucDichChayIds.includes(purpose.id)"
                                        @change="
                                            () => {
                                                const idx = searchParams.mucDichChayIds.indexOf(purpose.id);
                                                if (idx >= 0) {
                                                    searchParams.mucDichChayIds.splice(idx, 1);
                                                } else {
                                                    searchParams.mucDichChayIds.push(purpose.id);
                                                }
                                                handleFilterChange();
                                            }
                                        "
                                        class="custom-check-input"
                                    />
                                    <span class="checkbox-label-text">{{ purpose.ten }}</span>
                                </label>
                                <div v-if="purposeList.length === 0" class="empty-list-indicator">Không có mục đích</div>
                            </div>
                        </div>

                        <div class="filter-section-group">
                            <h4 class="filter-group-title">KÍCH CỠ</h4>
                            <div class="sizes-boxes-grid">
                                <div
                                    v-for="size in sizeList"
                                    :key="size"
                                    class="size-box-cell"
                                    :class="{ active: selectedSizes.includes(size) }"
                                    @click="
                                        () => {
                                            const idx = selectedSizes.indexOf(size);
                                            if (idx >= 0) {
                                                selectedSizes.splice(idx, 1);
                                            } else {
                                                selectedSizes.push(size);
                                            }
                                        }
                                    "
                                >
                                    {{ size }}
                                </div>
                            </div>
                        </div>
                    </div>
                </v-col>

                <!-- Right Products Grid -->
                <v-col cols="12" md="9" lg="10">
                    <v-row v-if="products.length > 0" class="products-list-row">
                        <v-col v-for="p in products" :key="p.id" cols="6" sm="4" md="4" lg="3" class="product-col-item">
                            <div class="product-item-card" @click="goToDetail(p.id)">
                                <div class="card-image-wrapper">
                                    <img
                                        :src="getImageUrl(p)"
                                        :alt="p.tenSanPham"
                                        class="card-shoe-img"
                                        referrerpolicy="no-referrer"
                                        @error="(e) => handleImageError(e, p.id)"
                                    />
                                    <div v-if="p.phanTramGiam > 0" class="badge-label-new">-{{ p.phanTramGiam }}%</div>
                                    <div class="favorite-overlay-btn" @click.stop="(e) => toggleFavorite(p.id, e)">
                                        <v-icon :color="isFavorite(p.id) ? 'red' : 'grey-darken-1'" size="20">
                                            {{ isFavorite(p.id) ? 'mdi-heart' : 'mdi-heart-outline' }}
                                        </v-icon>
                                    </div>
                                </div>
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

                    <div v-else-if="!loading" class="empty-state-card text-center py-16">
                        <v-icon size="64" color="grey-lighten-1">mdi-package-variant</v-icon>
                        <p class="text-h6 text-grey-darken-1 mt-4">Không tìm thấy sản phẩm nào.</p>
                    </div>

                    <div class="pagination-wrapper mt-12" v-if="totalElements > pageSize">
                        <v-pagination
                            v-model="currentPage"
                            :length="Math.ceil(totalElements / pageSize)"
                            :total-visible="5"
                            @update:model-value="onPageChange"
                            color="#2962FF"
                            class="custom-nav-pagination"
                        ></v-pagination>
                    </div>
                </v-col>
            </v-row>
        </v-container>

        <!-- ── Mobile Filter Drawer ────────────────────────────────────────── -->
        <transition name="filter-overlay-fade">
            <div v-if="isMobileFilterOpen" class="mobile-filter-overlay" @click.self="closeMobileFilter">
                <transition name="filter-drawer-slide">
                    <div v-if="isMobileFilterOpen" class="mobile-filter-drawer">
                        <!-- Drawer header -->
                        <div class="mobile-filter-drawer-header">
                            <span class="filter-main-title">BỘ LỌC</span>
                            <div class="mobile-filter-header-actions">
                                <span class="filter-reset-action" @click="resetFilters">Đặt lại</span>
                                <button class="drawer-close-btn" @click="closeMobileFilter">
                                    <v-icon size="20">mdi-close</v-icon>
                                </button>
                            </div>
                        </div>

                        <!-- Scrollable filter content -->
                        <div class="mobile-filter-body">
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

                            <div class="filter-section-group mb-4">
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

                            <div class="filter-section-group mb-4">
                                <h4 class="filter-group-title">MỤC ĐÍCH</h4>
                                <div class="checkboxes-list">
                                    <label v-for="purpose in purposeList" :key="purpose.id" class="checkbox-item-row">
                                        <input
                                            type="checkbox"
                                            :checked="searchParams.mucDichChayId === purpose.id"
                                            @change="
                                                searchParams.mucDichChayId = searchParams.mucDichChayId === purpose.id ? null : purpose.id;
                                                handleFilterChange();
                                            "
                                            class="custom-check-input"
                                        />
                                        <span class="checkbox-label-text">{{ purpose.ten }}</span>
                                    </label>
                                    <div v-if="purposeList.length === 0" class="empty-list-indicator">Không có mục đích</div>
                                </div>
                            </div>

                            <div class="filter-section-group mb-6">
                                <h4 class="filter-group-title">KÍCH CỠ</h4>
                                <div class="sizes-boxes-grid">
                                    <div
                                        v-for="size in sizeList"
                                        :key="size"
                                        class="size-box-cell"
                                        :class="{ active: selectedSize === size }"
                                        @click="selectedSize = selectedSize === size ? null : size"
                                    >
                                        {{ size }}
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Drawer footer -->
                        <div class="mobile-filter-drawer-footer">
                            <button class="filter-apply-btn" @click="closeMobileFilter">
                                Xem {{ totalElements }} sản phẩm
                            </button>
                        </div>
                    </div>
                </transition>
            </div>
        </transition>

        <!-- Customer Chat Overlay -->
        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.shoe-listing-page {
    background: #f9fafc;
    min-height: 100vh;
    font-family: 'Inter', sans-serif;
    color: #0a1329;
}

/* ── Header spacing (matches MainHeader height at each breakpoint) ── */
.header-spacing {
    height: 120px; /* 36px announcement + 84px navbar */

    @media (max-width: 768px) {
        height: 96px;
    }
    @media (max-width: 480px) {
        height: 100px;
    }
}

/* ── Container padding ────────────────────────────────────────────── */
.main-catalog-container {
    padding-left: 48px !important;
    padding-right: 48px !important;

    @media (max-width: 1024px) {
        padding-left: 24px !important;
        padding-right: 24px !important;
    }
    @media (max-width: 768px) {
        padding-left: 14px !important;
        padding-right: 14px !important;
        padding-top: 16px !important;
        padding-bottom: 16px !important;
    }
}

/* ── Breadcrumbs ──────────────────────────────────────────────────── */
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
    &:hover { color: #2962ff; }
}

.crumb-sep { color: #667387; opacity: 0.6; }
.crumb-active { font-weight: 500; }

/* ── Title + Sort row ─────────────────────────────────────────────── */
.title-sort-row {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
}

.title-row-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
}

.title-details {
    display: flex;
    flex-direction: column;
}

.page-catalog-title {
    font-family: 'Outfit', sans-serif;
    font-size: 36px;
    font-weight: 700;
    line-height: 1.2;
    color: #0a1329;

    @media (max-width: 768px) { font-size: 22px; }
    @media (max-width: 480px) { font-size: 20px; }
}

.product-count-label {
    font-size: 14px;
    color: #667387;
    margin-top: 4px;

    @media (max-width: 768px) { font-size: 12px; margin-top: 2px; }
}

/* ── Custom Sort button ────────────────────────────────────────────── */
.custom-sort-card-btn {
    background: #ffffff;
    border-radius: 10px;
    padding: 10px 18px;
    font-size: 13px;
    font-weight: 600;
    color: #0a1329;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
    border: 1px solid rgba(229, 235, 245, 0.8);
    display: flex;
    align-items: center;
    transition: all 0.2s;
    white-space: nowrap;

    &:hover { border-color: #2962ff; color: #2962ff; }

    @media (max-width: 480px) {
        padding: 8px 12px;
        font-size: 12px;
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
    color: #0a1329;
    cursor: pointer;
    &:hover { background-color: #f2f7fc; color: #2962ff; }
}

/* ── Mobile filter trigger button (hidden on desktop) ─────────────── */
.mobile-filter-btn {
    display: none;
    align-items: center;
    gap: 6px;
    background: #ffffff;
    border: 1px solid rgba(229, 235, 245, 0.8);
    border-radius: 10px;
    padding: 10px 14px;
    font-family: 'Inter', sans-serif;
    font-size: 13px;
    font-weight: 600;
    color: #0a1329;
    cursor: pointer;
    position: relative;
    transition: all 0.2s;

    &:hover { border-color: #2962ff; color: #2962ff; }

    @media (max-width: 959px) { display: flex; }
    @media (max-width: 480px) { padding: 8px 12px; font-size: 12px; }
}

.filter-badge {
    background: #2962ff;
    color: #fff;
    font-size: 10px;
    font-weight: 700;
    min-width: 18px;
    height: 18px;
    border-radius: 9px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0 5px;
}

/* ── Desktop filter column (hidden on mobile/tablet) ──────────────── */
.desktop-filter-col {
    @media (max-width: 959px) { display: none !important; }
}

/* ── Sidebar Filters ──────────────────────────────────────────────── */
.sidebar-filter-panel {
    background: #ffffff;
    border-radius: 18px;
    padding: 24px;
    border: 1px solid rgba(229, 235, 245, 0.6);
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.01);
    position: sticky;
    top: 130px;
}

.filter-header-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #f0f4fa;
    padding-bottom: 14px;
}

.filter-main-title {
    font-size: 15px;
    font-weight: 700;
    color: #0a1329;
    letter-spacing: 0.5px;
}

.filter-reset-action {
    font-size: 12px;
    font-weight: 600;
    color: #2962ff;
    cursor: pointer;
    transition: opacity 0.2s;
    &:hover { opacity: 0.8; }
}

.filter-group-title {
    font-size: 12px;
    font-weight: 700;
    color: #0a1329;
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
    accent-color: #2962ff;
}

.checkbox-label-text {
    font-size: 13px;
    color: #667387;
    transition: color 0.2s;
    &:hover { color: #0a1329; }
}

.empty-list-indicator {
    font-size: 12px;
    color: #a0aec0;
    font-style: italic;
}

/* ── Shoe Sizes Grid ──────────────────────────────────────────────── */
.sizes-boxes-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
}

.size-box-cell {
    height: 38px;
    border: 1px solid #e5ebf5;
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

    &:hover { border-color: #2962ff; color: #2962ff; background: #f2f7fc; }
    &.active { background: #2962ff; border-color: #2962ff; color: #ffffff; font-weight: 700; }
}

/* ── Product Grid ─────────────────────────────────────────────────── */
.products-list-row {
    margin: 0 !important;
}

.product-col-item {
    display: flex;
    justify-content: center;
    padding: 5px !important;

    @media (max-width: 768px) {
        padding: 4px !important;
    }
}

.product-item-card {
    background: #ffffff;
    border-radius: 18px;
    padding: 11px 10px;
    width: 100%;
    max-width: none;
    min-height: 300px;
    height: auto;
    cursor: pointer;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.01);
    border: 1px solid rgba(229, 235, 245, 0.5);
    display: flex;
    flex-direction: column;
    transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s ease;
    margin: 0 auto;

    &:hover {
        transform: translateY(-6px);
        box-shadow: 0 15px 30px rgba(41, 98, 255, 0.05);
        border-color: rgba(41, 98, 255, 0.12);

        .card-shoe-img { transform: scale(1.05) rotate(-2deg); }
    }

    @media (max-width: 768px) {
        border-radius: 12px;
        padding: 8px;
        min-height: unset;
    }
}

.card-image-wrapper {
    background: #f2f7fc;
    border-radius: 14px;
    width: 100%;
    height: 180px;
    position: relative;
    overflow: hidden;
    display: block;

    @media (max-width: 768px) {
        height: 130px;
        border-radius: 8px;
    }
    @media (max-width: 480px) {
        height: 110px;
    }
}

.card-shoe-img {
    position: absolute !important;
    top: 0 !important; left: 0 !important;
    width: 100% !important; height: 100% !important;
    object-fit: cover;
    display: block;
    transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.badge-label-new {
    position: absolute;
    top: 10px;
    left: 10px;
    color: #2962ff;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.5px;
    text-transform: uppercase;

    @media (max-width: 768px) { font-size: 9px; top: 7px; left: 7px; }
}

.favorite-overlay-btn {
    position: absolute;
    top: 10px;
    right: 10px;
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    transition: background-color 0.2s;
    &:hover { background-color: rgba(239, 68, 68, 0.08); }

    @media (max-width: 768px) { width: 24px; height: 24px; top: 7px; right: 7px; }
}

.card-info-wrapper {
    padding: 12px 4px 4px 4px;
    display: flex;
    flex-direction: column;
    flex-grow: 1;

    @media (max-width: 768px) {
        padding: 8px 3px 3px;
    }
}

.product-brand-badge {
    font-size: 10px;
    font-weight: 700;
    color: #2962ff;
    letter-spacing: 0.5px;
    text-transform: uppercase;
    margin-bottom: 3px;

    @media (max-width: 768px) { font-size: 9px; }
}

.product-name-title {
    font-size: 14px;
    font-weight: 600;
    color: #0a1329;
    line-height: 1.3;
    height: 36px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin-bottom: 6px;

    @media (max-width: 768px) { font-size: 12px; height: 30px; margin-bottom: 4px; }
    @media (max-width: 480px) { font-size: 11px; height: 28px; }
}

.price-row-block {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
    margin-top: auto;
}

.current-price-label {
    font-size: 15px;
    font-weight: 700;
    color: #0a1329;
    @media (max-width: 768px) { font-size: 13px; }
    @media (max-width: 480px) { font-size: 12px; }
}

.old-price-label {
    font-size: 11px;
    color: #667387;
    text-decoration: line-through;
    font-weight: 500;
    @media (max-width: 768px) { font-size: 10px; }
}

/* ── Pagination ───────────────────────────────────────────────────── */
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

/* ── Filter Select Overrides ──────────────────────────────────────── */
.custom-filter-select {
    margin-top: 6px;
    :deep(.v-field) {
        border-radius: 8px !important;
        font-size: 13px !important;
        background-color: #f8fafc !important;
        box-shadow: none !important;

        .v-field__outline {
            --v-field-border-width: 1px !important;
            --v-field-border-opacity: 0.15 !important;
            border-color: #cbd5e1 !important;
        }

        &.v-field--focused .v-field__outline {
            --v-field-border-opacity: 1 !important;
            border-color: #2962ff !important;
        }
    }
    :deep(.v-select__selection-text) {
        color: #0a1329 !important;
        font-weight: 500 !important;
    }
}

/* ── Empty state ──────────────────────────────────────────────────── */
.empty-state-card {
    background: #ffffff;
    border-radius: 18px;
    border: 1px dashed rgba(229, 235, 245, 1);
}

/* ── Mobile Filter Drawer ─────────────────────────────────────────── */
.mobile-filter-overlay {
    position: fixed;
    inset: 0;
    background: rgba(10, 19, 41, 0.5);
    z-index: 1300;
    backdrop-filter: blur(2px);
}

.mobile-filter-drawer {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    width: 100%;
    max-height: 88vh;
    background: #ffffff;
    border-radius: 24px 24px 0 0;
    display: flex;
    flex-direction: column;
    box-shadow: 0 -8px 32px rgba(0, 0, 0, 0.12);
    overflow: hidden;
}

.mobile-filter-drawer-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 18px 20px 14px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    flex-shrink: 0;
}

.mobile-filter-header-actions {
    display: flex;
    align-items: center;
    gap: 16px;
}

.drawer-close-btn {
    background: #f5f7fa;
    border: none;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #0a1329;
    transition: background 0.2s;
    &:hover { background: #e8edf5; }
}

.mobile-filter-body {
    flex: 1;
    overflow-y: auto;
    padding: 20px 20px 8px;
    overscroll-behavior: contain;
    -webkit-overflow-scrolling: touch;
}

.mobile-filter-drawer-footer {
    padding: 12px 20px 24px;
    border-top: 1px solid rgba(0, 0, 0, 0.06);
    flex-shrink: 0;
}

.filter-apply-btn {
    width: 100%;
    height: 52px;
    background: #2962ff;
    color: #ffffff;
    border: none;
    border-radius: 14px;
    font-family: 'Inter', sans-serif;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
    transition: background 0.2s, transform 0.15s;
    &:hover { background: #1a4fd4; }
    &:active { transform: scale(0.98); }
}

/* ── Mobile Drawer Transitions ────────────────────────────────────── */
.filter-overlay-fade-enter-active,
.filter-overlay-fade-leave-active { transition: opacity 0.3s ease; }
.filter-overlay-fade-enter-from,
.filter-overlay-fade-leave-to { opacity: 0; }

.filter-drawer-slide-enter-active,
.filter-drawer-slide-leave-active {
    transition: transform 0.38s cubic-bezier(0.4, 0, 0.2, 1);
}
.filter-drawer-slide-enter-from,
.filter-drawer-slide-leave-to { transform: translateY(100%); }
.filter-drawer-slide-enter-to,
.filter-drawer-slide-leave-from { transform: translateY(0); }
</style>


