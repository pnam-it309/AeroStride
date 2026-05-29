<script setup>
import { ref, onMounted, watch } from 'vue';
import { PATH } from '@/router/routePaths';
import { useRouter } from 'vue-router';
import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';
import { ReceiptIcon } from 'vue-tabler-icons';

// Reusable Components
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import TableEmptyState from '@/components/common/TableEmptyState.vue';
import { downloadFile } from '@/utils/fileUtils';
import { ADMIN_ICONS } from '@/constants/adminIcons';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import { useAdminTable } from '@/composables/useAdminTable';
import { formatCurrency, formatDate, formatDateTime } from '@/utils/formatters';
import { getOrderStatusMeta } from '@/utils/orderStatus';
import { ORDER_TYPES, ORDER_TYPE_OPTIONS } from '@/constants/appConstants';

const router = useRouter();
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
        loaiDon: p.loaiDon || undefined,
        ...(nTrangThai !== null ? { trangThai: nTrangThai } : {})
    };
    const res = await dichVuHoaDon.layHoaDonPhanTrang(params);
    await loadCounts();

    // Trả về đúng cấu trúc kèm tổng số bản ghi để hiển thị các nút phân trang
    return {
        content: Array.isArray(res) ? res : (res?.data || res?.content || []),
        totalElements: counts.value.all || 0
    };
}, {
    search: '',
    trangThai: TAB_ALL,
    loaiDon: null,
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
    pendingPayment: 0,
    processing: 0,
    shipped: 0,
    delivered: 0,
    cancelled: 0,
    refunded: 0
});
const showOrderDetailDialog = ref(false);
const selectedOrder = ref(null);

