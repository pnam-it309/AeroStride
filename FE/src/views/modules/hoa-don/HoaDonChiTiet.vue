<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { dichVuHoaDon } from "@/services/admin/dichVuHoaDon";
import { useNotifications } from "@/services/notificationService";
import {
    ChevronLeftIcon, PrinterIcon, EditIcon, CalendarIcon,
    PackageIcon, UserIcon, MapPinIcon, CreditCardIcon, TruckIcon,
    CircleCheckIcon, CircleXIcon, CheckIcon, TrashIcon,
    PlusIcon
} from "vue-tabler-icons";
import ProductPicker from "../banhang/components/ProductPicker.vue";
import AdminConfirm from "@/components/common/AdminConfirm.vue";

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
    action: null,
    loading: false
});

// Editing State
const infoDialogOpen = ref(false);
const productsDialogOpen = ref(false);
const editInfoForm = ref({
    soDienThoaiNguoiNhan: "",
    diaChiNguoiNhan: "",
    ghiChu: "",
    idKhachHang: null
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

const getStatusInfo = (status) => {
    switch (status) {
        case 1:
            return { text: 'Chờ xác nhận', color: 'warning', icon: 'mdi-clock-outline' };
        case 2:
            return { text: 'Đã xác nhận', color: 'info', icon: 'mdi-check-circle-outline' };
        case 3:
            return { text: 'Đang giao', color: 'primary', icon: 'mdi-truck-delivery-outline' };
        case 4:
            return { text: 'Hoàn thành', color: 'success', icon: 'mdi-checkbox-marked-circle-outline' };
        case 5:
            return { text: 'Đã hủy', color: 'error', icon: 'mdi-cancel' };
        default:
            return { text: 'Không xác định', color: 'grey', icon: 'mdi-help-circle-outline' };
    }
};

const loadOrderDetail = async () => {
    loading.value = true;
    try {
        const data = await dichVuHoaDon.layChiTietHoaDon(route.params.id);
        order.value = data;
        loaded.value = true;
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải thông tin hóa đơn', color: 'error' });
    } finally {
        loading.value = false;
    }
};

const getStatusLabel = (status) => {
    const map = {
        1: "Chờ xác nhận",
        2: "Đã xác nhận",
        3: "Đang giao",
        4: "Hoàn thành",
        5: "Đã hủy",
    };
    return map[status] || "Không xác định";
};

const getStatusTone = (status) => {
    const map = {
        1: "yellow",
        2: "blue",
        3: "orange",
        4: "green",
        5: "red",
    };
    return map[status] || "gray";
};

const timelineSteps = computed(() => {
    const isOnline = order.value.loaiDon !== 'TAI_QUAY';
    const status = order.value.trangThai;
    
    const steps = [
        { key: 1, label: "Chờ xác nhận", icon: CircleCheckIcon, note: "Đơn hàng đang chờ xử lý" },
        { key: 2, label: "Đã xác nhận", icon: CheckIcon, note: "Đơn hàng đã được xác nhận" },
    ];

    if (isOnline) {
        steps.push({ key: 3, label: "Đang giao", icon: TruckIcon, note: "Đơn đang được vận chuyển" });
    }

    steps.push({ 
        key: status === 5 ? 5 : 4, 
        label: status === 5 ? "Đã hủy" : "Hoàn thành", 
        icon: status === 5 ? CircleXIcon : CircleCheckIcon,
        note: status === 5 ? "Đơn hàng đã dừng" : "Đơn hàng hoàn tất"
    });

    const currentIndex = steps.findIndex(s => s.key === status);
    
    return steps.map((step, index) => ({
        ...step,
        state: status === 5 && step.key === 5 ? "active" : 
               (status === 5 && index < steps.length - 1) ? "done" :
               (index === currentIndex ? "active" : index < currentIndex ? "done" : "pending"),
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
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                await dichVuHoaDon.capNhatTrangThaiHoaDon(order.value.id, targetStatus);
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

const openEditInfo = () => {
    editInfoForm.value = {
        soDienThoaiNguoiNhan: order.value.soDienThoai,
        diaChiNguoiNhan: order.value.diaChi,
        ghiChu: order.value.ghiChu,
        idKhachHang: order.value.khachHang?.id
    };
    infoDialogOpen.value = true;
};

const saveInfoUpdate = async () => {
    updatingInfo.value = true;
    try {
        await dichVuHoaDon.capNhatThongTinHoaDon(order.value.id, editInfoForm.value);
        addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật thông tin hóa đơn', color: 'success' });
        await loadOrderDetail();
        infoDialogOpen.value = false;
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Cập nhật thất bại', color: 'error' });
    } finally {
        updatingInfo.value = false;
    }
};

const handleAddProduct = async (product) => {
    updatingItems.value = true;
    try {
        await dichVuHoaDon.capNhatSanPhamHoaDon(order.value.id, {
            idChiTietSanPham: product.id,
            soLuong: 1
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

onMounted(loadOrderDetail);
</script>

<template>
    <v-container fluid class="pa-6 animate-fade-in min-h-screen">
        <!-- Header -->
        <v-card class="premium-card mb-6 pa-6">
            <div class="header-section">
                <div class="header-left">
                    <v-btn icon variant="text" color="slate-600" class="mr-4 rounded-lg" @click="router.back()" style="background-color: transparent !important;">
                        <ChevronLeftIcon size="24" />
                    </v-btn>
                    <div v-if="loaded">
                        <div class="d-flex align-center">
                            <h2 class="text-h4 font-weight-bold text-slate-900 mr-4">Hóa đơn #{{ order.maHoaDon }}</h2>
                            <v-chip :color="getStatusTone(order.trangThai)" variant="flat" class="font-weight-bold px-4">
                                {{ getStatusLabel(order.trangThai) }}
                            </v-chip>
                        </div>
                        <div class="d-flex align-center mt-1 text-slate-500">
                            <CalendarIcon size="16" class="mr-2" />
                            <span>Ngày tạo: {{ formatDate(order.ngayTao) }}</span>
                        </div>
                    </div>
                </div>
                <div class="header-right d-flex gap-3">
                    <v-btn variant="outlined" color="#2E4E8E" class="rounded-lg px-6 font-weight-bold" height="44">
                        <PrinterIcon size="18" class="mr-2" />
                        In hóa đơn
                    </v-btn>
                    
                    <v-menu v-if="order && order.trangThai < 4">
                        <template v-slot:activator="{ props }">
                            <v-btn color="primary" variant="flat" class="rounded-lg px-6 font-weight-bold" height="44" v-bind="props" :loading="updatingStatus">
                                <EditIcon size="18" class="mr-2" />
                                Cập nhật trạng thái
                            </v-btn>
                        </template>
                        <v-list class="rounded-lg shadow-xl">
                            <v-list-item v-if="order.trangThai === 1" @click="requestStatusUpdate(2)" prepend-icon="mdi-check">
                                <v-list-item-title>Xác nhận đơn hàng</v-list-item-title>
                            </v-list-item>
                            <v-list-item v-if="order.trangThai === 2" @click="requestStatusUpdate(3)" prepend-icon="mdi-truck">
                                <v-list-item-title>Giao hàng</v-list-item-title>
                            </v-list-item>
                            <v-list-item v-if="order.trangThai === 3" @click="requestStatusUpdate(4)" prepend-icon="mdi-check-all">
                                <v-list-item-title>Đã giao & Hoàn thành</v-list-item-title>
                            </v-list-item>
                            <v-divider></v-divider>
                            <v-list-item @click="requestStatusUpdate(5)" color="error" prepend-icon="mdi-close">
                                <v-list-item-title class="text-error">Hủy đơn hàng</v-list-item-title>
                            </v-list-item>
                        </v-list>
                    </v-menu>
                </div>
            </div>
        </v-card>

        <!-- Timeline Progress -->
        <v-card class="premium-card mb-6 pa-6 overflow-hidden">
            <div class="timeline-wrap">
                <div v-for="(step, index) in timelineSteps" :key="index" class="timeline-step" :class="step.state">
                    <div class="timeline-node-section">
                        <div class="line left" v-if="index > 0" :class="{ active: step.state !== 'pending' }"></div>
                        <div class="node" :class="[step.state, step.tone]">
                            <component :is="step.icon" size="22" />
                        </div>
                        <div class="line right" v-if="index < timelineSteps.length - 1" :class="{ active: timelineSteps[index+1]?.state !== 'pending' }"></div>
                    </div>
                    <div class="timeline-info mt-3">
                        <div class="font-weight-bold text-slate-900 text-body-1">{{ step.label }}</div>
                        <div class="text-caption text-slate-500">{{ step.note }}</div>
                    </div>
                </div>
            </div>
        </v-card>

        <v-row v-if="loaded">
            <!-- Left Column: Items & History -->
            <v-col cols="12" lg="8">
                <!-- Products -->
                <v-card class="premium-card mb-6 overflow-hidden">
                    <div class="card-title pa-5 border-b d-flex align-center justify-space-between bg-slate-50">
                        <div class="d-flex align-center">
                            <PackageIcon size="20" class="mr-3 text-primary" />
                            <span class="font-weight-bold text-slate-800">Sản phẩm đã đặt</span>
                        </div>
                        <v-btn
                            v-if="order.trangThai < 3"
                            variant="tonal"
                            color="primary"
                            size="small"
                            class="px-4 font-weight-bold rounded-lg"
                            @click="productsDialogOpen = true"
                        >
                            <PlusIcon size="16" class="mr-2" />
                            Thay đổi sản phẩm
                        </v-btn>
                    </div>
                    <v-table class="premium-table">
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
                                            <div class="font-weight-bold text-slate-800">{{ item.tenSanPham }}</div>
                                            <div class="text-caption text-slate-500">Màu: {{ item.tenMauSac }} | Size: {{ item.tenKichThuoc }}</div>
                                        </div>
                                    </div>
                                </td>
                                <td class="text-center">{{ item.soLuong }}</td>
                                <td class="text-right text-slate-700">{{ formatCurrency(item.donGia) }}</td>
                                <td class="text-right font-weight-bold text-primary">{{ formatCurrency(item.thanhTien) }}</td>
                            </tr>
                        </tbody>
                    </v-table>
                    
                    <div class="summary-section pa-8 bg-slate-50">
                        <div class="summary-grid">
                            <div class="summary-row mb-2">
                                <span class="text-slate-500">Tạm tính:</span>
                                <span class="font-weight-bold text-slate-700">{{ formatCurrency(order.tongTien) }}</span>
                            </div>
                            <div class="summary-row mb-2 text-error">
                                <span>Giảm giá:</span>
                                <span class="font-weight-bold">- {{ formatCurrency(order.tongTien - (order.tongTienSauGiam || order.tongTien)) }}</span>
                            </div>
                            <div class="summary-row mb-2">
                                <span class="text-slate-500">Phí vận chuyển:</span>
                                <span class="font-weight-bold text-slate-700">{{ formatCurrency(order.phiVanChuyen || 0) }}</span>
                            </div>
                            <v-divider class="my-4 border-opacity-25"></v-divider>
                            <div class="summary-row">
                                <span class="text-h6 font-weight-bold text-slate-800">Tổng cộng:</span>
                                <span class="text-h5 font-weight-bold text-primary">{{ formatCurrency(order.tongTienSauGiam || order.tongTien) }}</span>
                            </div>
                        </div>
                    </div>
                </v-card>

                <!-- Status History Logs -->
                <v-card class="premium-card mb-6">
                    <div class="card-title pa-5 border-b d-flex align-center bg-slate-50">
                        <CalendarIcon size="20" class="mr-3 text-primary" />
                        <span class="font-weight-bold text-slate-800">Lịch sử đơn hàng</span>
                    </div>
                    <v-card-text class="pa-6">
                        <v-timeline side="end" align="start" density="compact">
                            <v-timeline-item
                                v-for="(log, idx) in order.listsLichSuHoaDon"
                                :key="idx"
                                :dot-color="getStatusInfo(log.trangThaiMoi).color"
                                size="small"
                            >
                                <div class="d-flex justify-space-between">
                                    <div>
                                        <div class="font-weight-bold">{{ getStatusInfo(log.trangThaiMoi).text }}</div>
                                        <div class="text-body-2 text-slate-600">{{ log.ghiChu || 'Cập nhật trạng thái hệ thống' }}</div>
                                        <div class="text-caption text-primary mt-1">
                                            Người thực hiện: {{ log.nguoiThucHien || 'Hệ thống' }}
                                        </div>
                                    </div>
                                    <div class="text-caption text-slate-400">{{ formatDate(log.ngayTao) }}</div>
                                </div>
                            </v-timeline-item>
                            <v-timeline-item v-if="!order.listsLichSuHoaDon || order.listsLichSuHoaDon.length === 0" dot-color="success" size="small">
                                <div class="d-flex justify-space-between">
                                    <div>
                                        <div class="font-weight-bold">Khởi tạo đơn hàng</div>
                                        <div class="text-caption text-slate-500">Đơn hàng được ghi nhận vào hệ thống</div>
                                    </div>
                                    <div class="text-caption text-slate-400">{{ formatDate(order.ngayTao) }}</div>
                                </div>
                            </v-timeline-item>
                        </v-timeline>
                    </v-card-text>
                </v-card>
            </v-col>

            <!-- Right Column: Customer & Shipping -->
            <v-col cols="12" lg="4">
                <v-card class="premium-card mb-6">
                    <div class="card-title pa-5 border-b d-flex align-center justify-space-between bg-slate-50">
                        <div class="d-flex align-center">
                            <UserIcon size="20" class="mr-3 text-primary" />
                            <span class="font-weight-bold text-slate-800">Thông tin khách hàng</span>
                        </div>
                        <v-btn
                            v-if="order.trangThai < 3"
                            icon
                            variant="text"
                            color="primary"
                            size="small"
                            @click="openEditInfo"
                        >
                            <EditIcon size="20" />
                        </v-btn>
                    </div>
                    <v-card-text class="pa-6">
                        <div class="d-flex align-center mb-6">
                            <v-avatar color="primary" size="64" class="mr-4 rounded-2xl shadow-lg">
                                <span class="text-h5 text-white font-weight-bold">{{ (order.tenKhachHang || 'K').charAt(0) }}</span>
                            </v-avatar>
                            <div>
                                <div class="text-h6 font-weight-bold text-slate-900">{{ order.tenKhachHang || 'Khách lẻ' }}</div>
                                <div class="d-flex align-center text-slate-500 mt-1">
                                    <v-icon size="16" class="mr-2">mdi-phone-outline</v-icon>
                                    <span>{{ order.soDienThoai || 'Chưa có SĐT' }}</span>
                                </div>
                            </div>
                        </div>
                        <v-divider class="mb-6 border-opacity-10"></v-divider>
                        <div class="info-group mb-6">
                            <div class="text-caption text-slate-400 font-weight-bold text-uppercase mb-2">Email</div>
                            <div class="text-body-1 text-slate-700 font-weight-medium">{{ order.email || 'N/A' }}</div>
                        </div>
                    </v-card-text>
                </v-card>

                <v-card class="premium-card mb-6">
                    <div class="card-title pa-5 border-b d-flex align-center bg-slate-50">
                        <TruckIcon size="20" class="mr-3 text-primary" />
                        <span class="font-weight-bold text-slate-800">Thông tin vận chuyển</span>
                    </div>
                    <v-card-text class="pa-6">
                        <div class="info-group mb-6">
                            <div class="text-caption text-slate-400 font-weight-bold text-uppercase mb-2">Loại đơn hàng</div>
                            <v-chip variant="tonal" color="primary" class="font-weight-bold">
                                {{ order.loaiDon === 'TAI_QUAY' ? 'Nhận tại quầy' : 'Giao hàng tận nơi' }}
                            </v-chip>
                        </div>
                        <v-divider class="mb-6 border-opacity-10"></v-divider>
                        <div class="info-group mb-6">
                            <div class="text-caption text-slate-400 font-weight-bold text-uppercase mb-2">Địa chỉ nhận</div>
                            <div class="text-body-1 text-slate-700 d-flex align-start">
                                <MapPinIcon size="18" class="mr-2 text-error mt-1" />
                                <span>{{ order.diaChi || 'Khách nhận tại quầy' }}</span>
                            </div>
                        </div>
                        <v-divider class="mb-6 border-opacity-10"></v-divider>
                        <div class="info-group">
                            <div class="text-caption text-slate-400 font-weight-bold text-uppercase mb-2">Ghi chú</div>
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
                            <div v-for="pay in order.listsGiaoDichThanhToan" :key="pay.id" class="payment-item mb-4 pa-4 rounded-xl border-dashed">
                                <div class="d-flex justify-space-between align-center">
                                    <div class="d-flex align-center">
                                        <div class="icon-wrap mr-4" :class="pay.loaiGiaoDich === 'TIEN_MAT' ? 'bg-success-light' : 'bg-primary-light'">
                                            <v-icon :color="pay.loaiGiaoDich === 'TIEN_MAT' ? 'success' : 'primary'">
                                                {{ pay.loaiGiaoDich === 'TIEN_MAT' ? 'mdi-cash-multiple' : 'mdi-bank-outline' }}
                                            </v-icon>
                                        </div>
                                        <div>
                                            <div class="font-weight-bold text-slate-800">{{ pay.tenPhuongThuc }}</div>
                                            <div class="text-caption text-slate-500">Mã GD: {{ pay.maGiaoDichNgoai || 'Nội bộ' }}</div>
                                        </div>
                                    </div>
                                    <div class="text-right">
                                        <div class="text-h6 font-weight-bold text-primary">{{ formatCurrency(pay.soTien) }}</div>
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
        <AdminConfirm
            v-model:show="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :color="confirmDialog.color"
            :loading="confirmDialog.loading"
            @confirm="confirmDialog.action"
        />

        <!-- Edit Info Dialog -->
        <v-dialog v-model="infoDialogOpen" max-width="500">
            <v-card class="rounded-2xl">
                <v-card-title class="pa-5 border-b font-weight-bold">
                    Cập nhật thông tin nhận hàng
                </v-card-title>
                <v-card-text class="pa-6">
                    <v-text-field
                        v-model="editInfoForm.soDienThoaiNguoiNhan"
                        label="Số điện thoại"
                        placeholder="Nhập số điện thoại"
                        variant="outlined"
                        class="mb-4"
                    ></v-text-field>
                    <v-textarea
                        v-model="editInfoForm.diaChiNguoiNhan"
                        label="Địa chỉ"
                        placeholder="Nhập địa chỉnhận hàng"
                        variant="outlined"
                        rows="3"
                        class="mb-4"
                    ></v-textarea>
                    <v-textarea
                        v-model="editInfoForm.ghiChu"
                        label="Ghi chú"
                        placeholder="Ghi chú về đơn hàng"
                        variant="outlined"
                        rows="2"
                    ></v-textarea>
                </v-card-text>
                <v-card-actions class="pa-6 pt-0">
                    <v-btn variant="tonal" color="slate-500" class="rounded-xl px-6" @click="infoDialogOpen = false">Đóng</v-btn>
                    <v-spacer></v-spacer>
                    <v-btn color="primary" variant="flat" class="rounded-xl px-8" :loading="updatingInfo" @click="saveInfoUpdate">Lưu thay đổi</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Edit Products Dialog -->
        <v-dialog v-model="productsDialogOpen" max-width="1000">
            <v-card class="rounded-2xl overflow-hidden">
                <v-card-title class="pa-5 border-b font-weight-bold d-flex align-center">
                    Quản lý danh sách sản phẩm
                    <v-spacer></v-spacer>
                    <v-btn icon variant="text" @click="productsDialogOpen = false"><CircleXIcon /></v-btn>
                </v-card-title>
                <v-card-text class="pa-0">
                    <div class="pa-6 bg-slate-50">
                        <ProductPicker @add-product="handleAddProduct" />
                    </div>
                    
                    <v-divider></v-divider>
                    
                    <v-table class="items-table px-6">
                        <thead>
                            <tr>
                                <th class="py-4">Sản phẩm</th>
                                <th class="text-center py-4">Số lượng</th>
                                <th class="text-right py-4">Hành động</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="item in order.listsHoaDonChiTiet" :key="item.id">
                                <td class="py-4">
                                    <div class="font-weight-bold">{{ item.tenSanPham }}</div>
                                    <div class="text-caption text-slate-500">{{ item.tenMauSac }} | {{ item.tenKichThuoc }}</div>
                                </td>
                                <td class="text-center">
                                    <div class="d-flex align-center justify-center">
                                        <v-btn icon size="x-small" variant="tonal" @click="updateItemQuantity(item, item.soLuong - 1)">-</v-btn>
                                        <span class="mx-3 font-weight-bold">{{ item.soLuong }}</span>
                                        <v-btn icon size="x-small" variant="tonal" @click="updateItemQuantity(item, item.soLuong + 1)">+</v-btn>
                                    </div>
                                </td>
                                <td class="text-right">
                                    <v-btn icon color="error" variant="text" size="small" @click="removeItem(item.id)">
                                        <TrashIcon />
                                    </v-btn>
                                </td>
                            </tr>
                        </tbody>
                    </v-table>
                </v-card-text>
                <v-card-actions class="pa-6 border-t bg-slate-50">
                    <v-spacer></v-spacer>
                    <v-btn color="primary" variant="flat" class="px-8 rounded-xl font-weight-bold" @click="productsDialogOpen = false">Hoàn tất</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-container>
</template>

<style scoped>
.animate-fade-in { animation: fadeIn 0.5s ease-in; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.premium-card {
    border-radius: 20px !important;
    border: 1px solid #e2e8f0 !important;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05) !important;
    background: white;
}

.header-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.header-left { display: flex; align-items: center; }
.header-right { display: flex; align-items: center; }

@media (max-width: 960px) {
    .header-section { flex-direction: column; align-items: flex-start; gap: 20px; }
}

.timeline-wrap {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 10px;
    padding: 10px 0;
}

.timeline-step { display: flex; flex-direction: column; align-items: center; text-align: center; }
.timeline-node-section { width: 100%; display: flex; align-items: center; }
.line { flex: 1; height: 3px; background: #e5e7eb; border-radius: 999px; }
.line.active { background: #3b82f6; }

.node {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fff;
    border: 2px solid #e5e7eb;
    color: #9ca3af;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.node.active { border-color: #3b82f6; color: #3b82f6; background: #eff6ff; transform: scale(1.1); }
.node.done { border-color: #10b981; color: #10b981; background: #ecfdf5; }

.node.yellow.active { border-color: #fbbf24; color: #fbbf24; background: #fffbeb; }
.node.blue.active { border-color: #3b82f6; color: #3b82f6; background: #eff6ff; }
.node.orange.active { border-color: #f59e0b; color: #f59e0b; background: #fff7ed; }
.node.green.active { border-color: #10b981; color: #10b981; background: #ecfdf5; }
.node.red.active { border-color: #ef4444; color: #ef4444; background: #fef2f2; }

.premium-table :deep(th) {
    background: transparent !important;
    font-weight: 700 !important;
    text-transform: uppercase;
    font-size: 0.7rem;
    letter-spacing: 0.05em;
    color: #475569 !important;
    border-bottom: 2px solid #f1f5f9 !important;
}

.hover-row:hover { background-color: #f8fafc; cursor: pointer; }

.product-icon { width: 44px; height: 44px; background: #eff6ff; border-radius: 12px; display: flex; align-items: center; justify-content: center; }

.summary-grid { max-width: 320px; margin-left: auto; }
.summary-row { display: flex; justify-content: space-between; font-size: 0.95rem; }

.payment-item { border: 1.5px dashed #e2e8f0; }
.bg-success-light { background: #ecfdf5; }
.bg-primary-light { background: #eff6ff; }
.icon-wrap { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }

.rounded-2xl { border-radius: 20px !important; }
.shadow-lg { box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1) !important; }
.text-slate-900 { color: #0f172a; }
.text-slate-800 { color: #1e293b; }
.text-slate-700 { color: #334155; }
.text-slate-500 { color: #64748b; }
.text-slate-400 { color: #94a3b8; }
.bg-slate-50 { background-color: #f8fafc; }
.border-b { border-bottom: 1px solid #f1f5f9; }
.gap-4 { gap: 16px; }
.gap-3 { gap: 12px; }
</style>



