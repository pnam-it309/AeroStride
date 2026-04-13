<script setup>
import { computed, ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EditIcon, EyeIcon } from 'vue-tabler-icons';

const loading = ref(false);
const isRefreshing = ref(false);
const allCustomers = ref([]);
const hasLoadedCustomers = ref(false);
const activeCustomerTab = ref('ONLINE');
const router = useRouter();

const pagination = ref({ page: 1, size: 5, totalElements: 0, totalPages: 1 });
const filters = ref({ keyword: '', trangThai: null, gioiTinh: null });

const customerTabs = [
    { key: 'ONLINE', label: 'Khách hàng online' },
    { key: 'OFFLINE', label: 'Khách hàng tại quầy' }
];

// Confirmation Logic
const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const normalizeText = (value) =>
    String(value ?? '')
        .trim()
        .toLowerCase();

const hasValue = (value) => normalizeText(value).length > 0;

const extractCustomerList = (response) => {
    if (Array.isArray(response)) return response;
    if (Array.isArray(response?.content)) return response.content;
    if (Array.isArray(response?.data)) return response.data;
    if (Array.isArray(response?.data?.content)) return response.data.content;
    return [];
};

const isOnlineCustomer = (item) => {
    const hasPhone = hasValue(item?.sdt);
    const hasEmail = hasValue(item?.email);
    const hasAccount = hasValue(item?.tenTaiKhoan);
    const hasPassword = hasValue(item?.matKhau);

    // Quy ước nghiệp vụ: khách online phải có SĐT + email + (tài khoản hoặc mật khẩu).
    if (hasPhone && hasEmail && (hasAccount || hasPassword)) {
        return true;
    }

    return false;
};

const isOfflineCustomer = (item) => {
    const hasPhone = hasValue(item?.sdt);
    const hasEmail = hasValue(item?.email);

    // Quy ước nghiệp vụ: khách tại quầy chỉ có SĐT, không có email.
    return hasPhone && !hasEmail;
};

const isObject = (value) => value && typeof value === 'object' && !Array.isArray(value);

const getDefaultAddress = (item) => {
    const addresses = Array.isArray(item?.addresses) ? item.addresses : [];
    if (addresses.length > 0) {
        return addresses.find((address) => address?.laMacDinh) || addresses[0] || null;
    }

    const directAddressObject = item?.diaChiMacDinh || item?.defaultAddress || item?.address;
    if (isObject(directAddressObject)) {
        return directAddressObject;
    }

    if (isObject(item?.diaChi)) {
        return item.diaChi;
    }

    return null;
};

const getReceiverName = (item) => {
    const defaultAddress = getDefaultAddress(item);
    return defaultAddress?.tenNguoiNhan || item?.tenNguoiNhan || item?.nguoiNhan || item?.ten || '-';
};

const getReceiverPhone = (item) => {
    const defaultAddress = getDefaultAddress(item);
    return defaultAddress?.sdtNguoiNhan || item?.sdtNguoiNhan || item?.sdtNhanHang || item?.sdt || '-';
};

const getAddressSummary = (item) => {
    const defaultAddress = getDefaultAddress(item);

    if (typeof item?.diaChi === 'string' && hasValue(item.diaChi)) {
        return item.diaChi;
    }
    if (typeof item?.diaChiDayDu === 'string' && hasValue(item.diaChiDayDu)) {
        return item.diaChiDayDu;
    }

    if (hasValue(defaultAddress?.diaChiChiTiet)) {
        const fromDefault = [defaultAddress.diaChiChiTiet, defaultAddress.phuongXa, defaultAddress.thanhPho, defaultAddress.tinh]
            .filter((part) => hasValue(part))
            .join(', ');

        if (hasValue(fromDefault)) {
            return fromDefault;
        }
    }

    if (hasValue(item?.diaChiChiTiet)) {
        const fromRoot = [item.diaChiChiTiet, item.phuongXa, item.thanhPho, item.tinh].filter((part) => hasValue(part)).join(', ');
        if (hasValue(fromRoot)) {
            return fromRoot;
        }
    }

    const composedAddress = [
        defaultAddress?.phuongXa || item?.phuongXa,
        defaultAddress?.thanhPho || item?.thanhPho,
        defaultAddress?.tinh || item?.tinh
    ]
        .filter((part) => hasValue(part))
        .join(', ');

    return composedAddress || 'Chưa cập nhật';
};

