<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';
import { PATH } from '@/router/routePaths';
import { formatDateTime } from '@/utils/formatters';
import { isActiveStatus, getStatusLabel } from '@/utils/statusUtils';
import { SYSTEM_STATUS } from '@/constants/statusConstants';

// REUSABLE COMPONENTS
import { AdminFilter, AdminTable, AdminPagination, AdminConfirm, AdminBreadcrumbs } from '@/components/common';
import { downloadFile } from '@/utils/fileUtils';
import { EditIcon, RefreshIcon } from 'vue-tabler-icons';

import { useNotifications } from '@/services/notificationService';

import { useAdminTable } from '@/composables/useAdminTable';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useRefreshHandler } from '@/composables/useRefreshHandler';

import { GIOI_TINH_FILTER_OPTIONS } from '@/constants/appConstants';
import { TRANG_THAI_FILTER_OPTIONS } from '@/constants/nhanVienConstants';

const {
    items: employees,
    loading,
    pagination,
    filters,
    loadData: loadEmployees,
    handleFilter: handleSearch,
    handleReset
} = useAdminTable(dichVuNhanVien.layNhanVienPhanTrang, { search: '', gioiTinh: null, trangThai: null });

const router = useRouter();
const { confirmDialog, setConfirm, clearConfirm, handleConfirm } = useConfirmDialog();
const { isRefreshing, handleRefresh: refreshData } = useRefreshHandler();

const { addNotification } = useNotifications();

const tableHeaders = [
    { text: 'STT', width: '60px', align: 'center', class: 'text-no-wrap' },
    { text: 'Mã nhân viên', width: '130px', align: 'center', class: 'text-no-wrap' },
    { text: 'Tên nhân viên', width: '160px', align: 'center', class: 'text-no-wrap' },
    { text: 'Tên tài khoản', width: '180px', align: 'center', class: 'text-no-wrap' },
    { text: 'Giới tính', width: '100px', align: 'center', class: 'text-no-wrap' },
    { text: 'Số điện thoại', width: '130px', align: 'center', class: 'text-no-wrap' },
    { text: 'Địa chỉ', width: '250px', align: 'start', class: 'text-no-wrap' },
    { text: 'Chức vụ', width: '130px', align: 'center', class: 'text-no-wrap' },
    { text: 'Trạng thái', width: '140px', align: 'center', class: 'text-no-wrap' },
    { text: 'Hành động', width: '120px', align: 'center', class: 'text-no-wrap' }
];

const handleRefresh = async () => {
    await refreshData(() => handleReset());
};

const handleExport = async () => {
    try {
        const blob = await dichVuNhanVien.xuatExcelNhanVien();
        downloadFile(blob, 'danh_sach_nhan_vien.xlsx');
    } catch (error) {
        console.error('Error exporting Excel:', error);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể xuất Excel',
            color: 'error'
        });
    }
};

const confirmChangeStatus = (item) => {
    setConfirm({
        title: 'Thay đổi trạng thái',
        message: `Bạn có chắc muốn đổi trạng thái của nhân viên [${item.ten}]?`,
        color: 'warning',
        action: async () => {
            try {
                const newS = item.trangThai === SYSTEM_STATUS.ACTIVE ? SYSTEM_STATUS.INACTIVE : SYSTEM_STATUS.ACTIVE;
                await dichVuNhanVien.thayDoiTrangThaiNhanVien(item.id, newS);
                item.trangThai = newS;
            } catch (e) {
                console.error('Status change error:', e);
                addNotification({
                    title: 'Lỗi',
                    subtitle: 'Không thể thay đổi trạng thái nhân viên',
                    color: 'error'
                });
            }
        }
    });
};



const getAddressSummary = (item) => {
    if (!item) return '-';

    const ct = item.diaChiChiTiet || item.dia_chi_chi_tiet || '';
    const xa = item.phuongXa || item.phuong_xa || '';
    const huyen = item.thanhPho || item.thanh_pho || '';
    const tinh = item.tinh || '';

    const parts = [ct, xa, huyen, tinh].map(p => String(p).trim()).filter(p => p !== '' && p !== 'null');

    return parts.length > 0 ? parts.join(', ') : 'Chưa cập nhật';
};

const getIndex = (index) => {
    return (pagination.value.page - 1) * pagination.value.size + index + 1;
};

const getGenderChipClass = (gioiTinh) => {
    return ['gender-chip', gioiTinh ? 'gender-chip-male' : 'gender-chip-female'];
};

const getGenderLabel = (gioiTinh) => {
    return gioiTinh === true ? 'Nam' : gioiTinh === false ? 'Nữ' : '-';
};

const getStatusChipClass = (trangThai) => {
    return isActiveStatus(trangThai) ? 'status-chip-active' : 'status-chip-inactive';
};

