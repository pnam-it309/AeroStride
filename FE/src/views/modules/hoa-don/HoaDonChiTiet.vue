<script setup>
import { PATH } from '@/router/routePaths';
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { dichVuHoaDon } from "@/services/admin/dichVuHoaDon";
import { useNotifications } from "@/services/notificationService";
import {
    ChevronLeftIcon, PrinterIcon, EditIcon, CalendarIcon,
    PackageIcon, UserIcon, MapPinIcon, CreditCardIcon, TruckIcon,
    CircleCheckIcon, CircleXIcon, CheckIcon, TrashIcon,
    PlusIcon, LayoutListIcon
} from "vue-tabler-icons";
import AdminConfirm from "@/components/common/AdminConfirm.vue";
import AdminBreadcrumbs from "@/components/common/AdminBreadcrumbs.vue";
import AdminTable from "@/components/common/AdminTable.vue";
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
    { key: "hinhAnh", text: "Hình ảnh" },
    { key: "tenSanPham", text: "Tên sản phẩm" },
    { key: "maBienThe", text: "Mã biến thể" },
    { key: "mauSac", text: "Màu sắc" },
    { key: "kichThuoc", text: "Kích thước" },
    { key: "soLuong", text: "Số lượng" },
    { key: "donGia", text: "Đơn giá" },
    { key: "thanhTien", text: "Thành tiền" },
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
const canUpdateStatus = computed(() => order.value && getOrderStatus() !== null && getOrderStatus() < 4);
const isOrderEditable = computed(() => order.value.trangThai === 'CHO_XAC_NHAN' || getOrderStatus() < 2);

const customerName = computed(() => order.value.tenKhachHang || 'Khách lẻ');
const orderTypeLabel = computed(() => order.value.loaiDon === 'TAI_QUAY' ? 'Nhận tại quầy' : 'Giao hàng tận nơi');

const orderDiscountAmount = computed(() => {
    const total = order.value.tongTien || 0;
    const final = order.value.tongTienSauGiam || total;
    return total - final;
});

const orderTotalAmount = computed(() => order.value.tongTienSauGiam || order.value.tongTien || 0);

const initialHistoryLog = computed(() => ({
    trangThaiMoi: 'CHO_XAC_NHAN', // Trạng thái mặc định khi tạo đơn
    ghiChu: order.value.ghiChu || 'Khởi tạo đơn hàng',
    nguoiThucHien: order.value.nguoiTao || 'SYSTEM',
    ngayTao: order.value.ngayTao
}));

const sortedHistoryLogs = computed(() => {
    const logs = Array.isArray(order.value?.listsLichSuHoaDon) ? [...order.value.listsLichSuHoaDon] : [];

    // Sort by date descending (Newest first)
    logs.sort((a, b) => new Date(b.ngayTao || 0) - new Date(a.ngayTao || 0));

    // If no logs yet, or if the first log (oldest) isn't the creation, 
    // we don't strictly need to prepend here because we have the fallback in template.
    // However, it's better to always have the creation log at the end of the list.

    return logs;
});

// --- Navigation ---
const goBack = () => router.push(PATH.HOA_DON);
const goToList = () => router.push(PATH.HOA_DON);

// --- Payment Helpers ---
const getPaymentItemClass = (loai) => loai === 'TIEN_MAT' ? 'bg-success-light' : 'bg-primary-light';
const getPaymentMethodIcon = (loai) => loai === 'TIEN_MAT' ? 'mdi-cash-multiple' : 'mdi-bank-outline';





