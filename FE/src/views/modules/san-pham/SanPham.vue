<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { isActiveStatus, getStatusLabel, getStatusColor } from '@/utils/statusUtils';
import { formatCurrency } from '@/utils/formatters';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';
import { AdminFilter, AdminTable, AdminPagination, AdminConfirm, AdminBreadcrumbs } from '@/components/common';
import QrScanner from '@/views/modules/san-pham/components/QrScanner.vue';
import FormattedNumberField from '@/views/modules/san-pham/components/FormattedNumberField.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EditIcon, EyeIcon } from 'vue-tabler-icons';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useRefreshHandler } from '@/composables/useRefreshHandler';

const MIN_PRICE = 0;
const DEFAULT_MAX_PRICE = 100000000;
const PRICE_STEP = 500000;
const PRODUCT_FETCH_SIZE = 1000;

const { addNotification } = useNotifications();
const router = useRouter();
const { confirmDialog, setConfirm, handleConfirm } = useConfirmDialog();
const { isRefreshing, handleRefresh: refreshData } = useRefreshHandler();

const loading = ref(false);
const products = ref([]);
const filterOptions = ref({
    danhMucs: [],
    thuongHieus: [],
    xuatXus: [],
    mucDichChays: [],
    chatLieus: [],
    coGiays: [],
    deGiays: [],
    gioiTinhKhachHangs: ['NAM', 'NU', 'TRE_EM', 'UNISEX']
});
const selectedProductIds = ref([]);
const productPriceBounds = ref({
    min: MIN_PRICE,
    max: DEFAULT_MAX_PRICE
});
const showQrScanner = ref(false);
const importing = ref(false);
const priceFilterDirty = ref(false);

const pagination = reactive({
    page: 1,
    size: 5
});

const filters = reactive({
    search: '',
    khoangGia: [MIN_PRICE, DEFAULT_MAX_PRICE],
    trangThai: null,
    danhMuc: null,
    thuongHieu: null,
    gioiTinh: null,
    chatLieu: null
});

let priceSearchTimer = null;

const toNumber = (value, fallback = 0) => {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
};

const getProductPriceCandidates = (item) => {
    const nestedVariantCollections = [
        item?.variants,
        item?.chiTietSanPhams,
        item?.bienThes,
        item?.danhSachBienThe
    ].filter(Array.isArray);

    const nestedPrices = nestedVariantCollections.flatMap((variants) => variants
        .map((variant) => Number(variant?.giaBan))
        .filter((price) => Number.isFinite(price)));

    return [
        item?.giaBanThapNhat,
        item?.giaBanCaoNhat,
        item?.giaBan,
        ...nestedPrices
    ]
        .map((value) => Number(value))
        .filter((value) => Number.isFinite(value));
};

const getProductLowestPrice = (item) => {
    const priceCandidates = getProductPriceCandidates(item);
    return priceCandidates.length ? Math.min(...priceCandidates) : 0;
};

const getProductHighestPrice = (item) => {
    const priceCandidates = getProductPriceCandidates(item);
    return priceCandidates.length ? Math.max(...priceCandidates) : getProductLowestPrice(item);
};

const productPriceMatches = (item) => {
    const selectedMin = filters.khoangGia[0];
    const selectedMax = filters.khoangGia[1];
    const productMin = getProductLowestPrice(item);
    const productMax = getProductHighestPrice(item);
    return productMax >= selectedMin && productMin <= selectedMax;
};

const filteredProducts = computed(() => products.value.filter(productPriceMatches));
const totalElements = computed(() => filteredProducts.value.length);
const totalPages = computed(() => Math.max(Math.ceil(totalElements.value / pagination.size), 1));
const paginatedProducts = computed(() => {
    const start = (pagination.page - 1) * pagination.size;
    return filteredProducts.value.slice(start, start + pagination.size);
});
const visibleProductIds = computed(() => paginatedProducts.value.map((item) => item.id));
const selectedProducts = computed(() => filteredProducts.value.filter((item) => selectedProductIds.value.includes(item.id)));
const allVisibleProductsSelected = computed(() => visibleProductIds.value.length > 0
    && visibleProductIds.value.every((id) => selectedProductIds.value.includes(id)));
const someVisibleProductsSelected = computed(() => visibleProductIds.value.some((id) => selectedProductIds.value.includes(id))
    && !allVisibleProductsSelected.value);
const productExportButtonText = computed(() => selectedProductIds.value.length
    ? `Xuất Excel (${selectedProductIds.value.length})`
    : 'Xuất Excel');
