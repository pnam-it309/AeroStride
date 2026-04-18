<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuPhieuGiamGia } from '@/services/admin/dichVuPhieuGiamGia';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EditIcon } from 'vue-tabler-icons';

const router = useRouter();
const loading = ref(false);
const isRefreshing = ref(false);
const vouchers = ref([]);
const searchDebounce = ref(null);

const pagination = ref({ page: 1, size: 5, totalElements: 0, totalPages: 1 });
const filters = ref({ 
    keyword: '', 
    loaiPhieu: null, 
    hinhThuc: null,
    startDate: null,
    endDate: null
});

// Confirmation Logic
const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const loadVouchers = async () => {
    loading.value = true;
    try {
        const params = {
            pageNo: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
            pageSize: pagination.value.size,
            keyword: filters.value.keyword || null,
            loaiPhieu: filters.value.loaiPhieu || null,
            hinhThuc: filters.value.hinhThuc || null
        };
        const response = await dichVuPhieuGiamGia.layPhieuGiamGiaPhanTrang(params);
        const data = response?.data?.content || response?.content || response?.data || response;
        vouchers.value = Array.isArray(data) ? data : [];
        const total = response?.data?.totalElements || response?.totalElements || vouchers.value.length;
        pagination.value.totalElements = total;
        pagination.value.totalPages = response?.data?.totalPages || response?.totalPages || Math.ceil(total / pagination.value.size) || 1;
    } catch (error) {
        console.error(error);
    } finally {
        loading.value = false;
    }
};

const handleRefresh = async () => {
    isRefreshing.value = true;
    filters.value = { keyword: '', loaiPhieu: null, hinhThuc: null };
    pagination.value.page = 1;
    await loadVouchers();
    setTimeout(() => (isRefreshing.value = false), 800);
};

const scheduleSearch = () => {
    if (searchDebounce.value) {
        clearTimeout(searchDebounce.value);
    }
    pagination.value.page = 1;
    searchDebounce.value = setTimeout(() => {
        loadVouchers();
    }, 350);
};

const handleExport = async () => {
    try {
        const blob = await dichVuPhieuGiamGia.xuatExcelPhieuGiamGia();
        downloadFile(blob, 'danh_sach_phieu_giam_gia.xlsx');
    } catch (error) {
        console.error('Lỗi xuất Excel:', error);
    }
};

