<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EditIcon } from 'vue-tabler-icons';

import { dichVuResetPassword } from '@/services/admin/dichVuResetPassword';
import { useNotifications } from '@/services/notificationService';

const loading = ref(false);
const isRefreshing = ref(false);
const employees = ref([]);
const router = useRouter();

const tab = ref(0); // 0: danh sách nhân viên, 1: reset mật khẩu
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

const pagination = ref({ page: 1, size: 5, totalElements: 0, totalPages: 1 });
const filters = ref({
    keyword: '',
    trangThai: null,
    gioiTinh: null,
    startDate: null,
    endDate: null
});

// Confirmation Logic
const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    color: 'primary',
    action: null,
    loading: false
});

const tableHeaders = [
    { text: 'STT', align: 'center', width: '60px' },
    { text: 'Mã nhân viên', align: 'left', width: '120px' },
    { text: 'Tên nhân viên', align: 'left', width: '140px' },
    { text: 'Tên tài khoản', align: 'left', width: '120px' },
    { text: 'Giới tính', align: 'center', width: '130px' },
    { text: 'Thông tin liên hệ', align: 'center', width: '230px' },
    { text: 'Chức vụ', align: 'left', width: '100px' },
    { text: 'Trạng thái', align: 'center', width: '130px' },
    { text: 'Ngày tạo', align: 'center', width: '120px' },
    { text: 'Hành động', align: 'center', width: '110px' }
];

const getRowNumber = (index) => (pagination.value.page - 1) * pagination.value.size + index + 1;

