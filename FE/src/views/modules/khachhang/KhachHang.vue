<script setup>
import { computed, ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';

import { useUIStore } from '@/stores/ui';
import { AdminFilter, AdminTable, AdminPagination, AdminConfirm, AdminBreadcrumbs } from '@/components/common';
import { downloadFile } from '@/utils/fileUtils';
import { formatDateTime } from '@/utils/formatters';
import { isActiveStatus, getStatusLabel, getStatusColor } from '@/utils/statusUtils';
import { EditIcon, EyeIcon, MapPinIcon } from 'vue-tabler-icons';
import axios from 'axios';

import { useAdminTable } from '@/composables/useAdminTable';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useNotifications } from '@/services/notificationService';

const isRefreshing = ref(false);

const {
    items: allCustomers,
    loading,
    pagination,
    filters,
    loadData: loadCustomers,
    handleFilter: handleLocalFilterChange,
    handleReset
} = useAdminTable(dichVuKhachHang.layKhachHangPhanTrang, { search: '', gioiTinh: null, trangThai: null });

const handleRefresh = async () => {
    isRefreshing.value = true;
    handleReset();
    setTimeout(() => (isRefreshing.value = false), 800);
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

const customerBreadcrumbs = [
    { title: 'Quản lý tài khoản', disabled: false, href: '#' },
    { title: 'Khách hàng', disabled: true }
];

watch(addrDialog, (isOpen) => {
    if (isOpen) {
        uiStore.setBreadcrumbs([
            { title: 'Quản lý tài khoản', disabled: false, href: '#' },
            { title: 'Khách hàng', disabled: false, to: '/khach-hang' },
            { title: `Địa chỉ: ${selectedKH.value?.ten || ''}`, disabled: true }
        ]);
    } else {
        uiStore.setBreadcrumbs(customerBreadcrumbs);
    }
});

// ─── Address Dialog ───────────────────────────────────────────────
const addrLoading = ref(false);
const addrSaving = ref(false);
const listDiaChi = ref([]);

const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);
const loadingLoc = ref({ provinces: false, districts: false, wards: false });

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

const fetchProvinces = async () => {
    if (provinces.value.length) return;
    loadingLoc.value.provinces = true;
    try {
        const res = await axios.get('https://provinces.open-api.vn/api/p/');
        provinces.value = res.data;
    } finally {
        loadingLoc.value.provinces = false;
    }
};

const fetchDistricts = async (code) => {
    if (!code) return;
    loadingLoc.value.districts = true;
    districts.value = [];
    wards.value = [];
    try {
        const res = await axios.get(`https://provinces.open-api.vn/api/p/${code}?depth=2`);
        districts.value = res.data.districts;
    } finally {
        loadingLoc.value.districts = false;
    }
};

const fetchWards = async (code) => {
    if (!code) return;
    loadingLoc.value.wards = true;
    wards.value = [];
    try {
        const res = await axios.get(`https://provinces.open-api.vn/api/d/${code}?depth=2`);
        wards.value = res.data.wards;
    } finally {
        loadingLoc.value.wards = false;
    }
};

const openAddrDialog = async (item) => {
    selectedKH.value = item;
    showAddrForm.value = false;
    isEditAddr.value = false;
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
    addrLoading.value = true;
    try {
        const res = await dichVuKhachHang.layDanhSachDiaChi(khId);
        listDiaChi.value = res?.content || res?.data || res || [];
    } catch (e) {
        console.error('Error loading addresses:', e);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể tải danh sách địa chỉ',
            color: 'error'
        });
        listDiaChi.value = [];
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
    addrForm.value = { ...addr };
    showAddrForm.value = true;
    const p = provinces.value.find((x) => x.name === addr.tinh);
    if (p) {
        await fetchDistricts(p.code);
        const d = districts.value.find((x) => x.name === addr.thanhPho);
        if (d) await fetchWards(d.code);
    }
};

