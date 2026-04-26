<script setup>
import { PATH } from '@/router/routePaths';
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { dichVuHoaDon } from "@/services/admin/dichVuHoaDon";
import { dichVuNhanVien } from "@/services/admin/dichVuNhanVien";
import { useNotifications } from "@/services/notificationService";
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
const productsDialogOpen = ref(false);
const employees = ref([]);
const editInfoForm = ref({
    soDienThoaiNguoiNhan: "",
    diaChiNguoiNhan: "",
    ghiChu: "",
    idKhachHang: null,
    idNhanVien: null
});
const updatingInfo = ref(false);
const updatingItems = ref(false);

const order = ref({
    id: "",
    maHoaDon: "",
    tenKhachHang: "",
    email: "",
    soDienThoai: "",
    trangThai: 1,
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
    // Prefer logs if available
    const logs = Array.isArray(order.value?.listsLichSuHoaDon) ? order.value.listsLichSuHoaDon : [];
    for (const log of logs) {
        const ord = getOrderStatusOrdinal(log?.trangThaiMoi);
        if (ord === null) continue;
        if (!map[ord]) map[ord] = log?.ngayTao ?? null; // first time reached
    }

    // Always have "created" timestamp for first step
    if (!map[0]) map[0] = order.value?.ngayTao ?? null;
    return map;
});



