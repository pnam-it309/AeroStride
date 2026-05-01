<script setup>
import { ref, onMounted, computed } from 'vue';
import { PATH } from '@/router/routePaths';
import { useRoute, useRouter } from 'vue-router';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';
import { useNotifications } from '@/services/notificationService';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import AdminEmptyState from '@/components/common/AdminEmptyState.vue';
import { ArrowLeftIcon, DeviceFloppyIcon, UserIcon } from 'vue-tabler-icons';

import QrcodeStream from '@/components/common/CCCDQRScanner';
import { parseCCCDQR } from '@/utils/cccdQR';
import { dichVuFile } from '@/services/core/dichVuFile';
import axios from 'axios';

const FB_DEFAULT_AVATAR = 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);
const isEditMode = ref(false);
const isDetailView = computed(() => route.path.includes('/detail'));
const showPassword = ref(false);

const employeeForm = ref({
    ma: '',
    ten: '',
    email: '',
    sdt: '',
    tenTaiKhoan: '',
    ngaySinh: '',
    idPhanQuyen: null,
    gioiTinh: true,
    trangThai: 'DANG_HOAT_DONG',
    hinhAnh: '',
    tinh: null,
    thanhPho: null,
    phuongXa: null,
    diaChiChiTiet: '',
    diaChi: ''
});

// Location Data
const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);
const loadingLocations = ref({ provinces: false, districts: false, wards: false });

const fetchProvinces = async () => {
    loadingLocations.value.provinces = true;
    try {
        const res = await axios.get('https://provinces.open-api.vn/api/p/');
        provinces.value = res.data;
    } catch (e) {
        console.error('Error loading provinces:', e);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải danh sách tỉnh/thành phố', color: 'error' });
    } finally {
        loadingLocations.value.provinces = false;
    }
};

const fetchDistricts = async (provinceCode) => {
    if (!provinceCode) return;
    loadingLocations.value.districts = true;
    try {
        const res = await axios.get(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`);
        districts.value = res.data.districts;
    } catch (e) {
        console.error('Error loading districts:', e);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải danh sách quận/huyện', color: 'error' });
    } finally {
        loadingLocations.value.districts = false;
    }
};

const fetchWards = async (districtCode) => {
    if (!districtCode) {
        wards.value = [];
        return;
    }
    loadingLocations.value.wards = true;
    try {
        const res = await axios.get(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`);
        wards.value = res.data.wards || [];
    } catch (e) {
        console.error('Error loading wards:', e);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải danh sách phường/xã', color: 'error' });
    } finally {
        loadingLocations.value.wards = false;
    }
};

// Robust Location Watchers
import { watch } from 'vue';

watch(() => employeeForm.value.tinh, async (newVal, oldVal) => {
    if (newVal && newVal !== oldVal) {
        await fetchDistricts(newVal);
    } else if (!newVal) {
        districts.value = [];
        employeeForm.value.thanhPho = null;
    }
});

watch(() => employeeForm.value.thanhPho, async (newVal, oldVal) => {
    if (newVal && newVal !== oldVal) {
        await fetchWards(newVal);
    } else if (!newVal) {
        wards.value = [];
        employeeForm.value.phuongXa = null;
    }
});

// Quét QR CCCD
const showQR = ref(false);
function onDetectQR(detectedCodes) {
    if (!detectedCodes || detectedCodes.length === 0) return;
    const result = detectedCodes[0].rawValue;

    // Parse dữ liệu QR
    const info = parseCCCDQR(result);
    if (info) {
        employeeForm.value.ten = info.ten;
        employeeForm.value.ngaySinh = info.ngaySinh;
        if (info.sdt) employeeForm.value.sdt = info.sdt;
        employeeForm.value.diaChi = info.diaChi;
        // Không gán info.ma vào employeeForm.value.ma theo yêu cầu
        // Nếu có giới tính
        if (typeof info.gioiTinh === 'boolean') employeeForm.value.gioiTinh = info.gioiTinh;
        addNotification({ title: 'Thành công', subtitle: 'Đã quét và điền thông tin từ CCCD', color: 'success' });
    } else {
        addNotification({ title: 'Lỗi', subtitle: 'Không nhận diện được dữ liệu QR', color: 'error' });
    }
    showQR.value = false;
}
function onInitQR(promise) {
    promise.catch((error) => {
        console.error('QR initialization error:', error);
    });
}

