<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { downloadFile } from '@/utils/fileUtils';
import { resolveMediaUrl } from '@/utils/mediaUrl';
import { EditIcon } from 'vue-tabler-icons';

const { addNotification } = useNotifications();
const router = useRouter();

const loading = ref(false);
const importing = ref(false);
const isRefreshing = ref(false);
const products = ref([]);
const searchDebounce = ref(null);
const MIN_PRICE = 0;
const MAX_PRICE = 100000000;

// Pagination logic
const pagination = ref({
    page: 1,
    size: 10,
    totalElements: 0,
    totalPages: 1
});

const filters = ref({
    keyword: '',
    khoangGia: [MIN_PRICE, MAX_PRICE],
    trangThai: null,
    danhMuc: null,
    thuongHieu: null,
    gioiTinh: null,
    chatLieu: null
});

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

const toNumber = (value, fallback = 0) => {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
};

const isActiveStatus = (status) => {
    if (status === null || status === undefined) return false;
    if (typeof status === 'number') return status === 0;
    const normalized = String(status).toUpperCase();
    return normalized === 'DANG_HOAT_DONG' || normalized === 'ACTIVE' || normalized === 'HOAT_DONG' || normalized === '0';
};

const getDisplayStatus = (status) => {
    return isActiveStatus(status) ? 'Hoạt động' : 'Ngừng hoạt động';
};

const resolveCreatedAt = (item) => {
    return item?.ngayTao ?? item?.createdAt ?? item?.ngay_tao ?? null;
};

const getProductCode = (item) => item?.maSanPham ?? item?.ma_san_pham ?? '--';
const getProductName = (item) => item?.tenSanPham ?? item?.ten_san_pham ?? '--';
const getBrandName = (item) => item?.tenThuongHieu ?? item?.ten_thuong_hieu ?? '--';
const getCategoryName = (item) => item?.tenDanhMuc ?? item?.ten_danh_muc ?? '--';
const getQuantity = (item) => toNumber(item?.tongSoLuongTon ?? item?.soLuongTon ?? item?.soLuong ?? item?.tong_so_luong_ton, 0);
const getPrice = (item) => toNumber(item?.giaBanThapNhat ?? item?.giaBan ?? item?.gia_ban ?? item?.giaBanNiemYet ?? item?.gia, 0);
const getMaxPrice = (item) => toNumber(item?.giaBanCaoNhat ?? item?.gia_ban_cao_nhat ?? item?.giaBanThapNhat ?? item?.giaBan ?? item?.gia, 0);
const getVariantCount = (item) => toNumber(item?.tongBienThe ?? item?.tong_bien_the, 0);
const getProductImage = (item) => resolveMediaUrl(item?.hinhAnh, 'aerostride/products/main') || 'https://placehold.co/120x120/f8fafc/94a3b8?text=SP';

// Confirmation Logic
const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    color: 'primary',
    action: null,
    loading: false
});

const loadProducts = async () => {
    loading.value = true;
    try {
        const selectedPriceRange = Array.isArray(filters.value.khoangGia) ? filters.value.khoangGia : [MIN_PRICE, MAX_PRICE];
        const giaTu = selectedPriceRange[0] <= MIN_PRICE ? null : selectedPriceRange[0];
        const giaDen = selectedPriceRange[1] >= MAX_PRICE ? null : selectedPriceRange[1];

        const params = {
            page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
            size: pagination.value.size,
            keyword: filters.value.keyword || null,
            trangThai: filters.value.trangThai || null,
            danhMucId: filters.value.danhMuc || null,
            thuongHieuId: filters.value.thuongHieu || null,
            gioiTinhKhachHang: filters.value.gioiTinh || null,
            chatLieuId: filters.value.chatLieu || null,
            giaTu,
            giaDen
        };
        const response = await dichVuSanPham.layDanhSachSanPham(params);
        // Robust extraction
        const data = response?.data?.content || response?.content || response?.data || response;
        products.value = Array.isArray(data) ? data : [];

        pagination.value.totalElements = response?.data?.totalElements || response?.totalElements || products.value.length;
        pagination.value.totalPages = response?.data?.totalPages || response?.totalPages || 1;
    } catch (error) {
        console.error('Error loading products:', error);
    } finally {
        loading.value = false;
    }
};

