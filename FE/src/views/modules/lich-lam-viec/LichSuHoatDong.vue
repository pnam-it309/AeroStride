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

// Active Tab: 'giao-ca' | 'hoat-dong'
const activeTab = ref('giao-ca');

// =========================================================================
// 1. LỊCH SỬ GIAO CA (Sử dụng useAdminTable & useRefreshHandler dùng chung)
// =========================================================================
const {
    items: rawListGiaoCa,
    loading: loadingGiaoCa,
    pagination: paginationGiaoCa,
    filters: filtersGiaoCa,
    loadData: loadGiaoCa,
    handleFilter: handleFilterGiaoCa,
    handleReset: handleResetGiaoCa
} = useAdminTable(
    async () => {
        try {
            const res = await dichVuGiaoCa.getAllLichSu();
            return res?.data || res || [];
        } catch (e) {
            addNotification({ title: 'Lỗi', subtitle: 'Không thể tải lịch sử giao ca', color: 'error' });
            return [];
        }
    },
    { search: '', trangThai: null, ngay: null }
);

const { isRefreshing: isRefreshingGiaoCa, handleRefresh: refreshGiaoCa } = useRefreshHandler();
const onRefreshGiaoCa = () => refreshGiaoCa(handleResetGiaoCa);

const headersGiaoCa = [
    { text: 'STT', align: 'center', width: '60px' },
    { text: 'Mã ca', align: 'center', width: '90px' },
    { text: 'Nhân viên mở', align: 'start', width: '150px' },
    { text: 'Trạng thái', align: 'center', width: '120px' },
    { text: 'Thời gian mở', align: 'center', width: '150px' },
    { text: 'Thời gian chốt', align: 'center', width: '150px' },
    { text: 'Tiền đầu ca', align: 'end', width: '130px' },
    { text: 'Doanh thu ca', align: 'end', width: '130px' },
    { text: 'Tiền chốt ca', align: 'end', width: '130px' },
    { text: 'Chênh lệch', align: 'end', width: '130px' },
    { text: 'Người nhận ca', align: 'start', width: '150px' }
];

const filteredListGiaoCa = computed(() => {
    let list = [...rawListGiaoCa.value];
    if (filtersGiaoCa.value.search) {
        const q = filtersGiaoCa.value.search.toLowerCase().trim();
        list = list.filter(
            (item) =>
                String(item.id).includes(q) ||
                (item.nhanVienTen && item.nhanVienTen.toLowerCase().includes(q)) ||
                (item.nhanVienNhanCaTen && item.nhanVienNhanCaTen.toLowerCase().includes(q))
        );
    }
    if (filtersGiaoCa.value.trangThai) {
        list = list.filter((item) => item.trangThai === filtersGiaoCa.value.trangThai);
    }
    if (filtersGiaoCa.value.ngay) {
        const targetDate = filtersGiaoCa.value.ngay;
        list = list.filter((item) => {
            if (!item.thoiGianMoCa) return false;
            const itemDate = new Date(item.thoiGianMoCa).toISOString().substr(0, 10);
            return itemDate === targetDate;
        });
    }
    return list;
});

const paginatedListGiaoCa = computed(() => {
    const start = (paginationGiaoCa.value.page - 1) * paginationGiaoCa.value.size;
    return filteredListGiaoCa.value.slice(start, start + paginationGiaoCa.value.size);
});

const totalPagesGiaoCa = computed(() => Math.max(Math.ceil(filteredListGiaoCa.value.length / paginationGiaoCa.value.size), 1));

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

const getChenhLech = (item) => {
    const expected = (item.tienBanDau || 0) + (item.tongDoanhThu || 0);
    return (item.tienThucTe || 0) - expected;
};

const getChenhLechColor = (val) => {
    if (val > 0) return 'text-info';
    if (val < 0) return 'text-error';
    return 'text-success';
};

