<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import { useUIStore } from '@/stores/ui';
import { dichVuGiaoCa } from '@/services/admin/dichVuGiaoCa';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';
import { useNotifications } from '@/services/notificationService';
import { isManagementRole } from '@/constants/appConstants';
import { formatCurrency, formatDateTime, readMoneyInVietnameseWords, formatNumberWithDots, parseNumberFromDots } from '@/utils/formatters';
import {
    BuildingStoreIcon,
    CashIcon,
    ClockIcon,
    UserCheckIcon,
    AlertCircleIcon,
    ReceiptIcon,
    RefreshIcon,
    CircleCheckIcon,
    HistoryIcon
} from 'vue-tabler-icons';

const uiStore = useUIStore();
const authStore = useAuthStore();
const { addNotification } = useNotifications();

const loading = ref(false);
const submitting = ref(false);
const activeCa = ref(null);
const lastClosedShift = ref(null);
const listGiaoCaHistory = ref([]);
const listNhanVien = ref([]);

// 4-step stepper state
const currentStep = computed(() => {
    if (lastClosedShift.value) return 4; // Step 4: Hoàn thành bàn giao
    if (!activeCa.value) return 1; // Step 1: Mở ca
    if (activeCa.value.trangThai === 'OPEN') return 2; // Step 2: Đóng ca / Chốt ca
    if (activeCa.value.trangThai === 'PENDING') return 3; // Step 3: Đã bàn giao - Chờ xác nhận
    return 4; // Step 4: Hoàn thành
});

// Stepper steps configuration
const steps = [
    { number: 1, title: 'Mở Ca', desc: 'Kiểm két & mở ca' },
    { number: 2, title: 'Đóng Ca', desc: 'Đếm tiền & chốt ca' },
    { number: 3, title: 'Bàn Giao', desc: 'Gửi nhân viên nhận ca' },
    { number: 4, title: 'Hoàn Thành', desc: 'Xác nhận nhận ca' }
];

// Open shift form
const openShiftForm = ref({
    tienBanDau: 0,
    ghiChuMoCa: '',
    canhBaoKiet: false
});

// Close shift form
const closeShiftForm = ref({
    tienThucTe: 0,
    nhanVienNhanCaId: null,
    ghiChuChotCa: ''
});

const tienBanDauFormatted = computed({
    get: () => formatNumberWithDots(openShiftForm.value.tienBanDau),
    set: (val) => {
        openShiftForm.value.tienBanDau = parseNumberFromDots(val);
    }
});

const tienThucTeFormatted = computed({
    get: () => formatNumberWithDots(closeShiftForm.value.tienThucTe),
    set: (val) => {
        closeShiftForm.value.tienThucTe = parseNumberFromDots(val);
    }
});

// Cash Denomination Calculator Modal State
const showCashCounterModal = ref(false);
const cashCounterTarget = ref('open'); // 'open' or 'close'
const denominations = ref([
    { value: 500000, label: '500,000 đ', count: 0 },
    { value: 200000, label: '200,000 đ', count: 0 },
    { value: 100000, label: '100,000 đ', count: 0 },
    { value: 50000, label: '50,000 đ', count: 0 },
    { value: 20000, label: '20,000 đ', count: 0 },
    { value: 10000, label: '10,000 đ', count: 0 },
    { value: 5000, label: '5,000 đ', count: 0 },
    { value: 2000, label: '2,000 đ', count: 0 },
    { value: 1000, label: '1,000 đ', count: 0 }
]);

const calculatedTotalFromDenominations = computed(() => {
    return denominations.value.reduce((sum, item) => sum + item.value * (Number(item.count) || 0), 0);
});

const openCashCounter = (target) => {
    cashCounterTarget.value = target;
    denominations.value.forEach((d) => (d.count = 0));
    showCashCounterModal.value = true;
};

const applyCashCounter = () => {
    if (cashCounterTarget.value === 'open') {
        openShiftForm.value.tienBanDau = calculatedTotalFromDenominations.value;
    } else {
        closeShiftForm.value.tienThucTe = calculatedTotalFromDenominations.value;
    }
    showCashCounterModal.value = false;
    addNotification({
        title: 'Thành công',
        subtitle: `Đã áp dụng tổng tiền ${formatCurrency(calculatedTotalFromDenominations.value)}`,
        color: 'success'
    });
};

// In-Shift Transactions & Income/Expense State
const showIncomeExpenseModal = ref(false);
const incomeExpenseForm = ref({
    loai: 'CHI', // 'THU' or 'CHI'
    soTien: 0,
    lyDo: ''
});
const listPhatSinh = ref([]);

const addPhatSinh = () => {
    if (!incomeExpenseForm.value.soTien || incomeExpenseForm.value.soTien <= 0) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng nhập số tiền hợp lệ', color: 'warning' });
        return;
    }
    if (!incomeExpenseForm.value.lyDo.trim()) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng nhập lý do thu/chi', color: 'warning' });
        return;
    }
    listPhatSinh.value.push({
        id: Date.now(),
        loai: incomeExpenseForm.value.loai,
        soTien: Number(incomeExpenseForm.value.soTien),
        lyDo: incomeExpenseForm.value.lyDo.trim(),
        thoiGian: new Date()
    });
    showIncomeExpenseModal.value = false;
    incomeExpenseForm.value = { loai: 'CHI', soTien: 0, lyDo: '' };
    addNotification({ title: 'Thành công', subtitle: 'Đã ghi nhận khoản thu/chi phát sinh', color: 'success' });
};

