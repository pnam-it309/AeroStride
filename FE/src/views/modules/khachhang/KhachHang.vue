<script setup>
import { computed, ref, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';

import { useUIStore } from '@/stores/ui';
import { AdminFilter, AdminTable, AdminPagination, AdminConfirm, AdminBreadcrumbs } from '@/components/common';
import { downloadFile } from '@/utils/fileUtils';
import { formatDateTime, formatCurrency } from '@/utils/formatters';
import { isActiveStatus, getStatusLabel, getStatusColor } from '@/utils/statusUtils';
import { SYSTEM_STATUS, STATUS_OPTIONS } from '@/constants/statusConstants';
import { PlusIcon, PencilIcon, StarIcon, TrashIcon, MapPinIcon, EyeIcon } from 'vue-tabler-icons';
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
    ADDRESS_CONSTANTS,
    DEFAULT_AVATAR_URL
} from '@/constants/khachHangConstants';
import { GIOI_TINH_FILTER_OPTIONS } from '@/constants/appConstants';
import { useAddressMapping } from '@/composables/useAddressMapping';
import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';
import { dichVuFile } from '@/services/core/dichVuFile';
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
    { text: 'Mã hóa đơn', width: '120px', align: 'center' },
    { text: 'Ngày mua', width: '140px', align: 'center' },
    { text: 'Tổng số lượng', width: '110px', align: 'center' },
    { text: 'Loại hóa đơn', width: '130px', align: 'center' },
    { text: 'Giá trị đơn hàng', width: '140px', align: 'center' },
    { text: 'Trạng thái', width: '120px', align: 'center' },
    { text: 'Hành động', width: '100px', align: 'center' }
];

const getInvoiceTypeLabel = (value) => {
    const normalized = String(value || '').toUpperCase();
    if (normalized === 'ONLINE') return 'Trực tuyến';
    if (normalized === 'TAI_QUAY' || normalized === 'OFFLINE') return 'Cửa hàng';
    return value || '-';
};

const getInvoiceTypeChipClass = (value) => {
    const normalized = String(value || '').toUpperCase();
    if (normalized === 'ONLINE') return 'order-type-online';
    if (normalized === 'TAI_QUAY' || normalized === 'OFFLINE') return 'order-type-offline';
    return '';
};

const openInvoiceDetail = (item) => {
    const id = item.id || item.hoaDonId || null;
    if (id) {
        router.push({ name: 'HoaDonChiTiet', params: { id: id }, query: { from: 'khachhang', khId: selectedKHForInvoices.value?.id } });
    } else if (typeof addNotification === 'function') {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể mở chi tiết hóa đơn', color: 'error' });
    }
};

const getCustomerAvatar = (itemOrImg) => {
    const img = typeof itemOrImg === 'string' ? itemOrImg : (itemOrImg?.hinhAnh || itemOrImg?.anhDaiDien || itemOrImg?.avatar);
    if (!img) return DEFAULT_AVATAR_URL;

    // 1. If it's a seed filename like 'kh11.jpg' or 'kh11'
    if (/^kh(\d+)(\.jpg)?$/i.test(img)) {
        return DEFAULT_AVATAR_URL;
    }

    // 2. Absolute uploads path
    if (img.startsWith('/uploads/')) {
        return img;
    }
    if (img.startsWith('uploads/')) {
        return `/${img}`;
    }

    // 3. Absolute URLs (Cloudinary, standard http/https, data urls, blob urls)
    if (img.startsWith('http://') || img.startsWith('https://') || img.startsWith('data:') || img.startsWith('blob:')) {
        if (img.includes('/uploads/')) {
            const index = img.indexOf('/uploads/');
            return img.substring(index);
        }
        return img;
    }

    // 4. If it already contains '/api/common/storage/files/'
    if (img.includes('/api/common/storage/files/')) {
        const apiBase = import.meta.env.VITE_API_URL || '';
        const cleanBase = apiBase.replace(/\/+$/, '');
        const cleanImg = img.startsWith('/') ? img : `/${img}`;
        return `${cleanBase}${cleanImg}`;
    }

    // 5. If it's a relative path containing /
    if (img.includes('/') && !img.startsWith('/')) {
        return `/uploads/${img}`;
    }

    return dichVuFile.layUrlFile(img);
};

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

