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

// Danh sách dữ liệu giao ca nạp ngầm để hỗ trợ xem chi tiết khi bấm vào hoạt động giao ca
const listGiaoCa = ref([]);
const loadingGiaoCaData = ref(false);

const loadGiaoCaList = async () => {
    loadingGiaoCaData.value = true;
    try {
        const res = await dichVuGiaoCa.getAllLichSu();
        listGiaoCa.value = res?.data || res || [];
    } catch (e) {
        console.error('Không thể nạp danh sách giao ca:', e);
        listGiaoCa.value = [];
    } finally {
        loadingGiaoCaData.value = false;
    }
};

// =========================================================================
// 1. LỊCH SỬ HOẠT ĐỘNG (BẢNG DUY NHẤT)
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
    loadGiaoCaList();
    refreshActivities(handleResetActivities);
};

const headersActivities = [
    { text: 'STT', width: '60px', align: 'center' },
    { text: 'Thời gian', width: '170px', align: 'center' },
    { text: 'Người thực hiện', width: '180px', align: 'start' },
    { text: 'Hành động', width: '220px', align: 'start' },
    { text: 'Đối tượng tác động', width: '280px', align: 'start' },
    { text: 'Phân loại', width: '140px', align: 'center' },
    { text: 'Thao tác', width: '90px', align: 'center' }
];