const hasSelectedActiveProducts = computed(() => selectedProducts.value.some((item) => isActiveStatus(item.trangThai)));
const hasSelectedInactiveProducts = computed(() => selectedProducts.value.some((item) => !isActiveStatus(item.trangThai)));
const canBulkActivateProducts = computed(() => selectedProducts.value.length > 0 && hasSelectedInactiveProducts.value);
const canBulkDeactivateProducts = computed(() => selectedProducts.value.length > 0 && hasSelectedActiveProducts.value);

const clearProductSelection = () => {
    selectedProductIds.value = [];
};

const syncProductSelection = () => {
    const validIds = new Set(filteredProducts.value.map((item) => item.id));
    selectedProductIds.value = selectedProductIds.value.filter((id) => validIds.has(id));
};

const resetProductFiltersState = () => {
    filters.search = '';
    filters.khoangGia = [MIN_PRICE, productPriceBounds.value.max];
    filters.trangThai = null;
    filters.danhMuc = null;
    filters.thuongHieu = null;
    filters.gioiTinh = null;
    filters.chatLieu = null;
    pagination.page = 1;
    priceFilterDirty.value = false;
    clearProductSelection();
};

const buildProductQueryParams = () => ({
    page: 0,
    size: PRODUCT_FETCH_SIZE,
    keyword: filters.search?.trim() || undefined,
    danhMucId: filters.danhMuc || undefined,
    thuongHieuId: filters.thuongHieu || undefined,
    trangThai: filters.trangThai || undefined,
    gioiTinhKhachHang: filters.gioiTinh || undefined,
    chatLieuId: filters.chatLieu || undefined
});

const updateProductPriceBounds = (items) => {
    const detectedMaxPrice = items.reduce((maxValue, item) => Math.max(
        maxValue,
        getProductLowestPrice(item),
        getProductHighestPrice(item)
    ), MIN_PRICE);
    const safeMaxPrice = Math.max(detectedMaxPrice, PRICE_STEP);
    productPriceBounds.value = {
        min: MIN_PRICE,
        max: safeMaxPrice
    };

    if (!priceFilterDirty.value) {
        filters.khoangGia = [MIN_PRICE, safeMaxPrice];
        return;
    }

    filters.khoangGia = sanitizePriceRange(filters.khoangGia, safeMaxPrice);
};

const loadProducts = async () => {
    if (loading.value) return;

    loading.value = true;
    try {
        const response = await dichVuSanPham.layDanhSachSanPham(buildProductQueryParams());
        const result = response?.data || response;
        const fetchedProducts = Array.isArray(result?.content) ? result.content : [];
        products.value = fetchedProducts;
        updateProductPriceBounds(fetchedProducts);
        syncProductSelection();
    } catch (error) {
        console.error('Error loading products:', error);
        products.value = [];
        clearProductSelection();
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể tải danh sách sản phẩm',
            color: 'error'
        });
    } finally {
        loading.value = false;
    }
};

const loadMaxPrice = async () => {
    try {
        const maxPrice = await dichVuSanPham.layGiaLonNhat();
        if (maxPrice) {
            const safeMaxPrice = Math.max(maxPrice, PRICE_STEP);
            productPriceBounds.value.max = safeMaxPrice;
            if (!priceFilterDirty.value) {
                filters.khoangGia = [MIN_PRICE, safeMaxPrice];
            }
        }
    } catch (error) {
        console.error('Error loading max price:', error);
    }
};

const loadFilterOptions = async () => {
    try {
        const options = await dichVuSanPham.layOptionsForm();
        filterOptions.value.danhMucs = options?.danhMucs || [];
        filterOptions.value.thuongHieus = options?.thuongHieus || [];
        filterOptions.value.xuatXus = options?.xuatXus || [];
        filterOptions.value.mucDichChays = options?.mucDichChays || [];
        filterOptions.value.chatLieus = options?.chatLieus || [];
        filterOptions.value.coGiays = options?.coGiays || [];
        filterOptions.value.deGiays = options?.deGiays || [];
    } catch (error) {
        console.error('Error loading filter options:', error);
        addNotification({
            title: 'Cảnh báo',
            subtitle: 'Không thể tải đầy đủ bộ lọc, hệ thống sẽ hiển thị dữ liệu khả dụng',
            color: 'warning'
        });
    }
};

const handleSearch = async () => {
    pagination.page = 1;
    clearProductSelection();
    await loadProducts();
};

