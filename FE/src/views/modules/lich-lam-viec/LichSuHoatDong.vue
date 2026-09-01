<script setup>
import { ref, onMounted, computed } from 'vue';
import { AdminFilter, AdminTable, AdminPagination, AdminBreadcrumbs } from '@/components/common';
import AppDatePicker from '@/components/common/AppDatePicker.vue';
import apiService from '@/services/apiService';
import { dichVuGiaoCa } from '@/services/admin/dichVuGiaoCa';
import { API_LICH_LAM_VIEC } from '@/constants/apiPaths';
import { GIAO_CA_STATUS, GIAO_CA_STATUS_CONFIG, GIAO_CA_STATUS_OPTIONS } from '@/constants/lichLamViecConstants';
import { formatDateTime, formatCurrency } from '@/utils/formatters';

const activeTab = ref('hoat-dong');
const loading = ref(false);
const isRefreshing = ref(false);

const breadcrumbs = [
    { title: 'Quản lý lịch', disabled: false, href: '#' },
    { title: 'Lịch sử hoạt động & ca làm', disabled: true }
];

const filters = ref({
    search: '',
    ngay: null,
    trangThai: null
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

// ==================== TAB 2: LỊCH SỬ GIAO CA / CA LÀM ====================
const listGiaoCa = ref([]);
const paginationGiaoCa = ref({
    page: 1,
    size: 10
});

const headersGiaoCa = [
    { text: 'STT', width: '50px', align: 'center' },
    { text: 'Nhân viên mở ca', width: '170px', align: 'start' },
    { text: 'Nhân viên nhận ca', width: '170px', align: 'start' },
    { text: 'Thời gian ca', width: '230px', align: 'center' },
    { text: 'Tiền ban đầu', width: '130px', align: 'end' },
    { text: 'Doanh thu', width: '130px', align: 'end' },
    { text: 'Tiền thực tế', width: '130px', align: 'end' },
    { text: 'Chênh lệch', width: '130px', align: 'end' },
    { text: 'Trạng thái', width: '120px', align: 'center' }
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
            const nvTrong = (item.nhanVienTen || item.tenNhanVienTrongCa || '').toLowerCase();
            const maNvTrong = (item.maNhanVien || item.maNhanVienTrongCa || '').toLowerCase();
            const nvNhan = (item.nhanVienNhanCaTen || item.tenNhanVienNhanCa || '').toLowerCase();
            const maNvNhan = (item.maNhanVienNhanCa || '').toLowerCase();
            return nvTrong.includes(search) || maNvTrong.includes(search) || nvNhan.includes(search) || maNvNhan.includes(search);
        });
    }
    if (filters.value.trangThai) {
        list = list.filter((item) => item.trangThai === filters.value.trangThai);
    }
    if (filters.value.ngay) {
        const filterDateStr = filters.value.ngay;
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

const getStatusChipClass = (status) => {
    return GIAO_CA_STATUS_CONFIG[status]?.chipClass || 'status-chip-default';
};

const getStatusLabel = (status) => {
    return GIAO_CA_STATUS_CONFIG[status]?.label || status || '--';
};

const formatDate = (dateNum) => {
    if (!dateNum) return '--';
    return formatDateTime(dateNum);
};

const formatDuration = (start, end) => {
    if (!start) return '--';
    const startTime = typeof start === 'number' ? start : new Date(start).getTime();
    const endTime = end ? (typeof end === 'number' ? end : new Date(end).getTime()) : Date.now();
    if (isNaN(startTime) || isNaN(endTime) || endTime < startTime) return '--';

    const diffMinutes = Math.floor((endTime - startTime) / (1000 * 60));
    const hours = Math.floor(diffMinutes / 60);
    const minutes = diffMinutes % 60;

    if (hours === 0) return `${minutes} phút`;
    if (minutes === 0) return `${hours} giờ`;
    return `${hours} giờ ${minutes} phút`;
};

// ==================== SHARED HANDLERS ====================
const handleRefresh = async () => {
    isRefreshing.value = true;
    filters.value = { search: '', ngay: null, trangThai: null };
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
                <template v-if="activeTab === 'hoat-dong'">
                    <v-col cols="12" md="6" class="filter-cell">
                        <div class="filter-field-label">Tìm kiếm hoạt động</div>
                        <v-text-field
                            v-model="filters.search"
                            placeholder="Nhập từ khóa tìm kiếm (người thực hiện, hành động)..."
                            variant="outlined"
                            density="compact"
                            hide-details
                            clearable
                            prepend-inner-icon="mdi-magnify"
                            @input="handleFilter"
                        />
                    </v-col>
                    <v-col cols="12" md="6" class="filter-cell">
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
                </template>

                <template v-else>
                    <v-col cols="12" md="4" class="filter-cell">
                        <div class="filter-field-label">Tìm kiếm nhân viên</div>
                        <v-text-field
                            v-model="filters.search"
                            placeholder="Nhập mã nhân viên hoặc tên..."
                            variant="outlined"
                            density="compact"
                            hide-details
                            clearable
                            prepend-inner-icon="mdi-magnify"
                            @input="handleFilter"
                        />
                    </v-col>
                    <v-col cols="12" md="4" class="filter-cell">
                        <div class="filter-field-label">Trạng thái ca</div>
                        <v-select
                            v-model="filters.trangThai"
                            :items="GIAO_CA_STATUS_OPTIONS"
                            variant="outlined"
                            density="compact"
                            hide-details
                            @update:model-value="handleFilter"
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
                </template>
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
                            <v-icon start size="18">mdi-calendar-clock</v-icon>
                            Lịch sử ca làm
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

        <!-- TAB 2: LỊCH SỬ CA LÀM / GIAO CA -->
        <AdminTable
            v-else-if="activeTab === 'giao-ca'"
            title="Lịch sử ca làm nhân viên"
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
                            <v-icon start size="18">mdi-calendar-clock</v-icon>
                            Lịch sử ca làm
                        </v-tab>
                    </v-tabs>
                </div>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <!-- STT -->
                    <td class="data-cell text-center font-weight-medium text-slate-500">
                        {{ (paginationGiaoCa.page - 1) * paginationGiaoCa.size + index + 1 }}
                    </td>
                    <!-- Nhân viên mở ca -->
                    <td class="data-cell">
                        <div class="d-flex align-center">
                            <v-avatar size="26" color="primary-lighten-5" class="mr-2 border">
                                <span class="font-weight-bold text-primary text-caption">
                                    {{ (item.nhanVienTen || item.tenNhanVienTrongCa || '?').charAt(0).toUpperCase() }}
                                </span>
                            </v-avatar>
                            <div>
                                <div class="font-weight-bold text-slate-800 text-body-2">
                                    {{ item.nhanVienTen || item.tenNhanVienTrongCa || 'N/A' }}
                                </div>
                                <div v-if="item.maNhanVien || item.maNhanVienTrongCa" class="text-caption text-slate-400 font-mono">
                                    {{ item.maNhanVien || item.maNhanVienTrongCa }}
                                </div>
                            </div>
                        </div>
                    </td>
                    <!-- Nhân viên nhận ca -->
                    <td class="data-cell">
                        <div v-if="item.nhanVienNhanCaTen || item.tenNhanVienNhanCa" class="d-flex align-center">
                            <v-avatar size="26" color="emerald-lighten-5" class="mr-2 border">
                                <span class="font-weight-bold text-emerald-darken-2 text-caption">
                                    {{ (item.nhanVienNhanCaTen || item.tenNhanVienNhanCa).charAt(0).toUpperCase() }}
                                </span>
                            </v-avatar>
                            <div>
                                <div class="font-weight-bold text-slate-800 text-body-2">
                                    {{ item.nhanVienNhanCaTen || item.tenNhanVienNhanCa }}
                                </div>
                                <div v-if="item.maNhanVienNhanCa" class="text-caption text-slate-400 font-mono">
                                    {{ item.maNhanVienNhanCa }}
                                </div>
                            </div>
                        </div>
                        <span v-else class="text-slate-400 text-caption font-italic">Chưa bàn giao</span>
                    </td>
                    <!-- Thời gian ca (Mở ca - Chốt ca) -->
                    <td class="data-cell text-center">
                        <div class="d-flex flex-column align-center ga-0.5">
                            <div class="text-slate-700 font-weight-medium text-caption">
                                <span class="text-slate-400">Mở:</span> {{ formatDate(item.thoiGianMoCa || item.thoiGianVaoCa) }}
                            </div>
                            <div class="text-slate-700 font-weight-medium text-caption">
                                <span class="text-slate-400">Chốt:</span> {{ (item.thoiGianChotCa || item.thoiGianRaCa) ? formatDate(item.thoiGianChotCa || item.thoiGianRaCa) : '--:--' }}
                            </div>
                        </div>
                    </td>
                    <!-- Tiền ban đầu -->
                    <td class="data-cell text-end font-mono font-weight-medium text-slate-700">
                        {{ formatCurrency(item.tienBanDau || 0) }}
                    </td>
                    <!-- Doanh thu -->
                    <td class="data-cell text-end font-mono font-weight-bold text-primary">
                        {{ formatCurrency(item.tongDoanhThu || 0) }}
                    </td>
                    <!-- Tiền thực tế -->
                    <td class="data-cell text-end font-mono font-weight-bold text-slate-800">
                        {{ formatCurrency(item.tienThucTe || 0) }}
                    </td>
                    <!-- Chênh lệch -->
                    <td class="data-cell text-end font-mono font-weight-bold">
                        <span :class="Number(item.tienChenhLech || 0) < 0 ? 'text-error' : Number(item.tienChenhLech || 0) > 0 ? 'text-success' : 'text-slate-500'">
                            {{ formatCurrency(item.tienChenhLech || 0) }}
                        </span>
                    </td>
                    <!-- Trạng thái -->
                    <td class="data-cell text-center">
                        <v-chip size="small" variant="flat" :class="['status-chip', getStatusChipClass(item.trangThai)]">
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
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
    </v-container>
</template>

<style scoped>
.admin-module-page {
    background-color: #f8fafc;
    min-height: 100vh;
}
</style>
