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
                const newS = isActiveStatus(item.trangThai) ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';

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
            chipClass: 'chip-upcoming'
        };
    }
    
    // 2. Đã kết thúc
    if (end && now > end) {
        return {
            label: 'Đã kết thúc',
            color: 'error',
            switchOn: false,
            switchDisabled: true,
            chipClass: 'chip-expired'
        };
    }

    // 3. Đang diễn ra
    return {
        label: manualActive ? 'Đang hoạt động' : 'Đã kết thúc',
        color: manualActive ? 'success' : 'error',
        switchOn: manualActive,
        switchDisabled: false,
        chipClass: manualActive ? 'chip-active' : 'chip-expired'
    };
};

import { watch } from 'vue';

// Tự động khôi phục dữ liệu nếu phát hiện dòng bị lỗi (mất mã/tên)
watch(
    vouchers,
    (newItems) => {
        if (newItems && newItems.length > 0) {
            newItems.forEach(async (item) => {
                if (!item.ma || !item.ten || item.ma === '--' || item.ten === '--') {
                    try {
                        // Trích xuất số từ id (ví dụ pgg1 -> 1) và format thành PGG01
                        const idNum = item.id.replace(/\D/g, '');
                        const formattedMa = idNum ? `PGG${idNum.padStart(2, '0')}` : item.id.toUpperCase();

                        const repairData = {
                            ...item,
                            ma: item.ma && item.ma !== '--' ? item.ma : formattedMa,
                            ten: item.ten && item.ten !== '--' ? item.ten : 'Phiếu khôi phục ' + formattedMa
                        };
                        // Gọi API cập nhật âm thầm để vá dữ liệu
                        await dichVuPhieuGiamGia.capNhatPhieuGiamGia(item.id, repairData);
                        console.log(`[Auto-Repair] Fixed voucher: ${item.id} -> ${formattedMa}`);
                    } catch (err) {
                        console.error('[Auto-Repair] Failed for:', item.id, err);
                    }
                }
            });
        }
    },
    { deep: true }
);

onMounted(() => loadVouchers());
</script>

