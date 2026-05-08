<script setup>
import { PATH } from '@/router/routePaths';
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { dichVuHoaDon } from "@/services/admin/dichVuHoaDon";
import { dichVuNhanVien } from "@/services/admin/dichVuNhanVien";
import { useNotifications } from "@/services/notificationService";
import axios from 'axios';
import {
    ChevronLeftIcon, PrinterIcon, EditIcon, CalendarIcon,
    PackageIcon, UserIcon, MapPinIcon, CreditCardIcon, TruckIcon,
    CircleCheckIcon, CircleXIcon, CheckIcon, TrashIcon,
    PlusIcon, LayoutListIcon
} from "vue-tabler-icons";
import ProductPicker from "../banhang/components/ProductPicker.vue";
import AdminConfirm from "@/components/common/AdminConfirm.vue";
import AdminBreadcrumbs from "@/components/common/AdminBreadcrumbs.vue";
import { getOrderStatusMeta, getOrderStatusOrdinal } from "@/utils/orderStatus";

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loaded = ref(false);
const loading = ref(false);
const updatingStatus = ref(false);
const statusDialogOpen = ref(false);
const selectedStatus = ref("");
const pendingStatus = ref("");

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

// Editing State
const receptionistDialogOpen = ref(false);
const shippingDialogOpen = ref(false);
const customerDialogOpen = ref(false);
const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);
const loadingLoc = ref({ provinces: false, districts: false, wards: false });
const addressDetail = ref({
    tinh: null,
    huyen: null,
    xa: null,
    soNha: ''
});

const productsDialogOpen = ref(false);
const employees = ref([]);
const editInfoForm = ref({
    soDienThoaiNguoiNhan: "",
    diaChiNguoiNhan: "",
    ghiChu: "",
    idKhachHang: null,
    idNhanVien: null,
    // Add guest customer info fields
    tenKhachHang: "",
    email: "",
    soDienThoai: ""
});
const updatingInfo = ref(false);
const updatingItems = ref(false);

const order = ref({
    id: "",
    maHoaDon: "",
    tenKhachHang: "",
    email: "",
    soDienThoai: "",
    trangThai: 'CHO_XAC_NHAN',
    ngayTao: "",
    tongTien: 0,
    tongTienSauGiam: 0,
    phiVanChuyen: 0,
    loaiDon: "OFFLINE",
    diaChi: "",
    ghiChu: "",
    listsHoaDonChiTiet: [],
    listsLichSuHoaDon: [],
    listsGiaoDichThanhToan: []
});