const removePhatSinh = (id) => {
    listPhatSinh.value = listPhatSinh.value.filter((item) => item.id !== id);
};

const tongThuPhatSinh = computed(() => listPhatSinh.value.filter((i) => i.loai === 'THU').reduce((s, i) => s + i.soTien, 0));
const tongChiPhatSinh = computed(() => listPhatSinh.value.filter((i) => i.loai === 'CHI').reduce((s, i) => s + i.soTien, 0));

// Revenue & Financials Calculation
const tienDauCa = computed(() => activeCa.value?.tienBanDau || openShiftForm.value.tienBanDau || 0);
const doanhThuTienMat = computed(() => activeCa.value?.doanhThuTienMat || activeCa.value?.tongDoanhThu || 0);
const doanhThuChuyenKhoan = computed(() => activeCa.value?.doanhThuChuyenKhoan || 0);
const tongDoanhThuCa = computed(() => doanhThuTienMat.value + doanhThuChuyenKhoan.value);

const tongTienKetiDukien = computed(() => {
    return tienDauCa.value + doanhThuTienMat.value + tongThuPhatSinh.value - tongChiPhatSinh.value;
});

const chenhLechTien = computed(() => {
    return Number(closeShiftForm.value.tienThucTe || 0) - tongTienKetiDukien.value;
});

// Load Current Active Shift and Employee List
const fetchCurrentShift = async () => {
    loading.value = true;
    try {
        const res = await dichVuGiaoCa.getCaHienTai();
        const data = res?.data || res;
        if (data && data.id) {
            activeCa.value = data;
            closeShiftForm.value.tienThucTe = data.tienThucTe || data.tienBanDau + (data.tongDoanhThu || 0);
        } else {
            activeCa.value = null;
        }
    } catch (e) {
        activeCa.value = null;
    } finally {
        loading.value = false;
    }
};

const fetchEmployees = async () => {
    try {
        const res = await dichVuNhanVien.layTatCaNhanVien();
        const content = res?.data?.content || res?.data || res || [];
        const rawList = Array.isArray(content) ? content : [];
        listNhanVien.value = rawList.filter((emp) => !isManagementRole(emp));
    } catch (e) {
        console.error(e);
    }
};

const fetchHistory = async () => {
    try {
        const res = await dichVuGiaoCa.getAllLichSu();
        listGiaoCaHistory.value = res?.data || res || [];
    } catch (e) {
        console.error(e);
    }
};

onMounted(() => {
    uiStore.setBreadcrumbs([
        { title: 'Quản lý lịch', disabled: false, href: '#' },
        { title: 'Bàn giao ca', disabled: true }
    ]);
    fetchCurrentShift();
    fetchEmployees();
    fetchHistory();
});

// Actions: Mo Ca, Chot Ca, Xac Nhan
const handleMoCa = async () => {
    const tienBanDau = Number(openShiftForm.value.tienBanDau) || 0;
    const ghiChu = (openShiftForm.value.ghiChuMoCa || '').trim();

    if (tienBanDau < 0 || tienBanDau > 1000000000) {
        addNotification({ title: 'Lỗi', subtitle: 'Số tiền ban đầu phải từ 0đ đến 1,000,000,000đ', color: 'error' });
        return;
    }

    if (ghiChu.length > 500) {
        addNotification({ title: 'Lỗi', subtitle: 'Ghi chú mở ca không được vượt quá 500 ký tự!', color: 'error' });
        return;
    }

    if (/[<>]|script/i.test(ghiChu)) {
        addNotification({ title: 'Lỗi', subtitle: 'Ghi chú chứa ký tự không hợp lệ!', color: 'error' });
        return;
    }

    submitting.value = true;
    try {
        const currentUsername = authStore.user?.username;
        const me = listNhanVien.value.find((e) => e.tenTaiKhoan === currentUsername);

        await dichVuGiaoCa.moCa({
            nhanVienId: me ? me.id : null,
            tienBanDau: tienBanDau,
            ghiChu: ghiChu
        });
        addNotification({ title: 'Thành công', subtitle: 'Đã mở ca làm việc thành công', color: 'success' });
        fetchCurrentShift();
        fetchHistory();
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: e.message || 'Thao tác mở ca thất bại', color: 'error' });
    } finally {
        submitting.value = false;
    }
};