const roles = ref([]);

const getRoleColor = (ma) => {
    if (ma === 'ADMIN' || ma?.includes('AD')) return 'error';
    if (ma === 'KHO' || ma?.includes('KH')) return 'primary';
    return 'success';
};

const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    color: 'success',
    action: null
});

const loadEmployee = async (id) => {
    loading.value = true;
    try {
        const data = await dichVuNhanVien.layChiTietNhanVien(id);

        // Cần đảm bảo tỉnh thành đã được load trước khi tìm kiếm
        if (provinces.value.length === 0) {
            await fetchProvinces();
        }

        // Tìm code từ tên (Backend lưu tên)
        const findProvince = (name) => {
            if (!name) return null;
            return provinces.value.find(p =>
                p.name === name || (p.name && p.name.includes(name)) || (name && name.includes(p.name))
            );
        };

        const p = findProvince(data.tinh);
        if (p) {
            data.tinh = p.code;
            await fetchDistricts(p.code);
            const d = districts.value.find(d => {
                const searchName = data.thanhPho;
                if (!searchName) return false;
                return d.name === searchName || (d.name && d.name.includes(searchName)) || (searchName && searchName.includes(d.name));
            });
            if (d) {
                data.thanhPho = d.code;
                await fetchWards(d.code);
                const w = wards.value.find(w => {
                    const searchName = data.phuongXa;
                    if (!searchName) return false;
                    return w.name === searchName || (w.name && w.name.includes(searchName)) || (searchName && searchName.includes(w.name));
                });
                if (w) data.phuongXa = w.code;
            }
        }

        employeeForm.value = { ...data };
        isEditMode.value = true;
    } catch (error) {
        console.error('Error loading employee:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải thông tin nhân viên', color: 'error' });
    } finally {
        loading.value = false;
    }
};

const handleSave = () => {
    confirmDialog.value = {
        show: true,
        title: isEditMode.value ? 'Cập nhật nhân viên' : 'Thêm nhân viên',
        message: `Xác nhận lưu thông tin nhân viên [${employeeForm.value.ten}]?`,
        color: 'primary',
        action: async () => {
            saving.value = true;
            try {
                const payload = {
                    ten: employeeForm.value.ten,
                    email: employeeForm.value.email,
                    sdt: employeeForm.value.sdt,
                    ngaySinh: employeeForm.value.ngaySinh,
                    gioiTinh: employeeForm.value.gioiTinh,
                    trangThai: employeeForm.value.trangThai,
                    // Map lại tên từ code cho backend
                    tinh: provinces.value.find(p => p.code === employeeForm.value.tinh)?.name || null,
                    thanhPho: districts.value.find(d => d.code === employeeForm.value.thanhPho)?.name || null,
                    phuongXa: wards.value.find(w => w.code === employeeForm.value.phuongXa)?.name || null,
                    diaChiChiTiet: employeeForm.value.diaChiChiTiet,
                    hinhAnh: employeeForm.value.hinhAnh,
                    idPhanQuyen: String(employeeForm.value.idPhanQuyen)
                };
                if (isEditMode.value) {
                    await dichVuNhanVien.capNhatNhanVien(route.params.id, payload);
                    addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật thông tin nhân viên', color: 'success' });
                } else {
                    await dichVuNhanVien.taoNhanVien(payload);
                    addNotification({ title: 'Thành công', subtitle: 'Đã thêm nhân viên mới', color: 'success' });
                }
                router.push(PATH.NHAN_VIEN);
            } catch (error) {
                console.error('Employee save error:', error);
                addNotification({ title: 'Lỗi', subtitle: 'Có lỗi xảy ra khi lưu thông tin', color: 'error' });
            } finally {
                saving.value = false;
                confirmDialog.value.show = false;
            }
        }
    };
};

const fileInput = ref(null);
const uploading = ref(false);