const formatNgayTao = (value) => {
    if (value === null || value === undefined || value === '') return '-';

    const numericValue = Number(value);
    if (!Number.isNaN(numericValue)) {
        const timestamp = numericValue < 1000000000000 ? numericValue * 1000 : numericValue;
        const date = new Date(timestamp);
        if (Number.isNaN(date.getTime())) return '-';

        const pad = (num) => String(num).padStart(2, '0');
        return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
    }

    return String(value);
};

const matchesFilters = (item) => {
    const keyword = normalizeText(filters.value.keyword);
    const searchPool = [item?.ma, item?.ten, item?.email, item?.sdt, item?.tenTaiKhoan].map((value) => normalizeText(value)).join(' ');

    const isMatchKeyword = !keyword || searchPool.includes(keyword);
    const isMatchTrangThai = !filters.value.trangThai || item?.trangThai === filters.value.trangThai;
    const isMatchGioiTinh = filters.value.gioiTinh === null || item?.gioiTinh === filters.value.gioiTinh;

    return isMatchKeyword && isMatchTrangThai && isMatchGioiTinh;
};

const baseFilteredCustomers = computed(() => allCustomers.value.filter(matchesFilters));

const onlineCustomers = computed(() => baseFilteredCustomers.value.filter(isOnlineCustomer));
const offlineCustomers = computed(() => baseFilteredCustomers.value.filter(isOfflineCustomer));

const onlineCount = computed(() => onlineCustomers.value.length);
const offlineCount = computed(() => offlineCustomers.value.length);
const isOnlineTab = computed(() => activeCustomerTab.value === 'ONLINE');

const currentTabCustomers = computed(() => (activeCustomerTab.value === 'ONLINE' ? onlineCustomers.value : offlineCustomers.value));

const tableHeaders = computed(() => {
    const baseHeaders = [
        { text: 'STT', align: 'center', width: '60px' },
        { text: 'Mã khách hàng', align: 'left', width: '110px' },
        { text: 'Tên khách hàng', align: 'left', width: '140px' },
        { text: 'Giới tính', align: 'center', width: '90px' },
        { text: 'Thông tin liên hệ', align: 'left', width: '150px' }
    ];

    if (isOnlineTab.value) {
        baseHeaders.push({ text: 'Thông tin giao hàng', align: 'left', width: '150px' });
    }

    baseHeaders.push(
        { text: 'Địa chỉ', align: 'left', width: '200px' },
        { text: 'Trạng thái', align: 'center', width: '110px' },
        { text: 'Ngày tạo', align: 'center', width: '120px' },
        { text: 'Hành động', align: 'center', width: '120px' }
    );

    return baseHeaders;
});

const pagedCustomers = computed(() => {
    const start = (pagination.value.page - 1) * pagination.value.size;
    return currentTabCustomers.value.slice(start, start + pagination.value.size);
});

const applyPaginationMeta = () => {
    pagination.value.totalElements = currentTabCustomers.value.length;
    pagination.value.totalPages = Math.max(1, Math.ceil(currentTabCustomers.value.length / pagination.value.size));

    if (pagination.value.page > pagination.value.totalPages) {
        pagination.value.page = 1;
    }
};

const loadCustomers = async () => {
    if (hasLoadedCustomers.value) {
        applyPaginationMeta();
        return;
    }

    loading.value = true;
    try {
        const response = await dichVuKhachHang.layTatCaKhachHang();
        allCustomers.value = extractCustomerList(response);
        hasLoadedCustomers.value = true;
        applyPaginationMeta();
    } catch (error) {
        console.error(error);
    } finally {
        loading.value = false;
    }
};