const tableHeaders = [
    { text: 'STT', align: 'center', width: '50px' },
    { text: 'Mã hóa đơn', width: '100px' },
    { text: 'Khách hàng', width: '130px' },
    { text: 'Mã nhân viên', width: '110px' },
    { text: 'Số điện thoại', width: '110px' },
    { text: 'Loại hóa đơn', width: '100px' },
    { text: 'Tổng tiền', width: '90px' },
    { text: 'Trạng thái', width: '120px' },
    { text: 'Hành động', width: '100px' }
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
            pendingConfirmation: data['0'] || 0,
            confirmed: data['1'] || 0,
            waitingDelivery: data['2'] || 0,
            delivering: data['3'] || 0,
            completed: data['4'] || 0,
            cancelled: data['5'] || 0,
            returned: data['6'] || 0
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
            loaiDon: filters.value.loaiDon || undefined,
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

    if (normalizedType.includes(ORDER_TYPES.ONLINE) || normalizedType.includes('TRUC TUYEN')) {
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

    if (normalizedType.includes(ORDER_TYPES.ONLINE) || normalizedType.includes('TRUC TUYEN')) {
        return 'Trực tuyến';
    }

    return 'Cửa hàng';
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
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý bán hàng', disabled: false, href: '#' },
            { title: 'Hóa đơn', disabled: true }
        ]" />

        <div class="mb-2"></div>

        <div class="filter-shell invoice-filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="4">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field v-model="filters.search" placeholder="Tìm theo mã hóa đơn / khách hàng"
                        persistent-placeholder variant="outlined" density="compact" hide-details
                        prepend-inner-icon="mdi-magnify" class="compact-input search-field"
                        @input="handleSearch"></v-text-field>
                </v-col>

                <v-col cols="12" md="2">
                    <div class="filter-field-label">Loại đơn</div>
                    <v-select v-model="filters.loaiDon" :items="ORDER_TYPE_OPTIONS" item-title="title"
                        item-value="value" variant="outlined" density="compact" hide-details
                        class="compact-input loai-don-field" @update:model-value="handleSearch"></v-select>
                </v-col>



                <v-col cols="12" md="2">
                    <div class="filter-field-label">Từ ngày</div>
                    <v-text-field ref="fromDateFieldRef" v-model="filters.fromDate" type="date" variant="outlined"
                        density="compact" class="compact-input date-field"
                        append-inner-icon="mdi-calendar-month-outline" hide-details
                        @click:append-inner="openFromDatePicker" @input="handleSearch"></v-text-field>
                </v-col>

                <v-col cols="12" md="2">
                    <div class="filter-field-label">Đến ngày</div>
                    <v-text-field ref="toDateFieldRef" v-model="filters.toDate" type="date" variant="outlined"
                        density="compact" class="compact-input date-field"
                        append-inner-icon="mdi-calendar-month-outline" hide-details
                        @click:append-inner="openToDatePicker" @input="handleSearch"></v-text-field>
                </v-col>
            </AdminFilter>
        </div>

        <AdminTable class="balanced-table" title="Danh sách hóa đơn" :showAddButton="false" show-export-button
            :headers="tableHeaders" :items="orders" :total-count="pagination.totalElements" :loading="loading"
            @export="handleExport">
            <template #extra-actions>
                <v-btn icon variant="tonal" color="primary" class="rounded-md mr-3" size="36"
                    @click="filters.sortDirection = filters.sortDirection === 'DESC' ? 'ASC' : 'DESC'; handleSearch()">
                    <v-icon size="20">{{ filters.sortDirection === 'DESC' ? 'mdi-sort-clock-descending-outline' :
                        'mdi-sort-clock-ascending-outline' }}</v-icon>
                    <v-tooltip activator="parent" location="top">
                        {{ filters.sortDirection === 'DESC' ? 'Đang sắp xếp: Mới nhất' : 'Đang sắp xếp: Cũ nhất' }}
                    </v-tooltip>
                </v-btn>
            </template>
            <template #top>
                <v-tabs v-model="filters.trangThai" bg-color="transparent" color="primary" grow
                    class="equal-tabs admin-tabs" @update:model-value="handleTabChange" height="54">
                    <v-tab :value="TAB_ALL" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-view-grid-outline</v-icon>
                        Tất cả
                    </v-tab>
                    <v-tab :value="0" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-progress-clock</v-icon>
                        Chờ xác nhận
                    </v-tab>
                    <v-tab :value="1" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-check-circle-outline</v-icon>
                        Đã xác nhận
                    </v-tab>
                    <v-tab :value="2" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-package-variant-closed</v-icon>
                        Chờ giao
                    </v-tab>
                    <v-tab :value="3" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-truck-fast-outline</v-icon>
                        Đang giao
                    </v-tab>
                    <v-tab :value="4" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-checkbox-marked-circle-outline</v-icon>
                        Hoàn thành
                    </v-tab>
                    <v-tab :value="5" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-close-circle-outline</v-icon>
                        Hủy
                    </v-tab>
                    <v-tab :value="6" class="text-none px-2 tab-item">
                        <v-icon start size="16">mdi-cash-refund</v-icon>
                        Hoàn đơn
                    </v-tab>
                </v-tabs>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center text-slate-500">{{ getRowNumber(index) }}</td>

                    <td class="data-cell text-center">
                        <div class="text-truncate" :title="item.maHoaDon">{{ item.maHoaDon }}</div>
                    </td>

                    <td class="data-cell">
                        <div class="text-truncate" :title="item.tenKhachHang || 'Khách vãng lai'">
                            {{ item.tenKhachHang || 'Khách vãng lai' }}
                        </div>
                    </td>

                    <td class="data-cell text-center">
                        <div class="text-truncate"
                            :title="item.maNhanVien || item.maNV || item.tenNhanVien || 'Hệ thống'">
                            {{ item.maNhanVien || item.maNV || item.tenNhanVien || 'Hệ thống' }}
                        </div>
                    </td>

                    <td class="data-cell text-center">
                        <div class="text-truncate" :title="item.soDienThoai || 'N/A'">{{ item.soDienThoai || 'N/A' }}
                        </div>
                    </td>

                    <td class="data-cell">
                        <v-chip :class="['status-chip', getOrderTypeClass(item.loaiDon)]" variant="flat">
                            {{ getOrderTypeLabel(item.loaiDon) }}
                        </v-chip>
                    </td>

                    <td class="data-cell price-value">
                        <div class="text-primary">
                            {{ formatCurrency(item.tongTienSauGiam || item.tongTien) }}
                        </div>
                    </td>

                    <td class="data-cell status-cell">
                        <template v-if="getStatusMeta(item.trangThai)">
                            <v-chip :class="['status-chip', getStatusMeta(item.trangThai).chipClass]" variant="flat">
                                <v-icon start size="16">{{ getStatusMeta(item.trangThai).icon }}</v-icon>
                                {{ getStatusMeta(item.trangThai).text }}
                            </v-chip>
                        </template>
                        <template v-else>
                            <span class="text-medium-emphasis">—</span>
                        </template>
                    </td>

                    <td class="data-cell action-cell">
                        <div class="d-flex align-center justify-center action-group action-controls">
                            <v-btn icon variant="text" size="28" color="slate-600" class="rounded-lg action-icon-btn"
                                @click.stop="viewOrderDetail(item)">
                                <component :is="ADMIN_ICONS.ACTION.VIEW" size="15" />
                                <v-tooltip activator="parent" location="top">Xem chi tiết</v-tooltip>
                            </v-btn>
                        </div>
                    </td>
                </tr>
            </template>

            <template #pagination>
                <AdminPagination v-model="pagination.page" :page-size="pagination.size"
                    @update:pageSize="pagination.size = $event" @update:page-size="pagination.size = $event"
                    :total-pages="pagination.totalPages" :total-elements="pagination.totalElements"
                    :current-size="orders.length" @change="loadOrders" />
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
                    <v-chip v-if="getStatusMeta(selectedOrder.trangThai)"
                        :class="['px-6 status-chip', getStatusMeta(selectedOrder.trangThai).chipClass]" variant="flat">
                        <v-icon start size="18">{{ getStatusMeta(selectedOrder.trangThai).icon }}</v-icon>
                        {{ getStatusMeta(selectedOrder.trangThai).text }}
                    </v-chip>
                    <v-chip v-else color="grey" variant="flat" class="font-weight-bold px-6">—</v-chip>
                </v-card-title>

                <v-card-text class="pa-6">
                    <v-row class="mb-6">
                        <v-col cols="12" md="6">
                            <div class="text-overline text-medium-emphasis font-weight-medium mb-2">Thông tin khách hàng
                            </div>
                            <p class="mb-1 text-subtitle-1">
                                Họ tên: <span class="font-weight-medium">{{ selectedOrder.tenKhachHang || 'Khách lẻ'
                                    }}</span>
                            </p>
                            <p class="text-subtitle-1">
                                Số điện thoại: <span class="font-weight-medium">{{ selectedOrder.soDienThoai }}</span>
                            </p>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="text-overline text-medium-emphasis font-weight-medium mb-2">Thông tin thanh toán
                            </div>
                            <p class="mb-1 text-subtitle-1">
                                Ngày mua: <span class="font-weight-medium">{{ formatDate(selectedOrder.ngayTao)
                                    }}</span>
                            </p>
                            <p class="text-h5 font-weight-medium text-error">Tổng tiền: {{
                                formatCurrency(selectedOrder.tongTien) }}</p>
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
                                <td class="pa-3 text-end border-b font-weight-medium">{{ formatCurrency(item.price *
                                    item.quantity) }}</td>
                            </tr>
                            <TableEmptyState v-if="!selectedOrder.items || selectedOrder.items.length === 0"
                                :colspan="4" text="Không có sản phẩm nào trong hóa đơn." />
                        </tbody>
                    </table>
                </v-card-text>

                <v-card-actions class="pa-4 bg-grey-lighten-4">
                    <v-spacer></v-spacer>
                    <v-btn variant="text" class="text-none font-weight-bold"
                        @click="showOrderDetailDialog = false">Thoát</v-btn>
                    <v-btn color="primary" variant="flat" rounded="md" class="px-8 text-none font-weight-bold"> In hóa
                        đơn (PDF)
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-container>
</template>

<style scoped>
:deep(.order-type-online) {
    background: #eff6ff !important;
    color: #1d4ed8 !important;
    border: 1px solid #dbeafe !important;
}

:deep(.order-type-offline) {
    background: #fdf2f8 !important;
    color: #be185d !important;
    border: 1px solid #fce7f3 !important;
}
</style>
