<script setup>
import { ref, onMounted, watch } from 'vue';
import { PATH } from '@/router/routePaths';
import { useRouter } from 'vue-router';
import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';

// Reusable Components
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EyeIcon, ReceiptIcon, PrinterIcon } from 'vue-tabler-icons';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import { useAdminTable } from '@/composables/useAdminTable';
import { formatCurrency, formatDate, formatDateTime } from '@/utils/formatters';
import { getOrderStatusMeta } from '@/utils/orderStatus';

const router = useRouter();
const fromDateFieldRef = ref(null);
const toDateFieldRef = ref(null);
const TAB_ALL = 'ALL';

const getTodayDate = () => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
};

const normalizeTrangThai = (value) => {
    if (value === null || value === undefined || value === '' || value === 'null' || value === 'all' || value === TAB_ALL) {
        return null;
    }
    const numericValue = Number(value);
    return Number.isFinite(numericValue) ? numericValue : null;
};

const {
    items: orders,
    loading,
    pagination,
    filters,
    loadData: loadOrders,
    handleFilter: handleSearch,
    handleReset
} = useAdminTable(async (p) => {
    const nTrangThai = normalizeTrangThai(p.trangThai);
    const params = {
        page: p.page,
        size: p.size,
        search: p.search || undefined,
        tuNgay: p.fromDate || undefined,
        denNgay: p.toDate || undefined,
        sortDirection: p.sortDirection,
        ...(nTrangThai !== null ? { trangThai: nTrangThai } : {})
    };
    const res = await dichVuHoaDon.layHoaDonPhanTrang(params);
    loadCounts(); 
    return res;
}, {
    search: '',
    trangThai: TAB_ALL,
    fromDate: getTodayDate(),
    toDate: getTodayDate(),
    sortDirection: 'DESC'
});

const sortOptions = [
    { title: 'Mới nhất', value: 'DESC' },
    { title: 'Cũ nhất', value: 'ASC' }
];

const isRefreshing = ref(false);
const counts = ref({
    all: 0,
    choThanhToan: 0,
    choXacNhan: 0,
    choGiaoHang: 0,
    dangVanChuyen: 0,
    daGiaoHang: 0,
    daThanhToan: 0,
    hoanThanh: 0,
    daHuy: 0
});
const showOrderDetailDialog = ref(false);
const selectedOrder = ref(null);

const tableHeaders = [
    { text: 'STT', align: 'center', width: '50px' },
    { text: 'Mã hóa đơn', align: 'left', width: '100px' },
    { text: 'Khách hàng', align: 'left', width: '130px' },
    { text: 'Mã nhân viên', align: 'left', width: '110px' },
    { text: 'Tên nhân viên', align: 'left', width: '130px' },
    { text: 'Số điện thoại', align: 'left', width: '100px' },
    { text: 'Loại hóa đơn', align: 'center', width: '110px' },
    { text: 'Loại thanh toán', align: 'left', width: '140px' },
    { text: 'Tổng tiền', align: 'left', width: '100px' },
    { text: 'Trạng thái', align: 'center', width: '160px' },
    { text: 'Hành động', align: 'center', width: '90px' }
];

const loadCounts = async () => {
    try {
        const params = {
            search: filters.value.search || undefined,
            tuNgay: filters.value.fromDate || undefined,
            denNgay: filters.value.toDate || undefined
        };
        const data = await dichVuHoaDon.laySoLuongHoaDon(params);
        counts.value = {
            all: data.all || 0,
            choThanhToan: data['0'] || 0,
            choXacNhan: data['1'] || 0,
            choGiaoHang: data['2'] || 0,
            dangVanChuyen: data['3'] || 0,
            daGiaoHang: data['4'] || 0,
            daThanhToan: data['5'] || 0,
            hoanThanh: data['6'] || 0,
            daHuy: data['7'] || 0
        };
    } catch (e) {
        console.error('Error counts:', e);
    }
};



