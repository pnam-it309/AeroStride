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
    { text: 'Mã khách hàng', align: 'center', width: '100px' },
    { text: 'Tên khách hàng', align: 'left', width: '80px' },
    { text: 'Giới tính', align: 'center', width: '150px' },
    { text: 'Thông tin liên hệ', align: 'left', width: '150px' },
    { text: 'Địa chỉ', align: 'left', width: '220px' },
    { text: 'Trạng thái', align: 'center', width: '90px' },
    { text: 'Hành động', align: 'center', width: '120px' }
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
    <v-container
        fluid
        class="pa-4 animate-fade-in font-body"
        style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important"
    >
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
                    <td class="data-cell text-center text-slate-400">
                        {{ (pagination.page - 1) * pagination.size + index + 1 }}
                    </td>
                    <td class="data-cell text-center">{{ item.ma || '-' }}</td>
                    <td class="data-cell text-left">{{ item.ten || '-' }}</td>
                    <td class="data-cell text-center">
                        <v-chip
                            size="small"
                            variant="flat"
                            :class="['gender-chip', item.gioiTinh ? 'gender-chip-male' : 'gender-chip-female']"
                        >
                            {{ item.gioiTinh === true ? 'Nam' : item.gioiTinh === false ? 'Nữ' : '-' }}
                        </v-chip>
                    </td>
                    <td class="data-cell contact-cell text-left px-4">
                        <div class="d-inline-flex flex-column align-start">
                            <div class="info-line text-slate-700 mb-1">{{ item.sdt || '-' }}</div>
                            <div v-if="hasValue(item.email)" class="info-line d-flex align-center text-slate-500">
                                <v-icon size="14" class="mr-2">mdi-email-outline</v-icon>{{ item.email }}
                            </div>
                        </div>
                    </td>
                    <td class="data-cell">
                        <div class="line-clamp-2" :title="getAddressSummary(item)">{{ getAddressSummary(item) }}</div>
                    </td>
                    <td class="data-cell text-center">
                        <v-chip
                            size="small"
                            variant="flat"
                            :class="['status-chip', item.trangThai === 'DANG_HOAT_DONG' ? 'status-chip-active' : 'status-chip-inactive']"
                        >
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>
                    <td class="data-cell text-center action-cell">
                        <div class="d-flex align-center justify-center action-controls">
                            <!-- Địa chỉ -->
                            <v-btn variant="text" class="action-icon-btn" @click.stop="openAddrDialog(item)">
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
                    v-model="pagination.page"
                    :page-size="pagination.size"
                    @update:page-size="pagination.size = $event"
                    :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements"
                    :current-size="allCustomers.length"
                    @change="loadCustomers"
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
        <v-dialog v-model="addrDialog" max-width="1000" width="80vw" scrollable transition="dialog-bottom-transition">
            <v-card class="rounded-xl font-body addr-dialog-card" min-height="600">
                <!-- Tiêu đề dialog -->
                <v-card-title class="d-flex align-center pa-5 border-b">
                    <MapPinIcon size="20" class="mr-2 text-primary" />
                    <span class="font-weight-bold text-subtitle-1"> Quản lý địa chỉ — {{ selectedKH?.ten }} </span>
                    <v-spacer />
                    <v-btn icon variant="text" size="small" @click="addrDialog = false">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </v-card-title>

                <v-card-text class="pa-4" style="max-height: 85vh; overflow: hidden">
                    <v-row no-gutters style="height: 100%">
                        <!-- ── Cột trái: Danh sách địa chỉ ── -->
                        <v-col
                            cols="12"
                            md="6"
                            style="border-right: 1px solid rgba(0, 0, 0, 0.08); overflow-y: auto; max-height: 85vh"
                            class="px-6 pt-2 pb-4"
                        >
                            <div class="d-flex align-center justify-space-between px-5 pt-4 pb-2 sticky-sub-header bg-white">
                                <span class="text-subtitle-2 font-weight-medium text-dark">Địa chỉ hiện tại</span>
                                <v-chip size="small" variant="flat" class="gender-chip gender-chip-male">
                                    {{ listDiaChi.length }} địa chỉ
                                </v-chip>
                            </div>

                            <div v-if="addrLoading" class="text-center py-10">
                                <v-progress-circular indeterminate color="primary" size="32" />
                            </div>

                            <div v-else-if="listDiaChi.length === 0" class="text-center py-16 px-4 bg-slate-50/30 rounded-lg mx-5 mt-4">
                                <v-icon style="font-size: 36px !important; color: #1e293b !important;" class="mb-4">mdi-map-marker-off</v-icon>
                                <div class="text-slate-500" style="font-size: 14px !important; font-weight: 400 !important;">Chưa có địa chỉ nào</div>
                            </div>

                            <div v-else>
                                <div v-for="addr in listDiaChi" :key="addr.id" class="addr-card mx-5 mb-4 pa-5 border rounded-xl">
                                    <!-- Dòng tên + badge mặc định -->
                                    <div class="d-flex align-center gap-2 mb-1">
                                        <span class="font-weight-medium text-dark" style="font-size: 13px">
                                            {{ addr.tenNguoiNhan }}
                                        </span>
                                        <v-chip
                                            v-if="addr.laMacDinh"
                                            color="success"
                                            size="small"
                                            variant="flat"
                                            class="px-3"
                                            style="font-size: 11.5px; height: 24px; font-weight: 400"
                                            >mặc định</v-chip
                                        >
                                    </div>
                                    <!-- SĐT -->
                                    <div class="text-dark mb-1" style="font-size: 13.5px; opacity: 0.8">
                                        <v-icon size="14" class="mr-1">mdi-phone-outline</v-icon>{{ addr.sdtNguoiNhan }}
                                    </div>
                                    <!-- Địa chỉ đầy đủ -->
                                    <div class="text-dark" style="line-height: 1.6; color: #334155; font-size: 13.5px">
                                        {{ [addr.diaChiChiTiet, addr.phuongXa, addr.thanhPho, addr.tinh].filter(Boolean).join(', ') }}
                                    </div>
                                    <!-- Actions -->
                                    <div class="d-flex align-center gap-2 mt-5">
                                        <v-btn
                                            v-if="!addr.laMacDinh"
                                            variant="tonal"
                                            size="small"
                                            color="success"
                                            class="text-none rounded-lg px-4"
                                            height="32"
                                            style="font-weight: 400"
                                            @click="handleSetDefault(addr.id)"
                                        >
                                            <v-icon size="16" class="mr-1">mdi-star-outline</v-icon>Đặt mặc định
                                        </v-btn>
                                        <v-btn
                                            variant="tonal"
                                            size="small"
                                            color="primary"
                                            class="text-none rounded-lg px-4"
                                            height="32"
                                            style="font-weight: 400"
                                            @click="openEditAddrForm(addr)"
                                        >
                                            <v-icon size="16" class="mr-1">mdi-pencil</v-icon>Sửa
                                        </v-btn>
                                        <v-btn
                                            v-if="!addr.laMacDinh"
                                            variant="tonal"
                                            size="small"
                                            color="error"
                                            class="text-none rounded-lg px-4"
                                            height="32"
                                            style="font-weight: 400"
                                            @click="handleDeleteAddr(addr.id)"
                                        >
                                            <v-icon size="16" class="mr-1">mdi-delete-outline</v-icon>Xóa
                                        </v-btn>
                                    </div>
                                </div>
                            </div>
                        </v-col>

                        <!-- ── Cột phải: Form thêm / sửa địa chỉ ── -->
                        <v-col
                            cols="12"
                            md="6"
                            style="overflow-y: auto; max-height: 85vh"
                            class="px-8 pt-2 pb-4"
                            :style="{ background: showAddrForm ? 'white' : '#fafafa' }"
                        >
                            <div
                                class="px-5 pt-4 pb-2 d-flex align-center justify-space-between sticky-sub-header"
                                :style="{ background: showAddrForm ? 'white' : '#fafafa' }"
                            >
                                <span class="text-subtitle-2 font-weight-medium text-dark">
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
                            <div v-if="!showAddrForm" class="text-center py-16 px-4 bg-slate-50/30 rounded-lg mx-5 mt-4 border border-dashed border-slate-200">
                                <v-icon style="font-size: 36px !important; color: #1e293b !important;" class="mb-4">mdi-map-marker-plus</v-icon>
                                <div class="text-slate-500" style="font-size: 14px !important; font-weight: 400 !important;">
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
                                            hide-details
                                        />
                                    </v-col>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Số điện thoại *</div>
                                        <v-text-field
                                            v-model="addrForm.sdtNguoiNhan"
                                            placeholder="09xx..."
                                            variant="outlined"
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
                                            height="48"
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
                                            <span class="force-white-text">{{ isEditAddr ? 'Cập nhật địa chỉ' : 'Lưu địa chỉ' }}</span>
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

