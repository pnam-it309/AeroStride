<script setup>
import { ref, onMounted } from 'vue';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { EditIcon, UserCheckIcon } from 'vue-tabler-icons';

const loading = ref(false);
const isRefreshing = ref(false);
const customers = ref([]);
const showCustomerDialog = ref(false);
const selectedCustomer = ref(null);
const isEditMode = ref(false);

const customerForm = ref({ ten: '', email: '', sdt: '', ngaySinh: '', gioiTinh: true, diaChiChiTiet: '', trangThai: 'DANG_HOAT_DONG' });
const pagination = ref({ page: 1, size: 5, totalElements: 0, totalPages: 1 });
const filters = ref({ keyword: '', trangThai: null, gioiTinh: null });

// Confirmation Logic
const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const loadCustomers = async () => {
    loading.value = true;
    try {
        const params = {
            // Backend thường nhận page từ 0
            page: pagination.value.page - 1,
            size: pagination.value.size,
            // Đảm bảo nếu rỗng thì gửi null để Backend dễ xử lý
            keyword: filters.value.keyword?.trim() || null,
            trangThai: filters.value.trangThai || null,
            gioiTinh: filters.value.gioiTinh !== null ? filters.value.gioiTinh : null
        };

        const response = await dichVuKhachHang.layKhachHangPhanTrang(params);
        customers.value = response.content || response;
        pagination.value.totalElements = response.totalElements || customers.value.length;
        pagination.value.totalPages = response.totalPages || 1;
    } catch (error) {
        console.error('Lỗi khi load dữ liệu:', error);
    } finally {
        loading.value = false;
    }
};
const handleFilterChange = () => {
    pagination.value.page = 1;
    loadCustomers();
};
const handleRefresh = async () => {
    isRefreshing.value = true;
    filters.value = { keyword: '', trangThai: null, gioiTinh: null };
    pagination.value.page = 1;
    await loadCustomers();
    setTimeout(() => (isRefreshing.value = false), 800);
};

