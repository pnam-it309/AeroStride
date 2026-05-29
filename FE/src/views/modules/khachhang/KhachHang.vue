<script setup>
import { computed, ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';

import { useUIStore } from '@/stores/ui';
import { AdminFilter, AdminTable, AdminPagination, AdminConfirm, AdminBreadcrumbs } from '@/components/common';
import { downloadFile } from '@/utils/fileUtils';
import { formatDateTime, formatCurrency } from '@/utils/formatters';
import { isActiveStatus, getStatusLabel, getStatusColor } from '@/utils/statusUtils';
import { SYSTEM_STATUS, STATUS_OPTIONS } from '@/constants/statusConstants';
import { PlusIcon, PencilIcon, StarIcon, TrashIcon, MapPinIcon } from 'vue-tabler-icons';
import axios from 'axios';

import { useAdminTable } from '@/composables/useAdminTable';
import { useLocation } from '@/composables/useLocation';
import { ADMIN_ICONS } from '@/constants/adminIcons';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useNotifications } from '@/services/notificationService';
import {
    TRANG_THAI_KHACH_HANG,
    TRANG_THAI_FILTER_OPTIONS,
    KHACH_HANG_TABLE_HEADERS,
    KHACH_HANG_STATS_TABLE_HEADERS,
    KHACH_HANG_BREADCRUMBS,
    ADDRESS_CONSTANTS
} from '@/constants/khachHangConstants';
import { GIOI_TINH_FILTER_OPTIONS } from '@/constants/appConstants';
import { useAddressMapping } from '@/composables/useAddressMapping';
import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';
import { getOrderStatusMeta } from '@/utils/orderStatus';
import { useRefreshHandler } from '@/composables/useRefreshHandler';

const { isRefreshing, handleRefresh: executeRefresh } = useRefreshHandler();
const activeTab = ref('danh-sach');
const currentView = ref('list'); // 'list' or 'invoice-history'

// ─── Invoices State ───
const selectedKHForInvoices = ref(null);

const INVOICE_STATUS_FILTER_OPTIONS = [
    { title: 'Tất cả', value: null },
    { title: 'Chờ xác nhận', value: 0 },
    { title: 'Đã xác nhận', value: 1 },
    { title: 'Chờ giao', value: 2 },
    { title: 'Đang giao', value: 3 },
    { title: 'Đã hoàn thành', value: 4 },
    { title: 'Hủy', value: 5 },
    { title: 'Hoàn đơn', value: 6 }
];

const invoiceHistoryTableHeaders = [
    { text: 'STT', width: '50px', align: 'center' },
    { text: 'Mã hóa đơn', width: '90px', align: 'center' },
    { text: 'Tên sản phẩm', width: '150px', align: 'left' },
    { text: 'Màu sắc', width: '80px', align: 'center' },
    { text: 'Kích thước', width: '80px', align: 'center' },
    { text: 'Số lượng', width: '70px', align: 'center' },
    { text: 'Nhận hàng', width: '120px', align: 'left' },
    { text: 'Giá trị đơn hàng', width: '110px', align: 'center' },
    { text: 'Trạng thái', width: '100px', align: 'center' }
];

const invoicesTab = useAdminTable(
    async (params) => {
        if (!selectedKHForInvoices.value) {
            return { content: [], totalElements: 0, totalPages: 1 };
        }
        return await dichVuHoaDon.layHoaDonPhanTrang({
            ...params,
            idKhachHang: selectedKHForInvoices.value.id,
            search: params.search || undefined,
            trangThai: params.trangThai !== null && params.trangThai !== undefined ? params.trangThai : undefined
        });
    },
    { search: '', trangThai: null }
);

const handleInvoicesFilterChange = () => {
    invoicesTab.handleFilter();
};

const handleInvoicesRefresh = () => {
    invoicesTab.handleReset();
};

const openInvoicesDialog = async (customer) => {
    selectedKHForInvoices.value = customer;
    currentView.value = 'invoice-history';
    await invoicesTab.handleReset();
};

const backToList = () => {
    currentView.value = 'list';
    selectedKHForInvoices.value = null;
};

const listTab = useAdminTable(dichVuKhachHang.layKhachHangPhanTrang, { search: '', gioiTinh: null, trangThai: null });
const statsTab = useAdminTable(dichVuKhachHang.layKhachHangPhanTrang, { search: '', gioiTinh: null, trangThai: null });

// Sync filters from listTab to statsTab
watch(
    () => listTab.filters.value,
    (newVal) => {
        statsTab.filters.value = { ...newVal };
    },
    { deep: true }
);

// Automatic fetch on tab switch if data is empty
watch(activeTab, (newTab) => {
    if (newTab === 'danh-sach' && listTab.items.value.length === 0) {
        listTab.loadData();
    } else if (newTab === 'thong-ke' && statsTab.items.value.length === 0) {
        statsTab.loadData();
    }
});

const allCustomers = computed(() => (activeTab.value === 'danh-sach' ? listTab.items.value : statsTab.items.value));
const loading = computed(() => (activeTab.value === 'danh-sach' ? listTab.loading.value : statsTab.loading.value));
const pagination = computed(() => (activeTab.value === 'danh-sach' ? listTab.pagination.value : statsTab.pagination.value));
const filters = listTab.filters;

const loadCustomers = () => {
    if (activeTab.value === 'danh-sach') {
        listTab.loadData();
    } else {
        statsTab.loadData();
    }
};

const handleLocalFilterChange = () => {
    listTab.handleFilter();
    statsTab.handleFilter();
};

const handleReset = () => {
    listTab.handleReset();
    statsTab.handleReset();
};

const handleRefresh = async () => {
    await executeRefresh(() => handleReset(), 1200);
};

const handleExport = async () => {
    try {
        const blob = await dichVuKhachHang.xuatExcelKhachHang();
        downloadFile(blob, 'danh_sach_khach_hang.xlsx');
    } catch (error) {
        console.error('Lỗi xuất Excel:', error);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể xuất Excel khách hàng',
            color: 'error'
        });
    }
};