const timelineSteps = computed(() => {
    const status = getOrderStatus();

    // Core flow steps
    const coreSteps = [
        { key: 0, label: "Chờ xác nhận", icon: CalendarIcon, note: "Đơn hàng mới tạo" },
        { key: 1, label: "Đã xác nhận", icon: CircleCheckIcon, note: "Đơn hàng đã được xác nhận" },
        { key: 2, label: "Chờ giao", icon: PackageIcon, note: "Đơn hàng chờ giao" },
        { key: 3, label: "Đang giao", icon: TruckIcon, note: "Đơn hàng đang được giao" },
        { key: 4, label: "Hoàn thành", icon: CheckIcon, note: "Đơn hàng đã hoàn thành" }
    ];

    // Exception steps
    const exceptionSteps = [
        { key: 5, label: "Đã hủy", icon: CircleXIcon, note: "Đơn hàng bị hủy" },
        { key: 6, label: "Hoàn đơn", icon: CircleXIcon, note: "Đơn hàng đã hoàn trả" }
    ];

    let steps = [...coreSteps];
    const tsMap = getStatusTimestampMap.value;
    const statusOrdinal = status === null ? -1 : status;

    // Nếu trạng thái hiện tại là Hủy hoặc Hoàn đơn, chúng ta sẽ hiển thị nó là bước cuối cùng hoặc thay thế bước tương ứng
    if (status === 5 || status === 6) {
        const exc = exceptionSteps.find(s => s.key === status);
        if (exc) {
            steps = [...coreSteps.slice(0, 4), exc]; // Giữ 4 bước đầu, bước 5 là trạng thái đặc biệt
        }
    }

    const currentActiveIndex = steps.findIndex(s => s.key === status);

    return steps
        .filter((_, index) => index <= currentActiveIndex)
        .map((step, index) => {
            let state = "pending";

            if (status === 5 || status === 6) {
                if (index < currentActiveIndex) state = "done";
                else if (index === currentActiveIndex) state = "active";
                else state = "disabled";
            } else {
                if (index < currentActiveIndex) state = "done";
                else if (index === currentActiveIndex) state = "active";
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





// --- Timeline Helpers ---
const getStepIcon = (step) => step.icon;

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



onMounted(() => {
    loadOrderDetail();
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
                        <v-chip v-if="showStatusChip" variant="flat"
                            :class="['px-4 status-chip', getOrderStatusMeta(order.trangThai)?.chipClass]">
                            {{ orderStatusLabel }}
                        </v-chip>
                    </div>
                    <div v-if="loaded" class="text-body-2 text-slate-500 mt-1 d-flex align-center">
                        Mã hóa đơn: <span class="text-slate-700 ml-1">#{{ order.maHoaDon }}</span>
                    </div>
                </div>
                <div class="header-right d-flex gap-3">
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
                            <v-btn color="primary" variant="flat" class="rounded-lg px-6" height="44" v-bind="props"
                                :loading="updatingStatus">
                                <EditIcon size="18" class="mr-2" />
                                Cập nhật trạng thái
                            </v-btn>
                        </template>
                        <v-list class="rounded-lg shadow-xl">
                            <!-- Luồng chuyển tiếp thông thường -->
                            <v-list-item v-if="getOrderStatus() === 0" @click="requestStatusUpdate(1)"
                                prepend-icon="mdi-check-circle-outline">
                                <v-list-item-title>Xác nhận đơn hàng</v-list-item-title>
                            </v-list-item>
                            <v-list-item v-if="getOrderStatus() === 1" @click="requestStatusUpdate(2)"
                                prepend-icon="mdi-package-variant-closed">
                                <v-list-item-title>Gửi chờ giao</v-list-item-title>
                            </v-list-item>
                            <v-list-item v-if="getOrderStatus() === 2" @click="requestStatusUpdate(3)"
                                prepend-icon="mdi-truck-fast-outline">
                                <v-list-item-title>Bắt đầu giao hàng</v-list-item-title>
                            </v-list-item>
                            <!-- Nút Hoàn thành (Hiển thị cho tất cả các trạng thái chưa hoàn thành) -->
                            <v-divider v-if="getOrderStatus() < 4"></v-divider>
                            <v-list-item v-if="getOrderStatus() < 4" @click="requestStatusUpdate(4)" color="success"
                                prepend-icon="mdi-checkbox-marked-circle-outline">
                                <v-list-item-title class="font-weight-bold text-success">
                                    {{ getOrderStatus() === 3 ? 'Giao hàng thành công' : 'Hoàn thành nhanh đơn hàng' }}
                                </v-list-item-title>
                            </v-list-item>
                            <v-divider v-if="previousStatusMeta"></v-divider>
                            <v-list-item v-if="previousStatusMeta" @click="requestStatusUpdate(previousStatus)"
                                prepend-icon="mdi-undo-variant">
                                <v-list-item-title>Quay lại: {{ previousStatusMeta.text }}</v-list-item-title>
                            </v-list-item>
                            <v-divider></v-divider>
                            <v-list-item v-if="getOrderStatus() < 4" @click="requestStatusUpdate(5)" color="error"
                                prepend-icon="mdi-close">
                                <v-list-item-title class="text-error">Hủy đơn hàng</v-list-item-title>
                            </v-list-item>
                            <v-list-item v-if="getOrderStatus() === 4" @click="requestStatusUpdate(6)"
                                color="deep-purple" prepend-icon="mdi-cash-refund">
                                <v-list-item-title class="text-deep-purple">Hoàn đơn</v-list-item-title>
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
                            <div class="text-subtitle-2 mb-1">{{ step.label }}</div>
                            <div class="text-body-2 text-slate-500">{{ step.note }}</div>
                            <div v-if="step.timestamp" class="text-body-2 text-slate-400 mt-1">
                                {{ formatDate(step.timestamp) }}
                            </div>
                        </div>
                    </div>
                </transition-group>
            </div>
        </v-card>

        <v-row v-if="loaded" class="gx-4" style="align-items: stretch;">
            <!-- Left Column: Primary Content (8/12) -->
            <v-col cols="12" lg="8" class="d-flex flex-column">
                <!-- Customer Info -->
                <v-card class="premium-card mb-4">
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
                                    class="rounded-xl shadow-lg border-2 border-primary-lighten-4 bg-white">
                                    <v-img :src="customerAvatarUrl" cover></v-img>
                                </v-avatar>
                            </v-col>

                            <!-- Primary Info Column -->
                            <v-col cols="12" md="5" class="px-2 py-0">
                                <div class="d-flex flex-column h-100 justify-start customer-main-col">
                                    <div class="text-h6 text-slate-900 mb-1">{{ customerName }}</div>
                                    <div class="d-flex align-center mb-1">
                                        <span class="text-primary text-body-2 mr-3 pe-3">
                                            ID: {{ order.khachHang?.maKhachHang || 'GUEST' }}
                                        </span>
                                    </div>
                                    <!-- <div>
                                        <span class="text-deep-purple-accent-4">
                                            Hạng: Thành viên
                                        </span>
                                    </div> -->
                                    <div class="text-body-2 text-slate-500">Ghi chú khách hàng</div>
                                    <div class="text-body-2 text-slate-600 mt-1">
                                        {{ order.khachHang?.ghiChu || 'Không có ghi chú đặc biệt' }}
                                    </div>
                                </div>
                            </v-col>

                            <!-- Contact & Metadata Column -->
                            <v-col cols="12" md="4" class="ml-auto ps-2 py-0">
                                <div class="d-flex flex-column h-100 justify-start gap-2 customer-contact-col">
                                    <div class="contact-item">
                                        <div class="text-body-2 text-slate-500 mb-0">Số điện thoại</div>
                                        <div class="text-subtitle-1 text-slate-800 d-flex align-center">
                                            <v-icon color="primary" class="mr-2" size="20">mdi-phone-check</v-icon>
                                            {{ order.soDienThoai || 'Chưa có' }}
                                        </div>
                                    </div>
                                    <div class="contact-item">
                                        <div class="text-body-2 text-slate-500 mb-0">Email</div>
                                        <div class="text-subtitle-1 text-slate-800 d-flex align-center">
                                            <v-icon color="primary" class="mr-2" size="20">mdi-email-check</v-icon>
                                            {{ order.email || 'N/A' }}
                                        </div>
                                    </div>
                                    <div class="contact-item">
                                        <div class="text-body-2 text-slate-500 mb-0">Ngày đăng ký</div>
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

                <!-- Status History Logs (Expands to fill height) -->
                <v-card class="premium-card mb-0 d-flex flex-column flex-grow-1">
                    <div class="card-title pa-4 border-b d-flex align-center bg-slate-50">
                        <CalendarIcon size="20" class="mr-3 text-primary" />
                        <span class="text-slate-800">Lịch sử đơn hàng</span>
                    </div>
                    <v-card-text class="pa-0 d-flex flex-column flex-grow-1 overflow-hidden">
                        <!-- Table-style Headers for Timeline -->
                        <div
                            class="d-flex align-center w-100 px-4 py-2 bg-slate-800 border-b text-subtitle-2 text-white uppercase">
                            <div class="text-center" style="width: 42px;"></div> <!-- Space for timeline dots -->
                            <div class="text-center" style="width: 110px;">Trạng thái</div>
                            <div class="text-center flex-grow-1">Mô tả</div>
                            <div class="text-center" style="width: 150px;">Người thực hiện</div>
                            <div class="text-center" style="width: 180px;">Thời gian</div>
                        </div>

                        <div class="history-scroll-container pa-3 flex-grow-1">
                            <v-timeline side="end" density="compact" line-color="slate-200"
                                class="custom-history-timeline">
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
                                            <span class="text-body-2 text-slate-600 uppercase">{{
                                                log.nguoiThucHien || 'SYSTEM' }}</span>
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
                                            <span class="text-body-2 text-slate-600 uppercase">{{
                                                initialHistoryLog.nguoiThucHien }}</span>
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
            </v-col>


            <!-- Right Column: Summary, Payments & Shipping (4/12) -->
            <v-col cols="12" lg="4" class="d-flex flex-column">
                <!-- Order Summary -->
                <v-card class="premium-card mb-4">
                    <div class="card-title pa-3 border-b d-flex align-center bg-slate-50">
                        <CreditCardIcon size="20" class="mr-3 text-primary" />
                        <span class="text-slate-800">Tổng thanh toán</span>
                    </div>
                    <div class="summary-section pa-3">
                        <div class="summary-grid">
                            <div class="summary-row mb-2">
                                <span class="text-slate-500 font-weight-medium">Tạm tính:</span>
                                <span class="text-body-2 text-slate-800">{{
                                    formatCurrency(order.tongTien)
                                }}</span>
                            </div>
                            <div class="summary-row mb-2 text-error">
                                <span class="font-weight-medium">Giảm giá:</span>
                                <span class="text-body-2">- {{ formatCurrency(Math.abs(orderDiscountAmount))
                                }}</span>
                            </div>
                            <div class="summary-row mb-2">
                                <span class="text-slate-500 font-weight-medium">Phí vận chuyển:</span>
                                <span class="text-body-2 text-slate-800">{{
                                    formatCurrency(order.phiVanChuyen || 0)
                                    }}</span>
                            </div>
                            <v-divider class="my-2 border-opacity-25"></v-divider>
                            <div class="summary-row">
                                <span class="text-subtitle-2 text-slate-800">Tổng cộng:</span>
                                <span class="text-h6 text-primary">{{
                                    formatCurrency(orderTotalAmount) }}</span>
                            </div>
                        </div>
                    </div>
                </v-card>

                <!-- Payment History (Moved here to balance height) -->
                <v-card class="premium-card mb-4">
                    <div class="card-title pa-4 border-b d-flex align-center bg-slate-50">
                        <CreditCardIcon size="20" class="mr-3 text-primary" />
                        <span class="text-slate-800">Lịch sử thanh toán</span>
                    </div>
                    <v-card-text class="pa-3">
                        <div v-if="order.listsGiaoDichThanhToan?.length" class="payment-list">
                            <div v-for="pay in order.listsGiaoDichThanhToan" :key="pay.id"
                                class="payment-item mb-2 pa-3 rounded-md border-dashed">
                                <div class="d-flex justify-space-between align-center">
                                    <div class="d-flex align-center">
                                        <div class="icon-wrap mr-3" :class="getPaymentItemClass(pay.loaiGiaoDich)">
                                            <v-icon :color="pay.loaiGiaoDich === 'TIEN_MAT' ? 'success' : 'primary'">
                                                {{ getPaymentMethodIcon(pay.loaiGiaoDich) }}
                                            </v-icon>
                                        </div>
                                        <div>
                                            <div class="text-slate-800 text-body-2 font-weight-medium">{{
                                                pay.tenPhuongThuc }}
                                            </div>
                                            <div class="text-body-2 text-slate-500">Mã GD: {{ pay.maGiaoDichNgoai ||
                                                'Nội bộ'
                                                }}</div>
                                        </div>
                                    </div>
                                    <div class="text-right">
                                        <div class="text-h6  text-primary">{{ formatCurrency(pay.soTien)
                                            }}</div>
                                        <div class="text-body-2 text-slate-400">{{ formatDate(pay.ngayTao) }}</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div v-else class="text-center py-8 text-slate-400">
                            Chưa có lịch sử giao dịch
                        </div>
                    </v-card-text>
                </v-card>

                <!-- Shipping Info (Expands to fill height) -->
                <v-card class="premium-card mb-0 d-flex flex-column flex-grow-1">
                    <div class="card-title pa-3 border-b d-flex align-center justify-space-between bg-slate-50">
                        <div class="d-flex align-center">
                            <TruckIcon size="20" class="mr-3 text-primary" />
                            <span class="text-slate-800">Thông tin vận chuyển</span>
                        </div>
                    </div>
                    <v-card-text class="pa-3 flex-grow-1">
                        <div class="info-group mb-3">
                            <div class="text-body-2 text-slate-500 mb-2">Loại đơn hàng
                            </div>
                            <v-chip variant="tonal" color="primary">
                                {{ orderTypeLabel }}
                            </v-chip>
                        </div>
                        <v-divider class="mb-3 border-opacity-10"></v-divider>
                        <div class="info-group mb-3">
                            <div class="text-body-2 text-slate-500 mb-2">Địa chỉ nhận
                            </div>
                            <div class="text-body-2 text-slate-700 d-flex align-start">
                                <MapPinIcon size="18" class="mr-2 text-error mt-1" />
                                <span>{{ order.diaChi || 'Khách nhận tại quầy' }}</span>
                            </div>
                        </div>
                        <v-divider class="mb-3 border-opacity-10"></v-divider>
                        <div class="info-group">
                            <div class="text-body-2 text-slate-500 mb-2">Ghi chú</div>
                            <div class="text-body-2 text-slate-600 pa-2 bg-slate-50 rounded-lg">
                                {{ order.ghiChu || 'Không có ghi chú' }}
                            </div>
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <!-- Products Section (Full Width Bottom) -->
        <v-row v-if="loaded">
            <v-col cols="12">
                <AdminTable title="Sản phẩm đã đặt" :headers="productColumns" :items="order.listsHoaDonChiTiet"
                    :showAddButton="false">
                    <template #row="{ item }">
                        <tr class="hover-row">
                            <td class="text-center py-4">
                                <v-avatar size="150" class="rounded-lg border bg-slate-50 shadow-sm">
                                    <v-img
                                        :src="item.chiTietSanPham?.hinhAnh || 'https://upload.wikimedia.org/wikipedia/commons/1/14/No_Image_Available.jpg'"
                                        cover></v-img>
                                </v-avatar>
                            </td>
                            <td class="py-4 text-center">
                                <div class="text-slate-900 text-subtitle-1">{{
                                    item.chiTietSanPham?.sanPham?.ten || 'N/A' }}</div>
                            </td>
                            <td class="py-4 text-center">
                                <div class="text-primary">#{{ item.chiTietSanPham?.maChiTietSanPham
                                    || 'N/A' }}</div>
                            </td>
                            <td class="text-center py-4">
                                <span class="text-slate-600">
                                    {{ item.chiTietSanPham?.mauSac?.ten || '—' }}
                                </span>
                            </td>
                            <td class="text-center py-4">
                                <span class="text-slate-600">
                                    {{ item.chiTietSanPham?.kichThuoc?.ten || '—' }}
                                </span>
                            </td>
                            <td class="text-center py-4">
                                <span class="text-h6 text-slate-800">{{ item.soLuong }}</span>
                            </td>
                            <td class="text-center py-4 text-slate-700">
                                {{ formatCurrency(item.donGia) }}
                            </td>
                            <td class="text-center py-4 text-primary text-subtitle-1">
                                {{ formatCurrency(Number(item.soLuong) * Number(item.donGia)) }}
                            </td>
                        </tr>
                    </template>
                </AdminTable>
            </v-col>
        </v-row>

        <div v-else-if="loading" class="d-flex flex-column align-center justify-center py-16">
            <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
            <div class="mt-4 text-slate-500">Đang tải thông tin hóa đơn...</div>
        </div>
        <!-- Confirmation Dialog -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" :show-input="confirmDialog.showInput"
            :input-label="confirmDialog.inputLabel" :input-required="confirmDialog.inputRequired"
            @confirm="note => confirmDialog.action(note)" />
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

.status-chip {
    font-size: 14px !important;
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
</style>