const handleRefresh = async () => {
    isRefreshing.value = true;
    if (searchDebounce.value) {
        clearTimeout(searchDebounce.value);
        searchDebounce.value = null;
    }
    filters.value = {
        keyword: '',
        khoangGia: [MIN_PRICE, MAX_PRICE],
        trangThai: null,
        danhMuc: null,
        thuongHieu: null,
        gioiTinh: null,
        chatLieu: null
    };
    pagination.value.page = 1;
    await loadProducts();
    setTimeout(() => {
        isRefreshing.value = false;
    }, 800);
};

const loadFilterOptions = async () => {
    try {
        const options = await dichVuSanPham.layOptionsForm();
        if (options) {
            filterOptions.value.danhMucs = options.danhMucs || [];
            filterOptions.value.thuongHieus = options.thuongHieus || [];
            filterOptions.value.xuatXus = options.xuatXus || [];
            filterOptions.value.mucDichChays = options.mucDichChays || [];
            filterOptions.value.chatLieus = options.chatLieus || [];
            filterOptions.value.coGiays = options.coGiays || [];
            filterOptions.value.deGiays = options.deGiays || [];
        }
    } catch (error) {
        console.error('Lỗi tải options:', error);
    }
};

const handleExport = async () => {
    try {
        const blob = await dichVuSanPham.xuatExcelSanPham();
        downloadFile(blob, 'danh_sach_san_pham.xlsx');
    } catch (error) {
        console.error('Lỗi xuất Excel:', error);
    }
};

const handleDownloadTemplate = async () => {
    try {
        const blob = await dichVuSanPham.taiTemplateExcel();
        downloadFile(blob, 'template_nhap_san_pham.xlsx');
    } catch (error) {
        console.error('Lỗi tải template:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải template', color: 'error' });
    }
};

const handleImport = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    importing.value = true;
    try {
        await dichVuSanPham.nhapExcelSanPham(file);
        addNotification({ title: 'Thành công', subtitle: 'Đã nhập dữ liệu từ Excel', color: 'success' });
        loadProducts(); // Refresh list
    } catch (error) {
        console.error('Lỗi nhập Excel:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Lỗi khi nhập dữ liệu Excel', color: 'error' });
    } finally {
        importing.value = false;
        event.target.value = ''; // Reset input
    }
};