<template>
    <v-container
        fluid
        class="pa-4 animate-fade-in font-body"
        style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important"
    >
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs
            :items="[
                { title: 'Quản lý phiếu giảm giá', disabled: false, href: '#' },
                { title: 'Phiếu giảm giá', disabled: true }
            ]"
        />

        <div class="mb-2"></div>

        <!-- 1. FILTER -->
        <div class="filter-top invoice-filter-shell">
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
                    <v-text-field ref="fromDateRef" v-model="filters.tuNgay" type="date" variant="outlined" density="compact"
                        hide-details class="compact-input date-field" 
                        @change="handleSearch" @input="handleSearch"></v-text-field>
                </v-col>
                <v-col cols="6" sm="3" md="2" class="filter-cell">
                    <div class="filter-field-label">Đến ngày</div>
                    <v-text-field ref="toDateRef" v-model="filters.denNgay" type="date" variant="outlined" density="compact"
                        hide-details class="compact-input date-field" 
                        @change="handleSearch" @input="handleSearch"></v-text-field>
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
                { text: 'Mã phiếu', align: 'center', width: '130px' },
                { text: 'Tên phiếu', align: 'left', width: '150px' },
                { text: 'Hình thức', align: 'center', width: '150px' },
                { text: 'Giá trị giảm', align: 'center', width: '150px' },
                { text: 'Đơn tối thiểu', align: 'center', width: '140px' },
                { text: 'Số lượng', align: 'center', width: '100px' },
                { text: 'Thời gian áp dụng', align: 'left', width: '180px' },
                { text: 'Trạng thái', align: 'center', width: '140px' },
                { text: 'Hành động', align: 'center', width: '110px' }
            ]"
            :items="vouchers"
            :total-count="pagination.totalElements"
            :loading="loading"
            @add="openCreateDialog"
            @export="handleExport"
        >
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center text-slate-400">{{ (pagination.page - 1) * pagination.size + index + 1 }}</td>
                    <td class="data-cell text-center text-primary">
                        <div class="text-truncate" :title="item.ma || item.id">{{ item.ma || item.id || '--' }}</div>
                    </td>
                    <td class="data-cell text-left">
                        <div class="text-truncate" :title="item.ten">{{ item.ten || '--' }}</div>
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
                    <td class="data-cell text-center">
                        <div class="text-primary text-truncate" :title="'Giảm ' + getDiscountDisplay(item)">
                            Giảm {{ getDiscountDisplay(item) }}
                        </div>
                        <div
                            class="max-discount-value text-slate-500 text-truncate"
                            v-if="item.loaiPhieu === 'PHAN_TRAM' || item.loaiPhieu === 'PERCENTAGE'"
                            :title="'Tối đa: ' + getMaxDiscountDisplay(item)"
                        >
                            Tối đa: {{ getMaxDiscountDisplay(item) }}
                        </div>
                    </td>
                    <td class="data-cell text-center">
                        <div class="price-value text-slate-700">{{ formatCurrency(item.donHangToiThieu) }}</div>
                    </td>
                    <td class="data-cell text-center text-slate-700">
                        {{ item.soLuong === -1 ? 'Vô hạn' : item.soLuong }}
                    </td>
                    <td class="data-cell text-left">
                        <div class="d-flex flex-column align-start" style="width: 100%; overflow: hidden">
                            <div
                                class="text-slate-700 text-truncate"
                                style="font-size: 12.5px; width: 100%"
                                :title="'Từ: ' + formatDateTime(item.ngayBatDau)"
                            >
                                Từ: {{ formatDateTime(item.ngayBatDau) }}
                            </div>
                            <div
                                class="text-slate-400 text-truncate"
                                style="font-size: 11px; width: 100%"
                                :title="'Đến: ' + formatDateTime(item.ngayKetThuc)"
                            >
                                Đến: {{ formatDateTime(item.ngayKetThuc) }}
                            </div>
                        </div>
                    </td>
                    <td class="data-cell text-center">
                        <v-chip
                            :class="['status-chip', getVoucherTimelineStatus(item).chipClass]"
                            :color="getVoucherTimelineStatus(item).color"
                            variant="flat"
                            size="small"
                        >
                            {{ getVoucherTimelineStatus(item).label }}
                        </v-chip>
                    </td>
                    <td class="data-cell action-cell text-center">
                        <div class="d-flex align-center justify-center action-controls">
                            <v-btn
                                icon
                                variant="text"
                                :ripple="false"
                                size="28"
                                color="slate-700"
                                class="action-icon-btn"
                                @click.stop="router.push({ name: 'PhieuGiamGiaForm', params: { id: item.id } })"
                            >
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <div class="switch-wrapper">
                                <v-switch
                                    :model-value="getVoucherTimelineStatus(item).switchOn"
                                    :disabled="getVoucherTimelineStatus(item).switchDisabled"
                                    color="primary"
                                    hide-details
                                    density="compact"
                                    class="tight-switch action-switch"
                                    :class="{ 'opacity-50': getVoucherTimelineStatus(item).switchDisabled }"
                                    @click.prevent.stop="!getVoucherTimelineStatus(item).switchDisabled && confirmToggleStatus(item)"
                                />
                                <v-tooltip activator="parent" location="top">
                                    {{ getVoucherTimelineStatus(item).switchDisabled ? 'Không thể đổi trạng thái lúc này' : 'Chuyển đổi trạng thái' }}
                                </v-tooltip>
                            </div>
                        </div>
                    </td>
                </tr>
            </template>
            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    :page-size="pagination.size"
                    @update:pageSize="pagination.size = $event"
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
            @confirm="handleConfirm(true)"
            @cancel="handleConfirm(false)"
        />
    </v-container>
</template>

<style scoped>
/* Scoped styles removed in favor of global _admin-common.scss */
.max-discount-value {
    color: inherit !important;
}

.price-value {
    font-weight: 400 !important;
}

:deep(.data-cell),
:deep(.data-cell *) {
    font-size: 13px !important;
}

:deep(.chip-private) {
    background-color: #fffbeb !important;
    color: #92400e !important;
}

:deep(.chip-public) {
    background-color: #dbeafe !important;
    color: #1e40af !important;
}

:deep(.chip-upcoming) {
    background-color: #f0fdfa !important;
    color: #0f766e !important;
}

:deep(.chip-active) {
    background-color: #f0f1ff !important;
    color: #1e257c !important;
}

:deep(.chip-expired) {
    background-color: #fef2f2 !important;
    color: #991b1b !important;
}

.opacity-50 {
    opacity: 0.5 !important;
}
</style>
