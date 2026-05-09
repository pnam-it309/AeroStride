<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { PATH } from '@/router/routePaths';
import { useRouter } from 'vue-router';
import { dichVuDotGiamGia } from '@/services/admin/dichVuDotGiamGia';
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
const startDateRef = ref(null);
const endDateRef = ref(null);

const {
    items: campaigns,
    loading,
    pagination,
    filters,
    loadData: loadCampaigns,
    handleFilter: handleSearch,
    handleReset
} = useAdminTable(dichVuDotGiamGia.layDotGiamGiaPhanTrang, {
    keyword: '',
    trangThai: null,
    startDate: null,
    endDate: null
});

const isRefreshing = ref(false);

const { confirmDialog, setConfirm, handleConfirm } = useConfirmDialog();

const handleRefresh = async () => {
    isRefreshing.value = true;
    handleReset();
    setTimeout(() => (isRefreshing.value = false), 800);
};

const handleExport = async () => {
    try {
        const blob = await dichVuDotGiamGia.xuatExcelDotGiamGia();
        downloadFile(blob, 'danh_sach_dot_giam_gia.xlsx');
    } catch (error) {
        console.error('Lỗi xuất Excel:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể xuất file Excel', color: 'error' });
    }
};

const confirmToggleStatus = (item) => {
    setConfirm({
        title: 'Thay đổi trạng thái',
        message: `Bạn có muốn đổi trạng thái của chiến dịch [${item.ten}]?`,
        color: 'warning',
        action: async () => {
            try {
                const newS = item.trangThai === 'DANG_HOAT_DONG' ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
                console.log(`[Discount] Changing status of ${item.id} to ${newS}`);

                await dichVuDotGiamGia.thayDoiTrangThaiDotGiamGia(item.id, newS);

                // Cập nhật trạng thái local
                item.trangThai = newS;

                addNotification({
                    title: 'Thành công',
                    subtitle: `Đã chuyển sang: ${getStatusLabel(newS)}`,
                    color: 'success'
                });
            } catch (e) {
                console.error('[Discount] Status change error:', e);
                addNotification({
                    title: 'Lỗi',
                    subtitle: 'Không thể thay đổi trạng thái đợt giảm giá',
                    color: 'error'
                });
            }
        }
    });
};


