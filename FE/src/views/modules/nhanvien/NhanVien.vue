<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';
import { PATH } from '@/router/routePaths';
import { formatDateTime } from '@/utils/formatters';
import { getStatusLabel, isActiveStatus } from '@/utils/statusUtils';
import { SYSTEM_STATUS } from '@/constants/statusConstants';

// REUSABLE COMPONENTS
import { AdminFilter, AdminTable, AdminPagination, AdminConfirm, AdminBreadcrumbs } from '@/components/common';
import { downloadFile } from '@/utils/fileUtils';

import { useNotifications } from '@/services/notificationService';
import { useAuthStore } from '@/stores/authStore';
import { ADMIN_ICONS } from '@/constants/adminIcons';

import { useAdminTable } from '@/composables/useAdminTable';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useRefreshHandler } from '@/composables/useRefreshHandler';

import { GIOI_TINH_FILTER_OPTIONS, isManagementRole } from '@/constants/appConstants';
import { TRANG_THAI_FILTER_OPTIONS, NHAN_VIEN_MESSAGES } from '@/constants/nhanVienConstants';

const {
    items: employees,
    loading,
    pagination,
    filters,
    loadData: loadEmployees,
    handleFilter,
    handleReset,
} = useAdminTable(
    async (params) => {
        const payload = { ...params };
        if (payload.search) {
            payload.keyword = payload.search;
        }
        // Remove custom filters from params before sending to API if needed
        // Or transform them as required by the API
        return dichVuNhanVien.layNhanVienPhanTrang(payload);
    },
    { search: '', gioiTinh: null, trangThai: null }
);

// Override handleReset to preserve custom filters
const originalHandleReset = handleReset;
const customHandleReset = () => {
    filters.value = {
        search: '',
        gioiTinh: null,
        trangThai: null
    };
    pagination.value.page = 1;
    loadEmployees();
};

const router = useRouter();
const { confirmDialog, setConfirm, clearConfirm, handleConfirm } = useConfirmDialog();
const { isRefreshing, handleRefresh: refreshData } = useRefreshHandler();

const { addNotification } = useNotifications();

const tableHeaders = [
    { text: 'STT', width: '50px', align: 'center', class: 'text-no-wrap' },
    { text: 'Mã NV', width: '100px', align: 'center', class: 'text-no-wrap' },
    { text: 'Tên nhân viên', width: '190px', align: 'start', class: 'text-no-wrap' },
    { text: 'Tài khoản / Email', width: '230px', align: 'start', class: 'text-no-wrap' },
    { text: 'Giới tính', width: '90px', align: 'center', class: 'text-no-wrap' },
    { text: 'Số điện thoại', width: '130px', align: 'center', class: 'text-no-wrap' },
    { text: 'Địa chỉ', width: '240px', align: 'start', class: 'text-no-wrap' },
    { text: 'Chức vụ', width: '110px', align: 'center', class: 'text-no-wrap' },
    { text: 'Trạng thái', width: '130px', align: 'center', class: 'text-no-wrap' },
    { text: 'Hành động', width: '110px', align: 'center', class: 'text-no-wrap' }
];

