<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuPhieuGiamGia } from '@/services/admin/dichVuPhieuGiamGia';
import { PATH } from '@/router/routePaths';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { useNotifications } from '@/services/notificationService';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminFilter from '@/components/common/AdminFilter.vue';
import {
    ChevronLeftIcon, DeviceFloppyIcon, PrinterIcon, MailIcon,
    AlertCircleIcon, InfoCircleIcon, SettingsIcon, CalendarIcon,
    SearchIcon, FilterIcon, UsersIcon
} from 'vue-tabler-icons';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);
const isDetailMode = computed(() => !!route.params.id && route.path.includes('/detail'));
const isEditMode = computed(() => !!route.params.id && !route.path.includes('/detail'));
const submitButtonText = computed(() => isEditMode.value ? 'Cập nhật phiếu giảm giá' : 'Thêm phiếu giảm giá');
const isViewOnly = computed(() => isDetailMode.value);

const customers = ref([]);
const selectedCustomerIds = ref([]);
const searchGlobal = ref('');
const isInfinite = ref(false);
const pagination = ref({
    page: 1,
    size: 5
});

const totalPages = computed(() => Math.ceil(filteredCustomers.value.length / pagination.value.size));
const paginatedCustomers = computed(() => {
    const start = (pagination.value.page - 1) * pagination.value.size;
    const end = start + pagination.value.size;
    return filteredCustomers.value.slice(start, end);
});

watch(searchGlobal, () => {
    pagination.value.page = 1;
});

const form = ref({
    ma: '',
    ten: '',
    moTa: '',
    loaiPhieu: 'TIEN_MAT',
    loaiHienThi: 'CONG_KHAI',
    phanTramGiamGia: 0,
    soTienGiam: 0,
    soLuong: 0,
    giatriToiThieu: 0,
    giamToiDa: 0,
    ngayBatDau: new Date().toISOString().slice(0, 16),
    ngayKetThuc: '',
    trangThai: 'DANG_HOAT_DONG',
    listIdKhachHang: []
});

watch(isInfinite, (val) => {
    form.value.soLuong = val ? -1 : 0;
});

watch([() => form.value.loaiHienThi, selectedCustomerIds], ([loai, selecteds]) => {
    if (loai === 'CA_NHAN') {
        form.value.soLuong = selecteds.length;
        isInfinite.value = false;
    }
}, { deep: true });

const isSelectAll = computed({
    get: () => filteredCustomers.value.length > 0 && selectedCustomerIds.value.length === filteredCustomers.value.length,
    set: (value) => {
        selectedCustomerIds.value = value ? filteredCustomers.value.map((c) => c.id) : [];
    }
});

const filteredCustomers = computed(() => {
    return customers.value.filter((c) => {
        const search = searchGlobal.value.toLowerCase();
        return !search || c.ten?.toLowerCase().includes(search) || c.sdt?.includes(search) || c.maKh?.toLowerCase().includes(search);
    });
});

const formatCurrency = (value) => {
    if (!value) return '0 VNĐ';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
};

const formatDate = (value) => {
    if (!value) return '---';
    const date = typeof value === 'number' ? new Date(value) : new Date(value);
    return date.toLocaleDateString('vi-VN');
};

const init = async () => {
    try {
        const data = await dichVuKhachHang.layTatCaKhachHang();
        customers.value = data?.content || data || [];
    } catch (e) {
        console.error(e);
    }

    if (isEditMode.value || isDetailMode.value) {
        loading.value = true;
        try {
            const data = await dichVuPhieuGiamGia.layChiTietPhieuGiamGia(route.params.id);
            form.value = {
                ...data,
                loaiHienThi: data.hinhThuc || 'CONG_KHAI',
                giatriToiThieu: data.donHangToiThieu || 0,
                ngayBatDau: data.ngayBatDau ? new Date(data.ngayBatDau).toISOString().slice(0, 16) : '',
                ngayKetThuc: data.ngayKetThuc ? new Date(data.ngayKetThuc).toISOString().slice(0, 16) : ''
            };
            selectedCustomerIds.value = data.listIdKhachHang || [];
            if (data.soLuong === -1) isInfinite.value = true;
        } finally {
            loading.value = false;
        }
    }
};