const handleFileClick = () => {
    if (isDetailView.value) return;
    fileInput.value.click();
};

const onFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    uploading.value = true;
    // Local preview
    const localUrl = URL.createObjectURL(file);
    employeeForm.value.hinhAnh = localUrl;

    try {
        const res = await dichVuFile.taiLenFile(file);
        // Safely extract URL if response is an object { url: '...' } or just the string
        const remoteUrl = typeof res === 'object' ? res.fileUrl || res.url || res.data || res.secure_url : res;
        employeeForm.value.hinhAnh = remoteUrl;
        addNotification({ title: 'Thành công', subtitle: 'Đã tải lên ảnh đại diện', color: 'success' });
    } catch (error) {
        // Fallback to default if upload fails
        employeeForm.value.hinhAnh = '';
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải lên ảnh', color: 'error' });
    } finally {
        uploading.value = false;
        URL.revokeObjectURL(localUrl);
    }
};

onMounted(async () => {
    loading.value = true;
    try {
        // Load provinces first to ensure they're available for name matching
        await fetchProvinces();

        const rolesData = await dichVuNhanVien.layDanhSachPhanQuyen();
        roles.value = rolesData.map((r) => ({
            title: r.ten,
            value: r.id,
            ma: r.ma,
            color: getRoleColor(r.ma)
        }));

        if (route.params.id) {
            await loadEmployee(route.params.id);
        } else {
            const defaultRole = roles.value.find((r) => r.title.toLowerCase().includes('nhân viên')) || roles.value[0];
            if (defaultRole) {
                employeeForm.value.idPhanQuyen = defaultRole.value;
            }
        }
    } catch (e) {
        console.error(e);
    } finally {
        loading.value = false;
    }
});
</script>