const router = useRouter();
const uiStore = useUIStore();
const { addNotification } = useNotifications();

// Use composables
const { confirmDialog, setConfirm, clearConfirm, handleConfirm } = useConfirmDialog();

// Move declarations used in watcher up
const addrDialog = ref(false);
const selectedKH = ref(null);

const customerBreadcrumbs = computed(() => {
    if (currentView.value === 'invoice-history') {
        return [
            { title: 'Quản lý tài khoản', disabled: false, href: '#' },
            { title: 'Khách hàng', disabled: false, to: '/admin/khach-hang' },
            { title: `Lịch sử mua hàng: ${selectedKHForInvoices.value?.ten || ''}`, disabled: true }
        ];
    }
    return KHACH_HANG_BREADCRUMBS;
});

watch(addrDialog, (isOpen) => {
    if (isOpen) {
        uiStore.setBreadcrumbs([
            { title: 'Quản lý tài khoản', disabled: false, href: '#' },
            { title: 'Khách hàng', disabled: false, to: '/admin/khach-hang' },
            { title: `Địa chỉ: ${selectedKH.value?.ten || ''}`, disabled: true }
        ]);
    } else {
        uiStore.setBreadcrumbs(customerBreadcrumbs.value);
    }
});

watch(currentView, () => {
    uiStore.setBreadcrumbs(customerBreadcrumbs.value);
});

// ─── Address Dialog ───────────────────────────────────────────────
const addrLoading = ref(false);
const addrSaving = ref(false);
const listDiaChi = ref([]);

const addrForm = ref({
    id: null,
    tinh: null,
    thanhPho: null,
    phuongXa: null,
    diaChiChiTiet: '',
    tenNguoiNhan: '',
    sdtNguoiNhan: '',
    laMacDinh: false
});
const isEditAddr = ref(false);
const showAddrForm = ref(false);

const { provinces, districts, wards, loadingLocations: loadingLoc, fetchProvinces, fetchDistricts, fetchWards, cleanName } = useLocation();
const { mapCodesToNames, isLegacyAddressId, createLegacyAddressId, parseAddressString } = useAddressMapping();

const openAddrDialog = async (item) => {
    selectedKH.value = item;
    showAddrForm.value = false;
    isEditAddr.value = false;

    // 1. Khởi tạo từ danh sách địa chỉ lồng nhau (nếu có)
    const existing = item.diaChis || item.listDiaChi || [];
    let initialList = Array.isArray(existing) ? [...existing] : [];

    // 2. Fallback: Nếu không có danh sách, nhưng có thông tin địa chỉ phẳng ở Root
    if (initialList.length === 0 && (item.tinh || item.thanhPho || item.diaChiChiTiet)) {
        initialList.push({
            id: createLegacyAddressId(item.id),
            tinh: item.tinh,
            thanhPho: item.thanhPho,
            phuongXa: item.phuongXa,
            diaChiChiTiet: item.diaChiChiTiet,
            tenNguoiNhan: item.ten,
            sdtNguoiNhan: item.sdt,
            laMacDinh: true
        });
    }

    listDiaChi.value = initialList;

    addrForm.value = {
        id: null,
        tinh: null,
        thanhPho: null,
        phuongXa: null,
        diaChiChiTiet: '',
        tenNguoiNhan: item.ten,
        sdtNguoiNhan: item.sdt,
        laMacDinh: false
    };
    addrDialog.value = true;
    await fetchProvinces();
    await loadAddresses(item.id);
};

const loadAddresses = async (khId) => {
    if (!khId) return;
    addrLoading.value = true;
    try {
        const res = await dichVuKhachHang.layDanhSachDiaChi(khId);

        let data = [];
        if (Array.isArray(res)) {
            data = res;
        } else if (res?.data) {
            if (Array.isArray(res.data)) data = res.data;
            else if (Array.isArray(res.data.content)) data = res.data.content;
            else if (Array.isArray(res.data.items)) data = res.data.items;
        } else if (Array.isArray(res?.content)) {
            data = res.content;
        }

        // Luôn cập nhật nếu data là mảng (kể cả mảng rỗng)
        if (Array.isArray(data)) {
            const newList = [...data];

            // Bảo toàn địa chỉ "gốc" (legacy) từ bảng KhachHang nếu có
            const item = selectedKH.value;
            const hasLegacyInfo = item && (item.tinh || item.thanhPho || item.diaChiChiTiet);

            if (hasLegacyInfo) {
                const legacyId = createLegacyAddressId(item.id);
                // Chỉ thêm nếu trong data chưa có ID này (tránh bị loadAddress ghi đè mất)
                if (!newList.some((addr) => addr.id === legacyId)) {
                    newList.push({
                        id: legacyId,
                        tinh: item.tinh,
                        thanhPho: item.thanhPho,
                        phuongXa: item.phuongXa,
                        diaChiChiTiet: item.diaChiChiTiet,
                        tenNguoiNhan: item.ten,
                        sdtNguoiNhan: item.sdt,
                        laMacDinh: data.length === 0 // Chỉ mặc định nếu không có địa chỉ thực nào khác
                    });
                }
            }

            listDiaChi.value = newList;
        }
    } catch (e) {
        console.error('Error loading addresses:', e);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể kết nối máy chủ để lấy địa chỉ',
            color: 'error'
        });
    } finally {
        addrLoading.value = false;
    }
};

const openNewAddrForm = () => {
    isEditAddr.value = false;
    addrForm.value = {
        id: null,
        tinh: null,
        thanhPho: null,
        phuongXa: null,
        diaChiChiTiet: '',
        tenNguoiNhan: selectedKH.value?.ten,
        sdtNguoiNhan: selectedKH.value?.sdt,
        laMacDinh: listDiaChi.value.length === 0
    };
    districts.value = [];
    wards.value = [];
    showAddrForm.value = true;
};