const handleRefresh = async () => {
    isRefreshing.value = true;
    handleReset();
    setTimeout(() => (isRefreshing.value = false), 800);
};

const handleTabChange = async (value) => {
    filters.value.trangThai = value ?? TAB_ALL;
    handleSearch();
};

watch(
    () => [filters.value.search, filters.value.fromDate, filters.value.toDate, filters.value.sortDirection],
    () => handleSearch()
);

const handleExport = async () => {
    try {
        const normalizedTrangThai = normalizeTrangThai(filters.value.trangThai);
        const params = {
            search: filters.value.search || undefined,
            tuNgay: filters.value.fromDate || undefined,
            denNgay: filters.value.toDate || undefined,
            sortDirection: filters.value.sortDirection,
            ...(normalizedTrangThai !== null ? { trangThai: normalizedTrangThai } : {})
        };
        const blob = await dichVuHoaDon.xuatExcelHoaDon(params);
        downloadFile(blob, 'danh_sach_hoa_don.xlsx');
    } catch (error) {
        console.error('Lỗi xuất Excel:', error);
    }
};



const getRowNumber = (index) => (pagination.value.page - 1) * pagination.value.size + index + 1;

const openNativeDatePicker = (fieldRef) => {
    const input = fieldRef?.$el?.querySelector("input[type='date']");
    if (!input) return;
    input.focus();
    if (typeof input.showPicker === 'function') {
        input.showPicker();
        return;
    }
    input.click();
};

const openFromDatePicker = () => openNativeDatePicker(fromDateFieldRef.value);
const openToDatePicker = () => openNativeDatePicker(toDateFieldRef.value);

const hasCount = (value) => Number(value) > 0;

