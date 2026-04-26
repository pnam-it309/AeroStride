<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuPhieuGiamGia } from '@/services/admin/dichVuPhieuGiamGia';
import { formatCurrency, formatDateTime } from '@/utils/formatters';
import { isActiveStatus, getStatusLabel } from '@/utils/statusUtils';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { downloadFile } from '@/utils/fileUtils';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import { EditIcon } from 'vue-tabler-icons';

import { useAdminTable } from '@/composables/useAdminTable';

const router = useRouter();

const {
    items: vouchers,
    loading,
    pagination,
    filters,
    loadData: loadVouchers,
    handleFilter: handleSearch,
    handleReset
} = useAdminTable(dichVuPhieuGiamGia.layPhieuGiamGiaPhanTrang, {
    keyword: '',
    loaiPhieu: null,
    hinhThuc: null,
    tuNgay: null,
    denNgay: null,
    trangThai: null
});

const isRefreshing = ref(false);

// Tính trạng thái theo thời gian thực (ngày bắt đầu / kết thúc)
const getDiscountTimeStatus = (item) => {
    const now = Date.now();
    const start = item.ngayBatDau ? new Date(item.ngayBatDau).getTime() : null;
    const end = item.ngayKetThuc ? new Date(item.ngayKetThuc).getTime() : null;
    if (start && now < start) return { label: 'Sắp diễn ra', color: '#f59e0b', bg: 'rgba(245,158,11,0.1)' };
    if (end && now > end)   return { label: 'Kết thúc',    color: '#ef4444', bg: 'rgba(239,68,68,0.1)' };
    return { label: 'Hoạt động', color: '#10b981', bg: 'rgba(16,185,129,0.1)' };
};

// Confirmation Logic
const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const handleRefresh = async () => {
    isRefreshing.value = true;
    handleReset();
    setTimeout(() => (isRefreshing.value = false), 800);
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
    return formatCurrency(item?.soTienGiam);
};
const getMaxDiscountDisplay = (item) => {
    const value = item?.giamToiDa ?? item?.giaTriGiamToiDa ?? item?.soTienGiamToiDa ?? null;
    return formatCurrency(value);
};
const getLoaiPhieuLabel = (type) => {
    const normalized = String(type ?? '').toUpperCase();
    if (normalized === 'PHAN_TRAM' || normalized === 'PERCENTAGE') return 'Phần trăm';
    if (normalized === 'TIEN_MAT' || normalized === 'FIXED_AMOUNT') return 'Tiền mặt';
    return type || '--';
};
const getHinhThucValue = (item) => {
    return item?.hinhThuc ?? item?.hinh_thuc ?? item?.loaiHienThi ?? item?.phanLoai ?? null;
};
const getHinhThucLabel = (value) => {
    if (value === null || value === undefined || value === '') return 'Công khai';
    const normalized = String(value).toUpperCase().replace(/[\s_]/g, '');
    if (normalized === 'CONGKHAI' || normalized === 'PUBLIC' || normalized === 'ALL' || normalized === '0' || normalized === 'FALSE') return 'Công khai';
    if (normalized === 'CANHAN' || normalized === 'PRIVATE' || normalized === 'PERSONAL' || normalized === '1' || normalized === 'TRUE') return 'Cá nhân';
    return value;
};