const handleRefresh = async () => {
    await refreshData(async () => {
        resetProductFiltersState();
        await loadProducts();
    });
};

const schedulePriceSearch = () => {
    priceFilterDirty.value = true;
    if (priceSearchTimer) {
        window.clearTimeout(priceSearchTimer);
    }

    priceSearchTimer = window.setTimeout(() => {
        pagination.page = 1;
        syncProductSelection();
    }, 120);
};

const sanitizePriceRange = (range, maxPrice = productPriceBounds.value.max) => {
    const safeMaxPrice = Math.max(MIN_PRICE, toNumber(maxPrice, DEFAULT_MAX_PRICE));
    const rawMin = Math.max(MIN_PRICE, toNumber(range?.[0], MIN_PRICE));
    const rawMax = Math.max(MIN_PRICE, toNumber(range?.[1], safeMaxPrice));
    const nextMin = Math.min(rawMin, safeMaxPrice);
    const nextMax = Math.min(Math.max(rawMax, nextMin), safeMaxPrice);
    return [nextMin, nextMax];
};

const updatePriceFilterBoundary = (boundary, value) => {
    const nextRange = [...filters.khoangGia];
    if (boundary === 'min') {
        nextRange[0] = value === '' ? MIN_PRICE : value;
    } else {
        nextRange[1] = value === '' ? productPriceBounds.value.max : value;
    }

    filters.khoangGia = sanitizePriceRange(nextRange);
    schedulePriceSearch();
};

const handleSliderPriceChange = (value) => {
    filters.khoangGia = sanitizePriceRange(value);
    schedulePriceSearch();
};

const escapeCell = (value) => String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');

const exportHtmlTableToExcel = ({ headers, rows, fileName }) => {
    const tableRows = [headers, ...rows]
        .map((columns) => `<tr>${columns.map((column) => `<td>${escapeCell(column)}</td>`).join('')}</tr>`)
        .join('');

    const htmlContent = `
        <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">
            <head>
                <meta charset="UTF-8" />
            </head>
            <body>
                <table>${tableRows}</table>
            </body>
        </html>
    `;

    const blob = new Blob(['\ufeff', htmlContent], {
        type: 'application/vnd.ms-excel;charset=utf-8;'
    });
    downloadFile(blob, fileName);
};

const getPriceRange = (item) => {
    if (item?.giaBanThapNhat == null && item?.giaBanCaoNhat == null) return '--';
    if (item?.giaBanThapNhat === item?.giaBanCaoNhat) return formatCurrency(item.giaBanThapNhat);
    return `${formatCurrency(item.giaBanThapNhat)} - ${formatCurrency(item.giaBanCaoNhat)}`;
};

const handleExportProducts = () => {
    const targetProducts = selectedProducts.value.length ? selectedProducts.value : filteredProducts.value;
    if (!targetProducts.length) {
        addNotification({
            title: 'Thông báo',
            subtitle: 'Không có sản phẩm để xuất Excel',
            color: 'warning'
        });
        return;
    }

    exportHtmlTableToExcel({
        headers: ['STT', 'Mã sản phẩm', 'Tên sản phẩm', 'Thương hiệu', 'Danh mục', 'Tổng số lượng', 'Khoảng giá', 'Trạng thái'],
        rows: targetProducts.map((item, index) => [
            index + 1,
            item.maSanPham || '--',
            item.tenSanPham || '--',
            item.tenThuongHieu || '--',
            item.tenDanhMuc || '--',
            toNumber(item.tongSoLuongTon, 0),
            getPriceRange(item),
            getStatusLabel(item.trangThai)
        ]),
        fileName: selectedProducts.value.length ? 'san_pham_da_chon.xls' : 'danh_sach_san_pham_da_loc.xls'
    });

    addNotification({
        title: 'Thành công',
        subtitle: `Đã xuất Excel ${targetProducts.length} sản phẩm`,
        color: 'success'
    });
};

const handleDownloadTemplate = async () => {
    try {
        const blob = await dichVuSanPham.taiTemplateExcel();
        downloadFile(blob, 'template_nhap_san_pham.xlsx');
    } catch (error) {
        console.error('Error downloading template:', error);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể tải template',
            color: 'error'
        });
    }
};

