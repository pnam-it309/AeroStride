<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { AdminFilter, AdminTable, AdminPagination, AdminBreadcrumbs } from '@/components/common';
import AppDatePicker from '@/components/common/AppDatePicker.vue';
import apiService from '@/services/apiService';
import { dichVuGiaoCa } from '@/services/admin/dichVuGiaoCa';
import { API_LICH_LAM_VIEC } from '@/constants/apiPaths';
import { useAdminTable } from '@/composables/useAdminTable';
import { useRefreshHandler } from '@/composables/useRefreshHandler';
import { useNotifications } from '@/services/notificationService';
import { formatDateTime, formatCurrency } from '@/utils/formatters';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

// Tab state: 'web' (Lịch sử vào web / hoạt động) hoặc 'giao-ca' (Giao ca nhân viên)
const activeTab = ref('web');

// =========================================================================
// TAB 1: LỊCH SỬ VÀO WEB & HOẠT ĐỘNG HỆ THỐNG
// =========================================================================
const {
    items: activities,
    loading: loadingActivities,
    pagination: paginationActivities,
    filters: filtersActivities,
    loadData: loadActivities,
    handleFilter: handleFilterActivities,
    handleReset: handleResetActivities
} = useAdminTable(
    async (params) => {
        try {
            const response = await apiService.get(API_LICH_LAM_VIEC.ACTIVITIES, {
                params: {
                    page: params.page,
                    size: params.size,
                    search: params.search || undefined,
                    ngay: params.ngay || undefined
                }
            });
            return response?.data?.data || response?.data || response;
        } catch (error) {
            console.error('Lỗi khi tải nhật ký hoạt động:', error);
            return { content: [], totalElements: 0, totalPages: 1 };
        }
    },
    { search: '', category: 'ALL', ngay: null }
);

const { isRefreshing: isRefreshingActivities, handleRefresh: refreshActivities } = useRefreshHandler();
const onRefreshActivities = () => {
    refreshActivities(handleResetActivities);
};

const headersActivities = [
    { text: 'STT', width: '60px', align: 'center' },
    { text: 'Thời gian', width: '170px', align: 'center' },
    { text: 'Người thực hiện', width: '180px', align: 'start' },
    { text: 'Hành động / Truy cập', width: '220px', align: 'start' },
    { text: 'Đối tượng / Chi tiết', width: '280px', align: 'start' },
    { text: 'Phân loại', width: '140px', align: 'center' },
    { text: 'Thao tác', width: '90px', align: 'center' }
];

// Phân loại danh mục hoạt động
const getActionCategory = (hanhDong = '') => {
    const text = String(hanhDong).toLowerCase();
    if (text.includes('đăng nhập') || text.includes('vào web') || text.includes('truy cập') || text.includes('login')) {
        return { label: 'Vào web', color: 'info', icon: 'mdi-login-variant', isGiaoCa: false };
    }
    if (text.includes('giao ca') || text.includes('mở ca') || text.includes('chốt ca') || text.includes('bàn giao ca')) {
        return { label: 'Giao ca', color: 'primary', icon: 'mdi-cash-register', isGiaoCa: true };
    }
    if (text.includes('chấm công') || text.includes('checkin') || text.includes('checkout') || text.includes('khuôn mặt')) {
        return { label: 'Chấm công', color: 'teal', icon: 'mdi-face-recognition', isGiaoCa: false };
    }
    if (text.includes('lịch') || text.includes('đổi ca') || text.includes('ca làm')) {
        return { label: 'Lịch làm việc', color: 'indigo', icon: 'mdi-calendar-clock', isGiaoCa: false };
    }
    if (text.includes('đơn hàng') || text.includes('hóa đơn') || text.includes('pos')) {
        return { label: 'Đơn hàng', color: 'emerald-darken-1', icon: 'mdi-receipt', isGiaoCa: false };
    }
    if (text.includes('sản phẩm') || text.includes('biến thể')) {
        return { label: 'Sản phẩm', color: 'amber-darken-3', icon: 'mdi-shoe-sneaker', isGiaoCa: false };
    }
    if (text.includes('giảm giá') || text.includes('khuyến mãi') || text.includes('phiếu')) {
        return { label: 'Khuyến mãi', color: 'pink', icon: 'mdi-ticket-percent', isGiaoCa: false };
    }
    return { label: 'Hệ thống', color: 'slate-600', icon: 'mdi-cog-outline', isGiaoCa: false };
};