:deep(.data-cell),
:deep(.data-cell *) {
    font-size: 13px !important;
}

.filter-top {
    position: sticky;
    top: 8px;
    z-index: 6;
}

:deep(.status-chip-active),
:deep(.gender-chip-male) {
    background-color: #f0f1ff !important;
    color: #1e257c !important;
    font-weight: 400 !important;
}
:deep(.status-chip-active .v-chip__content),
:deep(.gender-chip-male .v-chip__content) {
    color: #1e257c !important;
}

:deep(.status-chip-inactive) {
    background-color: #f8fafc !important;
    color: #64748b !important;
    font-weight: 400 !important;
}
:deep(.status-chip-inactive .v-chip__content) {
    color: #64748b !important;
}

:deep(.gender-chip-female) {
    background-color: #fef2f2 !important;
    color: #991b1b !important;
    font-weight: 400 !important;
}
:deep(.gender-chip-female .v-chip__content) {
    color: #991b1b !important;
}

:deep(.status-chip),
:deep(.gender-chip) {
    border-radius: 12px !important;
    font-size: 13px !important;
    padding: 0 16px !important;
    min-height: 28px !important;
    display: inline-flex !important;
    align-items: center !important;
    justify-content: center !important;
    font-weight: 400 !important;
}

:deep(.gender-chip) {
    min-width: 80px !important;
}