// Phân loại danh mục hoạt động
const getActionCategory = (hanhDong = '') => {
    const text = String(hanhDong).toLowerCase();
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
// 2. MODAL CHI TIẾT HOẠT ĐỘNG & GIAO CA
// =========================================================================
const detailModal = ref({
    show: false,
    activity: null,
    giaoCa: null,
    loading: false
});

// Tìm thông tin giao ca tương ứng với hoạt động
const findGiaoCaRecord = (activity) => {
    if (!activity) return null;
    const desc = `${activity.hanhDong || ''} ${activity.doiTuong || ''}`.toLowerCase();
    
    // 1. Thử match theo ID mã ca nếu có trong mô tả
    const idMatch = desc.match(/#?gc[a-zA-Z0-9_-]+/i) || desc.match(/ca\s*#?([0-9a-zA-Z-]+)/i);
    if (idMatch && idMatch[1]) {
        const targetId = idMatch[1];
        const found = listGiaoCa.value.find((g) => String(g.id).toLowerCase() === targetId.toLowerCase() || (g.maGiaoCa && g.maGiaoCa.toLowerCase() === targetId.toLowerCase()));
        if (found) return found;
    }

    // 2. Thử match theo tên nhân viên và ngày hoạt động
    if (activity.nguoiThucHien && activity.ngay) {
        const empName = activity.nguoiThucHien.toLowerCase().trim();
        const found = listGiaoCa.value.find((g) => {
            const nvTen = (g.nhanVienTen || '').toLowerCase().trim();
            const nvNhanTen = (g.nhanVienNhanCaTen || '').toLowerCase().trim();
            return nvTen.includes(empName) || nvNhanTen.includes(empName);
        });
        if (found) return found;
    }

    return null;
};

const openDetail = (item) => {
    const cat = getActionCategory(item.hanhDong);
    let matchedGiaoCa = null;
    if (cat.isGiaoCa || String(item.hanhDong).toLowerCase().includes('ca') || String(item.doiTuong).toLowerCase().includes('ca')) {
        matchedGiaoCa = findGiaoCaRecord(item);
    }

    detailModal.value = {
        show: true,
        activity: item,
        giaoCa: matchedGiaoCa,
        loading: false
    };
};

const getChenhLech = (item) => {
    if (!item) return 0;
    const expected = (item.tienBanDau || 0) + (item.tongDoanhThu || 0);
    return (item.tienThucTe || 0) - expected;
};

const getChenhLechColor = (val) => {
    if (val > 0) return 'text-info';
    if (val < 0) return 'text-error';
    return 'text-success';
};

const getStatusColor = (status) => {
    if (status === 'OPEN') return 'success';
    if (status === 'CLOSED') return 'grey';
    return 'primary';
};

const getStatusLabel = (status) => {
    if (status === 'OPEN') return 'Đang mở ca';
    if (status === 'CLOSED') return 'Đã chốt ca';
    return status || '--';
};

const breadcrumbs = [
    { title: 'Quản lý lịch', disabled: false, href: '#' },
    { title: 'Lịch sử hoạt động', disabled: true }
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

        <div class="mb-3"></div>

        <!-- 1. BỘ LỌC HOẠT ĐỘNG -->
        <div class="mb-4">
            <AdminFilter
                title="Bộ lọc lịch sử hoạt động"
                :loading="loadingActivities"
                :is-refreshing="isRefreshingActivities"
                @refresh="onRefreshActivities"
            >
                <v-col cols="12" md="4" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm hoạt động</div>
                    <v-text-field
                        v-model="filtersActivities.search"
                        placeholder="Nhập tên người thực hiện, hành động, đối tượng..."
                        variant="outlined"
                        density="compact"
                        hide-details
                        clearable
                        prepend-inner-icon="mdi-magnify"
                        class="compact-input"
                        @input="handleFilterActivities"
                    />
                </v-col>

                <v-col cols="12" md="4" class="filter-cell">
                    <div class="filter-field-label">Phân loại danh mục</div>
                    <v-select
                        v-model="filtersActivities.category"
                        :items="[
                            { title: 'Tất cả hoạt động', value: 'ALL' },
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

                <v-col cols="12" md="4" class="filter-cell">
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

        <!-- 2. BẢNG NHẬT KÝ HOẠT ĐỘNG DUY NHẤT -->
        <AdminTable
            title="Nhật ký hoạt động hệ thống"
            :headers="headersActivities"
            :items="filteredActivities"
            :loading="loadingActivities"
            :show-add-button="false"
            :total-count="paginationActivities.totalElements"
        >
            <template #row="{ item, index }">
                <tr class="data-row cursor-pointer" @click="openDetail(item)">
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
                            <v-avatar size="30" color="primary-lighten-5" class="mr-2 border">
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
                        <v-tooltip text="Xem chi tiết hoạt động" location="top">
                            <template #activator="{ props }">
                                <v-btn
                                    v-bind="props"
                                    icon="mdi-eye-outline"
                                    size="small"
                                    variant="text"
                                    color="primary"
                                    @click="openDetail(item)"
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

        <!-- 3. MODAL CHI TIẾT HOẠT ĐỘNG & THÔNG TIN GIAO CA -->
        <v-dialog v-model="detailModal.show" max-width="720px" transition="dialog-bottom-transition">
            <v-card v-if="detailModal.activity" class="rounded-xl overflow-hidden elevation-10 border">
                <!-- Header Modal -->
                <v-card-item class="pa-5 bg-slate-900 text-white border-b">
                    <div class="d-flex align-center justify-space-between w-100">
                        <div class="d-flex align-center ga-3">
                            <div class="d-flex align-center justify-center rounded-circle pa-2 bg-white-10 border border-white-20">
                                <v-icon size="24" color="white">{{ getActionCategory(detailModal.activity.hanhDong).icon }}</v-icon>
                            </div>
                            <div>
                                <div class="text-h6 font-weight-bold text-white mb-0">
                                    {{ getActionCategory(detailModal.activity.hanhDong).isGiaoCa ? 'Chi Tiết Hoạt Động Giao Ca' : 'Chi Tiết Nhật Ký Hoạt Động' }}
                                </div>
                                <div class="text-caption text-slate-300">
                                    Ghi nhận lúc {{ detailModal.activity.ngay }}
                                </div>
                            </div>
                        </div>
                        <v-btn icon="mdi-close" variant="text" color="white" density="compact" @click="detailModal.show = false" />
                    </div>
                </v-card-item>

                <!-- Body Modal -->
                <v-card-text class="pa-6 bg-slate-50">
                    <!-- Khối 1: Thông tin nhật ký chung -->
                    <div class="bg-white pa-5 rounded-xl border border-slate-200 mb-5 shadow-xs">
                        <div class="d-flex align-center justify-space-between mb-4 pb-2 border-b">
                            <div class="d-flex align-center ga-2">
                                <v-icon size="18" color="primary">mdi-information-outline</v-icon>
                                <span class="text-subtitle-2 font-weight-bold text-slate-800">Thông tin thực hiện</span>
                            </div>
                            <v-chip
                                size="small"
                                :color="getActionCategory(detailModal.activity.hanhDong).color"
                                variant="flat"
                                class="font-weight-bold"
                            >
                                {{ getActionCategory(detailModal.activity.hanhDong).label }}
                            </v-chip>
                        </div>

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
                                <div class="text-caption text-slate-500 font-weight-medium">Thời gian ghi nhận:</div>
                                <div class="font-weight-bold text-body-2 text-slate-800 mt-1">
                                    {{ detailModal.activity.ngay }}
                                </div>
                            </v-col>

                            <v-col cols="12" sm="6" class="mb-2">
                                <div class="text-caption text-slate-500 font-weight-medium">Hành động:</div>
                                <div class="font-weight-bold text-body-2 text-primary mt-1">
                                    {{ detailModal.activity.hanhDong }}
                                </div>
                            </v-col>

                            <v-col cols="12" sm="6" class="mb-2">
                                <div class="text-caption text-slate-500 font-weight-medium">Đối tượng tác động:</div>
                                <div class="font-weight-bold text-body-2 text-slate-700 mt-1">
                                    {{ detailModal.activity.doiTuong }}
                                </div>
                            </v-col>
                        </v-row>
                    </div>

                    <!-- Khối 2: Nếu là hoạt động Giao ca / Bàn giao ca (hoặc có dữ liệu ca liên quan) -->
                    <div v-if="detailModal.giaoCa || getActionCategory(detailModal.activity.hanhDong).isGiaoCa" class="bg-white pa-5 rounded-xl border border-slate-200 shadow-xs">
                        <div class="d-flex align-center justify-space-between mb-4 pb-2 border-b">
                            <div class="d-flex align-center ga-2">
                                <v-icon size="18" color="primary">mdi-cash-register</v-icon>
                                <span class="text-subtitle-2 font-weight-bold text-slate-800">Chi tiết số liệu ca làm việc</span>
                            </div>
                            <v-chip
                                v-if="detailModal.giaoCa"
                                size="small"
                                :color="getStatusColor(detailModal.giaoCa.trangThai)"
                                class="font-weight-bold"
                                variant="flat"
                            >
                                {{ getStatusLabel(detailModal.giaoCa.trangThai) }}
                            </v-chip>
                        </div>

                        <div v-if="detailModal.giaoCa">
                            <!-- Nhân viên mở & chốt ca -->
                            <v-row dense class="mb-3">
                                <v-col cols="12" sm="6">
                                    <div class="pa-3 bg-slate-50 rounded-lg border">
                                        <div class="text-caption text-slate-500">Nhân viên mở ca:</div>
                                        <div class="font-weight-bold text-body-2 text-slate-800 mt-1">
                                            {{ detailModal.giaoCa.nhanVienTen || 'N/A' }}
                                        </div>
                                        <div class="text-caption text-slate-500 mt-1">
                                            Mở lúc: {{ formatDateTime(detailModal.giaoCa.thoiGianMoCa) }}
                                        </div>
                                    </div>
                                </v-col>

                                <v-col cols="12" sm="6">
                                    <div class="pa-3 bg-slate-50 rounded-lg border">
                                        <div class="text-caption text-slate-500">Nhân viên nhận / chốt ca:</div>
                                        <div class="font-weight-bold text-body-2 text-slate-800 mt-1">
                                            {{ detailModal.giaoCa.nhanVienNhanCaTen || 'Chưa bàn giao' }}
                                        </div>
                                        <div class="text-caption text-slate-500 mt-1">
                                            Chốt lúc: {{ formatDateTime(detailModal.giaoCa.thoiGianChotCa) || '--' }}
                                        </div>
                                    </div>
                                </v-col>
                            </v-row>

                            <!-- 4 Thẻ tài chính đối soát -->
                            <v-row dense>
                                <v-col cols="6" sm="3">
                                    <div class="pa-3 rounded-lg border bg-blue-lighten-5 text-center">
                                        <div class="text-caption text-slate-600 font-weight-medium">Tiền đầu ca</div>
                                        <div class="text-subtitle-2 font-weight-bold text-primary mt-1">
                                            {{ formatCurrency(detailModal.giaoCa.tienBanDau) }}
                                        </div>
                                    </div>
                                </v-col>

                                <v-col cols="6" sm="3">
                                    <div class="pa-3 rounded-lg border bg-emerald-lighten-5 text-center">
                                        <div class="text-caption text-slate-600 font-weight-medium">Doanh thu ca</div>
                                        <div class="text-subtitle-2 font-weight-bold text-success mt-1">
                                            {{ formatCurrency(detailModal.giaoCa.tongDoanhThu) }}
                                        </div>
                                    </div>
                                </v-col>

                                <v-col cols="6" sm="3">
                                    <div class="pa-3 rounded-lg border bg-amber-lighten-5 text-center">
                                        <div class="text-caption text-slate-600 font-weight-medium">Tiền thực tế</div>
                                        <div class="text-subtitle-2 font-weight-bold text-amber-darken-3 mt-1">
                                            {{ formatCurrency(detailModal.giaoCa.tienThucTe) }}
                                        </div>
                                    </div>
                                </v-col>

                                <v-col cols="6" sm="3">
                                    <div class="pa-3 rounded-lg border bg-slate-50 text-center">
                                        <div class="text-caption text-slate-600 font-weight-medium">Chênh lệch</div>
                                        <div :class="['text-subtitle-2 font-weight-bold mt-1', getChenhLechColor(getChenhLech(detailModal.giaoCa))]">
                                            {{ getChenhLech(detailModal.giaoCa) > 0 ? '+' : '' }}{{ formatCurrency(getChenhLech(detailModal.giaoCa)) }}
                                        </div>
                                    </div>
                                </v-col>
                            </v-row>

                            <!-- Ghi chú nếu có -->
                            <div v-if="detailModal.giaoCa.ghiChu" class="mt-3 pa-3 bg-amber-lighten-5 rounded-lg border border-amber-200">
                                <div class="text-caption font-weight-bold text-amber-darken-3 mb-1">Ghi chú giao ca:</div>
                                <div class="text-caption text-slate-800">{{ detailModal.giaoCa.ghiChu }}</div>
                            </div>
                        </div>

                        <!-- Nếu không tìm thấy record giao ca chi tiết -->
                        <div v-else class="text-center py-4 text-slate-500 text-caption">
                            <v-icon size="24" color="slate-400" class="mb-1">mdi-alert-circle-outline</v-icon>
                            <div>Hoạt động được đồng bộ tự động từ hệ thống POS / Chấm công.</div>
                        </div>
                    </div>
                </v-card-text>

                <!-- Footer Modal -->
                <v-card-actions class="pa-4 bg-white border-t d-flex justify-end">
                    <v-btn color="primary" variant="flat" class="text-none px-6 font-weight-bold rounded-lg" @click="detailModal.show = false">
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
