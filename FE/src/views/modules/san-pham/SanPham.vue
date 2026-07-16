<script setup>
/**
 * Module: Quản lý sản phẩm (Admin)
 * View: SanPham
 * Chức năng: Màn hình danh sách sản phẩm. Cho phép lọc, tìm kiếm, xem khoảng giá, 
 *            đổi trạng thái (đơn lẻ hoặc hàng loạt), xuất/nhập Excel, và quét mã QR.
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { isActiveStatus, getStatusLabel, getStatusColor } from '@/utils/statusUtils';
import { SYSTEM_STATUS, STATUS_OPTIONS } from '@/constants/statusConstants';
import { formatCurrency } from '@/utils/formatters';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';
import { AdminFilter, AdminTable, AdminPagination, AdminConfirm, AdminBreadcrumbs } from '@/components/common';
import QrScanner from '@/views/modules/bien-the-san-pham/components/QrScanner.vue';
import FormattedNumberField from '@/views/modules/san-pham/components/FormattedNumberField.vue';
import { downloadFile } from '@/utils/fileUtils';
import { ADMIN_ICONS } from '@/constants/adminIcons';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useRefreshHandler } from '@/composables/useRefreshHandler';
import { useServerPagination } from '@/composables/useServerPagination';

const MIN_PRICE = 0;
const DEFAULT_MAX_PRICE = 6500000;
const PRICE_STEP = 500000;

const { addNotification } = useNotifications();
const router = useRouter();
const { confirmDialog, setConfirm, handleConfirm } = useConfirmDialog();
const { isRefreshing, handleRefresh: refreshData } = useRefreshHandler();

// Phân trang + tải dữ liệu server-side (composable dùng chung)
const {
    items: products,
    loading,
    pagination,
    totalElements,
    totalPages,
    load: loadProducts,
    reload: reloadProducts
} = useServerPagination(
    (pageable) => dichVuSanPham.layDanhSachSanPham({ ...pageable, ...buildProductFilterParams() }),
    {
        pageSize: 5,
        onLoaded: () => syncProductSelection(),
        onError: () => {
            clearProductSelection();
            addNotification({ title: 'Lỗi', subtitle: 'Không thể tải danh sách sản phẩm', color: 'error' });
        }
    }
);

const filterOptions = ref({
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

const filters = reactive({
    search: '',
    khoangGia: [MIN_PRICE, DEFAULT_MAX_PRICE],
    trangThai: null,
    thuongHieu: null,
    gioiTinh: null,
    chatLieu: null
});

let priceSearchTimer = null;

// Chuyển đổi một giá trị sang số hợp lệ, nếu lỗi trả về giá trị mặc định
const toNumber = (value, fallback = 0) => {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
};

// Danh sách sản phẩm hiển thị là dữ liệu của đúng trang hiện tại do BE trả về.
const filteredProductIds = computed(() => products.value.map((item) => item.id));
const selectedProducts = computed(() => products.value.filter((item) => selectedProductIds.value.includes(item.id)));
const allProductsSelected = computed(
    () => filteredProductIds.value.length > 0 && filteredProductIds.value.every((id) => selectedProductIds.value.includes(id))
);
const someProductsSelected = computed(
    () => filteredProductIds.value.some((id) => selectedProductIds.value.includes(id)) && !allProductsSelected.value
);
const productExportButtonText = computed(() =>
    selectedProductIds.value.length ? `Xuất Excel (${selectedProductIds.value.length})` : 'Xuất Excel'
);
const hasSelectedActiveProducts = computed(() => selectedProducts.value.some((item) => isActiveStatus(item.trangThai)));
const hasSelectedInactiveProducts = computed(() => selectedProducts.value.some((item) => !isActiveStatus(item.trangThai)));
const canBulkActivateProducts = computed(() => selectedProducts.value.length > 0 && hasSelectedInactiveProducts.value);
const canBulkDeactivateProducts = computed(() => selectedProducts.value.length > 0 && hasSelectedActiveProducts.value);

// Bỏ chọn tất cả các sản phẩm
const clearProductSelection = () => {
    selectedProductIds.value = [];
};

// Đồng bộ lại danh sách chọn sau khi lọc/đổi trang, loại bỏ những sản phẩm không còn hiển thị
const syncProductSelection = () => {
    // Để giữ lựa chọn khi sang trang, không xoá các ID khỏi selectedProductIds
    // Việc xoá (nếu cần khi filter thay đổi) đã được handle ở clearProductSelection() trong handleSearch.
};

// Xóa tất cả các bộ lọc, đưa về trạng thái mặc định
const resetProductFiltersState = () => {
    filters.search = '';
    filters.khoangGia = [MIN_PRICE, productPriceBounds.value.max];
    filters.trangThai = null;
    filters.thuongHieu = null;
    filters.gioiTinh = null;
    filters.chatLieu = null;
    pagination.page = 1;
    priceFilterDirty.value = false;
    clearProductSelection();
};

// Build phần params LỌC gửi lên API (page/size do composable useServerPagination tự thêm)
const buildProductFilterParams = () => ({
    sortBy: 'ngayTao',
    sortDirection: 'desc',
    keyword: filters.search?.trim() || undefined,
    thuongHieuId: filters.thuongHieu || undefined,
    trangThai: filters.trangThai || undefined,
    gioiTinhKhachHang: filters.gioiTinh || undefined,
    chatLieuId: filters.chatLieu || undefined,
    minGia: priceFilterDirty.value ? filters.khoangGia[0] : undefined,
    maxGia: priceFilterDirty.value ? filters.khoangGia[1] : undefined
});

// Gọi API lấy mức giá lớn nhất có trong database để làm max cho thanh trượt
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

// Gọi API lấy các tùy chọn cho các dropdown bộ lọc (danh mục, thương hiệu...)
const loadFilterOptions = async () => {
    try {
        const options = await dichVuSanPham.layOptionsForm();
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

// Xử lý sự kiện submit tìm kiếm (khi nhấn Enter/Click nút)
const handleSearch = async () => {
    clearProductSelection();
    await reloadProducts();
};

// Xử lý sự kiện làm mới dữ liệu
const handleRefresh = async () => {
    await refreshData(async () => {
        resetProductFiltersState();
        await loadProducts();
    });
};

// Debounce việc lọc giá khi kéo thanh trượt (gọi lại API với minGia/maxGia)
const schedulePriceSearch = () => {
    priceFilterDirty.value = true;
    if (priceSearchTimer) {
        window.clearTimeout(priceSearchTimer);
    }

    priceSearchTimer = window.setTimeout(() => {
        clearProductSelection();
        reloadProducts();
    }, 300);
};

// <- chỗ này viêt commet để biết mik làm gì đoạn này >
// Đảm bảo khoảng giá hợp lệ, min <= max, không vượt qua khoảng cho phép
const sanitizePriceRange = (range, maxPrice = productPriceBounds.value.max) => {
    const safeMaxPrice = Math.max(MIN_PRICE, toNumber(maxPrice, DEFAULT_MAX_PRICE));
    const rawMin = Math.max(MIN_PRICE, toNumber(range?.[0], MIN_PRICE));
    const rawMax = Math.max(MIN_PRICE, toNumber(range?.[1], safeMaxPrice));
    const nextMin = Math.min(rawMin, safeMaxPrice);
    const nextMax = Math.min(Math.max(rawMax, nextMin), safeMaxPrice);
    return [nextMin, nextMax];
};

// Cập nhật cận trên/dưới của khoảng giá (từ input textbox nếu có)
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

// Xử lý sự kiện khi thanh trượt thay đổi
const handleSliderPriceChange = (value) => {
    filters.khoangGia = sanitizePriceRange(value);
    schedulePriceSearch();
};

// Chống XSS cho các chuỗi dữ liệu đưa vào Excel
const escapeCell = (value) =>
    String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');

// Khởi tạo file Excel dạng HTML table và trigger tải về
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

// Format chuỗi khoảng giá để hiển thị trên bảng
const getPriceRange = (item) => {
    if (item?.giaBanThapNhat == null && item?.giaBanCaoNhat == null) return formatCurrency(0);
    if (item?.giaBanThapNhat === item?.giaBanCaoNhat) {
        if (item.giaBanThapNhat === 0) return formatCurrency(0);
        return `${formatCurrency(0)} - ${formatCurrency(item.giaBanCaoNhat)}`;
    }
    return `${formatCurrency(item.giaBanThapNhat)} - ${formatCurrency(item.giaBanCaoNhat)}`;
};

// Xuất danh sách sản phẩm (chỉ những sản phẩm đang chọn)
const handleExportProducts = () => {
    const targetProducts = selectedProducts.value;
    if (!targetProducts.length) {
        addNotification({
            title: 'Thông báo',
            subtitle: 'Vui lòng chọn ít nhất 1 sản phẩm để xuất Excel',
            color: 'warning'
        });
        return;
    }

    exportHtmlTableToExcel({
        headers: ['STT', 'Mã sản phẩm', 'Tên sản phẩm', 'Thương hiệu', 'Tổng số lượng', 'Khoảng giá', 'Trạng thái'],
        rows: targetProducts.map((item, index) => [
            index + 1,
            item.maSanPham || '--',
            item.tenSanPham || '--',
            item.tenThuongHieu || '--',
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

// Tải template file excel nhập liệu sản phẩm mẫu
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

// Gọi API upload file excel đã nhập để thêm/sửa nhanh sản phẩm
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

// Thay đổi trạng thái 1 sản phẩm
const updateSingleProductStatus = async (product, nextStatus) => {
    await dichVuSanPham.thayDoiTrangThai(product.id, nextStatus);
};

// Xác nhận với user trước khi thay đổi trạng thái sản phẩm
const confirmToggleStatus = (product) => {
    setConfirm({
        title: 'Xác nhận trạng thái',
        message: `Bạn có chắc chắn muốn thay đổi trạng thái sản phẩm [${product.tenSanPham}]?`,
        color: 'warning',
        action: async () => {
            try {
                const nextStatus = isActiveStatus(product.trangThai) ? SYSTEM_STATUS.INACTIVE : SYSTEM_STATUS.ACTIVE;
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

// Thay đổi trạng thái hàng loạt nhiều sản phẩm đang chọn
const handleBulkProductStatus = (nextStatus) => {
    if (!selectedProducts.value.length) {
        addNotification({
            title: 'Thông báo',
            subtitle: 'Bạn chưa chọn sản phẩm nào',
            color: 'warning'
        });
        return;
    }

    const canApplyStatus = nextStatus === 'DANG_HOAT_DONG' ? canBulkActivateProducts.value : canBulkDeactivateProducts.value;
    if (!canApplyStatus) {
        addNotification({
            title: 'Thông báo',
            subtitle: nextStatus === 'DANG_HOAT_DONG' ? 'Các sản phẩm đã ở trạng thái bật' : 'Các sản phẩm đã ở trạng thái tắt',
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

// Xử lý bật/tắt chọn 1 sản phẩm đơn lẻ
const toggleProductSelection = (productId, checked) => {
    if (checked) {
        if (!selectedProductIds.value.includes(productId)) {
            selectedProductIds.value = [...selectedProductIds.value, productId];
        }
        return;
    }

    selectedProductIds.value = selectedProductIds.value.filter((id) => id !== productId);
};

// Chọn/Bỏ chọn tất cả các sản phẩm đang hiển thị trên tất cả phân trang
const toggleSelectAllProducts = async (checked) => {
    if (checked) {
        try {
            // Lấy tất cả danh sách sản phẩm theo bộ lọc hiện tại thay vì chỉ lấy ID trên trang hiện tại
            const response = await dichVuSanPham.layDanhSachSanPham({
                ...buildProductFilterParams(),
                page: 1,
                size: 10000 // Tối đa để lấy đủ toàn bộ ID
            });
            const allIds = response?.content?.map(item => item.id) || [];
            selectedProductIds.value = allIds;
        } catch (e) {
            console.error('Lỗi khi lấy tất cả sản phẩm', e);
            selectedProductIds.value = [...filteredProductIds.value];
        }
        return;
    }

    selectedProductIds.value = [];
};

// Format số thông thường sang định dạng có dấu phẩy
const formatNumber = (value) => {
    if (value === null || value === undefined) return '0';
    return new Intl.NumberFormat('vi-VN').format(Number(value));
};

// Chuyển hướng sang màn quản lý biến thể với mã QR vừa quét được
const handleQrScan = async (decodedText) => {
    if (!decodedText) return;
    addNotification({ title: 'Đang chuyển hướng', subtitle: `Đã quét được mã: ${decodedText}`, color: 'info' });
    router.push({ name: 'BienTheSanPham', query: { keyword: decodedText } });
};

// Lắng nghe WebSocket khi tồn kho thay đổi từ server
const handleRealtimeStockUpdate = () => {
    if (products.value && products.value.length > 0) {
        loadProducts();
    }
};

onMounted(async () => {
    window.addEventListener('product-stock-update', handleRealtimeStockUpdate);
    await Promise.all([loadMaxPrice(), loadProducts(), loadFilterOptions()]);
});

onBeforeUnmount(() => {
    window.removeEventListener('product-stock-update', handleRealtimeStockUpdate);
    if (priceSearchTimer) {
        window.clearTimeout(priceSearchTimer);
    }
});
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page"
        style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important">
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý sản phẩm', disabled: false, href: '#' },
            { title: 'Danh sách sản phẩm', disabled: true }
        ]" />

        <div class="mb-2"></div>

        <div class="filter-shell">
            <AdminFilter @refresh="handleRefresh" :loading="isRefreshing">
                <v-col cols="12" md="3">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field v-model="filters.search" placeholder="Mã hoặc tên sản phẩm..."
                        prepend-inner-icon="mdi-magnify" variant="outlined" density="compact" hide-details clearable
                        class="compact-input" @update:model-value="handleSearch" />
                </v-col>


                <v-col cols="12" md="2">
                    <div class="filter-field-label">Thương hiệu</div>
                    <v-select v-model="filters.thuongHieu" :items="[
                        { title: 'Tất cả thương hiệu', value: null },
                        ...filterOptions.thuongHieus.map((thuongHieu) => ({ title: thuongHieu.ten, value: thuongHieu.id }))
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        :menu-props="{ contentClass: 'product-select-menu' }"
                        @update:model-value="handleSearch" />
                </v-col>

                <v-col cols="12" md="2">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select v-model="filters.trangThai" :items="STATUS_OPTIONS" variant="outlined" density="compact"
                        hide-details class="compact-input" :menu-props="{ contentClass: 'product-select-menu' }"
                        @update:model-value="handleSearch" />
                </v-col>
                <template #after>
                    <v-col cols="12" class="mt-4 pa-0">
                        <div class="d-flex align-center justify-space-between mb-2">
                            <div class="d-flex align-center gap-2">
                                <v-icon size="15" color="#3b82f6">mdi-cash-multiple</v-icon>
                                <span class="filter-range-label">Khoảng giá</span>
                            </div>
                            <span class="filter-range-value">
                                {{ formatCurrency(filters.khoangGia[0]) }} – {{ formatCurrency(filters.khoangGia[1]) }}
                            </span>
                        </div>
                        <v-range-slider :key="`${productPriceBounds.min}-${productPriceBounds.max}`"
                            v-model="filters.khoangGia" :min="productPriceBounds.min" :max="productPriceBounds.max"
                            :step="PRICE_STEP" hide-details color="primary" track-color="#e2e8f0" track-size="2"
                            thumb-size="14" class="blue-range-slider" @update:model-value="handleSliderPriceChange" />
                    </v-col>
                </template>
            </AdminFilter>
        </div>

        <AdminTable title="Danh sách sản phẩm" addButtonText="Tạo mới" class="balanced-table" :headers="[
            { text: 'STT', width: '60px' },
            { text: 'Mã sản phẩm', width: '110px' },
            { text: 'Tên sản phẩm', width: '120px' },
            { text: 'Thương hiệu', width: '110px' },
            { text: 'Tổng số lượng', width: '120px' },
            { text: 'Khoảng giá', width: '150px' },
            { text: 'Trạng thái', width: '110px' },
            { text: 'Hành động', width: '140px' }
        ]" :items="products" :loading="loading" :showExportButton="true"
            :exportButtonText="productExportButtonText" selectable @add="router.push({ name: 'SanPhamForm' })"
            @export="handleExportProducts">

            <template #header-select>
                <v-checkbox-btn :model-value="allProductsSelected" :indeterminate="someProductsSelected" color="primary"
                    hide-details density="compact" style="margin: auto; display: inline-flex; width: auto;"
                    @update:model-value="toggleSelectAllProducts" />
            </template>

            <template #top>
                <div class="px-6 py-3 bg-slate-50 border-b d-flex align-center justify-space-between flex-wrap gap-3">
                    <div class="d-flex align-center flex-wrap gap-2">
                        <span class="text-caption font-weight-medium text-slate-500">
                            Đã chọn {{ selectedProductIds.length }} sản phẩm
                        </span>
                    </div>
                </div>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell px-0" style="width: 50px; text-align: center;">
                        <v-checkbox-btn :model-value="selectedProductIds.includes(item.id)" color="primary" hide-details
                            density="compact" style="margin: auto; display: inline-flex; width: auto;"
                            @update:model-value="toggleProductSelection(item.id, $event)" />
                    </td>

                    <td class="data-cell">
                        {{ (pagination.page - 1) * pagination.size + index + 1 }}
                    </td>

                    <td class="data-cell">
                        <div class="text-truncate" :title="item.maSanPham || '--'">{{ item.maSanPham || '--' }}</div>
                    </td>

                    <td class="data-cell text-left px-4">
                        <div class="text-truncate" :title="item.tenSanPham || 'Không có tên'">
                            {{ item.tenSanPham || 'Không có tên' }}
                        </div>
                    </td>

                    <td class="data-cell text-center">
                        <div class="text-truncate" :title="item.tenThuongHieu || '--'">{{ item.tenThuongHieu || '--' }}
                        </div>
                    </td>


                    <td class="data-cell text-center">
                        <span class="text-primary">{{ formatNumber(item.tongSoLuongTon || 0) }}</span>
                    </td>

                    <td class="data-cell text-center">
                        <div class="text-primary text-truncate" :title="getPriceRange(item)">
                            {{ getPriceRange(item) }}
                        </div>
                    </td>

                    <td class="data-cell">
                        <v-chip variant="flat"
                            :class="['status-chip', isActiveStatus(item.trangThai) ? 'status-chip-active' : 'status-chip-inactive']">
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>

                    <td class="data-cell action-cell">
                        <div class="d-flex align-center justify-center action-controls">
                            <v-btn variant="text" class="action-icon-btn"
                                @click="router.push({ name: 'BienTheSanPham', query: { productId: item.id } })">
                                <component :is="ADMIN_ICONS.ACTION.VIEW" size="15" />
                                <v-tooltip activator="parent" location="top" text="Xem biến thể" />
                            </v-btn>
                            <v-btn variant="text" class="action-icon-btn"
                                @click="router.push({ name: 'SanPhamForm', params: { id: item.id } })">
                                <component :is="ADMIN_ICONS.ACTION.EDIT" size="15" />
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
                    @change="loadProducts" :total-pages="totalPages" :total-elements="totalElements"
                    :current-size="products.length" />
            </template>
        </AdminTable>

        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="handleConfirm(true)"
            @cancel="handleConfirm(false)" />

        <QrScanner v-model:show="showQrScanner" @scan="handleQrScan" />
    </v-container>
</template>

<style scoped>
:deep(.blue-range-slider .v-slider-track) {
    height: 2px !important;
}

:deep(.blue-range-slider .v-slider-track__background),
:deep(.blue-range-slider .v-slider-track__fill) {
    height: 2px !important;
    border-radius: 1px !important;
}

/* Style input text, placeholders and selections */
:deep(.compact-input) .v-field__input,
:deep(.compact-input) input,
:deep(.compact-input) input::placeholder,
:deep(.compact-input) .v-select__selection-text {
    font-size: 13px !important;
}

/* Style select dropdown popover option list */
:global(.product-select-menu .v-list-item-title),
:global(.product-select-menu .v-list-item) {
    font-size: 13px !important;
}
</style>