const openEditAddrForm = async (addr) => {
    isEditAddr.value = true;
    showAddrForm.value = true;

    // Nạp thông tin cơ bản
    addrForm.value = {
        id: addr.id,
        tenNguoiNhan: addr.tenNguoiNhan,
        sdtNguoiNhan: addr.sdtNguoiNhan,
        diaChiChiTiet: addr.diaChiChiTiet,
        laMacDinh: addr.laMacDinh,
        tinh: null,
        thanhPho: null,
        phuongXa: null
    };

    try {
        // Xử lý trường hợp dữ liệu bị dồn hết vào diaChiChiTiet (Flat address)
        let tinhName = addr.tinh;
        let huyenName = addr.thanhPho;
        let xaName = addr.phuongXa;

        if (!tinhName && addr.diaChiChiTiet?.includes(',')) {
            const parsed = parseAddressString(addr.diaChiChiTiet);
            if (parsed.tinh) {
                tinhName = parsed.tinh;
                huyenName = parsed.thanhPho;
                xaName = parsed.phuongXa;
                addrForm.value.diaChiChiTiet = parsed.diaChiChiTiet;
            }
        }

        // 1. Khớp Tỉnh
        const p = provinces.value.find((x) => cleanName(x.name) === cleanName(tinhName) || x.code === tinhName);
        if (p) {
            addrForm.value.tinh = p.code;
            await fetchDistricts(p.code);

            // 2. Khớp Huyện
            const d = districts.value.find((x) => cleanName(x.name) === cleanName(huyenName) || x.code === huyenName);
            if (d) {
                addrForm.value.thanhPho = d.code;
                await fetchWards(d.code);

                // 3. Khớp Xã
                const w = wards.value.find((x) => cleanName(x.name) === cleanName(xaName) || x.code === xaName);
                if (w) {
                    addrForm.value.phuongXa = w.code;
                }
            }
        }
    } catch (error) {
        console.error('Lỗi nạp địa chỉ:', error);
    }
};

const doSaveAddress = async () => {
    addrSaving.value = true;
    try {
        // Chuẩn bị payload
        const payload = {
            ...addrForm.value,
            idKhachHang: selectedKH.value.id
        };

        // Chuyển đổi Code -> Name trước khi gửi lên Server
        const mappedPayload = mapCodesToNames(addrForm.value, provinces.value, districts.value, wards.value);
        Object.assign(payload, mappedPayload);

        // Xử lý trường hợp ID giả (synthesized address)
        const isRealId = payload.id && !isLegacyAddressId(payload.id);

        if (isEditAddr.value && isRealId) {
            await dichVuKhachHang.capNhatDiaChi(addrForm.value.id, payload);
            addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật địa chỉ', color: 'success' });
        } else {
            // Nếu là thêm mới hoặc là địa chỉ giả lập -> gọi API Add
            delete payload.id; // Xóa ID giả trước khi Add
            await dichVuKhachHang.taoDiaChi(payload);
            addNotification({ title: 'Thành công', subtitle: 'Đã thêm địa chỉ mới', color: 'success' });
        }

        showAddrForm.value = false;
        await loadAddresses(selectedKH.value.id);
    } catch (e) {
        console.error('Error saving address:', e);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể lưu địa chỉ',
            color: 'error'
        });
    } finally {
        addrSaving.value = false;
    }
};

const saveAddress = () => {
    if (!selectedKH.value) return;

    const isEditing = isEditAddr.value && addrForm.value.id && !String(addrForm.value.id).startsWith('root-');
    setConfirm({
        title: isEditing ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ',
        message: isEditing ? 'Bạn có chắc muốn cập nhật địa chỉ này không?' : 'Bạn có chắc muốn thêm địa chỉ mới cho khách hàng này không?',
        color: 'primary',
        action: async () => {
            await doSaveAddress();
        }
    });
};

const handleSetDefault = async (addrId) => {
    try {
        await dichVuKhachHang.datDiaChiMacDinh(addrId);
        await loadAddresses(selectedKH.value.id);
    } catch (e) {
        console.error('Error setting default address:', e);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể đặt địa chỉ mặc định',
            color: 'error'
        });
    }
};

const confirmSetDefaultAddr = (addr) => {
    setConfirm({
        title: 'Đặt địa chỉ mặc định',
        message: `Bạn có chắc muốn đặt địa chỉ của [${addr.tenNguoiNhan}] làm mặc định không?`,
        color: 'warning',
        action: async () => {
            await handleSetDefault(addr.id);
        }
    });
};

const handleDeleteAddr = (addrId) => {
    setConfirm({
        title: 'Xóa địa chỉ',
        message: 'Bạn có chắc muốn xóa địa chỉ này không?',
        color: 'error',
        action: async () => {
            try {
                await dichVuKhachHang.xoaDiaChi(addrId);
                await loadAddresses(selectedKH.value.id);
            } catch (e) {
                console.error('Error deleting address:', e);
                addNotification({
                    title: 'Lỗi',
                    subtitle: 'Không thể xóa địa chỉ',
                    color: 'error'
                });
            }
        }
    });
};

const tableHeaders = KHACH_HANG_TABLE_HEADERS;
const statsTableHeaders = KHACH_HANG_STATS_TABLE_HEADERS;

onMounted(() => {
    loadCustomers();
});

const hasValue = (v) => v && String(v).trim().length > 0;

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

const getDefaultAddress = (item) => {
    // nếu có danh sách địa chỉ
    if (Array.isArray(item?.diaChis)) {
        return item.diaChis.find((dc) => dc.laMacDinh) || item.diaChis[0];
    }

    // nếu BE trả 1 địa chỉ mặc định riêng
    if (item?.diaChiMacDinh) {
        return item.diaChiMacDinh;
    }

    return null;
};