const handleRefresh = async () => {
    await refreshData(async () => {
        handleReset();
    });
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

const authStore = useAuthStore();
const currentUserInfo = ref(authStore.userProfile || null);

const fetchCurrentUser = async () => {
    try {
        if (!currentUserInfo.value) {
            const info = await dichVuNhanVien.layThongTinCaNhan();
            currentUserInfo.value = info;
        }
    } catch (e) {
        console.error('Error fetching current user info:', e);
    }
};

const isSelf = (item) => {
    if (!item) return false;
    const current = currentUserInfo.value || authStore.userProfile;
    if (current?.id && item.id === current.id) return true;
    if (current?.tenTaiKhoan && item.tenTaiKhoan === current.tenTaiKhoan) return true;
    if (authStore.user?.username && (item.tenTaiKhoan === authStore.user.username || item.email === authStore.user.username)) return true;
    return false;
};

const isAdminEmployee = (item) => {
    return isManagementRole(item);
};

const canChangeStatus = (item) => {
    return !isSelf(item) && !isAdminEmployee(item);
};

const getStatusTooltipText = (item) => {
    if (isSelf(item)) {
        return NHAN_VIEN_MESSAGES.CANNOT_CHANGE_OWN_STATUS;
    }
    if (isAdminEmployee(item)) {
        return NHAN_VIEN_MESSAGES.CANNOT_CHANGE_OTHER_ADMIN_STATUS;
    }
    return isActiveStatus(item.trangThai) ? 'Chuyển sang ngừng hoạt động' : 'Chuyển sang đang hoạt động';
};

const confirmChangeStatus = (item) => {
    if (isSelf(item)) {
        addNotification({
            title: 'Cảnh báo',
            subtitle: NHAN_VIEN_MESSAGES.CANNOT_CHANGE_OWN_STATUS,
            color: 'warning'
        });
        return;
    }

    if (isAdminEmployee(item)) {
        addNotification({
            title: 'Cảnh báo',
            subtitle: NHAN_VIEN_MESSAGES.CANNOT_CHANGE_OTHER_ADMIN_STATUS,
            color: 'warning'
        });
        return;
    }

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
                const errMsg = e.response?.data?.message || 'Không thể thay đổi trạng thái nhân viên';
                addNotification({
                    title: 'Lỗi',
                    subtitle: errMsg,
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

    const parts = [ct, xa, huyen, tinh].map((p) => String(p).trim()).filter((p) => p !== '' && p !== 'null');

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
    pagination.value.page = 1;
};

onMounted(() => {
    fetchCurrentUser();
    loadEmployees();
});
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs
            :items="[
                { title: 'Quản lý tài khoản', disabled: false, href: '#' },
                { title: 'Nhân viên', disabled: true }
            ]"
        />

        <div class="mb-2"></div>
        <!-- 1. FILTER -->
        <div class="filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" sm="5" md="4" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field
                        v-model="filters.search"
                        placeholder="Tên, SĐT, Email..."
                        variant="outlined"
                        bg-color="white"
                        density="compact"
                        hide-details
                        clearable
                        class="compact-input"
                        prepend-inner-icon="mdi-magnify"
                        @update:model-value="handleFilter"
                    ></v-text-field>
                </v-col>
                <v-col cols="12" sm="3" md="3" class="filter-cell">
                    <div class="filter-field-label">Giới tính</div>
                    <v-select
                        v-model="filters.gioiTinh"
                        :items="GIOI_TINH_FILTER_OPTIONS"
                        variant="outlined"
                        bg-color="white"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @update:model-value="handleFilter"
                    ></v-select>
                </v-col>
                <v-col cols="12" sm="3" md="3" class="filter-cell">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select
                        v-model="filters.trangThai"
                        :items="TRANG_THAI_FILTER_OPTIONS"
                        variant="outlined"
                        bg-color="white"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @update:model-value="handleFilter"
                    ></v-select>
                </v-col>
            </AdminFilter>
        </div>

        <AdminTable
            title="Danh sách nhân viên"
            addButtonText="Tạo mới"
            show-export-button
            :headers="tableHeaders"
            :items="employees"
            :total-count="pagination.totalElements"
            :loading="loading"
            @add="goToAdd"
            @export="handleExport"
        >
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center">{{ getIndex(index) }}</td>
                    <td class="data-cell text-center font-weight-medium">
                        <div class="text-no-wrap" :title="item.ma">{{ item.ma || '-' }}</div>
                    </td>
                    <td class="data-cell text-left px-4">
                        <div class="text-slate-800 font-weight-medium text-no-wrap" :title="item.ten">{{ item.ten || '-' }}</div>
                    </td>
                    <td class="data-cell text-left px-4">
                        <div class="text-slate-800 font-weight-medium text-no-wrap" :title="item.tenTaiKhoan">{{ item.tenTaiKhoan || '-' }}</div>
                        <div class="text-caption text-slate-500 text-no-wrap" :title="item.email">{{ item.email || '-' }}</div>
                    </td>
                    <td class="data-cell text-center">
                        <v-chip variant="flat" class="justify-center" :class="getGenderChipClass(item.gioiTinh)">
                            {{ getGenderLabel(item.gioiTinh) }}
                        </v-chip>
                    </td>
                    <td class="data-cell px-4 text-center">
                        <div class="d-flex align-center justify-center text-no-wrap text-slate-700" :title="item.sdt">
                            <v-icon size="14" class="mr-2 text-slate-400">mdi-phone</v-icon>
                            <span>{{ item.sdt || '-' }}</span>
                        </div>
                    </td>
                    <td class="data-cell text-left px-4" style="min-width: 220px">
                        <div class="text-slate-700" style="font-size: 13px; line-height: 1.4">
                            <span :class="{ 'text-slate-400': getAddressSummary(item) === 'Chưa cập nhật' }">
                                {{ getAddressSummary(item) }}
                            </span>
                        </div>
                    </td>

                    <td class="data-cell text-center">
                        <div class="text-no-wrap" :title="item.tenPhanQuyen || 'Nhân viên'">
                            {{ item.tenPhanQuyen || 'Nhân viên' }}
                        </div>
                    </td>
                    <td class="data-cell text-center">
                        <v-chip variant="flat" class="justify-center" :class="['status-chip', getStatusChipClass(item.trangThai)]">
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>

                    <td class="data-cell action-cell text-center">
                        <div class="d-flex align-center justify-center action-controls">
                            <v-btn variant="text" class="action-icon-btn" color="primary" @click.stop="goToEdit(item.id)">
                                <component :is="ADMIN_ICONS.ACTION.EDIT" size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <div class="switch-wrapper">
                                <v-switch
                                    :model-value="isActiveStatus(item.trangThai)"
                                    color="primary"
                                    hide-details
                                    density="compact"
                                    class="tight-switch action-switch"
                                    :disabled="!canChangeStatus(item)"
                                    @click.prevent.stop="confirmChangeStatus(item)"
                                />
                                <v-tooltip activator="parent" location="top">{{ getStatusTooltipText(item) }}</v-tooltip>
                            </div>
                        </div>
                    </td>
                </tr>
            </template>
            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    :page-size="pagination.size"
                    @update:page-size="updatePaginationSize"
                    :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements"
                    :current-size="employees.length"
                    @change="loadEmployees"
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
:deep(.v-field),
:deep(.v-field__outline) {
    border-radius: 12px !important;
}

:deep(.v-field__input),
:deep(input),
:deep(input::placeholder),
:deep(textarea),
:deep(.v-select__selection-text),
:deep(.v-label) {
    font-size: 13px !important;
}

:deep(.v-field__input::placeholder) {
    font-size: 13px !important;
}

.filter-field-label,
.field-label,
.field-label-small {
    font-size: 13px !important;
}

:global(.v-overlay-container .v-list-item-title),
:global(.v-overlay-container .v-list-item) {
    font-size: 13px !important;
}
</style>
