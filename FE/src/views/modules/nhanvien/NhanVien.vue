<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';
import { PATH } from '@/router/routePaths';
import { formatDateTime } from '@/utils/formatters';
import { isActiveStatus, getStatusLabel, getStatusColor } from '@/utils/statusUtils';

// REUSABLE COMPONENTS
import { AdminFilter, AdminTable, AdminPagination, AdminConfirm, AdminBreadcrumbs } from '@/components/common';
import { downloadFile } from '@/utils/fileUtils';
import { EditIcon, RefreshIcon } from 'vue-tabler-icons';

import { dichVuResetPassword } from '@/services/admin/dichVuResetPassword';
import { useNotifications } from '@/services/notificationService';

import { useAdminTable } from '@/composables/useAdminTable';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useRefreshHandler } from '@/composables/useRefreshHandler';

const {
    items: employees,
    loading,
    pagination,
    filters,
    loadData: loadEmployees,
    handleFilter: handleSearch,
    handleReset
} = useAdminTable(dichVuNhanVien.layNhanVienPhanTrang, { search: '', gioiTinh: null, trangThai: null });

// Debug dữ liệu
import { watch } from 'vue';
watch(
    employees,
    (newVal) => {
        if (newVal && newVal.length > 0) {
            console.log('Dữ liệu nhân viên mẫu:', newVal[0]);
        }
    },
    { deep: true }
);

const router = useRouter();
const { confirmDialog, setConfirm, clearConfirm, handleConfirm } = useConfirmDialog();
const { isRefreshing, handleRefresh: refreshData } = useRefreshHandler();

const tab = ref(0);
const pendingRequests = ref([]);
const resetLoading = ref(false);
const { addNotification } = useNotifications();

async function loadPendingRequests() {
    resetLoading.value = true;
    try {
        pendingRequests.value = await dichVuResetPassword.getPendingRequests();
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: 'Không tải được danh sách yêu cầu reset', color: 'error' });
    } finally {
        resetLoading.value = false;
    }
}

async function handleResetPassword(id) {
    resetLoading.value = true;
    try {
        await dichVuResetPassword.approveReset(id);
        addNotification({ title: 'Thành công', subtitle: 'Đã reset và gửi email cho nhân viên', color: 'success' });
        await loadPendingRequests();
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: 'Reset mật khẩu thất bại', color: 'error' });
    } finally {
        resetLoading.value = false;
    }
}