const confirmToggleStatus = (item) => {
    confirmDialog.value = {
        show: true,
        title: 'Thay đổi trạng thái',
        message: `Bạn có muốn đổi trạng thái của phiếu [${item.ma}]?`,
        color: 'warning',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                const newS = isActiveStatus(item.trangThai) ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
                await dichVuPhieuGiamGia.thayDoiTrangThaiPhieuGiamGia(item.id, newS);
                item.trangThai = newS;
                confirmDialog.value.show = false;
            } catch (e) {
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

const openCreateDialog = () => {
    router.push({ name: 'PhieuGiamGiaForm' });
};
const formatMoney = (amount) => {
    const n = Number(amount);
    if (!Number.isFinite(n)) return '--';
    return new Intl.NumberFormat('vi-VN').format(n) + 'đ';
};
const formatInteger = (value) => {
    const n = Number(value);
    if (!Number.isFinite(n)) return '--';
    return Number.isInteger(n) ? String(n) : n.toFixed(2).replace(/\.00$/, '');
};
const formatPercent = (value) => {
    const n = Number(value);
    if (!Number.isFinite(n)) return '--';
    return `${formatInteger(n)}%`;
};
const getDiscountDisplay = (item) => {
    const normalized = String(item?.loaiPhieu ?? '').toUpperCase();
    if (normalized === 'PHAN_TRAM' || normalized === 'PERCENTAGE') {
        return formatPercent(item?.phanTramGiamGia);
    }
    return formatMoney(item?.soTienGiam);
};
const getMaxDiscountDisplay = (item) => {
    const value = item?.giamToiDa ?? item?.giaTriGiamToiDa ?? item?.soTienGiamToiDa ?? null;
    return formatMoney(value);
};
const formatDateTime = (timestamp) => {
    if (!timestamp) return '--';
    const d = new Date(Number(timestamp));
    if (Number.isNaN(d.getTime())) return '--';
    const timePart = d.toLocaleTimeString('vi-VN', { hour12: false });
    const datePart = d.toLocaleDateString('vi-VN');
    return `${timePart} ${datePart}`;
};
const getLoaiPhieuLabel = (type) => {
    const normalized = String(type ?? '').toUpperCase();
    if (normalized === 'PHAN_TRAM' || normalized === 'PERCENTAGE') return 'Phần trăm';
    if (normalized === 'TIEN_MAT' || normalized === 'FIXED_AMOUNT') return 'Tiền mặt';
    return type || '--';
};
const getHinhThucValue = (item) => {
    const raw = item?.hinhThuc ?? item?.hinhthuc ?? item?.loaiHienThi ?? item?.hinh_thuc ?? null;
    if (raw && typeof raw === 'object') {
        return raw.name ?? raw.value ?? raw.code ?? raw.ten ?? raw.displayName ?? raw.label ?? null;
    }
    return raw;
};
const getHinhThucLabel = (value) => {
    const normalized = String(value ?? '')
        .trim()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/\s+/g, '_')
        .toUpperCase();
    if (
        normalized === 'CONG_KHAI' ||
        normalized === 'PUBLIC' ||
        normalized === 'CONGKHAI' ||
        normalized === 'ALL' ||
        normalized === '0' ||
        normalized === 'FALSE'
    ) {
        return 'Công khai';
    }
    if (
        normalized === 'CA_NHAN' ||
        normalized === 'PRIVATE' ||
        normalized === 'PERSONAL' ||
        normalized === 'CANHAN' ||
        normalized === '1' ||
        normalized === 'TRUE'
    ) {
        return 'Cá nhân';
    }
    return value ? String(value) : '--';
};
const isActiveStatus = (status) => {
    if (status === null || status === undefined) return false;
    if (typeof status === 'number') return status === 0;
    const normalized = String(status).toUpperCase();
    return normalized === 'DANG_HOAT_DONG' || normalized === 'ACTIVE' || normalized === 'HOAT_DONG' || normalized === '0';
};
const getStatusChipClass = (status) => (isActiveStatus(status) ? 'status-chip-active' : 'status-chip-inactive');
const getDisplayStatus = (status) => (isActiveStatus(status) ? 'Hoạt động' : 'Ngừng hoạt động');

onMounted(() => loadVouchers());

onBeforeUnmount(() => {
    if (searchDebounce.value) {
        clearTimeout(searchDebounce.value);
    }
});
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body" style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important;">
        <!-- Header -->
        <div class="mb-6">
            <h1 class="page-title text-h5 font-weight-bold text-slate-900 mb-0">Quản lý phiếu giảm giá</h1>
        </div>

        <!-- 1. FILTER -->
        <div class="filter-top invoice-filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field
                        v-model="filters.keyword"
                        placeholder="Mã hoặc tên phiếu..."
                        variant="outlined"
                        density="compact"
                        hide-details
                        prepend-inner-icon="mdi-magnify"
                        class="compact-input"
                        @input="scheduleSearch"
                    ></v-text-field>
                </v-col>
                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Loại phiếu</div>
                    <v-radio-group
                        v-model="filters.loaiPhieu"
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
                    <div class="filter-field-label">Hình thức</div>
                    <v-select
                        v-model="filters.hinhThuc"
                        :items="[
                            { title: 'Tất cả', value: null },
                            { title: 'Công khai', value: 'CONG_KHAI' },
                            { title: 'Cá nhân', value: 'CA_NHAN' }
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
            title="Danh sách phiếu giảm giá"
            addButtonText="Tạo mới"
            show-export-button
            :headers="[
                { text: 'STT', align: 'center', width: '60px' },
                { text: 'Mã phiếu', align: 'left', width: '90px' },
                { text: 'Tên phiếu', align: 'left', width: '130px' },
                { text: 'Loại phiếu', align: 'left', width: '110px' },
                { text: 'Hình thức', align: 'left', width: '100px' },
                { text: 'Giá trị giảm', align: 'left', width: '150px' },
                { text: 'Đơn hàng tối thiểu', align: 'left', width: '140px' },
                { text: 'Số lượng', align: 'left', width: '85px' },
                { text: 'Thời gian áp dụng', align: 'left', width: '150px' },
                { text: 'Trạng thái', align: 'center', width: '140px' },
                { text: 'Hành động', align: 'center', width: '100px' }
            ]"
            :items="vouchers"
            :total-count="pagination.totalElements"
            :loading="loading"
            @add="openCreateDialog"
            @export="handleExport"
        >
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center">{{ (pagination.page - 1) * pagination.size + index + 1 }}</td>
                    <td class="data-cell text-left col-left-tight">{{ item.ma || '--' }}</td>
                    <td class="data-cell text-left col-left-tight">{{ item.ten || '--' }}</td>
                    <td class="data-cell">{{ getLoaiPhieuLabel(item.loaiPhieu) }}</td>
                    <td class="data-cell">{{ getHinhThucLabel(getHinhThucValue(item)) }}</td>
                    <td class="data-cell">
                        <div>Giảm {{ getDiscountDisplay(item) }}</div>
                        <div class="max-discount-value">Giảm tối đa: {{ getMaxDiscountDisplay(item) }}</div>
                    </td>
                    <td class="data-cell price-value">{{ formatMoney(item.donHangToiThieu) }}</td>
                    <td class="data-cell">{{ formatInteger(item.soLuong) }}</td>
                    <td class="data-cell">
                        <div>Từ {{ formatDateTime(item.ngayBatDau) }}</div>
                        <div>Đến {{ formatDateTime(item.ngayKetThuc) }}</div>
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
                                color="slate-700"
                                class="rounded-lg action-icon-btn"
                                @click.stop="router.push({ name: 'PhieuGiamGiaForm', params: { id: item.id } })"
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
                    :current-size="vouchers.length"
                    @change="loadVouchers"
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
.gray-bg { /* Removed background */ }
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

.filter-field-label {
    font-size: 13px;
    font-weight: 700;
    color: #000000 !important;
    margin-bottom: 6px;
}

.invoice-filter-shell {
    margin-bottom: 8px;
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

:deep(.native-admin-table .header-cell:nth-child(1)),
:deep(.native-admin-table .header-cell:nth-child(10)),
:deep(.native-admin-table .data-cell:nth-child(1)),
:deep(.native-admin-table .header-cell:nth-child(11)),
:deep(.native-admin-table .data-cell:nth-child(11)) {
    text-align: center !important;
    padding-left: 0 !important;
}

:deep(.native-admin-table .data-cell.col-left-tight) {
    text-align: left !important;
    padding-left: 6px !important;
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

.filter-top :deep(.filter-reset-col) {
    margin-left: auto !important;
    flex: 0 0 8.333333% !important;
    max-width: 8.333333% !important;
    width: 8.333333% !important;
    align-self: flex-end !important;
    justify-content: flex-end !important;
}
</style>



