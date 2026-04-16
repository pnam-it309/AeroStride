<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuDotGiamGia } from '@/services/admin/dichVuDotGiamGia';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EditIcon, EyeIcon } from 'vue-tabler-icons';

const router = useRouter();
const loading = ref(false);
const isRefreshing = ref(false);
const campaigns = ref([]);
const searchDebounce = ref(null);

const pagination = ref({ page: 1, size: 5, totalElements: 0, totalPages: 1 });
const filters = ref({ 
    keyword: '', 
    loaiGiamGia: null,
    trangThai: null,
    startDate: null,
    endDate: null
});

// Confirmation Logic
const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const loadCampaigns = async () => {
    loading.value = true;
    try {
        const params = {
            page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
            size: pagination.value.size,
            keyword: filters.value.keyword || null,
            loaiGiamGia: filters.value.loaiGiamGia || null,
            trangThai: filters.value.trangThai || null,
            startDate: filters.value.startDate || null,
            endDate: filters.value.endDate || null
        };
        const response = await dichVuDotGiamGia.layDotGiamGiaPhanTrang(params);
        // Robust extraction: handle both direct array, Page object, and ApiResponse
        const data = response?.data?.content || response?.content || response?.data || response;
        campaigns.value = Array.isArray(data) ? data : [];

        pagination.value.totalElements = response?.data?.totalElements || response?.totalElements || campaigns.value.length;
        pagination.value.totalPages = response?.data?.totalPages || response?.totalPages || 1;
    } catch (error) {
        console.error(error);
    } finally {
        loading.value = false;
    }
};

const handleRefresh = async () => {
    isRefreshing.value = true;
    filters.value = { keyword: '', loaiGiamGia: null };
    pagination.value.page = 1;
    await loadCampaigns();
    setTimeout(() => (isRefreshing.value = false), 800);
};

const handleExport = async () => {
    try {
        const blob = await dichVuDotGiamGia.xuatExcelDotGiamGia();
        downloadFile(blob, 'danh_sach_dot_giam_gia.xlsx');
    } catch (error) {
        console.error('Lỗi xuất Excel:', error);
    }
};