const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    color: 'primary',
    action: null,
    loading: false
});

const handleSave = () => {
    confirmDialog.value = {
        show: true,
        title: isEditMode.value ? 'Cập nhật Voucher' : 'Thiết lập Voucher mới',
        message: `Xác nhận lưu thông tin Voucher [${form.value.ten || 'Không tên'}]?`,
        color: 'primary',
        action: async () => {
            confirmDialog.value.loading = true;
            saving.value = true;
            try {
                const payload = {
                    ...form.value,
                    donHangToiThieu: form.value.giatriToiThieu,
                    hinhThuc: form.value.loaiHienThi,
                    ngayBatDau: new Date(form.value.ngayBatDau).getTime(),
                    ngayKetThuc: form.value.ngayKetThuc ? new Date(form.value.ngayKetThuc).getTime() : null,
                    listIdKhachHang: form.value.loaiHienThi === 'CA_NHAN' ? selectedCustomerIds.value : []
                };

                if (isEditMode.value) {
                    await dichVuPhieuGiamGia.capNhatPhieuGiamGia(route.params.id, payload);
                    addNotification({ title: 'Thành công', subtitle: 'Cấu hình voucher hoàn tất', color: 'success' });
                } else {
                    await dichVuPhieuGiamGia.taoPhieuGiamGia(payload);
                    addNotification({ title: 'Thành công', subtitle: 'Đã tạo voucher mới', color: 'success' });
                }
                confirmDialog.value.show = false;
                router.push(PATH.PHIEU_GIAM_GIA);
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: 'Lỗi khi lưu dữ liệu', color: 'error' });
            } finally {
                saving.value = false;
                confirmDialog.value.loading = false;
            }
        }
    };
};

onMounted(init);
</script>