const productColumns = [
    { key: "tenSanPham", label: "Sản phẩm", width: "300px" },
    { key: "soLuong", label: "SL", width: "80px" },
    { key: "donGia", label: "Đơn giá", width: "130px" },
    { key: "thanhTien", label: "Tổng cộng", width: "130px" },
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

const receptionistLabel = computed(() => {
    const nv = order.value?.nhanVien;
    return (
        nv?.ten ||
        nv?.maNhanVien ||
        order.value?.maNhanVien ||
        order.value?.maNV ||
        order.value?.tenNhanVien ||
        "Hệ thống"
    );
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

const previousStatusMeta = computed(() =>
    previousStatus.value === null ? null : getOrderStatusMeta(previousStatus.value)
);

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
const canUpdateStatus = computed(() => order.value && getOrderStatus() !== null && getOrderStatus() < 3);
const isOrderEditable = computed(() => order.value.trangThai === 'CHUA_XAC_NHAN' || order.value.trangThai === 'CHO_XAC_NHAN' || getOrderStatus() < 3);

const customerName = computed(() => order.value.tenKhachHang || 'Khách lẻ');
const orderTypeLabel = computed(() => order.value.loaiDon === 'TAI_QUAY' ? 'Nhận tại quầy' : 'Giao hàng tận nơi');

const orderDiscountAmount = computed(() => {
    const total = order.value.tongTien || 0;
    const final = order.value.tongTienSauGiam || total;
    return total - final;
});

const orderTotalAmount = computed(() => order.value.tongTienSauGiam || order.value.tongTien || 0);

const initialHistoryLog = computed(() => ({
    trangThaiMoi: order.value.trangThai,
    ghiChu: order.value.ghiChu || 'Đơn hàng được ghi nhận vào hệ thống',
    nguoiThucHien: order.value.nguoiTao || 'SYSTEM',
    ngayTao: order.value.ngayTao
}));

// --- Navigation ---
const openEditCustomer = () => {
    editInfoForm.value = {
        tenKhachHang: order.value.tenKhachHang,
        email: order.value.email,
        soDienThoai: order.value.soDienThoai
    };
    customerDialogOpen.value = true;
};

const openEditInfo = () => openEditShipping();

const goBack = () => router.push(PATH.HOA_DON);
const goToList = () => router.push(PATH.HOA_DON);

// --- Payment Helpers ---
const getPaymentItemClass = (loai) => loai === 'TIEN_MAT' ? 'bg-success-light' : 'bg-primary-light';
const getPaymentMethodIcon = (loai) => loai === 'TIEN_MAT' ? 'mdi-cash-multiple' : 'mdi-bank-outline';

// --- Quantity Actions ---
const incrementQuantity = (item) => updateItemQuantity(item, Number(item.soLuong) + 1);
const decrementQuantity = (item) => updateItemQuantity(item, Number(item.soLuong) - 1);



const timelineSteps = computed(() => {
    const status = getOrderStatus();

    // Core flow steps
    const coreSteps = [
        { key: 0, label: "Chưa xác nhận", icon: PackageIcon, note: "Đơn hàng mới tạo" },
        { key: 1, label: "Chờ xác nhận", icon: CalendarIcon, note: "Đơn hàng chờ xác nhận" },
        { key: 2, label: "Đã xác nhận", icon: CircleCheckIcon, note: "Đơn hàng đã được xác nhận" },
        { key: 3, label: "Đang giao", icon: TruckIcon, note: "Đơn hàng đang được giao" },
        { key: 4, label: "Hoàn thành", icon: CheckIcon, note: "Đơn hàng đã hoàn thành" }
    ];

    // Exception steps
    const exceptionSteps = [
        { key: 5, label: "Đã hủy", icon: CircleXIcon, note: "Đơn hàng bị hủy" },
        { key: 6, label: "Hoàn đơn", icon: CircleXIcon, note: "Đơn hàng đã hoàn trả" }
    ];

    let steps = [...coreSteps];

    // If the current status is an exception, replace or append it
    if (status === 5 || status === 6) {
        const exc = exceptionSteps.find(s => s.key === status);
        if (exc) steps.push(exc);
    }

    const tsMap = getStatusTimestampMap.value;
    const currentIndex = status === null ? -1 : steps.findIndex(s => s.key === status);

    return steps.slice(0, currentIndex + 1).map((step, index) => {
        let state = "pending";

        if (status === 5 || status === 6) {
            if (index < currentIndex) state = "done";
            else if (index === currentIndex) state = "active";
            else state = "disabled";
        } else {
            if (index < currentIndex) state = "done";
            else if (index === currentIndex) state = "active";
            else state = "pending";
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

    confirmDialog.value = {
        show: true,
        title: 'Cập nhật trạng thái',
        message: `Xác nhận chuyển đơn hàng sang trạng thái [${getStatusLabel(targetStatus)}]?`,
        color: 'primary',
        showInput: true,
        inputLabel: 'Ghi chú cập nhật',
        inputRequired: targetStatus === 4, // Yêu cầu ghi chú khi hủy đơn
        action: async (note) => {
            confirmDialog.value.loading = true;
            try {
                await dichVuHoaDon.capNhatTrangThaiHoaDon(order.value.id, targetStatus, note);
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

const openEditReceptionist = () => {
    editInfoForm.value = {
        idNhanVien: order.value.nhanVien?.id
    };
    receptionistDialogOpen.value = true;
};

const fetchProvinces = async () => {
    if (provinces.value.length > 0) return;
    loadingLoc.value.provinces = true;
    try {
        const res = await axios.get('https://provinces.open-api.vn/api/p/');
        provinces.value = res.data;
    } catch (e) {
        console.error('Error loading provinces:', e);
    } finally {
        loadingLoc.value.provinces = false;
    }
};

const fetchDistricts = async (provinceCode) => {
    if (!provinceCode) return;
    loadingLoc.value.districts = true;
    try {
        const res = await axios.get(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`);
        districts.value = res.data.districts;
        wards.value = [];
        addressDetail.value.huyen = null;
        addressDetail.value.xa = null;
    } catch (e) {
        console.error('Error loading districts:', e);
    } finally {
        loadingLoc.value.districts = false;
    }
};

const fetchWards = async (districtCode) => {
    if (!districtCode) return;
    loadingLoc.value.wards = true;
    try {
        const res = await axios.get(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`);
        wards.value = res.data.wards;
        addressDetail.value.xa = null;
    } catch (e) {
        console.error('Error loading wards:', e);
    } finally {
        loadingLoc.value.wards = false;
    }
};

const updateFullAddress = () => {
    const p = provinces.value.find(x => x.code === addressDetail.value.tinh)?.name || '';
    const d = districts.value.find(x => x.code === addressDetail.value.huyen)?.name || '';
    const w = wards.value.find(x => x.code === addressDetail.value.xa)?.name || '';
    const detail = addressDetail.value.soNha || '';

    const parts = [detail, w, d, p].filter(x => x !== '');
    editInfoForm.value.diaChiNguoiNhan = parts.join(', ');
};

const openEditShipping = () => {
    editInfoForm.value = {
        soDienThoaiNguoiNhan: order.value.soDienThoaiNguoiNhan || order.value.soDienThoai,
        diaChiNguoiNhan: order.value.diaChiNguoiNhan || order.value.diaChi,
        ghiChu: order.value.ghiChu
    };

    // Attempt to parse existing address if possible
    // For now, just reset and let user re-select for accuracy
    addressDetail.value = {
        tinh: null,
        huyen: null,
        xa: null,
        soNha: order.value.diaChiNguoiNhan || ''
    };

    fetchProvinces();
    shippingDialogOpen.value = true;
};

const printInvoice = async () => {
    try {
        addNotification({ title: 'Đang chuẩn bị', subtitle: 'Đang tạo bản in hóa đơn...', color: 'info' });
        const html = await dichVuHoaDon.inHoaDon(order.value.id);
        
        const printWindow = window.open('', '_blank', 'width=900,height=1000');
        if (!printWindow) {
            addNotification({ title: 'Lỗi', subtitle: 'Trình duyệt đã chặn cửa sổ bật lên. Vui lòng cho phép popup để in.', color: 'warning' });
            return;
        }

        printWindow.document.write(html);
        printWindow.document.close();
        
        // Use onload to ensure all styles and fonts are ready before printing
        printWindow.onload = () => {
            setTimeout(() => {
                printWindow.print();
            }, 500);
        };
    } catch (error) {
        console.error('Print error:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải bản in từ máy chủ. Vui lòng kiểm tra lại kết nối.', color: 'error' });
    }
};

const saveInfoUpdate = async (type) => {
    updatingInfo.value = true;
    try {
        await dichVuHoaDon.capNhatThongTinHoaDon(order.value.id, editInfoForm.value);
        addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật thông tin hóa đơn', color: 'success' });
        await loadOrderDetail();
        if (type === 'receptionist') receptionistDialogOpen.value = false;
        else if (type === 'customer') customerDialogOpen.value = false;
        else shippingDialogOpen.value = false;
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Cập nhật thất bại', color: 'error' });
    } finally {
        updatingInfo.value = false;
    }
};

const handleAddProduct = async (product) => {
    updatingItems.value = true;
    try {
        const existing = (order.value?.listsHoaDonChiTiet || []).find(
            (x) => String(x?.idChiTietSanPham) === String(product?.id)
        );
        const nextQty = (Number(existing?.soLuong) || 0) + 1;
        await dichVuHoaDon.capNhatSanPhamHoaDon(order.value.id, {
            idChiTietSanPham: product.id,
            soLuong: nextQty
        });
        addNotification({ title: 'Thành công', subtitle: 'Đã thêm sản phẩm vào hóa đơn', color: 'success' });
        await loadOrderDetail();
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể thêm sản phẩm', color: 'error' });
    } finally {
        updatingItems.value = false;
    }
};

const updateItemQuantity = async (item, newQty) => {
    if (newQty < 1) return;
    updatingItems.value = true;
    try {
        await dichVuHoaDon.capNhatSanPhamHoaDon(order.value.id, {
            idChiTietSanPham: item.idChiTietSanPham,
            soLuong: newQty
        });
        addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật số lượng sản phẩm', color: 'success' });
        await loadOrderDetail();
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể cập nhật số lượng', color: 'error' });
    } finally {
        updatingItems.value = false;
    }
};

const removeItem = (idHdct) => {
    confirmDialog.value = {
        show: true,
        title: 'Xác nhận xóa',
        message: 'Bạn có chắc chắn muốn xóa sản phẩm này khỏi hóa đơn?',
        color: 'error',
        action: async () => {
            confirmDialog.value.loading = true;
            updatingItems.value = true;
            try {
                await dichVuHoaDon.xoaSanPhamHoaDon(order.value.id, idHdct);
                addNotification({ title: 'Thành công', subtitle: 'Đã xóa sản phẩm', color: 'success' });
                await loadOrderDetail();
                confirmDialog.value.show = false;
            } catch (error) {
                addNotification({ title: 'Lỗi', subtitle: 'Không thể xóa sản phẩm', color: 'error' });
            } finally {
                updatingItems.value = false;
                confirmDialog.value.loading = false;
            }
        }
    };
};

// --- Timeline Helpers ---
const getStepIcon = (step) => step.icon;

const loadEmployees = async () => {
    try {
        const data = await dichVuNhanVien.layNhanVienPhanTrang({ size: 1000, trangThai: 'DANG_HOAT_DONG' });
        employees.value = data.content || [];
    } catch (e) {
        console.error("Load employees failed", e);
    }
};

onMounted(() => {
    loadOrderDetail();
    loadEmployees();
});
</script>

<template>
    <v-container fluid class="pa-6 animate-fade-in screen-scroll">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý bán hàng', disabled: false, href: '#' },
            { title: 'Hóa đơn', disabled: false, to: PATH.HOA_DON },
            { title: `Chi tiết #${order?.maHoaDon || '...'}`, disabled: true }
        ]" />

        <!-- Header -->
        <v-card class="premium-card-detail mb-6 pa-6">
            <div class="header-section">
                <div class="header-left">
                    <div class="d-flex align-center header-left-row">
                        <v-btn icon variant="text" color="slate-600" class="mr-2 btn-back-header" @click="goBack">
                            <ChevronLeftIcon size="28" />
                        </v-btn>
                        <v-chip v-if="showStatusChip" :color="orderStatusTone" variant="flat"
                            class="font-bold px-4 status-chip text-white">
                            {{ orderStatusLabel }}
                        </v-chip>
                    </div>
                    <div v-if="loaded" class="text-caption text-slate-500 mt-1 d-flex align-center">
                        Nhân viên tiếp nhận: <span class="font-weight-bold text-slate-700 ml-1">{{ receptionistLabel }}</span>
                    </div>
                </div>
                <div class="header-right d-flex gap-3">
                    <v-btn variant="flat" color="white" class="btn-outline-primary" @click="openEditReceptionist">
                        <template v-slot:prepend>
                            <UserIcon size="18" class="mr-1" />
                        </template>
                        Đổi nhân viên
                    </v-btn>
                    <v-btn variant="flat" color="white" class="btn-outline-primary" @click="printInvoice">
                        <template v-slot:prepend>
                            <PrinterIcon size="18" class="mr-1" />
                        </template>
                        In hóa đơn
                    </v-btn>
                    <v-btn variant="flat" color="white" class="btn-outline-primary" @click="goToList">
                        <template v-slot:prepend>
                            <LayoutListIcon size="18" class="mr-1" />
                        </template>
                        Danh sách
                    </v-btn>

                    <v-menu v-if="canUpdateStatus">
                        <template v-slot:activator="{ props }">
                            <v-btn color="primary" variant="flat" class="rounded-lg px-6 font-bold btn-white-text"
                                height="44" v-bind="props" :loading="updatingStatus">
                                <EditIcon size="18" class="mr-2" />
                                Cập nhật trạng thái
                            </v-btn>
                        </template>
                        <v-list class="rounded-lg shadow-xl">
                            <v-list-item v-if="getOrderStatus() === 0" @click="requestStatusUpdate(1)"
                                prepend-icon="mdi-progress-clock">
                                <v-list-item-title>Gửi chờ xác nhận</v-list-item-title>
                            </v-list-item>
                            <v-list-item v-if="getOrderStatus() === 1" @click="requestStatusUpdate(2)"
                                prepend-icon="mdi-check-circle-outline">
                                <v-list-item-title>Xác nhận đơn hàng</v-list-item-title>
                            </v-list-item>
                            <v-list-item v-if="getOrderStatus() === 2" @click="requestStatusUpdate(3)"
                                prepend-icon="mdi-truck-fast-outline">
                                <v-list-item-title>Bắt đầu giao hàng</v-list-item-title>
                            </v-list-item>
                            <v-list-item v-if="getOrderStatus() === 3" @click="requestStatusUpdate(4)"
                                prepend-icon="mdi-checkbox-marked-circle-outline">
                                <v-list-item-title>Hoàn thành đơn hàng</v-list-item-title>
                            </v-list-item>
                            <v-divider v-if="previousStatusMeta"></v-divider>
                            <v-list-item v-if="previousStatusMeta" @click="requestStatusUpdate(previousStatus)"
                                prepend-icon="mdi-undo-variant">
                                <v-list-item-title>Quay lại: {{ previousStatusMeta.text }}</v-list-item-title>
                            </v-list-item>
                            <v-divider></v-divider>
                            <v-list-item v-if="getOrderStatus() < 4" @click="requestStatusUpdate(5)" color="error"
                                prepend-icon="mdi-close">
                                <v-list-item-title class="text-error font-bold">Hủy đơn hàng</v-list-item-title>
                            </v-list-item>
                            <v-list-item v-if="getOrderStatus() === 4" @click="requestStatusUpdate(6)"
                                color="deep-purple" prepend-icon="mdi-cash-refund">
                                <v-list-item-title class="text-deep-purple font-bold">Hoàn đơn</v-list-item-title>
                            </v-list-item>
                        </v-list>
                    </v-menu>
                </div>
            </div>
        </v-card>

        <v-card class="premium-card-detail mb-6 pa-6 overflow-hidden premium-timeline">
            <div class="timeline-wrap">
                <transition-group name="timeline-anim">
                    <div v-for="(step, index) in timelineSteps" :key="step.key" class="timeline-step"
                        :class="[step.state, step.state === 'active' ? 'text-' + step.tone : 'text-slate-400']">
                        <div class="node-section">
                            <div class="node" :class="step.state">
                                <component :is="getStepIcon(step)" size="22" />
                            </div>
                        </div>
                        <div class="timeline-info">
                            <div class="label">{{ step.label }}</div>
                            <div class="note">{{ step.note }}</div>
                            <div v-if="step.timestamp" class="text-caption text-slate-400 mt-1">
                                {{ formatDate(step.timestamp) }}
                            </div>
                        </div>
                    </div>
                </transition-group>
            </div>
        </v-card>

        <v-row v-if="loaded">
            <!-- Left Column: Items & History -->
            <v-col cols="12" lg="8">
                <!-- Customer Info -->
                <v-card class="premium-card mb-6">
                    <div class="card-title pa-5 border-b d-flex align-center justify-space-between bg-slate-50">
                        <div class="d-flex align-center">
                            <UserIcon size="20" class="mr-3 text-primary" />
                            <span class="font-weight-bold text-slate-800">Thông tin khách hàng</span>
                        </div>
                        <v-tooltip text="Cập nhật thông tin khách hàng" location="top">
                            <template v-slot:activator="{ props }">
                                <v-btn v-if="isOrderEditable" icon variant="text" color="primary" size="small"
                                    v-bind="props" @click="openEditCustomer">
                                    <EditIcon size="20" />
                                </v-btn>
                            </template>
                        </v-tooltip>
                    </div>
                    <v-card-text class="pa-8">
                        <v-row align="start">
                            <!-- Image Column -->
                            <v-col cols="auto">
                                <v-avatar size="200" class="rounded-xl shadow-lg border-2 border-primary-lighten-4 bg-white">
                                    <v-img :src="customerAvatarUrl" cover></v-img>
                                </v-avatar>
                            </v-col>

                            <!-- Primary Info Column -->
                            <v-col cols="12" md="4" class="px-6">
                                <div class="d-flex flex-column h-100 justify-center">
                                    <div class="text-h4 font-weight-bold text-slate-900 mb-2">{{ customerName }}</div>
                                    <div class="d-flex align-center mb-4">
                                        <span class="text-primary font-weight-bold mr-4 pe-4">
                                            ID: {{ order.khachHang?.maKhachHang || 'GUEST' }}
                                        </span>
                                        <span class="text-deep-purple-accent-4 font-weight-bold">
                                            Hạng: Thành viên
                                        </span>
                                    </div>
                                    <div class="text-caption text-slate-400 font-weight-bold ">Ghi chú khách hàng</div>
                                    <div class="text-body-2 text-slate-600 mt-1 italic">
                                        {{ order.khachHang?.ghiChu || 'Không có ghi chú đặc biệt' }}
                                    </div>
                                </div>
                            </v-col>

                            <!-- Contact & Metadata Column -->
                            <v-col cols="12" md="4" class="ml-auto ps-8">
                                <div class="d-flex flex-column h-100 justify-center gap-4">
                                    <div class="contact-item">
                                        <div class="text-caption text-slate-400 font-weight-bold mb-1">Số điện thoại</div>
                                        <div class="text-h6 text-slate-800 d-flex align-center">
                                            <v-icon color="primary" class="mr-2" size="20">mdi-phone-check</v-icon>
                                            {{ order.soDienThoai || 'Chưa có' }}
                                        </div>
                                    </div>
                                    <div class="contact-item">
                                        <div class="text-caption text-slate-400 font-weight-bold mb-1">Email</div>
                                        <div class="text-h6 text-slate-800 d-flex align-center">
                                            <v-icon color="primary" class="mr-2" size="20">mdi-email-check</v-icon>
                                            {{ order.email || 'N/A' }}
                                        </div>
                                    </div>
                                    <div class="contact-item">
                                        <div class="text-caption text-slate-400 font-weight-bold mb-1">Ngày đăng ký</div>
                                        <div class="text-body-1 text-slate-600 d-flex align-center">
                                            <v-icon color="slate-400" class="mr-2" size="20">mdi-calendar-range</v-icon>
                                            {{ formatDate(order.khachHang?.ngayTao) }}
                                        </div>
                                    </div>
                                </div>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <!-- Products -->
                <v-card class="premium-card mb-6 overflow-hidden">
                    <div class="card-title pa-5 border-b d-flex align-center justify-space-between bg-slate-50">
                        <div class="d-flex align-center">
                            <PackageIcon size="20" class="mr-3 text-primary" />
                            <span class="font-weight-bold text-slate-800">Sản phẩm đã đặt</span>
                        </div>
                        <v-tooltip text="Thêm / sửa sản phẩm" location="top">
                            <template v-slot:activator="{ props }">
                                <v-btn v-if="canUpdateStatus" icon variant="tonal" color="primary" size="small"
                                    class="rounded-md" v-bind="props" @click="productsDialogOpen = true">
                                    <PlusIcon size="20" />
                                </v-btn>
                            </template>
                        </v-tooltip>
                    </div>
                    <div class="table-scroll-container" style="max-height: 480px; overflow-y: auto; overflow-x: hidden;">
                        <v-table class="premium-table" density="compact">
                            <thead>
                                <tr>
                                    <th class="text-center py-4" style="width: 220px;">Hình ảnh</th>
                                    <th class="text-center py-4">Thông tin sản phẩm</th>
                                    <th class="text-center py-4" style="width: 170px;">Số lượng</th>
                                    <th class="text-center py-4" style="width: 130px;">Đơn giá</th>
                                    <th class="text-center py-4" style="width: 130px;">Thành tiền</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="item in order.listsHoaDonChiTiet" :key="item.id" class="hover-row">
                                    <td class="text-center py-6">
                                        <v-avatar size="200" class="rounded-xl border bg-slate-50 shadow-md">
                                            <v-img :src="item.chiTietSanPham?.hinhAnh || 'https://upload.wikimedia.org/wikipedia/commons/1/14/No_Image_Available.jpg'" cover></v-img>
                                        </v-avatar>
                                    </td>
                                    <td class="py-6">
                                        <div class="text-left d-flex flex-column justify-center h-100 px-4">
                                            <div class="text-h5 font-weight-bold text-slate-900 mb-2">{{
                                                item.chiTietSanPham?.sanPham?.ten || 'N/A' }}</div>
                                            <div class="text-h6 text-primary font-weight-bold mb-4">Mã SKU: {{
                                                item.chiTietSanPham?.maChiTietSanPham || 'N/A' }}</div>
                                            
                                            <div class="d-flex align-center gap-6 mt-2 text-slate-600">
                                                <div class="d-flex align-center">
                                                    <span class="text-caption font-weight-bold mr-2" style="white-space: nowrap;">Màu sắc:</span>
                                                    <span class="font-weight-bold text-slate-900" style="white-space: nowrap;">{{ item.chiTietSanPham?.mauSac?.ten || '—' }}</span>
                                                </div>
                                                <div class="d-flex align-center border-s ps-6">
                                                    <span class="text-caption font-weight-bold mr-2" style="white-space: nowrap;">Kích thước:</span>
                                                    <span class="font-weight-bold text-slate-900" style="white-space: nowrap;">{{ item.chiTietSanPham?.kichThuoc?.ten || '—' }}</span>
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="text-center">
                                        {{ item.soLuong }}
                                    </td>
                                    <td class="text-center text-slate-700">{{ formatCurrency(item.donGia) }}</td>
                                    <td class="text-center font-weight-bold text-primary">{{
                                        formatCurrency(Number(item.soLuong) * Number(item.donGia))
                                        }}</td>
                                </tr>
                            </tbody>
                        </v-table>
                    </div>

                </v-card>

                <!-- Status History Logs -->
                <v-card class="premium-card mb-6">
                    <div class="card-title pa-5 border-b d-flex align-center bg-slate-50">
                        <CalendarIcon size="20" class="mr-3 text-primary" />
                        <span class="font-weight-bold text-slate-800">Lịch sử đơn hàng</span>
                    </div>
                    <v-card-text class="pa-0">
                        <!-- Table-style Headers for Timeline -->
                        <div
                            class="d-flex align-center w-100 px-6 py-3 bg-slate-800 border-b text-caption font-weight-bold text-white uppercase">
                            <div class="text-center" style="width: 48px;"></div> <!-- Space for timeline dots -->
                            <div class="text-center" style="width: 130px;">Trạng thái</div>
                            <div class="text-center flex-grow-1">Mô tả</div>
                            <div class="text-center" style="width: 160px;">Người thực hiện</div>
                            <div class="text-center" style="width: 160px;">Thời gian</div>
                        </div>

                        <div class="history-scroll-container pa-6">
                            <v-timeline side="end" density="compact" line-color="slate-200" class="custom-history-timeline">
                                <v-timeline-item v-for="(log, idx) in order.listsLichSuHoaDon" :key="idx"
                                    :dot-color="getStatusInfo(log.trangThaiMoi).color" size="small">
                                    <div class="d-flex align-center w-100 py-1">
                                        <!-- Column 1: Status -->
                                        <div style="width: 130px;" class="text-center">
                                            <v-chip :color="getStatusInfo(log.trangThaiMoi).color" size="small"
                                                variant="flat" class="font-weight-bold text-white">
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
                                        <div style="width: 160px;" class="text-center">
                                            <span class="text-caption font-weight-bold text-slate-600 uppercase">{{
                                                log.nguoiThucHien || 'SYSTEM' }}</span>
                                        </div>

                                        <!-- Column 4: Time -->
                                        <div style="width: 160px;" class="text-center">
                                            <span class="text-caption text-slate-400 font-weight-bold">
                                                {{ formatDate(log.ngayTao) }}
                                            </span>
                                        </div>
                                    </div>
                                </v-timeline-item>

                                <!-- Fallback for initialization -->
                                <v-timeline-item v-if="!order.listsLichSuHoaDon || order.listsLichSuHoaDon.length === 0"
                                    :dot-color="getStatusInfo(initialHistoryLog.trangThaiMoi).color" size="small">
                                    <div class="d-flex align-center w-100 py-1">
                                        <!-- Column 1: Status -->
                                        <div style="width: 130px;" class="text-center">
                                            <v-chip :color="getStatusInfo(initialHistoryLog.trangThaiMoi).color" size="small"
                                                variant="flat" class="font-weight-bold text-white">
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
                                        <div style="width: 160px;" class="text-center">
                                            <span class="text-caption font-weight-bold text-slate-600 uppercase">{{
                                                initialHistoryLog.nguoiThucHien }}</span>
                                        </div>

                                        <!-- Column 4: Time -->
                                        <div style="width: 160px;" class="text-center">
                                            <span class="text-caption text-slate-400 font-weight-bold">
                                                {{ formatDate(initialHistoryLog.ngayTao) }}
                                            </span>
                                        </div>
                                    </div>
                                </v-timeline-item>
                            </v-timeline>
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>

            <!-- Right Column: Customer & Shipping -->
            <v-col cols="12" lg="4">
                <!-- Order Summary -->
                <v-card class="premium-card mb-6">
                    <div class="card-title pa-5 border-b d-flex align-center bg-slate-50">
                        <CreditCardIcon size="20" class="mr-3 text-primary" />
                        <span class="font-weight-bold text-slate-800">Tổng thanh toán</span>
                    </div>
                    <div class="summary-section pa-6">
                        <div class="summary-grid">
                            <div class="summary-row mb-2">
                                <span class="text-slate-500 font-weight-medium">Tạm tính:</span>
                                <span class="text-body-1 font-weight-bold text-slate-800">{{
                                    formatCurrency(order.tongTien)
                                    }}</span>
                            </div>
                            <div class="summary-row mb-2 text-error">
                                <span class="font-weight-medium">Giảm giá:</span>
                                <span class="text-body-1 font-weight-bold">- {{ formatCurrency(Math.abs(orderDiscountAmount))
                                    }}</span>
                            </div>
                            <div class="summary-row mb-2">
                                <span class="text-slate-500 font-weight-medium">Phí vận chuyển:</span>
                                <span class="text-body-1 font-weight-bold text-slate-800">{{
                                    formatCurrency(order.phiVanChuyen || 0)
                                }}</span>
                            </div>
                            <v-divider class="my-3 border-opacity-25"></v-divider>
                            <div class="summary-row">
                                <span class="text-subtitle-1 font-weight-bold text-slate-800">Tổng cộng:</span>
                                <span class="text-h5 font-weight-bold text-primary">{{
                                    formatCurrency(orderTotalAmount) }}</span>
                            </div>
                        </div>
                    </div>
                </v-card>



                <v-card class="premium-card mb-6">
                    <div class="card-title pa-5 border-b d-flex align-center justify-space-between bg-slate-50">
                        <div class="d-flex align-center">
                            <TruckIcon size="20" class="mr-3 text-primary" />
                            <span class="font-weight-bold text-slate-800">Thông tin vận chuyển</span>
                        </div>
                        <v-tooltip text="Cập nhật thông tin vận chuyển" location="top">
                            <template v-slot:activator="{ props }">
                                <v-btn variant="tonal" color="primary" icon size="small" class="rounded-md"
                                    v-bind="props" @click="openEditShipping">
                                    <EditIcon size="20" />
                                </v-btn>
                            </template>
                        </v-tooltip>
                    </div>
                    <v-card-text class="pa-6">
                        <div class="info-group mb-6">
                            <div class="text-caption text-slate-400 font-weight-bold mb-2">Loại đơn hàng
                            </div>
                            <v-chip variant="tonal" color="primary" class="font-weight-bold">
                                {{ orderTypeLabel }}
                            </v-chip>
                        </div>
                        <v-divider class="mb-6 border-opacity-10"></v-divider>
                        <div class="info-group mb-6">
                            <div class="text-caption text-slate-400 font-weight-bold mb-2">Địa chỉ nhận
                            </div>
                            <div class="text-body-1 text-slate-700 d-flex align-start">
                                <MapPinIcon size="18" class="mr-2 text-error mt-1" />
                                <span>{{ order.diaChi || 'Khách nhận tại quầy' }}</span>
                            </div>
                        </div>
                        <v-divider class="mb-6 border-opacity-10"></v-divider>
                        <div class="info-group">
                            <div class="text-caption text-slate-400 font-weight-bold mb-2">Ghi chú</div>
                            <div class="text-body-2 text-slate-600 italic pa-3 bg-slate-50 rounded-lg">
                                {{ order.ghiChu || 'Không có ghi chú' }}
                            </div>
                        </div>
                    </v-card-text>
                </v-card>

                <!-- Payment Info -->
                <v-card class="premium-card">
                    <div class="card-title pa-5 border-b d-flex align-center bg-slate-50">
                        <CreditCardIcon size="20" class="mr-3 text-primary" />
                        <span class="font-weight-bold text-slate-800">Lịch sử thanh toán</span>
                    </div>
                    <v-card-text class="pa-6">
                        <div v-if="order.listsGiaoDichThanhToan?.length" class="payment-list">
                            <div v-for="pay in order.listsGiaoDichThanhToan" :key="pay.id"
                                class="payment-item mb-4 pa-4 rounded-md border-dashed">
                                <div class="d-flex justify-space-between align-center">
                                    <div class="d-flex align-center">
                                        <div class="icon-wrap mr-4" :class="getPaymentItemClass(pay.loaiGiaoDich)">
                                            <v-icon :color="pay.loaiGiaoDich === 'TIEN_MAT' ? 'success' : 'primary'">
                                                {{ getPaymentMethodIcon(pay.loaiGiaoDich) }}
                                            </v-icon>
                                        </div>
                                        <div>
                                            <div class="font-weight-bold text-slate-800">{{ pay.tenPhuongThuc }}</div>
                                            <div class="text-caption text-slate-500">Mã GD: {{ pay.maGiaoDichNgoai ||
                                                'Nội bộ'
                                            }}</div>
                                        </div>
                                    </div>
                                    <div class="text-right">
                                        <div class="text-h6 font-weight-bold text-primary">{{ formatCurrency(pay.soTien)
                                        }}
                                        </div>
                                        <div class="text-caption text-slate-400">{{ formatDate(pay.ngayTao) }}</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div v-else class="text-center py-8 text-slate-400">
                            Chưa có lịch sử giao dịch
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <div v-else-if="loading" class="d-flex flex-column align-center justify-center py-16">
            <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
            <div class="mt-4 text-slate-500">Đang tải thông tin hóa đơn...</div>
        </div>

        <!-- SHARED CONFIRM -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />

        <!-- Edit Receptionist Dialog -->
        <v-dialog v-model="receptionistDialogOpen" max-width="400" transition="dialog-bottom-transition">
            <v-card class="rounded-md">
                <v-card-title class="pa-5 border-b font-weight-bold">Thay đổi nhân viên tiếp nhận</v-card-title>
                <v-card-text class="pa-6">
                    <v-autocomplete v-model="editInfoForm.idNhanVien" :items="employees" item-title="ten"
                        item-value="id" label="Nhân viên tiếp nhận" placeholder="Chọn nhân viên" variant="outlined"
                        hide-details></v-autocomplete>
                </v-card-text>
                <v-card-actions class="pa-6 pt-0">
                    <v-btn variant="tonal" color="slate-500" class="rounded-md px-6"
                        @click="receptionistDialogOpen = false">Hủy</v-btn>
                    <v-spacer></v-spacer>
                    <v-btn color="primary" variant="flat" class="rounded-md px-8 text-white" :loading="updatingInfo"
                        @click="saveInfoUpdate('receptionist')">
                        <span class="text-white">Cập nhật</span>
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Edit Customer Dialog -->
        <v-dialog v-model="customerDialogOpen" max-width="500" transition="dialog-bottom-transition">
            <v-card class="rounded-xl overflow-hidden shadow-2xl">
                <v-card-title class="pa-0">
                    <div class="bg-primary pa-6 text-white d-flex align-center">
                        <v-icon size="32" class="mr-4" style="color: white !important;">mdi-account-edit-outline</v-icon>
                        <div>
                            <div class="text-h6 font-weight-bold" style="color: white !important;">Cập nhật thông tin khách hàng</div>
                        </div>
                    </div>
                </v-card-title>
                <v-card-text class="pa-8">
                    <v-row>
                        <v-col cols="12">
                            <div class="text-caption text-slate-400 font-weight-bold mb-2">Tên khách hàng</div>
                            <v-text-field v-model="editInfoForm.tenKhachHang" placeholder="Nhập tên khách hàng" variant="outlined" density="comfortable" hide-details class="mb-4"></v-text-field>
                        </v-col>
                        <v-col cols="12">
                            <div class="text-caption text-slate-400 font-weight-bold mb-2">Số điện thoại</div>
                            <v-text-field v-model="editInfoForm.soDienThoai" placeholder="Nhập số điện thoại" variant="outlined" density="comfortable" hide-details class="mb-4"></v-text-field>
                        </v-col>
                        <v-col cols="12">
                            <div class="text-caption text-slate-400 font-weight-bold mb-2">Email</div>
                            <v-text-field v-model="editInfoForm.email" placeholder="Nhập email" variant="outlined" density="comfortable" hide-details></v-text-field>
                        </v-col>
                    </v-row>
                </v-card-text>
                <v-divider></v-divider>
                <v-card-actions class="pa-6 bg-slate-50">
                    <v-btn variant="text" color="slate-500" class="rounded-lg px-6 font-weight-bold" @click="customerDialogOpen = false">Hủy</v-btn>
                    <v-spacer></v-spacer>
                    <v-btn color="primary" variant="flat" class="rounded-lg px-8 font-weight-bold text-white" :loading="updatingInfo" @click="saveInfoUpdate('customer')">Lưu thay đổi</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Edit Shipping Dialog -->
        <v-dialog v-model="shippingDialogOpen" max-width="600" transition="dialog-bottom-transition">
            <v-card class="rounded-xl overflow-hidden shadow-2xl">
                <v-card-title class="pa-0">
                    <div class="bg-primary pa-6 text-white d-flex align-center">
                        <TruckIcon size="32" class="mr-4" style="color: white" />
                        <div>
                            <div class="text-h6 font-weight-bold" style="color: white !important;">Cập nhật thông tin
                                vận chuyển
                            </div>
                        </div>
                    </div>
                </v-card-title>
                <v-card-text class="pa-8">
                    <!-- Recipient Section -->
                    <div class="pa-0 mb-4 d-flex align-center">
                        <v-icon size="20" class="mr-2 text-slate-900">mdi-account-outline</v-icon>
                        <span class="text-subtitle-2 font-weight-bold text-slate-900">Thông tin người nhận</span>
                    </div>

                    <v-row>
                        <v-col cols="12">
                            <v-text-field v-model="editInfoForm.soDienThoaiNguoiNhan"
                                placeholder="Nhập số điện thoại nhận hàng (0xxx xxx xxx)" variant="outlined"
                                prepend-inner-icon="mdi-phone-outline" color="primary" class="mb-2"
                                hide-details></v-text-field>
                        </v-col>
                    </v-row>

                    <!-- Address Section -->
                    <div class="pa-0 mt-6 mb-4 d-flex align-center">
                        <v-icon size="20" class="mr-2 text-slate-900">mdi-map-marker-outline</v-icon>
                        <span class="text-subtitle-2 font-weight-bold text-slate-900">Địa chỉ giao
                            hàng</span>
                    </div>

                    <v-row dense>
                        <v-col cols="12" md="4">
                            <v-autocomplete v-model="addressDetail.tinh" :items="provinces" item-title="name"
                                item-value="code" placeholder="Chọn Tỉnh/Thành" variant="outlined" density="comfortable"
                                :loading="loadingLoc.provinces" @update:model-value="fetchDistricts" hide-details
                                class="mb-3"></v-autocomplete>
                        </v-col>
                        <v-col cols="12" md="4">
                            <v-autocomplete v-model="addressDetail.huyen" :items="districts" item-title="name"
                                item-value="code" placeholder="Chọn Quận/Huyện" variant="outlined" density="comfortable"
                                :loading="loadingLoc.districts" :disabled="!addressDetail.tinh"
                                @update:model-value="fetchWards" hide-details class="mb-3"></v-autocomplete>
                        </v-col>
                        <v-col cols="12" md="4">
                            <v-autocomplete v-model="addressDetail.xa" :items="wards" item-title="name"
                                item-value="code" placeholder="Chọn Phường/Xã" variant="outlined" density="comfortable"
                                :loading="loadingLoc.wards" :disabled="!addressDetail.huyen"
                                @update:model-value="updateFullAddress" hide-details class="mb-3"></v-autocomplete>
                        </v-col>
                        <v-col cols="12">
                            <v-text-field v-model="addressDetail.soNha" placeholder="Nhập số nhà, tên đường..."
                                variant="outlined" density="comfortable" @input="updateFullAddress"
                                hide-details></v-text-field>
                        </v-col>
                    </v-row>

                    <!-- Notes Section -->
                    <div class="pa-0 mt-6 mb-4 d-flex align-center">
                        <v-icon size="20" class="mr-2 text-slate-900">mdi-note-text-outline</v-icon>
                        <span class="text-subtitle-2 font-weight-bold text-slate-900">Ghi chú vận
                            chuyển</span>
                    </div>

                    <v-textarea v-model="editInfoForm.ghiChu"
                        placeholder="Nhập ghi chú cho shipper (vị trí, thời gian giao hàng...)" variant="outlined"
                        rows="2" color="primary" hide-details></v-textarea>
                </v-card-text>

                <v-divider></v-divider>

                <v-card-actions class="pa-6 bg-slate-50">
                    <v-btn variant="text" color="slate-500" class="rounded-lg px-6 font-weight-bold"
                        @click="shippingDialogOpen = false">
                        Đóng
                    </v-btn>
                    <v-spacer></v-spacer>
                    <v-btn color="primary" variant="flat" class="rounded-lg px-8 font-weight-bold shadow-sm text-white"
                        :loading="updatingInfo" @click="saveInfoUpdate('shipping')">
                        <v-icon left class="mr-2 text-white">mdi-check</v-icon>
                        <span class="text-white">Lưu thay đổi</span>
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Edit Products Dialog -->
        <v-dialog v-model="productsDialogOpen" max-width="1500" transition="dialog-bottom-transition">
            <v-card class="rounded-md overflow-hidden">
                <v-card-title class="pa-5 border-b font-weight-bold d-flex align-center">
                    Quản lý danh sách sản phẩm
                </v-card-title>
                <v-card-text class="pa-0">
                    <v-row no-gutters>
                        <!-- Left Side: Search & Filter -->
                        <v-col cols="12" lg="8" class="border-e">
                            <div class="pa-6 bg-slate-50 h-100">
                                <ProductPicker :loading-external="updatingItems" @add-product="handleAddProduct" />
                            </div>
                        </v-col>

                        <!-- Right Side: Added Items -->
                        <v-col cols="12" lg="4" class="d-flex flex-column">
                            <div class="pa-5 border-b bg-white">
                                <div class="font-weight-bold text-slate-700 d-flex align-center">
                                    <LayoutListIcon size="18" class="mr-2 text-primary" />
                                    Sản phẩm đã chọn ({{ order.listsHoaDonChiTiet?.length || 0 }})
                                </div>
                            </div>
                            <div class="flex-grow-1 overflow-y-auto" style="max-height: 700px;">
                                <v-table class="items-table-compact">
                                    <thead>
                                        <tr>
                                            <th class="py-3 text-caption font-weight-bold">Sản phẩm</th>
                                            <th class="text-center py-3 text-caption font-weight-bold" style="white-space: nowrap;">Số lượng</th>
                                            <th class="text-right py-3 text-caption font-weight-bold">Xóa</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr v-for="item in order.listsHoaDonChiTiet" :key="item.id" class="hover-row">
                                            <td class="py-3">
                                                <div class="text-subtitle-2 font-weight-bold text-slate-800">{{
                                                    item.chiTietSanPham?.sanPham?.ten || 'N/A'
                                                }}</div>
                                                <div class="text-caption text-slate-500">
                                                    {{ item.chiTietSanPham?.mauSac?.ten || '—' }} | {{
                                                        item.chiTietSanPham?.kichThuoc?.ten || '—' }}
                                                </div>
                                                <div class="text-caption font-weight-bold text-primary mt-1">
                                                    {{ formatCurrency(item.donGia) }}
                                                </div>
                                            </td>
                                            <td class="text-center">
                                                <div
                                                    class="d-flex align-center justify-center bg-slate-50 rounded-lg pa-1">
                                                    <v-btn icon size="x-small" variant="text" density="comfortable"
                                                        :disabled="updatingItems || Number(item.soLuong) <= 1"
                                                        @click="decrementQuantity(item)">
                                                        <v-icon size="14">mdi-minus</v-icon>
                                                    </v-btn>
                                                    <span class="mx-2 font-weight-bold text-caption">{{ item.soLuong
                                                    }}</span>
                                                    <v-btn icon size="x-small" variant="text" density="comfortable"
                                                        :disabled="updatingItems" @click="incrementQuantity(item)">
                                                        <v-icon size="14">mdi-plus</v-icon>
                                                    </v-btn>
                                                </div>
                                            </td>
                                            <td class="text-right">
                                                <v-btn icon color="error" variant="text" size="x-small"
                                                    :disabled="updatingItems" @click="removeItem(item.id)">
                                                    <TrashIcon size="16" />
                                                </v-btn>
                                            </td>
                                        </tr>
                                        <tr v-if="!order.listsHoaDonChiTiet?.length">
                                            <td colspan="3" class="text-center py-10 text-slate-400">
                                                Chưa có sản phẩm nào
                                            </td>
                                        </tr>
                                    </tbody>
                                </v-table>
                            </div>
                            <div class="pa-5 border-t bg-slate-50">
                                <div class="d-flex justify-space-between align-center mb-1">
                                    <span class="text-caption text-slate-500">Tạm tính:</span>
                                    <span class="text-subtitle-2 font-weight-bold">{{ formatCurrency(order.tongTien)
                                    }}</span>
                                </div>
                                <div class="d-flex justify-space-between align-center">
                                    <span class="text-caption text-slate-500">Tổng cộng:</span>
                                    <span class="text-h6 font-weight-bold text-primary">{{
                                        formatCurrency(orderTotalAmount) }}</span>
                                </div>
                            </div>
                        </v-col>
                    </v-row>
                </v-card-text>
                <v-card-actions class="pa-6 border-t bg-slate-50">
                    <v-spacer></v-spacer>
                    <v-btn variant="outlined" color="error" class="px-8 rounded-md font-weight-bold mr-2"
                        @click="productsDialogOpen = false">Hủy</v-btn>
                    <v-btn color="primary" variant="flat" class="px-8 rounded-md font-weight-bold text-white"
                        @click="productsDialogOpen = false">Hoàn tất</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Confirmation Dialog -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" :show-input="confirmDialog.showInput"
            :input-label="confirmDialog.inputLabel" :input-required="confirmDialog.inputRequired"
            @confirm="note => confirmDialog.action(note)" />
    </v-container>
</template>

<style scoped>
/* Scoped styles removed in favor of global _admin-common.scss */
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

.header-left-row {
    flex-wrap: nowrap;
    gap: 10px;
}

.btn-white-text {
    color: #fff !important;
}

.btn-white-text :deep(svg) {
    color: #fff !important;
    stroke: #fff !important;
    fill: none !important;
}

.info-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 12px;
}

.info-row {
    display: grid;
    grid-template-columns: 140px 1fr;
    align-items: center;
    gap: 12px;
}

.info-label {
    font-size: 12px;
    font-weight: 800;
    text-transform: uppercase;
    color: #94a3b8;
    white-space: nowrap;
}

.info-value {
    font-size: 13px;
    font-weight: 600;
    color: #334155;
    text-align: right;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.timeline-wrap {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    position: relative;
    padding: 10px 0;
}

.timeline-wrap::before {
    display: none !important;
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

.premium-table :deep(table) {
    table-layout: fixed !important;
    width: 100% !important;
}

.product-icon {
    width: 44px;
    height: 44px;
    background: #eff6ff;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.summary-row {
    display: flex;
    justify-content: space-between;
    font-size: 0.95rem;
}

.payment-item {
    border: 1.5px dashed #e2e8f0;
}

.icon-wrap {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.bg-success-light {
    background: #ecfdf5;
}

.bg-primary-light {
    background: #eff6ff;
}

.timeline-step {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;
    transition: all 0.3s ease;
    z-index: 1;
}

.node-section::before,
.node-section::after {
    content: '';
    position: absolute;
    top: 50%;
    height: 6px;
    background: #10b981;
    transform: translateY(-50%);
    z-index: 1;
}

.node-section::before {
    left: 0;
    right: calc(50% + 32px); /* 50% + half of 44px + 10px gap */
}

.node-section::after {
    left: calc(50% + 32px);
    right: 0;
}

.timeline-step:first-child .node-section::before {
    display: none;
}

.timeline-step:last-child .node-section::after {
    display: none;
}

.timeline-step:only-child::before {
    display: none;
}

.timeline-step.pending,
.timeline-step.disabled {
    opacity: 1;
    filter: none;
}

.timeline-step.done .node,
.timeline-step.done .timeline-info {
    opacity: 0.8;
}

.timeline-step.active {
    opacity: 1;
}

.timeline-step.active .node {
    transform: scale(1.1);
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

.timeline-step.active .node {
    border-color: #3b82f6 !important; /* Blue for active */
    color: #3b82f6 !important;
    box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
    animation: timeline-pulse 2s infinite;
}

@keyframes timeline-pulse {
    0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4); }
    70% { transform: scale(1.05); box-shadow: 0 0 0 12px rgba(59, 130, 246, 0); }
    100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(59, 130, 246, 0); }
}

.timeline-step.done .node {
    background: #ffffff !important;
    color: #94a3b8 !important;
    border-color: #e2e8f0 !important;
}

.timeline-step.active .node {
    /* Relying on global styles and bg- classes for colors */
}

.timeline-step.done .node {
    background: #f8fafc;
    color: #94a3b8;
    border-color: #e2e8f0;
}

/* Removed old .line and .line.active-line classes as we use ::before now */

.line.left {
    left: 0;
}

.line.right {
    right: 0;
}

.timeline-info {
    text-align: center;
}

.timeline-info .label {
    font-weight: 700;
    font-size: 13px;
    color: #1e293b;
    margin-bottom: 4px;
}

.timeline-info .note {
    font-size: 11px;
    color: #64748b;
}

/* Timeline Animation */
.timeline-anim-enter-active,
.timeline-anim-leave-active {
    transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.timeline-anim-enter-from {
    opacity: 0;
    transform: scale(0.8) translateY(20px);
}

.timeline-anim-leave-to {
    opacity: 0;
    transform: scale(0.8) translateY(-20px);
}

.timeline-anim-move {
    transition: transform 0.5s ease;
}

/* Force timeline content to take full width and remove default padding */
:deep(.v-timeline-item__body) {
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
    margin-bottom: 8px !important;
}
</style>