const goToAdd = () => {
    router.push(PATH.NHAN_VIEN_FORM);
};

const goToEdit = (id) => {
    router.push(`${PATH.NHAN_VIEN_FORM}/${id}`);
};

const updatePaginationSize = (size) => {
    pagination.value.size = size;
};

onMounted(() => {
    loadEmployees();
});
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý tài khoản', disabled: false, href: '#' },
            { title: 'Nhân viên', disabled: true }
        ]" />

        <div class="mb-2"></div>
        <!-- 1. FILTER -->
        <div class="filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="4" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm nhân viên</div>
                    <v-text-field v-model="filters.search" placeholder="Tên, SĐT, Email..." variant="outlined"
                        bg-color="white" density="compact" hide-details prepend-inner-icon="mdi-magnify" class="compact-input"
                        @keyup.enter="handleSearch"></v-text-field>
                </v-col>
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Giới tính</div>
                    <v-select v-model="filters.gioiTinh" :items="GIOI_TINH_FILTER_OPTIONS" variant="outlined"
                        bg-color="white" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch"></v-select>
                </v-col>
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select v-model="filters.trangThai" :items="TRANG_THAI_FILTER_OPTIONS" variant="outlined"
                        bg-color="white" density="compact" hide-details class="compact-input"
                        @update:model-value="handleSearch"></v-select>
                </v-col>
            </AdminFilter>
        </div>

        <AdminTable title="Danh sách nhân viên" addButtonText="Tạo mới" show-export-button :headers="tableHeaders"
            :items="employees" :total-count="pagination.totalElements" :loading="loading"
            @add="goToAdd" @export="handleExport">

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center">{{ getIndex(index) }}</td>
                    <td class="data-cell text-center">
                        <div class="text-truncate" :title="item.ma">{{ item.ma || '-' }}</div>
                    </td>
                    <td class="data-cell text-left px-4">
                        <div class="text-slate-800 text-truncate" :title="item.ten">{{ item.ten || '-' }}</div>
                    </td>
                    <td class="data-cell text-left px-4">
                        <div class="text-slate-800 text-truncate" :title="item.tenTaiKhoan">{{ item.tenTaiKhoan || '-'
                        }}</div>
                        <div class="text-caption text-slate-500 text-truncate" :title="item.email">{{ item.email || '-'
                        }}</div>
                    </td>
                    <td class="data-cell">
                        <v-chip variant="flat" class="justify-center" :class="getGenderChipClass(item.gioiTinh)">
                            {{ getGenderLabel(item.gioiTinh) }}
                        </v-chip>
                    </td>
                    <td class="data-cell px-4">
                        <div class="d-flex align-center justify-center text-truncate text-slate-700" :title="item.sdt">
                            <v-icon size="14" class="mr-2 text-slate-400">mdi-phone</v-icon>
                            <span>{{ item.sdt || '-' }}</span>
                        </div>
                    </td>
                    <td class="data-cell text-left px-4" style="min-width: 200px;">
                        <div class="text-slate-700" style="font-size: 13px; line-height: 1.4;">
                            <span :class="{ 'text-slate-400': getAddressSummary(item) === 'Chưa cập nhật' }">
                                {{ getAddressSummary(item) }}
                            </span>
                        </div>
                    </td>

                    <td class="data-cell text-center">
                        <div class="text-truncate" :title="item.tenPhanQuyen || 'Nhân viên'">
                            {{ item.tenPhanQuyen || 'Nhân viên' }}
                        </div>
                    </td>
                    <td class="data-cell text-center">
                        <v-chip variant="flat" class="justify-center" :class="['status-chip', getStatusChipClass(item.trangThai)]">
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>

                    <td class="data-cell action-cell">
                        <div class="d-flex align-center justify-center action-controls">

                            <v-btn variant="text" class="action-icon-btn"
                                @click.stop="goToEdit(item.id)">
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <div class="switch-wrapper">
                                <v-switch :model-value="isActiveStatus(item.trangThai)" color="primary" hide-details
                                    density="compact" class="tight-switch action-switch"
                                    @click.prevent.stop="confirmChangeStatus(item)" />
                                <v-tooltip activator="parent" location="top">Chuyển đổi trạng thái</v-tooltip>
                            </div>
                        </div>
                    </td>
                </tr>
            </template>
            <template #pagination>
                <AdminPagination v-model="pagination.page" :page-size="pagination.size"
                    @update:page-size="updatePaginationSize" :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements" :current-size="employees.length"
                    @change="loadEmployees" />
            </template>
        </AdminTable>

        <!-- SHARED CONFIRM -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="handleConfirm(true)"
            @cancel="handleConfirm(false)" />
    </v-container>
</template>

<style scoped></style>