const handleChotCa = async () => {
    const tienThucTe = Number(closeShiftForm.value.tienThucTe) || 0;
    const ghiChu = (closeShiftForm.value.ghiChuChotCa || '').trim();

    if (tienThucTe < 0 || tienThucTe > 1000000000) {
        addNotification({ title: 'Lỗi', subtitle: 'Số tiền thực tế phải từ 0đ đến 1,000,000,000đ', color: 'error' });
        return;
    }

    if (ghiChu.length > 500) {
        addNotification({ title: 'Lỗi', subtitle: 'Ghi chú chốt ca không được vượt quá 500 ký tự!', color: 'error' });
        return;
    }

    if (/[<>]|script/i.test(ghiChu)) {
        addNotification({ title: 'Lỗi', subtitle: 'Ghi chú chứa ký tự không hợp lệ!', color: 'error' });
        return;
    }

    submitting.value = true;
    try {
        const res = await dichVuGiaoCa.chotCa({
            nhanVienNhanCaId: closeShiftForm.value.nhanVienNhanCaId,
            tienThucTe: tienThucTe,
            ghiChu: ghiChu
        });
        const closedData = res?.data || res || {};
        const receiverName = listNhanVien.value.find((n) => n.id === closeShiftForm.value.nhanVienNhanCaId)?.ten;
        lastClosedShift.value = {
            ...closedData,
            nhanVienTen: closedData.nhanVienTen || activeCa.value?.nhanVienTen || authStore.user?.ten || authStore.user?.username,
            nhanVienNhanCaTen: closedData.nhanVienNhanCaTen || receiverName || 'Chưa chỉ định',
            tienThucTe: tienThucTe,
            tienChenhLech: chenhLechTien.value,
            ghiChu: ghiChu,
            id: closedData.id || activeCa.value?.id || closedData.maGiaoCa || ''
        };
        addNotification({ title: 'Thành công', subtitle: 'Đã chốt ca làm việc và gửi bàn giao ca thành công', color: 'success' });
        fetchCurrentShift();
        fetchHistory();
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: e.message || 'Thao tác chốt ca thất bại', color: 'error' });
    } finally {
        submitting.value = false;
    }
};

const resetToNewShift = () => {
    lastClosedShift.value = null;
    openShiftForm.value = {
        tienBanDau: 0,
        ghiChuMoCa: '',
        canhBaoKiet: false
    };
};

// Table headers for history
const historyHeaders = [
    { text: 'Mã Ca', align: 'center', width: '80px' },
    { text: 'Nhân viên mở', align: 'start' },
    { text: 'Thời gian mở ca', align: 'center' },
    { text: 'Thời gian chốt ca', align: 'center' },
    { text: 'Tiền đầu ca', align: 'end' },
    { text: 'Doanh thu', align: 'end' },
    { text: 'Tiền chốt thực tế', align: 'end' },
    { text: 'Chênh lệch', align: 'end' },
    { text: 'Nhân viên nhận ca', align: 'start' },
    { text: 'Trạng thái', align: 'center' }
];

const getStatusBadge = (status) => {
    if (status === 'OPEN') return { color: 'success', label: 'Đang hoạt động' };
    if (status === 'PENDING') return { color: 'warning', label: 'Chờ nhận ca' };
    if (status === 'CLOSED') return { color: 'info', label: 'Đã bàn giao' };
    return { color: 'grey', label: status };
};
</script>