const timelineSteps = computed(() => {
    const status = getOrderStatus();
    const steps = [
        { key: 0, label: "Tạo đơn hàng", icon: CircleCheckIcon, note: "Đơn hàng được tạo" },
        { key: 1, label: "Đang xử lý", icon: CheckIcon, note: "Đang chuẩn bị/duyệt đơn" },
        { key: 2, label: "Đã gửi hàng", icon: PackageIcon, note: "Đơn đã bàn giao vận chuyển" },
        { key: 3, label: "Đã giao", icon: TruckIcon, note: "Đơn đã giao thành công" }
    ];

    // Terminal branch
    if (status === 4) {
        steps.push({ key: 4, label: "Đã hủy", icon: CircleXIcon, note: "Đơn bị hủy" });
    } else if (status === 5) {
        steps.push({ key: 5, label: "Hoàn tiền", icon: CircleXIcon, note: "Đơn đã hoàn tiền" });
    } else {
        // Default end state is delivered (3). Nothing else.
    }

    const currentIndex = status === null ? -1 : steps.findIndex(s => s.key === status);
    
    // Chỉ hiển thị các bước từ khởi đầu cho đến trạng thái hiện tại của đơn hàng
    // Các trạng thái sau đó sẽ bị ẩn đi theo yêu cầu của người dùng
    const visibleSteps = currentIndex === -1 ? [] : steps.slice(0, currentIndex + 1);
    
    const tsMap = getStatusTimestampMap.value;

    return visibleSteps.map((step, index) => ({
        ...step,
        timestamp: tsMap?.[step.key] ?? null,
        state: index === currentIndex ? "active" : "done", // Tất cả các bước hiển thị đều là 'done' hoặc 'active'
        tone: getStatusTone(step.key)
    }));
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

const openEditShipping = () => {
    editInfoForm.value = {
        soDienThoaiNguoiNhan: order.value.soDienThoaiNguoiNhan || order.value.soDienThoai,
        diaChiNguoiNhan: order.value.diaChiNguoiNhan || order.value.diaChi,
        ghiChu: order.value.ghiChu
    };
    shippingDialogOpen.value = true;
};

const printInvoice = async () => {
    try {
        addNotification({ title: 'Đang chuẩn bị', subtitle: 'Đang tạo bản in hóa đơn...', color: 'info' });
        const html = await dichVuHoaDon.inHoaDon(order.value.id);
        const printWindow = window.open('', '_blank', 'width=800,height=900');
        printWindow.document.write(html);
        printWindow.document.close();
        setTimeout(() => {
            printWindow.print();
            // printWindow.close(); // Keep open if user wants to see it
        }, 500);
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tạo bản in', color: 'error' });
    }
};

const saveInfoUpdate = async (type) => {
    updatingInfo.value = true;
    try {
        await dichVuHoaDon.capNhatThongTinHoaDon(order.value.id, editInfoForm.value);
        addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật thông tin hóa đơn', color: 'success' });
        await loadOrderDetail();
        if (type === 'receptionist') receptionistDialogOpen.value = false;
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

const loadEmployees = async () => {
    try {
        const data = await dichVuNhanVien.layNhanVienPhanTrang({ size: 1000, trangThai: 1 });
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
                        <v-btn icon variant="text" color="slate-600" class="mr-2 btn-back-header"
                            @click="router.push(PATH.HOA_DON)">
                            <ChevronLeftIcon size="28" />
                        </v-btn>
                        <v-chip v-if="loaded && getOrderStatusMeta(order.trangThai)"
                            :color="getStatusTone(order.trangThai)" variant="flat" class="font-weight-bold px-4 status-chip">
                            {{ getStatusLabel(order.trangThai) }}
                        </v-chip>
                    </div>
                    <div v-if="loaded" class="text-caption text-slate-500 mt-1 d-flex align-center">
                        Nhân viên tiếp nhận: <span class="font-weight-medium text-slate-700 ml-1">{{ receptionistLabel }}</span>
                        <v-btn icon variant="text" size="x-small" color="primary" class="ml-1" @click="openEditReceptionist">
                            <EditIcon size="14" />
                        </v-btn>
                    </div>
                </div>
                <div class="header-right d-flex gap-3">
                    <v-btn variant="flat" color="white" class="btn-outline-primary" @click="printInvoice">
                        <template v-slot:prepend>
                            <PrinterIcon size="18" class="mr-1" />
                        </template>
                        In hóa đơn
                    </v-btn>
                    <v-btn variant="flat" color="white" class="btn-outline-primary" @click="router.push(PATH.HOA_DON)">
                        <template v-slot:prepend>
                            <LayoutListIcon size="18" class="mr-1" />
                        </template>
                        Danh sách
                    </v-btn>

                    <v-menu v-if="order && getOrderStatus() !== null && getOrderStatus() < 3">
                        <template v-slot:activator="{ props }">
                            <v-btn color="primary" variant="flat" class="rounded-lg px-6 font-weight-bold btn-white-text"
                                height="44" v-bind="props" :loading="updatingStatus">
                                <EditIcon size="18" class="mr-2" />
                                Cập nhật trạng thái
                            </v-btn>
                        </template>
                        <v-list class="rounded-lg shadow-xl">
                            <v-list-item v-if="getOrderStatus() === 0" @click="requestStatusUpdate(1)"
                                prepend-icon="mdi-progress-clock">
                                <v-list-item-title>Chuyển sang Đang xử lý</v-list-item-title>
                            </v-list-item>
                            <v-list-item v-if="getOrderStatus() === 1" @click="requestStatusUpdate(2)"
                                prepend-icon="mdi-truck-fast-outline">
                                <v-list-item-title>Chuyển sang Đã gửi hàng</v-list-item-title>
                            </v-list-item>
                            <v-list-item v-if="getOrderStatus() === 2" @click="requestStatusUpdate(3)"
                                prepend-icon="mdi-checkbox-marked-circle-outline">
                                <v-list-item-title>Chuyển sang Đã giao</v-list-item-title>
                            </v-list-item>
                            <v-divider v-if="previousStatusMeta"></v-divider>
                            <v-list-item v-if="previousStatusMeta" @click="requestStatusUpdate(previousStatus)"
                                prepend-icon="mdi-undo-variant">
                                <v-list-item-title>Quay lại: {{ previousStatusMeta.text }}</v-list-item-title>
                            </v-list-item>
                            <v-divider></v-divider>
                            <v-list-item v-if="getOrderStatus() !== 4 && getOrderStatus() !== 5"
                                @click="requestStatusUpdate(4)" color="error" prepend-icon="mdi-close">
                                <v-list-item-title class="text-error font-weight-bold">Hủy đơn hàng</v-list-item-title>
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
                        :class="[step.state, step.tone]">
                        <div class="node-section">
                            <div class="line left" v-if="index > 0" :class="{ active: true }"></div>
                            <div class="node" :class="[step.state, step.tone]">
                                <component :is="step.icon" size="22" />
                            </div>
                            <div class="line right" v-if="index < timelineSteps.length - 1"
                                :class="{ active: true }"></div>
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
                        <v-btn v-if="order.trangThai < 3" icon variant="text" color="primary" size="small"
                            @click="openEditInfo">
                            <EditIcon size="20" />
                        </v-btn>
                    </div>
                    <v-card-text class="pa-6">
                        <v-row>
                            <v-col cols="12" md="6">
                                <div class="d-flex align-center mb-6">
                                    <v-avatar color="primary" size="64" class="mr-4 rounded-md shadow-lg">
                                        <span class="text-h5 text-white font-weight-bold">{{ (order.tenKhachHang ||
                                            'K').charAt(0) }}</span>
                                    </v-avatar>
                                    <div>
                                        <div class="text-h6 font-weight-bold text-slate-900">{{ order.tenKhachHang ||
                                            'Khách lẻ'
                                            }}</div>
                                        <div class="text-caption text-slate-500 font-weight-medium">Mã KH: {{
                                            order.khachHang?.maKhachHang || 'N/A' }}</div>
                                        <div class="d-flex align-center text-slate-500 mt-1">
                                            <v-icon size="16" class="mr-2">mdi-phone-outline</v-icon>
                                            <span class="font-weight-medium">{{ order.soDienThoai || 'Chưa có SĐT' }}</span>
                                        </div>
                                    </div>
                                </div>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="info-grid">
                                    <div class="info-row">
                                        <span class="info-label">Email:</span>
                                        <span class="info-value">{{ order.email || 'N/A' }}</span>
                                    </div>
                                    <div class="info-row">
                                        <span class="info-label">Ngày đăng ký:</span>
                                        <span class="info-value">{{ formatDate(order.khachHang?.ngayTao) }}</span>
                                    </div>
                                    <div class="info-row">
                                        <span class="info-label">Hạng khách hàng:</span>
                                        <div class="info-value">
                                            <v-chip size="x-small" color="primary" variant="flat"
                                                class="font-weight-medium text-white">Thành viên</v-chip>
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
                        <v-btn v-if="getOrderStatus() !== null && getOrderStatus() < 3" variant="tonal" color="primary"
                            size="small" class="px-4 font-weight-medium rounded-md" @click="productsDialogOpen = true">
                            <v-icon size="16" class="mr-2">mdi-plus</v-icon>
                            Thêm / sửa sản phẩm
                        </v-btn>
                    </div>
                    <div class="table-scroll-container" style="max-height: 480px; overflow-y: auto;">
                        <v-table class="premium-table" density="compact">
                            <thead>
                                <tr>
                                    <th class="text-left py-4">Sản phẩm</th>
                                    <th class="text-center py-4">Số lượng</th>
                                    <th class="text-right py-4">Đơn giá</th>
                                    <th class="text-right py-4">Thành tiền</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="item in order.listsHoaDonChiTiet" :key="item.id" class="hover-row">
                                    <td class="py-4">
                                        <div class="d-flex align-center">
                                            <div class="product-icon mr-4">
                                                <v-icon size="24" color="primary">mdi-shoe-sneaker</v-icon>
                                            </div>
                                            <div>
                                                <div class="font-weight-medium text-slate-800">{{ item.tenSanPham }}</div>
                                                <div class="text-caption text-slate-500">Màu: {{ item.tenMauSac }} |
                                                    Size:
                                                    {{
                                                        item.tenKichThuoc }}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="text-center">
                                        <template v-if="getOrderStatus() !== null && getOrderStatus() < 3">
                                            <div class="d-flex align-center justify-center">
                                                <v-btn icon size="x-small" variant="tonal"
                                                    :disabled="updatingItems || Number(item.soLuong) <= 1"
                                                    @click="updateItemQuantity(item, Number(item.soLuong) - 1)">-</v-btn>
                                                <span class="mx-3 font-weight-bold">{{ item.soLuong }}</span>
                                                <v-btn icon size="x-small" variant="tonal" :disabled="updatingItems"
                                                    @click="updateItemQuantity(item, Number(item.soLuong) + 1)">+</v-btn>
                                            </div>
                                        </template>
                                        <template v-else>
                                            {{ item.soLuong }}
                                        </template>
                                    </td>
                                    <td class="text-right text-slate-700 font-weight-medium">{{ formatCurrency(item.donGia) }}</td>
                                    <td class="text-right font-weight-bold text-primary">{{
                                        formatCurrency(item.thanhTien)
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
                        <div class="history-scroll-container pa-6">
                            <v-timeline side="end" align="start" density="compact">
                                <v-timeline-item v-for="(log, idx) in order.listsLichSuHoaDon" :key="idx"
                                    :dot-color="getStatusInfo(log.trangThaiMoi).color" size="small">
                                    <div class="d-flex justify-space-between w-100">
                                        <div class="pr-10">
                                            <div class="font-weight-bold text-slate-800">{{
                                                getStatusInfo(log.trangThaiMoi).text
                                                }}</div>
                                            <div class="text-body-2 text-slate-600 mt-1">
                                                {{ log.ghiChu || 'Cập nhật trạng thái từ hệ thống quản trị' }}
                                            </div>
                                            <div class="text-caption text-primary mt-2 font-weight-bold">
                                                Người thực hiện: {{ log.nguoiThucHien || 'ADMIN' }}
                                            </div>
                                        </div>
                                        <div class="text-caption text-slate-400 font-weight-medium text-right"
                                            style="min-width: 140px;">
                                            {{ formatDate(log.ngayTao) }}
                                        </div>
                                    </div>
                                </v-timeline-item>
                                <v-timeline-item v-if="!order.listsLichSuHoaDon || order.listsLichSuHoaDon.length === 0"
                                    dot-color="success" size="small">
                                    <div class="d-flex justify-space-between w-100">
                                        <div>
                                            <div class="font-weight-bold text-slate-800">Khởi tạo đơn hàng</div>
                                            <div class="text-caption text-slate-500 mt-1">Đơn hàng được ghi nhận vào hệ
                                                thống
                                            </div>
                                        </div>
                                        <div class="text-caption text-slate-400 font-weight-medium text-right"
                                            style="min-width: 140px;">
                                            {{ formatDate(order.ngayTao) }}
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
                                <span class="text-body-1 font-weight-bold">- {{ formatCurrency(order.tongTien -
                                    (order.tongTienSauGiam || order.tongTien)) }}</span>
                            </div>
                            <div class="summary-row mb-2">
                                <span class="text-slate-500 font-weight-medium">Phí vận chuyển:</span>
                                <span class="text-body-1 font-weight-medium text-slate-800">{{
                                    formatCurrency(order.phiVanChuyen || 0)
                                }}</span>
                            </div>
                            <v-divider class="my-3 border-opacity-25"></v-divider>
                            <div class="summary-row">
                                <span class="text-subtitle-1 font-weight-bold text-slate-800">Tổng cộng:</span>
                                <span class="text-h5 font-weight-bold text-primary">{{
                                    formatCurrency(order.tongTienSauGiam ||
                                        order.tongTien) }}</span>
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
                        <v-btn v-if="order.trangThai < 3" icon variant="text" color="primary" size="small" @click="openEditShipping">
                            <EditIcon size="20" />
                        </v-btn>
                    </div>
                    <v-card-text class="pa-6">
                        <div class="info-group mb-6">
                            <div class="text-caption text-slate-400 font-weight-medium text-uppercase mb-2">Loại đơn hàng
                            </div>
                            <v-chip variant="tonal" color="primary" class="font-weight-medium">
                                {{ order.loaiDon === 'TAI_QUAY' ? 'Nhận tại quầy' : 'Giao hàng tận nơi' }}
                            </v-chip>
                        </div>
                        <v-divider class="mb-6 border-opacity-10"></v-divider>
                        <div class="info-group mb-6">
                            <div class="text-caption text-slate-400 font-weight-medium text-uppercase mb-2">Địa chỉ nhận
                            </div>
                            <div class="text-body-1 text-slate-700 d-flex align-start font-weight-medium">
                                <MapPinIcon size="18" class="mr-2 text-error mt-1" />
                                <span>{{ order.diaChi || 'Khách nhận tại quầy' }}</span>
                            </div>
                        </div>
                        <v-divider class="mb-6 border-opacity-10"></v-divider>
                        <div class="info-group">
                            <div class="text-caption text-slate-400 font-weight-medium text-uppercase mb-2">Ghi chú</div>
                            <div class="text-body-2 text-slate-600 italic pa-3 bg-slate-50 rounded-lg font-weight-medium">
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
                                        <div class="icon-wrap mr-4"
                                            :class="pay.loaiGiaoDich === 'TIEN_MAT' ? 'bg-success-light' : 'bg-primary-light'">
                                            <v-icon :color="pay.loaiGiaoDich === 'TIEN_MAT' ? 'success' : 'primary'">
                                                {{ pay.loaiGiaoDich === 'TIEN_MAT' ? 'mdi-cash-multiple' :
                                                    'mdi-bank-outline' }}
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
                                        <div class="text-caption text-slate-400 font-weight-medium">{{ formatDate(pay.ngayTao) }}</div>
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
                    <v-autocomplete
                        v-model="editInfoForm.idNhanVien"
                        :items="employees"
                        item-title="ten"
                        item-value="id"
                        label="Nhân viên tiếp nhận"
                        placeholder="Chọn nhân viên"
                        variant="outlined"
                        hide-details
                    ></v-autocomplete>
                </v-card-text>
                <v-card-actions class="pa-6 pt-0">
                    <v-btn variant="tonal" color="slate-500" class="rounded-md px-6" @click="receptionistDialogOpen = false">Hủy</v-btn>
                    <v-spacer></v-spacer>
                    <v-btn color="primary" variant="flat" class="rounded-md px-8" :loading="updatingInfo" @click="saveInfoUpdate('receptionist')">Cập nhật</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Edit Shipping Dialog -->
        <v-dialog v-model="shippingDialogOpen" max-width="500" transition="dialog-bottom-transition">
            <v-card class="rounded-md">
                <v-card-title class="pa-5 border-b font-weight-bold">Cập nhật thông tin vận chuyển</v-card-title>
                <v-card-text class="pa-6">
                    <v-text-field v-model="editInfoForm.soDienThoaiNguoiNhan" label="Số điện thoại"
                        placeholder="Nhập số điện thoại" variant="outlined" class="mb-4" hide-details></v-text-field>
                    <v-textarea v-model="editInfoForm.diaChiNguoiNhan" label="Địa chỉ nhận hàng"
                        placeholder="Nhập địa chỉ" variant="outlined" rows="3" class="mb-4" hide-details></v-textarea>
                    <v-textarea v-model="editInfoForm.ghiChu" label="Ghi chú" placeholder="Ghi chú về đơn hàng"
                        variant="outlined" rows="2" hide-details></v-textarea>
                </v-card-text>
                <v-card-actions class="pa-6 pt-0">
                    <v-btn variant="tonal" color="slate-500" class="rounded-md px-6" @click="shippingDialogOpen = false">Hủy</v-btn>
                    <v-spacer></v-spacer>
                    <v-btn color="primary" variant="flat" class="rounded-md px-8" :loading="updatingInfo" @click="saveInfoUpdate('shipping')">Lưu thay đổi</v-btn>
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
                                            <th class="text-center py-3 text-caption font-weight-bold">SL</th>
                                            <th class="text-right py-3 text-caption font-weight-bold">Xóa</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr v-for="item in order.listsHoaDonChiTiet" :key="item.id" class="hover-row">
                                            <td class="py-3">
                                                <div class="text-subtitle-2 font-weight-bold text-slate-800">{{ item.tenSanPham }}</div>
                                                <div class="text-caption text-slate-500">
                                                    {{ item.tenMauSac }} | {{ item.tenKichThuoc }}
                                                </div>
                                                <div class="text-caption font-weight-bold text-primary mt-1">
                                                    {{ formatCurrency(item.donGia) }}
                                                </div>
                                            </td>
                                            <td class="text-center">
                                                <div class="d-flex align-center justify-center bg-slate-50 rounded-lg pa-1">
                                                    <v-btn icon size="x-small" variant="text" density="comfortable"
                                                        :disabled="updatingItems || Number(item.soLuong) <= 1"
                                                        @click="updateItemQuantity(item, Number(item.soLuong) - 1)">
                                                        <v-icon size="14">mdi-minus</v-icon>
                                                    </v-btn>
                                                    <span class="mx-2 font-weight-bold text-caption">{{ item.soLuong }}</span>
                                                    <v-btn icon size="x-small" variant="text" density="comfortable" :disabled="updatingItems"
                                                        @click="updateItemQuantity(item, Number(item.soLuong) + 1)">
                                                        <v-icon size="14">mdi-plus</v-icon>
                                                    </v-btn>
                                                </div>
                                            </td>
                                            <td class="text-right">
                                                <v-btn icon color="error" variant="text" size="x-small" :disabled="updatingItems"
                                                    @click="removeItem(item.id)">
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
                                    <span class="text-subtitle-2 font-weight-bold">{{ formatCurrency(order.tongTien) }}</span>
                                </div>
                                <div class="d-flex justify-space-between align-center">
                                    <span class="text-caption text-slate-500">Tổng cộng:</span>
                                    <span class="text-h6 font-weight-bold text-primary">{{ formatCurrency(order.tongTienSauGiam || order.tongTien) }}</span>
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
        <AdminConfirm 
            v-model:show="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :color="confirmDialog.color"
            :loading="confirmDialog.loading"
            :show-input="confirmDialog.showInput"
            :input-label="confirmDialog.inputLabel"
            :input-required="confirmDialog.inputRequired"
            @confirm="note => confirmDialog.action(note)"
        />
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
    color: #000 !important;
    stroke: #000 !important;
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
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 10px;
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
</style>