// Lọc theo Category ở Client-side (nếu có chọn)
const filteredActivities = computed(() => {
    let list = activities.value || [];
    if (filtersActivities.value.category && filtersActivities.value.category !== 'ALL') {
        list = list.filter((item) => {
            const cat = getActionCategory(item.hanhDong);
            return cat.label === filtersActivities.value.category;
        });
    }
    return list;
});

// =========================================================================
// TAB 2: LỊCH SỬ GIAO CA CỦA NHÂN VIÊN
// =========================================================================
const listGiaoCa = ref([]);
const loadingGiaoCa = ref(false);
const filtersGiaoCa = ref({
    search: '',
    trangThai: null,
    ngay: null
});
const paginationGiaoCa = ref({
    page: 1,
    size: 10
});

const loadGiaoCaList = async () => {
    loadingGiaoCa.value = true;
    try {
        const res = await dichVuGiaoCa.getAllLichSu();
        listGiaoCa.value = res?.data || res || [];
    } catch (e) {
        console.error('Không thể nạp danh sách giao ca:', e);
        listGiaoCa.value = [];
    } finally {
        loadingGiaoCa.value = false;
    }
};

const { isRefreshing: isRefreshingGiaoCa, handleRefresh: refreshGiaoCa } = useRefreshHandler();
const onRefreshGiaoCa = () => {
    refreshGiaoCa(async () => {
        filtersGiaoCa.value = { search: '', trangThai: null, ngay: null };
        paginationGiaoCa.value.page = 1;
        await loadGiaoCaList();
    });
};

const headersGiaoCa = [
    { text: 'STT', width: '60px', align: 'center' },
    { text: 'Mã ca', width: '130px', align: 'center' },
    { text: 'Thời gian mở / chốt', width: '190px', align: 'center' },
    { text: 'Nhân viên mở ca', width: '160px', align: 'start' },
    { text: 'Nhân viên nhận ca', width: '160px', align: 'start' },
    { text: 'Tiền đầu ca', width: '130px', align: 'end' },
    { text: 'Doanh thu ca', width: '130px', align: 'end' },
    { text: 'Tiền thực tế', width: '130px', align: 'end' },
    { text: 'Chênh lệch', width: '130px', align: 'end' },
    { text: 'Trạng thái', width: '130px', align: 'center' },
    { text: 'Thao tác', width: '80px', align: 'center' }
];

const filteredGiaoCaList = computed(() => {
    let list = listGiaoCa.value || [];
    const search = filtersGiaoCa.value.search?.toLowerCase()?.trim();
    if (search) {
        list = list.filter((item) => {
            const ma = (item.maGiaoCa || item.id || '').toLowerCase();
            const nv = (item.nhanVienTen || item.tenNhanVienTrongCa || '').toLowerCase();
            const nvNhan = (item.nhanVienNhanCaTen || item.tenNhanVienNhanCa || '').toLowerCase();
            const note = (item.ghiChu || '').toLowerCase();
            return ma.includes(search) || nv.includes(search) || nvNhan.includes(search) || note.includes(search);
        });
    }

    if (filtersGiaoCa.value.trangThai) {
        list = list.filter((item) => item.trangThai === filtersGiaoCa.value.trangThai);
    }

    if (filtersGiaoCa.value.ngay) {
        const filterDateStr = filtersGiaoCa.value.ngay; // yyyy-MM-dd
        list = list.filter((item) => {
            const ts = item.thoiGianMoCa || item.thoiGianVaoCa;
            if (!ts) return false;
            const dateStr = new Date(ts).toISOString().substr(0, 10);
            return dateStr === filterDateStr;
        });
    }

    return list;
});

const totalPagesGiaoCa = computed(() => {
    return Math.max(1, Math.ceil(filteredGiaoCaList.value.length / paginationGiaoCa.value.size));
});

const paginatedGiaoCaList = computed(() => {
    const start = (paginationGiaoCa.value.page - 1) * paginationGiaoCa.value.size;
    const end = start + paginationGiaoCa.value.size;
    return filteredGiaoCaList.value.slice(start, end);
});

// =========================================================================
// MODALS
// =========================================================================
const detailModal = ref({
    show: false,
    activity: null
});

const openActivityDetail = (item) => {
    detailModal.value = {
        show: true,
        activity: item
    };
};

const giaoCaModal = ref({
    show: false,
    item: null
});