const confirmChangeStatus = (item) => {
    confirmDialog.value = {
        show: true,
        title: 'THAY ĐỔI TRẠNG THÁI',
        message: `Bạn có chắc muốn đổi trạng thái của khách hàng [${item.ten}]?`,
        color: 'warning',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                const newS = item.trangThai === 'DANG_HOAT_DONG' ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
                await dichVuKhachHang.thayDoiTrangThaiKhachHang(item.id, newS);
                item.trangThai = newS;
                confirmDialog.value.show = false;
            } catch (e) {
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

const confirmSaveCustomer = () => {
    confirmDialog.value = {
        show: true,
        title: isEditMode.value ? 'CẬP NHẬT KHÁCH HÀNG' : 'THÊM KHÁCH HÀNG',
        message: `Bạn có muốn lưu thông tin khách hàng [${customerForm.value.ten}]?`,
        color: 'success',
        action: async () => {
            confirmDialog.value.loading = true;
            try {
                if (isEditMode.value) await updateCustomer();
                else await createCustomer();
                confirmDialog.value.show = false;
            } catch (e) {
            } finally {
                confirmDialog.value.loading = false;
            }
        }
    };
};

const updateCustomer = async () => {
    const updated = await dichVuKhachHang.capNhatKhachHang(selectedCustomer.value.id, customerForm.value);
    const index = customers.value.findIndex((c) => c.id === selectedCustomer.value.id);
    if (index !== -1) customers.value[index] = updated;
    showCustomerDialog.value = false;
};

const createCustomer = async () => {
    const created = await dichVuKhachHang.taoKhachHang(customerForm.value);
    customers.value.unshift(created);
    showCustomerDialog.value = false;
    loadCustomers();
};

const editCustomer = (c) => {
    selectedCustomer.value = c;
    customerForm.value = { ...c };
    isEditMode.value = true;
    showCustomerDialog.value = true;
};
const openCreateDialog = () => {
    customerForm.value = { ten: '', email: '', sdt: '', ma: '', gioiTinh: true, diaChiChiTiet: '', trangThai: 'DANG_HOAT_DONG' };
    isViewMode.value = false; // Tắt chế độ chỉ xem
    showCustomerDialog.value = true;
};

// Hàm tạo màu sắc ngẫu nhiên cho Avatar dựa trên tên khách hàng
const getRandomPastelColor = (name) => {
    if (!name) return '#ccc';
    const colors = ['#6366f1', '#ec4899', '#8b5cf6', '#10b981', '#f59e0b'];
    const index = name.length % colors.length;
    return colors[index];
};

const isViewMode = ref(false); // Trạng thái xem chi tiết

const viewDetail = (item) => {
    selectedCustomer.value = item;
    customerForm.value = { ...item };
    isViewMode.value = true; // Bật chế độ chỉ xem
    showCustomerDialog.value = true;
};

onMounted(() => loadCustomers());
</script>

<template>
    <v-container fluid class="pa-8 modern-bg min-h-screen">
        <!-- Header -->
        <v-row class="mb-2 align-center">
            <v-col cols="12" md="6">
                <div class="d-flex align-center mb-2">
                    <v-icon color="primary" class="mr-2" size="32">mdi-account-group-outline</v-icon>
                    <h2 class="text-h4 font-weight-bold tracking-tight text-slate-900">Quản lý khách hàng</h2>
                </div>
            </v-col>
        </v-row>

        <!-- 1. FILTER -->

        <AdminFilter
            :loading="loading"
            :is-refreshing="isRefreshing"
            @refresh="handleRefresh"
            class="pa-4 mb-6 shadow-sm rounded-xl bg-white border"
        >
            <v-row dense align="center">
                <v-col cols="12" md="5">
                    <v-text-field
                        v-model="filters.keyword"
                        placeholder="Tìm tên, mã, email hoặc SĐT..."
                        variant="outlined"
                        density="comfortable"
                        hide-details
                        prepend-inner-icon="mdi-magnify"
                        bg-color="grey-lighten-5"
                        class="rounded-lg custom-field"
                        @keyup.enter="handleFilterChange"
                    >
                        <template #append-inner>
                            <v-btn
                                color="primary"
                                variant="flat"
                                size="small"
                                class="rounded-lg text-none px-4"
                                height="32"
                                @click="handleFilterChange"
                            >
                                Tìm kiếm
                            </v-btn>
                        </template>
                    </v-text-field>
                </v-col>

                <v-col cols="12" md="3">
                    <v-select
                        v-model="filters.gioiTinh"
                        label="Giới tính"
                        :items="[
                            { title: 'Giới tính', value: null },
                            { title: 'Nam', value: true },
                            { title: 'Nữ', value: false }
                        ]"
                        variant="outlined"
                        density="comfortable"
                        hide-details
                        bg-color="grey-lighten-5"
                        class="rounded-lg custom-field"
                        @update:model-value="handleFilterChange"
                    ></v-select>
                </v-col>

                <v-col cols="12" md="4">
                    <v-select
                        v-model="filters.trangThai"
                        label="Trạng thái"
                        :items="[
                            { title: 'Tất cả trạng thái', value: null },
                            { title: 'Đang hoạt động', value: 'DANG_HOAT_DONG' },
                            { title: 'Ngừng hoạt động', value: 'KHONG_HOAT_DONG' }
                        ]"
                        variant="outlined"
                        density="comfortable"
                        hide-details
                        bg-color="grey-lighten-5"
                        class="rounded-lg custom-field"
                        @update:model-value="handleFilterChange"
                    ></v-select>
                </v-col>
            </v-row>
        </AdminFilter>

        <!-- 2. TABLE -->
        <AdminTable
            title="DANH SÁCH KHÁCH HÀNG"
            addButtonText="Thêm mới"
            :headers="[
                { text: 'STT', align: 'center', width: '80px' },
                { text: 'Mã KH', align: 'start', width: '150px' },
                { text: 'Khách hàng', align: 'start' },
                { text: 'Liên hệ', align: 'start' },
                { text: 'Địa chỉ', align: 'start', width: '150px' },
                { text: 'Giới tính', align: 'center' },
                { text: 'Trạng thái', align: 'center' },
                { text: 'Thao tác', align: 'center' }
            ]"
            :items="customers"
            :total-count="pagination.totalElements"
            :loading="loading"
            @add="openCreateDialog"
        >
            <template #row="{ item }">
                <tr v-if="item" class="hover-row">
                    <td class="py-4 text-center">
                        <div class="stt-container">
                            {{ (pagination.page - 1) * pagination.size + customers.indexOf(item) + 1 }}
                        </div>
                    </td>
                    <td>
                        <div class="text-caption text-primary">#{{ (item.ma || 'KH' + item.id).toUpperCase() }}</div>
                    </td>
                    <td>
                        <div class="text-body-1 text-slate-900">
                            {{ item.ten }}
                        </div>
                    </td>

                    <td>
                        <div class="text-body-2 text-slate-900">{{ item.email || '-' }}</div>
                        <div class="text-caption text-slate-900 d-flex align-center mt-1">
                            <v-icon size="14" class="mr-1">mdi-phone</v-icon> {{ item.sdt }}
                        </div>
                    </td>

                    <td>
                        <div class="text-body-2 text-slate-900 text-truncate" style="max-width: 200px">
                            {{ item.diaChiChiTiet || 'Chưa cập nhật' }}
                            <v-tooltip activator="parent" location="top" maxWidth="300">
                                {{ item.diaChiChiTiet }}
                            </v-tooltip>
                        </div>
                    </td>

                    <td class="text-center">
                        <v-chip :class="item.gioiTinh ? 'chip-male' : 'chip-female'" size="small" variant="flat" class="px-4 custom-pill">
                            {{ item.gioiTinh ? 'Nam' : 'Nữ' }}
                        </v-chip>
                    </td>

                    <td class="text-center">
                        <v-chip
                            :class="item.trangThai === 'DANG_HOAT_DONG' ? 'chip-active' : 'chip-inactive'"
                            size="small"
                            variant="flat"
                            class="px-4 custom-pill"
                        >
                            {{ item.trangThai === 'DANG_HOAT_DONG' ? 'Hoạt động' : 'Ngừng HĐ' }}
                        </v-chip>
                    </td>

                    <td class="text-center">
                        <div class="d-flex align-center justify-center ga-1">
                            <v-switch
                                :model-value="item.trangThai === 'DANG_HOAT_DONG'"
                                color="#38660e"
                                inset
                                hide-details
                                density="compact"
                                class="compact-switch"
                                @click.stop="confirmChangeStatus(item)"
                            ></v-switch>

                            <v-btn icon variant="text" color="slate-400" size="32" class="action-btn" @click.stop="viewDetail(item)">
                                <v-icon size="20">mdi-eye-outline</v-icon>
                                <v-tooltip activator="parent" location="top">Xem chi tiết</v-tooltip>
                            </v-btn>

                            <v-btn icon variant="text" color="primary" size="32" class="action-btn" @click.stop="goToUpdatePage(item)">
                                <v-icon size="18">mdi-pencil-outline</v-icon>
                                <v-tooltip activator="parent" location="top">Cập nhật</v-tooltip>
                            </v-btn>
                        </div>
                    </td>
                </tr>
            </template>

            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    v-model:page-size="pagination.size"
                    :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements"
                    :current-size="customers.length"
                    @change="loadCustomers"
                />
            </template>
        </AdminTable>

        <!-- Dialog -->
        <v-dialog v-model="showCustomerDialog" max-width="950" persistent transition="scale-transition">
            <v-card class="rounded-xl overflow-hidden shadow-2xl border-bold bg-white d-flex flex-column" style="max-height: 90vh">
                <v-card-title class="pa-0">
                    <div class="bg-primary-gradient px-10 py-7 d-flex align-center justify-space-between">
                        <div class="d-flex align-center">
                            <v-avatar color="white" size="54" class="mr-5 shadow-lg">
                                <v-icon color="primary" size="32">
                                    {{ isViewMode ? 'mdi-account-details' : 'mdi-account-plus' }}
                                </v-icon>
                            </v-avatar>
                            <div>
                                <div class="text-h4 font-weight-black text-white line-height-1 mb-1">
                                    {{ isViewMode ? 'Chi tiết khách hàng' : 'Thêm khách hàng mới' }}
                                </div>
                            </div>
                        </div>
                        <v-btn icon color="white" variant="tonal" class="rounded-lg" @click="showCustomerDialog = false">
                            <v-icon size="28">mdi-close</v-icon>
                        </v-btn>
                    </div>
                </v-card-title>

                <v-card-text class="pa-10 bg-slate-50">
                    <v-form ref="form">
                        <v-row>
                            <v-col cols="12" md="6">
                                <div class="inner-card-premium pa-8 rounded-xl bg-white shadow-md h-100">
                                    <div
                                        class="text-subtitle-1 font-weight-black text-primary mb-6 d-flex align-center text-uppercase tracking-widest"
                                    >
                                        <v-icon size="22" class="mr-3">mdi-card-account-details</v-icon>
                                        Thông tin
                                    </div>

                                    <label class="form-label-bold">Họ và tên khách hàng <span class="text-error">*</span></label>
                                    <v-text-field
                                        v-model="customerForm.ten"
                                        :readonly="isViewMode"
                                        placeholder="Ví dụ: NGUYỄN VĂN AN"
                                        variant="outlined"
                                        density="comfortable"
                                        rounded="lg"
                                        prepend-inner-icon="mdi-account"
                                        class="bold-input mb-4"
                                    ></v-text-field>

                                    <v-row dense>
                                        <v-col cols="12">
                                            <label class="form-label-bold">Giới tính</label>
                                            <v-radio-group
                                                v-model="customerForm.gioiTinh"
                                                inline
                                                hide-details
                                                :readonly="isViewMode"
                                                class="mt-2 custom-radio-group"
                                            >
                                                <v-radio label="Nam" :value="true" color="primary" class="mr-10">
                                                    <template v-slot:label>
                                                        <span class="radio-label-bold">NAM</span>
                                                    </template>
                                                </v-radio>

                                                <v-radio label="Nữ" :value="false" color="pink-darken-1">
                                                    <template v-slot:label>
                                                        <span class="radio-label-bold">NỮ</span>
                                                    </template>
                                                </v-radio>
                                            </v-radio-group>
                                        </v-col>
                                    </v-row>
                                </div>
                            </v-col>

                            <v-col cols="12" md="6">
                                <div class="inner-card-premium pa-8 rounded-xl bg-white shadow-md h-100">
                                    <div
                                        class="text-subtitle-1 font-weight-black text-orange-darken-3 mb-6 d-flex align-center text-uppercase tracking-widest"
                                    >
                                        <v-icon size="22" class="mr-3">mdi-phone-marker</v-icon>
                                        Thông tin liên lạc
                                    </div>

                                    <label class="form-label-bold">Số điện thoại liên hệ <span class="text-error">*</span></label>
                                    <v-text-field
                                        v-model="customerForm.sdt"
                                        :readonly="isViewMode"
                                        placeholder="09xx xxx xxx"
                                        variant="outlined"
                                        density="comfortable"
                                        rounded="lg"
                                        prepend-inner-icon="mdi-phone"
                                        class="bold-input mb-4"
                                    ></v-text-field>

                                    <label class="form-label-bold">Email</label>
                                    <v-text-field
                                        v-model="customerForm.email"
                                        :readonly="isViewMode"
                                        placeholder="khachhang@example.com"
                                        variant="outlined"
                                        density="comfortable"
                                        rounded="lg"
                                        prepend-inner-icon="mdi-email"
                                        class="bold-input"
                                    ></v-text-field>
                                </div>
                            </v-col>

                            <v-col cols="12">
                                <div class="inner-card-premium pa-8 rounded-xl bg-white shadow-md border-left-primary">
                                    <label class="form-label-bold d-flex align-center">
                                        <v-icon size="20" class="mr-2 text-primary">mdi-map-marker-radius</v-icon>
                                        Địa chỉ chi tiết
                                    </label>
                                    <v-textarea
                                        v-model="customerForm.diaChiChiTiet"
                                        :readonly="isViewMode"
                                        placeholder="Nhập số nhà, tên đường, khu vực..."
                                        variant="outlined"
                                        rows="3"
                                        density="comfortable"
                                        rounded="lg"
                                        hide-details
                                        class="bold-input font-weight-bold"
                                    ></v-textarea>
                                </div>
                            </v-col>
                        </v-row>
                    </v-form>
                </v-card-text>

                <v-divider class="border-opacity-100"></v-divider>

                <v-card-actions class="pa-8 bg-white justify-end">
                    <v-btn
                        variant="outlined"
                        color="slate-500"
                        class="px-8 text-none font-weight-black rounded-lg border-bold"
                        height="48"
                        @click="showCustomerDialog = false"
                        >Hủy</v-btn
                    >

                    <v-btn
                        v-if="!isViewMode"
                        color="primary"
                        variant="flat"
                        size="large"
                        class="px-12 rounded-lg shadow-xl text-none font-weight-black ml-4"
                        height="48"
                        @click="confirmSaveCustomer"
                    >
                        <v-icon start class="mr-2">mdi-check-bold</v-icon>
                        Xác nhận thêm mới
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- SHARED CONFIRM -->
        <AdminConfirm v-model:show="confirmDialog.show" v-bind="confirmDialog" @confirm="confirmDialog.action" />
    </v-container>