onMounted(() => loadVouchers());
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body"
        style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important;">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý phiếu giảm giá', disabled: false, href: '#' },
            { title: 'Phiếu giảm giá', disabled: true }
        ]" />

        <div class="mb-2"></div>

        <!-- 1. FILTER -->
        <div class="filter-top invoice-filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field v-model="filters.keyword" placeholder="Mã hoặc tên phiếu..." variant="outlined"
                        density="compact" hide-details prepend-inner-icon="mdi-magnify" class="compact-input" clearable
                        @input="handleSearch"></v-text-field>
                </v-col>
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Loại phiếu</div>
                    <v-select v-model="filters.loaiPhieu" :items="[
                        { title: 'Tất cả', value: null },
                        { title: 'Phần trăm (%)', value: 'PHAN_TRAM' },
                        { title: 'Tiền mặt (VNĐ)', value: 'TIEN_MAT' }
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch"></v-select>
                </v-col>
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Hình thức</div>
                    <v-select v-model="filters.hinhThuc" :items="[
                        { title: 'Tất cả', value: null },
                        { title: 'Công khai', value: 'CONG_KHAI' },
                        { title: 'Cá nhân', value: 'CA_NHAN' }
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch"></v-select>
                </v-col>
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select v-model="filters.trangThai" :items="[
                        { title: 'Tất cả', value: null },
                        { title: 'Đang hoạt động', value: 'DANG_HOAT_DONG' },
                        { title: 'Ngừng hoạt động', value: 'KHONG_HOAT_DONG' }
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch"></v-select>
                </v-col>
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Từ ngày</div>
                    <v-text-field v-model="filters.tuNgay" type="date" variant="outlined" density="compact"
                        hide-details class="compact-input" @change="handleSearch" @input="handleSearch"></v-text-field>
                </v-col>
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Đến ngày</div>
                    <v-text-field v-model="filters.denNgay" type="date" variant="outlined" density="compact"
                        hide-details class="compact-input" @change="handleSearch" @input="handleSearch"></v-text-field>
                </v-col>
            </AdminFilter>
        </div>

        <!-- 2. TABLE -->
        <AdminTable title="Danh sách phiếu giảm giá" addButtonText="Tạo mới" show-export-button :headers="[
            { text: 'STT', align: 'center', width: '60px' },
            { text: 'Mã phiếu', align: 'center', width: '130px' },
            { text: 'Tên phiếu', align: 'center' },
            { text: 'Loại phiếu', align: 'center', width: '120px' },
            { text: 'Hình thức', align: 'center', width: '130px' },
            { text: 'Giá trị giảm', align: 'center' },
            { text: 'Đơn tối thiểu', align: 'center', width: '140px' },
            { text: 'Số lượng', align: 'center', width: '90px' },
            { text: 'Thời gian áp dụng', align: 'center', width: '180px' },
            { text: 'Trạng thái', align: 'center', width: '140px' },
            { text: 'Hành động', align: 'center', width: '110px' }
        ]" :items="vouchers" :total-count="pagination.totalElements" :loading="loading" @add="openCreateDialog"
            @export="handleExport">
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center text-slate-400 font-weight-medium">{{ (pagination.page - 1) * pagination.size + index + 1 }}</td>
                    <td class="data-cell text-center font-weight-medium text-primary">{{ item.ma || '--' }}</td>
                    <td class="data-cell text-left font-weight-medium">{{ item.ten || '--' }}</td>
                    <td class="data-cell text-center font-weight-medium">
                        {{ getLoaiPhieuLabel(item.loaiPhieu) }}
                    </td>
                    <td class="data-cell text-center">
                        <v-chip
                            size="small"
                            variant="flat"
                            :class="[
                                'status-chip',
                                getHinhThucLabel(getHinhThucValue(item)) === 'Cá nhân' ? 'chip-private' : 'chip-public'
                            ]"
                        >
                            {{ getHinhThucLabel(getHinhThucValue(item)) }}
                        </v-chip>
                    </td>
                    <td class="data-cell text-left">
                        <div class="font-weight-bold text-primary">Giảm {{ getDiscountDisplay(item) }}</div>
                        <div class="max-discount-value font-weight-medium text-slate-500"
                            v-if="item.loaiPhieu === 'PHAN_TRAM' || item.loaiPhieu === 'PERCENTAGE'">Tối đa: {{
                                getMaxDiscountDisplay(item) }}</div>
                    </td>
                    <td class="data-cell text-left">
                        <div class="price-value font-weight-bold text-slate-700">{{ formatCurrency(item.donHangToiThieu) }}</div>
                    </td>
                    <td class="data-cell text-center font-weight-medium text-slate-700">
                        {{ item.soLuong === -1 ? '∞' : item.soLuong }}
                    </td>
                    <td class="data-cell text-left">
                        <div class="d-flex flex-column align-start">
                            <div class="font-weight-medium text-slate-700" style="font-size: 12.5px">{{ formatDateTime(item.ngayBatDau) }}</div>
                            <div class="font-weight-medium text-slate-400" style="font-size: 11px">đến {{ formatDateTime(item.ngayKetThuc) }}</div>
                        </div>
                    </td>
                    <td class="data-cell text-center">
                        <v-chip 
                            :class="['status-chip', isActiveStatus(item.trangThai) ? 'status-chip-active' : 'status-chip-inactive']"
                            variant="flat" 
                            size="small">
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>
                    <td class="data-cell action-cell text-center">
                        <div class="d-flex align-center justify-center action-controls">
                            <v-btn icon variant="text" :ripple="false" size="28" color="slate-700"
                                class="action-icon-btn"
                                @click.stop="router.push({ name: 'PhieuGiamGiaForm', params: { id: item.id } })">
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <div class="switch-wrapper">
                                <v-switch :model-value="isActiveStatus(item.trangThai)" color="primary" hide-details
                                    density="compact" class="tight-switch action-switch"
                                    @click.prevent.stop="confirmToggleStatus(item)" />
                                <v-tooltip activator="parent" location="top">Chuyển đổi trạng thái</v-tooltip>
                            </div>
                        </div>
                    </td>
                </tr>
            </template>
            <template #pagination>
                <AdminPagination v-model="pagination.page" :page-size="pagination.size"
                    @update:pageSize="pagination.size = $event" :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements" :current-size="vouchers.length" @change="loadVouchers" />
            </template>
        </AdminTable>

        <!-- SHARED CONFIRM -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />
    </v-container>
</template>

<style scoped>
/* Scoped styles removed in favor of global _admin-common.scss */
.max-discount-value {
    color: inherit !important;
}

.price-value {
    font-weight: 700 !important;
}

:deep(.data-cell), :deep(.data-cell *) {
    font-size: 13px !important;
}

:deep(.chip-private) {
    background-color: #fff7ed !important;
    color: #c2410c !important;
}

:deep(.chip-public) {
    background-color: #f0fdf4 !important;
    color: #15803d !important;
}
</style>

