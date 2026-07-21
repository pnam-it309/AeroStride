<script setup>
import { PATH } from '@/router/routePaths';
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';
import { useNotifications } from '@/services/notificationService';
import { useHoaDonPrinter } from '@/composables/useHoaDonPrinter';
import {
    ChevronLeftIcon, PrinterIcon, EditIcon, CalendarIcon,
    PackageIcon, UserIcon, MapPinIcon, CreditCardIcon, TruckIcon,
    CircleCheckIcon, CircleXIcon, CheckIcon, TrashIcon,
    PlusIcon, LayoutListIcon, LayoutGridIcon
} from "vue-tabler-icons";
import { AdminConfirm, AdminBreadcrumbs, AdminTable, AdminPagination, AdminFilter } from "@/components/common";
import { getOrderStatusMeta, getOrderStatusOrdinal } from "@/utils/orderStatus";
import { ORDER_STATUS_ORDINALS } from "@/constants/hoaDonConstants";

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();
const { printHoaDonById } = useHoaDonPrinter();

const loaded = ref(false);
const loading = ref(false);
const updatingStatus = ref(false);
const statusDialogOpen = ref(false);
const showHistoryModal = ref(false);
const selectedStatus = ref("");
const pendingStatus = ref("");

const editOrderDialogOpen = ref(false);
const activeTab = ref(0);
const editForm = ref({
    trangThai: "",
    tenNguoiNhan: "",
    soDienThoaiNguoiNhan: "",
    diaChiNguoiNhan: "",
    ghiChu: "",
    ghiChuTrangThai: ""
});

const order = ref({
    id: "",
    maHoaDon: "",
    tenKhachHang: "",
    emailKhachHang: "",
    soDienThoaiKhachHang: "",
    soDienThoaiNguoiNhan: "",
    trangThai: 'CHO_XAC_NHAN',
    ngayTao: "",
    tongTien: 0,
    tongTienSauGiam: 0,
    phiVanChuyen: 0,
    loaiDon: "OFFLINE",
    diaChiNguoiNhan: "",
    ghiChu: "",
    listsHoaDonChiTiet: [],
    listsLichSuHoaDon: [],
    listsGiaoDichThanhToan: [],
    maPhieuGiamGia: null,
    tenPhieuGiamGia: null
});


// Product table logic
const productSearch = ref("");
const productColorFilter = ref(null);
const productSizeFilter = ref(null);

const maxOrderPrice = computed(() => {
    const items = order.value?.listsHoaDonChiTiet || [];
    if (!items.length) return 20000000; // fallback default
    const max = Math.max(...items.map(p => Number(p.donGia) || 0));
    return max > 0 ? max : 20000000;
});

const priceRange = ref([0, 20000000]);

watch(maxOrderPrice, (newMax) => {
    priceRange.value = [0, newMax];
}, { immediate: true });

const productPagination = ref({
    page: 1,
    size: 5,
});

const availableColors = computed(() => {
    const items = order.value?.listsHoaDonChiTiet || [];
    const colors = items.map(p => p.mauSac).filter(c => c && c !== '—');
    return ['Tất cả', ...new Set(colors)];
});

const availableSizes = computed(() => {
    const items = order.value?.listsHoaDonChiTiet || [];
    const sizes = items.map(p => p.kichThuoc).filter(s => s && s !== '—');
    return ['Tất cả', ...new Set(sizes)];
});

const filteredProducts = computed(() => {
    let items = order.value.listsHoaDonChiTiet || [];
    if (productSearch.value) {
        const s = productSearch.value.toLowerCase();
        items = items.filter(p =>
            (p.chiTietSanPham?.sanPham?.ten || '').toLowerCase().includes(s) ||
            (p.chiTietSanPham?.maChiTietSanPham || '').toLowerCase().includes(s) ||
            (p.tenSanPham || '').toLowerCase().includes(s) ||
            (p.maChiTietSanPham || p.maSanPham || '').toLowerCase().includes(s)
        );
    }
    if (priceRange.value) {
        items = items.filter(p =>
            p.donGia >= priceRange.value[0] && p.donGia <= priceRange.value[1]
        );
    }
    if (productColorFilter.value && productColorFilter.value !== 'Tất cả') {
        items = items.filter(p => p.mauSac === productColorFilter.value);
    }
    if (productSizeFilter.value && productSizeFilter.value !== 'Tất cả') {
        items = items.filter(p => p.kichThuoc === productSizeFilter.value);
    }
    return items;
});

const paginatedProducts = computed(() => {
    const start = (productPagination.value.page - 1) * productPagination.value.size;
    const end = start + productPagination.value.size;
    return filteredProducts.value.slice(start, end);
});

// Confirmation Logic
const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    color: 'primary',
    showInput: false,
    inputLabel: 'Ghi chú',
    inputRequired: false,
    action: null,
    loading: false
});