const tableHeaders = [
    { text: 'STT', width: '60px', align: 'center' },
    { text: 'Mã nhân viên', width: '100px', align: 'center' },
    { text: 'Tên nhân viên', width: '130px', align: 'center' },
    { text: 'Tên tài khoản', width: '100px', align: 'center' },
    { text: 'Giới tính', width: '120px', align: 'center' },
    { text: 'Thông tin liên hệ', width: '230px', align: 'start' },
    { text: 'Địa chỉ', width: '200px', align: 'start' },
    { text: 'Chức vụ', width: '120px', align: 'center' },
    { text: 'Trạng thái', width: '130px', align: 'center' },
    { text: 'Hành động', width: '130px', align: 'center' }
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
                const newS = item.trangThai === 'DANG_HOAT_DONG' ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
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

const confirmResetPassword = (item) => {
    setConfirm({
        title: 'Reset mật khẩu',
        message: `Bạn có chắc muốn reset mật khẩu cho nhân viên [${item.ten}]? Một mật khẩu mới sẽ được tạo và gửi qua email của họ.`,
        color: 'warning',
        action: () => handleResetPassword(item.id)
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

onMounted(() => {
    loadEmployees();
    loadPendingRequests();
});
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
                { title: 'Quản lý tài khoản', disabled: false, href: '#' },
                { title: 'Nhân viên', disabled: true }
            ]"
        />

        <div class="mb-2"></div>
        <!-- 1. FILTER -->
        <div class="invoice-filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="4" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm nhân viên</div>
                    <v-text-field
                        v-model="filters.search"
                        placeholder="Tên, SĐT, Email..."
                        variant="outlined"
                        density="compact"
                        hide-details
                        prepend-inner-icon="mdi-magnify"
                        class="compact-input"
                        @keyup.enter="handleSearch"
                    ></v-text-field>
                </v-col>
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Giới tính</div>
                    <v-select
                        v-model="filters.gioiTinh"
                        :items="[
                            { title: 'Tất cả', value: null },
                            { title: 'Nam', value: true },
                            { title: 'Nữ', value: false }
                        ]"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @update:model-value="handleSearch"
                    ></v-select>
                </v-col>
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select
                        v-model="filters.trangThai"
                        :items="[
                            { title: 'Tất cả trạng thái', value: null },
                            { title: 'Đang làm việc', value: 'DANG_HOAT_DONG' },
                            { title: 'Đã nghỉ việc', value: 'KHONG_HOAT_DONG' }
                        ]"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @update:model-value="handleSearch"
                    ></v-select>
                </v-col>
            </AdminFilter>
        </div>

        <AdminTable
            :title="tab === 0 ? 'Danh sách nhân viên' : 'Yêu cầu tạo mới mật khẩu'"
            :addButtonText="tab === 0 ? 'Tạo mới' : 'Tạo mới'"
            :hide-add-button="tab === 1"
            show-export-button
            :headers="tableHeaders"
            :items="tab === 0 ? employees : pendingRequests"
            :total-count="pagination.totalElements"
            :loading="loading"
            @add="router.push(PATH.NHAN_VIEN_FORM)"
            @export="handleExport"
        >
            <template #top>
                <v-tabs v-model="tab" bg-color="transparent" color="primary" height="54" align-tabs="start" class="admin-tabs">
                    <v-tab :value="0" class="text-none font-weight-bold px-4 tab-item">
                        <v-icon start size="16">mdi-view-grid-outline</v-icon>
                        Danh sách nhân viên
                    </v-tab>

                    <v-tab :value="1" class="text-none font-weight-bold px-4 tab-item">
                        <v-icon start size="16">mdi-clock-outline</v-icon>
                        Yêu cầu tạo mới mật khẩu
                    </v-tab>
                </v-tabs>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell">{{ (pagination.page - 1) * pagination.size + index + 1 }}</td>
                    <td class="data-cell">
                        <div class="text-truncate" :title="item.ma">{{ item.ma || '-' }}</div>
                    </td>
                    <td class="data-cell">
                        <div class="text-truncate" :title="item.ten">{{ item.ten || '-' }}</div>
                    </td>
                    <td class="data-cell">
                        <div class="text-truncate" :title="item.tenTaiKhoan">{{ item.tenTaiKhoan || '-' }}</div>
                    </td>
                    <td class="data-cell">
                        <v-chip
                            size="small"
                            variant="flat"
                            :class="['gender-chip', item.gioiTinh ? 'gender-chip-male' : 'gender-chip-female']"
                        >
                            {{ item.gioiTinh === true ? 'Nam' : item.gioiTinh === false ? 'Nữ' : '-' }}
                        </v-chip>
                    </td>

                    <td class="data-cell contact-cell px-4">
                        <div class="d-inline-flex flex-column align-start" style="width: 100%; overflow: hidden;">
                            <div class="contact-info-item d-flex align-center mb-1 text-truncate" :title="item.sdt" style="width: 100%;">
                                <v-icon size="14" class="mr-2 text-slate-400">mdi-phone</v-icon>
                                <span>{{ item.sdt }}</span>
                            </div>
                            <div class="contact-info-item d-flex align-center text-slate-500 text-truncate" :title="item.email" style="width: 100%;">
                                <v-icon size="14" class="mr-2">mdi-email-outline</v-icon>
                                <span>{{ item.email || '-' }}</span>
                            </div>
                        </div>
                    </td>
                    <td class="data-cell text-left px-4" style="min-width: 200px;">
                        <div class="text-slate-700" style="font-size: 13px; line-height: 1.4;">
                            <!-- Thử cả 2 bộ tên trường do xung đột DB -->
                            <span v-if="item.diaChiChiTiet || item.dia_chi_chi_tiet">
                                {{ item.diaChiChiTiet || item.dia_chi_chi_tiet }}, 
                            </span>
                            <span v-if="item.phuongXa || item.phuong_xa">
                                {{ item.phuongXa || item.phuong_xa }}, 
                            </span>
                            <span v-if="item.thanhPho || item.thanh_pho">
                                {{ item.thanhPho || item.thanh_pho }}, 
                            </span>
                            <span v-if="item.tinh">{{ item.tinh }}</span>
                            
                            <span v-if="!(item.diaChiChiTiet || item.dia_chi_chi_tiet) && !(item.phuongXa || item.phuong_xa) && !(item.thanhPho || item.thanh_pho) && !item.tinh" class="text-slate-400">
                                Chưa cập nhật
                            </span>
                        </div>
                    </td>

                    <td class="data-cell">
                        <div class="text-truncate" :title="item.tenPhanQuyen || 'Nhân viên'">
                            {{ item.tenPhanQuyen || 'Nhân viên' }}
                        </div>
                    </td>
                    <td class="data-cell">
                        <template v-if="tab === 0">
                            <v-chip
                                size="small"
                                variant="flat"
                                :class="[
                                    'status-chip',
                                    item.trangThai === 'DANG_HOAT_DONG' ? 'status-chip-active' : 'status-chip-inactive'
                                ]"
                            >
                                {{ getStatusLabel(item.trangThai) }}
                            </v-chip>
                        </template>
                        <template v-else>
                            <span class="text-caption text-primary">{{ formatDateTime(item.resetRequestedAt) }}</span>
                        </template>
                    </td>

                    <td class="data-cell action-cell">
                        <div v-if="tab === 0" class="d-flex align-center justify-center action-controls">
                            <v-btn variant="text" class="action-icon-btn" @click.stop="confirmResetPassword(item)">
                                <RefreshIcon size="15" />
                                <v-tooltip activator="parent" location="top">Reset mật khẩu</v-tooltip>
                            </v-btn>
                            <v-btn variant="text" class="action-icon-btn" @click.stop="router.push(`${PATH.NHAN_VIEN_FORM}/${item.id}`)">
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <div class="switch-wrapper">
                                <v-switch
                                    :model-value="isActiveStatus(item.trangThai)"
                                    color="primary"
                                    hide-details
                                    density="compact"
                                    class="tight-switch action-switch"
                                    @click.prevent.stop="confirmChangeStatus(item)"
                                />
                                <v-tooltip activator="parent" location="top">Chuyển đổi trạng thái</v-tooltip>
                            </div>
                        </div>
                        <v-btn v-else color="primary" size="small" variant="flat" class="text-none" @click="handleResetPassword(item.id)">
                            Reset Pass
                        </v-btn>
                    </td>
                </tr>
            </template>
            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    :page-size="pagination.size"
                    @update:page-size="pagination.size = $event"
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
/* 
   FORCE GLOBAL OVERRIDES FOR STAFF MODULE 
   Matching KhachHang aesthetics
*/
:deep(.gender-chip) {
    border-radius: 12px !important;
    font-size: 13px !important;
    padding: 0 16px !important;
    min-height: 28px !important;
    min-width: 80px !important;
    display: inline-flex !important;
    align-items: center !important;
    justify-content: center !important;
    font-weight: 400 !important;
}
:deep(.gender-chip-male) {
    background-color: #f0f1ff !important;
    color: #1e257c !important;
    font-weight: 400 !important;
}
:deep(.gender-chip-female) {
    background-color: #fef2f2 !important;
    color: #991b1b !important;
    font-weight: 400 !important;
}
:deep(.gender-chip-male .v-chip__content) {
    color: #1e257c !important;
    font-weight: 400 !important;
}
:deep(.gender-chip-female .v-chip__content) {
    color: #991b1b !important;
    font-weight: 400 !important;
}

/* Typography & Cell Alignment */
.data-cell {
    font-size: 13px !important;
    font-family: 'Inter', 'Outfit', sans-serif !important;
    font-weight: 400 !important;
    vertical-align: middle !important;
}

.data-row {
    height: 56px !important;
}

.data-cell.font-weight-bold {
    font-weight: 400 !important;
}

.center-cell {
    text-align: center !important;
}

/* Force header centering for columns marked as align center */
:deep(.v-table th.text-center) {
    text-align: center !important;
}

/* Tab Count Badge colors */
.active-chip {
    color: rgb(var(--v-theme-primary)) !important;
}

.inactive-chip {
    color: #64748b !important;
}

:deep(.tab-count-chip .v-chip__underlay) {
    display: none !important;
}

:deep(.tab-count-chip .v-chip__content) {
    justify-content: center !important;
    width: 100% !important;
}
</style>