</template>

<style scoped>
/* 1. NỀN VÀ TYPOGRAPHY CHUNG */
.modern-bg {
    background-color: #f8fafc; /* Nền xám cực nhẹ kiểu Tailwind */
}

.text-slate-900 {
    color: #060606;
}
.text-slate-800 {
    color: #1e293b;
}
.text-slate-700 {
    color: #334155;
}
.text-slate-500 {
    color: #64748b;
}
.text-slate-400 {
    color: #94a3b8;
}

/* 2. ĐỊNH DẠNG BẢNG (TABLE) */
:deep(.v-table) {
    background: transparent !important;
}

:deep(.v-table__wrapper table thead tr th) {
    background-color: #f1f5f9 !important;
    text-transform: uppercase;
    font-size: 0.75rem !important;
    letter-spacing: 0.05em;
    font-weight: 700 !important;
    color: #64748b !important;
    height: 48px !important;
}

.hover-row {
    transition: all 0.2s ease;
}

.hover-row:hover {
    background-color: #f1f5f9;
    cursor: pointer;
}

/* 3. CỘT SỐ THỨ TỰ (STT) */
.column-stt {
    width: 60px;
    padding-left: 16px !important;
}

.stt-container {
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #f1f5f9;
    color: #64748b;
    font-weight: 500; /* Thay 800 bằng 500 để chữ thanh mảnh như sidebar */
    font-size: 0.75rem;
    border-radius: 6px;
    border: 1px solid #e2e8f0;
    margin: 0 auto;
}

