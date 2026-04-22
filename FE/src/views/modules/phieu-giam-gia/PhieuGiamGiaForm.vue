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
import {
    ChevronLeftIcon, DeviceFloppyIcon, PrinterIcon, MailIcon,
    AlertCircleIcon, InfoCircleIcon, SettingsIcon, CalendarIcon
} from 'vue-tabler-icons';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);
const isEditMode = computed(() => !!route.params.id && !route.path.includes('/detail'));

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

const init = async () => {
    try {
        const data = await dichVuKhachHang.layTatCaKhachHang();
        customers.value = data?.content || data || [];
    } catch (e) {
        console.error(e);
    }

    if (isEditMode.value) {
        loading.value = true;
        try {
            const data = await dichVuPhieuGiamGia.layChiTietPhieuGiamGia(route.params.id);
            form.value = { ...data, loaiHienThi: data.hinhThuc || 'CONG_KHAI' };
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
            { title: isEditMode ? 'Cập nhật' : 'Thêm mới', disabled: true }
        ]" />

        <!-- Action Header -->
        <div class="d-flex align-center justify-space-between mb-8 mt-4">
            <div class="d-flex align-center gap-4">
                <v-btn icon variant="flat" @click="router.back()" class="btn-back-header">
                    <v-icon>mdi-arrow-left</v-icon>
                </v-btn>
            </div>
            <div class="d-flex gap-3">
                <v-btn color="primary" variant="flat"
                    class="text-none font-weight-black text-white px-8 rounded-lg h-11 elevation-4" @click="handleSave"
                    :loading="saving">
                    <v-icon size="18" class="mr-2 text-white">mdi-check-all</v-icon>
                    {{ isEditMode ? 'Cập nhật thay đổi' : 'Kích hoạt Voucher' }}
                </v-btn>
            </div>
        </div>

        <v-row v-if="loading">
            <v-col cols="12" class="text-center py-16">
                <v-progress-circular indeterminate color="primary" size="64" />
                <div class="mt-4 text-subtitle-1 font-weight-bold text-slate-500">Đang tải cấu hình voucher...</div>
            </v-col>
        </v-row>

        <v-row v-else class="form-grid pb-16">
            <!-- Left Column -->
            <v-col cols="12" lg="8">
                <!-- 1. General Identification -->
                <v-card class="premium-card mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <v-icon color="primary" size="18">mdi-ticket-percent</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Thông tin chương trình</span>
                        </div>

                        <v-row>
                            <v-col cols="12" md="4">
                                <div class="field-label">Mã phiếu giảm giá</div>
                                <v-text-field v-model="form.ma" readonly placeholder="Hệ thống tự tạo..." variant="outlined"
                                    density="comfortable" class="custom-input mono-font bg-slate-50"
                                    hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="8">
                                <div class="field-label">Tên chương trình <span class="text-red">*</span></div>
                                <v-text-field v-model="form.ten" placeholder="Ví dụ: Voucher Mừng Sinh Nhật..."
                                    variant="outlined" density="comfortable" hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12">
                                <div class="field-label">Mô tả chi tiết</div>
                                <v-textarea v-model="form.moTa" variant="outlined" rows="3"
                                    placeholder="Điều kiện chương trình..." hide-details class="custom-textarea"></v-textarea>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <!-- 2. Value Config -->
                <v-card class="premium-card mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-amber-lighten-5 mr-3">
                                <v-icon color="amber-darken-3" size="18">mdi-cash-register</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Giá trị & Giới hạn</span>
                        </div>

                        <v-row>
                            <v-col cols="12" md="4">
                                <div class="field-label">Hình thức giảm</div>
                                <v-btn-toggle v-model="form.loaiPhieu" mandatory color="primary" variant="outlined"
                                    density="comfortable" class="w-100 rounded-lg custom-toggle">
                                    <v-btn value="TIEN_MAT" class="flex-grow-1 font-weight-bold">VNĐ</v-btn>
                                    <v-btn value="PHAN_TRAM" class="flex-grow-1 font-weight-bold">%</v-btn>
                                </v-btn-toggle>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="field-label">Giá trị giảm</div>
                                <v-text-field v-if="form.loaiPhieu === 'TIEN_MAT'" v-model.number="form.soTienGiam"
                                    type="number" suffix="VNĐ" variant="outlined" density="comfortable"
                                    hide-details></v-text-field>
                                <v-text-field v-else v-model.number="form.phanTramGiamGia" type="number" suffix="%"
                                    variant="outlined" density="comfortable" hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="field-label">Giảm tối đa</div>
                                <v-text-field v-model.number="form.giamToiDa" :disabled="form.loaiPhieu === 'TIEN_MAT'"
                                    suffix="VNĐ" variant="outlined" density="comfortable" hide-details
                                    :class="form.loaiPhieu === 'TIEN_MAT' ? 'bg-slate-50' : ''"></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="d-flex align-center gap-4 mb-2" style="height: 24px;">
                                    <div class="field-label mb-0">Số lượng</div>
                                    <v-switch v-model="isInfinite" label="Vô hạn" color="primary" density="compact"
                                        hide-details class="custom-mini-switch ml-2"></v-switch>
                                </div>
                                <v-text-field v-model.number="form.soLuong"
                                    :disabled="isInfinite || form.loaiHienThi === 'CA_NHAN'" variant="outlined"
                                    density="comfortable" hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="d-flex align-center mb-2" style="height: 24px;">
                                    <div class="field-label mb-0">Đơn tối thiểu (VNĐ)</div>
                                </div>
                                <v-text-field v-model.number="form.giatriToiThieu" type="number" placeholder="0" variant="outlined"
                                    density="comfortable" hide-details></v-text-field>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <v-expand-transition>
                    <div v-if="form.loaiHienThi === 'CA_NHAN'">
                        <v-card class="premium-card mb-6">
                            <v-card-text class="pa-8">
                                <div class="section-header d-flex align-center mb-6">
                                    <div class="icon-blob bg-emerald-lighten-5 mr-3">
                                        <v-icon color="emerald-darken-2" size="18">mdi-account-multiple-check</v-icon>
                                    </div>
                                    <span class="text-subtitle-1 font-weight-black text-slate-800">Danh sách khách hàng chọn lọc</span>
                                    <v-spacer></v-spacer>
                                    <v-text-field v-model="searchGlobal" prepend-inner-icon="mdi-magnify" placeholder="Tìm tên, SĐT..." variant="outlined" density="compact" hide-details style="max-width: 250px"></v-text-field>
                                </div>

                                <v-table class="modern-table border rounded-lg overflow-hidden" height="300px" fixed-header>
                                    <thead>
                                        <tr>
                                            <th class="text-center" style="width: 50px">
                                                <v-checkbox-btn v-model="isSelectAll" color="primary" density="compact" hide-details class="d-inline-flex"></v-checkbox-btn>
                                            </th>
                                            <th>Khách hàng</th>
                                            <th>Liên hệ</th>
                                            <th class="text-center">Chi tiêu</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr v-for="item in paginatedCustomers" :key="item.id">
                                            <td class="text-center">
                                                <v-checkbox-btn v-model="selectedCustomerIds" :value="item.id" color="primary" density="compact" hide-details class="d-inline-flex"></v-checkbox-btn>
                                            </td>
                                            <td><div class="font-weight-bold">{{ item.ten }}</div><div class="text-caption text-slate-400">{{ item.maKh }}</div></td>
                                            <td><div class="text-body-2">{{ item.sdt }}</div><div class="text-caption text-slate-400">{{ item.email }}</div></td>
                                            <td class="text-center font-weight-bold text-primary">{{ item.tongChiTieu?.toLocaleString() }}đ</td>
                                        </tr>
                                    </tbody>
                                </v-table>
                                <AdminPagination
                                    v-model="pagination.page"
                                    :page-size="pagination.size"
                                    @update:pageSize="pagination.size = $event"
                                    :total-pages="totalPages"
                                    :total-elements="filteredCustomers.length"
                                    :current-size="paginatedCustomers.length"
                                    class="mt-4"
                                />
                                <div class="mt-2 px-2">
                                    <span class="text-caption font-weight-medium text-slate-500">Đã chọn {{ selectedCustomerIds.length }} khách hàng</span>
                                </div>
                            </v-card-text>
                        </v-card>
                    </div>
                </v-expand-transition>
            </v-col>

            <!-- Right Column -->
            <v-col cols="12" lg="4">
                <v-card class="premium-card mb-6 sticky-sidebar">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-emerald-lighten-5 mr-3">
                                <v-icon color="emerald-darken-2" size="18">mdi-clock-check</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Cài đặt phát hành</span>
                        </div>

                        <v-row>
                            <v-col cols="12">
                                <div class="field-label">Phạm vi áp dụng</div>
                                <v-btn-toggle v-model="form.loaiHienThi" mandatory color="primary" variant="outlined"
                                    density="comfortable" class="w-100 rounded-lg custom-toggle">
                                    <v-btn value="CONG_KHAI" class="flex-grow-1 font-weight-bold">Công khai</v-btn>
                                    <v-btn value="CA_NHAN" class="flex-grow-1 font-weight-bold">Cá nhân</v-btn>
                                </v-btn-toggle>
                            </v-col>
                            <v-col cols="12">
                                <div class="field-label">Bắt đầu từ ngày</div>
                                <v-text-field v-model="form.ngayBatDau" type="datetime-local" variant="outlined"
                                    density="comfortable" hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12">
                                <div class="field-label">Kết thúc vào ngày</div>
                                <v-text-field v-model="form.ngayKetThuc" type="datetime-local" variant="outlined"
                                    density="comfortable" hide-details></v-text-field>
                            </v-col>

                        </v-row>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

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

.modern-table :deep(th) {
    background: #f8fafc !important;
    font-size: 12px !important;
    font-weight: 700 !important;
    color: #64748b !important;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    vertical-align: middle !important;
}

.modern-table :deep(td) {
    vertical-align: middle !important;
    padding-top: 10px !important;
    padding-bottom: 10px !important;
}

.modern-table :deep(.v-checkbox-btn) {
    min-height: auto !important;
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
</style>