const getCampaignTimelineStatus = (item) => {
    const now = Date.now();
    const start = new Date(item.ngayBatDau).getTime();
    const end = new Date(item.ngayKetThuc).getTime();
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
            label: 'Đã kết thúc',
            color: 'error',
            switchOn: false,
            switchDisabled: true,
            chipClass: 'status-chip-expired',
            isEnded: true,
            switchTooltip: 'Không thể đổi trạng thái lúc này (Đã kết thúc)'
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

const getDiscountValueDisplay = (campaign) => {
    return `${campaign?.soTienGiam}%`;
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

onMounted(() => loadCampaigns());
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body"
        style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important;">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý đợt giảm giá', disabled: false, href: '#' },
            { title: 'Đợt giảm giá', disabled: true }
        ]" />

        <div class="mb-2"></div>

        <!-- 1. FILTER -->
        <div class="filter-top invoice-filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field v-model="filters.keyword" placeholder="Mã hoặc tên đợt..." variant="outlined"
                        density="compact" hide-details prepend-inner-icon="mdi-magnify" class="compact-input" clearable
                        @input="handleSearch"></v-text-field>
                </v-col>


                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select v-model="filters.trangThai" :items="[
                        { title: 'Tất cả trạng thái', value: null },
                        { title: 'Đang hoạt động', value: 'DANG_HOAT_DONG' },
                        { title: 'Sắp diễn ra', value: 'SAP_DIEN_RA' },
                        { title: 'Đã kết thúc', value: 'DA_KET_THUC' }
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch"></v-select>
                </v-col>

                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Từ ngày</div>
                    <v-text-field ref="startDateRef" v-model="filters.startDate" type="date" variant="outlined"
                        density="compact" hide-details class="compact-input date-field"
                        @change="handleSearch"></v-text-field>
                </v-col>

                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Đến ngày</div>
                    <v-text-field ref="endDateRef" v-model="filters.endDate" type="date" variant="outlined"
                        density="compact" hide-details class="compact-input date-field"
                        @change="handleSearch"></v-text-field>
                </v-col>
            </AdminFilter>
        </div>

        <!-- 2. TABLE -->
        <AdminTable title="Danh sách đợt giảm giá" addButtonText="Tạo mới" show-export-button :headers="[
            { text: 'STT', align: 'center', width: '60px' },
            { text: 'Mã', align: 'center', width: '110px' },
            { text: 'Tên đợt giảm giá', align: 'center', width: '180px' },
            { text: 'Giá trị giảm', align: 'center', width: '140px' },
            { text: 'Ngày bắt đầu', align: 'center', width: '160px' },
            { text: 'Ngày kết thúc', align: 'center', width: '160px' },
            { text: 'Trạng thái', align: 'center', width: '130px' },
            { text: 'Hành động', align: 'center', width: '120px' }
        ]" :items="campaigns" :total-count="pagination.totalElements" :loading="loading"
            @add="router.push(PATH.DOT_GIAM_GIA_FORM)" @export="handleExport">
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center text-slate-400">
                        {{ (pagination.page - 1) * pagination.size + index + 1 }}
                    </td>
                    <td class="data-cell text-center">
                        <div class="text-truncate" :title="item.ma">{{ item.ma }}</div>
                    </td>
                    <td class="data-cell text-center">
                        <div class="text-truncate" :title="item.ten">{{ item.ten || '--' }}</div>
                    </td>
                    <td class="data-cell text-center">
                        <div class="text-primary">Giảm {{ getDiscountValueDisplay(item) }}</div>
                    </td>
                    <td class="data-cell text-center">
                        <div class="text-slate-700 text-truncate" :title="formatDateTime(item.ngayBatDau)">{{
                            formatDateTime(item.ngayBatDau) }}</div>
                    </td>
                    <td class="data-cell text-center">
                        <div class="text-slate-700 text-truncate" :title="formatDateTime(item.ngayKetThuc)">{{
                            formatDateTime(item.ngayKetThuc) }}</div>
                    </td>
                    <td class="data-cell text-center">
                        <v-chip :class="['status-chip', getCampaignTimelineStatus(item).chipClass]" variant="flat"
                            size="small">
                            {{ getCampaignTimelineStatus(item).label }}
                        </v-chip>
                    </td>
                    <td class="data-cell action-cell" style="text-align: center">
                        <div class="d-flex align-center justify-center action-controls">
                            <span class="d-inline-block" v-if="getCampaignTimelineStatus(item).isEnded">
                                <v-btn variant="text" class="action-icon-btn opacity-50" style="pointer-events: none"
                                    :ripple="false">
                                    <EditIcon size="15" />
                                </v-btn>
                                <v-tooltip activator="parent" location="top">Không thể cập nhật đợt giảm giá đã kết
                                    thúc</v-tooltip>
                            </span>
                            <v-btn v-else variant="text" class="action-icon-btn"
                                @click.stop="router.push({ name: 'DotGiamGiaForm', params: { id: item.id } })">
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <div class="switch-wrapper">
                                <v-switch :model-value="getCampaignTimelineStatus(item).switchOn"
                                    :disabled="getCampaignTimelineStatus(item).switchDisabled" color="primary"
                                    hide-details density="compact" class="tight-switch action-switch"
                                    :class="{ 'opacity-50': getCampaignTimelineStatus(item).switchDisabled }"
                                    @click.prevent.stop="!getCampaignTimelineStatus(item).switchDisabled && confirmToggleStatus(item)" />
                                <v-tooltip activator="parent" location="top">
                                    {{ getCampaignTimelineStatus(item).switchTooltip }}
                                </v-tooltip>
                            </div>
                        </div>
                    </td>
                </tr>
            </template>
            <template #pagination>
                <AdminPagination v-model="pagination.page" v-model:pageSize="pagination.size"
                    :total-pages="pagination.totalPages" :total-elements="pagination.totalElements"
                    :current-size="campaigns.length" @change="loadCampaigns" />
            </template>
        </AdminTable>

        <!-- SHARED CONFIRM -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="handleConfirm(true)"
            @cancel="handleConfirm(false)" />
    </v-container>
</template>

<style scoped>
/* Scoped styles removed in favor of global _admin-common.scss */
.line-clamp-1 {
    display: -webkit-box;
    -webkit-line-clamp: 1;
    line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.filter-top {
    position: sticky;
    top: 8px;
    z-index: 6;
}

.native-admin-table th,
.native-admin-table td {
    text-align: center !important;
    vertical-align: middle !important;
}

:deep(.data-cell),
:deep(.data-cell *) {
    font-size: 13px !important;
}




.opacity-50 {
    opacity: 0.5 !important;
}
</style>