const confirmToggleStatus = (item) => {
    confirmDialog.value = {
        show: true,
        title: 'Thay đổi trạng thái',
        message: `Bạn có muốn đổi trạng thái của chiến dịch [${item.ten}]?`,
        color: 'warning',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                const newS = item.trangThai === 'DANG_HOAT_DONG' ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
                await dichVuDotGiamGia.thayDoiTrangThaiDotGiamGia(item.id, newS);
                item.trangThai = newS;
                confirmDialog.value.show = false;
            } catch (e) {
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

const scheduleSearch = () => {
    if (searchDebounce.value) {
        clearTimeout(searchDebounce.value);
    }
    pagination.value.page = 1;
    searchDebounce.value = setTimeout(() => {
        loadCampaigns();
    }, 350);
};

const isActiveStatus = (status) => {
    if (status === null || status === undefined) return false;
    if (typeof status === 'number') return status === 0;
    const normalized = String(status).toUpperCase();
    return normalized === 'DANG_HOAT_DONG' || normalized === 'ACTIVE' || normalized === 'HOAT_DONG' || normalized === '0';
};

const getStatusChipClass = (status) => (isActiveStatus(status) ? 'status-chip-active' : 'status-chip-inactive');

const getDisplayStatus = (status) => (isActiveStatus(status) ? 'Hoạt động' : 'Ngừng hoạt động');

const getLoaiGiamGiaLabel = (type) => {
    const normalized = String(type ?? '').toUpperCase();
    if (normalized === 'PERCENTAGE' || normalized === 'PHAN_TRAM') return 'Phần trăm';
    if (normalized === 'FIXED_AMOUNT' || normalized === 'TIEN_MAT') return 'Tiền mặt';
    return type || '--';
};

const editCampaign = (c) => {
    router.push(`/dot-giam-gia/form/${c.id}`);
};
const openCreateDialog = () => {
    router.push({ name: 'DotGiamGiaForm' });
};
const formatNumber = (value) => {
    const n = Number(value);
    if (!Number.isFinite(n)) return '--';
    return new Intl.NumberFormat('vi-VN').format(n);
};
const formatInteger = (value) => {
    const n = Number(value);
    if (!Number.isFinite(n)) return '0';
    return Number.isInteger(n) ? String(n) : n.toFixed(2).replace(/\.00$/, '');
};
const formatMoneyWithComma = (value) => {
    const n = Number(value);
    if (!Number.isFinite(n)) return '--';
    return `${new Intl.NumberFormat('vi-VN').format(n)}đ`;
};
const getMaxDiscountDisplay = (item) => {
    const value = item?.giamToiDa ?? item?.giaTriGiamToiDa ?? item?.soTienGiamToiDa ?? null;
    return formatMoneyWithComma(value);
};
const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
const formatDate = (timestamp) => (timestamp ? new Date(timestamp).toLocaleDateString('vi-VN') : 'N/A');
const formatDateTime = (timestamp) => {
    if (!timestamp) return '--';
    const d = new Date(Number(timestamp));
    if (Number.isNaN(d.getTime())) return '--';
    const timePart = d.toLocaleTimeString('vi-VN', { hour12: false });
    const datePart = d.toLocaleDateString('vi-VN');
    return `${timePart} ${datePart}`;
};
const getDiscountValueDisplay = (campaign) => {
    const normalized = String(campaign?.loaiGiamGia ?? '').toUpperCase();
    if (normalized === 'PERCENTAGE' || normalized === 'PHAN_TRAM') {
        return `${formatInteger(campaign?.soTienGiam)}%`;
    }
    return formatMoneyWithComma(campaign?.soTienGiam);
};

onMounted(() => loadCampaigns());

onBeforeUnmount(() => {
    if (searchDebounce.value) {
        clearTimeout(searchDebounce.value);
    }
});
</script>

<template>
    <v-container fluid class="pa-4 gray-bg min-h-screen font-body">
        <!-- Header -->
        <div class="mb-6">
            <h1 class="page-title text-h5 font-weight-bold text-slate-900 mb-0">Quản lí đợt giảm giá</h1>
        </div>

        <!-- 1. FILTER -->
        <div class="filter-top invoice-filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field
                        v-model="filters.keyword"
                        placeholder="Tên chiến dịch..."
                        variant="outlined"
                        density="compact"
                        hide-details
                        prepend-inner-icon="mdi-magnify"
                        class="compact-input"
                        @input="scheduleSearch"
                    ></v-text-field>
                </v-col>
                
                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Loại giảm giá</div>
                    <v-radio-group
                        v-model="filters.loaiGiamGia"
                        inline
                        hide-details
                        density="compact"
                        class="compact-radio-group mt-0"
                        @update:model-value="scheduleSearch"
                    >
                        <v-radio label="%" value="PHAN_TRAM" class="mr-1"></v-radio>
                        <v-radio label="VNĐ" value="TIEN_MAT"></v-radio>
                    </v-radio-group>
                </v-col>

                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select
                        v-model="filters.trangThai"
                        :items="[
                            { title: 'Tất cả trạng thái', value: null },
                            { title: 'Đang hoạt động', value: 'DANG_HOAT_DONG' },
                            { title: 'Ngừng hoạt động', value: 'KHONG_HOAT_DONG' }
                        ]"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @update:model-value="scheduleSearch"
                    ></v-select>
                </v-col>

                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Từ ngày</div>
                    <v-text-field
                        v-model="filters.startDate"
                        type="date"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @change="scheduleSearch"
                    ></v-text-field>
                </v-col>

                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Đến ngày</div>
                    <v-text-field
                        v-model="filters.endDate"
                        type="date"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @change="scheduleSearch"
                    ></v-text-field>
                </v-col>
            </AdminFilter>
        </div>

        <!-- 2. TABLE -->
        <AdminTable
            title="Danh sách chiến dịch"
            addButtonText="Tạo mới"
            show-export-button
            :headers="[
                { text: 'STT', align: 'center', width: '60px' },
                { text: 'Mã giảm giá', align: 'left', width: '100px' },
                { text: 'Tên đợt giảm giá', align: 'left', width: '140px' },
                { text: 'Loại giảm giá', align: 'left', width: '120px' },
                { text: 'Giá trị giảm', align: 'left', width: '150px' },
                { text: 'Đơn hàng tối thiểu', align: 'left', width: '150px' },
                { text: 'Thời gian áp dụng', align: 'left', width: '160px' },
                { text: 'Mức ưu tiên', align: 'center', width: '100px' },
                { text: 'Trạng thái', align: 'left', width: '130px' },
                { text: 'Hành động', align: 'center', width: '130px' }
            ]"
            :items="campaigns"
            :total-count="pagination.totalElements"
            :loading="loading"
            @add="openCreateDialog"
            @export="handleExport"
        >
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center">
                        {{ (pagination.page - 1) * pagination.size + index + 1 }}
                    </td>
                    <td class="data-cell text-left col-left-tight">
                        {{ item.ma }}
                    </td>
                    <td class="data-cell text-left col-left-tight">
                        {{ item.ten || '--' }}
                    </td>
                    <td class="data-cell">
                        {{ getLoaiGiamGiaLabel(item.loaiGiamGia) }}
                    </td>
                    <td class="data-cell">
                        <div>Giảm {{ getDiscountValueDisplay(item) }}</div>
                        <div class="max-discount-value">Giảm tối đa: {{ getMaxDiscountDisplay(item) }}</div>
                    </td>
                    <td class="data-cell price-value">{{ formatMoneyWithComma(item.dieuKienGiamGia) }}</td>
                    <td class="data-cell">
                        <div>Từ {{ formatDateTime(item.ngayBatDau) }}</div>
                        <div>Đến {{ formatDateTime(item.ngayKetThuc) }}</div>
                    </td>
                    <td class="data-cell">
                        {{ item.mucUuTien ?? '--' }}
                    </td>
                    <td class="data-cell">
                        <v-chip :class="['px-4 status-chip', getStatusChipClass(item.trangThai)]" variant="flat">{{
                            getDisplayStatus(item.trangThai)
                        }}</v-chip>
                    </td>
                    <td class="data-cell action-cell" style="text-align: center">
                        <div class="d-flex align-center justify-center action-controls">
                            <div class="switch-wrapper">
                                <v-switch
                                    :model-value="isActiveStatus(item.trangThai)"
                                    color="#1e3a8a"
                                    hide-details
                                    density="compact"
                                    class="tight-switch action-switch"
                                    @click.prevent.stop="confirmToggleStatus(item)"
                                />
                                <v-tooltip activator="parent" location="top">Chuyển đổi trạng thái</v-tooltip>
                            </div>
                            <v-btn
                                icon
                                variant="text"
                                size="28"
                                color="#2aa6a1"
                                class="rounded-lg action-icon-btn"
                                @click.stop="router.push({ name: 'DotGiamGiaDetail', params: { id: item.id } })"
                            >
                                <EyeIcon size="15" />
                                <v-tooltip activator="parent" location="top">Xem chi tiết</v-tooltip>
                            </v-btn>
                            <v-btn
                                icon
                                variant="text"
                                size="28"
                                color="#5f6f82"
                                class="rounded-lg action-icon-btn"
                                @click.stop="router.push({ name: 'DotGiamGiaForm', params: { id: item.id } })"
                            >
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                        </div>
                    </td>
                </tr>
            </template>
            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    v-model:page-size="pagination.size"
                    :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements"
                    :current-size="campaigns.length"
                    @change="loadCampaigns"
                />
            </template>
        </AdminTable>

        <!-- SHARED CONFIRM -->
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
    color: #000000 !important;
}
.page-title {
    line-height: 1.1;
}
.tight-switch {
    transform: none;
    width: 36px;
}
.font-body {
    font-family: 'Inter', sans-serif;
}
.line-clamp-1 {
    display: -webkit-box;
    -webkit-line-clamp: 1;
    line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.filter-field-label {
    font-size: 13px;
    font-weight: 700;
    color: #000000 !important;
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
    margin-left: auto !important;
    flex: 0 0 8.333333% !important;
    max-width: 8.333333% !important;
    width: 8.333333% !important;
    align-self: flex-end !important;
    justify-content: flex-end !important;
}

:deep(.filter-top .v-field input),
:deep(.filter-top .v-field__input),
:deep(.filter-top .v-select__selection),
:deep(.filter-top .v-select__selection-text),
:deep(.filter-top .v-field__input::placeholder) {
    font-size: 13px !important;
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

:deep(.native-admin-table .data-cell.col-left-tight) {
    text-align: left !important;
    padding-left: 6px !important;
}

:deep(.native-admin-table .header-cell:nth-child(1)),
:deep(.native-admin-table .header-cell:nth-child(2)),
:deep(.native-admin-table .header-cell:nth-child(3)) {
    text-align: left !important;
    padding-left: 6px !important;
}

:deep(.native-admin-table .header-cell:nth-child(1)),
:deep(.native-admin-table .data-cell:nth-child(1)) {
    text-align: center !important;
    padding-left: 0 !important;
}

:deep(.native-admin-table .header-cell:nth-child(8)),
:deep(.native-admin-table .header-cell:nth-child(9)),
:deep(.native-admin-table .header-cell:nth-child(10)),
:deep(.native-admin-table .data-cell:nth-child(8)),
:deep(.native-admin-table .data-cell:nth-child(9)),
:deep(.native-admin-table .data-cell:nth-child(10)) {
    text-align: center !important;
    padding-left: 0 !important;
}

:deep(.status-chip) {
    border-radius: 999px !important;
    border: 1px solid transparent !important;
    box-shadow: none !important;
    font-size: 13px !important;
    min-height: 32px !important;
}

:deep(.status-chip.status-chip-active) {
    background: #eef4ff !important;
    color: #3f67b0 !important;
    border: 1px solid #c8d8f7 !important;
}

:deep(.status-chip.status-chip-inactive) {
    background: #fdf0f0 !important;
    color: #ef4444 !important;
    border: 1px solid #f2b8b8 !important;
}

:deep(.status-chip .v-chip__content) {
    font-size: 13px !important;
    font-weight: 700 !important;
    line-height: 1.1 !important;
}

:deep(.native-admin-table .price-value) {
    color: #1e3a8a !important;
    font-weight: 700 !important;
}

:deep(.native-admin-table .max-discount-value) {
    color: #1e3a8a !important;
    font-weight: 700 !important;
}

.action-controls {
    gap: 6px;
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

:deep(.action-cell .action-icon-btn .v-btn__overlay),
:deep(.action-cell .action-icon-btn .v-btn__underlay),
:deep(.action-cell .action-icon-btn .v-ripple__container) {
    display: none !important;
}

:deep(.action-cell .action-switch .v-selection-control__wrapper) {
    width: 36px !important;
    min-width: 36px !important;
    height: 22px !important;
}

.switch-wrapper {
    display: inline-flex;
    align-items: center;
    justify-content: center;
}

:deep(.action-cell .action-switch .v-switch__track) {
    background: #ffffff !important;
    border: 1px solid #cbd5e1 !important;
    opacity: 1 !important;
    min-height: 18px !important;
    max-height: 18px !important;
    width: 32px !important;
}

:deep(.action-cell .action-switch .v-selection-control--dirty .v-switch__track) {
    background: #d9e6fb !important;
    border-color: #d9e6fb !important;
}

:deep(.action-cell .action-switch .v-switch__thumb) {
    background: #94a3b8 !important;
    width: 14px !important;
    height: 14px !important;
    box-shadow: none !important;
}

:deep(.action-cell .action-switch .v-selection-control--dirty .v-switch__thumb) {
    background: #1e3a8a !important;
}

.filter-top {
    position: sticky;
    top: 8px;
    z-index: 6;
}

.invoice-filter-shell :deep(.filter-card) {
    border-radius: 14px !important;
    margin-bottom: 8px !important;
}

.invoice-filter-shell :deep(.v-card-text) {
    padding-top: 12px !important;
    padding-bottom: 12px !important;
}
</style>