<template>
    <v-container fluid class="pa-4 pa-sm-6 bg-grey-lighten-4 min-h-screen">
        <!-- HEADER & STEPPER 4 BƯỚC -->
        <v-card class="rounded-xl border elevation-2 mb-6 bg-white overflow-hidden">
            <div class="pa-5 bg-gradient-primary text-white d-flex flex-wrap align-center justify-space-between gap-3">
                <div class="d-flex align-center">
                    <v-avatar color="white" class="mr-4 elevation-2" size="48">
                        <BuildingStoreIcon class="text-primary" size="28" />
                    </v-avatar>
                    <div>
                        <h2 class="text-h5 font-weight-bold mb-1" style="color: #ffffff !important;">Màn Hình Bàn Giao Ca Làm Việc</h2>
                        <div class="text-subtitle-2" style="color: #ffffff !important; opacity: 0.95;">
                            Quản lý két tiền, doanh thu ca, kiểm kê tài chính & bàn giao ca cho nhân viên tiếp theo
                        </div>
                    </div>
                </div>
                <v-btn
                    color="white"
                    variant="tonal"
                    class="rounded-pill px-4"
                    prepend-icon="mdi-refresh"
                    @click="fetchCurrentShift"
                    :loading="loading"
                >
                    Cập nhật dữ liệu
                </v-btn>
            </div>

            <!-- Stepper Timeline -->
            <v-divider></v-divider>
            <div class="py-4 px-6 bg-slate-50">
                <v-row class="align-center justify-space-between">
                    <v-col v-for="s in steps" :key="s.number" cols="12" sm="3" class="text-center">
                        <div class="d-flex align-center justify-center">
                            <v-avatar
                                :color="currentStep >= s.number ? 'primary' : 'grey-lighten-2'"
                                :class="{ 'text-white': currentStep >= s.number, 'text-grey-darken-1': currentStep < s.number }"
                                size="36"
                                class="font-weight-bold mr-3 elevation-1"
                            >
                                <template v-if="currentStep > s.number">
                                    <v-icon size="20">mdi-check</v-icon>
                                </template>
                                <template v-else>
                                    {{ s.number }}
                                </template>
                            </v-avatar>
                            <div class="text-left">
                                <div :class="['font-weight-bold text-body-2', currentStep >= s.number ? 'text-primary' : 'text-grey']">
                                    {{ s.title }}
                                </div>
                                <div class="text-caption text-grey">{{ s.desc }}</div>
                            </div>
                        </div>
                    </v-col>
                </v-row>
            </div>
        </v-card>

        <!-- CONTENT KHU VỰC THAO TÁC CA -->
        <v-row class="mb-6">
            <!-- CỘT TRÁI: FORM THAO TÁC THEO BƯỚC -->
            <v-col cols="12" md="7">
                <!-- STEP 3 & 4: HOÀN THÀNH BÀN GIAO CA -->
                <v-card v-if="lastClosedShift" class="rounded-xl border elevation-2 h-100 pa-6 bg-white animate-fade-in">
                    <div class="text-center py-4">
                        <v-avatar size="68" color="success" class="mb-3 elevation-2">
                            <v-icon size="40" color="white">mdi-check-circle-outline</v-icon>
                        </v-avatar>
                        <h3 class="text-h6 font-weight-bold text-slate-800 mb-1">
                            Bàn Giao Ca Thành Công!
                        </h3>
                        <div class="text-caption text-slate-500">
                            Mã ca #{{ lastClosedShift.id }} đã được chốt và gửi đến nhân viên nhận ca.
                        </div>
                    </div>

                    <v-divider class="my-4"></v-divider>

                    <div class="space-y-3 mb-6 bg-slate-50 pa-4 rounded-xl border">
                        <div class="d-flex justify-space-between align-center">
                            <span class="text-slate-600 text-body-2">Nhân viên chốt ca:</span>
                            <strong class="text-slate-800">{{ lastClosedShift.nhanVienTen }}</strong>
                        </div>
                        <div class="d-flex justify-space-between align-center">
                            <span class="text-slate-600 text-body-2">Nhân viên tiếp nhận:</span>
                            <strong class="text-primary">{{ lastClosedShift.nhanVienNhanCaTen }}</strong>
                        </div>
                        <div class="d-flex justify-space-between align-center">
                            <span class="text-slate-600 text-body-2">Tiền thực tế bàn giao:</span>
                            <strong class="text-success text-subtitle-1">{{ formatCurrency(lastClosedShift.tienThucTe || 0) }}</strong>
                        </div>
                        <div class="d-flex justify-space-between align-center">
                            <span class="text-slate-600 text-body-2">Chênh lệch két:</span>
                            <span :class="(lastClosedShift.tienChenhLech || 0) >= 0 ? 'text-success font-weight-bold' : 'text-error font-weight-bold'">
                                {{ formatCurrency(lastClosedShift.tienChenhLech || 0) }}
                            </span>
                        </div>
                        <div v-if="lastClosedShift.ghiChu" class="d-flex justify-space-between align-center">
                            <span class="text-slate-600 text-body-2">Ghi chú:</span>
                            <span class="text-slate-700 italic">{{ lastClosedShift.ghiChu }}</span>
                        </div>
                    </div>

                    <v-btn
                        color="primary"
                        block
                        size="large"
                        class="rounded-lg font-weight-bold text-subtitle-1 py-3 text-none"
                        @click="resetToNewShift"
                    >
                        <v-icon class="mr-2">mdi-plus-circle-outline</v-icon> Bắt đầu mở ca làm việc mới
                    </v-btn>
                </v-card>

                <!-- STEP 1: MỞ CA LÀM VIỆC -->
                <v-card v-else-if="!activeCa" class="rounded-xl border elevation-2 h-100 pa-5 bg-white">
                    <div class="d-flex align-center mb-4 text-primary">
                        <CashIcon size="28" class="mr-2" />
                        <h3 class="text-h6 font-weight-bold">Bắt Đầu Ca Làm Việc Mới (Mở Ca)</h3>
                    </div>
                    <v-alert type="info" variant="tonal" class="rounded-lg mb-5 text-body-2" icon="mdi-information-outline">
                        Bạn chưa có ca làm việc đang mở. Vui lòng kiểm đếm tiền két đầu ca và thực hiện Mở ca để bắt đầu thực hiện bán hàng
                        & giao dịch.
                    </v-alert>

                    <v-form @submit.prevent="handleMoCa">
                        <div class="mb-4">
                            <label class="font-weight-bold text-subtitle-2 mb-1 d-block">
                                Tiền Mặt Đầu Ca Trong Két (VNĐ) <span class="text-error">*</span>
                            </label>
                            <v-text-field
                                v-model="tienBanDauFormatted"
                                type="text"
                                variant="outlined"
                                density="comfortable"
                                color="primary"
                                class="rounded-lg"
                                prepend-inner-icon="mdi-cash"
                                min="0"
                                step="1000"
                                hide-details="auto"
                            >
                                <template v-slot:append-inner>
                                    <v-btn
                                        size="small"
                                        color="primary"
                                        variant="tonal"
                                        class="rounded-pill px-3"
                                        @click="openCashCounter('open')"
                                    >
                                        <CalculatorIcon size="16" class="mr-1" /> Đếm tiền mệnh giá
                                    </v-btn>
                                </template>
                            </v-text-field>

                            <!-- DIỄN GIẢI BẰNG CHỮ -->
                            <div
                                class="mt-2 text-caption bg-primary-lighten-5 text-primary font-weight-medium pa-2 rounded-lg border border-primary-lighten-4"
                            >
                                <strong>Bằng chữ:</strong> {{ readMoneyInVietnameseWords(openShiftForm.tienBanDau) }}
                            </div>
                        </div>

                        <div class="mb-4">
                            <label class="font-weight-bold text-subtitle-2 mb-1 d-block">Ghi Chú Mở Ca (Nếu Có)</label>
                            <v-textarea
                                v-model="openShiftForm.ghiChuMoCa"
                                rows="2"
                                variant="outlined"
                                density="comfortable"
                                placeholder="Nhập tình trạng két tiền, sự cố trước ca nếu có..."
                                class="rounded-lg"
                                hide-details="auto"
                            ></v-textarea>
                        </div>

                        <v-checkbox
                            v-model="openShiftForm.canhBaoKiet"
                            label="Gửi báo cáo cảnh báo sự cố két tiền nếu phát hiện bất thường"
                            color="error"
                            density="compact"
                            class="mb-4 text-body-2"
                            hide-details
                        ></v-checkbox>

                        <v-btn
                            type="submit"
                            color="primary"
                            block
                            size="large"
                            class="rounded-lg font-weight-bold text-subtitle-1 py-3"
                            :loading="submitting"
                        >
                            <CircleCheckIcon class="mr-2" /> Xác Nhận Mở Ca Làm Việc
                        </v-btn>
                    </v-form>
                </v-card>

                <!-- STEP 2 & 3: ĐANG MỞ CA HOẶC BÀN GIAO -->
                <v-card v-else class="rounded-xl border elevation-2 h-100 pa-5 bg-white">
                    <div class="d-flex align-center justify-space-between mb-4 border-b pb-3">
                        <div class="d-flex align-center">
                            <ClockIcon size="28" class="text-primary mr-2" />
                            <div>
                                <h3 class="text-h6 font-weight-bold">Ca Làm Việc Hiện Tại</h3>
                                <div class="text-caption text-grey">
                                    Mã ca: #{{ activeCa.id }} | Nhân viên:
                                    <strong>{{ activeCa.nhanVienTen || authStore.user?.ten }}</strong>
                                </div>
                            </div>
                        </div>
                        <v-chip color="success" class="font-weight-bold" variant="flat">
                            <span class="pulse-dot mr-1"></span> Đang hoạt động
                        </v-chip>
                    </div>

                    <!-- THỜI GIAN VÀ THÔNG TIN CA -->
                    <v-row class="mb-4 bg-grey-lighten-5 pa-3 rounded-lg border mx-0">
                        <v-col cols="6" class="py-1">
                            <div class="text-caption text-grey">Thời gian mở ca</div>
                            <div class="font-weight-bold text-body-2">{{ formatDateTime(activeCa.thoiGianMoCa) }}</div>
                        </v-col>
                        <v-col cols="6" class="py-1">
                            <div class="text-caption text-grey">Tiền mặt đầu ca</div>
                            <div class="font-weight-bold text-body-2 text-primary">{{ formatCurrency(tienDauCa) }}</div>
                        </v-col>
                    </v-row>

                    <!-- FORM ĐÓNG CA & BÀN GIAO -->
                    <div class="text-subtitle-1 font-weight-bold mb-3 d-flex align-center">
                        <UserCheckIcon size="20" class="mr-2 text-primary" /> Quyết Định Chốt Ca & Bàn Giao
                    </div>

                    <v-form @submit.prevent="handleChotCa">
                        <div class="mb-4">
                            <label class="font-weight-bold text-subtitle-2 mb-1 d-block">
                                Tiền Mặt Thực Tế Kiểm Đếm Chốt Ca (VNĐ) <span class="text-error">*</span>
                            </label>
                            <v-text-field
                                v-model="tienThucTeFormatted"
                                type="text"
                                variant="outlined"
                                density="comfortable"
                                color="primary"
                                class="rounded-lg"
                                prepend-inner-icon="mdi-cash-multiple"
                                min="0"
                                step="1000"
                                hide-details="auto"
                            >
                                <template v-slot:append-inner>
                                    <v-btn
                                        size="small"
                                        color="primary"
                                        variant="tonal"
                                        class="rounded-pill px-3"
                                        @click="openCashCounter('close')"
                                    >
                                        <CalculatorIcon size="16" class="mr-1" /> Đếm tiền mệnh giá
                                    </v-btn>
                                </template>
                            </v-text-field>

                            <!-- DIỄN GIẢI CHỮ TIỀN THỰC TẾ -->
                            <div
                                class="mt-2 text-caption bg-primary-lighten-5 text-primary font-weight-medium pa-2 rounded-lg border border-primary-lighten-4"
                            >
                                <strong>Bằng chữ:</strong> {{ readMoneyInVietnameseWords(closeShiftForm.tienThucTe) }}
                            </div>
                        </div>

                        <!-- CHÊNH LỆCH SO VỚI DỰ KIẾN HỆ THỐNG -->
                        <v-alert
                            :type="chenhLechTien === 0 ? 'success' : chenhLechTien > 0 ? 'info' : 'warning'"
                            variant="tonal"
                            class="rounded-lg mb-4 text-body-2"
                        >
                            <div class="d-flex justify-space-between align-center">
                                <div><strong>Tiền mặt dự kiến két hệ thống:</strong> {{ formatCurrency(tongTienKetiDukien) }}</div>
                                <div class="text-right">
                                    <strong>Chênh lệch:</strong>
                                    <span
                                        :class="{
                                            'text-success': chenhLechTien === 0,
                                            'text-info': chenhLechTien > 0,
                                            'text-error font-weight-bold': chenhLechTien < 0
                                        }"
                                    >
                                        {{ chenhLechTien > 0 ? '+' : '' }}{{ formatCurrency(chenhLechTien) }} ({{
                                            chenhLechTien === 0 ? 'Khớp tiền' : chenhLechTien > 0 ? 'Thừa tiền' : 'Thiếu tiền'
                                        }})
                                    </span>
                                </div>
                            </div>
                        </v-alert>

                        <div class="mb-4">
                            <label class="font-weight-bold text-subtitle-2 mb-1 d-block">
                                Chọn Nhân Viên Tiếp Nhận Ca <span class="text-error">*</span>
                            </label>
                            <v-select
                                v-model="closeShiftForm.nhanVienNhanCaId"
                                :items="listNhanVien"
                                item-title="ten"
                                item-value="id"
                                variant="outlined"
                                density="comfortable"
                                placeholder="Chọn nhân viên nhận ca ca tiếp theo..."
                                class="rounded-lg"
                                prepend-inner-icon="mdi-account-switch"
                                hide-details="auto"
                            >
                                <template v-slot:item="{ props, item }">
                                    <v-list-item v-bind="props" :subtitle="`Mã NV: ${item.raw.ma || 'N/A'}`"></v-list-item>
                                </template>
                            </v-select>
                        </div>

                        <div class="mb-4">
                            <label class="font-weight-bold text-subtitle-2 mb-1 d-block">Ghi Chú Chốt Ca / Lý Do Chênh Lệch</label>
                            <v-textarea
                                v-model="closeShiftForm.ghiChuChotCa"
                                rows="2"
                                variant="outlined"
                                density="comfortable"
                                placeholder="Nhập lý do nếu tiền thực tế thiếu/thừa hoặc thông điệp cho ca sau..."
                                class="rounded-lg"
                                hide-details="auto"
                            ></v-textarea>
                        </div>

                        <v-btn
                            type="submit"
                            color="error"
                            block
                            size="large"
                            class="rounded-lg font-weight-bold text-subtitle-1 py-3"
                            :loading="submitting"
                        >
                            <CircleCheckIcon class="mr-2" /> Chốt Ca & Gửi Bàn Giao Ca
                        </v-btn>
                    </v-form>
                </v-card>
            </v-col>

            <!-- CỘT PHẢI: THỐNG KÊ DOANH THU & THU/CHI TRONG CA -->
            <v-col cols="12" md="5">
                <v-card class="rounded-xl border elevation-2 h-100 pa-5 bg-white">
                    <div class="d-flex align-center justify-space-between mb-4 border-b pb-3">
                        <div class="d-flex align-center">
                            <ReceiptIcon size="24" class="text-primary mr-2" />
                            <h3 class="text-h6 font-weight-bold">Tổng Hợp Doanh Thu Ca</h3>
                        </div>
                        <v-btn
                            color="primary"
                            size="small"
                            variant="tonal"
                            class="rounded-pill"
                            prepend-icon="mdi-plus"
                            @click="showIncomeExpenseModal = true"
                        >
                            Ghi Thu/Chi
                        </v-btn>
                    </div>

                    <!-- METRIC CARDS -->
                    <div class="space-y-3">
                        <div class="d-flex justify-space-between align-center pa-3 rounded-lg bg-slate-50 border">
                            <span class="text-body-2 text-slate-600">Tiền đầu ca (Két):</span>
                            <span class="font-weight-bold text-subtitle-1 text-slate-800">{{ formatCurrency(tienDauCa) }}</span>
                        </div>

                        <div class="d-flex justify-space-between align-center pa-3 rounded-lg bg-green-50 border border-green-200">
                            <span class="text-body-2 text-green-800 font-weight-medium">Doanh thu tiền mặt ca:</span>
                            <span class="font-weight-bold text-subtitle-1 text-green-700">+{{ formatCurrency(doanhThuTienMat) }}</span>
                        </div>

                        <div class="d-flex justify-space-between align-center pa-3 rounded-lg bg-blue-50 border border-blue-200">
                            <span class="text-body-2 text-blue-800 font-weight-medium">Doanh thu chuyển khoản:</span>
                            <span class="font-weight-bold text-subtitle-1 text-blue-700">+{{ formatCurrency(doanhThuChuyenKhoan) }}</span>
                        </div>

                        <div class="d-flex justify-space-between align-center pa-3 rounded-lg bg-purple-50 border border-purple-200">
                            <span class="text-body-2 text-purple-800 font-weight-medium">Khoản thu phát sinh:</span>
                            <span class="font-weight-bold text-subtitle-1 text-purple-700">+{{ formatCurrency(tongThuPhatSinh) }}</span>
                        </div>

                        <div class="d-flex justify-space-between align-center pa-3 rounded-lg bg-orange-50 border border-orange-200">
                            <span class="text-body-2 text-orange-800 font-weight-medium">Khoản chi phát sinh:</span>
                            <span class="font-weight-bold text-subtitle-1 text-orange-700">-{{ formatCurrency(tongChiPhatSinh) }}</span>
                        </div>

                        <v-divider class="my-3"></v-divider>

                        <div
                            class="d-flex justify-space-between align-center pa-3 rounded-lg bg-primary-lighten-5 border border-primary-lighten-3"
                        >
                            <span class="text-subtitle-2 font-weight-bold text-primary">TỔNG TIỀN DỰ KIẾN KÉT:</span>
                            <span class="text-h6 font-weight-black text-primary">{{ formatCurrency(tongTienKetiDukien) }}</span>
                        </div>
                    </div>

                    <!-- DANH SÁCH THU/CHI PHÁT SINH -->
                    <div class="mt-5" v-if="listPhatSinh.length">
                        <div class="text-subtitle-2 font-weight-bold mb-2">Nhật Ký Thu/Chi Trong Ca:</div>
                        <v-list density="compact" class="border rounded-lg pa-0 overflow-y-auto" style="max-height: 180px">
                            <v-list-item v-for="item in listPhatSinh" :key="item.id" class="border-b pa-2">
                                <template v-slot:prepend>
                                    <v-chip size="x-small" :color="item.loai === 'THU' ? 'success' : 'error'" class="font-weight-bold mr-2">
                                        {{ item.loai }}
                                    </v-chip>
                                </template>
                                <v-list-item-title class="text-body-2 font-weight-medium">{{ item.lyDo }}</v-list-item-title>
                                <template v-slot:append>
                                    <span
                                        :class="['font-weight-bold text-body-2 mr-2', item.loai === 'THU' ? 'text-success' : 'text-error']"
                                    >
                                        {{ item.loai === 'THU' ? '+' : '-' }}{{ formatCurrency(item.soTien) }}
                                    </span>
                                    <v-btn icon size="x-small" color="error" variant="text" @click="removePhatSinh(item.id)">
                                        <v-icon size="16">mdi-close</v-icon>
                                    </v-btn>
                                </template>
                            </v-list-item>
                        </v-list>
                    </div>
                </v-card>
            </v-col>
        </v-row>

        <!-- LỊCH SỬ BÀN GIAO CA -->
        <AdminTable
            title="Lịch Sử Bàn Giao Ca Gần Đây"
            :headers="historyHeaders"
            :items="listGiaoCaHistory"
            :loading="loading"
            :show-add-button="false"
            class="rounded-xl border elevation-2 bg-white"
        >
            <template #extra-actions>
                <v-btn color="primary" variant="tonal" size="small" prepend-icon="mdi-refresh" @click="fetchHistory">Tải lại</v-btn>
            </template>
            <template #row="{ item }">
                <tr class="data-row">
                    <td class="data-cell text-center">#{{ item.id }}</td>
                    <td class="data-cell font-weight-medium">{{ item.nhanVienTen || 'N/A' }}</td>
                    <td class="data-cell text-center text-slate-500">{{ formatDateTime(item.thoiGianMoCa) }}</td>
                    <td class="data-cell text-center text-slate-500">{{ formatDateTime(item.thoiGianChotCa) }}</td>
                    <td class="data-cell text-right">{{ formatCurrency(item.tienBanDau) }}</td>
                    <td class="data-cell text-right text-success font-weight-bold">{{ formatCurrency(item.tongDoanhThu) }}</td>
                    <td class="data-cell text-right">{{ formatCurrency(item.tienThucTe) }}</td>
                    <td class="data-cell text-right">
                        <span
                            :class="[
                                'font-weight-bold',
                                item.tienThucTe - (item.tienBanDau + item.tongDoanhThu) < 0 ? 'text-error' : 'text-success'
                            ]"
                        >
                            {{ formatCurrency(item.tienThucTe - (item.tienBanDau + item.tongDoanhThu)) }}
                        </span>
                    </td>
                    <td class="data-cell font-weight-medium">{{ item.nhanVienNhanCaTen || 'Chưa bàn giao' }}</td>
                    <td class="data-cell text-center">
                        <v-chip size="small" :color="getStatusBadge(item.trangThai).color" class="font-weight-bold" variant="flat">
                            {{ getStatusBadge(item.trangThai).label }}
                        </v-chip>
                    </td>
                </tr>
            </template>
        </AdminTable>

        <!-- MODAL ĐẾM TIỀN MỆNH GIÁ -->
        <v-dialog v-model="showCashCounterModal" max-width="600" persistent>
            <v-card class="rounded-xl pa-2">
                <v-card-title class="pa-4 bg-primary text-white font-weight-bold d-flex align-center justify-space-between rounded-t-lg">
                    <div class="d-flex align-center"><CalculatorIcon class="mr-2" /> Công Cụ Đếm Tiền Theo Mệnh Giá</div>
                    <v-btn icon size="small" variant="text" color="white" @click="showCashCounterModal = false">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </v-card-title>

                <v-card-text class="pa-4">
                    <div class="text-caption text-grey mb-3">Nhập số lượng tờ tiền tương ứng với từng mệnh giá trong két:</div>
                    <v-row density="compact">
                        <v-col v-for="denom in denominations" :key="denom.value" cols="6" sm="4" class="py-1">
                            <div class="pa-2 border rounded-lg bg-slate-50">
                                <div class="font-weight-bold text-caption text-primary mb-1">{{ denom.label }}</div>
                                <v-text-field
                                    v-model.number="denom.count"
                                    type="number"
                                    min="0"
                                    variant="outlined"
                                    density="compact"
                                    suffix="tờ"
                                    hide-details
                                ></v-text-field>
                                <div class="text-right text-caption text-grey mt-1">
                                    {{ formatCurrency(denom.value * (denom.count || 0)) }}
                                </div>
                            </div>
                        </v-col>
                    </v-row>

                    <v-divider class="my-4"></v-divider>

                    <div class="pa-3 bg-primary-lighten-5 rounded-lg border border-primary-lighten-3">
                        <div class="d-flex justify-space-between align-center mb-1">
                            <span class="font-weight-bold text-subtitle-1 text-primary">TỔNG TIỀN ĐẾM ĐƯỢC:</span>
                            <span class="text-h6 font-weight-black text-primary">{{
                                formatCurrency(calculatedTotalFromDenominations)
                            }}</span>
                        </div>
                        <div class="text-caption text-primary">
                            <strong>Bằng chữ:</strong> {{ readMoneyInVietnameseWords(calculatedTotalFromDenominations) }}
                        </div>
                    </div>
                </v-card-text>

                <v-card-actions class="pa-4 pt-0">
                    <v-spacer></v-spacer>
                    <v-btn variant="outlined" color="grey" class="rounded-lg px-4" @click="showCashCounterModal = false">Hủy Bỏ</v-btn>
                    <v-btn color="primary" class="rounded-lg px-4 font-weight-bold" @click="applyCashCounter">Áp Dụng Số Tiền</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- MODAL GHI NHẬN THU CHI NGOÀI -->
        <v-dialog v-model="showIncomeExpenseModal" max-width="480">
            <v-card class="rounded-xl">
                <v-card-title class="pa-4 bg-primary text-white font-weight-bold d-flex align-center">
                    <PlusIcon class="mr-2" /> Ghi Nhận Khoản Thu / Chi Phát Sinh
                </v-card-title>
                <v-card-text class="pa-4">
                    <div class="mb-3">
                        <label class="font-weight-bold text-subtitle-2 mb-1 d-block">Loại Giao Dịch</label>
                        <v-radio-group v-model="incomeExpenseForm.loai" inline hide-details>
                            <v-radio label="Khoản Thu (+)" value="THU" color="success"></v-radio>
                            <v-radio label="Khoản Chi (-)" value="CHI" color="error"></v-radio>
                        </v-radio-group>
                    </div>

                    <div class="mb-3">
                        <label class="font-weight-bold text-subtitle-2 mb-1 d-block">Số Tiền (VNĐ)</label>
                        <v-text-field
                            v-model.number="incomeExpenseForm.soTien"
                            type="number"
                            variant="outlined"
                            density="comfortable"
                            min="0"
                            step="1000"
                            hide-details
                        ></v-text-field>
                    </div>

                    <div class="mb-3">
                        <label class="font-weight-bold text-subtitle-2 mb-1 d-block">Lý Do Thu / Chi</label>
                        <v-textarea
                            v-model="incomeExpenseForm.lyDo"
                            rows="2"
                            variant="outlined"
                            density="comfortable"
                            placeholder="Ví dụ: Chi tiền mua nước lọc, Thu tiền bồi thường hỏng đồ..."
                            hide-details
                        ></v-textarea>
                    </div>
                </v-card-text>
                <v-card-actions class="pa-4 pt-0">
                    <v-spacer></v-spacer>
                    <v-btn variant="outlined" color="grey" class="rounded-lg" @click="showIncomeExpenseModal = false">Hủy</v-btn>
                    <v-btn color="primary" class="rounded-lg font-weight-bold" @click="addPhatSinh">Lưu Ghi Nhận</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-container>
</template>

<style scoped>
.min-h-screen {
    min-height: 100vh;
}
.bg-gradient-primary {
    background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
}
.pulse-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background-color: #22c55e;
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
    animation: pulse 1.6s infinite;
}
@keyframes pulse {
    0% {
        transform: scale(0.95);
        box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
    }
    70% {
        transform: scale(1);
        box-shadow: 0 0 0 6px rgba(34, 197, 94, 0);
    }
    100% {
        transform: scale(0.95);
        box-shadow: 0 0 0 0 rgba(34, 197, 94, 0);
    }
}
.custom-table :deep(th) {
    background-color: #f8fafc !important;
    font-weight: 600 !important;
    color: #334155 !important;
    white-space: nowrap;
}

:deep(.v-field__input),
:deep(.v-field__input input),
:deep(.v-field__input input::placeholder),
:deep(.v-select__selection),
:deep(.v-select__selection-text),
:deep(.v-autocomplete__selection),
:deep(.v-autocomplete__selection-text) {
    font-size: 13px !important;
}
</style>