// =========================================================================
// 2. LỊCH SỬ HOẠT ĐỘNG (Sử dụng useAdminTable & useRefreshHandler dùng chung)
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
            console.error('Error fetching activities:', error);
            return { content: [], totalElements: 0, totalPages: 1 };
        }
    },
    { search: '', ngay: null }
);

const { isRefreshing: isRefreshingActivities, handleRefresh: refreshActivities } = useRefreshHandler();
const onRefreshActivities = () => refreshActivities(handleResetActivities);

const headersActivities = [
    { text: 'STT', width: '60px', align: 'center' },
    { text: 'Người thực hiện', width: '160px', align: 'start' },
    { text: 'Hành động', width: '180px', align: 'start' },
    { text: 'Đối tượng', width: '280px', align: 'start' },
    { text: 'Thời gian', width: '180px', align: 'center' }
];

// =========================================================================
// 3. LIFECYCLE & TAB SYNC
// =========================================================================
const breadcrumbs = computed(() => [
    { title: 'Quản lý lịch', disabled: false, href: '#' },
    {
        title: activeTab.value === 'giao-ca' ? 'Lịch sử giao ca' : 'Lịch sử hoạt động hệ thống',
        disabled: true
    }
]);

const handleTabChange = (newTab) => {
    activeTab.value = newTab;
    router.replace({ query: { ...route.query, tab: newTab } });
    if (newTab === 'giao-ca' && rawListGiaoCa.value.length === 0) {
        loadGiaoCa();
    } else if (newTab === 'hoat-dong' && activities.value.length === 0) {
        loadActivities();
    }
};

onMounted(() => {
    paginationGiaoCa.value.size = 10;
    paginationActivities.value.size = 10;

    if (route.query.tab === 'hoat-dong') {
        activeTab.value = 'hoat-dong';
        loadActivities();
    } else {
        activeTab.value = 'giao-ca';
        loadGiaoCa();
    }
});