const handleRefresh = async () => {
    isRefreshing.value = true;
    filters.value = { keyword: '', trangThai: null, gioiTinh: null };
    activeCustomerTab.value = 'ONLINE';
    pagination.value.page = 1;
    applyPaginationMeta();
    setTimeout(() => (isRefreshing.value = false), 800);
};

const handleTabChange = (tabKey) => {
    if (activeCustomerTab.value === tabKey) return;
    activeCustomerTab.value = tabKey;
    pagination.value.page = 1;
    applyPaginationMeta();
};

const handleLocalFilterChange = () => {
    pagination.value.page = 1;
    applyPaginationMeta();
};

const handleExport = async () => {
    try {
        const blob = await dichVuKhachHang.xuatExcelKhachHang();
        downloadFile(blob, 'danh_sach_khach_hang.xlsx');
    } catch (error) {
        console.error('Lỗi xuất Excel:', error);
    }
};

const confirmChangeStatus = (item) => {
    confirmDialog.value = {
        show: true,
        title: 'THAY ĐỔI TRẠNG THÁI',
        message: `Bạn có chắc muốn đổi trạng thái của khách hàng [${item.ten}]?`,
        color: 'warning',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                const newS = item.trangThai === 'DANG_HOAT_DONG' ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
                await dichVuKhachHang.thayDoiTrangThaiKhachHang(item.id, newS);
                item.trangThai = newS;
                confirmDialog.value.show = false;
            } catch (e) {
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

onMounted(() => loadCustomers());

watch(
    () => pagination.value.size,
    () => {
        pagination.value.page = 1;
        applyPaginationMeta();
    }
);

watch(
    () => pagination.value.page,
    () => {
        applyPaginationMeta();
    }
);

watch(baseFilteredCustomers, () => {
    applyPaginationMeta();
});
</script>

<template>
    <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
        <!-- Header -->
        <div class="d-flex justify-space-between align-center mb-6">
            <div>
                <h5 class="text-h5 font-weight-bold">Quản lý khách hàng</h5>
            </div>
        </div>

        <!-- 1. FILTER -->
        <div class="filter-top invoice-filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <v-col cols="12" md="5" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field
                        v-model="filters.keyword"
                        placeholder="Tên, SĐT, Email..."
                        persistent-placeholder
                        variant="outlined"
                        density="compact"
                        hide-details
                        prepend-inner-icon="mdi-magnify"
                        class="font-weight-bold"
                        @input="handleLocalFilterChange"
                        @keyup.enter="handleLocalFilterChange"
                    ></v-text-field>
                </v-col>
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select
                        v-model="filters.trangThai"
                        :items="[
                            { title: 'Tất cả trạng thái', value: null },
                            { title: 'Hoạt động', value: 'DANG_HOAT_DONG' },
                            { title: 'Ngừng hoạt động', value: 'KHONG_HOAT_DONG' }
                        ]"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="font-weight-bold"
                        @update:model-value="handleLocalFilterChange"
                    ></v-select>
                </v-col>
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Giới tính</div>
                    <v-select
                        v-model="filters.gioiTinh"
                        :items="[
                            { title: 'Tất cả giới tính', value: null },
                            { title: 'Nam', value: true },
                            { title: 'Nữ', value: false }
                        ]"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="font-weight-bold"
                        @update:model-value="handleLocalFilterChange"
                    ></v-select>
                </v-col>
            </AdminFilter>
        </div>

        <!-- 2. TABLE -->
        <AdminTable
            title="Danh sách khách hàng"
            addButtonText="Thêm khách hàng"
            show-export-button
            :headers="tableHeaders"
            :items="pagedCustomers"
            :total-count="pagination.totalElements"
            :loading="loading"
            @add="router.push({ name: 'KhachHangForm' })"
            @export="handleExport"
        >
            <template #top>
                <div class="customer-tab-shell">
                    <button
                        v-for="tab in customerTabs"
                        :key="tab.key"
                        type="button"
                        class="customer-tab-btn"
                        :class="{ 'is-active': activeCustomerTab === tab.key }"
                        @click="handleTabChange(tab.key)"
                    >
                        <v-icon size="16" class="mr-1">mdi-account-outline</v-icon>
                        <span>{{ tab.label }}</span>
                        <span
                            v-if="(tab.key === 'ONLINE' ? onlineCount : offlineCount) > 0"
                            class="tab-badge"
                            :class="tab.key === 'ONLINE' ? 'badge-online' : 'badge-offline'"
                        >
                            {{ tab.key === 'ONLINE' ? onlineCount : offlineCount }}
                        </span>
                    </button>
                </div>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell center-cell">
                        {{ (pagination.page - 1) * pagination.size + index + 1 }}
                    </td>
                    <td class="data-cell">{{ item.ma || '-' }}</td>
                    <td class="data-cell">{{ item.ten || '-' }}</td>
                    <td class="data-cell center-cell">
                        <v-chip
                            size="x-small"
                            variant="flat"
                            :class="['gender-chip', item.gioiTinh ? 'gender-chip-male' : 'gender-chip-female']"
                        >
                            {{ item.gioiTinh ? 'Nam' : 'Nữ' }}
                        </v-chip>
                    </td>
                    <td class="data-cell">
                        <div class="info-line">{{ item.sdt || '-' }}</div>
                        <div v-if="hasValue(item.email)" class="info-line mt-1 d-flex align-center">
                            <v-icon size="14" class="mr-1">mdi-email-outline</v-icon>
                            {{ item.email }}
                        </div>
                    </td>
                    <td v-if="isOnlineTab" class="data-cell">
                        <div class="info-line">{{ getReceiverName(item) }}</div>
                        <div class="info-line mt-1">{{ getReceiverPhone(item) }}</div>
                    </td>
                    <td class="data-cell">
                        <div class="line-clamp-2" :title="getAddressSummary(item)">{{ getAddressSummary(item) }}</div>
                    </td>
                    <td class="data-cell center-cell">
                        <v-chip
                            size="small"
                            variant="flat"
                            :class="['status-chip', item.trangThai === 'DANG_HOAT_DONG' ? 'status-chip-active' : 'status-chip-inactive']"
                            >{{ item.trangThai === 'DANG_HOAT_DONG' ? 'Hoạt động' : 'Ngừng hoạt động' }}</v-chip
                        >
                    </td>
                    <td class="data-cell center-cell">{{ formatNgayTao(item.ngayTao) }}</td>
                    <td class="data-cell center-cell action-cell">
                        <div class="d-flex align-center justify-center action-controls">
                            <v-switch
                                :model-value="item.trangThai === 'DANG_HOAT_DONG'"
                                color="#1e3a8a"
                                hide-details
                                density="compact"
                                :readonly="item.trangThai !== 'DANG_HOAT_DONG'"
                                class="tight-switch action-switch"
                                @click.stop="item.trangThai === 'DANG_HOAT_DONG' ? confirmChangeStatus(item) : null"
                            >
                                <v-tooltip activator="parent" location="top">Đổi trạng thái</v-tooltip>
                            </v-switch>
                            <v-btn
                                icon
                                variant="text"
                                size="28"
                                color="#2aa6a1"
                                class="rounded-lg action-icon-btn"
                                @click.stop="router.push({ name: 'KhachHangDetail', params: { id: item.id } })"
                            >
                                <EyeIcon size="15" />
                                <v-tooltip activator="parent" location="top">Xem chi tiết</v-tooltip>
                            </v-btn>
                            <v-btn
                                icon
                                variant="text"
                                size="28"
                                color="#5f6f82"
                                class="rounded-lg action-icon-btn"
                                @click.stop="router.push({ name: 'KhachHangForm', params: { id: item.id } })"
                            >
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
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
                    :current-size="pagedCustomers.length"
                    @change="handleLocalFilterChange"
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
.gray-bg {
    background-color: #f5f7fb;
}
.text-dark {
    color: #0f172a !important;
}
.font-body {
    font-family: 'Inter', sans-serif;
}