<template>
    <v-container fluid class="pa-6 animate-fade-in overflow-y-auto font-body" style="height: 100vh">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý tài khoản', disabled: false, href: '#' },
            { title: 'Nhân viên', disabled: false, to: PATH.NHAN_VIEN },
            { title: isDetailView ? 'Chi tiết' : isEditMode ? 'Cập nhật' : 'Thêm mới', disabled: true }
        ]" />

        <!-- Action Header -->
        <div class="d-flex align-center justify-space-between mb-8 mt-4">
            <div class="d-flex align-center gap-4">
                <v-btn icon variant="flat" @click="router.push(PATH.NHAN_VIEN)" class="btn-back-header">
                    <ArrowLeftIcon size="20" />
                </v-btn>
            </div>
            <div class="d-flex gap-3">
                <v-btn v-if="!isDetailView" color="success" variant="flat"
                    class="text-none font-weight-bold text-white px-8 rounded-lg h-11 elevation-4"
                    @click="showQR = true">
                    <v-icon size="20" class="mr-2 text-white">mdi-qrcode-scan</v-icon>
                    Quét QR CCCD
                </v-btn>
                <v-btn v-if="!isDetailView" color="primary" variant="flat"
                    class="text-none font-weight-bold text-white px-8 rounded-lg h-11 elevation-4" :loading="saving"
                    @click="handleSave">
                    <v-icon size="18" class="mr-2 text-white">mdi-check-all</v-icon>
                    {{ isEditMode ? 'Cập nhật nhân viên' : 'Thêm nhân viên' }}
                </v-btn>
                <v-btn v-if="isDetailView" color="primary" variant="flat"
                    class="text-none font-weight-bold text-white px-8 rounded-lg h-11 elevation-4"
                    @click="router.push(`${PATH.NHAN_VIEN_FORM}/${route.params.id}`)">
                    <v-icon size="18" class="mr-2 text-white">mdi-pencil</v-icon>
                    Chỉnh sửa hồ sơ
                </v-btn>
            </div>
        </div>

        <!-- Dialog quét QR CCCD -->
        <v-dialog v-model="showQR" max-width="500" transition="dialog-bottom-transition">
            <v-card class="premium-card">
                <v-card-title class="pa-6 font-weight-bold border-b">
                    <v-icon start color="success" class="mr-2">mdi-qrcode-scan</v-icon>
                    Quét mã QR CCCD
                </v-card-title>
                <v-card-text class="pa-6">
                    <div class="border-4 border-dashed rounded-lg overflow-hidden mb-4">
                        <QrcodeStream @detect="onDetectQR" @init="onInitQR" />
                    </div>
                    <div class="text-caption font-weight-bold text-slate-400 text-center">
                        Đưa mã QR CCCD vào camera để tự động nhận diện thông tin.
                    </div>
                </v-card-text>
                <v-divider></v-divider>
                <v-card-actions class="pa-4 bg-slate-50">
                    <v-spacer></v-spacer>
                    <v-btn color="slate-400" variant="text" class="text-none font-weight-medium"
                        @click="showQR = false">Hủy bỏ</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <v-row v-if="loading">
            <v-col cols="12" class="text-center py-16">
                <v-progress-circular indeterminate color="primary" size="64" width="6"></v-progress-circular>
                <div class="mt-4 text-subtitle-1 font-weight-medium text-slate-500">Đang tải hồ sơ nhân viên...</div>
            </v-col>
        </v-row>

        <!-- DASHBOARD VIEW (DETAIL) -->
        <v-row v-else-if="isDetailView">
            <v-col cols="12" lg="4">
                <v-card class="premium-card mb-6 text-center pa-8">
                    <v-avatar size="140" color="blue-lighten-5" class="mb-4 border-xl border-white elevation-4">
                        <v-img :key="employeeForm.hinhAnh" :src="employeeForm.hinhAnh || FB_DEFAULT_AVATAR"></v-img>
                    </v-avatar>
                    <h2 class="text-h5 font-weight-bold mb-1 text-slate-800">{{ employeeForm.ten }}</h2>
                    <div class="text-subtitle-2 font-weight-medium text-slate-400 mb-6">{{ employeeForm.email }}</div>

                    <v-chip :color="roles.find((r) => r.value === employeeForm.idPhanQuyen)?.color || 'grey'"
                        variant="flat" class="px-8 font-weight-medium rounded-lg mb-8">
                        {{roles.find((r) => r.value === employeeForm.idPhanQuyen)?.title || 'Chưa phân quyền'}}
                    </v-chip>

                    <v-divider class="mb-6 opacity-50"></v-divider>

                    <div class="text-left">
                        <div class="d-flex align-center mb-4 bg-slate-50 pa-3 rounded-lg border">
                            <div class="icon-blob bg-success-lighten-4 mr-3">
                                <v-icon color="success" size="18">mdi-check-circle</v-icon>
                            </div>
                            <div>
                                <div class="text-caption font-weight-medium text-slate-400 uppercase tracking-wider">
                                    TRẠNG THÁI</div>
                                <div class="text-subtitle-2 font-weight-bold"
                                    :class="employeeForm.trangThai === 'DANG_HOAT_DONG' ? 'text-success' : 'text-error'">
                                    {{ employeeForm.trangThai === 'DANG_HOAT_DONG' ? 'Đang hoạt động' : 'Tạm khóa' }}
                                </div>
                            </div>
                        </div>
                        <div class="d-flex align-center bg-slate-50 pa-3 rounded-lg border">
                            <div class="icon-blob bg-info-lighten-4 mr-3">
                                <v-icon color="info" size="18">mdi-account-key</v-icon>
                            </div>
                            <div>
                                <div class="text-caption font-weight-medium text-slate-400 uppercase tracking-wider">TÊN
                                    TÀI KHOẢN</div>
                                <div class="text-subtitle-2 font-weight-bold text-slate-700">{{ employeeForm.tenTaiKhoan
                                    }}</div>
                            </div>
                        </div>
                    </div>
                </v-card>
            </v-col>

            <v-col cols="12" lg="8">
                <v-card class="premium-card mb-6 pb-2">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-8">
                            <div class="icon-blob bg-slate-100 mr-3">
                                <v-icon color="slate-600" size="18">mdi-card-account-details-outline</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Thông tin chi tiết hồ
                                sơ</span>
                        </div>

                        <v-row>
                            <v-col cols="12" md="6">
                                <div class="pa-4 bg-slate-50 rounded-lg border mb-2">
                                    <div
                                        class="text-caption text-slate-400 font-weight-medium mb-1 uppercase tracking-wider">
                                        HỌ VÀ TÊN
                                    </div>
                                    <div class="text-subtitle-1 font-weight-medium text-slate-800">{{ employeeForm.ten
                                        }}</div>
                                </div>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="pa-4 bg-slate-50 rounded-lg border mb-2">
                                    <div
                                        class="text-caption text-slate-400 font-weight-medium mb-1 uppercase tracking-wider">
                                        ĐỊA CHỈ EMAIL
                                    </div>
                                    <div class="text-subtitle-1 font-weight-medium text-slate-800">{{ employeeForm.email
                                        }}</div>
                                </div>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="pa-4 bg-slate-50 rounded-lg border mb-2">
                                    <div
                                        class="text-caption text-slate-400 font-weight-medium mb-1 uppercase tracking-wider">
                                        SỐ ĐIỆN THOẠI
                                    </div>
                                    <div class="text-subtitle-1 font-weight-medium text-slate-800">{{ employeeForm.sdt
                                        }}</div>
                                </div>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="pa-4 bg-slate-50 rounded-lg border mb-2">
                                    <div
                                        class="text-caption text-slate-400 font-weight-medium mb-1 uppercase tracking-wider">
                                        NGÀY SINH
                                    </div>
                                    <div class="text-subtitle-1 font-weight-medium text-slate-800">
                                        {{ employeeForm.ngaySinh || 'Chưa cập nhật' }}
                                    </div>
                                </div>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="pa-4 bg-slate-50 rounded-lg border mb-2">
                                    <div
                                        class="text-caption text-slate-400 font-weight-medium mb-1 uppercase tracking-wider">
                                        GIỚI TÍNH
                                    </div>
                                    <div class="text-subtitle-1 font-weight-medium text-slate-800">
                                        {{ employeeForm.gioiTinh ? 'Nam' : 'Nữ' }}
                                    </div>
                                </div>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="pa-4 bg-slate-50 rounded-lg border mb-2">
                                    <div
                                        class="text-caption text-slate-400 font-weight-medium mb-1 uppercase tracking-wider">
                                        NGÀY GIA NHẬP
                                    </div>
                                    <div class="text-subtitle-1 font-weight-medium text-slate-800">
                                        {{ new Date().toLocaleDateString('vi-VN') }}
                                    </div>
                                </div>
                            </v-col>
                            <v-col cols="12">
                                <div class="pa-4 bg-slate-50 rounded-lg border mb-2">
                                    <div
                                        class="text-caption text-slate-400 font-weight-bold mb-1 uppercase tracking-wider">
                                        ĐỊA CHỈ THƯỜNG TRÚ
                                    </div>
                                    <div class="text-subtitle-1 font-weight-black text-slate-800">
                                        {{ employeeForm.diaChi || 'Chưa cập nhật' }}
                                    </div>
                                </div>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <v-card class="premium-card bg-primary-lighten-5 border-primary-lighten-4">
                    <v-card-text class="pa-8 d-flex align-center">
                        <v-icon color="primary" size="32" class="mr-6">mdi-lock-reset</v-icon>
                        <div>
                            <div class="text-subtitle-1 font-weight-bold text-primary mb-1">Thiết lập tài khoản</div>
                            <p class="text-body-2 font-weight-medium text-slate-600 mb-0">
                                Liên kết thiết lập mật khẩu đã được gửi đến email nhân viên khi tạo mới.
                            </p>
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <!-- FORM VIEW (CREATE/EDIT) -->
        <v-row v-else class="pb-16">
            <v-col cols="12" lg="8">
                <v-card class="premium-card mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <UserIcon class="text-primary" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-700">Thông tin định danh</span>
                        </div>
                        <v-row>
                            <v-col cols="12" md="4">
                                <div class="field-label">Mã nhân viên</div>
                                <v-text-field v-model="employeeForm.ma" readonly placeholder="Hệ thống tự tạo..."
                                    variant="outlined" density="comfortable" class="bg-slate-50"
                                    hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="8">
                                <div class="field-label">Họ và tên *</div>
                                <v-text-field v-model="employeeForm.ten" :readonly="isDetailView"
                                    placeholder="Ví dụ: Nguyễn Văn A" variant="outlined" density="comfortable"
                                    hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Email / Tài khoản *</div>
                                <v-text-field v-model="employeeForm.email" :readonly="isDetailView"
                                    placeholder="name@company.com" variant="outlined" density="comfortable"
                                    class="font-weight-bold" hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Số điện thoại *</div>
                                <v-text-field v-model="employeeForm.sdt" :readonly="isDetailView"
                                    placeholder="09xx.xxx.xxx" variant="outlined" density="comfortable"
                                    hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Ngày sinh</div>
                                <v-text-field v-model="employeeForm.ngaySinh" :readonly="isDetailView" type="date"
                                    variant="outlined" density="comfortable" hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Giới tính</div>
                                <v-select v-model="employeeForm.gioiTinh" :readonly="isDetailView" :items="[
                                    { title: 'Nam', value: true },
                                    { title: 'Nữ', value: false }
                                ]" variant="outlined" density="comfortable" hide-details></v-select>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="field-label">Tỉnh / Thành phố</div>
                                <v-autocomplete v-model="employeeForm.tinh" :readonly="isDetailView" :items="provinces"
                                    item-title="name" item-value="code" placeholder="Chọn Tỉnh/TP" variant="outlined"
                                    density="comfortable" hide-details :loading="loadingLocations.provinces"
                                    @update:model-value="
                                        (val) => {
                                            if (!val) {
                                                employeeForm.thanhPho = null;
                                                employeeForm.phuongXa = null;
                                            }
                                        }
                                    "></v-autocomplete>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="field-label">Quận / Huyện</div>
                                <v-autocomplete v-model="employeeForm.thanhPho" :readonly="isDetailView"
                                    :items="districts" item-title="name" item-value="code" placeholder="Chọn Quận/Huyện"
                                    variant="outlined" density="comfortable" hide-details
                                    :loading="loadingLocations.districts" :disabled="!employeeForm.tinh"
                                    @update:model-value="
                                        (val) => {
                                            if (!val) {
                                                employeeForm.phuongXa = null;
                                            }
                                        }
                                    "></v-autocomplete>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="field-label">Phường / Xã</div>
                                <v-autocomplete v-model="employeeForm.phuongXa" :readonly="isDetailView" :items="wards"
                                    item-title="name" item-value="code" placeholder="Chọn Phường/Xã" variant="outlined"
                                    density="comfortable" hide-details :loading="loadingLocations.wards"
                                    :disabled="!employeeForm.thanhPho"></v-autocomplete>
                            </v-col>
                            <v-col cols="12">
                                <div class="field-label">Địa chỉ chi tiết</div>
                                <v-textarea v-model="employeeForm.diaChiChiTiet" :readonly="isDetailView"
                                    placeholder="Số nhà, tên đường..." variant="outlined" density="comfortable" rows="2"
                                    hide-details class="font-weight-bold"></v-textarea>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" lg="4">
                <v-card class="premium-card mb-6 text-center">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6 text-left">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <v-icon color="primary" size="18">mdi-camera</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-700">Ảnh chân dung</span>
                        </div>

                        <div class="position-relative d-inline-block mx-auto mb-6">
                            <v-avatar size="160" color="blue-lighten-5"
                                class="border-xl border-white elevation-6 cursor-pointer avatar-hover transition-all overflow-hidden"
                                @click="handleFileClick">
                                <v-img :key="employeeForm.hinhAnh" :src="employeeForm.hinhAnh || FB_DEFAULT_AVATAR"
                                    cover>
                                    <template v-slot:placeholder>
                                        <v-row class="fill-height ma-0" align="center" justify="center">
                                            <v-progress-circular indeterminate color="primary"></v-progress-circular>
                                        </v-row>
                                    </template>
                                </v-img>
                                <div v-if="uploading" class="upload-overlay d-flex align-center justify-center">
                                    <v-progress-circular indeterminate size="40" color="white"
                                        width="5"></v-progress-circular>
                                </div>
                            </v-avatar>
                            <div v-if="!isDetailView" class="camera-icon-bubble" @click="handleFileClick">
                                <v-icon color="white" size="20">mdi-camera-plus</v-icon>
                            </div>
                        </div>
                        <input type="file" ref="fileInput" class="d-none" accept="image/*" @change="onFileChange" />

                        <div v-if="!isDetailView" class="text-left">
                            <div class="field-label">Liên kết ảnh (URL)</div>
                            <v-text-field v-if="!isDetailView" v-model="employeeForm.hinhAnh"
                                placeholder="Dán URL ảnh hoặc nhấn vào vòng tròn" variant="outlined"
                                density="comfortable" hide-details class="bg-slate-50"></v-text-field>
                            <p class="text-caption font-weight-medium text-slate-400 mt-3 px-1">
                                Ảnh đại diện sẽ hiển thị trên hồ sơ và thanh thực đơn cá nhân.
                            </p>
                        </div>
                    </v-card-text>
                </v-card>

                <v-card class="premium-card mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6 text-left">
                            <div class="icon-blob bg-slate-100 mr-3">
                                <v-icon color="slate-600" size="18">mdi-account-shield-outline</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-700">Tài khoản & Phân quyền</span>
                        </div>
                        <v-row>
                            <v-col cols="12">
                                <div class="field-label">Vai trò</div>
                                <v-select
                                    v-model="employeeForm.idPhanQuyen"
                                    :readonly="isDetailView"
                                    :items="roles"
                                    item-title="title"
                                    item-value="value"
                                    variant="outlined"
                                    density="comfortable"
                                    class="font-weight-bold"
                                    hide-details
                                >
                                    <template #selection="{ item }">
                                        <v-chip
                                            :color="item.raw.color"
                                            size="small"
                                            variant="tonal"
                                            class="px-5 font-weight-black rounded-lg h-7"
                                            >{{ item.title }}</v-chip
                                        >
                                    </template>
                                </v-select>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <v-card class="premium-card bg-slate-50 border-dashed">
                    <v-card-text class="pa-4">
                        <AdminEmptyState 
                            icon="mdi-history" 
                            title="Chưa có hoạt động" 
                            subtitle="Chưa ghi nhận hoạt động nào của nhân viên này trên hệ thống."
                            class="py-6"
                            icon-size="32"
                        />
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <!-- SHARED CONFIRM -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" @confirm="confirmDialog.action" />
    </v-container>