const formatDateTime = (value) => {
    if (value === null || value === undefined || value === '') return '-';

    const numericValue = Number(value);
    if (!Number.isNaN(numericValue)) {
        const timestamp = numericValue < 1000000000000 ? numericValue * 1000 : numericValue;
        const date = new Date(timestamp);
        if (Number.isNaN(date.getTime())) return '-';

        const pad = (num) => String(num).padStart(2, '0');
        return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    const pad = (num) => String(num).padStart(2, '0');
    return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const getGenderLabel = (value) => {
    if (value === true || value === 'true' || value === 1 || value === '1') return 'Nam';
    if (value === false || value === 'false' || value === 0 || value === '0') return 'Nữ';
    return '-';
};

const isActiveStatus = (status) => {
    if (status === null || status === undefined) return false;
    if (typeof status === 'number') return status === 0;
    const normalized = String(status).toUpperCase();
    return normalized === 'DANG_HOAT_DONG' || normalized === 'ACTIVE' || normalized === 'HOAT_DONG' || normalized === '0';
};

const getStatusChipClass = (status) => (isActiveStatus(status) ? 'status-chip-active' : 'status-chip-inactive');

const loadEmployees = async () => {
    loading.value = true;
    try {
        const params = {
            page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
            size: pagination.value.size,
            keyword: filters.value.keyword || null,
            trangThai: filters.value.trangThai || null,
            gioiTinh: filters.value.gioiTinh !== null ? filters.value.gioiTinh : null
        };
        const response = await dichVuNhanVien.layNhanVienPhanTrang(params);
        employees.value = response.content || response;
        pagination.value.totalElements = response.totalElements || employees.value.length;
        pagination.value.totalPages = response.totalPages || 1;
    } catch (error) {
        console.error(error);
    } finally {
        loading.value = false;
    }
};

const handleRefresh = async () => {
    isRefreshing.value = true;
    filters.value = { keyword: '', trangThai: null, gioiTinh: null };
    pagination.value.page = 1;
    await loadEmployees();
    setTimeout(() => (isRefreshing.value = false), 800);
};

const handleExport = async () => {
    try {
        const blob = await dichVuNhanVien.xuatExcelNhanVien();
        downloadFile(blob, 'danh_sach_nhan_vien.xlsx');
    } catch (error) {
        console.error('Lỗi xuất Excel:', error);
    }
};

const confirmChangeStatus = (item) => {
    confirmDialog.value = {
        show: true,
        title: 'Thay đổi trạng thái',
        message: `Bạn có chắc muốn đổi trạng thái của nhân viên [${item.ten}]?`,
        color: 'warning',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                const newS = item.trangThai === 'DANG_HOAT_DONG' ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
                await dichVuNhanVien.thayDoiTrangThaiNhanVien(item.id, newS);
                item.trangThai = newS;
                confirmDialog.value.show = false;
            } catch (e) {
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

onMounted(() => {
    loadEmployees();
    loadPendingRequests();
});
</script>

<template>
    <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
        <!-- Header -->
        <div class="mb-6">
            <h5 class="text-h5 font-weight-bold">Quản lí nhân viên</h5>
        </div>
        <!-- 1. FILTER -->
        <div class="invoice-filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="4" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm nhân viên</div>
                    <v-text-field
                        v-model="filters.keyword"
                        placeholder="Tên, SĐT, Email..."
                        variant="outlined"
                        density="compact"
                        hide-details
                        prepend-inner-icon="mdi-magnify"
                        class="compact-input"
                        @keyup.enter="loadEmployees"
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
                        @update:model-value="loadEmployees"
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
                        @update:model-value="loadEmployees"
                    ></v-select>
                </v-col>
            </AdminFilter>
        </div>

        <AdminTable
            :title="tab === 0 ? 'Danh sách nhân viên' : 'Yêu cầu tạo mới mật khẩu'"
            :addButtonText="tab === 0 ? 'Thêm nhân viên' : 'Thêm nhân viên'"
            :hide-add-button="tab === 1"
            show-export-button
            :headers="tableHeaders"
            :items="tab === 0 ? employees : pendingRequests"
            :total-count="pagination.totalElements"
            :loading="loading"
            @add="router.push('/nhan-vien/form')"
            @export="handleExport"
        >
            <template #top>
                <v-tabs v-model="tab" bg-color="transparent" color="#1e3a8a" height="54" align-tabs="start">
                    <v-tab :value="0" class="text-none font-weight-bold px-4 tab-item">
                        <v-icon start size="16">mdi-view-grid-outline</v-icon>
                        Danh sách nhân viên
                        <v-chip
                            v-if="employees.length > 0"
                            size="x-small"
                            :class="['ml-2 font-weight-bold tab-count-chip', tab === 0 ? 'active-chip' : 'inactive-chip']"
                            variant="flat"
                        >
                            {{ employees.length }}
                        </v-chip>
                    </v-tab>

                    <v-tab :value="1" class="text-none font-weight-bold px-4 tab-item">
                        <v-icon start size="16">mdi-clock-outline</v-icon>
                        Yêu cầu tạo mới mật khẩu
                        <v-chip
                            v-if="pendingRequests.length > 0"
                            size="x-small"
                            :class="['ml-2 font-weight-bold tab-count-chip', tab === 1 ? 'active-chip' : 'inactive-chip']"
                            variant="flat"
                        >
                            {{ pendingRequests.length }}
                        </v-chip>
                    </v-tab>
                </v-tabs>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center">{{ index + 1 }}</td>
                    <td class="data-cell text-left">{{ item.ma || '-' }}</td>
                    <td class="data-cell text-left">{{ item.ten || '-' }}</td>
                    <td class="data-cell text-left">{{ item.tenTaiKhoan || '-' }}</td>
                    <td class="data-cell text-center">
                        <v-chip
                            size="x-small"
                            variant="flat"
                            :class="['gender-chip', item.gioiTinh ? 'gender-chip-male' : 'gender-chip-female']"
                        >
                            {{ item.gioiTinh === true ? 'Nam' : 'Nữ' }}
                        </v-chip>
                    </td>

                    <td class="data-cell text-left pl-8 col-lien-he">
                        <div class="contact-line mb-1 font-weight-bold">
                            {{ item.sdt || '-' }}
                        </div>
                        <div class="contact-line d-flex align-center">
                            <v-icon size="14" class="mr-2">mdi-email-outline</v-icon>
                            <span>{{ item.email || '-' }}</span>
                        </div>
                    </td>

                    <td class="data-cell text-left">{{ item.tenPhanQuyen || 'Nhân viên' }}</td>

                    <td class="data-cell text-center">
                        <template v-if="tab === 0">
                            <v-chip
                                size="small"
                                variant="flat"
                                :class="[
                                    'status-chip',
                                    item.trangThai === 'DANG_HOAT_DONG' ? 'status-chip-active' : 'status-chip-inactive'
                                ]"
                            >
                                {{ item.trangThai === 'DANG_HOAT_DONG' ? 'Hoạt động' : 'Ngừng hoạt động' }}
                            </v-chip>
                        </template>
                        <template v-else>
                            <span class="text-caption font-weight-bold text-primary">{{ formatDateTime(item.resetRequestedAt) }}</span>
                        </template>
                    </td>

                    <td class="data-cell text-center">{{ formatDateTime(item.ngayTao) }}</td>

                    <td class="data-cell text-center">
                        <div v-if="tab === 0" class="d-flex align-center justify-center">
                            <v-btn icon variant="text" size="28" color="#5f6f82" @click.stop="router.push(`/nhan-vien/form/${item.id}`)">
                                <EditIcon size="15" />
                            </v-btn>
                            <v-switch
                                :model-value="item.trangThai === 'DANG_HOAT_DONG'"
                                color="#1e3a8a"
                                hide-details
                                density="compact"
                                class="ml-2"
                                @click.prevent.stop="confirmChangeStatus(item)"
                            />
                        </div>
                        <v-btn v-else color="primary" size="small" variant="flat" class="text-none" @click="handleResetPassword(item.id)">
                            Reset Pass
                        </v-btn>
                    </td>
                </tr>
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
:deep(.native-admin-table .header-cell:nth-child(6)) {
    text-align: left !important;
    padding-left: 32px !important;
}

:deep(.v-tab) {
    color: black !important; /* Màu xám Slate */
    text-transform: none !important;
    font-size: 14px !important;
    font-weight: 600 !important;
    letter-spacing: 0 !important;
    opacity: 1 !important;
}
:deep(.v-tab--selected) {
    color: #1e3a8a !important; /* Navy Blue đậm (giống hóa đơn) */
    background: transparent !important;
}
:deep(.v-tab .tab-count-chip),
:deep(.v-tab .v-chip) {
    color: #ffffff !important;
    font-weight: 800 !important;
    font-size: 11px !important;
    min-height: 20px !important;
    min-width: 22px !important;
    padding: 0 6px !important;
    border-radius: 10px !important;
    margin-left: 8px !important;
    opacity: 1 !important;
    transition: all 0.3s ease; /* Hiệu ứng đổi màu mượt */
}
:deep(.tab-count-chip) {
    font-size: 11px !important;
    min-height: 20px !important;
    min-width: 22px !important;
    padding: 0 6px !important;
    border-radius: 10px !important;
    transition: all 0.3s ease; /* Hiệu ứng đổi màu mượt */
}

/* 2. Màu ĐỎ khi tab KHÔNG được chọn */
.inactive-chip {
    background-color: #ff4d4f !important;
}

/* 3. Màu XANH NAVY khi tab ĐƯỢC chọn */
.active-chip {
    background-color: #1e3a8a !important;
}

/* 4. Đảm bảo chữ của Tab được chọn cũng là Navy đậm */
:deep(.v-tab--selected) {
    color: #1e3a8a !important;
}
:deep(.native-admin-table .header-cell:nth-child(6)),
.col-lien-he {
    text-align: left !important;
    padding-left: 32px !important;
}
.col-lien-he {
    text-align: left !important;
    padding-left: 32px !important;
}
.col-lien-he .contact-line {
    justify-content: flex-start !important;
    display: flex;
    align-items: center;
}
.contact-cell {
    text-align: left !important;
    padding-left: 16px !important;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
}
.contact-cell-inner {
    text-align: left !important;
    justify-content: flex-start !important;
    display: flex;
    align-items: center;
}

.gray-bg {
    background-color: #f5f7fb;
}
.text-dark {
    color: #000000 !important;
}
.font-body {
    font-family: 'Inter', sans-serif;
}
.tab-item {
    min-height: 54px;
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

.center-cell {
    text-align: center;
    vertical-align: middle;
}
.gender-chip {
    min-width: 34px;
    height: 24px !important;
    border-radius: 999px !important;
    font-size: 13px !important;
    font-weight: 700;
    padding: 0 8px !important;
}
.gender-chip-male {
    background: #eef4ff;
    color: #4a6fae;
}
.gender-chip-female {
    background: #fff3e8;
    color: #b97745;
}

.status-chip {
    border-radius: 999px !important;
    font-size: 13px !important;
    font-weight: 600;
    padding: 0 12px !important;
    min-height: 28px !important;
}
.status-chip-active {
    background: #f2f7ff;
    color: #4e73ad;
    border: 1px solid #dbe7fb;
}
.status-chip-inactive {
    background: #fff1f2;
    color: #b77986;
    border: 1px solid #f8dde1;
}
.gender-chip,
.status-chip {
    margin: 0 auto !important;
    display: flex !important;
    justify-content: center;
    align-items: center;
}
:deep(.data-cell .v-chip) {
    display: block;
    width: fit-content;
    margin: 0 auto;
}

:deep(.filter-top .filter-grid) {
    align-items: flex-start !important;
}
.filter-top .filter-grid {
    align-items: flex-end !important;
    margin-left: 0 !important;
    margin-right: 0 !important;
}
.filter-cell {
    padding-left: 8px !important;
    padding-right: 8px !important;
    margin-bottom: 0 !important;
}
.filter-reset-col {
    display: flex !important;
    align-items: flex-end !important;
    justify-content: flex-end !important;
    padding-bottom: 0 !important;
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
:deep(.data-cell:not(.col-lien-he) > div) {
    justify-content: center !important;
}
:deep(.v-slide-group__content) {
    justify-content: flex-start !important;
}
:deep(.filter-top .v-field input),
:deep(.filter-top .v-field__input),
:deep(.filter-top .v-select__selection),
:deep(.filter-top .v-select__selection-text),
:deep(.filter-top .v-field__input::placeholder) {
    font-size: 13px !important;
}

:deep(.filter-top .v-field-label) {
    font-size: 13px !important;
}

:deep(.admin-table-container .table-toolbar h3) {
    font-size: 19px !important;
}

:deep(.native-admin-table .header-cell) {
    font-size: 13px !important;
    font-weight: 700 !important;
    text-align: center !important; /* Đổi từ left sang center */
    padding: 12px 8px !important;
}

:deep(.native-admin-table .data-cell) {
    font-size: 13px !important;
    font-weight: 600 !important;
    text-align: center !important; /* Đảm bảo tất cả là center */
    padding: 12px 8px !important;
}
:deep(.native-admin-table .header-cell),
:deep(.native-admin-table .data-cell) {
    padding-left: 0 !important;
}
:deep(.data-cell > div) {
    justify-content: center !important;
}
:deep(.native-admin-table .data-cell.text-right) {
    text-align: left !important;
    padding-right: 20px !important; /* Tạo khoảng trống nhỏ với đường kẻ bảng */
}

/* Đảm bảo nội dung bên trong div cũng dồn về phải */
.justify-end {
    justify-content: flex-end !important;
}

:deep(.native-admin-table .header-cell:nth-child(1)),
:deep(.native-admin-table .data-cell:nth-child(1)),
:deep(.native-admin-table .header-cell:nth-child(10)),
:deep(.native-admin-table .data-cell:nth-child(10)) {
    text-align: center !important;
    padding-left: 0 !important;
}
</style>