.filter-field-label {
    font-size: 13px;
    font-weight: 700;
    color: #334155;
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

:deep(.native-admin-table .data-cell .text-subtitle-2),
:deep(.native-admin-table .data-cell .text-caption),
:deep(.native-admin-table .data-cell .text-body-2),
:deep(.native-admin-table .data-cell .text-subtitle-1) {
    font-size: 13px !important;
    font-weight: 600 !important;
    line-height: 1.35 !important;
}

:deep(.native-admin-table .header-cell:nth-child(1)),
:deep(.native-admin-table .data-cell:nth-child(1)),
:deep(.native-admin-table .header-cell:last-child),
:deep(.native-admin-table .data-cell:last-child) {
    text-align: center !important;
    padding-left: 0 !important;
}

.tight-switch {
    transform: none;
    width: 36px;
}
.line-clamp-2 {
    display: -webkit-box;
    line-clamp: 2;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.info-line {
    font-size: 13px;
    font-weight: 600;
    line-height: 1.35;
    color: #1e293b;
}

.center-cell {
    text-align: center;
}

.customer-tab-shell {
    display: flex;
    align-items: center;
    gap: 20px;
    padding: 0 16px;
    background: #ffffff;
}

.customer-tab-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background: transparent;
    border: none;
    border-bottom: 2px solid transparent;
    color: #0f172a;
    font-size: 14px;
    font-weight: 700;
    padding: 10px 0;
    cursor: pointer;
    transition: all 0.2s ease;
}

.customer-tab-btn.is-active {
    color: #0f172a;
    border-bottom-color: #1e3a8a;
}

.tab-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 18px;
    height: 18px;
    border-radius: 999px;
    padding: 0 6px;
    font-size: 11px;
    font-weight: 700;
    line-height: 1;
    background: #ef4444;
    color: #ffffff;
    border: none;
    transition: all 0.2s ease;
}