const saveAddress = async () => {
    addrSaving.value = true;
    try {
        const payload = { ...addrForm.value, idKhachHang: selectedKH.value.id };
        const p = provinces.value.find((x) => x.code === addrForm.value.tinh);
        const d = districts.value.find((x) => x.code === addrForm.value.thanhPho);
        const w = wards.value.find((x) => x.code === addrForm.value.phuongXa);
        if (p) payload.tinh = p.name;
        if (d) payload.thanhPho = d.name;
        if (w) payload.phuongXa = w.name;

        if (isEditAddr.value) await dichVuKhachHang.capNhatDiaChi(addrForm.value.id, payload);
        else await dichVuKhachHang.taoDiaChi(payload);

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

const tableHeaders = [
    { text: 'STT', align: 'center', width: '60px' },
    { text: 'Mã khách hàng', align: 'center', width: '110px' },
    { text: 'Tên khách hàng', align: 'center', width: '100px' },
    { text: 'Giới tính', align: 'center', width: '110px' },
    { text: 'Thông tin liên hệ', align: 'left', width: '220px' },
    { text: 'Địa chỉ', align: 'center', width: '200px' },
    { text: 'Trạng thái', align: 'center', width: '100px' },
    { text: 'Hành động', align: 'center', width: '140px' }
];

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
                const newS = item.trangThai === 'DANG_HOAT_DONG' ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
                await dichVuKhachHang.thayDoiTrangThaiKhachHang(item.id, newS);
                item.trangThai = newS;
            } catch (e) {
                console.error(e);
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
</script>

<template>
    <v-container fluid class="pa-4 animate-fade-in font-body" style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important;">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="customerBreadcrumbs" />

        <div class="mb-2"></div>

        <!-- Filter -->
        <div class="filter-top invoice-filter-shell">
            <AdminFilter title="Bộ lọc" :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
                <!-- Tìm kiếm -->
                <v-col cols="12" md="4" class="filter-cell">
                    <div class="filter-field-label">Tìm kiếm</div>
                    <v-text-field
                        v-model="filters.search"
                        placeholder="Tên, SĐT, Email, Mã..."
                        variant="outlined"
                        density="compact"
                        hide-details
                        prepend-inner-icon="mdi-magnify"
                        class="compact-input"
                        @input="handleLocalFilterChange"
                    />
                </v-col>

                <!-- Giới tính -->
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Giới tính</div>
                    <v-select
                        v-model="filters.gioiTinh"
                        :items="[
                            { title: 'Tất cả', value: null },
                            { title: 'Nam', value: true },
                            { title: 'Nữ', value: false }
                        ]"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @update:model-value="handleLocalFilterChange"
                    />
                </v-col>

                <!-- Trạng thái -->
                <v-col cols="12" md="3" class="filter-cell">
                    <div class="filter-field-label">Trạng thái</div>
                    <v-select
                        v-model="filters.trangThai"
                        :items="[
                            { title: 'Tất cả', value: null },
                            { title: 'Hoạt động', value: 'DANG_HOAT_DONG' },
                            { title: 'Ngừng hoạt động', value: 'KHONG_HOAT_DONG' }
                        ]"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="compact-input"
                        @update:model-value="handleLocalFilterChange"
                    />
                </v-col>
            </AdminFilter>
        </div>

        <!-- Table — không có tab, chỉ có bảng -->
        <AdminTable
            title="Danh sách khách hàng"
            addButtonText="Thêm khách hàng"
            show-export-button
            :headers="tableHeaders"
            :items="allCustomers"
            :total-count="pagination.totalElements"
            :loading="loading"
            @add="router.push({ name: 'KhachHangForm' })"
            @export="handleExport"
        >
            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell center-cell">{{ (pagination.page - 1) * pagination.size + index + 1 }}</td>
                    <td class="data-cell text-center">{{ item.ma || '-' }}</td>
                    <td class="data-cell text-center">{{ item.ten || '-' }}</td>
                    <td class="data-cell center-cell">
                        <v-chip
                            size="small"
                            variant="tonal"
                            :class="['gender-chip', item.gioiTinh ? 'gender-chip-male' : 'gender-chip-female']"
                        >
                            {{ item.gioiTinh === true ? 'Nam' : item.gioiTinh === false ? 'Nữ' : '-' }}
                        </v-chip>
                    </td>
                    <td class="data-cell contact-cell text-left px-4">
                        <div class="d-inline-flex flex-column align-start">
                            <div class="info-line mb-1">{{ item.sdt || '-' }}</div>
                            <div v-if="hasValue(item.email)" class="info-line d-flex align-center text-slate-500">
                                <v-icon size="14" class="mr-2">mdi-email-outline</v-icon>{{ item.email }}
                            </div>
                        </div>
                    </td>
                    <td class="data-cell">
                        <div class="line-clamp-2" :title="getAddressSummary(item)">{{ getAddressSummary(item) }}</div>
                    </td>
                    <td class="data-cell">
                        <v-chip
                            size="small"
                            variant="flat"
                            :class="['status-chip', item.trangThai === 'DANG_HOAT_DONG' ? 'status-chip-active' : 'status-chip-inactive']"
                        >
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>
                    <td class="data-cell center-cell action-cell">
                        <div class="d-flex align-center justify-center action-controls">
                            <!-- Địa chỉ -->
                            <v-btn
                                variant="text"
                                class="action-icon-btn"
                                @click.stop="openAddrDialog(item)"
                            >
                                <MapPinIcon />
                                <v-tooltip activator="parent" location="top">Quản lý địa chỉ</v-tooltip>
                            </v-btn>
                            <!-- Xem chi tiết -->
                            <v-btn
                                variant="text"
                                class="action-icon-btn"
                                @click.stop="router.push({ name: 'KhachHangDetail', params: { id: item.id } })"
                            >
                                <EyeIcon />
                                <v-tooltip activator="parent" location="top">Xem chi tiết</v-tooltip>
                            </v-btn>
                            <!-- Chỉnh sửa -->
                            <v-btn
                                variant="text"
                                class="action-icon-btn"
                                @click.stop="router.push({ name: 'KhachHangForm', params: { id: item.id } })"
                            >
                                <EditIcon />
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>
                            <!-- Switch trạng thái -->
                            <div class="switch-wrapper">
                                <v-switch
                                    :model-value="isActiveStatus(item.trangThai)"
                                    color="primary"
                                    hide-details
                                    density="compact"
                                    class="tight-switch action-switch"
                                    @click.prevent.stop="confirmChangeStatus(item)"
                                />
                                <v-tooltip activator="parent" location="top">Chuyển đổi trạng thái</v-tooltip>
                            </div>
                        </div>
                    </td>
                </tr>
            </template>

            <template #pagination>
                <AdminPagination
                    v-model:page="pagination.page"
                    v-model:page-size="pagination.size"
                    :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements"
                    :current-size="allCustomers.length"
                    @change="handleLocalFilterChange"
                />
            </template>
        </AdminTable>

        <!-- Confirm Dialog -->
        <AdminConfirm
            v-model:show="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :color="confirmDialog.color"
            :loading="confirmDialog.loading"
            @confirm="handleConfirm(true)"
            @cancel="handleConfirm(false)"
        />

        <!-- ═══════════════════════════════════════════════════
         Dialog Quản lý địa chỉ (2 cột: danh sách | form)
         ═══════════════════════════════════════════════════ -->
        <v-dialog v-model="addrDialog" max-width="960" scrollable transition="dialog-bottom-transition">
            <v-card class="rounded-xl">
                <!-- Tiêu đề dialog -->
                <v-card-title class="d-flex align-center pa-5 border-b">
                    <MapPinIcon size="20" class="mr-2 text-primary" />
                    <span class="font-weight-bold text-subtitle-1"> Quản lý địa chỉ — {{ selectedKH?.ten }} </span>
                    <v-spacer />
                    <v-btn icon variant="text" size="small" @click="addrDialog = false">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </v-card-title>

                <v-card-text class="pa-0" style="max-height: 72vh; overflow: hidden">
                    <v-row no-gutters style="height: 100%">
                        <!-- ── Cột trái: Danh sách địa chỉ ── -->
                        <v-col cols="12" md="6" style="border-right: 1px solid rgba(0, 0, 0, 0.08); overflow-y: auto; max-height: 72vh">
                            <div class="d-flex align-center justify-space-between px-5 pt-4 pb-2">
                                <span class="text-subtitle-2 font-weight-bold text-dark">Địa chỉ hiện tại</span>
                                <v-chip size="x-small" color="info" variant="tonal" class="font-weight-bold">
                                    {{ listDiaChi.length }} địa chỉ
                                </v-chip>
                            </div>

                            <div v-if="addrLoading" class="text-center py-10">
                                <v-progress-circular indeterminate color="primary" size="32" />
                            </div>

                            <div v-else-if="listDiaChi.length === 0" class="text-center py-12">
                                <v-icon size="40" color="grey-lighten-2">mdi-map-marker-off</v-icon>
                                <div class="mt-3 text-caption text-medium-emphasis">Chưa có địa chỉ nào</div>
                            </div>

                            <div v-else>
                                <div v-for="addr in listDiaChi" :key="addr.id" class="addr-card mx-4 mb-3 pa-4 border rounded-xl">
                                    <!-- Dòng tên + badge mặc định -->
                                    <div class="d-flex align-center gap-2 mb-1">
                                        <span class="font-weight-bold text-dark" style="font-size: 13px">
                                            {{ addr.tenNguoiNhan }}
                                        </span>
                                        <v-chip
                                            v-if="addr.laMacDinh"
                                            color="success"
                                            size="x-small"
                                            variant="flat"
                                            class="font-weight-bold px-2"
                                            >Mặc định</v-chip
                                        >
                                    </div>
                                    <!-- SĐT -->
                                    <div class="text-caption text-medium-emphasis mb-1">
                                        <v-icon size="12" class="mr-1">mdi-phone-outline</v-icon>{{ addr.sdtNguoiNhan }}
                                    </div>
                                    <!-- Địa chỉ đầy đủ -->
                                    <div class="text-caption" style="line-height: 1.5; color: #475569">
                                        {{ [addr.diaChiChiTiet, addr.phuongXa, addr.thanhPho, addr.tinh].filter(Boolean).join(', ') }}
                                    </div>
                                    <!-- Actions -->
                                    <div class="d-flex align-center gap-2 mt-3">
                                        <v-btn
                                            v-if="!addr.laMacDinh"
                                            variant="tonal"
                                            size="x-small"
                                            color="success"
                                            class="text-none font-weight-bold"
                                            @click="handleSetDefault(addr.id)"
                                        >
                                            <v-icon size="12" class="mr-1">mdi-star-outline</v-icon>Đặt mặc định
                                        </v-btn>
                                        <v-btn
                                            variant="tonal"
                                            size="x-small"
                                            color="primary"
                                            class="text-none"
                                            @click="openEditAddrForm(addr)"
                                        >
                                            <v-icon size="12" class="mr-1">mdi-pencil</v-icon>Sửa
                                        </v-btn>
                                        <v-btn
                                            v-if="!addr.laMacDinh"
                                            variant="tonal"
                                            size="x-small"
                                            color="error"
                                            class="text-none"
                                            @click="handleDeleteAddr(addr.id)"
                                        >
                                            <v-icon size="12" class="mr-1">mdi-delete-outline</v-icon>Xóa
                                        </v-btn>
                                    </div>
                                </div>
                            </div>
                        </v-col>

                        <!-- ── Cột phải: Form thêm / sửa địa chỉ ── -->
                        <v-col cols="12" md="6" style="overflow-y: auto; max-height: 72vh">
                            <div class="px-5 pt-4 pb-2 d-flex align-center justify-space-between">
                                <span class="text-subtitle-2 font-weight-bold text-dark">
                                    {{ showAddrForm ? (isEditAddr ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ mới') : 'Thêm địa chỉ khác' }}
                                </span>
                                <v-btn
                                    v-if="!showAddrForm"
                                    color="primary"
                                    variant="tonal"
                                    size="small"
                                    class="text-none font-weight-bold"
                                    prepend-icon="mdi-plus"
                                    @click="openNewAddrForm"
                                >
                                    Thêm địa chỉ
                                </v-btn>
                                <v-btn v-else variant="text" size="small" class="text-none" @click="showAddrForm = false">
                                    <v-icon size="14" class="mr-1">mdi-arrow-left</v-icon>Hủy
                                </v-btn>
                            </div>

                            <!-- Placeholder khi chưa mở form -->
                            <div v-if="!showAddrForm" class="text-center py-16 mx-4">
                                <v-icon size="48" color="grey-lighten-2">mdi-map-marker-plus</v-icon>
                                <div class="mt-3 text-caption text-medium-emphasis">
                                    Nhấn "Thêm địa chỉ" để đăng ký địa chỉ nhận hàng mới
                                </div>
                            </div>

                            <!-- Form -->
                            <v-form v-else class="px-5 pt-2 pb-6">
                                <v-row dense>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Tên người nhận *</div>
                                        <v-text-field
                                            v-model="addrForm.tenNguoiNhan"
                                            placeholder="Nhập tên"
                                            variant="outlined"
                                            density="compact"
                                            hide-details
                                        />
                                    </v-col>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Số điện thoại *</div>
                                        <v-text-field
                                            v-model="addrForm.sdtNguoiNhan"
                                            placeholder="09xx..."
                                            variant="outlined"
                                            density="compact"
                                            hide-details
                                        />
                                    </v-col>
                                    <v-col cols="12">
                                        <div class="field-label">Tỉnh / Thành phố *</div>
                                        <v-autocomplete
                                            v-model="addrForm.tinh"
                                            :items="provinces"
                                            item-title="name"
                                            item-value="code"
                                            placeholder="Chọn tỉnh"
                                            variant="outlined"
                                            density="compact"
                                            hide-details
                                            :loading="loadingLoc.provinces"
                                        />
                                    </v-col>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Quận / Huyện *</div>
                                        <v-autocomplete
                                            v-model="addrForm.thanhPho"
                                            :items="districts"
                                            item-title="name"
                                            item-value="code"
                                            placeholder="Chọn quận/huyện"
                                            variant="outlined"
                                            density="compact"
                                            hide-details
                                            :disabled="!addrForm.tinh"
                                            :loading="loadingLoc.districts"
                                        />
                                    </v-col>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Phường / Xã *</div>
                                        <v-autocomplete
                                            v-model="addrForm.phuongXa"
                                            :items="wards"
                                            item-title="name"
                                            item-value="code"
                                            placeholder="Chọn phường/xã"
                                            variant="outlined"
                                            density="compact"
                                            hide-details
                                            :disabled="!addrForm.thanhPho"
                                            :loading="loadingLoc.wards"
                                        />
                                    </v-col>
                                    <v-col cols="12">
                                        <div class="field-label">Địa chỉ cụ thể *</div>
                                        <v-textarea
                                            v-model="addrForm.diaChiChiTiet"
                                            placeholder="Số nhà, tên đường..."
                                            variant="outlined"
                                            rows="2"
                                            hide-details
                                        />
                                    </v-col>
                                    <v-col cols="12" v-if="listDiaChi.length > 0">
                                        <v-checkbox
                                            v-model="addrForm.laMacDinh"
                                            label="Đặt làm địa chỉ mặc định"
                                            color="primary"
                                            hide-details
                                            density="compact"
                                        />
                                    </v-col>
                                    <v-col cols="12" class="mt-2">
                                        <v-btn
                                            color="primary"
                                            variant="flat"
                                            block
                                            class="text-none font-weight-bold rounded-lg"
                                            height="40"
                                            :loading="addrSaving"
                                            :disabled="
                                                !addrForm.tenNguoiNhan ||
                                                !addrForm.sdtNguoiNhan ||
                                                !addrForm.tinh ||
                                                !addrForm.thanhPho ||
                                                !addrForm.phuongXa ||
                                                !addrForm.diaChiChiTiet
                                            "
                                            @click="saveAddress"
                                        >
                                            {{ isEditAddr ? 'Cập nhật địa chỉ' : 'Lưu địa chỉ' }}
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
/* Scoped styles removed in favor of global _admin-common.scss */
.contact-cell {
    text-align: left !important;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
}
.line-clamp-2 {
    display: -webkit-box;
    line-clamp: 2;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
.addr-card {
    transition: box-shadow 0.15s;
}
.addr-card:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
</style>