watch(
    () => route.query.tab,
    (val) => {
        if (val && val !== activeTab.value) {
            activeTab.value = val;
            if (val === 'hoat-dong' && activities.value.length === 0) loadActivities();
            if (val === 'giao-ca' && rawListGiaoCa.value.length === 0) loadGiaoCa();
        }
    }
);
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="breadcrumbs" />

        <div class="mb-3"></div>

        <!-- Unified Card Container -->
        <v-card class="elevation-1 rounded-xl overflow-hidden bg-white border">
            <!-- Navigation Tabs Bar -->
            <div class="border-b bg-slate-50 px-4 pt-3">
                <v-tabs
                    :model-value="activeTab"
                    @update:model-value="handleTabChange"
                    color="primary"
                    density="comfortable"
                    class="unified-history-tabs"
                >
                    <v-tab value="giao-ca" class="text-none font-weight-bold px-6 tab-item">
                        <v-icon start size="18" class="mr-2">mdi-cash-register</v-icon>
                        Lịch Sử Giao Ca
                        <v-chip size="x-small" color="primary" variant="flat" class="ml-2 font-weight-bold">
                            {{ filteredListGiaoCa.length }}
                        </v-chip>
                    </v-tab>
                    <v-tab value="hoat-dong" class="text-none font-weight-bold px-6 tab-item">
                        <v-icon start size="18" class="mr-2">mdi-history</v-icon>
                        Lịch Sử Hoạt Động Hệ Thống
                        <v-chip size="x-small" color="secondary" variant="flat" class="ml-2 font-weight-bold">
                            {{ paginationActivities.totalElements }}
                        </v-chip>
                    </v-tab>
                </v-tabs>
            </div>

            <!-- ========================================================= -->
            <!-- TAB 1: LỊCH SỬ GIAO CA                                   -->
            <!-- ========================================================= -->
            <div v-if="activeTab === 'giao-ca'" class="pa-4">
                <!-- Filter Shell -->
                <div class="mb-4">
                    <AdminFilter title="Bộ lọc giao ca" :loading="loadingGiaoCa" :is-refreshing="isRefreshingGiaoCa" @refresh="onRefreshGiaoCa">
                        <v-col cols="12" md="4" class="filter-cell">
                            <div class="filter-field-label">Tìm kiếm nhân viên / mã ca</div>
                            <v-text-field
                                v-model="filtersGiaoCa.search"
                                placeholder="Nhập tên nhân viên, mã ca..."
                                variant="outlined"
                                density="compact"
                                hide-details
                                clearable
                                prepend-inner-icon="mdi-magnify"
                                @input="handleFilterGiaoCa"
                            />
                        </v-col>
                        <v-col cols="12" md="4" class="filter-cell">
                            <div class="filter-field-label">Trạng thái ca</div>
                            <v-select
                                v-model="filtersGiaoCa.trangThai"
                                :items="[
                                    { title: 'Tất cả trạng thái', value: null },
                                    { title: 'Đang mở', value: 'OPEN' },
                                    { title: 'Đã chốt ca', value: 'CLOSED' }
                                ]"
                                variant="outlined"
                                density="compact"
                                hide-details
                                @update:model-value="handleFilterGiaoCa"
                            />
                        </v-col>
                        <v-col cols="12" md="4" class="filter-cell">
                            <div class="filter-field-label">Ngày mở ca</div>
                            <AppDatePicker
                                :model-value="filtersGiaoCa.ngay"
                                @update:model-value="
                                    (val) => {
                                        filtersGiaoCa.ngay = val
                                            ? new Date(val.getTime() - val.getTimezoneOffset() * 60000).toISOString().substr(0, 10)
                                            : null;
                                        handleFilterGiaoCa();
                                    }
                                "
                                placeholder="Chọn ngày mở ca"
                                clearable
                            />
                        </v-col>
                    </AdminFilter>
                </div>

                <!-- Table View -->
                <AdminTable
                    title="Danh sách giao ca làm việc"
                    :headers="headersGiaoCa"
                    :items="paginatedListGiaoCa"
                    :loading="loadingGiaoCa"
                    :show-add-button="false"
                    :total-count="filteredListGiaoCa.length"
                >
                    <template #extra-actions>
                        <v-btn color="primary" variant="tonal" prepend-icon="mdi-refresh" @click="loadGiaoCa" :loading="loadingGiaoCa">
                            Làm mới
                        </v-btn>
                    </template>
                    <template #row="{ item, index }">
                        <tr class="data-row">
                            <td class="data-cell text-center">{{ (paginationGiaoCa.page - 1) * paginationGiaoCa.size + index + 1 }}</td>
                            <td class="data-cell text-center font-weight-bold text-primary">#{{ item.id }}</td>
                            <td class="data-cell font-weight-medium">{{ item.nhanVienTen || 'N/A' }}</td>
                            <td class="data-cell text-center">
                                <v-chip size="small" :color="getStatusColor(item.trangThai)" class="font-weight-bold" variant="flat">
                                    {{ getStatusLabel(item.trangThai) }}
                                </v-chip>
                            </td>
                            <td class="data-cell text-center text-slate-600">{{ formatDateTime(item.thoiGianMoCa) }}</td>
                            <td class="data-cell text-center text-slate-600">{{ formatDateTime(item.thoiGianChotCa) }}</td>
                            <td class="data-cell text-right font-weight-medium">{{ formatCurrency(item.tienBanDau) }}</td>
                            <td class="data-cell text-right text-success font-weight-bold">{{ formatCurrency(item.tongDoanhThu) }}</td>
                            <td class="data-cell text-right font-weight-medium">{{ formatCurrency(item.tienThucTe) }}</td>
                            <td class="data-cell text-right">
                                <span :class="['font-weight-bold', getChenhLechColor(getChenhLech(item))]">
                                    {{ getChenhLech(item) > 0 ? '+' : '' }}{{ formatCurrency(getChenhLech(item)) }}
                                </span>
                            </td>
                            <td class="data-cell font-weight-medium text-slate-700">{{ item.nhanVienNhanCaTen || '--' }}</td>
                        </tr>
                    </template>
                    <template #pagination>
                        <AdminPagination
                            v-model="paginationGiaoCa.page"
                            v-model:page-size="paginationGiaoCa.size"
                            :total-pages="totalPagesGiaoCa"
                            :total-elements="filteredListGiaoCa.length"
                            :current-size="paginatedListGiaoCa.length"
                            @change="() => {}"
                        />
                    </template>
                </AdminTable>
            </div>

            <!-- ========================================================= -->
            <!-- TAB 2: LỊCH SỬ HOẠT ĐỘNG HỆ THỐNG                        -->
            <!-- ========================================================= -->
            <div v-else-if="activeTab === 'hoat-dong'" class="pa-4">
                <!-- Filter Shell -->
                <div class="mb-4">
                    <AdminFilter title="Bộ lọc lịch sử" :loading="loadingActivities" :is-refreshing="isRefreshingActivities" @refresh="onRefreshActivities">
                        <v-col cols="12" md="6" class="filter-cell">
                            <div class="filter-field-label">Tìm kiếm hoạt động</div>
                            <v-text-field
                                v-model="filtersActivities.search"
                                placeholder="Nhập tên người thực hiện, hành động..."
                                variant="outlined"
                                density="compact"
                                hide-details
                                clearable
                                prepend-inner-icon="mdi-magnify"
                                @input="handleFilterActivities"
                            />
                        </v-col>
                        <v-col cols="12" md="6" class="filter-cell">
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
                                placeholder="Chọn ngày"
                                clearable
                            />
                        </v-col>
                    </AdminFilter>
                </div>

                <!-- Table View -->
                <AdminTable
                    title="Nhật ký hoạt động hệ thống"
                    :headers="headersActivities"
                    :items="activities"
                    :loading="loadingActivities"
                    :show-add-button="false"
                    :total-count="paginationActivities.totalElements"
                >
                    <template #extra-actions>
                        <v-btn color="primary" variant="tonal" prepend-icon="mdi-refresh" @click="loadActivities" :loading="loadingActivities">
                            Làm mới
                        </v-btn>
                    </template>
                    <template #row="{ item, index }">
                        <tr class="data-row">
                            <td class="data-cell text-center">{{ (paginationActivities.page - 1) * paginationActivities.size + index + 1 }}</td>
                            <td class="data-cell">
                                <div class="d-flex align-center">
                                    <v-avatar size="28" color="primary" class="mr-2 text-white font-weight-bold text-caption">
                                        {{ item.nguoiThucHien ? item.nguoiThucHien.charAt(0).toUpperCase() : '?' }}
                                    </v-avatar>
                                    <span class="font-weight-medium text-slate-800">{{ item.nguoiThucHien }}</span>
                                </div>
                            </td>
                            <td class="data-cell font-weight-medium text-slate-800">{{ item.hanhDong }}</td>
                            <td class="data-cell">
                                <v-chip size="small" variant="tonal" color="primary" class="font-weight-medium">{{ item.doiTuong }}</v-chip>
                            </td>
                            <td class="data-cell text-center text-slate-500 text-caption">{{ item.ngay }}</td>
                        </tr>
                    </template>
                    <template #pagination>
                        <AdminPagination
                            v-model="paginationActivities.page"
                            v-model:page-size="paginationActivities.size"
                            :total-pages="paginationActivities.totalPages"
                            :total-elements="paginationActivities.totalElements"
                            :current-size="activities.length"
                            @change="loadActivities"
                        />
                    </template>
                </AdminTable>
            </div>
        </v-card>
    </v-container>
</template>

<style scoped>
.unified-history-tabs {
    border-bottom: none !important;
}

.unified-history-tabs :deep(.v-tab) {
    border-top-left-radius: 10px;
    border-top-right-radius: 10px;
    letter-spacing: 0.2px;
}

.tab-item {
    font-size: 0.925rem;
}
</style>