const requestDeleteProduct = (product) => {
    confirmDialog.value = {
        show: true,
        title: 'Xóa sản phẩm',
        message: `Bạn có chắc muốn xóa mềm sản phẩm [${product.tenSanPham}] không?`,
        color: 'error',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                await dichVuSanPham.xoaSanPham(product.id);
                addNotification({
                    title: 'Đã xóa sản phẩm',
                    subtitle: `Sản phẩm [${product.tenSanPham}] đã được xóa mềm`,
                    color: 'success'
                });
                confirmDialog.value.show = false;
                await loadProducts();
            } catch (error) {
                addNotification({
                    title: 'Lỗi xóa sản phẩm',
                    subtitle: 'Không thể xóa sản phẩm này',
                    color: 'error'
                });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

const formatPricePlain = (amount) => {
    const value = toNumber(amount, 0);
    return `${new Intl.NumberFormat('vi-VN').format(value)}đ`;
};

const formatDateTime = (value) => {
    if (!value) return '--';
    const date = typeof value === 'number' ? new Date(value) : new Date(String(value));
    if (Number.isNaN(date.getTime())) return '--';
    const d = String(date.getDate()).padStart(2, '0');
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const y = date.getFullYear();
    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    return `${d}/${m}/${y} ${hh}:${mm}`;
};

const scheduleSearch = () => {
    if (searchDebounce.value) {
        clearTimeout(searchDebounce.value);
    }
    pagination.value.page = 1;
    searchDebounce.value = setTimeout(() => {
        loadProducts();
    }, 350);
};

const getStatusChipClass = (status) => {
    return status === 'DANG_HOAT_DONG' ? 'status-chip-active' : 'status-chip-inactive';
};

onMounted(() => {
    loadProducts();
    loadFilterOptions();
});

onBeforeUnmount(() => {
    if (searchDebounce.value) {
        clearTimeout(searchDebounce.value);
    }
});
</script>

<template>
    <v-container fluid class="pa-4 gray-bg min-h-screen font-body">
        <!-- Header -->
        <div class="mb-2 d-flex align-center justify-space-between flex-wrap gap-3">
            <div>
                <h1 class="page-title text-h5 font-weight-bold text-slate-900 mb-0">Quản lí sản phẩm</h1>
                <div class="text-body-2 text-medium-emphasis mt-1">
                    Màn này quản lý thông tin sản phẩm chính, còn SKU và ảnh biến thể được tách sang màn riêng.
                </div>
            </div>
            <v-btn
                variant="outlined"
                color="primary"
                prepend-icon="mdi-shape-outline"
                class="text-none"
                @click="router.push({ name: 'SanPhamVariants' })"
            >
                Quản lý biến thể
            </v-btn>
        </div>

        <!-- 1. FILTER -->
        <div class="filter-top">
            <AdminFilter @refresh="handleRefresh" :loading="isRefreshing">
                <!-- Search -->
                <v-col cols="12" md="3">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field
                        v-model="filters.keyword"
                        placeholder="Mã hoặc tên sản phẩm..."
                        prepend-inner-icon="mdi-magnify"
                        variant="outlined"
                        density="compact"
                        hide-details
                        clearable
                        class="compact-input"
                        @update:model-value="scheduleSearch"
                    ></v-text-field>
                </v-col>

                <!-- Danh mục -->
                <v-col cols="12" md="2">
                    <div class="filter-field-label">Danh mục</div>
                    <v-select
                        v-model="filters.danhMuc"
                        :items="[
                            { title: 'Tất cả danh mục', value: null },
                            ...filterOptions.danhMucs.map(d => ({ title: d.ten, value: d.id }))
                        ]"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @update:model-value="loadProducts"
                    ></v-select>
                </v-col>

                <!-- Thương hiệu -->
                <v-col cols="12" md="2">
                    <div class="filter-field-label">Thương hiệu</div>
                    <v-select
                        v-model="filters.thuongHieu"
                        :items="[
                            { title: 'Tất cả thương hiệu', value: null },
                            ...filterOptions.thuongHieus.map(t => ({ title: t.ten, value: t.id }))
                        ]"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @update:model-value="loadProducts"
                    ></v-select>
                </v-col>

                <!-- Giới tính -->
                <v-col cols="12" md="2">
                    <div class="filter-field-label">Giới tính</div>
                    <v-select
                        v-model="filters.gioiTinh"
                        :items="[
                            { title: 'Tất cả', value: null },
                            { title: 'Nam', value: 'NAM' },
                            { title: 'Nữ', value: 'NU' },
                            { title: 'Trẻ em', value: 'TRE_EM' },
                            { title: 'Unisex', value: 'UNISEX' }
                        ]"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @update:model-value="loadProducts"
                    ></v-select>
                </v-col>

                <!-- Trạng thái -->
                <v-col cols="12" md="2">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select
                        v-model="filters.trangThai"
                        :items="[
                            { title: 'Tất cả', value: null },
                            { title: 'Hoạt động', value: 'DANG_HOAT_DONG' },
                            { title: 'Ngừng hoạt động', value: 'KHONG_HOAT_DONG' }
                        ]"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @update:model-value="loadProducts"
                    ></v-select>
                </v-col>
            </AdminFilter>
        </div>

        <!-- 2. TABLE -->
        <AdminTable
            title="Danh sách sản phẩm"
            addButtonText="Thêm sản phẩm"
            :headers="[
                { text: 'Sản phẩm', align: 'left', width: '34%' },
                { text: 'Thuộc tính', align: 'left', width: '22%' },
                { text: 'Tồn kho và giá', align: 'left', width: '20%' },
                { text: 'Trạng thái', align: 'center', width: '12%' },
                { text: 'Hành động', align: 'center', width: '12%' }
            ]"
            :items="products"
            :loading="loading"
            :show-export-button="true"
            :show-import-button="true"
            :show-template-button="true"
            @add="router.push({ name: 'SanPhamForm' })"
            @export="handleExport"
            @import="$refs.fileInput.click()"
            @download-template="handleDownloadTemplate"
        >
            <template #row="{ item }">
                <tr class="data-row">
                    <td class="data-cell text-left">
                        <div class="product-summary-cell">
                            <v-avatar size="52" rounded="lg" class="product-avatar">
                                <v-img :src="getProductImage(item)" cover />
                            </v-avatar>
                            <div class="min-w-0">
                                <div class="text-caption product-code">{{ getProductCode(item) }}</div>
                                <div class="font-weight-bold text-dark product-name">{{ getProductName(item) }}</div>
                                <div class="text-caption text-medium-emphasis mt-1">
                                    {{ getCategoryName(item) }} • {{ getBrandName(item) }}
                                </div>
                            </div>
                        </div>
                    </td>

                    <td class="data-cell text-left">
                        <div class="font-weight-medium text-dark">{{ item.gioiTinhKhachHang || '--' }}</div>
                        <div class="text-caption text-medium-emphasis mt-1">
                            {{ item.tenXuatXu || '--' }} • {{ item.tenMucDichChay || '--' }}
                        </div>
                        <div class="text-caption text-medium-emphasis mt-1">
                            {{ formatDateTime(resolveCreatedAt(item)) }}
                        </div>
                    </td>

                    <td class="data-cell text-left">
                        <div class="font-weight-bold text-dark">{{ getVariantCount(item) }} biến thể</div>
                        <div class="text-caption text-medium-emphasis mt-1">{{ getQuantity(item) }} đôi</div>
                        <div class="price-value mt-1">
                            {{ formatPricePlain(getPrice(item)) }}
                            <span v-if="getMaxPrice(item) !== getPrice(item)"> - {{ formatPricePlain(getMaxPrice(item)) }}</span>
                        </div>
                    </td>

                    <td class="data-cell text-center">
                        <v-chip
                            :class="['font-weight-bold px-4 status-chip', getStatusChipClass(item.trangThai)]"
                            variant="flat"
                            size="small"
                        >
                            {{ getDisplayStatus(item.trangThai) }}
                        </v-chip>
                    </td>

                    <td class="data-cell action-cell" style="text-align: center">
                        <div class="d-flex align-center justify-center action-controls">
                            <v-btn
                                icon
                                variant="text"
                                size="28"
                                color="#2aa6a1"
                                class="rounded-lg action-icon-btn"
                                @click="router.push({ name: 'SanPhamVariants', query: { productId: item.id } })"
                            >
                                <v-icon size="18">mdi-shape-outline</v-icon>
                                <v-tooltip activator="parent" location="top">Quản lý biến thể</v-tooltip>
                            </v-btn>
                            <v-btn
                                icon
                                variant="text"
                                size="28"
                                color="#5f6f82"
                                class="rounded-lg action-icon-btn"
                                @click="router.push({ name: 'SanPhamForm', params: { id: item.id } })"
                            >
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <v-btn
                                icon
                                variant="text"
                                size="28"
                                color="#dc2626"
                                class="rounded-lg action-icon-btn"
                                @click="requestDeleteProduct(item)"
                            >
                                <v-icon size="18">mdi-delete-outline</v-icon>
                                <v-tooltip activator="parent" location="top">Xóa mềm</v-tooltip>
                            </v-btn>
                        </div>
                    </td>
                </tr>
            </template>

            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    :page-size="pagination.size"
                    @update:pageSize="pagination.size = $event"
                    @update:page-size="pagination.size = $event"
                    :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements"
                    :current-size="products.length"
                    @change="loadProducts"
                />
            </template>
        </AdminTable>

        <!-- Hidden Input for Excel Import -->
        <input type="file" ref="fileInput" class="d-none" accept=".xlsx, .xls" @change="handleImport" />

        <!-- SHARED CONFIRM DIALOG -->
        <AdminConfirm
            v-model:show="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :color="confirmDialog.color"
            :loading="confirmDialog.loading"
            @confirm="confirmDialog.action"
        />
    </v-container>