/* 4. CỘT THAO TÁC (3 NÚT SÁT NHAU) */
:deep(.v-table__wrapper table tbody tr td:last-child) {
    width: 140px !important; /* Độ rộng tối ưu để 3 nút chụm lại giữa */
    text-align: center !important;
}

/* Căn giữa tiêu đề "THAO TÁC" */
:deep(.v-data-table-header__content) {
    justify-content: center !important;
    width: 100%;
}

/* Thu gọn diện tích Switch để không đẩy các nút khác */
.compact-switch {
    display: inline-flex !important;
    flex: none !important;
    width: 44px !important;
}

.compact-switch :deep(.v-selection-control) {
    min-height: unset !important;
    width: 44px !important;
}

.compact-switch :deep(.v-switch__track) {
    width: 36px !important;
    height: 18px !important;
    min-width: unset !important;
    opacity: 1; /* Giúp màu xanh #38660e hiện rõ nét */
}

.compact-switch :deep(.v-switch__thumb) {
    width: 14px !important;
    height: 14px !important;
}

/* 5. CHIP GIỚI TÍNH & TRẠNG THÁI (PILL SOFT) */
.custom-pill {
    border-radius: 20px !important;
    height: 24px !important;
    font-size: 0.75rem !important;
    font-weight: 500 !important; /* Độ đậm vừa đủ nhìn, không bị thô */
}

