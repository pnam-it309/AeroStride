<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';

// Reusable Components
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EyeIcon, ReceiptIcon, PrinterIcon } from 'vue-tabler-icons';

const router = useRouter();
const TAB_ALL = 'ALL';
const loading = ref(false);
const isRefreshing = ref(false);
const orders = ref([]);
const counts = ref({ all: 0, pending: 0, confirmed: 0, shipping: 0, completed: 0, cancelled: 0 });
const showOrderDetailDialog = ref(false);
const selectedOrder = ref(null);

const filters = ref({
    keyword: '',
    trangThai: TAB_ALL,
    fromDate: '',
    toDate: ''
});

const fromDateFieldRef = ref(null);
const toDateFieldRef = ref(null);

const pagination = ref({
    page: 1,
    size: 5,
    totalElements: 0,
    totalPages: 1
});

const tableHeaders = [
    { text: 'STT', align: 'center', width: '50px' },
    { text: 'Mã hóa đơn', align: 'left', width: '120px' },
    { text: 'Khách hàng', align: 'left', width: '150px' },
    { text: 'Số điện thoại', align: 'left', width: '120px' },
    { text: 'Loại hóa đơn', align: 'left', width: '120px' },
    { text: 'Loại thanh toán', align: 'left', width: '140px' },
    { text: 'Mã nhân viên', align: 'left', width: '140px' },
    { text: 'Tổng tiền', align: 'left', width: '100px' },
    { text: 'Trạng thái', align: 'left', width: '120px' },
    { text: 'Ngày tạo', align: 'left', width: '120px' },
    { text: 'Hành động', align: 'center', width: '90px' }
];

const loadCounts = async () => {
    try {
        const data = await dichVuHoaDon.laySoLuongHoaDon();
        counts.value = {
            all: data.all || 0,
            pending: data['1'] || 0,
            confirmed: data['2'] || 0,
            shipping: data['6'] || 0,
            delivering: data['3'] || 0,
            completed: data['4'] || 0,
            cancelled: data['5'] || 0
        };
    } catch (e) {
        console.error('Error counts:', e);
    }
};

const normalizeTrangThai = (value) => {
    if (value === null || value === undefined || value === '' || value === 'null' || value === 'all' || value === TAB_ALL) {
        return null;
    }
    const numericValue = Number(value);
    return Number.isFinite(numericValue) ? numericValue : null;
};

const loadOrders = async () => {
    loading.value = true;
    try {
        const normalizedTrangThai = normalizeTrangThai(filters.value.trangThai);
        const params = {
            page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
            size: pagination.value.size,
            search: filters.value.keyword || undefined,
            ...(normalizedTrangThai !== null ? { trangThai: normalizedTrangThai } : {})
        };
        const response = await dichVuHoaDon.layHoaDonPhanTrang(params);
        orders.value = response.content || response;
        pagination.value.totalElements = response.totalElements || orders.value.length;
        pagination.value.totalPages = response.totalPages || 1;

        // Tải lại count để số liệu luôn mới
        loadCounts();
    } catch (error) {
        console.error(error);
    } finally {
        loading.value = false;
    }
};

const handleRefresh = async () => {
    isRefreshing.value = true;
    filters.value = { keyword: '', trangThai: TAB_ALL, fromDate: '', toDate: '' };
    pagination.value.page = 1;
    await loadOrders();
    setTimeout(() => (isRefreshing.value = false), 800);
};

const handleTabChange = async (value) => {
    filters.value.trangThai = value ?? TAB_ALL;
    pagination.value.page = 1;
    await loadOrders();
};

let searchTimer = null;
const scheduleSearch = () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
        pagination.value.page = 1;
        loadOrders();
    }, 350);
};

watch(
    () => [filters.value.keyword, filters.value.fromDate, filters.value.toDate],
    () => scheduleSearch()
);

const handleExport = async () => {
    try {
        const normalizedTrangThai = normalizeTrangThai(filters.value.trangThai);
        const params = {
            search: filters.value.keyword || undefined,
            ...(normalizedTrangThai !== null ? { trangThai: normalizedTrangThai } : {})
        };
        const blob = await dichVuHoaDon.xuatExcelHoaDon(params);
        downloadFile(blob, 'danh_sach_hoa_don.xlsx');
    } catch (error) {
        console.error('Lỗi xuất Excel:', error);
    }
};

const formatPrice = (amount) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
};