// --- Xác nhận hoàn phí (đơn trả trước bị hủy) ---
const confirmHoanPhi = () => {
    confirmDialog.value = {
        show: true,
        title: 'Xác nhận hoàn phí',
        message: `Xác nhận đã hoàn trả tiền cho khách hàng của đơn #${order.value.maHoaDon}?`,
        color: 'primary',
        showInput: false,
        inputLabel: '',
        inputRequired: false,
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                await dichVuHoaDon.xacNhanHoanPhi(order.value.id);
                addNotification({ title: 'Thành công', subtitle: 'Đã xác nhận hoàn phí cho khách hàng', color: 'success' });
                await loadOrderDetail();
                confirmDialog.value.show = false;
            } catch (error) {
                addNotification({ title: 'Lỗi', subtitle: error?.response?.data?.message || 'Xác nhận hoàn phí thất bại', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

// --- Chỉnh sửa số lượng sản phẩm trong hóa đơn ---
const editingProducts = ref(false);
const savingProducts = ref(false);
const editQtyMap = ref({}); // { [idHdct]: soLuong }

const startEditProducts = () => {
    const map = {};
    (order.value.listsHoaDonChiTiet || []).forEach((it) => {
        map[it.id] = it.soLuong;
    });
    editQtyMap.value = map;
    editingProducts.value = true;
};

const cancelEditProducts = () => {
    editingProducts.value = false;
    editQtyMap.value = {};
};

const changeProductQty = (item, delta) => {
    const current = editQtyMap.value[item.id] || 1;
    const next = current + delta;
    if (next < 1) return; // tối thiểu 1
    editQtyMap.value[item.id] = next;
};

const saveProducts = async () => {
    const items = order.value.listsHoaDonChiTiet || [];
    const changed = items.filter((it) => (editQtyMap.value[it.id] ?? it.soLuong) !== it.soLuong);
    if (!changed.length) {
        editingProducts.value = false;
        return;
    }
    savingProducts.value = true;
    try {
        for (const it of changed) {
            await dichVuHoaDon.capNhatSanPhamHoaDon(order.value.id, {
                idChiTietSanPham: it.idCtsp,
                soLuong: editQtyMap.value[it.id]
            });
        }
        addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật số lượng sản phẩm', color: 'success' });
        editingProducts.value = false;
        await loadOrderDetail();
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: error?.response?.data?.message || 'Cập nhật sản phẩm thất bại', color: 'error' });
    } finally {
        savingProducts.value = false;
    }
};

const removeProduct = (item) => {
    if ((order.value.listsHoaDonChiTiet || []).length <= 1) {
        addNotification({ title: 'Không thể xóa', subtitle: 'Đơn hàng phải còn ít nhất 1 sản phẩm', color: 'warning' });
        return;
    }
    confirmDialog.value = {
        show: true,
        title: 'Xóa sản phẩm',
        message: `Xóa "${item.tenSanPham}" khỏi đơn hàng?`,
        color: 'error',
        showInput: false,
        inputLabel: '',
        inputRequired: false,
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                await dichVuHoaDon.xoaSanPhamHoaDon(order.value.id, item.id);
                addNotification({ title: 'Thành công', subtitle: 'Đã xóa sản phẩm khỏi đơn hàng', color: 'success' });
                await loadOrderDetail();
                confirmDialog.value.show = false;
            } catch (error) {
                addNotification({ title: 'Lỗi', subtitle: error?.response?.data?.message || 'Xóa sản phẩm thất bại', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

// --- Đổi giá sản phẩm: phát hiện & áp giá mới ---
const isPriceChanged = (item) => item.giaHienTai != null && Number(item.giaHienTai) !== Number(item.donGia);

const applyNewPrice = (item) => {
    confirmDialog.value = {
        show: true,
        title: 'Áp dụng giá mới',
        message: `Cập nhật đơn giá "${item.tenSanPham}" từ ${formatCurrency(item.donGia)} thành ${formatCurrency(item.giaHienTai)}?`,
        color: 'warning',
        showInput: false,
        inputLabel: '',
        inputRequired: false,
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                await dichVuHoaDon.capNhatSanPhamHoaDon(order.value.id, {
                    idChiTietSanPham: item.idCtsp,
                    soLuong: item.soLuong
                });
                addNotification({ title: 'Thành công', subtitle: 'Đã áp dụng giá mới cho sản phẩm', color: 'success' });
                await loadOrderDetail();
                confirmDialog.value.show = false;
            } catch (error) {
                addNotification({ title: 'Lỗi', subtitle: error?.response?.data?.message || 'Cập nhật giá thất bại', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};



// Configuration logic moved up

const productColumns = [
    { key: 'hinhAnh', text: 'Hình ảnh' },
    { key: 'tenSanPham', text: 'Tên sản phẩm' },
    { key: 'maBienThe', text: 'Mã biến thể' },
    { key: 'mauSac', text: 'Màu sắc' },
    { key: 'kichThuoc', text: 'Kích thước' },
    { key: 'soLuong', text: 'Số lượng' },
    { key: 'donGia', text: 'Đơn giá' },
    { key: 'thanhTien', text: 'Thành tiền' }
];

const getStatusInfo = (s) => {
    const meta = getOrderStatusMeta(s);
    if (meta) return meta;
    return { text: '—', color: 'grey', icon: 'mdi-minus-circle-outline', chipClass: 'status-chip-unknown' };
};

const loadOrderDetail = async () => {
    loading.value = true;
    try {
        const data = await dichVuHoaDon.layChiTietHoaDon(route.params.id);
        console.log('Order Detail Data:', data);
        order.value = data;
        loaded.value = true;
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải thông tin hóa đơn', color: 'error' });
    } finally {
        loading.value = false;
    }
};

const getStatusLabel = (s) => getStatusInfo(s).text;
const getStatusTone = (s) => getStatusInfo(s).color;

const getOrderStatus = () => getOrderStatusOrdinal(order.value?.trangThai);

const customerAvatar = computed(() => (order.value.tenKhachHang || 'K').charAt(0));
const customerAvatarUrl = computed(() => {
    return order.value.khachHang?.anhDaiDien || 'https://upload.wikimedia.org/wikipedia/commons/7/7c/Profile_avatar_placeholder_large.png';
});

const previousStatus = computed(() => {
    const logs = Array.isArray(order.value?.listsLichSuHoaDon) ? order.value.listsLichSuHoaDon : [];
    if (logs.length === 0) return null;

    const latest = logs.reduce((acc, cur) => {
        const a = Number(acc?.ngayTao ?? -Infinity);
        const b = Number(cur?.ngayTao ?? -Infinity);
        return b > a ? cur : acc;
    }, logs[0]);

    const prev = getOrderStatusOrdinal(latest?.trangThaiCu);
    const curr = getOrderStatus();
    if (prev === null || curr === null) return null;
    if (prev === curr) return null;
    return prev;
});

const previousStatusMeta = computed(() => (previousStatus.value === null ? null : getOrderStatusMeta(previousStatus.value)));

const getStatusTimestampMap = computed(() => {
    const map = {};
    const logs = Array.isArray(order.value?.listsLichSuHoaDon) ? order.value.listsLichSuHoaDon : [];
    for (const log of logs) {
        const ord = getOrderStatusOrdinal(log?.trangThaiMoi);
        if (ord === null) continue;
        if (!map[ord]) map[ord] = log?.ngayTao ?? null;
    }
    if (!map[0]) map[0] = order.value?.ngayTao ?? null;
    return map;
});

// --- UI Logic Computed ---
const orderStatusLabel = computed(() => getStatusLabel(order.value.trangThai));
const orderStatusTone = computed(() => getStatusTone(order.value.trangThai));
const showStatusChip = computed(() => loaded.value && getOrderStatusMeta(order.value.trangThai));
const canUpdateStatus = computed(() => order.value && getOrderStatus() !== null && getOrderStatus() < ORDER_STATUS_ORDINALS.HOAN_THANH);
const isOrderEditable = computed(() => order.value.trangThai === 'CHO_XAC_NHAN' || getOrderStatus() < ORDER_STATUS_ORDINALS.CHO_GIAO);

const customerName = computed(() => order.value.tenKhachHang || 'Khách vãng lai');
const orderTypeLabel = computed(() => (order.value.loaiDon === 'TAI_QUAY' ? 'Nhận tại quầy' : 'Giao hàng tận nơi'));

/**
 * Dịch tên người thực hiện sang tiếng Việt thân thiện:
 * - "GUEST"  → "Khách vãng lai"
 * - "SYSTEM" → nếu đặt ONLINE → "Người hệ thống", ngược lại → "Hệ thống"
 * - email (chứa @) → lấy phần trước @ để hiển thị gọn hơn
 */
const formatNguoi = (value, isOnline = false) => {
    if (!value) return isOnline ? 'Người hệ thống' : 'Hệ thống';
    const v = value.trim().toUpperCase();
    if (v === 'GUEST') return 'Khách vãng lai';
    if (v === 'SYSTEM') return isOnline ? 'Người hệ thống' : 'Hệ thống';
    if (v === 'ADMIN') return 'Quản trị viên';
    // Nếu là email, giữ nguyên để hiển thị (backend mới đã lưu tên rồi, trường hợp cũ vẫn xử lý)
    if (value.includes('@')) return value.split('@')[0];
    return value;
};

const displayAddress = computed(() => {
    let addr = order.value.diaChiNguoiNhan;
    // Bỏ qua nếu là địa chỉ rác kiểu "Mua tại quầy"
    if (addr && addr !== 'Mua tại quầy' && addr !== 'Khách nhận tại quầy') {
        return addr;
    }
    // Nếu là TAI_QUAY hoặc chưa có địa chỉ, lấy địa chỉ từ customer
    const item = order.value.khachHang;
    if (!item) return '';

    if (typeof item.diaChi === 'string' && item.diaChi) return item.diaChi;
    if (typeof item.diaChiDayDu === 'string' && item.diaChiDayDu) return item.diaChiDayDu;

    const lists = item.listsDiaChi || item.listDiaChi || item.diaChis;
    const defaultAddress = Array.isArray(lists) ? (lists.find(d => d.laMacDinh || d.laDiaChiMacDinh) || lists[0]) : null;

    if (defaultAddress && defaultAddress.diaChiChiTiet) {
        const fromDefault = [defaultAddress.diaChiChiTiet, defaultAddress.phuongXa, defaultAddress.thanhPho, defaultAddress.tinh].filter(Boolean).join(', ');
        if (fromDefault) return fromDefault;
    }

    if (item.diaChiChiTiet) {
        const fromRoot = [item.diaChiChiTiet, item.phuongXa, item.thanhPho, item.tinh].filter(Boolean).join(', ');
        if (fromRoot) return fromRoot;
    }

    const composed = [
        defaultAddress?.phuongXa || item.phuongXa,
        defaultAddress?.thanhPho || item.thanhPho,
        defaultAddress?.tinh || item.tinh
    ].filter(Boolean).join(', ');

    return composed || '';
});

const orderDiscountAmount = computed(() => {
    const total = order.value.tongTien || 0;
    const final = order.value.tongTienSauGiam || total;
    const shipping = order.value.phiVanChuyen || 0;
    // tongTienSauGiam đã gồm phí ship, nên dòng giảm giá chỉ so tiền hàng sau giảm với tongTien.
    const productTotalAfterDiscount = Math.max(0, final - shipping);
    return Math.max(0, total - productTotalAfterDiscount);
});

const orderTotalAmount = computed(() => order.value.tongTienSauGiam || order.value.tongTien || 0);

// Tổng tiền theo GIÁ GỐC (trước đợt giảm giá) — giaHienTai đóng vai trò giá gốc như POS.
const originalTotalAmount = computed(() => {
    const items = order.value?.listsHoaDonChiTiet || [];
    return items.reduce((sum, it) => sum + (Number(it.giaHienTai || it.donGia || 0) * Number(it.soLuong || 0)), 0);
});

// Số tiền đã giảm từ ĐỢT GIẢM GIÁ = chênh lệch giá gốc và giá đã chốt (donGia đã là giá sau đợt).
const campaignDiscountAmount = computed(() => Math.max(0, originalTotalAmount.value - Number(order.value?.tongTien || 0)));

const campaignDiscountPercent = computed(() => {
    if (originalTotalAmount.value === 0) return 0;
    return Math.round((campaignDiscountAmount.value / originalTotalAmount.value) * 100);
});

const initialHistoryLog = computed(() => {
    let performer;
    if (order.value.loaiDon === 'ONLINE') {
        // Đặt online: người tạo là khách hàng
        performer = order.value.tenKhachHang || 'Khách hàng';
    } else {
        // Tại quầy: người tạo là nhân viên
        performer = order.value.tenNhanVien || order.value.maNhanVien || null;
    }
    return {
        trangThaiMoi: 'CHO_XAC_NHAN', // Trạng thái mặc định khi tạo đơn
        ghiChu: order.value.ghiChu || 'Khởi tạo đơn hàng',
        nguoiThucHien: performer || order.value.nguoiTao || 'Hệ thống',
        ngayTao: order.value.ngayTao
    };
});

const sortedHistoryLogs = computed(() => {
    const logs = Array.isArray(order.value?.listsLichSuHoaDon) ? [...order.value.listsLichSuHoaDon] : [];
    logs.sort((a, b) => new Date(b.ngayTao || 0) - new Date(a.ngayTao || 0));
    return logs;
});

const allowedStatuses = computed(() => {
    const current = order.value.trangThai;
    const allItems = [
        { title: 'Chờ xác nhận', value: 'CHO_XAC_NHAN' },
        { title: 'Đã xác nhận', value: 'XAC_NHAN' },
        { title: 'Chờ giao hàng', value: 'CHO_GIAO' },
        { title: 'Đang giao hàng', value: 'DANG_GIAO' },
        { title: 'Hoàn thành', value: 'HOAN_THANH' },
        { title: 'Đã hủy', value: 'DA_HUY' },
        { title: 'Hoàn đơn', value: 'HOAN_DON' }
    ];

    if (!current) return allItems;

    return allItems.filter(item => {
        if (item.value === current) return true;

        switch (current) {
            case 'CHO_XAC_NHAN':
                return item.value === 'XAC_NHAN' || item.value === 'DA_HUY';
            case 'XAC_NHAN':
                return item.value === 'CHO_GIAO' || item.value === 'DA_HUY';
            case 'CHO_GIAO':
                return item.value === 'DANG_GIAO' || item.value === 'DA_HUY';
            case 'DANG_GIAO':
                return item.value === 'HOAN_THANH' || item.value === 'HOAN_DON';
            case 'HOAN_THANH':
                return item.value === 'HOAN_DON';
            case 'DA_HUY':
            case 'HOAN_DON':
            default:
                return false;
        }
    });
});

// --- Navigation ---
const goBack = () => {
    if (route.query.from === 'khachhang') {
        router.push({ path: PATH.KHACH_HANG, query: { view: 'invoices', khId: route.query.khId } });
    } else {
        router.push(PATH.HOA_DON);
    }
};

const goToList = () => {
    if (route.query.from === 'khachhang') {
        router.push({ path: PATH.KHACH_HANG, query: { view: 'invoices', khId: route.query.khId } });
    } else {
        router.push(PATH.HOA_DON);
    }
};

// --- Payment Helpers ---
const getPaymentItemClass = (tenPhuongThuc) => {
    const name = String(tenPhuongThuc || '').toLowerCase();
    if (name.includes('tiền mặt') || name.includes('tien mat') || name.includes('cash')) {
        return 'bg-success-light';
    }
    if (name.includes('momo') || name.includes('zalopay') || name.includes('vi') || name.includes('ví')) {
        return 'bg-warning-light';
    }
    return 'bg-primary-light';
};

const getPaymentMethodIcon = (tenPhuongThuc) => {
    const name = String(tenPhuongThuc || '').toLowerCase();
    if (name.includes('tiền mặt') || name.includes('tien mat') || name.includes('cash')) {
        return 'mdi-cash-multiple';
    }
    if (name.includes('momo') || name.includes('zalopay') || name.includes('vi') || name.includes('ví')) {
        return 'mdi-wallet-outline';
    }
    return 'mdi-bank-outline';
};

const getPaymentMethodColor = (tenPhuongThuc) => {
    const name = String(tenPhuongThuc || '').toLowerCase();
    if (name.includes('tiền mặt') || name.includes('tien mat') || name.includes('cash')) {
        return 'success';
    }
    if (name.includes('momo') || name.includes('zalopay') || name.includes('vi') || name.includes('ví')) {
        return 'purple';
    }
    return 'primary';
};

const getPaymentStatusColor = (pay) => {
    const status = pay.trangThai;
    if (status === 1 || status === 'NGUNG_HOAT_DONG') {
        return 'success';
    }
    if (status === 0 || status === 'DANG_HOAT_DONG') {
        const note = String(pay.ghiChu || '').toLowerCase();
        if (note.includes('chờ') || note.includes('cho') || note.includes('pending')) {
            return 'warning';
        }
        return 'success';
    }
    return 'success';
};

const getPaymentStatusText = (pay) => {
    const status = pay.trangThai;
    if (status === 1 || status === 'NGUNG_HOAT_DONG') {
        return 'Thành công';
    }
    if (status === 0 || status === 'DANG_HOAT_DONG') {
        const note = String(pay.ghiChu || '').toLowerCase();
        if (note.includes('chờ') || note.includes('cho') || note.includes('pending')) {
            return 'Chờ thanh toán';
        }
        return 'Thành công';
    }
    return 'Thành công';
};





const timelineSteps = computed(() => {
    const status = getOrderStatus();

    // Core flow steps
    const coreSteps = [
        { key: 0, label: 'Chờ xác nhận', icon: CalendarIcon, note: 'Đơn hàng mới tạo' },
        { key: 1, label: 'Đã xác nhận', icon: CircleCheckIcon, note: 'Đơn hàng đã được xác nhận' },
        { key: 2, label: 'Chờ giao', icon: PackageIcon, note: 'Đơn hàng chờ giao' },
        { key: 3, label: 'Đang giao', icon: TruckIcon, note: 'Đơn hàng đang được giao' },
        { key: 4, label: 'Hoàn thành', icon: CheckIcon, note: 'Đơn hàng đã hoàn thành' }
    ];

    // Exception steps
    const exceptionSteps = [
        { key: 5, label: 'Đã hủy', icon: CircleXIcon, note: 'Đơn hàng bị hủy' },
        { key: 6, label: 'Hoàn đơn', icon: CircleXIcon, note: 'Đơn hàng đã hoàn trả' }
    ];

    let steps = [...coreSteps];
    const tsMap = getStatusTimestampMap.value;
    const statusOrdinal = status === null ? -1 : status;

    // Nếu trạng thái hiện tại là Hủy hoặc Hoàn đơn, chúng ta sẽ hiển thị nó là bước cuối cùng hoặc thay thế bước tương ứng
    if (status === 5 || status === 6) {
        const exc = exceptionSteps.find((s) => s.key === status);
        if (exc) {
            steps = [...coreSteps.slice(0, 4), exc]; // Giữ 4 bước đầu, bước 5 là trạng thái đặc biệt
        }
    }

    const currentActiveIndex = steps.findIndex((s) => s.key === status);

    return steps
        .filter((_, index) => index <= currentActiveIndex)
        .map((step, index) => {
            let state = 'pending';

            if (status === 5 || status === 6) {
                if (index < currentActiveIndex) state = 'done';
                else if (index === currentActiveIndex) state = 'active';
                else state = 'disabled';
            } else {
                if (index < currentActiveIndex) state = 'done';
                else if (index === currentActiveIndex) state = 'active';
                else state = 'pending';
            }

            return {
                ...step,
                timestamp: tsMap?.[step.key] ?? null,
                state: state,
                tone: getStatusTone(step.key)
            };
        });
});

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);
const formatDate = (date) => (date ? new Date(date).toLocaleString('vi-VN') : 'N/A');

const openStatusDialog = () => {
    selectedStatus.value = order.value.trangThai;
    statusDialogOpen.value = true;
};

const requestStatusUpdate = (status) => {
    let targetStatus = status;
    if (!targetStatus) {
        if (selectedStatus.value === order.value.trangThai) return;
        targetStatus = selectedStatus.value;
    }

    const targetOrdinal = getOrderStatusOrdinal(targetStatus);

    confirmDialog.value = {
        show: true,
        title: 'Cập nhật trạng thái',
        message: `Xác nhận chuyển đơn hàng sang trạng thái [${getStatusLabel(targetStatus)}]?`,
        color: 'primary',
        showInput: true,
        inputLabel: 'Ghi chú cập nhật',
        inputRequired: targetOrdinal === ORDER_STATUS_ORDINALS.DA_HUY || targetOrdinal === ORDER_STATUS_ORDINALS.HOAN_DON, // Yêu cầu ghi chú khi hủy đơn hoặc trả hàng
        action: async (note) => {
            confirmDialog.value.loading = true;
            try {
                await dichVuHoaDon.capNhatTrangThaiHoaDon(order.value.id, targetOrdinal, note);
                addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật trạng thái đơn hàng', color: 'success' });
                await loadOrderDetail();
                confirmDialog.value.show = false;
                statusDialogOpen.value = false;
            } catch (error) {
                addNotification({ title: 'Lỗi', subtitle: 'Cập nhật thất bại', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

const openEditModal = () => {
    activeTab.value = 0;
    editForm.value = {
        trangThai: order.value.trangThai,
        tenNguoiNhan: order.value.tenNguoiNhan || "",
        soDienThoaiNguoiNhan: order.value.soDienThoaiNguoiNhan || "",
        diaChiNguoiNhan: order.value.diaChiNguoiNhan || "",
        ghiChu: order.value.ghiChu || "",
        ghiChuTrangThai: ""
    };
    editOrderDialogOpen.value = true;
};

const submitEditOrder = async () => {
    loading.value = true;
    try {
        // 1. Update Info (Recipient details, note)
        await dichVuHoaDon.capNhatThongTinHoaDon(order.value.id, {
            tenNguoiNhan: editForm.value.tenNguoiNhan,
            soDienThoaiNguoiNhan: editForm.value.soDienThoaiNguoiNhan,
            diaChiNguoiNhan: editForm.value.diaChiNguoiNhan,
            ghiChu: editForm.value.ghiChu
        });

        // 2. Update Status if changed
        if (editForm.value.trangThai !== order.value.trangThai) {
            const targetOrdinal = getOrderStatusOrdinal(editForm.value.trangThai);
            await dichVuHoaDon.capNhatTrangThaiHoaDon(
                order.value.id,
                targetOrdinal,
                editForm.value.ghiChuTrangThai || "Cập nhật trạng thái từ modal chỉnh sửa"
            );
        }

        addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật thông tin đơn hàng', color: 'success' });
        await loadOrderDetail();
        editOrderDialogOpen.value = false;
    } catch (error) {
        console.error(error);
        addNotification({ title: 'Lỗi', subtitle: 'Cập nhật đơn hàng thất bại', color: 'error' });
    } finally {
        loading.value = false;
    }
};





// --- Timeline Helpers ---
const getStepIcon = (step) => step.icon;

const printInvoice = async () => {
    // Dung helper chung de mau in o Quan ly hoa don va Ban hang luon di cung mot API/HTML.
    await printHoaDonById(order.value.id);
};

onMounted(() => {
    loadOrderDetail();
});
</script>

<template>
    <v-container fluid class="pa-6 animate-fade-in screen-scroll bg-slate-50">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý bán hàng', disabled: false, href: '#' },
            { title: 'Hóa đơn', disabled: false, to: PATH.HOA_DON },
            { title: `Chi tiết #${order?.maHoaDon || '...'}`, disabled: true }
        ]" />

        <!-- Header -->
        <v-card elevation="0" class="premium-card-detail mb-6 pa-6 bg-white">
            <div class="header-section">
                <div class="header-left d-flex align-center">
                    <v-btn icon variant="flat" color="white" class="mr-3 border elevation-1 rounded-lg" size="36"
                        style="height: 36px !important; width: 36px !important; min-height: 36px !important"
                        @click="goBack">
                        <v-icon size="18" color="slate-700">mdi-arrow-left</v-icon>
                    </v-btn>
                    <div class="d-flex align-center flex-wrap ga-3">
                        <div v-if="loaded" class="text-body-2 text-slate-800 d-flex align-center">
                            Mã hóa đơn: <span class="ml-1">#{{ order.maHoaDon }}</span>
                        </div>
                        |
                        <v-chip v-if="showStatusChip" variant="flat"
                            :class="['px-4 status-chip', getOrderStatusMeta(order.trangThai)?.chipClass]">
                            {{ orderStatusLabel }}
                        </v-chip>
                    </div>
                </div>
                <div class="header-right d-flex align-center">
                    <div v-if="loaded" class="text-body-2 text-slate-700 d-flex align-center">
                        <v-icon color="primary" class="mr-2" size="20">mdi-account-tie-outline</v-icon>
                        <span>Nhân viên hỗ trợ:</span>
                        <span class="font-weight-bold ml-1 text-slate-900">
                            {{ order.tenNhanVien ? `${order.tenNhanVien}
                            (${order.maNhanVien || 'N/A'})` : 'Hệ thống' }}
                        </span>
                    </div>
                </div>
            </div>
        </v-card>

        <v-card elevation="0" class="premium-card-detail mb-6 overflow-hidden premium-timeline bg-white">
            <div class="card-title pa-4 border-b d-flex align-center justify-space-between bg-slate-50">
                <div class="d-flex align-center">
                    <CalendarIcon size="20" class="mr-3 text-primary" />
                    <span class="text-slate-800 font-weight-bold">Trạng thái hóa đơn</span>
                </div>
                <v-btn variant="outlined" color="primary" class="rounded-lg px-4" height="36"
                    @click="showHistoryModal = true">
                    <v-icon size="18" class="mr-1">mdi-history</v-icon>
                    Lịch sử thao tác
                </v-btn>
            </div>
            <div class="timeline-wrap pa-6">
                <transition-group name="timeline-anim">
                    <div v-for="(step, index) in timelineSteps" :key="step.key" class="timeline-step"
                        :class="[step.state, step.state === 'active' ? 'text-' + step.tone : 'text-slate-400']">
                        <div class="node-section">
                            <div class="node" :class="step.state">
                                <component :is="getStepIcon(step)" size="22" />
                            </div>
                        </div>
                        <div class="timeline-info">
                            <div class="text-body-2 mb-1">{{ step.label }}</div>

                        </div>
                    </div>
                </transition-group>
            </div>
        </v-card>

        <!-- Main Layout: 2 Columns (6-6) -->
        <v-row v-if="loaded" class="gx-4 mb-4" style="align-items: stretch;">
            <!-- LEFT COLUMN (6/12): Tổng thanh toán, Lịch sử thanh toán, Thao tác đơn hàng -->
            <v-col cols="12" lg="6" class="d-flex flex-column ga-4">
                <!-- 0. Invoice Info -->
                <v-card elevation="0" class="premium-card mb-0 bg-white flex-grow-0">
                    <div class="card-title pa-4 border-b d-flex align-center bg-slate-50">
                        <v-icon size="20" class="mr-3 text-primary">mdi-information-outline</v-icon>
                        <span class="text-slate-800">Thông tin hóa đơn</span>
                    </div>
                    <v-card-text class="pa-4">
                        <v-row align="center">
                            <v-col cols="4" class="text-body-2 text-slate-500 font-weight-medium">
                                Tạo lúc
                            </v-col>
                            <v-col cols="8">
                                <div class="text-body-2 text-slate-600 pa-2 bg-slate-50 rounded-lg d-flex justify-space-between align-center border"
                                    style="border-color: #f1f5f9 !important;">
                                    <span>{{ order.ngayTao ? formatDate(order.ngayTao) : '' }}</span>
                                    <v-icon size="16" class="text-slate-400">mdi-calendar-blank-outline</v-icon>
                                </div>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <!-- 1. Customer Info -->
                <v-card elevation="0" class="premium-card mb-0 bg-white flex-grow-0">
                    <div class="card-title pa-4 border-b d-flex align-center justify-space-between bg-slate-50">
                        <div class="d-flex align-center">
                            <UserIcon size="20" class="mr-3 text-primary" />
                            <span class="text-slate-800">Thông tin khách hàng</span>
                        </div>
                    </div>
                    <v-card-text class="pa-4">
                        <v-row align="start" class="ga-1 customer-info-row">
                            <!-- Image Column -->
                            <v-col cols="auto">
                                <v-avatar size="150"
                                    class="rounded-xl elevation-4 border-md border-primary-subtle bg-white">
                                    <v-img :src="customerAvatarUrl" cover></v-img>
                                </v-avatar>
                            </v-col>

                            <!-- Unified Grid Info Column -->
                            <v-col cols="12" sm="" class="flex-grow-1 ps-sm-6 py-0 d-flex align-center">
                                <v-row class="w-100 h-100 py-1" dense>
                                    <!-- Left Info: Profile & Notes -->
                                    <v-col cols="12" md="6" class="py-0">
                                        <div class="d-flex flex-column h-100 justify-center ga-3">
                                            <div class="info-item mb-3">
                                                <div class="text-body-2 text-slate-500 mb-1">Họ và tên</div>
                                                <div class="text-body-2 text-slate-900 d-flex align-center">
                                                    <v-icon color="primary" class="mr-2"
                                                        size="18">mdi-account-outline</v-icon>
                                                    <span>{{ customerName }}</span>
                                                </div>
                                            </div>
                                            <div class="info-item mb-3">
                                                <div class="text-body-2 text-slate-500 mb-1">Mã khách hàng</div>
                                                <div class="text-body-2 text-slate-800 d-flex align-center">
                                                    <v-icon color="primary" class="mr-2"
                                                        size="18">mdi-card-account-details-outline</v-icon>
                                                    <span>{{ order.maKhachHang || 'Khách vãng lai' }}</span>
                                                </div>
                                            </div>
                                            <div class="info-item">
                                                <div class="text-body-2 text-slate-500 mb-1">Ghi chú khách hàng</div>
                                                <div class="text-body-2 text-slate-600 d-flex align-center">
                                                    <v-icon color="primary" class="mr-2"
                                                        size="18">mdi-note-text-outline</v-icon>
                                                    <span>{{ order.khachHang?.ghiChu || 'Không có ghi chú đặc biệt'
                                                        }}</span>
                                                </div>
                                            </div>
                                        </div>
                                    </v-col>
                                    <v-col cols="12" md="6" class="py-0 ps-md-4">
                                        <div class="d-flex flex-column h-100 justify-center ga-3">
                                            <div class="info-item mb-3">
                                                <div class="text-body-2 text-slate-500 mb-1">Số điện thoại</div>
                                                <div class="text-body-2 text-slate-800 d-flex align-center">
                                                    <v-icon color="primary" class="mr-2"
                                                        size="18">mdi-phone-check</v-icon>
                                                    <span>{{ order.soDienThoaiKhachHang || 'Chưa có' }}</span>
                                                </div>
                                            </div>
                                            <div class="info-item mb-3">
                                                <div class="text-body-2 text-slate-500 mb-1">Email</div>
                                                <div class="text-body-2 text-slate-800 d-flex align-center">
                                                    <v-icon color="primary" class="mr-2"
                                                        size="18">mdi-email-check</v-icon>
                                                    <span>{{ order.emailKhachHang || 'N/A' }}</span>
                                                </div>
                                            </div>
                                            <div class="info-item">
                                                <div class="text-body-2 text-slate-500 mb-1">Ngày đăng ký</div>
                                                <div class="text-body-2 text-slate-600 d-flex align-center">
                                                    <v-icon color="primary" class="mr-2"
                                                        size="18">mdi-calendar-range</v-icon>
                                                    <span>{{ formatDate(order.khachHang?.ngayTao) }}</span>
                                                </div>
                                            </div>
                                        </div>
                                    </v-col>
                                </v-row>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <!-- 2. Shipping Info -->
                <v-card elevation="0" class="premium-card mb-0 bg-white flex-grow-1">
                    <div class="card-title pa-3 border-b d-flex align-center justify-space-between bg-slate-50">
                        <div class="d-flex align-center">
                            <TruckIcon size="20" class="mr-3 text-primary" />
                            <span class="text-slate-800">Thông tin nhận hàng</span>
                        </div>
                    </div>
                    <v-card-text class="pa-3">
                        <div class="info-group mb-4">
                            <v-row dense align="center" class="mb-2">
                                <v-col cols="4" class="text-body-2 text-slate-500 font-weight-medium">
                                    Tên người nhận
                                </v-col>
                                <v-col cols="8">
                                    <div class="text-body-2 text-slate-600 pa-2 bg-slate-50 rounded-lg border"
                                        style="border-color: #f1f5f9 !important;">
                                        {{ order.tenNguoiNhan || customerName || 'Khách vãng lai' }}
                                    </div>
                                </v-col>
                            </v-row>
                            <v-row dense align="center" class="mb-2">
                                <v-col cols="4"
                                    class="text-body-2 text-slate-500 font-weight-medium d-flex align-center">
                                    Số điện thoại

                                </v-col>
                                <v-col cols="8">
                                    <div class="text-body-2 text-slate-600 pa-2 bg-slate-50 rounded-lg border"
                                        style="border-color: #f1f5f9 !important;">
                                        {{ order.soDienThoaiNguoiNhan || order.soDienThoaiKhachHang || 'Chưa có SĐT' }}
                                    </div>
                                </v-col>
                            </v-row>
                            <v-row dense align="center">
                                <v-col cols="4"
                                    class="text-body-2 text-slate-500 font-weight-medium d-flex align-center">
                                    Địa chỉ nhận hàng
                                </v-col>
                                <v-col cols="8">
                                    <div class="text-body-2 text-slate-600 pa-2 bg-slate-50 rounded-lg border"
                                        style="border-color: #f1f5f9 !important;">
                                        <template v-if="displayAddress">{{ displayAddress }}</template>
                                        <template v-else><em class="text-slate-400">Chưa có địa chỉ</em></template>
                                    </div>
                                </v-col>
                            </v-row>
                        </div>

                        <v-divider class="mb-3 border-opacity-10"></v-divider>

                        <!-- Order Type -->
                        <div class="info-group mb-3">
                            <v-row dense align="center">
                                <v-col cols="4" class="text-body-2 text-slate-500 font-weight-medium">
                                    Loại đơn hàng
                                </v-col>
                                <v-col cols="8">
                                    <v-chip variant="flat" class="px-3"
                                        style="background-color: #eff6ff !important; color: #1e40af !important; font-weight: 500;">
                                        {{ orderTypeLabel }}
                                    </v-chip>
                                </v-col>
                            </v-row>
                        </div>

                        <v-divider class="mb-3 border-opacity-10"></v-divider>

                        <!-- Ghi chú -->
                        <div class="info-group">
                            <div class="text-body-2 text-slate-500 mb-2 font-weight-medium">Ghi chú vận chuyển</div>
                            <div class="text-body-2 text-slate-600 pa-2 bg-slate-50 rounded-lg border"
                                style="border-color: #f1f5f9 !important; min-height: 44px;">
                                {{ order.ghiChu || 'Không có ghi chú' }}
                            </div>
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>

            <!-- RIGHT COLUMN (6/12): Thông tin khách hàng, Thông tin vận chuyển -->

            <v-col cols="12" lg="6" class="d-flex flex-column ga-4">
                <!-- 1. Order Summary -->
                <v-card elevation="0" class="premium-card mb-0 bg-white flex-grow-0">
                    <div class="card-title pa-3 border-b d-flex align-center bg-slate-50">
                        <CreditCardIcon size="20" class="mr-3 text-primary" />
                        <span class="text-slate-800">Tổng thanh toán</span>
                    </div>
                    <div class="summary-section pa-3 pt-6 d-flex align-center">
                        <div class="summary-grid w-100">
                            <div class="summary-row mb-5">
                                <span class="text-slate-500">Tổng tiền hàng:</span>
                                <span class="text-body-2 text-primary" style="font-weight: 700 !important;">{{
                                    formatCurrency(order.tongTien) }}</span>
                            </div>
                            <div v-if="campaignDiscountAmount > 0" class="summary-row mb-3">
                                <span class="text-slate-500">Đợt giảm giá:</span>
                                <span class="text-body-2 font-weight-bold" style="color: #dc2626 !important;">-{{
                                    campaignDiscountPercent }}%</span>
                            </div>
                            <div class="summary-row mb-3">
                                <span class="text-slate-500">Phiếu giảm giá:</span>
                                <div class="text-right d-flex align-center justify-end">
                                    <template v-if="order.maPhieuGiamGia">
                                        <span class="text-body-2 font-weight-bold text-slate-800">{{
                                            order.maPhieuGiamGia }}</span>
                                    </template>
                                    <template v-else-if="orderDiscountAmount > 0">
                                        <v-chip size="x-small" color="error" variant="flat">Đợt giảm giá</v-chip>
                                    </template>
                                    <template v-else>
                                        <span class="text-body-2 text-slate-500 d-flex align-center">
                                            <v-icon size="14" class="mr-1">mdi-minus-circle-outline</v-icon>
                                            Chưa áp dụng
                                        </span>
                                    </template>
                                </div>
                            </div>
                            <div v-if="orderDiscountAmount > 0" class="summary-row mb-5 discount-amount-row">
                                <span class="font-weight-medium">Giảm giá:</span>
                                <span class="text-body-2 font-weight-bold discount-amount-value">- {{
                                    formatCurrency(Math.abs(orderDiscountAmount)) }}</span>
                            </div>
                            <div class="summary-row mb-4">
                                <span class="text-slate-500 d-flex align-center">
                                    <span>Phí vận chuyển:</span>
                                    <svg width="45" height="15" viewBox="0 0 45 15" fill="none"
                                        xmlns="http://www.w3.org/2000/svg"
                                        style="display: inline-block; vertical-align: middle; margin-left: 6px;">
                                        <!-- Left Chevron (Deep Blue/Teal) -->
                                        <path d="M1 2.5 L7 2.5 L4.5 6.5 L7 6.5 L3.5 10.5 L1 10.5 L3.5 6.5 L1 6.5 Z"
                                            fill="#0C2A46" />
                                        <!-- Right Chevron (Orange) -->
                                        <path
                                            d="M5.5 2.5 L11.5 2.5 L9 6.5 L11.5 6.5 L8 10.5 L5.5 10.5 L8 6.5 L5.5 6.5 Z"
                                            fill="#FA6400" />
                                        <!-- GHN Text (Italic Orange) -->
                                        <text x="13.5" y="11" fill="#FA6400" font-family="'Inter', sans-serif"
                                            font-weight="900" font-style="italic" font-size="10.5"
                                            letter-spacing="-0.5px">GHN</text>
                                    </svg>
                                </span>
                                <span class="text-body-2 text-slate-800">{{
                                    formatCurrency(order.phiVanChuyen || 0)
                                    }}</span>
                            </div>
                            <div v-if="order.phiHoanHang > 0" class="summary-row mb-4">
                                <span class="text-slate-500 d-flex align-center">
                                    <span>Phí hoàn hàng:</span>
                                    <svg width="45" height="15" viewBox="0 0 45 15" fill="none"
                                        xmlns="http://www.w3.org/2000/svg"
                                        style="display: inline-block; vertical-align: middle; margin-left: 6px;">
                                        <!-- Left Chevron (Deep Blue/Teal) -->
                                        <path d="M1 2.5 L7 2.5 L4.5 6.5 L7 6.5 L3.5 10.5 L1 10.5 L3.5 6.5 L1 6.5 Z"
                                            fill="#0C2A46" />
                                        <!-- Right Chevron (Orange) -->
                                        <path
                                            d="M5.5 2.5 L11.5 2.5 L9 6.5 L11.5 6.5 L8 10.5 L5.5 10.5 L8 6.5 L5.5 6.5 Z"
                                            fill="#FA6400" />
                                        <!-- GHN Text (Italic Orange) -->
                                        <text x="13.5" y="11" fill="#FA6400" font-family="'Inter', sans-serif"
                                            font-weight="900" font-style="italic" font-size="10.5"
                                            letter-spacing="-0.5px">GHN</text>
                                    </svg>
                                </span>
                                <span class="text-body-2 text-slate-800">{{
                                    formatCurrency(order.phiHoanHang || 0)
                                    }}</span>
                            </div>
                            <v-divider class="my-5 border-opacity-25"></v-divider>
                            <div class="summary-row pb-2">
                                <span class="text-body-2 text-slate-800">Tổng cộng:</span>
                                <span class="text-body-2 text-primary" style="font-weight: 700 !important;">{{
                                    formatCurrency(orderTotalAmount) }}</span>
                            </div>
                        </div>
                    </div>
                </v-card>

                <!-- 2. Payment History -->
                <v-card elevation="0" class="premium-card mb-0 bg-white flex-grow-1">
                    <div class="card-title pa-4 border-b d-flex align-center bg-slate-50">
                        <CreditCardIcon size="20" class="mr-3 text-primary" />
                        <span class="text-slate-800">Lịch sử thanh toán</span>
                    </div>
                    <v-card-text class="pa-3">
                        <template v-if="order.listsGiaoDichThanhToan?.length">
                            <div v-for="pay in [order.listsGiaoDichThanhToan[0]]" :key="pay.id">
                                <div class="info-group mb-3">
                                    <div class="d-flex justify-space-between align-start w-100">
                                        <div>
                                            <div class="text-body-2 text-slate-500 mb-2">Phương thức thanh toán</div>
                                            <v-chip variant="tonal" :color="getPaymentMethodColor(pay.tenPhuongThuc)"
                                                class="px-3 py-1 font-weight-bold">
                                                <v-icon start size="16">{{ getPaymentMethodIcon(pay.tenPhuongThuc)
                                                    }}</v-icon>
                                                {{ pay.tenPhuongThuc === 'TIEN_MAT' ? 'Tiền mặt' : pay.tenPhuongThuc ===
                                                    'CHUYEN_KHOAN' ? 'Chuyển khoản' : pay.tenPhuongThuc }}
                                                <span v-if="pay.tenPhuongThuc !== 'TIEN_MAT' && pay.ghiChu"
                                                    class="ml-1">
                                                    - {{ pay.ghiChu.length > 30 ? pay.ghiChu.substring(0, 30) + '...' :
                                                    pay.ghiChu }}
                                                </span>
                                            </v-chip>
                                        </div>
                                        <div class="text-right">
                                            <div class="text-body-2 text-slate-500 mb-2">Tổng tiền</div>
                                            <div class="text-primary"
                                                style="font-weight: 700 !important; font-size: 1.1rem">{{
                                                    formatCurrency(pay.soTien) }}</div>
                                        </div>
                                    </div>
                                </div>
                                <v-divider class="mb-4 border-opacity-10"></v-divider>
                                <div class="info-group mb-3">
                                    <div class="d-flex justify-space-between align-start w-100">
                                        <div>
                                            <div class="text-body-2 text-slate-500 mb-2">Xác nhận giao dịch</div>
                                            <div class="text-body-2 text-slate-700 d-flex align-center mb-2 mt-1">
                                                <v-icon color="primary" class="mr-2"
                                                    size="18">mdi-clock-outline</v-icon>
                                                <span><span class="font-weight-medium">Thời gian:</span> {{
                                                    formatDate(pay.ngayTao) }}</span>
                                            </div>
                                            <div class="text-body-2 text-slate-700 d-flex align-center">
                                                <v-icon color="primary" class="mr-2"
                                                    size="18">mdi-account-check-outline</v-icon>
                                                <span><span class="font-weight-medium">Người xác nhận:</span> {{
                                                    formatNguoi(pay.nguoiXacNhan || order.tenNhanVien, order.loaiDon === 'ONLINE') }}</span>
                                            </div>
                                        </div>
                                        <div class="text-right d-flex flex-column align-end">
                                            <div class="text-body-2 text-slate-500 mb-2">Trạng thái</div>
                                            <v-chip :color="getPaymentStatusColor(pay)" size="small" variant="tonal"
                                                class="px-3 rounded-lg font-weight-bold mb-2">
                                                {{ getPaymentStatusText(pay) }}
                                            </v-chip>
                                            <div
                                                class="text-caption text-slate-600 d-flex align-center bg-slate-50 px-2 py-1 rounded">
                                                <v-icon size="14" class="mr-1">mdi-barcode-scan</v-icon>
                                                Mã GD: {{ pay.maGiaoDichNgoai || 'Nội bộ' }}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <v-divider class="mb-3 border-opacity-10"></v-divider>
                                <div class="info-group">
                                    <div class="text-body-2 text-slate-500 mb-2">Ghi chú thanh toán</div>
                                    <div class="text-body-2 text-slate-600 pa-2 bg-slate-50 rounded-lg">
                                        {{ pay.ghiChu || 'Không có ghi chú' }}
                                    </div>
                                </div>
                            </div>
                        </template>
                        <div v-else class="text-center py-8 text-slate-400">
                            Chưa có lịch sử giao dịch
                        </div>
                    </v-card-text>
                </v-card>

                <!-- 3. Action Buttons Card -->
                <v-card elevation="0"
                    class="premium-card mb-0 pa-4 bg-white d-flex flex-column justify-center ga-3 flex-grow-0"
                    style="border: 1px dashed rgba(30, 37, 124, 0.3) !important; background: rgba(30, 37, 124, 0.02) !important;">
                    <div class="text-body-2 text-slate-600 font-weight-bold text-center mb-1">Thao tác đơn hàng</div>
                    <v-btn v-if="order.canHoanPhi" variant="flat" color="deep-purple" class="rounded-lg px-6"
                        height="44" @click="confirmHoanPhi">
                        <template v-slot:prepend>
                            <v-icon size="18" class="mr-1">mdi-cash-refund</v-icon>
                        </template>
                        Xác nhận hoàn phí
                    </v-btn>
                    <div v-else-if="order.daHoanPhi" class="d-flex align-center justify-center ga-2 py-1">
                        <v-icon size="18" color="success">mdi-check-decagram</v-icon>
                        <span class="text-success font-weight-bold">Đã hoàn phí cho khách</span>
                    </div>
                    <v-btn variant="flat" color="primary" class="rounded-lg px-6" height="44" @click="printInvoice">
                        <template v-slot:prepend>
                            <PrinterIcon size="18" class="mr-1" />
                        </template>
                        In hóa đơn
                    </v-btn>
                    <v-btn color="primary" variant="flat" class="rounded-lg px-6" height="44" @click="openEditModal">
                        <template v-slot:prepend>
                            <EditIcon size="18" class="mr-2" />
                        </template>
                        Chỉnh sửa đơn hàng
                    </v-btn>
                </v-card>
            </v-col>
        </v-row>

        <v-card v-if="loaded" elevation="0" class="premium-card-detail mt-8 bg-white overflow-hidden">
            <div class="card-title pa-4 border-b d-flex align-center justify-space-between bg-slate-50">
                <div class="d-flex align-center">
                    <LayoutGridIcon size="20" class="mr-3 text-primary" />
                    <span class="text-slate-800">Sản phẩm đã đặt</span>
                </div>
                <div v-if="isOrderEditable" class="d-flex align-center ga-2">
                    <v-btn v-if="!editingProducts" variant="tonal" color="primary" size="small" class="rounded-lg"
                        @click="startEditProducts">
                        <EditIcon size="16" class="mr-1" /> Sửa số lượng
                    </v-btn>
                    <template v-else>
                        <v-btn variant="text" color="slate-500" size="small" class="rounded-lg"
                            :disabled="savingProducts" @click="cancelEditProducts">Hủy</v-btn>
                        <v-btn variant="flat" color="primary" size="small" class="rounded-lg" :loading="savingProducts"
                            @click="saveProducts">
                            <CheckIcon size="16" class="mr-1" /> Lưu
                        </v-btn>
                    </template>
                </div>
            </div>

            <AdminFilter title="Tìm kiếm sản phẩm" class="px-4 pt-4 border-b-0"
                @refresh="() => { productSearch = ''; priceRange = [0, maxOrderPrice]; productColorFilter = null; productSizeFilter = null; productPagination.page = 1; }">
                    <v-col cols="12" md="4" class="pr-2">
                        <div class="text-caption font-weight-medium text-slate-700 mb-1" style="height: 20px;">Từ khóa
                        </div>
                        <v-text-field v-model="productSearch" placeholder="Tìm tên, mã..." variant="outlined"
                            density="compact" hide-details prepend-inner-icon="mdi-magnify" class="compact-input"
                            @input="productPagination.page = 1"></v-text-field>
                    </v-col>
                    <v-col cols="12" md="3" class="px-2">
                        <div class="text-caption font-weight-medium text-slate-700 mb-1" style="height: 20px;">Màu sắc
                        </div>
                        <v-select v-model="productColorFilter" :items="availableColors" placeholder="Tất cả"
                            variant="outlined" density="compact" hide-details class="compact-input"
                            :menu-props="{ contentClass: 'product-select-menu' }"
                            @update:modelValue="productPagination.page = 1"></v-select>
                    </v-col>
                    <v-col cols="12" md="3" class="px-2">
                        <div class="text-caption font-weight-medium text-slate-700 mb-1" style="height: 20px;">Kích
                            thước</div>
                        <v-select v-model="productSizeFilter" :items="availableSizes" placeholder="Tất cả"
                            variant="outlined" density="compact" hide-details class="compact-input"
                            :menu-props="{ contentClass: 'product-select-menu' }"
                            @update:modelValue="productPagination.page = 1"></v-select>
                    </v-col>
                <template #after>
                    <div class="mt-4 px-2">
                        <div class="d-flex align-center justify-space-between mb-1" style="height: 20px;">
                            <div class="d-flex align-center ga-2">
                                <v-icon size="15" color="#3b82f6" class="mr-2">mdi-cash-multiple</v-icon>
                                <span class="text-caption font-weight-medium text-slate-700">Khoảng giá</span>
                            </div>
                            <span class="text-caption font-weight-medium text-slate-700">
                                {{ formatCurrency(priceRange[0]) }} – {{ formatCurrency(priceRange[1]) }}
                            </span>
                        </div>
                        <v-range-slider v-model="priceRange" :min="0" :max="maxOrderPrice" :step="10000"
                            hide-details color="primary" track-color="#e2e8f0" track-size="2" thumb-size="14"
                            class="blue-range-slider" @update:modelValue="productPagination.page = 1" />
                    </div>
                </template>
            </AdminFilter>

            <AdminTable :headers="productColumns" :items="paginatedProducts" :showAddButton="false" hideToolbar
                class="all-center-table full-width-admin-table">
                <template #row="{ item }">
                    <tr class="hover-row" :class="{ 'price-changed-cell': isPriceChanged(item) }">
                        <td class="py-4">
                            <v-avatar size="80" class="rounded-lg border bg-slate-50 elevation-1">
                                <v-img :src="item.hinhAnh ||
                                    'https://upload.wikimedia.org/wikipedia/commons/1/14/No_Image_Available.jpg'
                                    " cover></v-img>
                            </v-avatar>
                        </td>
                        <td class="py-4">
                            <div class="text-slate-900 text-body-2">{{ item.tenSanPham || 'N/A' }}
                            </div>
                        </td>
                        <td class="py-4">
                            <div class="text-primary">#{{ item.maChiTietSanPham || item.maSanPham || 'N/A' }}</div>
                        </td>
                        <td class="py-4">
                            <span class="text-slate-600">
                                {{ item.mauSac || '—' }}
                            </span>
                        </td>
                        <td class="py-4">
                            <span class="text-slate-600">
                                {{ item.kichThuoc || '—' }}
                            </span>
                        </td>
                        <td class="py-4">
                            <div v-if="editingProducts" class="d-flex align-center justify-center ga-1">
                                <v-btn icon size="x-small" variant="tonal" color="primary"
                                    :disabled="(editQtyMap[item.id] || 1) <= 1" @click="changeProductQty(item, -1)">
                                    <v-icon size="16">mdi-minus</v-icon>
                                </v-btn>
                                <span class="text-body-2 text-slate-800 mx-1"
                                    style="min-width: 24px; display: inline-block; text-align: center;">{{
                                    editQtyMap[item.id] }}</span>
                                <v-btn icon size="x-small" variant="tonal" color="primary"
                                    @click="changeProductQty(item, 1)">
                                    <v-icon size="16">mdi-plus</v-icon>
                                </v-btn>
                            </div>
                            <span v-else class="text-body-2 text-slate-800">{{ item.soLuong }}</span>
                        </td>
                        <td class="py-4 text-slate-700">
                            <template v-if="isPriceChanged(item)">
                                <span class="text-decoration-line-through text-slate-400 mr-1">{{
                                    formatCurrency(item.donGia) }}</span>
                                <span class="text-warning font-weight-bold">{{ formatCurrency(item.giaHienTai) }}</span>
                            </template>
                            <template v-else>{{ formatCurrency(item.donGia) }}</template>
                        </td>
                        <td class="py-4 text-primary text-body-2">
                            <div class="d-flex align-center justify-center ga-2">
                                <span>{{ formatCurrency(Number(editingProducts ? (editQtyMap[item.id] || item.soLuong) :
                                    item.soLuong) *
                                    Number(item.donGia)) }}</span>
                                <v-btn v-if="editingProducts" icon size="x-small" variant="text" color="error"
                                    @click="removeProduct(item)">
                                    <TrashIcon size="16" />
                                </v-btn>
                            </div>
                        </td>
                    </tr>
                    <!-- Dòng vàng: cảnh báo đổi giá -->
                    <tr v-if="isPriceChanged(item)" class="price-warning-row">
                        <td :colspan="productColumns.length" class="py-2 px-4">
                            <div class="d-flex align-center justify-space-between flex-wrap ga-2">
                                <div class="d-flex align-center ga-2 text-amber-darken-4">
                                    <v-icon size="18" color="amber-darken-3">mdi-alert-outline</v-icon>
                                    <span>Giá sản phẩm đã thay đổi: từ <b>{{ formatCurrency(item.donGia) }}</b> thành
                                        <b>{{
                                            formatCurrency(item.giaHienTai) }}</b></span>
                                </div>
                                <v-btn v-if="isOrderEditable" size="x-small" variant="flat" color="amber-darken-2"
                                    class="rounded-lg text-none font-weight-bold" @click="applyNewPrice(item)">
                                    Áp dụng giá mới
                                </v-btn>
                            </div>
                        </td>
                    </tr>
                </template>
                <template #pagination>
                    <AdminPagination v-model="productPagination.page" :page-size="productPagination.size"
                        :total-elements="filteredProducts.length" :current-size="paginatedProducts.length" />
                </template>
            </AdminTable>
        </v-card>

        <div v-else-if="loading" class="d-flex flex-column align-center justify-center py-16">
            <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
            <div class="mt-4 text-slate-500">Đang tải thông tin hóa đơn...</div>
        </div>

        <!-- History Modal -->
        <v-dialog v-model="showHistoryModal" max-width="1000">
            <v-card class="rounded-lg overflow-hidden premium-card mb-0 d-flex flex-column bg-white">
                <div class="card-title pa-4 border-b d-flex align-center justify-space-between bg-slate-50">
                    <div class="d-flex align-center">
                        <v-icon color="primary" class="mr-3">mdi-history</v-icon>
                        <span class="text-slate-800 font-weight-bold text-h6">Lịch sử đơn hàng</span>
                    </div>
                    <v-btn icon="mdi-close" variant="text" density="comfortable"
                        @click="showHistoryModal = false"></v-btn>
                </div>
                <v-card-text class="pa-0 d-flex flex-column flex-grow-1 overflow-hidden">
                    <!-- Table-style Headers for Timeline -->
                    <div class="d-flex align-center w-100 px-4 py-2 bg-slate-800 border-b text-body-2 text-white">
                        <div class="text-center" style="width: 42px;"></div> <!-- Space for timeline dots -->
                        <div class="text-center" style="width: 110px;">Trạng thái</div>
                        <div class="text-center flex-grow-1">Mô tả</div>
                        <div class="text-center" style="width: 150px;">Người thực hiện</div>
                        <div class="text-center" style="width: 180px;">Thời gian</div>
                    </div>

                    <div class="history-scroll-container pa-3 flex-grow-1">
                        <v-timeline side="end" density="compact" line-color="slate-200" class="custom-history-timeline">
                            <!-- Actual logs from DB -->
                            <v-timeline-item v-for="(log, idx) in sortedHistoryLogs" :key="log.id || idx"
                                :dot-color="getStatusInfo(log.trangThaiMoi).color" size="small">
                                <div class="d-flex align-center w-100 py-0">
                                    <!-- Column 1: Status -->
                                    <div style="width: 110px;" class="text-center">
                                        <v-chip variant="flat"
                                            :class="['status-chip', getStatusInfo(log.trangThaiMoi).chipClass]">
                                            {{ getStatusInfo(log.trangThaiMoi).text }}
                                        </v-chip>
                                    </div>

                                    <!-- Column 2: Description -->
                                    <div class="flex-grow-1 text-center">
                                        <div class="text-body-2 text-slate-700">
                                            {{ log.ghiChu || 'Cập nhật trạng thái từ hệ thống' }}
                                        </div>
                                    </div>

                                    <!-- Column 3: Performer -->
                                    <div style="width: 150px;" class="text-center">
                                        <span class="text-body-2 text-slate-600">{{
                                            formatNguoi(log.nguoiThucHien, order.loaiDon === 'ONLINE') }}</span>
                                    </div>

                                    <!-- Column 4: Time -->
                                    <div style="width: 180px;" class="text-center">
                                        <span class="text-body-2 text-slate-400" style="white-space: nowrap;">
                                            {{ formatDate(log.ngayTao) }}
                                        </span>
                                    </div>
                                </div>
                            </v-timeline-item>

                            <!-- Always show creation as the final step (at bottom) -->
                            <v-timeline-item
                                v-if="!sortedHistoryLogs.some(l => l.trangThaiMoi === 'CHO_XAC_NHAN' || l.trangThaiMoi === 0)"
                                :dot-color="getStatusInfo(initialHistoryLog.trangThaiMoi).color" size="small">
                                <div class="d-flex align-center w-100 py-0">
                                    <!-- Column 1: Status -->
                                    <div style="width: 110px;" class="text-center">
                                        <v-chip variant="flat"
                                            :class="['status-chip', getStatusInfo(initialHistoryLog.trangThaiMoi).chipClass]">
                                            {{ getStatusInfo(initialHistoryLog.trangThaiMoi).text }}
                                        </v-chip>
                                    </div>

                                    <!-- Column 2: Description -->
                                    <div class="flex-grow-1 text-center">
                                        <div class="text-body-2 text-slate-700">
                                            {{ initialHistoryLog.ghiChu }}
                                        </div>
                                    </div>

                                    <!-- Column 3: Performer -->
                                    <div style="width: 150px;" class="text-center">
                                        <span class="text-body-2 text-slate-600">{{
                                            formatNguoi(initialHistoryLog.nguoiThucHien, order.loaiDon === 'ONLINE') }}</span>
                                    </div>

                                    <!-- Column 4: Time -->
                                    <div style="width: 180px;" class="text-center">
                                        <span class="text-body-2 text-slate-400" style="white-space: nowrap;">
                                            {{ formatDate(initialHistoryLog.ngayTao) }}
                                        </span>
                                    </div>
                                </div>
                            </v-timeline-item>
                        </v-timeline>
                    </div>
                </v-card-text>
            </v-card>
        </v-dialog>

        <!-- Confirmation Dialog -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" :show-input="confirmDialog.showInput"
            :input-label="confirmDialog.inputLabel" :input-required="confirmDialog.inputRequired"
            @confirm="note => confirmDialog.action(note)" />

        <!-- Edit Order Dialog -->
        <v-dialog v-model="editOrderDialogOpen" max-width="650px" persistent>
            <v-card class="rounded-xl overflow-hidden elevation-12">
                <v-card-title class="pa-5 bg-slate-50 border-b d-flex justify-space-between align-center">
                    <span class="text-h6 font-weight-bold text-slate-800">Chỉnh sửa đơn hàng</span>
                    <v-btn icon variant="text" color="slate-400" @click="editOrderDialogOpen = false">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </v-card-title>

                <!-- Beautiful Tabs -->
                <v-tabs v-model="activeTab" color="primary" border-b grow density="comfortable">
                    <v-tab :value="0" class="text-none font-weight-bold">
                        <v-icon start size="18" class="mr-2">mdi-list-status</v-icon>
                        Trạng thái đơn
                    </v-tab>
                    <v-tab :value="1" class="text-none font-weight-bold">
                        <v-icon start size="18" class="mr-2">mdi-truck-delivery-outline</v-icon>
                        Thông tin giao nhận
                    </v-tab>
                </v-tabs>

                <v-card-text class="pa-6">
                    <v-window v-model="activeTab">
                        <!-- TAB 1: Status selection & update note -->
                        <v-window-item :value="0">
                            <v-row class="ga-3" dense>
                                <!-- Order Status Selection -->
                                <v-col cols="12" class="mb-2">
                                    <span class="text-body-2 text-slate-600 font-weight-bold d-block mb-2">Trạng thái
                                        đơn
                                        hàng</span>
                                    <v-select v-model="editForm.trangThai" :items="allowedStatuses" item-title="title"
                                        item-value="value" variant="outlined" rounded="lg" density="comfortable"
                                        hide-details></v-select>
                                </v-col>

                                <!-- Status Update Note (Visible if status changed) -->
                                <v-col cols="12" v-if="editForm.trangThai !== order.trangThai" class="mb-2">
                                    <span class="text-body-2 text-warning font-weight-bold d-block mb-2">Mô tả cập nhật
                                        trạng
                                        thái</span>
                                    <v-text-field v-model="editForm.ghiChuTrangThai"
                                        placeholder="Nhập mô tả thay đổi trạng thái..." variant="outlined" rounded="lg"
                                        density="comfortable" hide-details></v-text-field>
                                </v-col>
                            </v-row>
                        </v-window-item>

                        <!-- TAB 2: Shipping/Delivery Information -->
                        <v-window-item :value="1">
                            <v-row class="ga-3" dense>
                                <!-- Recipient Name Field -->
                                <v-col cols="12" class="mb-2">
                                    <span class="text-body-2 text-slate-600 font-weight-bold d-block mb-2">Tên người
                                        nhận</span>
                                    <v-text-field v-model="editForm.tenNguoiNhan" placeholder="Nhập tên người nhận..."
                                        variant="outlined" rounded="lg" density="comfortable" hide-details
                                        prepend-inner-icon="mdi-account"></v-text-field>
                                </v-col>
                                <!-- Phone Field -->
                                <v-col cols="12" class="mb-2">
                                    <span class="text-body-2 text-slate-600 font-weight-bold d-block mb-2">Số điện thoại
                                        nhận
                                        hàng</span>
                                    <v-text-field v-model="editForm.soDienThoaiNguoiNhan"
                                        placeholder="Nhập số điện thoại..." variant="outlined" rounded="lg"
                                        density="comfortable" hide-details
                                        prepend-inner-icon="mdi-phone"></v-text-field>
                                </v-col>

                                <!-- Address Field -->
                                <v-col cols="12" class="mb-2">
                                    <span class="text-body-2 text-slate-600 font-weight-bold d-block mb-2">Địa chỉ giao
                                        hàng</span>
                                    <v-textarea v-model="editForm.diaChiNguoiNhan"
                                        placeholder="Nhập địa chỉ giao hàng chi tiết..." variant="outlined" rounded="lg"
                                        density="comfortable" hide-details rows="3"
                                        prepend-inner-icon="mdi-map-marker"></v-textarea>
                                </v-col>

                                <!-- Notes Field -->
                                <v-col cols="12" class="mb-2">
                                    <span class="text-body-2 text-slate-600 font-weight-bold d-block mb-2">Ghi chú đơn
                                        hàng</span>
                                    <v-textarea v-model="editForm.ghiChu"
                                        placeholder="Ghi chú cho shipper hoặc cửa hàng..." variant="outlined"
                                        rounded="lg" density="comfortable" hide-details rows="2"
                                        prepend-inner-icon="mdi-note-text"></v-textarea>
                                </v-col>
                            </v-row>
                        </v-window-item>
                    </v-window>
                </v-card-text>

                <v-card-actions class="pa-5 bg-slate-50 border-t d-flex justify-end ga-3">
                    <v-btn variant="outlined" color="slate-500" class="rounded-lg px-4" height="40"
                        @click="editOrderDialogOpen = false">
                        Hủy
                    </v-btn>
                    <v-btn variant="flat" color="primary" class="rounded-lg px-6" height="40" :loading="loading"
                        @click="submitEditOrder">
                        Lưu thay đổi
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-container>
</template>

<style scoped>
/* Common Layout */
.screen-scroll {
    height: 100%;
    overflow-y: auto !important;
    overflow-x: hidden;
}

.header-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

@media (max-width: 960px) {
    .header-section {
        flex-direction: column;
        align-items: flex-start;
        gap: 20px;
    }
}

.hover-row:hover {
    background-color: #f8fafc;
    cursor: pointer;
}

/* Đổi giá: dòng cảnh báo màu vàng */
.price-warning-row td {
    background-color: #fffbeb !important;
    border-bottom: 1px solid #fde68a !important;
}

.price-changed-cell td {
    background-color: #fffdf5 !important;
}

.discount-amount-row {
    color: #b91c1c !important;
}

.discount-amount-value {
    color: #b91c1c !important;
    font-weight: 800 !important;
    letter-spacing: 0.15px;
    text-shadow: 0 0 0 #b91c1c;
}

.customer-main-col,
.customer-contact-col {
    min-height: 140px;
}

.customer-info-row {
    flex-wrap: nowrap;
}

.contact-item {
    line-height: 1.05;
}

/* Horizontal Timeline Design */
.timeline-wrap {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    position: relative;
    padding: 10px 0;
}

.timeline-step {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;
    z-index: 1;
}

.node-section {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 44px;
    margin-bottom: 12px;
    position: relative;
    z-index: 2;
}

/* Timeline Connecting Line */
.node-section::before,
.node-section::after {
    content: '';
    position: absolute;
    top: 50%;
    height: 3px;
    background: #e2e8f0;
    transform: translateY(-50%);
    z-index: 1;
    transition: background 0.4s ease;
}

.node-section::before {
    left: 0;
    right: 50%;
}

.node-section::after {
    left: 50%;
    right: 0;
}

.timeline-step:first-child .node-section::before,
.timeline-step:last-child .node-section::after {
    display: none;
}

/* Active/Done States for Line */
.timeline-step.done .node-section::before,
.timeline-step.done .node-section::after,
.timeline-step.active .node-section::before {
    background: #1e257c;
}

.node {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #ffffff !important;
    color: #94a3b8 !important;
    z-index: 5;
    border: 2px solid #e2e8f0 !important;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.timeline-step.done .node {
    background: #1e257c !important;
    color: #ffffff !important;
    border-color: #1e257c !important;
}

.timeline-step.active .node {
    border-color: #1e257c !important;
    color: #1e257c !important;
    box-shadow: 0 0 0 4px rgba(30, 37, 124, 0.15);
    animation: timeline-pulse 2s infinite;
}

@keyframes timeline-pulse {

    0%,
    100% {
        transform: scale(1);
        box-shadow: 0 0 0 0 rgba(30, 37, 124, 0.4);
    }

    70% {
        transform: scale(1.05);
        box-shadow: 0 0 0 12px rgba(30, 37, 124, 0);
    }
}

/* Timeline Animations */
.timeline-anim-enter-active {
    transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.timeline-anim-enter-from {
    opacity: 0;
    transform: scale(0.8) translateY(20px);
}

.timeline-anim-move {
    transition: transform 0.5s ease;
}

/* Vertical History Timeline Overrides */
:deep(.custom-history-timeline .v-timeline-item__body) {
    flex: 1 1 auto !important;
    width: 100% !important;
    padding: 0 !important;
}

/* Fix timeline dot alignment */
:deep(.custom-history-timeline .v-timeline-divider) {
    padding-top: 0 !important;
    padding-bottom: 0 !important;
    align-self: center !important;
}

:deep(.custom-history-timeline .v-timeline-item) {
    margin-bottom: 4px !important;
}

:deep(.custom-history-timeline .v-timeline-item__body) {
    padding-top: 2px !important;
    padding-bottom: 2px !important;
}

/* Utilities */
.bg-success-light {
    background: #ecfdf5;
}

.bg-primary-light {
    background: #eff6ff;
}

.payment-item {
    border: 1.5px dashed #e2e8f0;
}

/* Đồng bộ kích thước chữ 13px và loại bỏ in đậm cho toàn màn hình chi tiết hóa đơn */
.screen-scroll,
.screen-scroll :deep(div:not(.v-icon)),
.screen-scroll :deep(span:not(.v-icon)),
.screen-scroll :deep(td),
.screen-scroll :deep(th),
.screen-scroll :deep(p) {
    font-size: 13px !important;
    font-weight: 400 !important;
    text-transform: none !important;
    /* Bỏ in hoa */
}

/* Khử viết hoa toàn bộ cho các phần tử con */
.screen-scroll :deep(*) {
    text-transform: none !important;
}

/* THAY ĐỔI: Đồng bộ tất cả các nút bấm (Buttons) về cỡ 13px và độ đậm 600 để rõ hành động */
.screen-scroll :deep(.v-btn),
.screen-scroll :deep(.v-btn span),
.screen-scroll :deep(.v-btn *),
.screen-scroll :deep(.v-btn__content) {
    font-size: 13px !important;
    font-weight: 600 !important;
}

/* Ngoại lệ: Giữ cỡ chữ 16px CHO DUY NHẤT CÁC TIÊU ĐỀ PHẦN */
.screen-scroll :deep(.card-title),
.screen-scroll :deep(.card-title span),
.screen-scroll :deep(h3),
.screen-scroll :deep(.v-card-title) {
    font-size: 16px !important;
    font-weight: 600 !important;
}

.summary-row {
    display: flex;
    justify-content: space-between;
}

.summary-row span:last-child {
    text-align: right;
}

.history-scroll-container {
    min-height: 200px;
    max-height: 600px;
    overflow-y: auto;
}

@media (max-width: 1264px) {
    .customer-info-row {
        flex-wrap: wrap;
    }

    .customer-main-col,
    .customer-contact-col {
        min-height: unset;
    }
}

.premium-card,
.premium-card-detail {
    border: 1px solid rgba(226, 232, 240, 0.8) !important;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03) !important;
}

.border-x-0 {
    border-left: none !important;
    border-right: none !important;
}

/* Full-width AdminTable override */
.full-width-admin-table :deep(.admin-table-container) {
    border: none !important;
    margin-bottom: 0 !important;
}

.full-width-admin-table :deep(.native-admin-table) {
    width: 100% !important;
    table-layout: auto !important;
    /* Cho phép bảng giãn hết cỡ */
}

.all-center-table :deep(.data-cell),
.all-center-table :deep(.header-cell),
.all-center-table :deep(th),
.all-center-table :deep(td) {
    text-align: center !important;
    height: auto !important;
    padding: 16px !important;
}

/* Style input text, placeholders and selections */
:deep(.compact-input) .v-field__input,
:deep(.compact-input) input,
:deep(.compact-input) input::placeholder,
:deep(.compact-input) .v-select__selection-text {
    font-size: 13px !important;
}

/* Style select dropdown popover option list */
:global(.product-select-menu .v-list-item-title),
:global(.product-select-menu .v-list-item) {
    font-size: 13px !important;
}
</style>
