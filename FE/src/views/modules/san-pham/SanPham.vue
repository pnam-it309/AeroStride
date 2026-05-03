<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { isActiveStatus, getStatusLabel, getStatusColor } from '@/utils/statusUtils';
import { formatCurrency } from '@/utils/formatters';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';

// REUSABLE COMPONENTS
import { AdminFilter, AdminTable, AdminPagination, AdminConfirm, AdminBreadcrumbs } from '@/components/common';
import QrScanner from '@/views/modules/san-pham/components/QrScanner.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EditIcon, EyeIcon, PackageIcon, QrcodeIcon } from 'vue-tabler-icons';

import { useAdminTable } from '@/composables/useAdminTable';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useRefreshHandler } from '@/composables/useRefreshHandler';

const { addNotification } = useNotifications();
const router = useRouter();

// Use composables
const { confirmDialog, setConfirm, clearConfirm, handleConfirm } = useConfirmDialog();
const { isRefreshing, handleRefresh: refreshData } = useRefreshHandler();

const MIN_PRICE = 0;
const MAX_PRICE = 100000000;
const PRICE_STEP = 500000;

const {
    items: products,
    loading,
    pagination,
    filters,
    loadData: loadProducts,
    handleFilter: handleSearch,
    handleReset
} = useAdminTable(dichVuSanPham.layDanhSachSanPham, {
    search: '',
    khoangGia: [MIN_PRICE, MAX_PRICE],
    trangThai: null,
    danhMuc: null,
    thuongHieu: null,
    chatLieu: null
});

const importing = ref(false);
const scannerEnabled = ref(false);
const showQrScanner = ref(false);

const filterOptions = ref({
    danhMucs: [],
    thuongHieus: [],
    xuatXus: [],
    mucDichChays: [],
    chatLieus: [],
    coGiays: [],
    deGiays: []
});

const handleRefresh = async () => {
    await refreshData(() => handleReset());
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
        console.error('Error loading filter options:', error);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể tải các tùy chọn lọc',
            color: 'error'
        });
    }
};

const handleExport = async () => {
    try {
        const blob = await dichVuSanPham.xuatExcelSanPham();
        downloadFile(blob, 'danh_sach_san_pham.xlsx');
    } catch (error) {
        console.error('Error exporting Excel:', error);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể xuất Excel',
            color: 'error'
        });
    }
};

const handleDownloadTemplate = async () => {
    try {
        const blob = await dichVuSanPham.taiTemplateExcel();
        downloadFile(blob, 'template_nhap_san_pham.xlsx');
    } catch (error) {
        console.error('Error downloading template:', error);
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
        loadProducts();
    } catch (error) {
        console.error('Lỗi nhập Excel:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Lỗi khi nhập dữ liệu Excel', color: 'error' });
    } finally {
        importing.value = false;
        event.target.value = '';
    }
};

const confirmToggleStatus = (product) => {
    setConfirm({
        title: 'Xác nhận trạng thái',
        message: `Bạn có chắc chắn muốn thay đổi trạng thái sản phẩm [${product.tenSanPham}]?`,
        color: 'warning',
        action: async () => {
            try {
                const newS = isActiveStatus(product.trangThai) ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
                await dichVuSanPham.thayDoiTrangThai(product.id, newS);
                await loadProducts();
                addNotification({
                    title: 'Thành công',
                    subtitle: `Đã cập nhật trạng thái sản phẩm [${product.tenSanPham}]`,
                    color: 'success'
                });
            } catch (e) {
                console.error('Status update error:', e);
                addNotification({
                    title: 'Lỗi',
                    subtitle: 'Không thể cập nhật trạng thái',
                    color: 'error'
                });
            }
        }
    });
};