const getOrderTypeClass = (orderType) => {
    const normalizedType = String(orderType || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toUpperCase()
        .trim();

    if (normalizedType.includes('ONLINE') || normalizedType.includes('TRUC TUYEN')) {
        return 'order-type-online';
    }

    return 'order-type-offline';
};

const getOrderTypeLabel = (orderType) => {
    const normalizedType = String(orderType || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toUpperCase()
        .trim();

    if (normalizedType.includes('ONLINE') || normalizedType.includes('TRUC TUYEN')) {
        return 'ONLINE';
    }

    return 'OFFLINE';
};

const getPaymentLabel = (item) => item.tenLoaiThanhToan || item.loaiThanhToan || item.hinhThucThanhToan || 'Chưa cập nhật';

const handlePrint = async (orderId) => {
    try {
        const html = await dichVuHoaDon.inHoaDon(orderId);
        // Mở cửa sổ mới để in
        const printWindow = window.open('', '_blank', 'width=900,height=800');
        printWindow.document.write(html);
        printWindow.document.close();
        // printWindow.print(); // Sẽ in ngay khi load xong nếu đã có script trong HTML
    } catch (error) {
        console.error('Lỗi in hóa đơn:', error);
    }
};


const getStatusMeta = (s) => getOrderStatusMeta(s);

const viewOrderDetail = (order) => {
    router.push(`${PATH.HOA_DON_CHI_TIET}/${order.id}`);
};

onMounted(() => loadOrders());
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body" style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important;">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs
            :items="[
                { title: 'Quản lý bán hàng', disabled: false, href: '#' },
                { title: 'Hóa đơn', disabled: true }
            ]"
        />

        <div class="mb-2"></div>

        <div class="filter-top invoice-filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="2">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field
                        v-model="filters.search"
                        placeholder="Tìm mã hóa đơn / khách hàng"
                        persistent-placeholder
                        variant="outlined"
                        density="compact"
                        hide-details
                        prepend-inner-icon="mdi-magnify"
                        class="search-field"
                        @input="handleSearch"
                    ></v-text-field>
                </v-col>

                <v-col cols="12" md="3">
                    <div class="filter-field-label">Hiển thị</div>
                    <v-select
                        v-model="filters.sortDirection"
                        :items="sortOptions"
                        item-title="title"
                        item-value="value"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="sort-field"
                        @update:model-value="handleSearch"
                    >
                        <template v-slot:prepend-inner>
                            <v-icon size="20" color="primary">mdi-sort</v-icon>
                        </template>
                    </v-select>
                </v-col>

                <v-col cols="12" md="3">
                    <div class="filter-field-label">Từ ngày</div>
                    <v-text-field
                        ref="fromDateFieldRef"
                        v-model="filters.fromDate"
                        type="date"
                        variant="outlined"
                        density="compact"
                        class="date-field"
                        append-inner-icon="mdi-calendar-month-outline"
                        persistent-placeholder
                        hide-details
                        @click:append-inner="openFromDatePicker"
                        @input="handleSearch"
                    ></v-text-field>
                </v-col>

                <v-col cols="12" md="3">
                    <div class="filter-field-label">Đến ngày</div>
                    <v-text-field
                        ref="toDateFieldRef"
                        v-model="filters.toDate"
                        type="date"
                        variant="outlined"
                        density="compact"
                        class="date-field"
                        append-inner-icon="mdi-calendar-month-outline"
                        hide-details
                        @click:append-inner="openToDatePicker"
                        @input="handleSearch"
                    ></v-text-field>
                </v-col>
            </AdminFilter>
        </div>

        <AdminTable
            title="Danh sách hóa đơn"
            :showAddButton="false"
            show-export-button
            :headers="tableHeaders"
            :items="orders"
            :total-count="pagination.totalElements"
            :loading="loading"
            @export="handleExport"
        >
            <template #top>
                <v-tabs
                    v-model="filters.trangThai"
                    bg-color="transparent"
                    color="primary"
                    grow
                    class="equal-tabs admin-tabs"
                    @update:model-value="handleTabChange"
                    height="54"
                >
                    <v-tab :value="TAB_ALL" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-view-grid-outline</v-icon>
                        Tất cả
                        <v-avatar
                            v-if="hasCount(counts.all)"
                            size="22"
                            class="ml-2 tab-count-avatar"
                        >
                            {{ counts.all }}
                        </v-avatar>
                    </v-tab>
                    <v-tab :value="0" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-cash-clock</v-icon>
                        Chờ thanh toán
                        <v-avatar v-if="hasCount(counts.choThanhToan)" size="22" class="ml-2 tab-count-avatar">
                            {{ counts.choThanhToan }}
                        </v-avatar>
                    </v-tab>
                    <v-tab :value="1" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-progress-clock</v-icon>
                        Chờ xác nhận
                        <v-avatar v-if="hasCount(counts.choXacNhan)" size="22" class="ml-2 tab-count-avatar">
                            {{ counts.choXacNhan }}
                        </v-avatar>
                    </v-tab>
                    <v-tab :value="2" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-truck-delivery-outline</v-icon>
                        Chờ giao hàng
                        <v-avatar v-if="hasCount(counts.choGiaoHang)" size="22" class="ml-2 tab-count-avatar">
                            {{ counts.choGiaoHang }}
                        </v-avatar>
                    </v-tab>
                    <v-tab :value="3" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-truck-fast-outline</v-icon>
                        Đang vận chuyển
                        <v-avatar v-if="hasCount(counts.dangVanChuyen)" size="22" class="ml-2 tab-count-avatar">
                            {{ counts.dangVanChuyen }}
                        </v-avatar>
                    </v-tab>
                    <v-tab :value="4" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-package-variant-closed-check</v-icon>
                        Đã giao hàng
                        <v-avatar v-if="hasCount(counts.daGiaoHang)" size="22" class="ml-2 tab-count-avatar">
                            {{ counts.daGiaoHang }}
                        </v-avatar>
                    </v-tab>
                    <v-tab :value="5" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-currency-usd</v-icon>
                        Đã thanh toán
                        <v-avatar v-if="hasCount(counts.daThanhToan)" size="22" class="ml-2 tab-count-avatar">
                            {{ counts.daThanhToan }}
                        </v-avatar>
                    </v-tab>
                    <v-tab :value="6" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-checkbox-marked-circle-outline</v-icon>
                        Hoàn thành
                        <v-avatar v-if="hasCount(counts.hoanThanh)" size="22" class="ml-2 tab-count-avatar">
                            {{ counts.hoanThanh }}
                        </v-avatar>
                    </v-tab>
                    <v-tab :value="7" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-close-circle-outline</v-icon>
                        Đã hủy
                        <v-avatar v-if="hasCount(counts.daHuy)" size="22" class="ml-2 tab-count-avatar">
                            {{ counts.daHuy }}
                        </v-avatar>
                    </v-tab>
                </v-tabs>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell stt-cell text-slate-400">{{ getRowNumber(index) }}</td>

                    <td class="data-cell col-left-tight">
                        <div class="text-caption text-dark">{{ item.maHoaDon }}</div>
                    </td>

                    <td class="data-cell col-left-tight">
                        <div class="text-dark">{{ item.tenKhachHang || 'Khách vãng lai' }}</div>
                    </td>

                    <td class="data-cell">
                        <div class="text-dark">{{ item.maNhanVien || 'N/A' }}</div>
                    </td>

                    <td class="data-cell">
                        <div class="text-dark">{{ item.tenNhanVien || 'Hệ thống' }}</div>
                    </td>

                    <td class="data-cell col-left-tight">
                        <div class="text-dark">{{ item.soDienThoai || 'N/A' }}</div>
                    </td>

                    <td class="data-cell text-center">
                        <v-chip
                            :class="['status-chip', getOrderTypeClass(item.loaiDon)]"
                            variant="flat"
                            size="small"
                        >
                            {{ getOrderTypeLabel(item.loaiDon) }}
                        </v-chip>
                    </td>

                    <td class="data-cell">
                        <div class="text-dark">{{ getPaymentLabel(item) }}</div>
                    </td>

                    <td class="data-cell price-value">
                        <div class="text-dark font-weight-medium">
                            {{ formatCurrency(item.tongTienSauGiam || item.tongTien) }}
                        </div>
                    </td>

                    <td class="data-cell status-cell text-center">
                        <template v-if="getStatusMeta(item.trangThai)">
                            <v-chip
                                :class="['status-chip', getStatusMeta(item.trangThai).chipClass]"
                                variant="flat"
                                size="small"
                            >
                                <v-icon start size="16">{{ getStatusMeta(item.trangThai).icon }}</v-icon>
                                {{ getStatusMeta(item.trangThai).text }}
                            </v-chip>
                        </template>
                        <template v-else>
                            <span class="text-caption text-medium-emphasis">—</span>
                        </template>
                    </td>



                    <td class="data-cell action-cell" style="text-align: center">
                        <div class="d-flex align-center justify-center action-group action-controls">
                            <v-btn
                                icon
                                variant="text"
                                size="28"
                                color="slate-700"
                                class="rounded-md action-icon-btn"
                                @click.stop="viewOrderDetail(item)"
                            >
                                <EyeIcon size="15" />
                                <v-tooltip activator="parent" location="top">Xem chi tiết</v-tooltip>
                            </v-btn>
                        </div>
                    </td>
                </tr>
            </template>

            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    :page-size="pagination.size"
                    @update:pageSize="pagination.size = $event"
                    :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements"
                    :current-size="orders.length"
                    @change="loadOrders"
                />
            </template>
        </AdminTable>

        <!-- Detail Dialog -->
        <v-dialog v-model="showOrderDetailDialog" max-width="850" transition="dialog-bottom-transition">
            <v-card class="rounded-md overflow-hidden" v-if="selectedOrder">
                <v-card-title class="pa-5 d-flex justify-space-between align-center border-b bg-grey-lighten-4">
                    <div class="d-flex align-center">
                        <ReceiptIcon size="24" class="mr-3 text-primary" />
                        <span class="font-weight-medium">Chi tiết hóa đơn #{{ selectedOrder.maHoaDon }}</span>
                    </div>
                    <v-chip
                        v-if="getStatusMeta(selectedOrder.trangThai)"
                        :color="getStatusMeta(selectedOrder.trangThai).color"
                        variant="flat"
                        class="font-weight-medium px-6"
                    >
                        <v-icon start size="18">{{ getStatusMeta(selectedOrder.trangThai).icon }}</v-icon>
                        {{ getStatusMeta(selectedOrder.trangThai).text }}
                    </v-chip>
                    <v-chip v-else color="grey" variant="flat" class="font-weight-medium px-6">—</v-chip>
                </v-card-title>

                <v-card-text class="pa-6">
                    <v-row class="mb-6">
                        <v-col cols="12" md="6">
                            <div class="text-overline text-medium-emphasis font-weight-medium mb-2">Thông tin khách hàng</div>
                            <p class="mb-1 text-subtitle-1">
                                Họ tên: <span class="font-weight-medium">{{ selectedOrder.tenKhachHang || 'Khách lẻ' }}</span>
                            </p>
                            <p class="text-subtitle-1">
                                Số điện thoại: <span class="font-weight-medium">{{ selectedOrder.soDienThoai }}</span>
                            </p>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="text-overline text-medium-emphasis font-weight-medium mb-2">Thông tin thanh toán</div>
                            <p class="mb-1 text-subtitle-1">
                                Ngày mua: <span class="font-weight-medium">{{ formatDate(selectedOrder.ngayTao) }}</span>
                            </p>
                            <p class="text-h5 font-weight-medium text-error">Tổng tiền: {{ formatCurrency(selectedOrder.tongTien) }}</p>
                        </v-col>
                    </v-row>

                    <v-divider class="mb-6"></v-divider>

                    <div class="text-overline text-medium-emphasis font-weight-medium mb-4">Danh sách sản phẩm</div>
                    <table class="w-100 invoice-table border">
                        <thead class="bg-grey-lighten-4">
                            <tr>
                                <th class="pa-3 text-start border-b">Tên sản phẩm</th>
                                <th class="pa-3 text-center border-b">Số lượng</th>
                                <th class="pa-3 text-end border-b">Đơn giá</th>
                                <th class="pa-3 text-end border-b">Thành tiền</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="item in selectedOrder.items" :key="item.name">
                                <td class="pa-3 border-b">{{ item.name }}</td>
                                <td class="pa-3 text-center border-b font-weight-bold">{{ item.quantity }}</td>
                                <td class="pa-3 text-end border-b">{{ formatCurrency(item.price) }}</td>
                                <td class="pa-3 text-end border-b font-weight-medium">{{ formatCurrency(item.price * item.quantity) }}</td>
                            </tr>
                        </tbody>
                    </table>
                </v-card-text>

                <v-card-actions class="pa-4 bg-grey-lighten-4">
                    <v-spacer></v-spacer>
                    <v-btn variant="text" class="text-none font-weight-bold" @click="showOrderDetailDialog = false">Thoát</v-btn>
                    <v-btn color="primary" variant="flat" rounded="md" class="px-8 text-none font-weight-bold"> In hóa đơn (PDF) </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-container>
</template>

<style scoped>
.back-btn {
    border-radius: 12px !important;
    width: 42px;
    height: 42px;
}
.filter-top {
    position: sticky;
    top: 8px;
    z-index: 6;
}
.page-title {
    line-height: 1.1;
}
.action-group {
    min-width: 64px;
}
.stt-cell {
    text-align: center !important;
}

/* Hide native date picker icon so it only opens via the custom icon click */
:deep(.date-field input::-webkit-calendar-picker-indicator) {
    display: none;
    -webkit-appearance: none;
}

/* Ensure the date input doesn't trigger native picker on simple click/focus in some browsers */
:deep(.date-field input) {
    position: relative;
}
</style>




