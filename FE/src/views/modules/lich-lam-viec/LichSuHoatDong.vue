<script setup>
import { ref, onMounted, computed } from 'vue';
import { AdminFilter, AdminTable, AdminPagination, AdminBreadcrumbs } from '@/components/common';
import AppDatePicker from '@/components/common/AppDatePicker.vue';
import apiService from '@/services/apiService';
import { dichVuGiaoCa } from '@/services/admin/dichVuGiaoCa';
import { API_LICH_LAM_VIEC } from '@/constants/apiPaths';
import { formatDateTime, formatCurrency } from '@/utils/formatters';

const activeTab = ref('hoat-dong');
const loading = ref(false);
const isRefreshing = ref(false);

const breadcrumbs = [
    { title: 'Quản lý lịch', disabled: false, href: '#' },
    { title: 'Lịch sử hoạt động', disabled: true }
];

const filters = ref({
    search: '',
    ngay: null
});

// ==================== TAB 1: LỊCH SỬ HOẠT ĐỘNG ====================
const items = ref([]);
const pagination = ref({
    page: 1,
    size: 10,
    totalElements: 0,
    totalPages: 0
});

const tableHeadersHoatDong = [
    { text: 'STT', width: '60px', align: 'center' },
    { text: 'Người thực hiện', width: '180px', align: 'start' },
    { text: 'Hành động', width: '220px', align: 'start' },
    { text: 'Đối tượng', width: '280px', align: 'start' },
    { text: 'Thời gian', width: '180px', align: 'center' }
];

const loadActivities = async () => {
    loading.value = true;
    try {
        const response = await apiService.get(API_LICH_LAM_VIEC.ACTIVITIES, {
            params: {
                page: pagination.value.page - 1,
                size: pagination.value.size,
                search: filters.value.search || undefined,
                ngay: filters.value.ngay || undefined
            }
        });
        if (response?.data?.success) {
            items.value = response.data.data.content || [];
            pagination.value.totalElements = response.data.data.totalElements || 0;
            pagination.value.totalPages = response.data.data.totalPages || 0;
        }
    } catch (error) {
        console.error('Error fetching activities:', error);
        items.value = [];
    } finally {
        loading.value = false;
    }
};

// ==================== TAB 2: LỊCH SỬ GIAO CA ====================
const listGiaoCa = ref([]);
const paginationGiaoCa = ref({
    page: 1,
    size: 10
});

const headersGiaoCa = [
    { text: 'Mã ca', align: 'center', width: '90px' },
    { text: 'Nhân viên mở', align: 'start' },
    { text: 'Trạng thái', align: 'center', width: '120px' },
    { text: 'Thời gian mở', align: 'center', width: '160px' },
    { text: 'Thời gian chốt', align: 'center', width: '160px' },
    { text: 'Tiền mặt đầu ca', align: 'end', width: '140px' },
    { text: 'Doanh thu ca', align: 'end', width: '140px' },
    { text: 'Tiền mặt chốt ca', align: 'end', width: '140px' },
    { text: 'Chênh lệch', align: 'end', width: '140px' },
    { text: 'Người nhận ca', align: 'start' }
];

const fetchListGiaoCa = async () => {
    loading.value = true;
    try {
        const res = await dichVuGiaoCa.getAllLichSu();
        listGiaoCa.value = res?.data || res || [];
    } catch (e) {
        console.error('Error fetching giao ca:', e);
        listGiaoCa.value = [];
    } finally {
        loading.value = false;
    }
};