const formatCurrency = (amount) => formatPrice(amount);

const formatDate = (date) => {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
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

const formatDateTime = (timestamp) => {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
};

const getStatusInfo = (status) => {
    switch (status) {
        case 1:
            return { text: 'Chờ xác nhận', color: 'warning' };
        case 2:
            return { text: 'Đã xác nhận', color: 'info' };
        case 3:
            return { text: 'Đang giao', color: 'primary' };
        case 4:
            return { text: 'Hoàn thành', color: 'success' };
        case 5:
            return { text: 'Đã hủy', color: 'error' };
        default:
            return { text: 'Không xác định', color: 'grey' };
    }
};

const getStatusChipClass = (status) => {
    switch (status) {
        case 1:
            return 'status-chip-pending';
        case 2:
            return 'status-chip-confirmed';
        case 3:
            return 'status-chip-delivering';
        case 4:
            return 'status-chip-completed';
        case 5:
            return 'status-chip-cancelled';
        case 6:
            return 'status-chip-waiting-delivery';
        default:
            return 'status-chip-unknown';
    }
};

const viewOrderDetail = (order) => {
    router.push(`/hoa-don/chi-tiet/${order.id}`);
};

onMounted(() => loadOrders());
</script>

<template>
    <v-container fluid class="pa-4 gray-bg min-h-screen font-body">
        <div class="mb-2">
            <h1 class="page-title text-h5 font-weight-bold text-slate-900 mb-0">Quản lí hóa đơn</h1>
        </div>

        <div class="filter-top invoice-filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="5">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field
                        v-model="filters.keyword"
                        placeholder="Tìm theo mã hóa đơn / khách hàng"
                        persistent-placeholder
                        variant="outlined"
                        density="compact"
                        hide-details
                        prepend-inner-icon="mdi-magnify"
                        class="font-weight-bold search-field"
                        @input="scheduleSearch"
                    ></v-text-field>
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
                        hide-details
                        @click:append-inner="openFromDatePicker"
                        @input="scheduleSearch"
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
                        @input="scheduleSearch"
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
                    class="equal-tabs"
                    @update:model-value="handleTabChange"
                    height="54"
                >
                    <v-tab :value="TAB_ALL" class="text-none font-weight-bold px-2 tab-item">
                        <v-icon start size="16">mdi-view-grid-outline</v-icon>
                        Tất cả
                        <v-chip
                            v-if="hasCount(counts.all)"
                            size="x-small"
                            color="error"
                            class="ml-2 font-weight-bold tab-count-chip"
                            variant="flat"
                        >
                            {{ counts.all }}
                        </v-chip>
                    </v-tab>
                    <v-tab :value="1" class="text-none font-weight-bold px-2 tab-item">
                        <v-icon start size="16">mdi-clock-outline</v-icon>
                        Chờ xác nhận
                        <v-chip
                            v-if="hasCount(counts.pending)"
                            size="x-small"
                            color="error"
                            class="ml-2 font-weight-bold tab-count-chip"
                            variant="flat"
                        >
                            {{ counts.pending }}
                        </v-chip>
                    </v-tab>
                    <v-tab :value="2" class="text-none font-weight-bold px-2 tab-item">
                        <v-icon start size="16">mdi-check-circle-outline</v-icon>
                        Đã xác nhận
                        <v-chip
                            v-if="hasCount(counts.confirmed)"
                            size="x-small"
                            color="error"
                            class="ml-2 font-weight-bold tab-count-chip"
                            variant="flat"
                        >
                            {{ counts.confirmed }}
                        </v-chip>
                    </v-tab>
                    <v-tab :value="6" class="text-none font-weight-bold px-2 tab-item">
                        <v-icon start size="16">mdi-truck-fast-outline</v-icon>
                        Chờ giao
                        <v-chip
                            v-if="hasCount(counts.shipping)"
                            size="x-small"
                            color="error"
                            class="ml-2 font-weight-bold tab-count-chip"
                            variant="flat"
                        >
                            {{ counts.shipping }}
                        </v-chip>
                    </v-tab>
                    <v-tab :value="3" class="text-none font-weight-bold px-2 tab-item">
                        <v-icon start size="16">mdi-truck-delivery-outline</v-icon>
                        Đang giao
                        <v-chip
                            v-if="hasCount(counts.delivering)"
                            size="x-small"
                            color="error"
                            class="ml-2 font-weight-bold tab-count-chip"
                            variant="flat"
                        >
                            {{ counts.delivering }}
                        </v-chip>
                    </v-tab>
                    <v-tab :value="4" class="text-none font-weight-bold px-2 tab-item">
                        <v-icon start size="16">mdi-checkbox-marked-circle-outline</v-icon>
                        Hoàn thành
                        <v-chip
                            v-if="hasCount(counts.completed)"
                            size="x-small"
                            color="error"
                            class="ml-2 font-weight-bold tab-count-chip"
                            variant="flat"
                        >
                            {{ counts.completed }}
                        </v-chip>
                    </v-tab>
                    <v-tab :value="5" class="text-none font-weight-bold px-2 tab-item">
                        <v-icon start size="16">mdi-close-circle-outline</v-icon>
                        Đã hủy
                        <v-chip
                            v-if="hasCount(counts.cancelled)"
                            size="x-small"
                            color="error"
                            class="ml-2 font-weight-bold tab-count-chip"
                            variant="flat"
                        >
                            {{ counts.cancelled }}
                        </v-chip>
                    </v-tab>
                </v-tabs>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell stt-cell">{{ getRowNumber(index) }}</td>

                    <td class="data-cell text-left col-left-tight">
                        <div class="text-caption font-weight-medium text-dark">{{ item.maHoaDon }}</div>
                    </td>

                    <td class="data-cell text-left col-left-tight">
                        <div class="text-caption font-weight-medium text-dark">{{ item.tenKhachHang || 'Khách vãng lai' }}</div>
                    </td>

                    <td class="data-cell col-left-tight">
                        <div class="text-caption font-weight-medium text-dark">{{ item.soDienThoai || 'N/A' }}</div>
                    </td>

                    <td class="data-cell">
                        <v-chip
                            :class="['font-weight-bold mb-1 order-type-chip', getOrderTypeClass(item.loaiDon)]"
                            variant="flat"
                            size="x-small"
                        >
                            {{ getOrderTypeLabel(item.loaiDon) }}
                        </v-chip>
                    </td>

                    <td class="data-cell">
                        <div class="text-caption font-weight-medium text-dark">{{ getPaymentLabel(item) }}</div>
                    </td>

                    <td class="data-cell">
                        <div class="text-caption font-weight-medium text-dark">
                            {{ item.maNhanVien || item.maNV || item.tenNhanVien || 'Hệ thống' }}
                        </div>
                    </td>

                    <td class="data-cell price-value">
                        <div class="text-subtitle-2 font-weight-black total-price-text">
                            {{ formatPrice(item.tongTienSauGiam || item.tongTien) }}
                        </div>
                    </td>

                    <td class="data-cell status-cell">
                        <v-chip
                            :class="['font-weight-bold px-2 status-chip', getStatusChipClass(item.trangThai)]"
                            variant="flat"
                            size="small"
                        >
                            {{ getStatusInfo(item.trangThai).text }}
                        </v-chip>
                    </td>

                    <td class="data-cell">
                        <div class="text-caption font-weight-medium text-dark">{{ formatDateTime(item.ngayTao) }}</div>
                    </td>

                    <td class="data-cell action-cell" style="text-align: center">
                        <div class="d-flex align-center justify-center action-group action-controls">
                            <v-btn
                                icon
                                variant="text"
                                size="28"
                                color="#2aa6a1"
                                class="rounded-lg action-icon-btn"
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
                    @update:page-size="pagination.size = $event"
                    :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements"
                    :current-size="orders.length"
                    @change="loadOrders"
                />
            </template>
        </AdminTable>

        <!-- Detail Dialog -->
        <v-dialog v-model="showOrderDetailDialog" max-width="850">
            <v-card class="rounded-lg overflow-hidden" v-if="selectedOrder">
                <v-card-title class="pa-5 d-flex justify-space-between align-center border-b bg-grey-lighten-4">
                    <div class="d-flex align-center">
                        <ReceiptIcon size="24" class="mr-3 text-primary" />
                        <span class="font-weight-medium">Chi tiết hóa đơn #{{ selectedOrder.maHoaDon }}</span>
                    </div>
                    <v-chip :color="getStatusInfo(selectedOrder.trangThai).color" variant="flat" class="font-weight-bold px-6">
                        {{ getStatusInfo(selectedOrder.trangThai).text }}
                    </v-chip>
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
.gray-bg {
    background-color: #f5f7fb;
}
.text-dark {
    color: #0f172a !important;
}
.font-body {
    font-family: 'Inter', sans-serif;
}
.back-btn {
    border-radius: 12px !important;
    width: 42px;
    height: 42px;
}
.tab-item {
    min-height: 54px;
}
.equal-tabs :deep(.v-slide-group__content) {
    width: 100%;
}
.equal-tabs :deep(.v-tab) {
    flex: 1 1 0;
    max-width: none;
}

.equal-tabs :deep(.v-tab.v-btn) {
    min-height: 46px !important;
    font-size: 14px !important;
    font-weight: 600 !important;
    letter-spacing: 0 !important;
    line-height: 1.1 !important;
}

.equal-tabs :deep(.v-tab .v-btn__content) {
    gap: 4px;
}

.equal-tabs :deep(.v-tab .v-icon) {
    font-size: 14px !important;
}

.equal-tabs :deep(.v-tab .v-chip) {
    font-size: 0.68rem !important;
    min-height: 18px !important;
    padding: 0 6px !important;
}

.equal-tabs :deep(.v-tab .tab-count-chip) {
    background-color: #ff0000 !important;
    color: #ffffff !important;
    opacity: 1 !important;
    border: none !important;
}

.equal-tabs :deep(.v-tab) {
    color: #0f172a !important;
    background: transparent !important;
}

.equal-tabs :deep(.v-tab .v-icon) {
    color: inherit !important;
}

.equal-tabs :deep(.v-tab--selected) {
    color: #1e3a8a !important;
    background: transparent !important;
}

.equal-tabs :deep(.v-tab--selected .tab-count-chip) {
    background-color: #1e3a8a !important;
    color: #ffffff !important;
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
    color: #0f172a !important;
    text-align: left !important;
    padding-left: 6px !important;
}

:deep(.native-admin-table .data-cell .text-subtitle-2),
:deep(.native-admin-table .data-cell .text-caption),
:deep(.native-admin-table .data-cell .text-body-2),
:deep(.native-admin-table .data-cell .text-subtitle-1) {
    font-size: 13px !important;
    font-weight: 600 !important;
    line-height: 1.35 !important;
}

:deep(.native-admin-table .data-cell:nth-child(2)),
:deep(.native-admin-table .data-cell:nth-child(3)),
:deep(.native-admin-table .data-cell:nth-child(4)),
:deep(.native-admin-table .data-cell:nth-child(6)),
:deep(.native-admin-table .data-cell:nth-child(7)),
:deep(.native-admin-table .data-cell:nth-child(10)) {
    color: #0f172a !important;
    font-weight: 700 !important;
}

:deep(.native-admin-table .data-cell.col-left-tight) {
    text-align: left !important;
    padding-left: 6px !important;
}

:deep(.native-admin-table .header-cell:nth-child(2)),
:deep(.native-admin-table .header-cell:nth-child(3)),
:deep(.native-admin-table .header-cell:nth-child(4)) {
    text-align: left !important;
    padding-left: 6px !important;
}

:deep(.native-admin-table .header-cell:nth-child(1)),
:deep(.native-admin-table .data-cell.stt-cell) {
    text-align: center !important;
    padding-left: 0 !important;
}

:deep(.native-admin-table .header-cell:nth-child(9)) {
    text-align: left !important;
    padding-left: 6px !important;
    padding-right: 6px !important;
}

:deep(.native-admin-table .header-cell:nth-child(11)) {
    text-align: center !important;
    padding-left: 6px !important;
    padding-right: 6px !important;
}

:deep(.native-admin-table .data-cell:nth-child(11)) {
    text-align: center !important;
    padding-left: 0 !important;
}

:deep(.native-admin-table .data-cell.status-cell) {
    display: flex !important;
    justify-content: flex-start !important;
    align-items: center !important;
    text-align: left !important;
    padding-left: 6px !important;
}

:deep(.native-admin-table .data-cell.status-cell .status-chip) {
    display: inline-flex !important;
    margin: 0 !important;
}

:deep(.native-admin-table .total-price-text) {
    color: #1e3a8a !important;
    font-weight: bold !important;
}

:deep(.native-admin-table .price-value) {
    color: #1e3a8a !important;
    font-weight: 700 !important;
}

:deep(.order-type-chip) {
    min-height: 24px !important;
    padding: 0 12px !important;
    border-radius: 6px !important;
    letter-spacing: 0.04em;
    font-size: 0.8rem !important;
    font-weight: 700 !important;
    border: 1px solid transparent !important;
}

:deep(.order-type-chip.order-type-offline) {
    background-color: #fff4e5 !important;
    color: #b26a00 !important;
    border-color: #f4c98a !important;
}

:deep(.order-type-chip.order-type-online) {
    background-color: #e8f7ee !important;
    color: #157347 !important;
    border-color: #b9e7ca !important;
}

:deep(.status-chip) {
    border-radius: 999px !important;
    border: 1px solid transparent !important;
    box-shadow: none !important;
    font-size: 13px !important;
    min-height: 28px !important;
}

:deep(.status-chip.status-chip-pending) {
    background-color: #fff7e8 !important;
    color: #b45309 !important;
    border-color: #f6d8a8 !important;
}

:deep(.status-chip.status-chip-confirmed) {
    background-color: #ecf3ff !important;
    color: #1d4ed8 !important;
    border-color: #c9dcff !important;
}

:deep(.status-chip.status-chip-delivering) {
    background-color: #eef2ff !important;
    color: #3730a3 !important;
    border-color: #d5dcff !important;
}

:deep(.status-chip.status-chip-waiting-delivery) {
    background-color: #fff4e5 !important;
    color: #b26a00 !important;
    border-color: #f4c98a !important;
}

:deep(.status-chip.status-chip-completed) {
    background-color: #e8f7ee !important;
    color: #0f766e !important;
    border-color: #b9e7ca !important;
}

:deep(.status-chip.status-chip-cancelled) {
    background-color: #feeceb !important;
    color: #dc2626 !important;
    border-color: #f9c7c3 !important;
}

:deep(.status-chip.status-chip-unknown) {
    background-color: #f1f5f9 !important;
    color: #475569 !important;
    border-color: #dbe4ef !important;
}
.action-group {
    min-width: 64px;
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

:deep(.action-cell .action-icon-btn:hover),
:deep(.action-cell .action-icon-btn:focus),
:deep(.action-cell .action-icon-btn:focus-visible),
:deep(.action-cell .action-icon-btn:active) {
    background: transparent !important;
    border: none !important;
    outline: none !important;
    box-shadow: none !important;
}

:deep(.action-cell .action-icon-btn .v-btn__overlay),
:deep(.action-cell .action-icon-btn .v-btn__underlay),
:deep(.action-cell .action-icon-btn .v-ripple__container) {
    display: none !important;
}
.filter-top {
    position: sticky;
    top: 8px;
    z-index: 6;
}
.invoice-filter-shell :deep(.filter-card) {
    border-radius: 14px !important;
    margin-bottom: 8px !important;
}
.invoice-filter-shell :deep(.v-card-text) {
    padding-top: 12px !important;
    padding-bottom: 12px !important;
}
.invoice-filter-shell :deep(.filter-header) {
    margin-bottom: 12px !important;
}
.page-title {
    line-height: 1.1;
}
.filter-top :deep(.v-card-text) {
    padding-top: 10px !important;
    padding-bottom: 10px !important;
}
.filter-top :deep(.filter-header) {
    margin-bottom: 8px !important;
}
.filter-top :deep(.v-field) {
    min-height: 40px;
}
.filter-top :deep(.v-input__control) {
    min-height: 40px;
}
.filter-field-label {
    font-size: 13px;
    font-weight: 700;
    color: #0f172a;
    margin-bottom: 4px;
}

.filter-top :deep(.date-field .v-field__append-inner) {
    padding-inline-end: 6px;
}

.filter-top :deep(.date-field input[type='date'])::-webkit-calendar-picker-indicator {
    opacity: 0;
    width: 0;
    margin: 0;
    padding: 0;
    pointer-events: none;
}

.filter-top :deep(.date-field input[type='date']) {
    padding-right: 0;
}

.filter-top :deep(.search-field input::placeholder) {
    font-size: 13px !important;
}

/* Invoice-specific reset button placement (do not affect other modules) */
.filter-top :deep(.filter-reset-col) {
    margin-left: auto !important;
    flex: 0 0 8.333333% !important;
    max-width: 8.333333% !important;
    width: 8.333333% !important;
    align-self: flex-end !important;
    justify-content: flex-end !important;
}
</style>