</template>

<style scoped>
.gray-bg {
    background-color: #f5f7fb;
}
.text-dark {
    color: #0f172a !important;
}
.font-body {
    font-family: 'Inter', sans-serif;
}
.page-title {
    line-height: 1.1;
}
.filter-field-label {
    font-size: 13px;
    font-weight: 700;
    color: #334155;
    margin-bottom: 6px;
}

:deep(.filter-top .filter-grid) {
    align-items: flex-start !important;
}

:deep(.filter-top .filter-cell) {
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
}

:deep(.filter-top .filter-grid > .v-col) {
    padding-top: 4px;
    padding-bottom: 4px;
}

:deep(.filter-top .filter-reset-col) {
    align-self: flex-end !important;
    justify-content: flex-end !important;
    display: flex !important;
    padding-bottom: 2px !important;
}

:deep(.filter-top .v-field input),
:deep(.filter-top .v-field__input),
:deep(.filter-top .v-select__selection),
:deep(.filter-top .v-select__selection-text),
:deep(.filter-top .v-field__input::placeholder) {
    font-size: 13px !important;
}

:deep(.filter-top .price-range-slider) {
    margin-top: 0;
    padding-top: 2px;
}

.price-slider-wrap {
    padding-right: 10px;
}

.price-endpoints {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 2px;
    font-size: 11px;
    font-weight: 600;
    color: #64748b;
}

