<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuPhieuGiamGia } from '@/services/admin/dichVuPhieuGiamGia';
import { formatCurrency, formatDateTime } from '@/utils/formatters';
import { isActiveStatus } from '@/utils/statusUtils';

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
    search: '',
    loaiPhieu: null,
    hinhThuc: null,
    startDate: null,
    endDate: null
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
    return item?.hinhThuc ?? item?.loaiHienThi ?? item?.phanLoai ?? item?.hinh_thuc ?? null;
};
const getHinhThucLabel = (value) => {
    if (value === null || value === undefined) return '--';
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
                    <v-text-field v-model="filters.search" placeholder="Mã hoặc tên phiếu..." variant="outlined"
                        density="compact" hide-details prepend-inner-icon="mdi-magnify" class="compact-input" clearable
                        @update:model-value="handleSearch"></v-text-field>
                </v-col>
                <v-col cols="12" md="2" class="filter-cell pt-1">
                    <div class="filter-field-label">Loại phiếu</div>
                    <v-radio-group v-model="filters.loaiPhieu" inline hide-details density="compact"
                        class="compact-radio-group mt-1" @update:model-value="handleSearch">
                        <v-radio label="%" value="PHAN_TRAM" class="mr-1"></v-radio>
                        <v-radio label="VNĐ" value="TIEN_MAT"></v-radio>
                    </v-radio-group>
                </v-col>
                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Hình thức</div>
                    <v-select v-model="filters.hinhThuc" :items="[
                        { title: 'Tất cả', value: null },
                        { title: 'Công khai', value: 'CONG_KHAI' },
                        { title: 'Cá nhân', value: 'CA_NHAN' }
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch"></v-select>
                </v-col>
                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Từ ngày</div>
                    <v-text-field v-model="filters.startDate" type="date" variant="outlined" density="compact"
                        hide-details class="compact-input" @update:model-value="handleSearch"></v-text-field>
                </v-col>
                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Đến ngày</div>
                    <v-text-field v-model="filters.endDate" type="date" variant="outlined" density="compact"
                        hide-details class="compact-input" @update:model-value="handleSearch"></v-text-field>
                </v-col>
            </AdminFilter>
        </div>

        <!-- 2. TABLE -->
        <AdminTable title="Danh sách phiếu giảm giá" addButtonText="Tạo mới" show-export-button :headers="[
            { text: 'STT', align: 'center', width: '55px' },
            { text: 'Mã phiếu', align: 'center', width: '120px' },
            { text: 'Tên phiếu', align: 'center', width: '200px' },
            { text: 'Hình thức', align: 'center', width: '120px' },
            { text: 'Giá trị giảm', align: 'center', width: '170px' },
            { text: 'Đơn tối thiểu', align: 'center', width: '130px' },
            { text: 'Số lượng', align: 'center', width: '90px' },
            { text: 'Thời gian áp dụng', align: 'center', width: '200px' },
            { text: 'Trạng thái', align: 'center', width: '140px' },
            { text: 'Hành động', align: 'center', width: '110px' }
        ]" :items="vouchers" :total-count="pagination.totalElements" :loading="loading" @add="openCreateDialog"
            @export="handleExport">
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center text-slate-400">{{ (pagination.page - 1) * pagination.size + index + 1 }}</td>
                    <td class="data-cell text-left col-left-tight">{{ item.ma || '--' }}</td>
                    <td class="data-cell text-center">{{ item.ten || '--' }}</td>

                    <td class="data-cell text-center">
                        <v-chip size="small" variant="tonal"
                            :color="getHinhThucValue(item) === 'CA_NHAN' || getHinhThucValue(item) === 'PRIVATE' ? 'purple' : getHinhThucValue(item) === 'CONG_KHAI' || getHinhThucValue(item) === 'PUBLIC' ? 'info' : 'grey'"
                            class="px-3">
                            {{ getHinhThucLabel(getHinhThucValue(item)) }}
                        </v-chip>
                    </td>
                    <td class="data-cell text-center">
                        <div class="text-primary">Giảm {{ getDiscountDisplay(item) }}</div>
                        <div class="max-discount-value"
                            v-if="item.loaiPhieu === 'PHAN_TRAM' || item.loaiPhieu === 'PERCENTAGE'">Tối đa: {{
                                getMaxDiscountDisplay(item) }}</div>
                    </td>
                    <td class="data-cell text-center price-value">{{ formatCurrency(item.donHangToiThieu) }}</td>
                    <td class="data-cell text-center text-slate-700">
                        {{ item.soLuong === -1 ? 'Vô hạn' : item.soLuong }}
                    </td>
                    <td class="data-cell">
                        <div class="d-flex flex-column align-center">
                            <div class="text-caption text-slate-700">{{ formatDateTime(item.ngayBatDau) }}</div>
                            <div class="text-caption text-slate-400">đến {{ formatDateTime(item.ngayKetThuc) }}</div>
                        </div>
                    </td>
                    <td class="data-cell text-center">
                        <span class="discount-status-chip"
                            :style="{ color: getDiscountTimeStatus(item).color, background: getDiscountTimeStatus(item).bg }">
                            {{ getDiscountTimeStatus(item).label }}
                        </span>
                    </td>
                    <td class="data-cell action-cell" style="text-align: center">
                        <div class="d-flex align-center justify-center action-controls">
                            <v-btn icon variant="text" :ripple="false" size="28" color="slate-700"
                                class="action-icon-btn"
                                @click.stop="router.push({ name: 'PhieuGiamGiaForm', params: { id: item.id } })">
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <div class="switch-wrapper">
                                <v-switch :model-value="isActiveStatus(item.trangThai)" color="#000" hide-details
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
    color: #64748b !important;
    font-size: 12px;
}
</style>