.customer-tab-btn.is-active .tab-badge {
    background: #1e3a8a;
    color: #ffffff;
}

.gender-chip {
    min-width: 34px;
    height: 24px !important;
    border-radius: 999px !important;
    font-size: 13px;
    font-weight: 700;
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

.status-chip {
    border-radius: 999px !important;
    font-size: 13px;
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

:deep(.action-cell .action-switch .v-switch__track),
:deep(.action-cell .action-switch .v-selection-control),
:deep(.action-cell .action-switch .v-selection-control__wrapper),
:deep(.action-cell .action-switch .v-selection-control__input),
:deep(.action-cell .action-switch .v-switch__thumb) {
    border: none !important;
    outline: none !important;
    box-shadow: none !important;
}

:deep(.action-cell .action-switch .v-selection-control__wrapper) {
    width: 36px !important;
    min-width: 36px !important;
    height: 22px !important;
}

:deep(.action-cell .action-switch .v-switch__track) {
    background: #d9e6fb !important;
    opacity: 1 !important;
    min-height: 17px !important;
    max-height: 17px !important;
    height: 18px !important;
    min-width: 30px !important;
    width: 30px !important;
}

:deep(.action-cell .action-switch .v-switch__thumb) {
    background: #2a5fb8 !important;
    width: 14px !important;
    height: 14px !important;
}

/* Force chip text sizes so Vuetify defaults do not make this screen look larger */
:deep(.gender-chip .v-chip__content) {
    font-size: 13px !important;
    font-weight: 700 !important;
    line-height: 1.1 !important;
}

:deep(.status-chip .v-chip__content) {
    font-size: 13px !important;
    font-weight: 600 !important;
    line-height: 1.1 !important;
}

/* Keep body text identical to other list screens */
:deep(.native-admin-table .data-cell),
:deep(.native-admin-table .data-cell .text-body-2),
:deep(.native-admin-table .data-cell .text-caption),
:deep(.native-admin-table .data-cell .text-subtitle-1),
:deep(.native-admin-table .data-cell .text-subtitle-2) {
    font-size: 13px !important;
}
</style>