const handleImport = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    importing.value = true;
    try {
        await dichVuSanPham.nhapExcelSanPham(file);
        addNotification({
            title: 'Thành công',
            subtitle: 'Đã nhập dữ liệu từ Excel',
            color: 'success'
        });
        await loadProducts();
    } catch (error) {
        console.error('Lỗi nhập Excel:', error);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Lỗi khi nhập dữ liệu Excel',
            color: 'error'
        });
    } finally {
        importing.value = false;
        event.target.value = '';
    }
};

const updateSingleProductStatus = async (product, nextStatus) => {
    await dichVuSanPham.thayDoiTrangThai(product.id, nextStatus);
};

const confirmToggleStatus = (product) => {
    setConfirm({
        title: 'Xác nhận trạng thái',
        message: `Bạn có chắc chắn muốn thay đổi trạng thái sản phẩm [${product.tenSanPham}]?`,
        color: 'warning',
        action: async () => {
            try {
                const nextStatus = isActiveStatus(product.trangThai) ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
                await updateSingleProductStatus(product, nextStatus);
                await loadProducts();
                addNotification({
                    title: 'Thành công',
                    subtitle: `Đã cập nhật trạng thái sản phẩm [${product.tenSanPham}]`,
                    color: 'success'
                });
            } catch (error) {
                console.error('Status update error:', error);
                addNotification({
                    title: 'Lỗi',
                    subtitle: 'Không thể cập nhật trạng thái',
                    color: 'error'
                });
            }
        }
    });
};

const handleBulkProductStatus = (nextStatus) => {
    if (!selectedProducts.value.length) {
        addNotification({
            title: 'Thông báo',
            subtitle: 'Bạn chưa chọn sản phẩm nào',
            color: 'warning'
        });
        return;
    }

    const canApplyStatus = nextStatus === 'DANG_HOAT_DONG'
        ? canBulkActivateProducts.value
        : canBulkDeactivateProducts.value;
    if (!canApplyStatus) {
        addNotification({
            title: 'Thông báo',
            subtitle: nextStatus === 'DANG_HOAT_DONG'
                ? 'Các sản phẩm đã ở trạng thái bật'
                : 'Các sản phẩm đã ở trạng thái tắt',
            color: 'warning'
        });
        return;
    }

    const selectedCount = selectedProducts.value.length;
    const actionLabel = nextStatus === 'DANG_HOAT_DONG' ? 'bật hoạt động' : 'ngừng hoạt động';
    setConfirm({
        title: 'Cập nhật trạng thái hàng loạt',
        message: `Bạn có chắc chắn muốn ${actionLabel} ${selectedCount} sản phẩm đã chọn?`,
        color: 'warning',
        action: async () => {
            try {
                await Promise.all(selectedProducts.value.map((product) => updateSingleProductStatus(product, nextStatus)));
                clearProductSelection();
                await loadProducts();
                addNotification({
                    title: 'Thành công',
                    subtitle: `Đã cập nhật trạng thái ${selectedCount} sản phẩm`,
                    color: 'success'
                });
            } catch (error) {
                console.error('Bulk product status update error:', error);
                addNotification({
                    title: 'Lỗi',
                    subtitle: 'Không thể cập nhật trạng thái hàng loạt',
                    color: 'error'
                });
            }
        }
    });
};

const toggleProductSelection = (productId, checked) => {
    if (checked) {
        if (!selectedProductIds.value.includes(productId)) {
            selectedProductIds.value = [...selectedProductIds.value, productId];
        }
        return;
    }

    selectedProductIds.value = selectedProductIds.value.filter((id) => id !== productId);
};

const toggleSelectVisibleProducts = (checked) => {
    if (checked) {
        const mergedIds = new Set([...selectedProductIds.value, ...visibleProductIds.value]);
        selectedProductIds.value = Array.from(mergedIds);
        return;
    }

    const visibleIdSet = new Set(visibleProductIds.value);
    selectedProductIds.value = selectedProductIds.value.filter((id) => !visibleIdSet.has(id));
};

const formatNumber = (value) => {
    if (value === null || value === undefined) return '0';
    return new Intl.NumberFormat('vi-VN').format(Number(value));
};

const handleQrScan = async (decodedText) => {
    filters.search = decodedText;
    await handleSearch();
};

watch(filteredProducts, () => {
    syncProductSelection();
    if (pagination.page > totalPages.value) {
        pagination.page = totalPages.value;
    }
});

watch(() => pagination.size, () => {
    pagination.page = 1;
});

onMounted(async () => {
    await Promise.all([loadMaxPrice(), loadProducts(), loadFilterOptions()]);
});

