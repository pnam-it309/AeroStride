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
import { ADMIN_ICONS } from '@/constants/adminIcons';

import { useAdminTable } from '@/composables/useAdminTable';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useNotifications } from '@/services/notificationService';

const router = useRouter();
const { addNotification } = useNotifications();
const fromDateRef = ref(null);
const toDateRef = ref(null);

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
    trangThai: null,
    timelineStatus: null
});

const isRefreshing = ref(false);

const { confirmDialog, setConfirm, handleConfirm } = useConfirmDialog();

const handleRefresh = async () => {
    isRefreshing.value = true;
    handleReset();
    setTimeout(() => (isRefreshing.value = false), 800);
};

const openDatePicker = (ref) => {
    const input = ref?.$el?.querySelector('input[type="date"]');
    if (input) {
        if (typeof input.showPicker === 'function') {
            input.showPicker();
        } else {
            input.click();
        }
    }
};

const handleExport = async () => {
    try {
        const blob = await dichVuPhieuGiamGia.xuatExcelPhieuGiamGia();
        downloadFile(blob, 'danh_sach_phieu_giam_gia.xlsx');
    } catch (error) {
        console.error('Lỗi xuất Excel:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể xuất file Excel', color: 'error' });
    }
};

const confirmToggleStatus = (item) => {
    setConfirm({
        title: 'Thay đổi trạng thái',
        message: `Bạn có muốn đổi trạng thái của phiếu [${item.ma}]?`,
        color: 'warning',
        action: async () => {
            try {
                const newS = isActiveStatus(item.trangThai) ? 'NGUNG_HOAT_DONG' : 'DANG_HOAT_DONG';

                await dichVuPhieuGiamGia.thayDoiTrangThaiPhieuGiamGia(item.id, newS);

                // Cập nhật trạng thái local
                item.trangThai = newS;

                addNotification({
                    title: 'Thành công',
                    subtitle: `Đã chuyển sang: ${getStatusLabel(newS)}`,
                    color: 'success'
                });
            } catch (e) {
                console.error('[Voucher] Status change error:', e);
                addNotification({
                    title: 'Lỗi',
                    subtitle: 'Không thể thay đổi trạng thái phiếu giảm giá',
                    color: 'error'
                });
            }
        }
    });
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
    if (normalized === 'CONGKHAI' || normalized === 'PUBLIC' || normalized === 'ALL' || normalized === '0' || normalized === 'FALSE')
        return 'Công khai';
    if (normalized === 'CANHAN' || normalized === 'PRIVATE' || normalized === 'PERSONAL' || normalized === '1' || normalized === 'TRUE')
        return 'Cá nhân';
    return value;
};

const getVoucherTimelineStatus = (item) => {
    const now = Date.now();
    const start = item.ngayBatDau;
    const end = item.ngayKetThuc;
    const manualActive = isActiveStatus(item.trangThai);

    // 1. Sắp diễn ra
    if (now < start) {
        return {
            label: 'Sắp diễn ra',
            color: 'info',
            switchOn: false,
            switchDisabled: true,
            chipClass: 'status-chip-upcoming',
            isEnded: false,
            switchTooltip: 'Không thể đổi trạng thái lúc này (Chưa bắt đầu)'
        };
    }

    // 2. Đã kết thúc
    if (end && now > end) {
        return {
            label: 'Đã kết thúc',
            color: 'error',
            switchOn: false,
            switchDisabled: true,
            chipClass: 'status-chip-expired',
            isEnded: true,
            switchTooltip: 'Không thể đổi trạng thái lúc này (Đã kết thúc)'
        };
    }

    // 3. Đang diễn ra
    if (!manualActive) {
        return {
            label: 'Ngừng hoạt động',
            color: 'warning',
            switchOn: false,
            switchDisabled: false,
            chipClass: 'status-chip-inactive',
            isEnded: false,
            switchTooltip: 'Kích hoạt lại phiếu giảm giá'
        };
    }

    return {
        label: 'Đang hoạt động',
        color: 'success',
        switchOn: true,
        switchDisabled: false,
        chipClass: 'status-chip-active',
        isEnded: false,
        switchTooltip: 'Chuyển đổi trạng thái'
    };
};



