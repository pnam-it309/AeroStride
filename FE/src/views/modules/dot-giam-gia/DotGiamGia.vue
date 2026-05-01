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

const router = useRouter();

const {
    items: campaigns,
    loading,
    pagination,
    filters,
    loadData: loadCampaigns,
    handleFilter: handleSearch,
    handleReset
} = useAdminTable(dichVuDotGiamGia.layDotGiamGiaPhanTrang, {
    search: '',
    loaiGiamGia: null,
    trangThai: null,
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
    if (end && now > end) return { label: 'Kết thúc', color: '#ef4444', bg: 'rgba(239,68,68,0.1)' };
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


const getDiscountValueDisplay = (campaign) => {
    return `${campaign?.soTienGiam}%`;
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
                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field v-model="filters.search" placeholder="Tên đợt giảm giá..." variant="outlined"
                        density="compact" hide-details prepend-inner-icon="mdi-magnify" class="compact-input"
                        @input="handleSearch"></v-text-field>
                </v-col>


                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select v-model="filters.trangThai" :items="[
                        { title: 'Tất cả trạng thái', value: null },
                        { title: 'Đang hoạt động', value: 'DANG_HOAT_DONG' },
                        { title: 'Ngừng hoạt động', value: 'KHONG_HOAT_DONG' }
                    ]" variant="outlined" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch"></v-select>
                </v-col>

                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Từ ngày</div>
                    <v-text-field v-model="filters.startDate" type="date" variant="outlined" density="compact"
                        hide-details class="compact-input" @change="handleSearch"></v-text-field>
                </v-col>

                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Đến ngày</div>
                    <v-text-field v-model="filters.endDate" type="date" variant="outlined" density="compact"
                        hide-details class="compact-input" @change="handleSearch"></v-text-field>
                </v-col>
            </AdminFilter>
        </div>

        <!-- 2. TABLE -->
        <AdminTable title="Danh sách đợt giảm giá" addButtonText="Tạo mới" show-export-button :headers="[
            { text: 'STT', width: '60px' },
            { text: 'Mã', width: '120px' },
            { text: 'Tên đợt giảm giá', width: '160px' },
            { text: 'Giá trị giảm', width: '140px' },
            { text: 'Ngày bắt đầu', width: '160px' },
            { text: 'Ngày kết thúc', width: '140px' },
            { text: 'Trạng thái', width: '130px' },
            { text: 'Hành động', width: '120px' }
        ]" :items="campaigns" :total-count="pagination.totalElements" :loading="loading"
            @add="router.push(PATH.DOT_GIAM_GIA_FORM)" @export="handleExport">
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center text-slate-400">
                        {{ (pagination.page - 1) * pagination.size + index + 1 }}
                    </td>
                    <td class="data-cell text-center">
                        {{ item.ma }}
                    </td>
                    <td class="data-cell text-left">
                        {{ item.ten || '--' }}
                    </td>
                    <td class="data-cell text-left">
                        <div class="text-primary">Giảm {{ getDiscountValueDisplay(item) }}</div>
                    </td>
                    <td class="data-cell text-left">
                        <div class="text-slate-700">{{ formatDateTime(item.ngayBatDau) }}</div>
                    </td>
                    <td class="data-cell text-left">
                        <div class="text-slate-700">{{ formatDateTime(item.ngayKetThuc) }}</div>
                    </td>
                    <td class="data-cell text-center">
                        <v-chip
                            :class="['status-chip', isActiveStatus(item.trangThai) ? 'status-chip-active' : 'status-chip-inactive']"
                            variant="flat" size="small">
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>
                    <td class="data-cell action-cell" style="text-align: center">
                        <div class="d-flex align-center justify-center action-controls">
                            <v-btn variant="text" class="action-icon-btn"
                                @click.stop="router.push({ name: 'DotGiamGiaForm', params: { id: item.id } })">
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
                <AdminPagination v-model:page="pagination.page" v-model:page-size="pagination.size"
                    :total-pages="pagination.totalPages" :total-elements="pagination.totalElements"
                    :current-size="campaigns.length" @change="loadCampaigns" />
            </template>
        </AdminTable>

        <!-- SHARED CONFIRM -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />
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

:deep(.data-cell),
:deep(.data-cell *) {
    font-size: 13px !important;
}

:deep(.status-chip-active) {
    background-color: #10b9811a !important;
    color: #10b981 !important;
}

:deep(.status-chip-inactive) {
    background-color: #ef44441a !important;
    color: #ef4444 !important;
}
</style>