onBeforeUnmount(() => {
    if (priceSearchTimer) {
        window.clearTimeout(priceSearchTimer);
    }
});
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body"
        style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important;">
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý sản phẩm', disabled: false, href: '#' },
            { title: 'Danh sách sản phẩm', disabled: true }
        ]" />

        <div class="mb-2"></div>

        <div class="filter-top">
            <AdminFilter @refresh="handleRefresh" :loading="isRefreshing">
                <v-col cols="12" md="3">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field v-model="filters.search" placeholder="Mã hoặc tên sản phẩm..."
                        prepend-inner-icon="mdi-magnify" variant="outlined" density="compact" hide-details clearable
                        class="compact-input" @update:model-value="handleSearch" />
                </v-col>

                <v-col cols="12" md="2">
                    <div class="filter-field-label">Danh mục</div>
                    <v-select v-model="filters.danhMuc" :items="[
                        { title: 'Tất cả danh mục', value: null },
                        ...filterOptions.danhMucs.map((danhMuc) => ({ title: danhMuc.ten, value: danhMuc.id }))
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch" />
                </v-col>

                <v-col cols="12" md="2">
                    <div class="filter-field-label">Thương hiệu</div>
                    <v-select v-model="filters.thuongHieu" :items="[
                        { title: 'Tất cả thương hiệu', value: null },
                        ...filterOptions.thuongHieus.map((thuongHieu) => ({ title: thuongHieu.ten, value: thuongHieu.id }))
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch" />
                </v-col>

                <v-col cols="12" md="2">
                    <div class="filter-field-label">Giới tính</div>
                    <v-select v-model="filters.gioiTinh" :items="[
                        { title: 'Tất cả', value: null },
                        { title: 'Nam', value: 'NAM' },
                        { title: 'Nữ', value: 'NU' },
                        { title: 'Trẻ em', value: 'TRE_EM' },
                        { title: 'Unisex', value: 'UNISEX' }
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch" />
                </v-col>

                <v-col cols="12" md="2">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select v-model="filters.trangThai" :items="[
                        { title: 'Tất cả', value: null },
                        { title: 'Hoạt động', value: 'DANG_HOAT_DONG' },
                        { title: 'Ngừng hoạt động', value: 'KHONG_HOAT_DONG' }
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch" />
                </v-col>

                <v-col cols="12" md="10" lg="11">
                    <div class="filter-field-label">Khoảng giá</div>
                    <div class="price-slider-wrap">
                        <div class="d-flex align-center bg-white pa-4 rounded-lg ">
                            <v-icon size="20" class="mr-4 text-primary">mdi-cash-multiple</v-icon>
                            <div class="flex-grow-1">
                                <div class="d-flex justify-space-between mb-2">
                                    <span class="text-caption font-weight-black text-slate-600">Lọc theo giá bán sản phẩm</span>
                                    <span class="text-caption font-weight-black text-primary">
                                        {{ formatCurrency(filters.khoangGia[0]) }} - {{ formatCurrency(filters.khoangGia[1]) }}
                                    </span>
                                </div>
                                <v-range-slider v-model="filters.khoangGia" :min="productPriceBounds.min"
                                    :max="productPriceBounds.max" :step="PRICE_STEP" hide-details color="primary"
                                    track-color="slate-200" @update:model-value="handleSliderPriceChange" />
                                <v-row class="mt-1" dense>
                                    <v-col cols="12" sm="6">
                                        <div class="text-caption font-weight-bold text-slate-500 mb-1">Từ giá</div>
                                        <FormattedNumberField :model-value="filters.khoangGia[0]" placeholder="0"
                                            variant="outlined" density="compact" hide-details class="bg-white"
                                            @update:model-value="updatePriceFilterBoundary('min', $event)" />
                                    </v-col>
                                    <v-col cols="12" sm="6">
                                        <div class="text-caption font-weight-bold text-slate-500 mb-1">Đến giá</div>
                                        <FormattedNumberField :model-value="filters.khoangGia[1]" placeholder="0"
                                            variant="outlined" density="compact" hide-details class="bg-white"
                                            @update:model-value="updatePriceFilterBoundary('max', $event)" />
                                    </v-col>
                                </v-row>
                            </div>
                        </div>
                    </div>
                </v-col>
            </AdminFilter>
        </div>

        <AdminTable title="Danh sách sản phẩm" addButtonText="Tạo mới" :headers="[
            { text: 'Chọn', width: '70px' },
            { text: 'STT', width: '50px' },
            { text: 'Mã sản phẩm', width: '120px' },
            { text: 'Tên sản phẩm', width: '220px' },
            { text: 'Thương hiệu', width: '140px' },
            { text: 'Danh mục', width: '140px' },
            { text: 'Tổng số lượng', width: '120px' },
            { text: 'Khoảng giá', width: '200px' },
            { text: 'Trạng thái', width: '130px' },
            { text: 'Hành động', width: '130px' }
        ]" :items="paginatedProducts" :loading="loading" :showExportButton="true"
            :exportButtonText="productExportButtonText" @add="router.push({ name: 'SanPhamForm' })"
            @export="handleExportProducts">
            <template #top>
                <div class="px-6 py-3 bg-slate-50 border-b d-flex align-center justify-space-between flex-wrap gap-3">
                    <div class="d-flex align-center flex-wrap gap-2">
                        <v-checkbox-btn :model-value="allVisibleProductsSelected"
                            :indeterminate="someVisibleProductsSelected" color="primary" hide-details density="compact"
                            @update:model-value="toggleSelectVisibleProducts" />
                        <span class="text-caption font-weight-black text-slate-500">
                            đã chọn {{ selectedProductIds.length }} sản phẩm
                        </span>
                    </div>
                </div>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell">
                        <v-checkbox-btn :model-value="selectedProductIds.includes(item.id)" color="primary"
                            hide-details density="compact" @update:model-value="toggleProductSelection(item.id, $event)" />
                    </td>

                    <td class="data-cell">
                        {{ (pagination.page - 1) * pagination.size + index + 1 }}
                    </td>

                    <td class="data-cell">
                        <div class="text-truncate" :title="item.maSanPham || '--'">{{ item.maSanPham || '--' }}</div>
                    </td>

                    <td class="data-cell">
                        <div class="text-truncate" :title="item.tenSanPham || 'Không có tên'">
                            {{ item.tenSanPham || 'Không có tên' }}
                        </div>
                    </td>

                    <td class="data-cell">
                        <div class="text-truncate" :title="item.tenThuongHieu || '--'">{{ item.tenThuongHieu || '--' }}</div>
                    </td>

                    <td class="data-cell">
                        <div class="text-truncate" :title="item.tenDanhMuc || '--'">{{ item.tenDanhMuc || '--' }}</div>
                    </td>

                    <td class="data-cell">
                        <span class="font-weight-black text-primary">{{ formatNumber(item.tongSoLuongTon || 0) }}</span>
                    </td>

                    <td class="data-cell text-center price-value px-2">
                        <div class="font-weight-black text-primary text-truncate" :title="getPriceRange(item)">
                            {{ getPriceRange(item) }}
                        </div>
                    </td>

                    <td class="data-cell">
                        <v-chip :color="getStatusColor(item.trangThai)" variant="flat" size="small"
                            class="font-weight-black text-white status-chip">
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>

                    <td class="data-cell action-cell">
                        <div class="d-flex align-center justify-center action-controls">
                            <v-btn variant="text" class="action-icon-btn"
                                @click="router.push({ name: 'BienTheSanPham', query: { productId: item.id } })">
                                <EyeIcon size="15" />
                                <v-tooltip activator="parent" location="top" text="Xem biến thể" />
                            </v-btn>
                            <v-btn variant="text" class="action-icon-btn"
                                @click="router.push({ name: 'SanPhamForm', params: { id: item.id } })">
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top" text="Chỉnh sửa" />
                            </v-btn>
                            <div class="switch-wrapper">
                                <v-switch :model-value="isActiveStatus(item.trangThai)" color="primary" hide-details
                                    density="compact" class="tight-switch action-switch"
                                    @click.prevent.stop="confirmToggleStatus(item)" />
                                <v-tooltip activator="parent" location="top" text="Chuyển đổi trạng thái" />
                            </div>
                        </div>
                    </td>
                </tr>
            </template>

            <template #pagination>
                <AdminPagination v-model="pagination.page" :page-size="pagination.size"
                    @update:pageSize="pagination.size = $event" @update:page-size="pagination.size = $event"
                    :total-pages="totalPages" :total-elements="totalElements" :current-size="paginatedProducts.length" />
            </template>
        </AdminTable>

        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="handleConfirm(true)"
            @cancel="handleConfirm(false)" />

        <QrScanner v-model:show="showQrScanner" @scan="handleQrScan" />
    </v-container>
</template>

<style scoped>
.price-slider-wrap {
    padding-right: 10px;
}
</style>