onMounted(() => loadVouchers());
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý phiếu giảm giá', disabled: false, href: '#' },
            { title: 'Phiếu giảm giá', disabled: true }
        ]" />

        <div class="mb-2"></div>

        <!-- 1. FILTER -->
        <div class="filter-shell invoice-filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" sm="6" md="3" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field v-model="filters.keyword" placeholder="Mã hoặc tên phiếu..." variant="outlined"
                        density="compact" hide-details prepend-inner-icon="mdi-magnify" class="compact-input" clearable
                        @input="handleSearch"></v-text-field>
                </v-col>
                <v-col cols="6" sm="3" md="2" class="filter-cell">
                    <div class="filter-field-label">Hình thức</div>
                    <v-select v-model="filters.hinhThuc" :items="[
                        { title: 'Tất cả', value: null },
                        { title: 'Công khai', value: 'CONG_KHAI' },
                        { title: 'Cá nhân', value: 'CA_NHAN' }
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch"></v-select>
                </v-col>
                <v-col cols="6" sm="3" md="2" class="filter-cell">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select v-model="filters.timelineStatus" :items="[
                        { title: 'Tất cả', value: null },
                        { title: 'Đang hoạt động', value: 'DANG_HOAT_DONG' },
                        { title: 'Sắp diễn ra', value: 'SAP_DIEN_RA' },
                        { title: 'Đã kết thúc', value: 'DA_KET_THUC' }
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch"></v-select>
                </v-col>
                <v-col cols="6" sm="3" md="2" class="filter-cell">
                    <div class="filter-field-label">Từ ngày</div>
                    <v-text-field ref="fromDateRef" v-model="filters.tuNgay" type="date" variant="outlined"
                        density="compact" hide-details class="compact-input date-field" @change="handleSearch"
                        @input="handleSearch"></v-text-field>
                </v-col>
                <v-col cols="6" sm="3" md="2" class="filter-cell">
                    <div class="filter-field-label">Đến ngày</div>
                    <v-text-field ref="toDateRef" v-model="filters.denNgay" type="date" variant="outlined"
                        density="compact" hide-details class="compact-input date-field" @change="handleSearch"
                        @input="handleSearch"></v-text-field>
                </v-col>
            </AdminFilter>
        </div>

        <!-- 2. TABLE -->
        <AdminTable title="Danh sách phiếu giảm giá" addButtonText="Tạo mới" show-export-button :headers="[
            { text: 'STT', width: '60px' },
            { text: 'Mã phiếu', width: '130px' },
            { text: 'Tên phiếu', width: '180px' },
            { text: 'Hình thức', width: '150px' },
            { text: 'Giá trị giảm', width: '150px' },
            { text: 'Đơn tối thiểu', width: '140px' },
            { text: 'Số lượng', width: '100px' },
            { text: 'Thời gian áp dụng', width: '180px' },
            { text: 'Trạng thái', width: '140px' },
            { text: 'Hành động', width: '110px' }
        ]" :items="vouchers" :total-count="pagination.totalElements" :loading="loading" @add="openCreateDialog"
            @export="handleExport">
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-slate-400">{{ (pagination.page - 1) * pagination.size + index
                        + 1 }}</td>
                    <td class="data-cell text-center text-primary">
                        <div class="text-truncate" :title="item.ma || item.id">{{ item.ma || item.id || '--' }}</div>
                    </td>
                    <td class="data-cell text-left px-4">
                        <div class="text-truncate" :title="item.ten">{{ item.ten || '--' }}</div>
                    </td>

                    <td class="data-cell">
                        <v-chip variant="flat" :class="[
                            'status-chip',
                            getHinhThucLabel(getHinhThucValue(item)) === 'Cá nhân' ? 'status-chip-private' : 'status-chip-public'
                        ]">
                            {{ getHinhThucLabel(getHinhThucValue(item)) }}
                        </v-chip>
                    </td>
                    <td class="data-cell text-left px-4">
                        <div class="text-primary text-truncate" :title="'Giảm ' + getDiscountDisplay(item)">
                            Giảm {{ getDiscountDisplay(item) }}
                        </div>
                        <div class="max-discount-value text-slate-500 text-truncate"
                            v-if="item.loaiPhieu === 'PHAN_TRAM' || item.loaiPhieu === 'PERCENTAGE'"
                            :title="'Tối đa: ' + getMaxDiscountDisplay(item)">
                            Tối đa: {{ getMaxDiscountDisplay(item) }}
                        </div>
                    </td>
                    <td class="data-cell text-center">
                        <div class="price-value text-slate-700">{{ formatCurrency(item.donHangToiThieu) }}</div>
                    </td>
                    <td class="data-cell text-balanced text-slate-700">
                        {{ item.soLuong === -1 ? 'Vô hạn' : item.soLuong }}
                    </td>
                    <td class="data-cell text-center">
                        <div class="d-inline-flex flex-column align-center" style="width: 100%; overflow: hidden">
                            <div class="text-slate-700 text-truncate" style="width: 100%"
                                :title="'Từ: ' + formatDateTime(item.ngayBatDau)">
                                Từ: {{ formatDateTime(item.ngayBatDau) }}
                            </div>
                            <div class="text-slate-400 text-truncate" style="width: 100%"
                                :title="'Đến: ' + formatDateTime(item.ngayKetThuc)">
                                Đến: {{ formatDateTime(item.ngayKetThuc) }}
                            </div>
                        </div>
                    </td>
                    <td class="data-cell">
                        <v-chip :class="['status-chip', getVoucherTimelineStatus(item).chipClass]" variant="flat">
                            {{ getVoucherTimelineStatus(item).label }}
                        </v-chip>
                    </td>
                    <td class="data-cell action-cell text-center">
                        <div class="d-flex align-center justify-center action-controls">
                            <span class="d-inline-block" v-if="getVoucherTimelineStatus(item).isEnded">
                                <v-btn icon variant="text" :ripple="false" size="28" color="slate-700"
                                    class="action-icon-btn opacity-50" style="pointer-events: none">
                                    <component :is="ADMIN_ICONS.ACTION.EDIT" size="15" />
                                </v-btn>
                                <v-tooltip activator="parent" location="top">Không thể cập nhật phiếu giảm giá đã kết
                                    thúc</v-tooltip>
                            </span>
                            <v-btn v-else icon variant="text" :ripple="false" size="28" color="slate-700"
                                class="action-icon-btn"
                                @click.stop="router.push({ name: 'PhieuGiamGiaForm', params: { id: item.id } })">
                                <component :is="ADMIN_ICONS.ACTION.EDIT" size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <div class="switch-wrapper">
                                <v-switch :model-value="getVoucherTimelineStatus(item).switchOn"
                                    :disabled="getVoucherTimelineStatus(item).switchDisabled" color="primary"
                                    hide-details density="compact" class="tight-switch action-switch"
                                    :class="{ 'opacity-50': getVoucherTimelineStatus(item).switchDisabled }"
                                    @click.prevent.stop="!getVoucherTimelineStatus(item).switchDisabled && confirmToggleStatus(item)" />
                                <v-tooltip activator="parent" location="top">
                                    {{ getVoucherTimelineStatus(item).switchTooltip }}
                                </v-tooltip>
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
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="handleConfirm(true)"
            @cancel="handleConfirm(false)" />
    </v-container>
</template>

<style scoped>

</style>