const filteredGiaoCaList = computed(() => {
    let list = listGiaoCa.value || [];
    const search = filters.value.search?.toLowerCase()?.trim();
    if (search) {
        list = list.filter((item) => {
            const ma = (item.maGiaoCa || item.id || '').toLowerCase();
            const nv = (item.nhanVienTen || '').toLowerCase();
            const nvNhan = (item.nhanVienNhanCaTen || '').toLowerCase();
            return ma.includes(search) || nv.includes(search) || nvNhan.includes(search);
        });
    }
    if (filters.value.ngay) {
        const filterDateStr = filters.value.ngay;
        list = list.filter((item) => {
            const ts = item.thoiGianMoCa;
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

const getStatusColor = (status) => {
    if (status === 'OPEN') return 'success';
    if (status === 'CLOSED') return 'grey';
    return 'primary';
};

const getStatusLabel = (status) => {
    if (status === 'OPEN') return 'Đang mở';
    if (status === 'CLOSED') return 'Đã chốt';
    return status || '--';
};

const formatDate = (dateNum) => {
    if (!dateNum) return '--';
    return formatDateTime(dateNum);
};

const getChenhLech = (item) => {
    const expected = (item.tienBanDau || 0) + (item.tongDoanhThu || 0);
    return (item.tienThucTe || 0) - expected;
};

const getChenhLechColor = (val) => {
    if (val > 0) return 'text-info';
    if (val < 0) return 'text-error';
    return 'text-success';
};

// ==================== SHARED HANDLERS ====================
const handleRefresh = async () => {
    isRefreshing.value = true;
    filters.value = { search: '', ngay: null };
    if (activeTab.value === 'hoat-dong') {
        pagination.value.page = 1;
        await loadActivities();
    } else {
        paginationGiaoCa.value.page = 1;
        await fetchListGiaoCa();
    }
    setTimeout(() => (isRefreshing.value = false), 600);
};

const handleFilter = () => {
    if (activeTab.value === 'hoat-dong') {
        pagination.value.page = 1;
        loadActivities();
    } else {
        paginationGiaoCa.value.page = 1;
    }
};

const onTabChange = (val) => {
    activeTab.value = val;
    handleFilter();
};

onMounted(() => {
    loadActivities();
    fetchListGiaoCa();
});
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page">
        <AdminBreadcrumbs :items="breadcrumbs" />

        <div class="mb-2"></div>

        <div class="filter-top invoice-filter-shell mb-3">
            <AdminFilter title="Bộ lọc lịch sử" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="6" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field
                        v-model="filters.search"
                        placeholder="Nhập từ khóa tìm kiếm..."
                        variant="outlined"
                        density="compact"
                        hide-details
                        clearable
                        prepend-inner-icon="mdi-magnify"
                        @input="handleFilter"
                    />
                </v-col>
                <v-col cols="12" md="4" class="filter-cell">
                    <div class="filter-field-label">Lọc theo ngày</div>
                    <AppDatePicker
                        :model-value="filters.ngay"
                        @update:model-value="
                            (val) => {
                                filters.ngay = val
                                    ? new Date(val.getTime() - val.getTimezoneOffset() * 60000).toISOString().substr(0, 10)
                                    : null;
                                handleFilter();
                            }
                        "
                        placeholder="Chọn ngày"
                        clearable
                    />
                </v-col>
            </AdminFilter>
        </div>

        <!-- TAB 1: LỊCH SỬ HOẠT ĐỘNG -->
        <AdminTable
            v-if="activeTab === 'hoat-dong'"
            title="Lịch sử hoạt động hệ thống"
            :headers="tableHeadersHoatDong"
            :items="items"
            :loading="loading"
            :show-add-button="false"
        >
            <template #top>
                <div class="border-b bg-white px-2">
                    <v-tabs
                        v-model="activeTab"
                        bg-color="transparent"
                        color="primary"
                        show-arrows
                        class="admin-tabs invoice-status-tabs"
                        height="48"
                        @update:model-value="onTabChange"
                    >
                        <v-tab value="hoat-dong" class="text-none px-4 font-weight-bold">
                            <v-icon start size="18">mdi-history</v-icon>
                            Lịch sử hoạt động
                        </v-tab>
                        <v-tab value="giao-ca" class="text-none px-4 font-weight-bold">
                            <v-icon start size="18">mdi-cash-register</v-icon>
                            Lịch sử giao ca
                        </v-tab>
                    </v-tabs>
                </div>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center">{{ (pagination.page - 1) * pagination.size + index + 1 }}</td>
                    <td class="data-cell">
                        <div class="d-flex align-center">
                            <v-avatar size="24" color="primary" class="mr-2 text-white text-caption">
                                {{ item.nguoiThucHien ? item.nguoiThucHien.charAt(0) : '?' }}
                            </v-avatar>
                            <span>{{ item.nguoiThucHien || 'Hệ thống' }}</span>
                        </div>
                    </td>
                    <td class="data-cell">{{ item.hanhDong }}</td>
                    <td class="data-cell">
                        <v-chip size="x-small" variant="outlined">{{ item.doiTuong }}</v-chip>
                    </td>
                    <td class="data-cell text-center text-slate-500 text-caption">{{ item.ngay }}</td>
                </tr>
            </template>

            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    v-model:page-size="pagination.size"
                    :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements"
                    :current-size="items.length"
                    @change="loadActivities"
                />
            </template>
        </AdminTable>

        <!-- TAB 2: LỊCH SỬ GIAO CA -->
        <AdminTable
            v-else-if="activeTab === 'giao-ca'"
            title="Lịch sử giao ca nhân viên"
            :headers="headersGiaoCa"
            :items="paginatedGiaoCaList"
            :loading="loading"
            :show-add-button="false"
        >
            <template #top>
                <div class="border-b bg-white px-2">
                    <v-tabs
                        v-model="activeTab"
                        bg-color="transparent"
                        color="primary"
                        show-arrows
                        class="admin-tabs invoice-status-tabs"
                        height="48"
                        @update:model-value="onTabChange"
                    >
                        <v-tab value="hoat-dong" class="text-none px-4 font-weight-bold">
                            <v-icon start size="18">mdi-history</v-icon>
                            Lịch sử hoạt động
                        </v-tab>
                        <v-tab value="giao-ca" class="text-none px-4 font-weight-bold">
                            <v-icon start size="18">mdi-cash-register</v-icon>
                            Lịch sử giao ca
                        </v-tab>
                    </v-tabs>
                </div>
            </template>

            <template #row="{ item }">
                <tr class="data-row">
                    <td class="data-cell text-center">#{{ item.id }}</td>
                    <td class="data-cell font-weight-medium">{{ item.nhanVienTen || 'N/A' }}</td>
                    <td class="data-cell text-center">
                        <v-chip size="small" :color="getStatusColor(item.trangThai)" class="font-weight-bold" variant="flat">
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>
                    <td class="data-cell text-center text-slate-500">{{ formatDate(item.thoiGianMoCa) }}</td>
                    <td class="data-cell text-center text-slate-500">{{ formatDate(item.thoiGianChotCa) }}</td>
                    <td class="data-cell text-right">{{ formatCurrency(item.tienBanDau) }}</td>
                    <td class="data-cell text-right text-success font-weight-bold">{{ formatCurrency(item.tongDoanhThu) }}</td>
                    <td class="data-cell text-right">{{ formatCurrency(item.tienThucTe) }}</td>
                    <td class="data-cell text-right">
                        <span :class="['font-weight-bold', getChenhLechColor(getChenhLech(item))]">
                            {{ getChenhLech(item) > 0 ? '+' : '' }}{{ formatCurrency(getChenhLech(item)) }}
                        </span>
                    </td>
                    <td class="data-cell font-weight-medium">{{ item.nhanVienNhanCaTen || '--' }}</td>
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
    </v-container>
</template>

<style scoped>
.admin-module-page {
    background-color: #f8fafc;
    min-height: 100vh;
}
</style>