const listTab = useAdminTable(
    async (params) => {
        const payload = { ...params };
        if (payload.search) {
            payload.keyword = payload.search;
        }
        return dichVuKhachHang.layKhachHangPhanTrang(payload);
    },
    { search: '', gioiTinh: null, trangThai: null }
);
const statsTab = useAdminTable(
    async (params) => {
        const payload = { ...params };
        if (payload.search) {
            payload.keyword = payload.search;
        }
        if (payload.khoangChiTieu && payload.khoangChiTieu.length === 2) {
            payload.minTongChiTieu = payload.khoangChiTieu[0];
            payload.maxTongChiTieu = payload.khoangChiTieu[1];
        }
        delete payload.khoangChiTieu;
        return dichVuKhachHang.layKhachHangPhanTrang(payload);
    },
    {
        search: '',
        gioiTinh: null,
        trangThai: null,
        khoangChiTieu: [0, 50000000],
        minNgayDonHang: null,
        maxNgayDonHang: null
    }
);


// Automatic fetch on tab switch if data is empty
watch(activeTab, (newTab) => {
    if (newTab === 'danh-sach' && listTab.items.value.length === 0) {
        listTab.loadData();
    } else if (newTab === 'thong-ke' && statsTab.items.value.length === 0) {
        statsTab.loadData();
    }
});

const maxChiTieu = ref(50000000);

const fetchMaxChiTieu = async () => {
    try {
        const rawCust = await dichVuKhachHang.layTatCaKhachHang();
        let allCust = [];
        if (Array.isArray(rawCust)) allCust = rawCust;
        else if (Array.isArray(rawCust?.content)) allCust = rawCust.content;
        else if (Array.isArray(rawCust?.data)) allCust = rawCust.data;
        else if (Array.isArray(rawCust?.data?.content)) allCust = rawCust.data.content;

        let max = 0;
        allCust.forEach(c => {
            const chiTieu = Number(c?.tongChiTieu || c?.tongTienDaChi || 0);
            if (chiTieu > max) {
                max = chiTieu;
            }
        });

        if (max > 0) {
            // Làm tròn lên hàng trăm nghìn
            const roundedMax = Math.ceil(max / 100000) * 100000;
            maxChiTieu.value = roundedMax;
            statsTab.filters.value.khoangChiTieu = [0, roundedMax];
            if (activeTab.value === 'thong-ke') {
                statsTab.loadData();
            }
        }
    } catch (error) {
        console.error('Không thể lấy danh sách khách hàng để tính chi tiêu lớn nhất:', error);
    }
};

onMounted(() => {
    fetchMaxChiTieu();
});

const allCustomers = computed(() => (activeTab.value === 'danh-sach' ? listTab.items.value : statsTab.items.value));
const loading = computed(() => (activeTab.value === 'danh-sach' ? listTab.loading.value : statsTab.loading.value));
const pagination = computed(() => (activeTab.value === 'danh-sach' ? listTab.pagination.value : statsTab.pagination.value));
const listFilters = listTab.filters;
const statsFilters = statsTab.filters;

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

const onDateRangeChange = (val) => {
    handleLocalFilterChange();
};