const confirmChangeStatus = (item) => {
    setConfirm({
        title: 'Thay đổi trạng thái',
        message: `Bạn có chắc muốn đổi trạng thái của khách hàng [${item.ten}]?`,
        color: 'warning',
        action: async () => {
            try {
                const newS =
                    item.trangThai === SYSTEM_STATUS.ACTIVE
                        ? SYSTEM_STATUS.INACTIVE
                        : SYSTEM_STATUS.ACTIVE;
                await dichVuKhachHang.thayDoiTrangThaiKhachHang(item.id, newS);

                // Cập nhật local
                item.trangThai = newS;

                addNotification({
                    title: 'Thành công',
                    subtitle: `Đã chuyển sang: ${getStatusLabel(newS)}`,
                    color: 'success'
                });

                // Load lại dữ liệu để đồng bộ
                loadCustomers();
            } catch (e) {
                console.error('[Customer] Status change error:', e);
                addNotification({
                    title: 'Lỗi',
                    subtitle: 'Không thể thay đổi trạng thái khách hàng',
                    color: 'error'
                });
            }
        }
    });
};

onMounted(() => loadCustomers());

// addr form watchers
watch(
    () => addrForm.value.tinh,
    (val) => {
        addrForm.value.thanhPho = null;
        addrForm.value.phuongXa = null;
        if (val) fetchDistricts(val);
    }
);
watch(
    () => addrForm.value.thanhPho,
    (val) => {
        addrForm.value.phuongXa = null;
        if (val) fetchWards(val);
    }
);

