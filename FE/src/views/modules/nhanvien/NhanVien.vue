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

const loading = ref(false);
const isRefreshing = ref(false);
const employees = ref([]);
const router = useRouter();

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
    { text: 'Giới tính', align: 'center', width: '100px' },
    { text: 'Liên hệ', align: 'left', width: '230px' },
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
        const total = response.totalElements || employees.value.length;
        pagination.value.totalElements = total;
        pagination.value.totalPages = response.totalPages || Math.ceil(total / pagination.value.size) || 1;
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

onMounted(() => loadEmployees());
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body" style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important;">
        <!-- Header -->
        <div class="mb-6">
            <h5 class="text-h5 font-weight-bold">Quản lý nhân viên</h5>
        </div>

        <!-- 1. FILTER -->
        <div class="filter-top invoice-filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="3" class="filter-cell">
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
                <v-col cols="12" md="2" class="filter-cell">
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
                <v-col cols="12" md="2" class="filter-cell">
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
                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Từ ngày</div>
                    <v-text-field
                        v-model="filters.startDate"
                        type="date"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @change="loadEmployees"
                    ></v-text-field>
                </v-col>
                <v-col cols="12" md="2" class="filter-cell">
                    <div class="filter-field-label">Đến ngày</div>
                    <v-text-field
                        v-model="filters.endDate"
                        type="date"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @change="loadEmployees"
                    ></v-text-field>
                </v-col>
            </AdminFilter>
        </div>

        <!-- 2. TABLE -->
        <AdminTable
            title="Danh sách nhân viên"
            addButtonText="Thêm nhân viên"
            show-export-button
            :headers="tableHeaders"
            :items="employees"
            :total-count="pagination.totalElements"
            :loading="loading"
            @add="router.push('/nhan-vien/form')"
            @export="handleExport"
        >
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center">
                        {{ getRowNumber(index) }}
                    </td>
                    <td class="data-cell text-left col-left-tight">{{ item.ma || '-' }}</td>
                    <td class="data-cell text-left col-left-tight">{{ item.ten || '-' }}</td>
                    <td class="data-cell text-left col-left-tight">{{ item.tenTaiKhoan || '-' }}</td>
                    <td class="data-cell text-center">
                        <v-chip
                            variant="flat"
                            size="x-small"
                            :class="[
                                'gender-chip',
                                getGenderLabel(item.gioiTinh) === 'Nam'
                                    ? 'gender-chip-male'
                                    : getGenderLabel(item.gioiTinh) === 'Nữ'
                                    ? 'gender-chip-female'
                                    : 'gender-chip-neutral'
                            ]"
                        >
                            {{ getGenderLabel(item.gioiTinh) }}
                        </v-chip>
                    </td>
                    <td class="data-cell">
                        <div class="contact-line mb-1">{{ item.sdt || '-' }}</div>
                        <div class="contact-line d-flex align-center">
                            <v-icon size="14" class="mr-1">mdi-email-outline</v-icon> {{ item.email || '-' }}
                        </div>
                    </td>
                    <td class="data-cell text-left">{{ item.tenPhanQuyen || 'Nhân viên' }}</td>
                    <td class="data-cell">
                        <v-chip :class="['px-3 status-chip', getStatusChipClass(item.trangThai)]" variant="flat">
                            {{ isActiveStatus(item.trangThai) ? 'Hoạt động' : 'Ngừng hoạt động' }}
                        </v-chip>
                    </td>
                    <td class="data-cell text-center">{{ formatDateTime(item.ngayTao) }}</td>
                    <td class="data-cell action-cell" style="text-align: center">
                        <div class="d-flex align-center justify-center action-controls">
                            <v-btn
                                icon
                                variant="text"
                                size="28"
                                color="slate-700"
                                class="rounded-lg action-icon-btn"
                                @click.stop="router.push(`/nhan-vien/form/${item.id}`)"
                            >
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <div class="switch-wrapper">
                                <v-switch
                                    :model-value="isActiveStatus(item.trangThai)"
                                    color="slate-700"
                                    hide-details
                                    density="compact"
                                    class="tight-switch action-switch"
                                    @click.prevent.stop="confirmChangeStatus(item)"
                                />
                                <v-tooltip activator="parent" location="top">Chuyển đổi trạng thái</v-tooltip>
                            </div>
                        </div>
                    </td>
                </tr>
            </template>
            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    v-model:page-size="pagination.size"
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
            @confirm="confirmDialog.action"
        />
    </v-container>