const formatNumber = (value) => {
    if (value === null || value === undefined) return '0';
    return new Intl.NumberFormat('vi-VN').format(Number(value));
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

const formatRangePrice = (amount) => {
    return new Intl.NumberFormat('vi-VN').format(amount) + 'đ';
};

const handleQrScan = (decodedText) => {
    filters.value.search = decodedText;
    handleSearch();
};


const getCategoryColor = (name) => {
    if (!name) return 'grey';
    const n = name.toLowerCase();
    if (n.includes('chạy bộ')) return 'blue';
    if (n.includes('thể thao')) return 'orange';
    if (n.includes('sneaker')) return 'indigo';
    if (n.includes('thời trang')) return 'purple';
    if (n.includes('đá bóng')) return 'green';
    return 'blue-grey';
};

const getStatusChipClass = (status) => {
    return status === 'DANG_HOAT_DONG' ? 'status-chip-active' : 'status-chip-inactive';
};

// HELPER FUNCTIONS FOR TABLE BINDING
const getProductCode = (item) => item.maSanPham || '--';
const getProductName = (item) => item.tenSanPham || 'Không có tên';
const getBrandName = (item) => item.tenThuongHieu || '--';
const getCategoryName = (item) => item.tenDanhMuc || '--';
const getQuantity = (item) => item.tongSoLuongTon || 0;
const getPriceRange = (item) => {
    if (!item.giaBanThapNhat && !item.giaBanCaoNhat) return '--';
    if (item.giaBanThapNhat === item.giaBanCaoNhat) return formatCurrency(item.giaBanThapNhat);
    return `${formatCurrency(item.giaBanThapNhat)} - ${formatCurrency(item.giaBanCaoNhat)}`;
};

onMounted(() => {
    loadProducts();
    loadFilterOptions();
});
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body"
        style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important;">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý sản phẩm', disabled: false, href: '#' },
            { title: 'Danh sách sản phẩm', disabled: true }
        ]" />

        <div class="mb-2"></div>

        <!-- 1. FILTER -->
        <div class="filter-top">
            <AdminFilter @refresh="handleRefresh" :loading="isRefreshing">
                <!-- Search -->
                <v-col cols="12" md="3">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field v-model="filters.search" placeholder="Mã hoặc tên sản phẩm..."
                        prepend-inner-icon="mdi-magnify" variant="outlined" density="compact" hide-details clearable
                        class="compact-input" @update:model-value="handleSearch"></v-text-field>
                </v-col>

                <!-- Danh mục -->
                <v-col cols="12" md="3">
                    <div class="filter-field-label">Danh mục</div>
                    <v-select v-model="filters.danhMuc" :items="[
                        { title: 'Tất cả danh mục', value: null },
                        ...filterOptions.danhMucs.map(d => ({ title: d.ten, value: d.id }))
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch"></v-select>
                </v-col>

                <!-- Thương hiệu -->
                <v-col cols="12" md="3">
                    <div class="filter-field-label">Thương hiệu</div>
                    <v-select v-model="filters.thuongHieu" :items="[
                        { title: 'Tất cả thương hiệu', value: null },
                        ...filterOptions.thuongHieus.map(t => ({ title: t.ten, value: t.id }))
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch"></v-select>
                </v-col>


                <!-- Trạng thái -->
                <v-col cols="12" md="3">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select v-model="filters.trangThai" :items="[
                        { title: 'Tất cả', value: null },
                        { title: 'Hoạt động', value: 'DANG_HOAT_DONG' },
                        { title: 'Ngừng hoạt động', value: 'KHONG_HOAT_DONG' }
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch"></v-select>
                </v-col>
            </AdminFilter>
        </div>

        <!-- 2. TABLE -->
        <AdminTable title="Danh sách sản phẩm" addButtonText="Tạo mới" :headers="[
            { text: 'STT', align: 'center', width: '50px' },
            { text: 'Mã sản phẩm', align: 'center', width: '120px' },
            { text: 'Tên sản phẩm', align: 'left', width: '180px' },
            { text: 'Thương hiệu', align: 'left', width: '140px' },
            { text: 'Danh mục', align: 'left', width: '140px' },
            { text: 'Tổng số lượng', align: 'center', width: '120px' },
            { text: 'Khoảng giá', align: 'left', width: '100px' },
            { text: 'Trạng thái', align: 'center', width: '130px' },
            { text: 'Hành động', align: 'center', width: '130px' }
        ]" :items="products" :loading="loading" @add="router.push({ name: 'SanPhamForm' })" @export="handleExport"
            @import="$refs.fileInput.click()" @download-template="handleDownloadTemplate">
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center">
                        {{ (pagination.page - 1) * pagination.size + index + 1 }}
                    </td>

                    <td class="data-cell text-center">
                        {{ getProductCode(item) }}
                    </td>

                    <td class="data-cell text-left">
                        <div class="text-dark">
                            {{ getProductName(item) }}
                        </div>
                    </td>

                    <td class="data-cell text-left">
                        {{ getBrandName(item) }}
                    </td>

                    <td class="data-cell text-left">
                        {{ getCategoryName(item) }}
                    </td>

                    <td class="data-cell text-center">
                        {{ formatNumber(getQuantity(item)) }}
                    </td>

                    <td class="data-cell text-left price-value px-2">
                        <div class="text-primary">
                            {{ getPriceRange(item) }}
                        </div>
                    </td>

                    <td class="data-cell text-center status-cell">
                        <v-chip
                            :class="['status-chip', item.trangThai === 'DANG_HOAT_DONG' ? 'status-chip-active' : 'status-chip-inactive']"
                            variant="flat"
                            size="small"
                        >
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>


                    <td class="data-cell action-cell" style="text-align: center">
                        <div class="d-flex align-center justify-center action-controls">
                            <v-btn variant="text" class="action-icon-btn"
                                @click="router.push({ name: 'BienTheSanPham', query: { productId: item.id } })">
                                <EyeIcon size="15" />
                                <v-tooltip activator="parent" location="top" text="Xem biến thể"></v-tooltip>
                            </v-btn>
                            <v-btn variant="text" class="action-icon-btn"
                                @click="router.push({ name: 'SanPhamForm', params: { id: item.id } })">
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top" text="Chỉnh sửa"></v-tooltip>
                            </v-btn>
                            <div class="switch-wrapper">
                                <v-switch :model-value="isActiveStatus(item.trangThai)" color="primary" hide-details
                                    density="compact" class="tight-switch action-switch"
                                    @click.prevent.stop="confirmToggleStatus(item)" />
                                <v-tooltip activator="parent" location="top" text="Chuyển đổi trạng thái"></v-tooltip>
                            </div>
                        </div>
                    </td>
                </tr>
            </template>

            <template #pagination>
                <AdminPagination v-model="pagination.page" :page-size="pagination.size"
                    @update:pageSize="pagination.size = $event" @update:page-size="pagination.size = $event"
                    :total-pages="pagination.totalPages" :total-elements="pagination.totalElements"
                    :current-size="products.length" @change="loadProducts" />
            </template>
        </AdminTable>

        <!-- Hidden Input for Excel Import -->
        <input type="file" ref="fileInput" class="d-none" accept=".xlsx, .xls" @change="handleImport" />

        <!-- SHARED CONFIRM DIALOG -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="handleConfirm(true)"
            @cancel="handleConfirm(false)" />

        <!-- QR Scanner -->
        <QrScanner v-model:show="showQrScanner" @scan="handleQrScan" />
    </v-container>
</template>

<style scoped>
/* Scoped styles removed in favor of global _admin-common.scss */
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
    color: #000;
}

:deep(.filter-top .primary-range-slider .v-slider-track__background) {
    height: 3px !important;
    background: #dbeafe !important;
}

:deep(.filter-top .primary-range-slider .v-slider-track__fill) {
    height: 3px !important;
    background: #000 !important;
}

:deep(.filter-top .primary-range-slider .v-slider-thumb__surface) {
    width: 12px !important;
    height: 12px !important;
    background: #000 !important;
    border: 2px solid #ffffff !important;
}

:deep(.data-cell), :deep(.data-cell *) {
    font-size: 13.5px !important;
    font-family: 'Inter', sans-serif !important;
}
</style>