const addrFormTitle = computed(() => {
    return showAddrForm.value ? (isEditAddr.value ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ mới') : 'Thêm địa chỉ khác';
});

const submitAddrBtnText = computed(() => {
    return isEditAddr.value ? 'Cập nhật địa chỉ' : 'Lưu địa chỉ mới';
});

const formatAddressFull = (addr) => {
    return [addr.diaChiChiTiet, addr.phuongXa, addr.thanhPho, addr.tinh].filter(Boolean).join(', ');
};
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body admin-module-page">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="customerBreadcrumbs" />

        <div class="mb-2"></div>

        <div v-if="currentView === 'list'" class="admin-table-main-root">
            <!-- Filter -->
            <div class="filter-shell">
                <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                    <!-- Tìm kiếm -->
                    <v-col cols="12" md="4" class="filter-cell">
                        <div class="filter-field-label">Tìm kiếm</div>
                        <v-text-field v-model="filters.search" placeholder="Tên, SĐT, Email, Mã..." variant="outlined"
                            density="compact" hide-details prepend-inner-icon="mdi-magnify" class="compact-input"
                            @input="handleLocalFilterChange" />
                    </v-col>

                    <!-- Giới tính -->
                    <v-col cols="12" md="3" class="filter-cell">
                        <div class="filter-field-label">Giới tính</div>
                        <v-select v-model="filters.gioiTinh" :items="GIOI_TINH_FILTER_OPTIONS" variant="outlined"
                            density="compact" hide-details class="compact-input"
                            @update:model-value="handleLocalFilterChange" />
                    </v-col>

                    <!-- Trạng thái -->
                    <v-col cols="12" md="3" class="filter-cell">
                        <div class="filter-field-label">Trạng thái</div>
                        <v-select v-model="filters.trangThai" :items="TRANG_THAI_FILTER_OPTIONS" variant="outlined"
                            density="compact" hide-details class="compact-input"
                            @update:model-value="handleLocalFilterChange" />
                    </v-col>
                </AdminFilter>
            </div>

            <AdminTable title="Khách hàng" addButtonText="Tạo mới" show-export-button
                :headers="activeTab === 'danh-sach' ? tableHeaders : statsTableHeaders" :items="allCustomers"
                :total-count="pagination.totalElements" :loading="loading" @add="router.push({ name: 'KhachHangForm' })"
                @export="handleExport">
                <template #top>
                    <!-- Tabs navigation inside the table, no transition animation -->
                    <v-tabs v-model="activeTab" bg-color="transparent" color="primary" height="54" align-tabs="start"
                        class="admin-tabs">
                        <v-tab value="danh-sach" class="text-none px-4 tab-item">
                            <v-icon start class="mr-2" size="13">mdi-account-multiple-outline</v-icon>
                            Danh sách khách hàng
                        </v-tab>
                        <v-tab value="thong-ke" class="text-none px-4 tab-item">
                            <v-icon start class="mr-2" size="13">mdi-chart-line</v-icon>
                            Tổng đơn mua hàng
                        </v-tab>
                    </v-tabs>
                </template>

                <template #row="{ item, index }">
                    <!-- Row for Tab 1: Danh sách chi tiết -->
                    <tr v-if="activeTab === 'danh-sach'" class="data-row">
                        <td class="data-cell text-center text-black font-weight-medium">
                            {{ (pagination.page - 1) * pagination.size + index + 1 }}
                        </td>
                        <td class="data-cell text-center">
                            <div class="text-truncate" :title="item.ma">{{ item.ma || '-' }}</div>
                        </td>
                        <td class="data-cell text-left px-4">
                            <div class="text-truncate" :title="item.ten">{{ item.ten || '-' }}</div>
                        </td>
                        <td class="data-cell">
                            <v-chip variant="flat"
                                :class="['gender-chip', item.gioiTinh ? 'gender-chip-male' : 'gender-chip-female']">
                                {{ item.gioiTinh === true ? 'Nam' : item.gioiTinh === false ? 'Nữ' : '-' }}
                            </v-chip>
                        </td>
                        <td class="data-cell text-left px-4">
                            <div class="d-inline-flex flex-column align-start" style="width: 100%; overflow: hidden">
                                <div class="info-line text-black mb-1 text-truncate" style="width: 100%"
                                    :title="item.sdt">
                                    <v-icon size="14" class="mr-2 text-slate-400">mdi-phone</v-icon>
                                    <span>{{ item.sdt || '-' }}</span>
                                </div>
                                <div v-if="hasValue(item.email)"
                                    class="info-line d-flex align-center justify-start text-slate-800 text-truncate"
                                    style="width: 100%" :title="item.email">
                                    <v-icon size="14" class="mr-2">mdi-email-outline</v-icon>{{ item.email }}
                                </div>
                            </div>
                        </td>
                        <td class="data-cell text-left px-4 allow-wrap">
                            <div class="text-slate-800" :title="getAddressSummary(item)">{{ getAddressSummary(item) }}
                            </div>
                        </td>
                        <td class="data-cell">
                            <v-chip variant="flat"
                                :class="['status-chip', isActiveStatus(item.trangThai) ? 'status-chip-active' : 'status-chip-inactive']">
                                {{ getStatusLabel(item.trangThai) }}
                            </v-chip>
                        </td>
                        <td class="data-cell action-cell">
                            <div class="d-flex align-center justify-center action-controls">
                                <!-- Địa chỉ -->
                                <v-btn variant="text" class="action-icon-btn" @click.stop="openAddrDialog(item)">
                                    <component :is="ADMIN_ICONS.ACTION.MAP" size="15" />
                                    <v-tooltip activator="parent" location="top">Quản lý địa chỉ</v-tooltip>
                                </v-btn>
                                <!-- Chỉnh sửa -->
                                <v-btn variant="text" class="action-icon-btn"
                                    @click.stop="router.push({ name: 'KhachHangForm', params: { id: item.id } })">
                                    <component :is="ADMIN_ICONS.ACTION.EDIT" size="15" />
                                    <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                                </v-btn>
                                <!-- Switch trạng thái -->
                                <div class="switch-wrapper">
                                    <v-switch :model-value="isActiveStatus(item.trangThai)" color="primary" hide-details
                                        density="compact" class="tight-switch action-switch"
                                        @click.prevent.stop="confirmChangeStatus(item)" />
                                    <v-tooltip activator="parent" location="top">Chuyển đổi trạng thái</v-tooltip>
                                </div>
                            </div>
                        </td>
                    </tr>

                    <!-- Row for Tab 2: Thống kê mua hàng -->
                    <tr v-else class="data-row">
                        <td class="data-cell text-center text-black font-weight-medium">
                            {{ (pagination.page - 1) * pagination.size + index + 1 }}
                        </td>
                        <td class="data-cell text-center">
                            <div class="text-truncate" :title="item.ma">{{ item.ma || '-' }}</div>
                        </td>
                        <td class="data-cell text-left px-4">
                            <div class="text-truncate" :title="item.ten">{{ item.ten || '-' }}</div>
                        </td>
                        <td class="data-cell text-center">
                            {{ item.tongDonHang || 0 }}
                        </td>
                        <td class="data-cell text-center">
                            {{ item.tongDonHoan || 0 }}
                        </td>
                        <td class="data-cell text-center font-weight-bold" style="color: #1e257c !important">
                            {{ formatCurrency(item.tongChiTieu || 0) }}
                        </td>
                        <td class="data-cell text-center text-slate-800">
                            {{ item.ngayDonHangGanNhat ? formatDateTime(item.ngayDonHangGanNhat) : '-' }}
                        </td>
                        <td class="data-cell">
                            <v-chip variant="flat"
                                :class="['status-chip', isActiveStatus(item.trangThai) ? 'status-chip-active' : 'status-chip-inactive']">
                                {{ getStatusLabel(item.trangThai) }}
                            </v-chip>
                        </td>
                        <td class="data-cell action-cell">
                            <div class="d-flex align-center justify-center action-controls">
                                <!-- Chi tiết -->
                                <v-btn variant="text" class="action-icon-btn" @click.stop="openInvoicesDialog(item)">
                                    <component :is="ADMIN_ICONS.ACTION.VIEW" size="15" />
                                    <v-tooltip activator="parent" location="top">Chi tiết hóa đơn</v-tooltip>
                                </v-btn>
                                <!-- Switch trạng thái -->
                                <div class="switch-wrapper">
                                    <v-switch :model-value="isActiveStatus(item.trangThai)" color="primary" hide-details
                                        density="compact" class="tight-switch action-switch"
                                        @click.prevent.stop="confirmChangeStatus(item)" />
                                    <v-tooltip activator="parent" location="top">Chuyển đổi trạng thái</v-tooltip>
                                </div>
                            </div>
                        </td>
                    </tr>
                </template>

                <template #pagination>
                    <AdminPagination v-model="pagination.page" :page-size="pagination.size"
                        @update:page-size="pagination.size = $event" :total-pages="pagination.totalPages"
                        :total-elements="pagination.totalElements" :current-size="allCustomers.length"
                        @change="loadCustomers" />
                </template>
            </AdminTable>
        </div>

        <div v-else-if="currentView === 'invoice-history'"
            class="invoice-history-container admin-table-main-root animate-fade-in font-body">
            <!-- Header: Back button, title -->
            <div class="d-flex align-center gap-2 mb-3 mt-1">
                <v-btn icon variant="flat" color="white" class="border shadow-sm rounded-lg" size="36"
                    style="height: 36px !important; width: 36px !important; min-height: 36px !important"
                    @click="backToList">
                    <v-icon size="18" color="slate-700">mdi-arrow-left</v-icon>
                </v-btn>
                <div class="text-h6 font-weight-black text-slate-800"
                    style="font-family: 'Inter', sans-serif !important; font-size: 15px !important">
                    Hóa đơn đã mua — {{ selectedKHForInvoices?.ten }}
                </div>
            </div>

            <!-- Filter -->
            <div class="filter-shell">
                <AdminFilter title="Bộ lọc" :loading="invoicesTab.loading.value"
                    :is-refreshing="invoicesTab.loading.value" @refresh="handleInvoicesRefresh">
                    <!-- Tìm kiếm -->
                    <v-col cols="12" md="4" class="filter-cell">
                        <div class="filter-field-label" style="font-family: 'Inter', sans-serif !important">Tìm kiếm
                        </div>
                        <v-text-field v-model="invoicesTab.filters.value.search" placeholder="Mã hóa đơn..."
                            prepend-inner-icon="mdi-magnify" variant="outlined" density="compact" hide-details
                            class="compact-input" style="font-family: 'Inter', sans-serif !important"
                            @update:model-value="handleInvoicesFilterChange" />
                    </v-col>

                    <!-- Trạng thái -->
                    <v-col cols="12" md="3" class="filter-cell">
                        <div class="filter-field-label" style="font-family: 'Inter', sans-serif !important">Trạng thái
                            hóa đơn
                        </div>
                        <v-select v-model="invoicesTab.filters.value.trangThai" :items="INVOICE_STATUS_FILTER_OPTIONS"
                            variant="outlined" density="compact" hide-details class="compact-input"
                            style="font-family: 'Inter', sans-serif !important"
                            @update:model-value="handleInvoicesFilterChange" />
                    </v-col>
                </AdminFilter>
            </div>

            <!-- Table Card -->
            <AdminTable hide-toolbar :headers="invoiceHistoryTableHeaders" :items="invoicesTab.items.value"
                :loading="invoicesTab.loading.value" empty-text="Chưa có hóa đơn nào được tìm thấy cho khách hàng này"
                empty-icon="mdi-receipt-text-off">
                <template #row="{ item, index }">
                    <tr class="data-row">
                        <!-- STT -->
                        <td class="data-cell text-center text-black font-weight-medium"
                            style="font-size: 13px !important">
                            {{ (invoicesTab.pagination.value.page - 1) * invoicesTab.pagination.value.size +
                                index + 1 }}
                        </td>

                        <!-- Mã hóa đơn -->
                        <td class="data-cell text-center text-slate-800" style="font-size: 13px !important">
                            <span class="font-weight-bold">{{ item.maHoaDon }}</span>
                        </td>

                        <!-- Tên sản phẩm (Danh sách) -->
                        <td class="data-cell text-left px-4 text-slate-700" style="font-size: 13px !important">
                            <div class="d-flex flex-column gap-2 py-1">
                                <div v-for="(detail, dIdx) in item.details" :key="dIdx" class="text-truncate"
                                    style="max-width: 200px">
                                    {{ detail.tenSanPham || 'N/A' }}
                                </div>
                                <span v-if="!item.details || item.details.length === 0" class="text-slate-400 italic">Không có sản phẩm</span>
                            </div>
                        </td>

                        <!-- Màu sắc (Danh sách) -->
                        <td class="data-cell text-center text-slate-600" style="font-size: 13px !important">
                            <div class="d-flex flex-column gap-2 py-1 align-center">
                                <div v-for="(detail, dIdx) in item.details" :key="dIdx">
                                    <v-chip v-if="detail.mauSac && detail.mauSac !== '-'" size="x-small" variant="tonal" color="secondary"
                                        class="font-weight-medium">
                                        {{ detail.mauSac }}
                                    </v-chip>
                                    <span v-else>-</span>
                                </div>
                            </div>
                        </td>

                        <!-- Kích thước (Danh sách) -->
                        <td class="data-cell text-center text-slate-600" style="font-size: 13px !important">
                            <div class="d-flex flex-column gap-2 py-1 align-center">
                                <div v-for="(detail, dIdx) in item.details" :key="dIdx">
                                    <v-chip v-if="detail.kichThuoc && detail.kichThuoc !== '-'" size="x-small" variant="tonal" color="info"
                                        class="font-weight-medium">
                                        {{ detail.kichThuoc }}
                                    </v-chip>
                                    <span v-else>-</span>
                                </div>
                            </div>
                        </td>

                        <!-- Số lượng (Danh sách) -->
                        <td class="data-cell text-center text-black font-weight-bold"
                            style="font-size: 13px !important">
                            <div class="d-flex flex-column gap-2 py-1 align-center">
                                <div v-for="(detail, dIdx) in item.details" :key="dIdx">
                                    {{ detail.soLuong || 0 }}
                                </div>
                            </div>
                        </td>

                        <!-- Thông tin nhận hàng -->
                        <td class="data-cell text-left px-4" style="font-size: 13px !important">
                            <div class="d-flex flex-column" style="word-break: break-word; white-space: normal">
                                <div v-if="item.soDienThoai" class="text-slate-700 mb-1"
                                    style="font-size: 13px !important">
                                    <v-icon size="14" class="mr-1 text-slate-400"
                                        style="font-size: 14px !important">mdi-phone</v-icon>
                                    {{ item.soDienThoai }}
                                </div>
                                <div v-if="item.diaChiNguoiNhan" class="text-slate-500 line-height-1-4"
                                    style="font-size: 12px !important">
                                    <v-icon size="14" class="mr-1 text-slate-400"
                                        style="font-size: 14px !important">mdi-map-marker</v-icon>
                                    {{ item.diaChiNguoiNhan }}
                                </div>
                            </div>
                        </td>

                        <!-- Tổng tiền -->
                        <td class="data-cell text-center font-weight-bold"
                            style="color: #1e257c !important; font-size: 13px !important">
                            {{ formatCurrency(item.tongTienSauGiam || item.tongTien || 0) }}
                        </td>

                        <!-- Trạng thái -->
                        <td class="data-cell text-center" style="font-size: 13px !important">
                            <template v-if="getOrderStatusMeta(item.trangThai)">
                                <v-chip :class="['status-chip', getOrderStatusMeta(item.trangThai).chipClass]"
                                    variant="flat" size="small" style="font-size: 12px !important">
                                    <v-icon start size="14" style="font-size: 14px !important">{{
                                        getOrderStatusMeta(item.trangThai).icon
                                        }}</v-icon>
                                    {{ getOrderStatusMeta(item.trangThai).text }}
                                </v-chip>
                            </template>
                        </td>
                    </tr>
                </template>

                <template #pagination>
                    <AdminPagination v-model="invoicesTab.pagination.value.page"
                        :page-size="invoicesTab.pagination.value.size"
                        @update:page-size="invoicesTab.pagination.value.size = $event"
                        :total-pages="invoicesTab.pagination.value.totalPages"
                        :total-elements="invoicesTab.pagination.value.totalElements"
                        :current-size="invoicesTab.items.value.length" @change="invoicesTab.loadData" />
                </template>
            </AdminTable>
        </div>

        <!-- Confirm Dialog -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="handleConfirm(true)"
            @cancel="handleConfirm(false)" />

        <!-- ═══════════════════════════════════════════════════
         Dialog Quản lý địa chỉ (2 cột: danh sách | form)
         ═══════════════════════════════════════════════════ -->
        <v-dialog v-model="addrDialog" max-width="1100" width="90vw" scrollable transition="scale-transition">
            <v-card class="rounded-xl font-body addr-dialog-card overflow-hidden" min-height="650">
                <!-- Tiêu đề dialog -->
                <v-card-title class="d-flex align-center pa-6 bg-slate-50">
                    <div class="icon-blob bg-blue-lighten-5 mr-4" style="width: 50px; height: 50px">
                        <MapPinIcon :size="30" class="text-primary" />
                    </div>
                    <div>
                        <div class="text-h6 font-weight-black text-slate-800 line-height-1">Quản lý địa chỉ — {{
                            selectedKH?.ten
                        }}</div>
                    </div>
                    <v-spacer />
                    <v-btn icon variant="text" size="small" color="slate-400" @click="addrDialog = false">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </v-card-title>

                <v-card-text class="pa-0" style="max-height: 85vh; overflow: hidden">
                    <v-row no-gutters style="height: 100%">
                        <!-- ── Cột trái: Danh sách địa chỉ ── -->
                        <v-col cols="12" md="6" class="pb-6 overflow-y-auto" style="max-height: calc(85vh - 80px)">
                            <div
                                class="px-8 pt-6 pb-4 d-flex align-center justify-space-between sticky-top bg-white z-10 position-relative">
                                <span class="text-subtitle-2 font-weight-bold text-slate-800"
                                    style="font-size: 15px !important">Địa chỉ hiện tại</span>
                                <v-progress-linear v-if="addrLoading && listDiaChi.length > 0" indeterminate
                                    color="primary" height="2" class="position-absolute"
                                    style="bottom: 0; left: 0; right: 0; z-index: 20"></v-progress-linear>
                            </div>

                            <div v-if="addrLoading && listDiaChi.length === 0" class="text-center py-16">
                                <v-progress-circular indeterminate color="primary" size="40" />
                            </div>

                            <div v-else-if="listDiaChi.length === 0"
                                class="text-center py-16 px-8 mx-8 bg-slate-50/50 rounded-xl mt-4">
                                <v-icon size="48" color="slate-200" class="mb-4">mdi-map-marker-off</v-icon>
                                <div class="text-slate-400 font-weight-medium">Chưa có địa chỉ nào được đăng ký</div>
                            </div>

                            <div v-else class="px-8">
                                <div v-for="addr in listDiaChi" :key="addr.id"
                                    class="pa-6 rounded-xl bg-white border border-slate-200 mb-6 hover-shadow-sm transition-all position-relative"
                                    :class="{ 'border-s-4 border-s-success border-success-light': addr.laMacDinh }"
                                    style="
                                        margin-top: 15px;
                                        box-shadow:
                                            0 4px 6px -1px rgba(0, 0, 0, 0.05),
                                            0 2px 4px -1px rgba(0, 0, 0, 0.03) !important;
                                    ">
                                    <!-- Dòng tên + badge mặc định -->
                                    <div class="d-flex align-center gap-3 mb-3">
                                        <span class="text-subtitle-1 font-weight-medium text-slate-700"
                                            style="font-size: 15px !important">
                                            {{ addr.tenNguoiNhan }}
                                        </span>
                                        <v-chip v-if="addr.laMacDinh" color="success" size="x-small" variant="tonal"
                                            class="font-weight-medium px-2" style="font-size: 15px !important">Mặc
                                            định</v-chip>
                                    </div>

                                    <!-- SĐT -->
                                    <div class="d-flex align-center gap-2 mb-2 text-slate-600">
                                        <v-icon size="20" color="slate-400"
                                            style="font-size: 20px !important">mdi-phone-outline</v-icon>
                                        <span class="text-body-2 font-weight-medium"
                                            style="font-size: 14px !important">{{
                                                addr.sdtNguoiNhan
                                            }}</span>
                                    </div>

                                    <!-- Địa chỉ đầy đủ -->
                                    <div class="text-body-2 text-slate-500 mb-4 font-weight-medium line-height-1-6"
                                        style="font-size: 14px !important">
                                        {{ [addr.diaChiChiTiet, addr.phuongXa, addr.thanhPho, addr.tinh].filter(Boolean).join(', ') }}
                                    </div>

                                    <!-- Actions -->
                                    <div class="d-flex align-center gap-1 mt-2">
                                        <v-btn variant="text" icon size="small" color="primary" class="action-icon-btn"
                                            @click="openEditAddrForm(addr)">
                                            <PencilIcon size="18" />
                                            <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                                        </v-btn>
                                        <v-btn v-if="!addr.laMacDinh" variant="text" icon size="small" color="success"
                                            class="action-icon-btn" @click="confirmSetDefaultAddr(addr)">
                                            <StarIcon size="18" />
                                            <v-tooltip activator="parent" location="top">Đặt mặc định</v-tooltip>
                                        </v-btn>
                                        <v-btn v-if="!addr.laMacDinh" variant="text" icon size="small" color="error"
                                            class="action-icon-btn" @click="handleDeleteAddr(addr.id)">
                                            <TrashIcon size="18" />
                                            <v-tooltip activator="parent" location="top">Xóa địa chỉ</v-tooltip>
                                        </v-btn>
                                    </div>
                                </div>
                            </div>
                        </v-col>

                        <!-- ── Cột phải: Form thêm / sửa địa chỉ ── -->
                        <v-col cols="12" md="6" class="pb-6 bg-white overflow-y-auto"
                            style="max-height: calc(85vh - 80px)">
                            <div
                                class="px-8 pt-6 pb-4 d-flex align-center justify-space-between sticky-top bg-white z-10">
                                <span class="text-subtitle-2 font-weight-bold text-slate-800"
                                    style="font-size: 15px !important">
                                    {{ showAddrForm ? (isEditAddr ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ mới') : 'Thêm địa chỉ khác' }}
                                </span>
                                <v-btn v-if="!showAddrForm" variant="text" color="primary"
                                    class="text-none font-weight-bold px-4 rounded-lg text-primary bg-blue-lighten-5"
                                    style="
                                        font-size: 15px !important;
                                        height: 36px !important;
                                        min-height: 36px !important;
                                        box-shadow: none !important;
                                    " @click="openNewAddrForm">
                                    <PlusIcon size="14" class="mr-1" />
                                    <span style="
                                            font-size: 15px !important;
                                            font-weight: 600 !important;
                                            color: rgb(var(--v-theme-primary)) !important;
                                        ">Thêm địa chỉ mới</span>
                                </v-btn>
                                <v-btn v-else variant="text" size="small" color="slate-400"
                                    class="text-none font-weight-medium" @click="showAddrForm = false">
                                    <v-icon start size="16">mdi-arrow-left</v-icon>Hủy bỏ
                                </v-btn>
                            </div>

                            <!-- Placeholder khi chưa mở form -->
                            <div v-if="!showAddrForm"
                                class="d-flex flex-column align-center justify-center py-16 text-center"
                                style="opacity: 0.8; height: 100%">
                                <div class="icon-blob bg-slate-50 mb-4" style="width: 100px; height: 100px">
                                    <v-icon size="64" color="slate-300"
                                        style="font-size: 64px !important">mdi-map-marker-plus-outline</v-icon>
                                </div>
                                <div class="text-body-2 text-slate-400 font-weight-medium max-w-200 mx-auto px-8">
                                    Nhấn "Thêm địa chỉ mới" để đăng ký địa chỉ nhận hàng mới.
                                </div>
                            </div>

                            <!-- Form -->
                            <v-form v-else class="px-8 pt-2 pb-6">
                                <v-row dense>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Tên người nhận</div>
                                        <v-text-field v-model="addrForm.tenNguoiNhan" placeholder="Ví dụ: Nguyễn Văn A"
                                            variant="outlined" density="compact" hide-details />
                                    </v-col>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Số điện thoại</div>
                                        <v-text-field v-model="addrForm.sdtNguoiNhan" placeholder="09xx.xxx.xxx"
                                            variant="outlined" density="compact" hide-details />
                                    </v-col>
                                    <v-col cols="12">
                                        <div class="field-label">Tỉnh / Thành phố</div>
                                        <v-autocomplete v-model="addrForm.tinh" :items="provinces" item-title="name"
                                            item-value="code" placeholder="Chọn tỉnh thành" variant="outlined"
                                            density="compact" hide-details :loading="loadingLoc.provinces" />
                                    </v-col>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Quận / Huyện</div>
                                        <v-autocomplete v-model="addrForm.thanhPho" :items="districts" item-title="name"
                                            item-value="code" placeholder="Chọn quận huyện" variant="outlined"
                                            density="compact" hide-details :disabled="!addrForm.tinh"
                                            :loading="loadingLoc.districts" />
                                    </v-col>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Phường / Xã</div>
                                        <v-autocomplete v-model="addrForm.phuongXa" :items="wards" item-title="name"
                                            item-value="code" placeholder="Chọn phường xã" variant="outlined"
                                            density="compact" hide-details :disabled="!addrForm.thanhPho"
                                            :loading="loadingLoc.wards" />
                                    </v-col>
                                    <v-col cols="12">
                                        <div class="field-label">Địa chỉ cụ thể</div>
                                        <v-textarea v-model="addrForm.diaChiChiTiet"
                                            placeholder="Số nhà, tên đường, tòa nhà..." variant="outlined" rows="2"
                                            hide-details />
                                    </v-col>
                                    <v-col cols="12" v-if="listDiaChi.length > 0">
                                        <div class="pa-4 rounded-lg bg-slate-50">
                                            <v-checkbox v-model="addrForm.laMacDinh" color="primary" hide-details
                                                density="compact">
                                                <template v-slot:label>
                                                    <span class="text-body-2 font-weight-bold text-slate-600">Đặt làm
                                                        địa chỉ mặc định</span>
                                                </template>
                                            </v-checkbox>
                                        </div>
                                    </v-col>
                                    <v-col cols="12" class="mt-6">
                                        <v-btn color="primary" variant="flat" block
                                            class="text-none font-weight-bold rounded-xl shadow-sm hover-primary-btn"
                                            height="48" :loading="addrSaving" :disabled="!addrForm.tenNguoiNhan ||
                                                !addrForm.sdtNguoiNhan ||
                                                !addrForm.tinh ||
                                                !addrForm.thanhPho ||
                                                !addrForm.phuongXa ||
                                                !addrForm.diaChiChiTiet
                                                " @click="saveAddress">
                                            <v-icon start size="20" style="color: white !important">mdi-check</v-icon>
                                            <span style="color: white !important">{{
                                                isEditAddr ? 'Cập nhật địa chỉ' : 'Lưu địa chỉ mới'
                                                }}</span>
                                        </v-btn>
                                    </v-col>
                                </v-row>
                            </v-form>
                        </v-col>
                    </v-row>
                </v-card-text>
            </v-card>
        </v-dialog>
    </v-container>
</template>
<style scoped>
</style>