.price-live-values {
    margin-top: 2px;
    font-size: 12px;
    font-weight: 700;
    color: #1e3a8a;
}

:deep(.filter-top .primary-range-slider .v-slider-track__background) {
    height: 3px !important;
    background: #dbeafe !important;
}

:deep(.filter-top .primary-range-slider .v-slider-track__fill) {
    height: 3px !important;
    background: #1e3a8a !important;
}

:deep(.filter-top .primary-range-slider .v-slider-thumb__surface) {
    width: 12px !important;
    height: 12px !important;
    background: #1e3a8a !important;
    border: 2px solid #ffffff !important;
}

:deep(.filter-top .gender-filter-cell) {
    padding-left: 0 !important;
}

.gender-inline-row {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 0;
}

.gender-inline-title {
    margin-bottom: 6px;
    white-space: nowrap;
}

.gender-native-group {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 12px;
    margin-top: 2px;
    margin-left: 0;
    padding-left: 0;
}

.gender-option {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    margin: 0;
    padding: 0;
    font-size: 13px;
    font-weight: 600;
    color: #334155;
}

.gender-option input[type='radio'] {
    margin: 0;
    accent-color: #1e3a8a;
}

.action-controls {
    gap: 6px;
}

.product-summary-cell {
    display: flex;
    align-items: center;
    gap: 12px;
}

.product-avatar {
    border: 1px solid #dbe4ef;
    background: #ffffff;
}

.product-code {
    color: #1e3a8a !important;
    font-weight: 700 !important;
}

.product-name {
    line-height: 1.35;
}

:deep(.action-cell .action-icon-btn) {
    background: transparent !important;
    border: none !important;
    outline: none !important;
    box-shadow: none !important;
    min-width: 28px !important;
    width: 28px !important;
    height: 28px !important;
    padding: 0 !important;
}

:deep(.action-cell .action-icon-btn:hover),
:deep(.action-cell .action-icon-btn:focus),
:deep(.action-cell .action-icon-btn:focus-visible),
:deep(.action-cell .action-icon-btn:active) {
    background: transparent !important;
    border: none !important;
    outline: none !important;
    box-shadow: none !important;
}

:deep(.action-cell .action-icon-btn .v-btn__overlay),
:deep(.action-cell .action-icon-btn .v-btn__underlay),
:deep(.action-cell .action-icon-btn .v-ripple__container) {
    display: none !important;
}

:deep(.native-admin-table .header-cell) {
    font-size: 13px !important;
    font-weight: 700 !important;
    text-align: left !important;
    padding-left: 6px !important;
}

:deep(.native-admin-table .data-cell) {
    font-size: 13px !important;
    font-weight: 600 !important;
    text-align: left !important;
    padding-left: 6px !important;
}

:deep(.native-admin-table .data-cell .text-subtitle-2),
:deep(.native-admin-table .data-cell .text-caption),
:deep(.native-admin-table .data-cell .text-body-2),
:deep(.native-admin-table .data-cell .text-subtitle-1) {
    font-size: 13px !important;
    font-weight: 600 !important;
    line-height: 1.35 !important;
}

:deep(.native-admin-table .total-price-text) {
    color: #1e3a8a !important;
}

:deep(.native-admin-table .price-value) {
    color: #1e3a8a !important;
    font-weight: 700 !important;
}

:deep(.status-chip) {
    border-radius: 999px !important;
    border: 1px solid transparent !important;
    box-shadow: none !important;
    font-size: 13px !important;
    min-height: 28px !important;
}

:deep(.status-chip.status-chip-active) {
    background-color: #edf5ff !important;
    color: #2f4f87 !important;
    border-color: #d3e4ff !important;
}

:deep(.status-chip.status-chip-inactive) {
    background-color: #feeceb !important;
    color: #dc2626 !important;
    border-color: #f9c7c3 !important;
}
</style>