</template>

<style scoped>
/* Đồng bộ font chữ và cỡ chữ toàn trang */
:deep(.v-container),
:deep(.v-card),
:deep(.v-btn),
:deep(.v-field),
:deep(.v-list-item-title),
:deep(span),
:deep(div),
:deep(p) {
    font-family: 'Inter', 'Outfit', sans-serif !important;
    font-size: 14px !important;
    font-weight: 400;
    /* Reset default weight to normal */
}

:deep(.text-subtitle-1),
:deep(.text-h5),
:deep(.text-h6) {
    font-family: 'Inter', 'Outfit', sans-serif !important;
}

:deep(.v-field__input) {
    font-weight: 400 !important;
    /* Force normal weight for inputs */
    color: #334155 !important;
}

.field-label {
    font-size: 14px !important;
    font-weight: 500 !important;
    /* Unified to Medium weight */
    color: #475569;
    margin-bottom: 6px;
    margin-left: 2px;
}

.font-weight-bold,
.text-subtitle-1 {
    font-weight: 500 !important;
    /* Use Medium (500) for a softer, premium look */
}

.gap-2 {
    gap: 8px;
}

.camera-icon-bubble {
    position: absolute;
    bottom: 0px;
    right: 0px;
    background: #000;
    width: 42px;
    height: 42px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 4px solid #fff;
    cursor: pointer;
    z-index: 10;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    transition: all 0.2s ease;
}

.camera-icon-bubble:hover {
    transform: scale(1.1);
    background: #1e293b;
}

.avatar-hover {
    transition: all 0.3s ease;
}

.avatar-hover:hover {
    transform: translateY(-2px);
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04) !important;
}

.upload-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.4);
    z-index: 5;
}
</style>