.addr-dialog-card :deep(.v-card-title),
.addr-dialog-card :deep(.v-card-text),
.addr-dialog-card :deep(.field-label),
.addr-dialog-card :deep(.text-caption),
.addr-dialog-card {
    font-family: 'Inter', sans-serif !important;
}

.addr-dialog-card :deep(.v-card-title),
.addr-dialog-card :deep(.v-card-text),
.addr-dialog-card :deep(.field-label),
.addr-dialog-card :deep(.text-caption),
.addr-dialog-card :deep(.v-btn__content),
.addr-dialog-card :deep(.v-field__input),
.addr-dialog-card :deep(.v-list-item-title),
.addr-dialog-card :deep(span),
.addr-dialog-card :deep(div) {
    font-family: 'Inter', sans-serif !important;
    font-size: 14px !important;
}

.addr-dialog-card :deep(.v-card-title span) {
    font-size: 17px !important;
    font-weight: 700 !important;
}

:deep(.force-white-text) {
    color: #ffffff !important;
}

/* Loại bỏ lớp phủ mờ của Vuetify trên nút bấm */
.addr-dialog-card :deep(.v-btn__overlay),
.addr-dialog-card :deep(.v-btn__underlay) {
    display: none !important;
}

.addr-dialog-card :deep(.v-btn) {
    opacity: 1 !important;
}

.addr-card {
    transition: box-shadow 0.15s;
}

.addr-card:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
</style>

<style>
/* 
   Đồng bộ giao diện Dialog Địa chỉ 
*/
.addr-dialog-card,
.addr-dialog-card *,
.addr-dialog-card .v-table th,
.addr-dialog-card .v-table td,
.addr-dialog-card .v-btn__content,
.addr-dialog-card .v-field__input {
    font-size: 13px !important;
    font-family: 'Inter', sans-serif !important;
    text-transform: none !important;
    font-weight: 500 !important;
    vertical-align: middle !important;
}

.addr-dialog-card .v-card-title,
.addr-dialog-card .v-card-title *,
.addr-dialog-card .font-weight-bold,
.addr-dialog-card .font-weight-black {
    font-weight: 700 !important;
}

/* Khoảng cách cho nội dung dialog */
.addr-dialog-card .v-card-text {
    padding-top: 0px !important;
}

.sticky-sub-header {
    position: sticky;
    top: 0;
    z-index: 10;
    margin-top: -8px; /* Offset for better alignment */
    padding-bottom: 12px !important;
}
</style>
