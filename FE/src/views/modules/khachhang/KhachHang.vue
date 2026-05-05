<script setup>
import { computed, ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';

import { useUIStore } from '@/stores/ui';
import { AdminFilter, AdminTable, AdminPagination, AdminConfirm, AdminBreadcrumbs } from '@/components/common';
import { downloadFile } from '@/utils/fileUtils';
import { formatDateTime } from '@/utils/formatters';
import { isActiveStatus, getStatusLabel, getStatusColor } from '@/utils/statusUtils';
import { EditIcon, MapPinIcon } from 'vue-tabler-icons';
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
    
    // 1. Khởi tạo từ danh sách địa chỉ lồng nhau (nếu có)
    const existing = item.diaChis || item.listDiaChi || [];
    let initialList = Array.isArray(existing) ? [...existing] : [];

    // 2. Fallback: Nếu không có danh sách, nhưng có thông tin địa chỉ phẳng ở Root
    if (initialList.length === 0 && (item.tinh || item.thanhPho || item.diaChiChiTiet)) {
        initialList.push({
            id: 'root-' + item.id,
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
        
        // Cấu trúc phân tích dữ liệu đa lớp cực kỳ linh hoạt
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

        // Chỉ cập nhật nếu tìm thấy mảng dữ liệu thực tế từ API
        if (Array.isArray(data) && data.length > 0) {
            listDiaChi.value = [...data];
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

    // Hàm chuẩn hóa địa danh cực mạnh để khớp lệnh
    const clean = (s) => {
        if (!s) return '';
        let str = String(s).toLowerCase()
            .replace(/\s+/g, ' ')
            .trim();
        
        // Đặc trị các thành phố lớn thường bị viết tắt
        if (str.includes('hồ chí minh') || str.includes('hcm')) return 'hcm';
        if (str.includes('hà nội') || str === 'hn') return 'hanoi';
        if (str.includes('đà nẵng') || str === 'dn') return 'danang';

        // Loại bỏ tiền tố viết tắt và tiền tố đầy đủ
        return str
            .replace(/^(thành phố|tỉnh|quận|huyện|phường|xã|thị xã|thị trấn|tp\.?|t\.?|q\.?|h\.?|x\.?)\s+/gi, '')
            .replace(/\s+/g, '') // Xóa trắng để so sánh chuỗi dính liền (ví dụ: "tháibình")
            .trim();
    };

    try {
        // Xử lý trường hợp dữ liệu bị dồn hết vào diaChiChiTiet (Flat address)
        let tinhName = addr.tinh;
        let huyenName = addr.thanhPho;
        let xaName = addr.phuongXa;

        if (!tinhName && addr.diaChiChiTiet?.includes(',')) {
            const parts = addr.diaChiChiTiet.split(',').map(p => p.trim());
            if (parts.length >= 4) {
                // Định dạng: [Số nhà], [Phường], [Quận], [Tỉnh]
                tinhName = parts[parts.length - 1];
                huyenName = parts[parts.length - 2];
                xaName = parts[parts.length - 3];
                addrForm.value.diaChiChiTiet = parts.slice(0, parts.length - 3).join(', ');
            }
        }

        // 1. Khớp Tỉnh
        const p = provinces.value.find(x => clean(x.name) === clean(tinhName) || x.code === tinhName);
        if (p) {
            addrForm.value.tinh = p.code;
            await fetchDistricts(p.code);
            
            // 2. Khớp Huyện
            const d = districts.value.find(x => clean(x.name) === clean(huyenName) || x.code === huyenName);
            if (d) {
                addrForm.value.thanhPho = d.code;
                await fetchWards(d.code);
                
                // 3. Khớp Xã
                const w = wards.value.find(x => clean(x.name) === clean(xaName) || x.code === xaName);
                if (w) {
                    addrForm.value.phuongXa = w.code;
                }
            }
        }
    } catch (error) {
        console.error('Lỗi nạp địa chỉ:', error);
    }
};

const saveAddress = async () => {
    addrSaving.value = true;
    try {
        // Chuẩn bị payload
        const payload = { 
            ...addrForm.value, 
            idKhachHang: selectedKH.value.id 
        };

        // Chuyển đổi Code -> Name trước khi gửi lên Server
        const p = provinces.value.find((x) => x.code === addrForm.value.tinh);
        const d = districts.value.find((x) => x.code === addrForm.value.thanhPho);
        const w = wards.value.find((x) => x.code === addrForm.value.phuongXa);
        
        if (p) payload.tinh = p.name;
        if (d) payload.thanhPho = d.name;
        if (w) payload.phuongXa = w.name;

        // Xử lý trường hợp ID giả (synthesized address)
        const isRealId = payload.id && !String(payload.id).startsWith('root-');

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
    { text: 'STT', width: '50px', align: 'center' },
    { text: 'Mã khách hàng', width: '100px', align: 'center' },
    { text: 'Tên khách hàng', width: '80px', align: 'start' },
    { text: 'Giới tính', width: '90px', align: 'center' },
    { text: 'Thông tin liên hệ', width: '120px', align: 'start' },
    { text: 'Địa chỉ', width: '180px', align: 'start' },
    { text: 'Trạng thái', width: '90px', align: 'center' },
    { text: 'Hành động', width: '100px', align: 'center' }
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
            addButtonText="Tạo mới"
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
                    <td class="data-cell text-slate-400">
                        {{ (pagination.page - 1) * pagination.size + index + 1 }}
                    </td>
                    <td class="data-cell">
                        <div class="text-truncate" :title="item.ma">{{ item.ma || '-' }}</div>
                    </td>
                    <td class="data-cell text-left px-4">
                        <div class="text-truncate font-weight-medium text-slate-700" :title="item.ten">{{ item.ten || '-' }}</div>
                    </td>
                    <td class="data-cell">
                        <v-chip
                            size="small"
                            variant="flat"
                            :class="['gender-chip', item.gioiTinh ? 'gender-chip-male' : 'gender-chip-female']"
                        >
                            {{ item.gioiTinh === true ? 'Nam' : item.gioiTinh === false ? 'Nữ' : '-' }}
                        </v-chip>
                    </td>
                    <td class="data-cell contact-cell px-4">
                        <div class="d-inline-flex flex-column align-start" style="width: 100%; overflow: hidden;">
                            <div class="info-line text-slate-700 mb-1 text-truncate" style="width: 100%;" :title="item.sdt">
                                <v-icon size="14" class="mr-2 text-slate-400">mdi-phone</v-icon>
                                <span>{{ item.sdt || '-' }}</span>
                            </div>
                            <div v-if="hasValue(item.email)" class="info-line d-flex align-center text-slate-500 text-truncate" style="width: 100%;" :title="item.email">
                                <v-icon size="14" class="mr-2">mdi-email-outline</v-icon>{{ item.email }}
                            </div>
                        </div>
                    </td>
                    <td class="data-cell text-left px-4">
                        <div class="line-clamp-2 text-slate-500" :title="getAddressSummary(item)">{{ getAddressSummary(item) }}</div>
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
                    <td class="data-cell action-cell">
                        <div class="d-flex align-center justify-center action-controls">
                            <!-- Địa chỉ -->
                            <v-btn variant="text" class="action-icon-btn" @click.stop="openAddrDialog(item)">
                                <MapPinIcon />
                                <v-tooltip activator="parent" location="top">Quản lý địa chỉ</v-tooltip>
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
        <v-dialog v-model="addrDialog" max-width="1100" width="90vw" scrollable transition="scale-transition">
            <v-card class="rounded-xl font-body addr-dialog-card overflow-hidden" min-height="650">
                <!-- Tiêu đề dialog -->
                <v-card-title class="d-flex align-center pa-6 border-b bg-slate-50">
                    <div class="icon-blob bg-blue-lighten-5 mr-4" style="width: 50px; height: 50px">
                        <MapPinIcon :size="30" class="text-primary" />
                    </div>
                    <div>
                        <div class="text-h6 font-weight-black text-slate-800 line-height-1">Quản lý địa chỉ — {{ selectedKH?.ten }}</div>
                    </div>
                    <v-spacer />
                    <v-btn icon variant="text" size="small" color="slate-400" @click="addrDialog = false">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </v-card-title>

                <v-card-text class="pa-0" style="max-height: 85vh; overflow: hidden">
                    <v-row no-gutters style="height: 100%">
                        <!-- ── Cột trái: Danh sách địa chỉ ── -->
                        <v-col cols="12" md="6" class="pb-6 border-e overflow-y-auto" style="max-height: calc(85vh - 80px)">
                            <div class="px-8 pt-6 pb-4 d-flex align-center justify-space-between sticky-top bg-white z-10">
                                <span class="text-subtitle-2 font-weight-bold text-slate-800">Địa chỉ hiện tại</span>

                            </div>

                            <div v-if="addrLoading" class="text-center py-16">
                                <v-progress-circular indeterminate color="primary" size="40" />
                            </div>

                            <div v-else-if="listDiaChi.length === 0" class="text-center py-16 px-8 mx-8 bg-slate-50/50 rounded-xl border-dashed border mt-4">
                                <v-icon size="48" color="slate-200" class="mb-4">mdi-map-marker-off</v-icon>
                                <div class="text-slate-400 font-weight-medium">Chưa có địa chỉ nào được đăng ký</div>
                            </div>

                            <div v-else class="px-8">
                                <div v-for="addr in listDiaChi" :key="addr.id" 
                                    class="pa-6 rounded-xl bg-blue-lighten-5 border border-blue-lighten-4 mb-6 hover-shadow-sm transition-all position-relative"
                                    style="margin-top: 15px">
                                    <!-- Dòng tên + badge mặc định -->
                                    <div class="d-flex align-center gap-3 mb-3">
                                        <span class="text-subtitle-1 font-weight-medium text-slate-700">
                                            {{ addr.tenNguoiNhan }}
                                        </span>
                                        <v-chip v-if="addr.laMacDinh" color="success" size="x-small" variant="tonal" class="font-weight-medium px-2">Mặc định</v-chip>
                                    </div>
                                    
                                    <!-- SĐT -->
                                    <div class="d-flex align-center gap-2 mb-2 text-slate-600">
                                        <v-icon size="20" color="slate-400" style="font-size: 20px !important">mdi-phone-outline</v-icon>
                                        <span class="text-body-2 font-weight-medium">{{ addr.sdtNguoiNhan }}</span>
                                    </div>
                                    
                                    <!-- Địa chỉ đầy đủ -->
                                    <div class="text-body-2 text-slate-500 mb-4 font-weight-medium line-height-1-6">
                                        {{ [addr.diaChiChiTiet, addr.phuongXa, addr.thanhPho, addr.tinh].filter(Boolean).join(', ') }}
                                    </div>
                                    
                                    <!-- Actions -->
                                    <div class="d-flex align-center gap-1 mt-2">
                                        <v-btn variant="text" size="small" color="primary" class="text-none px-0 font-weight-medium h-auto min-width-0 mr-4" @click="openEditAddrForm(addr)">
                                            <v-icon start size="20" style="font-size: 20px !important" class="mr-1">mdi-pencil</v-icon>
                                            Sửa
                                        </v-btn>
                                        <v-btn v-if="!addr.laMacDinh" variant="text" size="small" color="success" class="text-none px-0 font-weight-medium h-auto min-width-0 mr-4" @click="handleSetDefault(addr.id)">
                                            <v-icon start size="20" style="font-size: 20px !important" class="mr-1">mdi-star-outline</v-icon>
                                            Mặc định
                                        </v-btn>
                                        <v-btn v-if="!addr.laMacDinh" variant="text" size="small" color="error" class="text-none px-0 font-weight-medium h-auto min-width-0" @click="handleDeleteAddr(addr.id)">
                                            <v-icon start size="20" style="font-size: 20px !important" class="mr-1">mdi-delete-outline</v-icon>
                                            Xóa
                                        </v-btn>
                                    </div>
                                </div>
                            </div>
                        </v-col>

                        <!-- ── Cột phải: Form thêm / sửa địa chỉ ── -->
                        <v-col cols="12" md="6" class="pb-6 bg-white overflow-y-auto" style="max-height: calc(85vh - 80px)">
                            <div class="px-8 pt-6 pb-4 d-flex align-center justify-space-between sticky-top bg-white z-10">
                                <span class="text-subtitle-2 font-weight-bold text-slate-800">
                                    {{ showAddrForm ? (isEditAddr ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ mới') : 'Thêm địa chỉ khác' }}
                                </span>
                                <v-btn v-if="!showAddrForm" color="primary" variant="text" size="small" class="text-none font-weight-medium" @click="openNewAddrForm">
                                    <v-icon start size="18">mdi-plus</v-icon>
                                    Thêm địa chỉ
                                </v-btn>
                                <v-btn v-else variant="text" size="small" color="slate-400" class="text-none font-weight-medium" @click="showAddrForm = false">
                                    <v-icon start size="16">mdi-arrow-left</v-icon>Hủy bỏ
                                </v-btn>
                            </div>

                            <!-- Placeholder khi chưa mở form -->
                            <div v-if="!showAddrForm" class="d-flex flex-column align-center justify-center py-16 text-center" style="opacity: 0.8; height: 100%">
                                <div class="icon-blob bg-slate-50 mb-4" style="width: 100px; height: 100px">
                                    <v-icon size="64" color="slate-300" style="font-size: 64px !important">mdi-map-marker-plus-outline</v-icon>
                                </div>
                                <div class="text-body-2 text-slate-400 font-weight-medium max-w-200 mx-auto px-8">
                                    Nhấn "Thêm địa chỉ" để đăng ký địa chỉ nhận hàng mới.
                                </div>
                            </div>

                            <!-- Form -->
                            <v-form v-else class="px-8 pt-2 pb-6">
                                <v-row dense>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Tên người nhận *</div>
                                        <v-text-field v-model="addrForm.tenNguoiNhan" placeholder="Ví dụ: Nguyễn Văn A" variant="outlined" density="compact" hide-details />
                                    </v-col>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Số điện thoại *</div>
                                        <v-text-field v-model="addrForm.sdtNguoiNhan" placeholder="09xx.xxx.xxx" variant="outlined" density="compact" hide-details />
                                    </v-col>
                                    <v-col cols="12">
                                        <div class="field-label">Tỉnh / Thành phố *</div>
                                        <v-autocomplete v-model="addrForm.tinh" :items="provinces" item-title="name" item-value="code" placeholder="Chọn tỉnh thành" variant="outlined" density="compact" hide-details :loading="loadingLoc.provinces" />
                                    </v-col>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Quận / Huyện *</div>
                                        <v-autocomplete v-model="addrForm.thanhPho" :items="districts" item-title="name" item-value="code" placeholder="Chọn quận huyện" variant="outlined" density="compact" hide-details :disabled="!addrForm.tinh" :loading="loadingLoc.districts" />
                                    </v-col>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Phường / Xã *</div>
                                        <v-autocomplete v-model="addrForm.phuongXa" :items="wards" item-title="name" item-value="code" placeholder="Chọn phường xã" variant="outlined" density="compact" hide-details :disabled="!addrForm.thanhPho" :loading="loadingLoc.wards" />
                                    </v-col>
                                    <v-col cols="12">
                                        <div class="field-label">Địa chỉ cụ thể *</div>
                                        <v-textarea v-model="addrForm.diaChiChiTiet" placeholder="Số nhà, tên đường, tòa nhà..." variant="outlined" rows="2" hide-details />
                                    </v-col>
                                    <v-col cols="12" v-if="listDiaChi.length > 0">
                                        <div class="pa-4 rounded-lg bg-slate-50 border border-dashed border-slate-200">
                                            <v-checkbox v-model="addrForm.laMacDinh" color="primary" hide-details density="compact">
                                                <template v-slot:label>
                                                    <span class="text-body-2 font-weight-bold text-slate-600">Đặt làm địa chỉ mặc định</span>
                                                </template>
                                            </v-checkbox>
                                        </div>
                                    </v-col>
                                    <v-col cols="12" class="mt-6">
                                        <v-btn color="primary" variant="flat" block class="text-none font-weight-bold rounded-xl shadow-sm hover-primary-btn" height="48" :loading="addrSaving" :disabled="!addrForm.tenNguoiNhan || !addrForm.sdtNguoiNhan || !addrForm.tinh || !addrForm.thanhPho || !addrForm.phuongXa || !addrForm.diaChiChiTiet" @click="saveAddress">
                                            <v-icon start size="20" style="color: white !important">mdi-check</v-icon>
                                            <span style="color: white !important">{{ isEditAddr ? 'Cập nhật địa chỉ' : 'Lưu địa chỉ mới' }}</span>
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
    border-color: #1e257c !important;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.border-right-divider {
    border-right: 1px solid rgba(0, 0, 0, 0.08) !important;
    overflow-y: auto;
    height: 600px; /* Fixed height to match card */
}

.addr-col-right {
    overflow-y: auto;
    height: 600px;
}

.bg-white {
    background-color: #ffffff !important;
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