</template>

<style scoped>
.gray-bg { /* Removed background */ }
.text-dark {
    color: #000000 !important;
}
.font-body {
    font-family: 'Inter', sans-serif;
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

:deep(.filter-top .filter-grid) {
    align-items: flex-start !important;
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
    text-align: left !important;
    padding-left: 6px !important;
}

:deep(.native-admin-table .data-cell) {
    font-size: 13px !important;
    font-weight: 600 !important;
    text-align: left !important;
    padding-left: 6px !important;
}

:deep(.native-admin-table .header-cell:nth-child(1)),
:deep(.native-admin-table .data-cell:nth-child(1)),
:deep(.native-admin-table .header-cell:nth-child(10)),
:deep(.native-admin-table .data-cell:nth-child(10)) {
    text-align: center !important;
    padding-left: 0 !important;
}

:deep(.native-admin-table .data-cell.col-left-tight) {
    text-align: left !important;
    padding-left: 6px !important;
}

.contact-line {
    font-size: 13px;
    font-weight: 600;
    line-height: 1.35;
    color: #1e293b;
}

:deep(.status-chip) {
    border-radius: 999px !important;
    border: 1px solid transparent !important;
    box-shadow: none !important;
    font-size: 13px !important;
    min-height: 28px !important;
}

:deep(.status-chip.status-chip-active) {
    background: #f2f7ff !important;
    color: #4e73ad !important;
    border: 1px solid #dbe7fb !important;
}

:deep(.status-chip.status-chip-inactive) {
    background: #fff1f2 !important;
    color: #b77986 !important;
    border: 1px solid #f8dde1 !important;
}

:deep(.status-chip .v-chip__content) {
    font-size: 13px !important;
    font-weight: 600 !important;
    line-height: 1.1 !important;
}

.gender-chip {
    min-width: 34px;
    height: 24px !important;
    border-radius: 999px !important;
    padding: 0 8px !important;
    letter-spacing: 0;
}

.gender-chip-male {
    background: #eef4ff;
    color: #4a6fae;
}

.gender-chip-female {
    background: #fff3e8;
    color: #b97745;
}

.gender-chip-neutral {
    background: #f1f5f9;
    color: #64748b;
}

:deep(.gender-chip .v-chip__content) {
    font-size: 13px !important;
    font-weight: 600 !important;
    line-height: 1.1 !important;
}

.action-controls {
    gap: 6px;
}

:deep(.action-cell .action-icon-btn) {
    background: transparent !important;
    border: none !important;
    outline: none !important;
    box-shadow: none !important;
    min-width: 28px !important;
    width: 28px !important;
    height: 28px !important;
    padding: 0 !important;
}

:deep(.action-cell .action-icon-btn .v-btn__overlay),
:deep(.action-cell .action-icon-btn .v-btn__underlay),
:deep(.action-cell .action-icon-btn .v-ripple__container) {
    display: none !important;
}

.switch-wrapper {
    display: inline-flex;
    align-items: center;
    justify-content: center;
}

:deep(.action-cell .action-switch .v-switch__track) {
    background: #ffffff !important;
    border: 1px solid #cbd5e1 !important;
    opacity: 1 !important;
    min-height: 18px !important;
    max-height: 18px !important;
    width: 32px !important;
}

:deep(.action-cell .action-switch .v-selection-control--dirty .v-switch__track) {
    background: #d9e6fb !important;
    border-color: #d9e6fb !important;
}

:deep(.action-cell .action-switch .v-switch__thumb) {
    background: #94a3b8 !important;
    width: 14px !important;
    height: 14px !important;
    box-shadow: none !important;
}

:deep(.action-cell .action-switch .v-selection-control--dirty .v-switch__thumb) {
    background: #1e3a8a !important;
}

.tight-switch {
    transform: none;
    width: 36px;
}
</style>