const handleReset = () => {
    listTab.handleReset();
    statsTab.handleReset();
    if (statsTab.filters.value.khoangChiTieu) {
        statsTab.filters.value.khoangChiTieu = [0, maxChiTieu.value];
    }
    statsTab.handleFilter();
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
const route = useRoute();
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
            addNotification({ title: 'Thành công', subtitle: isEditAddr.value ? 'Đã cập nhật địa chỉ' : 'Đã thêm địa chỉ mới', color: 'success' });
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

    setConfirm({
        title: isEditAddr.value ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ',
        message: isEditAddr.value ? 'Bạn có chắc muốn cập nhật địa chỉ này không?' : 'Bạn có chắc muốn thêm địa chỉ mới cho khách hàng này không?',
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

onMounted(async () => {
    loadCustomers();
    if (route.query.view === 'invoices' && route.query.khId) {
        activeTab.value = 'thong-ke';
        try {
            const cus = await dichVuKhachHang.layChiTietKhachHang(route.query.khId);
            if (cus) {
                await openInvoicesDialog(cus);
                router.replace('/admin/khach-hang');
            }
        } catch (e) {
            console.error('Lỗi khi tải chi tiết khách hàng để xem hóa đơn:', e);
        }
    }
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
                const newS = item.trangThai === SYSTEM_STATUS.ACTIVE ? SYSTEM_STATUS.INACTIVE : SYSTEM_STATUS.ACTIVE;
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

const getIndex = (index) => {
    return (pagination.value.page - 1) * pagination.value.size + index + 1;
};

const getInvoiceIndex = (index) => {
    return (invoicesTab.pagination.value.page - 1) * invoicesTab.pagination.value.size + index + 1;
};

const getGenderChipClass = (gioiTinh) => {
    return ['gender-chip', gioiTinh ? 'gender-chip-male' : 'gender-chip-female'];
};

const getGenderLabel = (gioiTinh) => {
    return gioiTinh === true ? 'Nam' : gioiTinh === false ? 'Nữ' : '-';
};

const getStatusChipClass = (trangThai) => {
    return ['status-chip', isActiveStatus(trangThai) ? 'status-chip-active' : 'status-chip-inactive'];
};

const goToAdd = () => {
    router.push({ name: 'KhachHangForm' });
};

const goToEdit = (id) => {
    router.push({ name: 'KhachHangForm', params: { id } });
};

const getTotalQuantity = (details) => {
    return (details || []).reduce((s, d) => s + (d.soLuong || 0), 0);
};

const formatInvoiceDate = (item) => {
    const date = item.ngayTao || item.ngayDatHang || item.createdAt;
    return date ? formatDateTime(date) : '-';
};

const updatePaginationSize = (size) => {
    pagination.value.size = size;
};

const updateInvoicePaginationSize = (size) => {
    invoicesTab.pagination.value.size = size;
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
                    <!-- ===== BỘ LỌC TAB 1: DANH SÁCH ===== -->
                    <template v-if="activeTab === 'danh-sach'">
                        <!-- Tìm kiếm -->
                        <v-col cols="12" md="4" class="filter-cell">
                            <div class="filter-field-label">Tìm kiếm</div>
                            <v-text-field v-model="listFilters.search" placeholder="Mã, tên, số điện thoại, email..."
                                variant="outlined" bg-color="white" density="compact" hide-details class="compact-input"
                                @input="handleLocalFilterChange" />
                        </v-col>

                        <!-- Giới tính -->
                        <v-col cols="12" md="3" class="filter-cell">
                            <div class="filter-field-label">Giới tính</div>
                            <v-select v-model="listFilters.gioiTinh" :items="GIOI_TINH_FILTER_OPTIONS"
                                variant="outlined" bg-color="white" density="compact" hide-details class="compact-input"
                                @update:model-value="handleLocalFilterChange" />
                        </v-col>

                        <!-- Trạng thái -->
                        <v-col cols="12" md="3" class="filter-cell">
                            <div class="filter-field-label">Trạng thái</div>
                            <v-select v-model="listFilters.trangThai" :items="TRANG_THAI_FILTER_OPTIONS"
                                variant="outlined" bg-color="white" density="compact" hide-details class="compact-input"
                                @update:model-value="handleLocalFilterChange" />
                        </v-col>
                    </template>

                    <!-- ===== BỘ LỌC TAB 2: TỔNG ĐƠN MUA HÀNG ===== -->
                    <template v-else>
                        <!-- Tìm kiếm chung -->
                        <v-col cols="12" md="5" class="filter-cell">
                            <div class="filter-field-label">Tìm kiếm </div>
                            <v-text-field v-model="statsFilters.search" placeholder="Mã, tên, số điện thoại, email..."
                                variant="outlined" bg-color="white" density="compact" hide-details class="compact-input"
                                @input="handleLocalFilterChange" />
                        </v-col>

                        <!-- Khoảng thời gian mua -->
                        <v-col cols="12" md="3" class="filter-cell">
                            <div class="filter-field-label">Từ ngày</div>
                            <AppDatePicker v-model="statsFilters.minNgayDonHang"
                                @update:model-value="handleLocalFilterChange" placeholder="Từ ngày"
                                :text-field-props="{ class: 'compact-input date-field' }" />
                        </v-col>
                        <v-col cols="12" md="3" class="filter-cell">
                            <div class="filter-field-label">Đến ngày</div>
                            <AppDatePicker v-model="statsFilters.maxNgayDonHang"
                                @update:model-value="handleLocalFilterChange" placeholder="Đến ngày"
                                :text-field-props="{ class: 'compact-input date-field' }" />
                        </v-col>
                    </template>

                    <template #after v-if="activeTab !== 'danh-sach'">
                        <v-row dense class="mt-2">
                            <v-col cols="12" class="filter-cell filter-range-row">
                                <div class="d-flex align-center justify-space-between mb-1">
                                    <div class="filter-field-label mb-0">Khoảng tiền đã chi</div>
                                    <span class="filter-range-value text-caption text-primary font-weight-bold">
                                        {{ formatCurrency(statsFilters.khoangChiTieu[0]) }} –
                                        {{ formatCurrency(statsFilters.khoangChiTieu[1]) }}
                                    </span>
                                </div>
                                <v-range-slider v-model="statsFilters.khoangChiTieu" :max="maxChiTieu" :min="0"
                                    :step="100000" hide-details color="primary" track-color="#e2e8f0" track-size="2"
                                    thumb-size="12" class="blue-range-slider" @end="handleLocalFilterChange" />
                            </v-col>
                        </v-row>
                    </template>
                </AdminFilter>
            </div>

            <AdminTable title="Khách hàng" addButtonText="Tạo mới" show-export-button
                :headers="activeTab === 'danh-sach' ? tableHeaders : statsTableHeaders" :items="allCustomers"
                :total-count="pagination.totalElements" :loading="loading" @add="goToAdd" @export="handleExport">
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
                            {{ getIndex(index) }}
                        </td>
                        <td class="data-cell text-center">
                            <div class="customer-square-box">
                                <v-img :src="getCustomerAvatar(item)" cover class="customer-square-image" />
                            </div>
                        </td>
                        <td class="data-cell text-center">
                            <div class="text-truncate" :title="item.ma">{{ item.ma || '-' }}</div>
                        </td>
                        <td class="data-cell text-left px-4">
                            <div class="text-truncate" :title="item.ten">{{ item.ten || '-' }}</div>
                        </td>
                        <td class="data-cell text-center">
                            <v-chip variant="flat" :class="getGenderChipClass(item.gioiTinh)">
                                {{ getGenderLabel(item.gioiTinh) }}
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
                        <td class="data-cell text-center">
                            <v-chip variant="flat" :class="getStatusChipClass(item.trangThai)">
                                {{ getStatusLabel(item.trangThai) }}
                            </v-chip>
                        </td>
                        <td class="data-cell action-cell text-center">
                            <div class="d-flex align-center justify-center action-controls">
                                <!-- Địa chỉ -->
                                <v-btn variant="text" class="action-icon-btn" @click.stop="openAddrDialog(item)">
                                    <component :is="ADMIN_ICONS.ACTION.MAP" size="15" />
                                    <v-tooltip activator="parent" location="top">Quản lý địa chỉ</v-tooltip>
                                </v-btn>
                                <v-btn variant="text" class="action-icon-btn" @click.stop="goToEdit(item.id)">
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
                            {{ getIndex(index) }}
                        </td>
                        <td class="data-cell text-center">
                            <div class="customer-square-box">
                                <v-img :src="getCustomerAvatar(item)" cover class="customer-square-image" />
                            </div>
                        </td>
                        <td class="data-cell text-center">
                            <div class="text-truncate" :title="item.ma">{{ item.ma || '-' }}</div>
                        </td>
                        <td class="data-cell text-left px-4">
                            <div class="text-truncate" :title="item.ten">{{ item.ten || '-' }}</div>
                        </td>
                        <td class="data-cell text-center">
                            <div class="text-truncate" :title="item.sdt">{{ item.sdt || '-' }}</div>
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
                        <td class="data-cell action-cell text-center">
                            <div class="d-flex align-center justify-center action-controls">
                                <!-- Chi tiết -->
                                <v-btn variant="text" class="action-icon-btn" @click.stop="openInvoicesDialog(item)">
                                    <component :is="ADMIN_ICONS.ACTION.VIEW" size="15" />
                                    <v-tooltip activator="parent" location="top">Lịch sử mua hàng</v-tooltip>
                                </v-btn>
                            </div>
                        </td>
                    </tr>
                </template>

                <template #pagination>
                    <AdminPagination v-model="pagination.page" :page-size="pagination.size"
                        @update:page-size="updatePaginationSize" :total-pages="pagination.totalPages"
                        :total-elements="pagination.totalElements" :current-size="allCustomers.length"
                        @change="loadCustomers" />
                </template>
            </AdminTable>
        </div>

        <div v-else-if="currentView === 'invoice-history'"
            class="invoice-history-container admin-table-main-root animate-fade-in font-body">
            <!-- Header: Back button, title -->
            <div class="d-flex align-center ga-2 mb-3 mt-1">
                <v-btn icon variant="flat" color="white" class="border elevation-1 rounded-lg" size="36"
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
            <AdminTable title="Lịch sử hóa đơn" :show-add-button="false" :headers="invoiceHistoryTableHeaders"
                :items="invoicesTab.items.value" :loading="invoicesTab.loading.value"
                empty-text="Chưa có hóa đơn nào được tìm thấy cho khách hàng này!" empty-icon="mdi-receipt-text-off">
                <template #row="{ item, index }">
                    <tr class="data-row">
                        <!-- STT -->
                        <td class="data-cell text-center text-black font-weight-medium" style="font-size: 13px">
                            {{ getInvoiceIndex(index) }}
                        </td>

                        <!-- Mã hóa đơn (click to open detail) -->
                        <td class="data-cell text-center" style="font-size: 13px">
                            <a href="#" class="invoice-link font-weight-bold text-primary"
                                @click.prevent="openInvoiceDetail(item)">
                                {{ item.maHoaDon || item.ma || '-' }}
                            </a>
                        </td>

                        <!-- Ngày mua -->
                        <td class="data-cell text-center" style="font-size: 13px">
                            {{ formatInvoiceDate(item) }}
                        </td>

                        <!-- Tổng số lượng -->
                        <td class="data-cell text-center" style="font-size: 13px">
                            {{ getTotalQuantity(item.details) }}
                        </td>

                        <!-- Loại hóa đơn -->
                        <td class="data-cell text-center" style="font-size: 13px">
                            <v-chip size="small" variant="flat"
                                :class="['status-chip', getInvoiceTypeChipClass(item.loaiDon || item.hinhThuc)]">
                                {{ getInvoiceTypeLabel(item.loaiDon || item.hinhThuc) }}
                            </v-chip>
                        </td>

                        <!-- Giá trị đơn hàng -->
                        <td class="data-cell text-center font-weight-bold order-value" style="font-size: 13px">
                            {{ formatCurrency(item.tongTienSauGiam || item.tongTien || 0) }}
                        </td>

                        <!-- Trạng thái -->
                        <td class="data-cell text-center" style="font-size: 13px">
                            <template v-if="getOrderStatusMeta(item.trangThai)">
                                <v-chip :class="['status-chip', getOrderStatusMeta(item.trangThai).chipClass]"
                                    variant="flat" size="small">
                                    <v-icon start size="14">{{ getOrderStatusMeta(item.trangThai).icon }}</v-icon>
                                    {{ getOrderStatusMeta(item.trangThai).text }}
                                </v-chip>
                            </template>
                        </td>

                        <!-- Hành động -->
                        <td class="data-cell text-center" style="font-size: 13px">
                            <v-btn icon variant="text" size="small" color="primary" class="action-icon-btn"
                                @click.stop="openInvoiceDetail(item)">
                                <EyeIcon size="18" />
                                <v-tooltip activator="parent" location="top">Chi tiết hóa đơn</v-tooltip>
                            </v-btn>
                        </td>
                    </tr>
                </template>

                <template #pagination>
                    <AdminPagination v-model="invoicesTab.pagination.value.page"
                        :page-size="invoicesTab.pagination.value.size" @update:page-size="updateInvoicePaginationSize"
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
                        <div class="text-h6 font-weight-black text-slate-800 line-height-1"
                            style="font-size: 15px !important">
                            Quản lý địa chỉ — {{ selectedKH?.ten }}</div>
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
                                class="px-8 pt-6 pb-4 d-flex align-center justify-space-between sticky-top bg-white z-index-10 position-relative">
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
                                class="text-center py-16 px-8 mx-8 bg-slate-50-50 rounded-xl mt-4">
                                <v-icon size="48" color="slate-200" class="mb-4">mdi-map-marker-off</v-icon>
                                <div class="text-slate-400 font-weight-medium">Chưa có địa chỉ nào được đăng ký</div>
                            </div>

                            <div v-else class="px-8">
                                <div v-for="addr in listDiaChi" :key="addr.id"
                                    class="pa-6 rounded-xl bg-white border border-slate-200 mb-6 hover-elevation-1 transition-all position-relative"
                                    :class="{ 'border-s-4 border-s-success border-success-light': addr.laMacDinh }"
                                    style="
                                        margin-top: 15px;
                                        box-shadow:
                                            0 4px 6px -1px rgba(0, 0, 0, 0.05),
                                            0 2px 4px -1px rgba(0, 0, 0, 0.03) !important;
                                    ">
                                    <!-- Dòng tên + badge mặc định -->
                                    <div class="d-flex align-center ga-3 mb-3">
                                        <span class="text-subtitle-1 font-weight-medium text-slate-700"
                                            style="font-size: 13px !important">
                                            {{ addr.tenNguoiNhan }}
                                        </span>
                                        <v-chip v-if="addr.laMacDinh" color="success" size="x-small" variant="flat"
                                            class="font-weight-medium px-2"
                                            style="font-size: 11px !important; color: white !important">Mặc
                                            định</v-chip>
                                    </div>

                                    <!-- SĐT -->
                                    <div class="d-flex align-center ga-2 mb-2 text-slate-600">
                                        <v-icon size="16" color="slate-400"
                                            style="font-size: 16px !important">mdi-phone-outline</v-icon>
                                        <span class="text-body-2 font-weight-medium"
                                            style="font-size: 13px !important">{{
                                                addr.sdtNguoiNhan
                                            }}</span>
                                    </div>

                                    <!-- Địa chỉ đầy đủ -->
                                    <div class="text-body-2 text-slate-500 mb-4 font-weight-medium line-height-1-6"
                                        style="font-size: 13px !important">

                                        {{
                                            [addr.diaChiChiTiet, addr.phuongXa, addr.thanhPho,
                                            addr.tinh].filter(Boolean).join(',') }}
                                    </div>

                                    <!-- Actions -->
                                    <div class="d-flex align-center ga-1 mt-2">
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
                                class="px-8 pt-6 pb-4 d-flex align-center justify-space-between sticky-top bg-white z-index-10">
                                <span class="text-subtitle-2 font-weight-bold text-slate-800"
                                    style="font-size: 15px !important">
                                    {{ showAddrForm ?
                                        (isEditAddr ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ mới') : 'Thêm địa chỉ khác' }}
                                </span>
                                <v-btn v-if="!showAddrForm" variant="text" color="primary"
                                    class="text-none font-weight-bold px-4 rounded-lg text-primary bg-blue-lighten-5"
                                    style="
                                        font-size: 13px !important;
                                        height: 36px !important;
                                        min-height: 36px !important;
                                        box-shadow: none !important;
                                    " @click="openNewAddrForm">
                                    <PlusIcon size="14" class="mr-1" />
                                    <span style="
                                            font-size: 13px !important;
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
                                            :readonly="true" class="bg-slate-50" variant="outlined" density="compact"
                                            hide-details />
                                    </v-col>
                                    <v-col cols="12" md="6">
                                        <div class="field-label">Số điện thoại</div>
                                        <v-text-field v-model="addrForm.sdtNguoiNhan" placeholder="09xx.xxx.xxx"
                                            :readonly="true" class="bg-slate-50" variant="outlined" density="compact"
                                            hide-details />
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
                                                    <span class="text-body-2 font-weight-bold text-slate-600"
                                                        style="font-size: 13px !important">Đặt làm địa chỉ mặc
                                                        định</span>
                                                </template>
                                            </v-checkbox>
                                        </div>
                                    </v-col>
                                    <v-col cols="12" class="mt-6">
                                        <v-btn color="primary" variant="flat" block
                                            class="text-none font-weight-bold rounded-xl elevation-1 hover-primary-btn"
                                            height="48" :loading="addrSaving" :disabled="!addrForm.tenNguoiNhan ||
                                                !addrForm.sdtNguoiNhan ||
                                                !addrForm.tinh ||
                                                !addrForm.thanhPho ||
                                                !addrForm.phuongXa ||
                                                !addrForm.diaChiChiTiet
                                                " @click="saveAddress">
                                            <v-icon start size="20" style="color: white !important">mdi-check</v-icon>
                                            <span style="color: white !important; font-size: 13px !important">{{
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
.invoice-link {
    text-decoration: none !important;
    cursor: pointer;
}

.invoice-link:hover {
    text-decoration: none !important;
    opacity: 0.9;
}

.customer-square-box {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto;
}

.customer-square-image {
    width: 40px;
    height: 40px;
    border-radius: 10px !important;
    overflow: hidden;
}

.customer-square-image :deep(img) {
    object-fit: cover;
}

:deep(.order-type-online) {
    background: #eff6ff !important;
    border: 1px solid #dbeafe !important;
}

:deep(.order-type-online) .v-chip__content {
    color: #166534 !important;
}

:deep(.order-type-offline) {
    background: #fdf2f8 !important;
    border: 1px solid #fce7f3 !important;
}

:deep(.order-type-offline) .v-chip__content {
    color: #9a3412 !important;
}

.order-value {
    color: #1e257c !important;
    /* main blue color for order value */
}

.invoice-dialog-title,
.invoice-dialog-code {
    font-size: 13px;
    font-weight: 500;
    font-family: 'Inter', sans-serif;
}

.invoice-dialog-code {
    color: #0f172a;
}

.invoice-product-square {
    width: 56px;
    height: 56px;
    border-radius: 8px;
    overflow: hidden;
}

.invoice-product-square-placeholder {
    width: 56px;
    height: 56px;
    border-radius: 8px;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
}

.invoice-amount,
.invoice-total-amount {
    color: #1e257c !important;
    font-weight: 600;
}

.khach-hang-dialog-card .header-cell {
    font-weight: 700;
    color: #374151;
}

.khach-hang-dialog-card .data-row td[data-label] {
    vertical-align: middle;
}

/* Đồng bộ kích thước icon lịch cho ô lọc ngày */
:deep(.date-field .v-icon) {
    font-size: 20px !important;
    opacity: 0.8 !important;
}

:deep(.v-field),
:deep(.v-field__outline) {
    border-radius: 12px !important;
}

:deep(.v-field__input),
:deep(input),
:deep(input::placeholder),
:deep(textarea),
:deep(.v-select__selection-text),
:deep(.v-label) {
    font-size: 13px !important;
}

:deep(.v-field__input::placeholder) {
    font-size: 13px !important;
}

.field-label,
.field-label-small {
    font-size: 13px !important;
}

:global(.v-overlay-container .v-list-item-title),
:global(.v-overlay-container .v-list-item) {
    font-size: 13px !important;
}
</style>
