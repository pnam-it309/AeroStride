<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuPhieuGiamGia } from '@/services/admin/dichVuPhieuGiamGia';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { useNotifications } from '@/services/notificationService';

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

const form = ref({
    ma: '',
    ten: '',
    moTa: '',
    loaiPhieu: 'TIEN_MAT', // Đã sửa: Mặc định là VNĐ
    loaiHienThi: 'CONG_KHAI', // Đã sửa: Mặc định là Công khai
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

// Logic đồng bộ switch vô hạn với giá trị -1
watch(isInfinite, (val) => {
    form.value.soLuong = val ? -1 : 0;
});

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

const handleSave = async () => {
    saving.value = true;
    try {
        const payload = {
            ...form.value,
            hinhThuc: form.value.loaiHienThi,
            listIdKhachHang: form.value.loaiHienThi === 'CA_NHAN' ? selectedCustomerIds.value : [],
            ngayBatDau: new Date(form.value.ngayBatDau).getTime(),
            ngayKetThuc: form.value.ngayKetThuc ? new Date(form.value.ngayKetThuc).getTime() : null
        };
        isEditMode.value
            ? await dichVuPhieuGiamGia.capNhatPhieuGiamGia(route.params.id, payload)
            : await dichVuPhieuGiamGia.taoPhieuGiamGia(payload);
        addNotification({ title: 'Thành công', color: 'success' });
        router.push('/phieu-giam-gia');
    } catch (e) {
        addNotification({ title: 'Lỗi', color: 'error' });
    } finally {
        saving.value = false;
    }
};

onMounted(init);
</script>

<template>
    <v-container fluid class="pa-6 bg-grey-lighten-4 min-h-screen">
        <div class="d-flex align-center mb-6">
            <v-btn icon="mdi-arrow-left" variant="text" @click="router.back()" class="mr-2"></v-btn>
            <h2 class="text-subtitle-1 font-weight-bold uppercase">THÊM PHIẾU GIẢM GIÁ</h2>
            <v-spacer></v-spacer>
            <v-btn color="blue-darken-2" class="text-none px-8 text-white elevation-1" @click="handleSave" :loading="saving">
                {{ isEditMode ? 'Cập nhật' : 'Tạo mới' }}
            </v-btn>
        </div>

        <v-card class="rounded-lg mb-6 shadow-sm border-0">
            <v-card-text class="pa-8">
                <v-row>
                    <v-col cols="12">
                        <label class="label-custom">Mã phiếu giảm giá *</label>
                        <v-text-field
                            v-model="form.ma"
                            readonly
                            placeholder="Mã tự động tạo..."
                            variant="outlined"
                            density="comfortable"
                            class="mt-1 readonly-field"
                            bg-color="#f8f9fa"
                            hide-details
                        ></v-text-field>
                    </v-col>

                    <v-col cols="12">
                        <label class="label-custom">Tên chương trình giảm giá</label>
                        <v-text-field
                            v-model="form.ten"
                            placeholder="Nhập tên phiếu..."
                            variant="outlined"
                            density="comfortable"
                            class="mt-1"
                            hide-details
                        ></v-text-field>
                    </v-col>

                    <v-col cols="12" md="4">
                        <label class="label-custom">Hình thức giảm</label>
                        <v-radio-group v-model="form.loaiPhieu" inline hide-details class="mt-1">
                            <v-radio label="VNĐ" value="TIEN_MAT" color="primary" class="mr-15"></v-radio>
                            <v-radio label="%" value="PHAN_TRAM" color="primary"></v-radio>
                        </v-radio-group>
                    </v-col>

                    <v-col cols="12" md="4">
                        <label class="label-custom">Giá trị giảm {{ form.loaiPhieu === 'TIEN_MAT' ? '(VNĐ)' : '(%)' }}</label>
                        <v-text-field
                            v-if="form.loaiPhieu === 'TIEN_MAT'"
                            v-model.number="form.soTienGiam"
                            type="number"
                            variant="outlined"
                            density="comfortable"
                            class="mt-1"
                            hide-details
                        ></v-text-field>
                        <v-text-field
                            v-else
                            v-model.number="form.phanTramGiamGia"
                            type="number"
                            variant="outlined"
                            density="comfortable"
                            class="mt-1"
                            hide-details
                        ></v-text-field>
                    </v-col>

                    <v-col cols="12" md="4">
                        <label class="label-custom">Giảm tối đa (VNĐ)</label>
                        <v-text-field
                            v-model.number="form.giamToiDa"
                            :disabled="form.loaiPhieu === 'TIEN_MAT'"
                            type="number"
                            variant="outlined"
                            density="comfortable"
                            class="mt-1"
                            hide-details
                            :bg-color="form.loaiPhieu === 'TIEN_MAT' ? '#f0f0f0' : ''"
                        ></v-text-field>
                    </v-col>

                   <v-col cols="12" md="6">
    <div class="d-flex align-center">
        <label class="label-custom">Số lượng phiếu</label>
        <v-switch
            v-model="isInfinite"
            label="Vô hạn"
            color="primary"
            density="compact"
            hide-details
            class="ms-8" 
        ></v-switch>
    </div>
    <v-text-field
        v-model.number="form.soLuong"
        :disabled="isInfinite"
        :placeholder="isInfinite ? 'Vô hạn' : '0'"
        type="number"
        variant="outlined"
        density="comfortable"
        class="mt-1"
        hide-details
    ></v-text-field>
</v-col>
                    <v-col cols="12" md="6">
                        <div class="d-flex align-center">
                            <label class="label-custom">Giá trị hóa đơn tối thiểu</label>
                            <v-spacer></v-spacer>
                            <v-switch style="visibility: hidden" density="compact" hide-details class="ms-2"></v-switch>
                        </div>
                        <v-text-field
                            v-model.number="form.giatriToiThieu"
                            type="number"
                            variant="outlined"
                            density="comfortable"
                            class="mt-1"
                            hide-details
                        ></v-text-field>
                    </v-col>

                    <v-col cols="12" md="6">
                        <label class="label-custom">Ngày bắt đầu</label>
                        <v-text-field
                            v-model="form.ngayBatDau"
                            type="datetime-local"
                            variant="outlined"
                            density="comfortable"
                            class="mt-1"
                            hide-details
                        ></v-text-field>
                    </v-col>

                    <v-col cols="12" md="6">
                        <label class="label-custom">Ngày kết thúc</label>
                        <v-text-field
                            v-model="form.ngayKetThuc"
                            type="datetime-local"
                            variant="outlined"
                            density="comfortable"
                            class="mt-1"
                            hide-details
                        ></v-text-field>
                    </v-col>

                    <v-col cols="12">
                        <label class="label-custom">Phạm vi áp dụng</label>
                        <v-radio-group v-model="form.loaiHienThi" inline hide-details class="mt-1">
                            <v-radio label="Công khai" value="CONG_KHAI" color="primary" class="mr-5"></v-radio>
                            <v-radio label="Gửi cá nhân" value="CA_NHAN" color="primary"></v-radio>
                        </v-radio-group>
                    </v-col>

                    <v-col cols="12">
                        <label class="label-custom">Mô tả / Điều kiện áp dụng</label>
                        <v-textarea
                            v-model="form.moTa"
                            placeholder="Nhập ghi chú chi tiết về phiếu giảm giá..."
                            variant="outlined"
                            rows="3"
                            class="mt-1"
                            hide-details
                        ></v-textarea>
                    </v-col>
                </v-row>
            </v-card-text>
        </v-card>

        <v-expand-transition>
            <v-card v-if="form.loaiHienThi === 'CA_NHAN'" class="rounded-lg shadow-sm border-0 overflow-hidden">
                <div class="pa-5 bg-white border-b">
                    <div class="text-subtitle-1 font-weight-bold">Danh sách khách hàng nhận phiếu</div>
                    <div class="text-caption text-blue-darken-2 font-weight-medium">
                        Đã chọn: <span class="text-red font-weight-bold">{{ selectedCustomerIds.length }}</span> khách hàng
                    </div>

                    <div class="d-flex align-center mt-4 flex-wrap gap-2">
                        <v-text-field
                            v-model="searchGlobal"
                            prepend-inner-icon="mdi-magnify"
                            placeholder="Tìm theo mã, tên, SĐT..."
                            variant="outlined"
                            density="compact"
                            hide-details
                            style="max-width: 400px"
                        ></v-text-field>
                        <v-spacer></v-spacer>
                        <v-btn variant="outlined" class="text-none border-dashed px-4" @click="isSelectAll = !isSelectAll">
                            Chọn tất cả bản ghi
                        </v-btn>
                    </div>
                </div>

                <v-table class="customer-table-style">
                    <thead>
                        <tr>
                            <th class="text-center" style="width: 60px">
                                <v-checkbox-btn v-model="isSelectAll" color="primary"></v-checkbox-btn>
                            </th>
                            <th>Mã KH</th>
                            <th>Tên khách hàng</th>
                            <th>Số điện thoại</th>
                            <th>Email</th>
                            <th>Ngày sinh</th>
                            <th>Chi tiêu gần nhất</th>
                            <th>Tổng chi tiêu</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="item in filteredCustomers" :key="item.id">
                            <td class="text-center">
                                <v-checkbox-btn v-model="selectedCustomerIds" :value="item.id" color="primary"></v-checkbox-btn>
                            </td>
                            <td>{{ item.maKh || 'KH-' + item.id }}</td>
                            <td class="font-weight-bold">{{ item.ten }}</td>
                            <td>{{ item.sdt }}</td>
                            <td class="text-grey">{{ item.email }}</td>
                            <td>{{ item.ngaySinh || '-' }}</td>
                            <td>07/03/2026</td>
                            <td class="font-weight-bold text-blue-darken-2">{{ item.tongChiTieu?.toLocaleString() || '0' }}đ</td>
                        </tr>
                    </tbody>
                </v-table>

                <div class="pa-4 d-flex align-center border-t bg-grey-lighten-5">
                    <span class="text-body-2 text-grey-darken-1">
                        Đang hiển thị {{ filteredCustomers.length > 5 ? 5 : filteredCustomers.length }} bản ghi trong tổng
                        {{ filteredCustomers.length }} kết quả
                    </span>
                    <v-spacer></v-spacer>

                    <div class="d-flex align-center">
                        <span class="text-body-2 mr-2">Hiển thị</span>
                        <select class="custom-page-select mr-4">
                            <option>5 dòng</option>
                            <option>10 dòng</option>
                        </select>

                        <div v-if="filteredCustomers.length > 5" class="pagination-buttons">
                            <v-btn icon="mdi-chevron-left" variant="outlined" size="x-small" class="rounded mr-1" disabled></v-btn>
                            <v-btn variant="flat" size="x-small" class="rounded active-page-btn mr-1">1</v-btn>
                            <v-btn icon="mdi-chevron-right" variant="outlined" size="x-small" class="rounded"></v-btn>
                        </div>
                    </div>
                </div>
            </v-card>
        </v-expand-transition>
    </v-container>
</template>

<style scoped>
.label-custom {
    font-size: 13px;
    font-weight: 600;
    color: #444;
}

.readonly-field {
    pointer-events: none;
    user-select: none;
    cursor: not-allowed;
}

.shadow-sm {
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.1) !important;
}

/* Table Căn giữa và màu Header */
.customer-table-style {
    border-collapse: separate;
}

.customer-table-style thead {
    background-color: #2c3e50 !important;
}

.customer-table-style thead th {
    color: white !important;
    text-align: center !important;
    font-weight: 500 !important;
    height: 44px !important;
}

.customer-table-style tbody td {
    text-align: center !important;
    border-bottom: 1px solid #eee !important;
    padding: 12px 8px !important;
}

/* Pagination Style khớp hình ảnh */
.custom-page-select {
    border: 1px solid #ddd;
    border-radius: 6px;
    padding: 2px 8px;
    font-size: 13px;
    background: white;
    outline: none;
}

.active-page-btn {
    background-color: #2c3e50 !important;
    color: white !important;
    min-width: 28px !important;
}

.pagination-buttons .v-btn {
    border: 1px solid #ddd !important;
}

:deep(.v-field__outline) {
    --v-field-border-opacity: 0.15;
}
</style>