const openGiaoCaDetail = (item) => {
    giaoCaModal.value = {
        show: true,
        item: item
    };
};

const getChenhLech = (item) => {
    if (!item) return 0;
    if (item.tienChenhLech != null) return Number(item.tienChenhLech);
    const expected = (Number(item.tienBanDau) || 0) + (Number(item.tongDoanhThu) || 0);
    return (Number(item.tienThucTe) || 0) - expected;
};

const getChenhLechColor = (val) => {
    if (val > 0) return 'text-info';
    if (val < 0) return 'text-error';
    return 'text-success';
};

const getStatusBadge = (status) => {
    if (status === 'OPEN') return { label: 'Đang mở ca', color: 'success', icon: 'mdi-lock-open-outline' };
    if (status === 'CLOSED') return { label: 'Đã chốt ca', color: 'grey', icon: 'mdi-lock-outline' };
    if (status === 'PENDING') return { label: 'Chờ nhận ca', color: 'warning', icon: 'mdi-clock-outline' };
    return { label: status || '--', color: 'primary', icon: 'mdi-help-circle-outline' };
};

const breadcrumbs = [
    { title: 'Quản lý lịch', disabled: false, href: '#' },
    { title: 'Lịch sử hoạt động & Giao ca', disabled: true }
];

onMounted(() => {
    paginationActivities.value.size = 10;
    loadActivities();
    loadGiaoCaList();
});
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="breadcrumbs" />

        <!-- Tab Switcher -->
        <div class="activity-tabs-container mb-3 flex-shrink-0">
            <v-tabs
                v-model="activeTab"
                color="primary"
                align-tabs="start"
                class="bg-white rounded-lg border"
                density="comfortable"
            >
                <v-tab value="web" class="font-weight-bold text-none px-5">
                    <v-icon start size="18" color="primary">mdi-web</v-icon>
                    Lịch sử vào web & Hoạt động
                </v-tab>
                <v-tab value="giao-ca" class="font-weight-bold text-none px-5">
                    <v-icon start size="18" color="amber-darken-3">mdi-cash-register</v-icon>
                    Lịch sử giao ca của nhân viên
                    <v-chip size="x-small" color="primary" class="ml-2 font-weight-bold" variant="tonal">
                        {{ filteredGiaoCaList.length }}
                    </v-chip>
                </v-tab>
            </v-tabs>
        </div>

        <!-- ================= TAB 1: LỊCH SỬ VÀO WEB & HOẠT ĐỘNG ================= -->
        <template v-if="activeTab === 'web'">
            <div class="filter-shell mb-3">
                <AdminFilter
                    title="Bộ lọc nhật ký hoạt động"
                    :loading="loadingActivities"
                    :is-refreshing="isRefreshingActivities"
                    @refresh="onRefreshActivities"
                >
                    <v-col cols="12" sm="4" md="4" class="filter-cell">
                        <div class="filter-field-label">Tìm kiếm hoạt động</div>
                        <v-text-field
                            v-model="filtersActivities.search"
                            placeholder="Nhập người thực hiện, hành động, đối tượng..."
                            variant="outlined"
                            density="compact"
                            hide-details
                            clearable
                            prepend-inner-icon="mdi-magnify"
                            class="compact-input"
                            @input="handleFilterActivities"
                        />
                    </v-col>

                    <v-col cols="12" sm="4" md="3" class="filter-cell">
                        <div class="filter-field-label">Phân loại danh mục</div>
                        <v-select
                            v-model="filtersActivities.category"
                            :items="[
                                { title: 'Tất cả hoạt động', value: 'ALL' },
                                { title: 'Vào web & Đăng nhập', value: 'Vào web' },
                                { title: 'Giao ca & Bán hàng', value: 'Giao ca' },
                                { title: 'Chấm công & Điểm danh', value: 'Chấm công' },
                                { title: 'Lịch làm việc & Ca làm', value: 'Lịch làm việc' },
                                { title: 'Đơn hàng & POS', value: 'Đơn hàng' },
                                { title: 'Sản phẩm & Biến thể', value: 'Sản phẩm' },
                                { title: 'Khuyến mãi & Phiếu giảm', value: 'Khuyến mãi' },
                                { title: 'Hệ thống', value: 'Hệ thống' }
                            ]"
                            variant="outlined"
                            density="compact"
                            hide-details
                            class="compact-input"
                        />
                    </v-col>

                    <v-col cols="12" sm="4" md="3" class="filter-cell">
                        <div class="filter-field-label">Lọc theo ngày</div>
                        <AppDatePicker
                            :model-value="filtersActivities.ngay"
                            @update:model-value="
                                (val) => {
                                    filtersActivities.ngay = val
                                        ? new Date(val.getTime() - val.getTimezoneOffset() * 60000).toISOString().substr(0, 10)
                                        : null;
                                    handleFilterActivities();
                                }
                            "
                            placeholder="Chọn ngày thực hiện"
                            clearable
                        />
                    </v-col>
                </AdminFilter>
            </div>

            <AdminTable
                title="Nhật ký vào web & hoạt động hệ thống"
                :headers="headersActivities"
                :items="filteredActivities"
                :loading="loadingActivities"
                :show-add-button="false"
                :total-count="paginationActivities.totalElements"
            >
                <template #row="{ item, index }">
                    <tr class="data-row cursor-pointer" @click="openActivityDetail(item)">
                        <td class="data-cell text-center font-weight-medium text-slate-500">
                            {{ (paginationActivities.page - 1) * paginationActivities.size + index + 1 }}
                        </td>

                        <td class="data-cell text-center">
                            <div class="d-inline-flex align-center bg-slate-50 px-2 py-1 rounded text-caption font-weight-medium text-slate-700 border">
                                <v-icon size="14" color="primary" class="mr-1">mdi-clock-outline</v-icon>
                                {{ item.ngay }}
                            </div>
                        </td>

                        <td class="data-cell">
                            <div class="d-flex align-center">
                                <v-avatar size="28" color="primary-lighten-5" class="mr-2 border">
                                    <span class="font-weight-bold text-primary text-caption">
                                        {{ item.nguoiThucHien ? item.nguoiThucHien.charAt(0).toUpperCase() : '?' }}
                                    </span>
                                </v-avatar>
                                <span class="font-weight-bold text-slate-800 text-body-2">{{ item.nguoiThucHien || 'Hệ thống' }}</span>
                            </div>
                        </td>

                        <td class="data-cell">
                            <span class="font-weight-medium text-slate-800">{{ item.hanhDong }}</span>
                        </td>

                        <td class="data-cell">
                            <v-chip size="small" variant="tonal" color="slate-700" class="font-weight-medium">
                                {{ item.doiTuong }}
                            </v-chip>
                        </td>

                        <td class="data-cell text-center">
                            <v-chip
                                size="small"
                                :color="getActionCategory(item.hanhDong).color"
                                variant="flat"
                                class="font-weight-bold"
                            >
                                <v-icon start size="14">{{ getActionCategory(item.hanhDong).icon }}</v-icon>
                                {{ getActionCategory(item.hanhDong).label }}
                            </v-chip>
                        </td>

                        <td class="data-cell text-center" @click.stop>
                            <v-tooltip text="Xem chi tiết" location="top">
                                <template #activator="{ props }">
                                    <v-btn
                                        v-bind="props"
                                        icon="mdi-eye-outline"
                                        size="small"
                                        variant="text"
                                        color="primary"
                                        @click="openActivityDetail(item)"
                                    />
                                </template>
                            </v-tooltip>
                        </td>
                    </tr>
                </template>

                <template #pagination>
                    <AdminPagination
                        v-model="paginationActivities.page"
                        v-model:page-size="paginationActivities.size"
                        :total-pages="paginationActivities.totalPages"
                        :total-elements="paginationActivities.totalElements"
                        :current-size="filteredActivities.length"
                        @change="loadActivities"
                    />
                </template>
            </AdminTable>
        </template>

        <!-- ================= TAB 2: LỊCH SỬ GIAO CA CỦA NHÂN VIÊN ================= -->
        <template v-else-if="activeTab === 'giao-ca'">
            <div class="filter-shell mb-3">
                <AdminFilter
                    title="Bộ lọc lịch sử giao ca"
                    :loading="loadingGiaoCa"
                    :is-refreshing="isRefreshingGiaoCa"
                    @refresh="onRefreshGiaoCa"
                >
                    <v-col cols="12" sm="4" md="4" class="filter-cell">
                        <div class="filter-field-label">Tìm kiếm ca</div>
                        <v-text-field
                            v-model="filtersGiaoCa.search"
                            placeholder="Mã giao ca, nhân viên mở/nhận ca..."
                            variant="outlined"
                            density="compact"
                            hide-details
                            clearable
                            prepend-inner-icon="mdi-magnify"
                            class="compact-input"
                            @update:model-value="paginationGiaoCa.page = 1"
                        />
                    </v-col>

                    <v-col cols="12" sm="4" md="3" class="filter-cell">
                        <div class="filter-field-label">Trạng thái ca</div>
                        <v-select
                            v-model="filtersGiaoCa.trangThai"
                            :items="[
                                { title: 'Tất cả trạng thái', value: null },
                                { title: 'Đang mở ca', value: 'OPEN' },
                                { title: 'Đã chốt ca', value: 'CLOSED' },
                                { title: 'Chờ nhận ca', value: 'PENDING' }
                            ]"
                            variant="outlined"
                            density="compact"
                            hide-details
                            class="compact-input"
                            @update:model-value="paginationGiaoCa.page = 1"
                        />
                    </v-col>

                    <v-col cols="12" sm="4" md="3" class="filter-cell">
                        <div class="filter-field-label">Lọc theo ngày mở</div>
                        <AppDatePicker
                            :model-value="filtersGiaoCa.ngay"
                            @update:model-value="
                                (val) => {
                                    filtersGiaoCa.ngay = val
                                        ? new Date(val.getTime() - val.getTimezoneOffset() * 60000).toISOString().substr(0, 10)
                                        : null;
                                    paginationGiaoCa.page = 1;
                                }
                            "
                            placeholder="Chọn ngày mở ca"
                            clearable
                        />
                    </v-col>
                </AdminFilter>
            </div>

            <AdminTable
                title="Danh sách ca làm & bàn giao nhân viên"
                :headers="headersGiaoCa"
                :items="paginatedGiaoCaList"
                :loading="loadingGiaoCa"
                :show-add-button="false"
                :total-count="filteredGiaoCaList.length"
            >
                <template #row="{ item, index }">
                    <tr class="data-row cursor-pointer" @click="openGiaoCaDetail(item)">
                        <td class="data-cell text-center font-weight-medium text-slate-500">
                            {{ (paginationGiaoCa.page - 1) * paginationGiaoCa.size + index + 1 }}
                        </td>

                        <td class="data-cell text-center">
                            <span class="font-weight-bold text-primary font-mono text-body-2">
                                {{ item.maGiaoCa || item.id?.substring(0, 8) || '--' }}
                            </span>
                        </td>

                        <td class="data-cell text-center">
                            <div class="d-flex flex-column align-center ga-1 py-1">
                                <span class="text-caption font-weight-medium text-slate-700">
                                    <v-icon size="13" color="success" class="mr-0.5">mdi-login</v-icon>
                                    {{ formatDateTime(item.thoiGianMoCa || item.thoiGianVaoCa) || '--' }}
                                </span>
                                <span v-if="item.thoiGianChotCa || item.thoiGianRaCa" class="text-caption text-slate-500">
                                    <v-icon size="13" color="grey" class="mr-0.5">mdi-logout</v-icon>
                                    {{ formatDateTime(item.thoiGianChotCa || item.thoiGianRaCa) }}
                                </span>
                            </div>
                        </td>

                        <td class="data-cell">
                            <div class="d-flex align-center">
                                <v-avatar size="26" color="blue-lighten-5" class="mr-2 border">
                                    <span class="font-weight-bold text-primary text-caption">
                                        {{ (item.nhanVienTen || item.tenNhanVienTrongCa || '?').charAt(0).toUpperCase() }}
                                    </span>
                                </v-avatar>
                                <span class="font-weight-bold text-slate-800 text-body-2">
                                    {{ item.nhanVienTen || item.tenNhanVienTrongCa || 'N/A' }}
                                </span>
                            </div>
                        </td>

                        <td class="data-cell">
                            <div class="d-flex align-center">
                                <v-avatar size="26" color="teal-lighten-5" class="mr-2 border">
                                    <span class="font-weight-bold text-teal text-caption">
                                        {{ (item.nhanVienNhanCaTen || item.tenNhanVienNhanCa || '?').charAt(0).toUpperCase() }}
                                    </span>
                                </v-avatar>
                                <span class="font-weight-medium text-slate-700 text-body-2">
                                    {{ item.nhanVienNhanCaTen || item.tenNhanVienNhanCa || 'Chưa bàn giao' }}
                                </span>
                            </div>
                        </td>

                        <td class="data-cell text-right font-weight-medium text-slate-700">
                            {{ formatCurrency(item.tienBanDau) }}
                        </td>

                        <td class="data-cell text-right font-weight-bold text-success">
                            {{ formatCurrency(item.tongDoanhThu) }}
                        </td>

                        <td class="data-cell text-right font-weight-bold text-amber-darken-3">
                            {{ item.tienThucTe != null ? formatCurrency(item.tienThucTe) : '--' }}
                        </td>

                        <td :class="['data-cell text-right font-weight-bold', getChenhLechColor(getChenhLech(item))]">
                            <template v-if="item.trangThai === 'CLOSED'">
                                {{ getChenhLech(item) > 0 ? '+' : '' }}{{ formatCurrency(getChenhLech(item)) }}
                            </template>
                            <template v-else>
                                --
                            </template>
                        </td>

                        <td class="data-cell text-center">
                            <v-chip
                                size="small"
                                :color="getStatusBadge(item.trangThai).color"
                                variant="flat"
                                class="font-weight-bold"
                            >
                                <v-icon start size="14">{{ getStatusBadge(item.trangThai).icon }}</v-icon>
                                {{ getStatusBadge(item.trangThai).label }}
                            </v-chip>
                        </td>

                        <td class="data-cell text-center" @click.stop>
                            <v-tooltip text="Xem chi tiết ca làm" location="top">
                                <template #activator="{ props }">
                                    <v-btn
                                        v-bind="props"
                                        icon="mdi-eye-outline"
                                        size="small"
                                        variant="text"
                                        color="primary"
                                        @click="openGiaoCaDetail(item)"
                                    />
                                </template>
                            </v-tooltip>
                        </td>
                    </tr>
                </template>

                <template #pagination>
                    <AdminPagination
                        v-model="paginationGiaoCa.page"
                        v-model:page-size="paginationGiaoCa.size"
                        :total-pages="totalPagesGiaoCa"
                        :total-elements="filteredGiaoCaList.length"
                        :current-size="paginatedGiaoCaList.length"
                    />
                </template>
            </AdminTable>
        </template>

        <!-- ================= MODAL CHI TIẾT NHẬT KÝ HOẠT ĐỘNG ================= -->
        <v-dialog v-model="detailModal.show" max-width="600px" transition="dialog-bottom-transition">
            <v-card v-if="detailModal.activity" class="rounded-xl overflow-hidden elevation-10 border">
                <v-card-item class="pa-4 bg-slate-900 text-white border-b">
                    <div class="d-flex align-center justify-space-between w-100">
                        <div class="d-flex align-center ga-3">
                            <div class="d-flex align-center justify-center rounded-circle pa-2 bg-white-10 border border-white-20">
                                <v-icon size="22" color="white">{{ getActionCategory(detailModal.activity.hanhDong).icon }}</v-icon>
                            </div>
                            <div>
                                <div class="text-subtitle-1 font-weight-bold text-white mb-0">
                                    Chi Tiết Nhật Ký Hoạt Động
                                </div>
                                <div class="text-caption text-slate-300">
                                    Thời gian: {{ detailModal.activity.ngay }}
                                </div>
                            </div>
                        </div>
                        <v-btn icon="mdi-close" variant="text" color="white" density="compact" @click="detailModal.show = false" />
                    </div>
                </v-card-item>

                <v-card-text class="pa-5 bg-slate-50">
                    <div class="bg-white pa-4 rounded-xl border border-slate-200 shadow-xs">
                        <v-row dense>
                            <v-col cols="12" sm="6" class="mb-2">
                                <div class="text-caption text-slate-500 font-weight-medium">Người thực hiện:</div>
                                <div class="d-flex align-center mt-1">
                                    <v-avatar size="24" color="primary" class="mr-2 text-white font-weight-bold text-caption">
                                        {{ detailModal.activity.nguoiThucHien ? detailModal.activity.nguoiThucHien.charAt(0).toUpperCase() : '?' }}
                                    </v-avatar>
                                    <span class="font-weight-bold text-body-2 text-slate-800">
                                        {{ detailModal.activity.nguoiThucHien || 'Hệ thống' }}
                                    </span>
                                </div>
                            </v-col>

                            <v-col cols="12" sm="6" class="mb-2">
                                <div class="text-caption text-slate-500 font-weight-medium">Phân loại:</div>
                                <v-chip
                                    size="small"
                                    :color="getActionCategory(detailModal.activity.hanhDong).color"
                                    variant="flat"
                                    class="font-weight-bold mt-1"
                                >
                                    <v-icon start size="14">{{ getActionCategory(detailModal.activity.hanhDong).icon }}</v-icon>
                                    {{ getActionCategory(detailModal.activity.hanhDong).label }}
                                </v-chip>
                            </v-col>

                            <v-col cols="12" class="mb-2">
                                <div class="text-caption text-slate-500 font-weight-medium">Hành động:</div>
                                <div class="font-weight-bold text-body-2 text-primary mt-1">
                                    {{ detailModal.activity.hanhDong }}
                                </div>
                            </v-col>

                            <v-col cols="12" class="mb-2">
                                <div class="text-caption text-slate-500 font-weight-medium">Đối tượng tác động:</div>
                                <div class="font-weight-bold text-body-2 text-slate-700 mt-1">
                                    {{ detailModal.activity.doiTuong }}
                                </div>
                            </v-col>
                        </v-row>
                    </div>
                </v-card-text>

                <v-card-actions class="pa-4 bg-white border-t d-flex justify-end">
                    <v-btn color="primary" variant="flat" class="text-none px-6 font-weight-bold rounded-lg" @click="detailModal.show = false">
                        Đóng
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- ================= MODAL CHI TIẾT GIAO CA ================= -->
        <v-dialog v-model="giaoCaModal.show" max-width="720px" transition="dialog-bottom-transition">
            <v-card v-if="giaoCaModal.item" class="rounded-xl overflow-hidden elevation-10 border">
                <v-card-item class="pa-5 bg-slate-900 text-white border-b">
                    <div class="d-flex align-center justify-space-between w-100">
                        <div class="d-flex align-center ga-3">
                            <div class="d-flex align-center justify-center rounded-circle pa-2 bg-white-10 border border-white-20">
                                <v-icon size="24" color="white">mdi-cash-register</v-icon>
                            </div>
                            <div>
                                <div class="text-h6 font-weight-bold text-white mb-0">
                                    Chi Tiết Ca Làm & Bàn Giao #{{ giaoCaModal.item.maGiaoCa || giaoCaModal.item.id?.substring(0, 8) }}
                                </div>
                                <div class="text-caption text-slate-300">
                                    Mở lúc: {{ formatDateTime(giaoCaModal.item.thoiGianMoCa || giaoCaModal.item.thoiGianVaoCa) }}
                                </div>
                            </div>
                        </div>
                        <v-btn icon="mdi-close" variant="text" color="white" density="compact" @click="giaoCaModal.show = false" />
                    </div>
                </v-card-item>

                <v-card-text class="pa-6 bg-slate-50">
                    <div class="bg-white pa-5 rounded-xl border border-slate-200 shadow-xs">
                        <div class="d-flex align-center justify-space-between mb-4 pb-2 border-b">
                            <div class="d-flex align-center ga-2">
                                <v-icon size="18" color="primary">mdi-account-group</v-icon>
                                <span class="text-subtitle-2 font-weight-bold text-slate-800">Thông tin nhân sự ca làm</span>
                            </div>
                            <v-chip
                                size="small"
                                :color="getStatusBadge(giaoCaModal.item.trangThai).color"
                                class="font-weight-bold"
                                variant="flat"
                            >
                                {{ getStatusBadge(giaoCaModal.item.trangThai).label }}
                            </v-chip>
                        </div>

                        <!-- Nhân viên mở & nhận ca -->
                        <v-row dense class="mb-4">
                            <v-col cols="12" sm="6">
                                <div class="pa-3 bg-slate-50 rounded-lg border">
                                    <div class="text-caption text-slate-500 font-weight-medium">Nhân viên mở ca:</div>
                                    <div class="font-weight-bold text-body-2 text-slate-800 mt-1">
                                        {{ giaoCaModal.item.nhanVienTen || giaoCaModal.item.tenNhanVienTrongCa || 'N/A' }}
                                    </div>
                                    <div class="text-caption text-slate-500 mt-1">
                                        Mở lúc: {{ formatDateTime(giaoCaModal.item.thoiGianMoCa || giaoCaModal.item.thoiGianVaoCa) || '--' }}
                                    </div>
                                </div>
                            </v-col>

                            <v-col cols="12" sm="6">
                                <div class="pa-3 bg-slate-50 rounded-lg border">
                                    <div class="text-caption text-slate-500 font-weight-medium">Nhân viên nhận / chốt ca:</div>
                                    <div class="font-weight-bold text-body-2 text-slate-800 mt-1">
                                        {{ giaoCaModal.item.nhanVienNhanCaTen || giaoCaModal.item.tenNhanVienNhanCa || 'Chưa bàn giao' }}
                                    </div>
                                    <div class="text-caption text-slate-500 mt-1">
                                        Chốt lúc: {{ formatDateTime(giaoCaModal.item.thoiGianChotCa || giaoCaModal.item.thoiGianRaCa) || '--' }}
                                    </div>
                                </div>
                            </v-col>
                        </v-row>

                        <div class="d-flex align-center ga-2 mb-3">
                            <v-icon size="18" color="primary">mdi-cash-multiple</v-icon>
                            <span class="text-subtitle-2 font-weight-bold text-slate-800">Số liệu đối soát tài chính</span>
                        </div>

                        <!-- 4 Thẻ tài chính đối soát -->
                        <v-row dense>
                            <v-col cols="6" sm="3">
                                <div class="pa-3 rounded-lg border bg-blue-lighten-5 text-center">
                                    <div class="text-caption text-slate-600 font-weight-medium">Tiền đầu ca</div>
                                    <div class="text-subtitle-2 font-weight-bold text-primary mt-1">
                                        {{ formatCurrency(giaoCaModal.item.tienBanDau) }}
                                    </div>
                                </div>
                            </v-col>

                            <v-col cols="6" sm="3">
                                <div class="pa-3 rounded-lg border bg-emerald-lighten-5 text-center">
                                    <div class="text-caption text-slate-600 font-weight-medium">Doanh thu ca</div>
                                    <div class="text-subtitle-2 font-weight-bold text-success mt-1">
                                        {{ formatCurrency(giaoCaModal.item.tongDoanhThu) }}
                                    </div>
                                </div>
                            </v-col>

                            <v-col cols="6" sm="3">
                                <div class="pa-3 rounded-lg border bg-amber-lighten-5 text-center">
                                    <div class="text-caption text-slate-600 font-weight-medium">Tiền thực tế</div>
                                    <div class="text-subtitle-2 font-weight-bold text-amber-darken-3 mt-1">
                                        {{ giaoCaModal.item.tienThucTe != null ? formatCurrency(giaoCaModal.item.tienThucTe) : '--' }}
                                    </div>
                                </div>
                            </v-col>

                            <v-col cols="6" sm="3">
                                <div class="pa-3 rounded-lg border bg-slate-50 text-center">
                                    <div class="text-caption text-slate-600 font-weight-medium">Chênh lệch</div>
                                    <div :class="['text-subtitle-2 font-weight-bold mt-1', getChenhLechColor(getChenhLech(giaoCaModal.item))]">
                                        <template v-if="giaoCaModal.item.trangThai === 'CLOSED'">
                                            {{ getChenhLech(giaoCaModal.item) > 0 ? '+' : '' }}{{ formatCurrency(getChenhLech(giaoCaModal.item)) }}
                                        </template>
                                        <template v-else>
                                            --
                                        </template>
                                    </div>
                                </div>
                            </v-col>
                        </v-row>

                        <!-- Ghi chú nếu có -->
                        <div v-if="giaoCaModal.item.ghiChu" class="mt-4 pa-3 bg-amber-lighten-5 rounded-lg border border-amber-200">
                            <div class="text-caption font-weight-bold text-amber-darken-3 mb-1">Ghi chú ca làm:</div>
                            <div class="text-caption text-slate-800">{{ giaoCaModal.item.ghiChu }}</div>
                        </div>
                    </div>
                </v-card-text>

                <v-card-actions class="pa-4 bg-white border-t d-flex justify-end">
                    <v-btn color="primary" variant="flat" class="text-none px-6 font-weight-bold rounded-lg" @click="giaoCaModal.show = false">
                        Đóng
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-container>
</template>

<style scoped>
.bg-white-10 {
    background-color: rgba(255, 255, 255, 0.12);
}
.border-white-20 {
    border-color: rgba(255, 255, 255, 0.24) !important;
}
.shadow-xs {
    box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

:deep(.v-field__input),
:deep(.v-field__input input),
:deep(.v-field__input input::placeholder),
:deep(.v-select__selection),
:deep(.v-select__selection-text),
:deep(.v-autocomplete__selection),
:deep(.v-autocomplete__selection-text) {
    font-size: 13px !important;
}
</style>