<template>
    <v-container fluid class="pa-6 animate-fade-in overflow-y-auto" style="height: 100vh;">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý phiếu giảm giá', disabled: false, href: '#' },
            { title: 'Phiếu giảm giá', disabled: false, to: PATH.PHIEU_GIAM_GIA },
            { title: isEditMode.value ? 'Cập nhật' : (isDetailMode.value ? 'Chi tiết' : 'Thêm mới'), disabled: true }
        ]" />

        <!-- Action Header -->
        <div class="d-flex align-center justify-space-between mb-8 mt-4">
            <div class="d-flex align-center gap-4">
                <v-btn icon variant="flat" @click="router.back()" class="btn-back-header">
                    <v-icon>mdi-arrow-left</v-icon>
                </v-btn>
            </div>
            <div class="d-flex gap-3">
                <v-btn v-if="!isViewOnly" color="primary" variant="flat"
                    class="text-none font-weight-bold text-white px-8 rounded-lg h-11 elevation-4" @click="handleSave"
                    :loading="saving">
                    <v-icon size="18" class="mr-2 text-white">mdi-check-all</v-icon>
                    {{ submitButtonText }}
                </v-btn>
            </div>
        </div>

        <v-row v-if="loading">
            <v-col cols="12" class="text-center py-16">
                <v-progress-circular indeterminate color="primary" size="64" />
                <div class="mt-4 text-subtitle-1 font-weight-bold text-slate-500">Đang tải cấu hình voucher...</div>
            </v-col>
        </v-row>

        <v-fade-transition mode="out-in">
            <div v-if="!loading" :key="form.loaiHienThi">
                <!-- LAYOUT 1: CONG KHAI (Original 8/4 Stacking) -->
                <v-row v-if="form.loaiHienThi === 'CONG_KHAI'" class="pb-16 animate-fade-in">
                    <v-col cols="12" lg="8">
                        <!-- 1. General Identification -->
                        <v-card class="filter-card elevation-0 mb-6">
                            <v-card-text class="pa-8">
                                <div class="section-header d-flex align-center mb-6">
                                    <div class="icon-blob bg-blue-lighten-5 mr-3">
                                        <v-icon color="primary" size="18">mdi-ticket-percent</v-icon>
                                    </div>
                                    <span class="text-subtitle-1 font-weight-bold text-slate-800">Thông tin chương
                                        trình</span>
                                </div>
                                <v-row>
                                    <v-col cols="12" md="4">
                                        <div class="field-label">Mã phiếu giảm giá</div>
                                        <v-text-field v-model="form.ma" readonly placeholder="Hệ thống tự tạo..."
                                            variant="outlined" density="compact"
                                            class="custom-input mono-font bg-slate-50" hide-details></v-text-field>
                                    </v-col>
                                    <v-col cols="12" md="8">
                                        <div class="field-label">Tên chương trình <span class="text-red">*</span></div>
                                        <v-text-field v-model="form.ten" :readonly="isViewOnly"
                                            placeholder="Ví dụ: Voucher Mừng Sinh Nhật..." variant="outlined"
                                            density="compact" hide-details></v-text-field>
                                    </v-col>
                                    <v-col cols="12">
                                        <div class="field-label">Mô tả chi tiết</div>
                                        <v-textarea v-model="form.moTa" :readonly="isViewOnly" variant="outlined"
                                            rows="3" placeholder="Điều kiện chương trình..." hide-details
                                            class="custom-textarea"></v-textarea>
                                    </v-col>
                                </v-row>
                            </v-card-text>
                        </v-card>

                        <!-- 2. Value Config -->
                        <v-card class="filter-card elevation-0 mb-6">
                            <v-card-text class="pa-8">
                                <div class="section-header d-flex align-center mb-6">
                                    <div class="icon-blob bg-amber-lighten-5 mr-3">
                                        <v-icon color="amber-darken-3" size="18">mdi-cash-register</v-icon>
                                    </div>
                                    <span class="text-subtitle-1 font-weight-bold text-slate-800">Giá trị & Giới
                                        hạn</span>
                                </div>
                                <v-row>
                                    <v-col cols="12" md="4">
                                        <div class="field-label">Hình thức giảm</div>
                                        <v-btn-toggle v-model="form.loaiPhieu" :disabled="isViewOnly" mandatory
                                            color="primary" variant="outlined" density="compact"
                                            class="w-100 rounded-lg custom-toggle">
                                            <v-btn value="TIEN_MAT" class="flex-grow-1">VNĐ</v-btn>
                                            <v-btn value="PHAN_TRAM" class="flex-grow-1">%</v-btn>
                                        </v-btn-toggle>
                                    </v-col>
                                    <v-col cols="12" md="4">
                                        <div class="field-label">Giá trị giảm</div>
                                        <v-text-field v-if="form.loaiPhieu === 'TIEN_MAT'"
                                            v-model.number="form.soTienGiam" :readonly="isViewOnly" type="number"
                                            suffix="VNĐ" variant="outlined" density="compact"
                                            hide-details></v-text-field>
                                        <v-text-field v-else v-model.number="form.phanTramGiamGia"
                                            :readonly="isViewOnly" type="number" suffix="%" variant="outlined"
                                            density="compact" hide-details></v-text-field>
                                    </v-col>
                                    <v-col cols="12" md="4">
                                        <div class="field-label">Giảm tối đa</div>
                                        <v-text-field v-model.number="form.giamToiDa" :readonly="isViewOnly"
                                            :disabled="form.loaiPhieu === 'TIEN_MAT'" suffix="VNĐ" variant="outlined"
                                            density="compact" hide-details
                                            :class="form.loaiPhieu === 'TIEN_MAT' ? 'bg-slate-50' : ''"></v-text-field>
                                    </v-col>
                                    <v-col cols="12" md="6">
                                        <div class="d-flex align-center gap-4 mb-2" style="height: 24px;">
                                            <div class="field-label mb-0">Số lượng</div>
                                            <v-switch v-model="isInfinite" :disabled="isViewOnly" label="Vô hạn"
                                                color="primary" density="compact" hide-details
                                                class="custom-mini-switch ml-2"></v-switch>
                                        </div>
                                        <v-text-field :model-value="isInfinite ? 'Vô hạn' : form.soLuong"
                                            @update:model-value="val => !isInfinite && (form.soLuong = Number(val))"
                                            :type="isInfinite ? 'text' : 'number'" :disabled="isInfinite || isViewOnly"
                                            variant="outlined" density="compact" hide-details></v-text-field>
                                    </v-col>
                                    <v-col cols="12" md="6">
                                        <div class="d-flex align-center mb-2" style="height: 24px;">
                                            <div class="field-label mb-0">Đơn tối thiểu (VNĐ)</div>
                                        </div>
                                        <v-text-field v-model.number="form.giatriToiThieu" :readonly="isViewOnly"
                                            type="number" placeholder="0" variant="outlined" density="compact"
                                            hide-details></v-text-field>
                                    </v-col>
                                </v-row>
                            </v-card-text>
                        </v-card>
                    </v-col>

                    <v-col cols="12" lg="4">
                        <v-card class="filter-card elevation-0 sticky-sidebar">
                            <v-card-text class="pa-8">
                                <div class="section-header d-flex align-center mb-6">
                                    <div class="icon-blob bg-emerald-lighten-5 mr-3">
                                        <v-icon color="emerald-darken-2" size="18">mdi-clock-check</v-icon>
                                    </div>
                                    <span class="text-subtitle-1 font-weight-bold text-slate-800">Cài đặt phát
                                        hành</span>
                                </div>
                                <v-row>
                                    <v-col cols="12">
                                        <div class="field-label">Phạm vi áp dụng</div>
                                        <v-btn-toggle v-model="form.loaiHienThi" :disabled="isViewOnly" mandatory
                                            color="primary" variant="outlined" density="compact"
                                            class="w-100 rounded-lg custom-toggle">
                                            <v-btn value="CONG_KHAI" class="flex-grow-1">Công khai</v-btn>
                                            <v-btn value="CA_NHAN" class="flex-grow-1">Cá nhân</v-btn>
                                        </v-btn-toggle>
                                    </v-col>
                                    <v-col cols="12">
                                        <div class="field-label">Bắt đầu từ ngày</div>
                                        <v-text-field v-model="form.ngayBatDau" :readonly="isViewOnly"
                                            type="datetime-local" variant="outlined" density="compact"
                                            hide-details></v-text-field>
                                    </v-col>
                                    <v-col cols="12">
                                        <div class="field-label">Kết thúc vào ngày</div>
                                        <v-text-field v-model="form.ngayKetThuc" :readonly="isViewOnly"
                                            type="datetime-local" variant="outlined" density="compact"
                                            hide-details></v-text-field>
                                    </v-col>
                                </v-row>
                            </v-card-text>
                        </v-card>
                    </v-col>
                </v-row>

                <!-- LAYOUT 2: CA NHAN (3-Column Row + Full Width List) -->
                <v-row v-else class="pb-16 animate-fade-in">
                    <v-col cols="12">
                        <v-row class="personal-mode-row">
                            <!-- 1. Program Info -->
                            <v-col cols="12" lg="4">
                                <v-card class="filter-card elevation-0 h-100">
                                    <v-card-text class="pa-8">
                                        <div class="section-header d-flex align-center mb-6">
                                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                                <v-icon color="primary" size="18">mdi-ticket-percent</v-icon>
                                            </div>
                                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Thông tin
                                                chương trình</span>
                                        </div>
                                        <v-row>
                                            <v-col cols="12">
                                                <div class="field-label">Mã phiếu giảm giá</div>
                                                <v-text-field v-model="form.ma" readonly variant="outlined"
                                                    density="compact" class="bg-slate-50 mono-font"
                                                    hide-details></v-text-field>
                                            </v-col>
                                            <v-col cols="12">
                                                <div class="field-label">Tên chương trình <span
                                                        class="text-red">*</span></div>
                                                <v-text-field v-model="form.ten" :readonly="isViewOnly"
                                                    variant="outlined" density="compact" hide-details></v-text-field>
                                            </v-col>
                                            <v-col cols="12">
                                                <div class="field-label">Mô tả</div>
                                                <v-textarea v-model="form.moTa" :readonly="isViewOnly"
                                                    variant="outlined" rows="4" hide-details></v-textarea>
                                            </v-col>
                                        </v-row>
                                    </v-card-text>
                                </v-card>
                            </v-col>

                            <!-- 2. Release Settings -->
                            <v-col cols="12" lg="4">
                                <v-card class="filter-card elevation-0 h-100">
                                    <v-card-text class="pa-8">
                                        <div class="section-header d-flex align-center mb-6">
                                            <div class="icon-blob bg-emerald-lighten-5 mr-3">
                                                <v-icon color="emerald-darken-2" size="18">mdi-clock-check</v-icon>
                                            </div>
                                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Cài đặt phát
                                                hành</span>
                                        </div>
                                        <v-row>
                                            <v-col cols="12">
                                                <div class="field-label">Phạm vi áp dụng</div>
                                                <v-btn-toggle v-model="form.loaiHienThi" mandatory color="primary"
                                                    variant="outlined" density="compact" class="w-100 rounded-lg">
                                                    <v-btn value="CONG_KHAI" class="flex-grow-1">Công khai</v-btn>
                                                    <v-btn value="CA_NHAN" class="flex-grow-1">Cá nhân</v-btn>
                                                </v-btn-toggle>
                                            </v-col>
                                            <v-col cols="12">
                                                <div class="field-label">Ngày bắt đầu</div>
                                                <v-text-field v-model="form.ngayBatDau" type="datetime-local"
                                                    variant="outlined" density="compact" hide-details></v-text-field>
                                            </v-col>
                                            <v-col cols="12">
                                                <div class="field-label">Ngày kết thúc</div>
                                                <v-text-field v-model="form.ngayKetThuc" type="datetime-local"
                                                    variant="outlined" density="compact" hide-details></v-text-field>
                                            </v-col>
                                        </v-row>
                                    </v-card-text>
                                </v-card>
                            </v-col>

                            <!-- 3. Value & Limits -->
                            <v-col cols="12" lg="4">
                                <v-card class="filter-card elevation-0 h-100">
                                    <v-card-text class="pa-8">
                                        <div class="section-header d-flex align-center mb-6">
                                            <div class="icon-blob bg-amber-lighten-5 mr-3">
                                                <v-icon color="amber-darken-3" size="18">mdi-cash-register</v-icon>
                                            </div>
                                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Giá trị & Giới
                                                hạn</span>
                                        </div>
                                        <v-row>
                                            <v-col cols="12">
                                                <div class="field-label">Hình thức</div>
                                                <v-btn-toggle v-model="form.loaiPhieu" mandatory color="primary"
                                                    variant="outlined" density="compact" class="w-100 rounded-lg">
                                                    <v-btn value="TIEN_MAT" class="flex-grow-1">VNĐ</v-btn>
                                                    <v-btn value="PHAN_TRAM" class="flex-grow-1">%</v-btn>
                                                </v-btn-toggle>
                                            </v-col>
                                            <v-col cols="12">
                                                <div class="field-label">Giá trị giảm</div>
                                                <v-text-field v-if="form.loaiPhieu === 'TIEN_MAT'"
                                                    v-model.number="form.soTienGiam" suffix="VNĐ"
                                                    variant="outlined" density="compact" hide-details></v-text-field>
                                                <v-text-field v-else
                                                    v-model.number="form.phanTramGiamGia" suffix="%"
                                                    variant="outlined" density="compact" hide-details></v-text-field>
                                            </v-col>
                                            <v-col cols="12">
                                                <div class="field-label">Đơn tối thiểu</div>
                                                <v-text-field v-model.number="form.giatriToiThieu" suffix="VNĐ"
                                                    variant="outlined" density="compact" hide-details></v-text-field>
                                            </v-col>
                                        </v-row>
                                    </v-card-text>
                                </v-card>
                            </v-col>
                        </v-row>
                    </v-col>

                    <!-- Row 2: Full Width Customer List -->
                    <v-col cols="12">
                        <v-card class="filter-card elevation-0 border-2 border-primary-lighten-4">
                            <v-card-text class="pa-8">
                                <div class="section-header d-flex align-center mb-6">
                                    <div class="icon-blob bg-primary-lighten-5 mr-3">
                                        <UsersIcon class="text-primary" size="20" />
                                    </div>
                                    <span class="text-subtitle-1 font-weight-bold text-slate-800">Danh sách khách hàng
                                        chọn lọc</span>
                                    <v-spacer></v-spacer>
                                    <v-chip color="primary" size="small" variant="flat" class="font-weight-bold px-4">
                                        Đã chọn {{ selectedCustomerIds.length }} khách hàng
                                    </v-chip>
                                </div>

                                <AdminFilter title="Tìm kiếm khách hàng" @refresh="searchGlobal = ''">
                                    <v-col cols="12" md="4">
                                        <v-text-field v-model="searchGlobal" prepend-inner-icon="mdi-magnify"
                                            placeholder="Nhập tên, mã hoặc số điện thoại..." variant="outlined"
                                            density="compact" hide-details></v-text-field>
                                    </v-col>
                                </AdminFilter>

                                <div class="table-wrapper border rounded-xl overflow-hidden mt-6">
                                    <table class="native-admin-table">
                                        <thead>
                                            <tr>
                                                <th class="header-cell text-center" style="width: 60px">
                                                    <v-checkbox-btn v-model="isSelectAll" :disabled="isViewOnly"
                                                        color="primary" density="compact" hide-details
                                                        class="d-inline-flex"></v-checkbox-btn>
                                                </th>
                                                <th class="header-cell text-center" style="width: 60px">STT</th>
                                                <th class="header-cell text-center">Mã KH</th>
                                                <th class="header-cell text-center">Tên khách hàng</th>
                                                <th class="header-cell text-center">Ngày sinh</th>
                                                <th class="header-cell text-center">Số điện thoại</th>
                                                <th class="header-cell text-center">Email</th>
                                                <th class="header-cell text-center">Tổng chi tiêu</th>
                                                <th class="header-cell text-center">Đơn hàng gần nhất</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr v-for="(item, index) in paginatedCustomers" :key="item.id"
                                                class="data-row">
                                                <td class="data-cell text-center">
                                                    <v-checkbox-btn v-model="selectedCustomerIds" :value="item.id"
                                                        :disabled="isViewOnly" color="primary" density="compact"
                                                        hide-details class="d-inline-flex"></v-checkbox-btn>
                                                </td>
                                                <td class="data-cell text-center text-slate-400 font-weight-medium">
                                                    {{ (pagination.page - 1) * pagination.size + index + 1 }}
                                                </td>
                                                <td class="data-cell text-center">
                                                    <span class="mono-font text-slate-500">{{ item.maKh || item.ma
                                                        }}</span>
                                                </td>
                                                <td class="data-cell text-center">
                                                    <span class="font-weight-bold text-slate-700">{{ item.ten }}</span>
                                                </td>
                                                <td class="data-cell text-center text-slate-600">
                                                    {{ formatDate(item.ngaySinh) }}
                                                </td>
                                                <td class="data-cell text-center">
                                                    <span class="text-slate-600">{{ item.sdt }}</span>
                                                </td>
                                                <td class="data-cell text-center text-slate-500">
                                                    {{ item.email }}
                                                </td>
                                                <td class="data-cell text-center font-weight-bold text-emerald-600">
                                                    {{ formatCurrency(item.tongChiTieu) }}
                                                </td>
                                                <td class="data-cell text-center text-slate-500">
                                                    {{ formatDate(item.ngayDonHangGanNhat) }}
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>

                                <AdminPagination v-model="pagination.page" :page-size="pagination.size"
                                    @update:pageSize="pagination.size = $event" :total-pages="totalPages"
                                    :total-elements="filteredCustomers.length" :current-size="paginatedCustomers.length"
                                    class="mt-6" />
                            </v-card-text>
                        </v-card>
                    </v-col>
                </v-row>
            </div>
        </v-fade-transition>

        <!-- CONFIRM DIALOG -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />
    </v-container>
</template>

<style scoped>
/* Redundant local styles removed - now using global _admin-common.scss */
@media (min-width: 1280px) {
    .form-grid {
        position: relative;
    }
}




:deep(.custom-mini-switch) {
    .v-selection-control {
        min-height: unset !important;
    }

    .v-label {
        font-size: 12px !important;
        font-weight: 700 !important;
        padding-inline-start: 8px !important;
    }
}

.personal-mode-row {
    &>.v-col {
        transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    }
}

.animate-fade-in {
    animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.border-2 {
    border-width: 2px !important;
}

.table-wrapper {
    background: #fff;
}
</style>