.chip-male {
    background-color: #eff6ff !important;
    color: #2563eb !important;
}
.chip-female {
    background-color: #fdf2f8 !important;
    color: #db2777 !important;
}
.chip-active {
    background-color: #f0fdf4 !important;
    color: #16a34a !important;
}
.chip-inactive {
    background-color: #fff1f2 !important;
    color: #e11d48 !important;
}

/* 6. GIAO DIỆN DIALOG (XỊN & NHẸ) */
.bg-primary-gradient {
    background: linear-gradient(135deg, #cad4f6 0%, #2675f5 100%);
}

.inner-card {
    border: 1px solid #e2e8f0 !important;
    transition: all 0.3s ease;
}

.inner-card:hover {
    border-color: #cbd5e1 !important;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important;
}

.form-label-new {
    display: block;
    font-size: 0.75rem;
    font-weight: 800;
    color: #64748b;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    margin-bottom: 8px;
    margin-left: 2px;
}

/* 7. TIỆN ÍCH KHÁC */
.text-error {
    color: #ef4444;
}
.shadow-md {
    box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1) !important;
}
.rounded-xl {
    border-radius: 12px !important;
}
.gap-4 {
    gap: 16px;
}
.line-height-1 {
    line-height: 1.2;
}

/* Border mờ cho input khi chưa focus */
:deep(.v-field__outline) {
    --v-field-border-opacity: 0.15 !important;
}
</style>
