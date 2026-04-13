<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { dichVuHoaDon } from "@/services/admin/dichVuHoaDon";
import { useNotifications } from "@/services/notificationService";
import {
    ChevronLeftIcon, PrinterIcon, EditIcon, CalendarIcon,
    PackageIcon, UserIcon, MapPinIcon, CreditCardIcon, TruckIcon,
    CircleCheckIcon, CircleXIcon, CheckIcon
} from "vue-tabler-icons";

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loaded = ref(false);
const loading = ref(false);
const updatingStatus = ref(false);
const statusDialogOpen = ref(false);
const selectedStatus = ref("");
const confirmNoticeOpen = ref(false);
const pendingStatus = ref("");

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
const formatDate = (date) => date ? new Date(date).toLocaleString('vi-VN') : 'N/A';

const openStatusDialog = () => {
    selectedStatus.value = order.value.trangThai;
    statusDialogOpen.value = true;
};

const requestStatusUpdate = () => {
    if (selectedStatus.value === order.value.trangThai) return;
    pendingStatus.value = selectedStatus.value;
    statusDialogOpen.value = false;
    confirmNoticeOpen.value = true;
};

const executeStatusUpdate = async () => {
    updatingStatus.value = true;
    try {
        await dichVuHoaDon.capNhatTrangThaiHoaDon(order.value.id, pendingStatus.value);
        addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật trạng thái đơn hàng', color: 'success' });
        await loadOrderDetail();
        confirmNoticeOpen.value = false;
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Cập nhật thất bại', color: 'error' });
    } finally {
        updatingStatus.value = false;
    }
};

onMounted(loadOrderDetail);
</script>

<template>
    <v-container fluid class="pa-6 gray-bg min-h-screen">
        <!-- Header -->
        <v-card class="premium-card mb-6 pa-6">
            <div class="header-section">
                <div class="header-left">
                    <v-btn icon variant="tonal" color="primary" class="mr-4 rounded-lg" @click="router.back()">
                        <ChevronLeftIcon size="24" />
                    </v-btn>
                    <div>
                        <h2 class="text-h4 font-weight-bold text-slate-900">Hóa đơn #{{ order.maHoaDon }}</h2>
                        <div class="d-flex align-center mt-1 text-slate-500">
                            <CalendarIcon size="16" class="mr-2" />
                            <span>Ngày tạo: {{ formatDate(order.ngayTao) }}</span>
                        </div>
                    </div>
                </div>
                <div class="header-right">
                    <v-chip :color="getStatusTone(order.trangThai)" variant="flat" class="font-weight-bold px-6 py-4 mr-4">
                        {{ getStatusLabel(order.trangThai) }}
                    </v-chip>
                    
                    <v-btn v-if="order.trangThai < 4" color="primary" variant="flat" class="rounded-lg px-6 font-weight-bold mr-3" height="44" @click="openStatusDialog">
                        <EditIcon size="18" class="mr-2" />
                        Cập nhật trạng thái
                    </v-btn>

                    <v-btn variant="outlined" color="slate-600" class="rounded-lg px-6 font-weight-bold" height="44">
                        <PrinterIcon size="18" class="mr-2" />
                        In hóa đơn
                    </v-btn>
                </div>
            </div>
        </v-card>

        <!-- Timeline -->
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

        <v-row>
            <!-- Details & Items -->
            <v-col cols="12" lg="8">
                <v-card class="premium-card mb-6 overflow-hidden">
                    <div class="card-title pa-5 border-b d-flex align-center bg-slate-50">
                        <PackageIcon size="20" class="mr-3 text-primary" />
                        <span class="font-weight-bold text-slate-800">Sản phẩm đã đặt</span>
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

                <!-- Transaction History -->
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

            <!-- Customer & Shipping -->
            <v-col cols="12" lg="4">
                <v-card class="premium-card mb-6">
                    <div class="card-title pa-5 border-b d-flex align-center bg-slate-50">
                        <UserIcon size="20" class="mr-3 text-primary" />
                        <span class="font-weight-bold text-slate-800">Thông tin khách hàng</span>
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

                <v-card class="premium-card">
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
            </v-col>
        </v-row>

        <!-- Status Update Dialog -->
        <v-dialog v-model="statusDialogOpen" max-width="500" persistent>
            <v-card class="rounded-2xl">
                <v-card-title class="pa-6 pb-0 font-weight-bold">Cập nhật trạng thái</v-card-title>
                <v-card-text class="pa-6">
                    <v-select
                        v-model="selectedStatus"
                        label="Chọn trạng thái đơn hàng"
                        :items="[
                            { title: 'Đã xác nhận', value: 2 },
                            { title: 'Đang giao hàng', value: 3 },
                            { title: 'Hoàn thành', value: 4 },
                            { title: 'Hủy đơn', value: 5 }
                        ]"
                        variant="outlined"
                        rounded="xl"
                        density="comfortable"
                    ></v-select>
                </v-card-text>
                <v-card-actions class="pa-6 pt-0">
                    <v-spacer></v-spacer>
                    <v-btn variant="text" color="slate-500" @click="statusDialogOpen = false" class="rounded-lg font-weight-bold">Hủy</v-btn>
                    <v-btn color="primary" variant="flat" @click="requestStatusUpdate" class="rounded-lg font-weight-bold px-6 shadow-md">Xác nhận</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Confirm Dialog -->
        <v-dialog v-model="confirmNoticeOpen" max-width="400" persistent>
            <v-card class="rounded-2xl text-center pa-6">
                <div class="text-center mb-4">
                    <v-icon color="warning" size="64">mdi-alert-circle-outline</v-icon>
                </div>
                <h3 class="text-h5 font-weight-bold mb-2">Xác nhận thay đổi?</h3>
                <p class="text-slate-500 mb-6">Hành động này sẽ cập nhật trạng thái đơn hàng và không thể hoàn tác.</p>
                <div class="d-flex gap-4">
                    <v-btn block variant="tonal" height="48" class="rounded-xl font-weight-bold" color="slate-500" @click="confirmNoticeOpen = false">Hủy bỏ</v-btn>
                    <v-btn block color="primary" variant="flat" height="48" class="rounded-xl font-weight-bold shadow-lg" :loading="updatingStatus" @click="executeStatusUpdate">Xác nhận ngay</v-btn>
                </div>
            </v-card>
        </v-dialog>
    </v-container>
</template>

<style scoped>
.gray-bg { background-color: #f8fafc; }
.premium-card {
    border-radius: 20px !important;
    border: 1px solid #e2e8f0 !important;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05) !important;
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
</style>
