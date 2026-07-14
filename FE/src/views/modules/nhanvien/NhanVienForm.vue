<script setup>
import { ref, onMounted, computed } from 'vue';
import { PATH } from '@/router/routePaths';
import { useRoute, useRouter } from 'vue-router';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';
import { generateRandomCode } from '@/utils/codeGenerator';
import { useNotifications } from '@/services/notificationService';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { ArrowLeftIcon, DeviceFloppyIcon, UserIcon } from 'vue-tabler-icons';

import QrcodeStream from '@/components/common/CCCDQRScanner';
import { parseCCCDQR } from '@/utils/cccdQR';
import { SYSTEM_STATUS, STATUS_OPTIONS } from '@/constants/statusConstants';
import { dichVuFile } from '@/services/core/dichVuFile';
import axios from 'axios';

import { GIOI_TINH_OPTIONS } from '@/constants/appConstants';
import { TRANG_THAI_NHAN_VIEN } from '@/constants/nhanVienConstants';

const FB_DEFAULT_AVATAR = 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

// Helper to clean location names for better matching
const cleanName = (s) => {
    if (!s) return '';
    return String(s).toLowerCase()
        .replace(/^(thành phố|tỉnh|quận|huyện|phường|xã|thị xã|thị trấn|tp\.?|t\.?|q\.?|h\.?|x\.?)\s+/gi, '')
        .replace(/\s+/g, ' ')
        .trim();
};

const loading = ref(false);
const saving = ref(false);
const isEditMode = ref(false);
const isDetailView = computed(() => route.path.includes('/detail') || route.query.view === 'true');
const submitButtonText = computed(() => isEditMode.value ? 'Cập nhật nhân viên' : 'Thêm nhân viên');


const employeeForm = ref({
    ma: '',
    ten: '',
    email: '',
    sdt: '',
    tenTaiKhoan: '',
    ngaySinh: '',
    idPhanQuyen: null,
    gioiTinh: true,
    trangThai: SYSTEM_STATUS.ACTIVE,
    hinhAnh: '',
    diaChi: '',
    tinh: null,
    thanhPho: null,
    phuongXa: null,
    diaChiChiTiet: ''
});

const TRANG_THAI_OPTIONS = [
    { title: 'Đang hoạt động', value: SYSTEM_STATUS.ACTIVE },
    { title: 'Ngừng hoạt động', value: SYSTEM_STATUS.INACTIVE }
];

// Address Selection Logic
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
    if (!districtCode) return;
    loadingLocations.value.wards = true;
    try {
        const res = await axios.get(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`);
        wards.value = res.data.wards;
    } catch (e) {
        console.error('Error loading wards:', e);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải danh sách phường/xã', color: 'error' });
    } finally {
        loadingLocations.value.wards = false;
    }
};

import { watch } from 'vue';
watch(
    () => employeeForm.value.tinh,
    (newVal) => {
        employeeForm.value.thanhPho = null;
        employeeForm.value.phuongXa = null;
        if (newVal) fetchDistricts(newVal);
    }
);

watch(
    () => employeeForm.value.thanhPho,
    (newVal) => {
        employeeForm.value.phuongXa = null;
        if (newVal) fetchWards(newVal);
    }
);

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
        employeeForm.value.diaChiChiTiet = info.diaChi;
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

        // Map nested phanQuyen.id to idPhanQuyen for the select component
        const idPhanQuyen = data.idPhanQuyen || (data.phanQuyen ? data.phanQuyen.id : null);

        employeeForm.value = {
            ...data,
            idPhanQuyen,
            tinh: null, // Reset to null first to trigger watchers correctly later
            thanhPho: null,
            phuongXa: null,
            diaChiChiTiet: data.diaChiChiTiet || data.diaChi || ''
        };
        isEditMode.value = true;

        // Load data for selects based on names from BE
        if (data.tinh || data.thanhPho || data.phuongXa) {
            await fetchProvinces();
            const province = provinces.value.find(p => cleanName(p.name) === cleanName(data.tinh) || p.code === data.tinh);
            if (province) {
                employeeForm.value.tinh = province.code;
                await fetchDistricts(province.code);
                const district = districts.value.find(d => cleanName(d.name) === cleanName(data.thanhPho) || d.code === data.thanhPho);
                if (district) {
                    employeeForm.value.thanhPho = district.code;
                    await fetchWards(district.code);
                    const ward = wards.value.find(w => cleanName(w.name) === cleanName(data.phuongXa) || w.code === data.phuongXa);
                    if (ward) {
                        employeeForm.value.phuongXa = ward.code;
                    }
                }
            }
        } else if (data.diaChi && data.diaChi.includes(',')) {
            // Fallback to parsing if separate fields are missing
            const parts = data.diaChi.split(',').map((p) => p.trim());
            if (parts.length >= 4) {
                employeeForm.value.diaChiChiTiet = parts.slice(0, parts.length - 3).join(', ');
                await fetchProvinces();
                const pName = parts[parts.length - 1];
                const province = provinces.value.find((p) => p.name.includes(pName) || pName.includes(p.name));
                if (province) {
                    employeeForm.value.tinh = province.code;
                    await fetchDistricts(province.code);
                    const dName = parts[parts.length - 2];
                    const district = districts.value.find((d) => d.name.includes(dName) || dName.includes(d.name));
                    if (district) {
                        employeeForm.value.thanhPho = district.code;
                        await fetchWards(district.code);
                        const wName = parts[parts.length - 3];
                        const ward = wards.value.find((w) => w.name.includes(wName) || wName.includes(w.name));
                        if (ward) {
                            employeeForm.value.phuongXa = ward.code;
                        }
                    }
                }
            }
        }

    } catch (error) {
        console.error('Error loading employee:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải thông tin nhân viên', color: 'error' });
    } finally {
        loading.value = false;
    }
};

const handleSave = () => {
    const rawName = employeeForm.value.ten;
    if (!rawName || !String(rawName).trim()) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng nhập họ và tên nhân viên', color: 'error' });
        return;
    }
    if (String(rawName).trim().length > 255) {
        addNotification({ title: 'Lỗi', subtitle: 'Họ và tên không được vượt quá 255 ký tự', color: 'error' });
        return;
    }
    if (!/^[\p{L}0-9\s]+$/u.test(rawName)) {
        addNotification({ title: 'Lỗi', subtitle: 'Họ và tên không được chứa ký tự đặc biệt', color: 'error' });
        return;
    }
    if (rawName.trim() !== rawName) {
        addNotification({ title: 'Lỗi', subtitle: 'Họ và tên không được chứa khoảng trắng ở 2 đầu', color: 'error' });
        return;
    }

    const email = employeeForm.value.email;
    if (!email || !String(email).trim()) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng nhập email', color: 'error' });
        return;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        addNotification({ title: 'Lỗi', subtitle: 'Email không đúng định dạng', color: 'error' });
        return;
    }

    const phone = employeeForm.value.sdt;
    if (!phone || !String(phone).trim()) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng nhập số điện thoại', color: 'error' });
        return;
    }
    const phoneRegex = /(84|0[3|5|7|8|9])+([0-9]{8})\b/;
    if (!phoneRegex.test(phone) || String(phone).length < 10 || String(phone).length > 11) {
        addNotification({ title: 'Lỗi', subtitle: 'Số điện thoại không hợp lệ', color: 'error' });
        return;
    }

    if (employeeForm.value.ngaySinh) {
        const bdDate = new Date(employeeForm.value.ngaySinh);
        const nowDate = new Date();
        if (bdDate.getTime() > nowDate.getTime()) {
            addNotification({ title: 'Lỗi', subtitle: 'Ngày sinh không thể ở trong tương lai', color: 'error' });
            return;
        }
        let age = nowDate.getFullYear() - bdDate.getFullYear();
        const m = nowDate.getMonth() - bdDate.getMonth();
        if (m < 0 || (m === 0 && nowDate.getDate() < bdDate.getDate())) {
            age--;
        }
        if (age < 18) {
            addNotification({ title: 'Lỗi', subtitle: 'Nhân viên phải từ 18 tuổi trở lên', color: 'error' });
            return;
        }
    }

    if (!employeeForm.value.tinh || !employeeForm.value.thanhPho || !employeeForm.value.phuongXa || !employeeForm.value.diaChiChiTiet) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng điền đầy đủ địa chỉ thường trú', color: 'error' });
        return;
    }

    confirmDialog.value = {
        show: true,
        title: isEditMode.value ? 'Cập nhật nhân viên' : 'Thêm nhân viên mới',
        message: `Xác nhận lưu thông tin nhân viên [${employeeForm.value.ten}]?`,
        color: 'primary',
        action: async () => {
            saving.value = true;
            try {
                // Combine address fields robustly
                const p = provinces.value.find(x => x.code === employeeForm.value.tinh);
                const d = districts.value.find(x => x.code === employeeForm.value.thanhPho);
                const w = wards.value.find(x => x.code === employeeForm.value.phuongXa);

                const provinceName = p ? p.name : employeeForm.value.tinhName;
                const districtName = d ? d.name : employeeForm.value.thanhPhoName;
                const wardName = w ? w.name : employeeForm.value.phuongXaName;

                const addrParts = [
                    employeeForm.value.diaChiChiTiet,
                    wardName,
                    districtName,
                    provinceName
                ].filter(part => part && String(part).trim() !== '');

                const combinedAddress = addrParts.length > 0 ? addrParts.join(', ') : '';

                const payload = {
                    ma: employeeForm.value.ma,
                    ten: employeeForm.value.ten,
                    email: employeeForm.value.email,
                    sdt: employeeForm.value.sdt,
                    tenTaiKhoan: employeeForm.value.tenTaiKhoan,
                    ngaySinh: employeeForm.value.ngaySinh,
                    gioiTinh: employeeForm.value.gioiTinh,
                    trangThai: employeeForm.value.trangThai,
                    tinh: provinceName || null,
                    thanhPho: districtName || null,
                    phuongXa: wardName || null,
                    diaChiChiTiet: employeeForm.value.diaChiChiTiet || null,
                    hinhAnh: employeeForm.value.hinhAnh,
                    idPhanQuyen: employeeForm.value.idPhanQuyen
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
                const errMsg = error.response?.data?.message || error.response?.data || 'Có lỗi xảy ra khi lưu thông tin';
                addNotification({ title: 'Lỗi', subtitle: errMsg, color: 'error' });
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
    try {
        const res = await dichVuFile.taiLenFile(file);
        // Safely extract URL if response is an object { url: '...' } or just the string
        employeeForm.value.hinhAnh = typeof res === 'object' ? res.fileUrl || res.url || res.data || res.secure_url : res;
        addNotification({ title: 'Thành công', subtitle: 'Đã tải lên ảnh đại diện', color: 'success' });
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải lên ảnh', color: 'error' });
    } finally {
        uploading.value = false;
    }
};

// Hàm mở trình chọn ngày khi bấm vào icon
const openDatePicker = (event) => {
    // Tìm input type="date" gần nhất trong cùng một v-input container
    const container = event.target.closest('.v-input');
    const input = container ? container.querySelector('input[type="date"]') : null;

    if (input) {
        if (typeof input.showPicker === 'function') {
            input.showPicker();
        } else {
            input.click();
        }
    }
};

const goBack = () => {
    router.push(PATH.NHAN_VIEN);
};

const handlePhoneUpdate = (val) => {
    employeeForm.value.sdt = val.replace(/\D/g, '');
};

const nameRules = [
    (v) => !!v || 'Vui lòng nhập họ và tên',
    (v) => (v && v.trim() === v) || 'Không được chứa khoảng trắng ở 2 đầu',
    (v) => (v && /^[\p{L}0-9\s]+$/u.test(v)) || 'Không được chứa ký tự đặc biệt'
];

const emailRules = [
    (v) => !!v || 'Vui lòng nhập Email',
    (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) || 'Email không hợp lệ'
];

const phoneRules = [
    (v) => !!v || 'Vui lòng nhập số điện thoại',
    (v) => /^0[0-9]{9}$/.test(v) || 'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0'
];

const dobRules = [
    (v) => {
        if (!v) return true;
        const dob = new Date(v);
        const today = new Date();
        if (dob > today) return 'Ngày sinh không được ở tương lai';
        let age = today.getFullYear() - dob.getFullYear();
        const m = today.getMonth() - dob.getMonth();
        if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) age--;
        return age >= 18 || 'Nhân viên phải từ 18 tuổi trở lên';
    }
];

onMounted(async () => {
    loading.value = true;
    fetchProvinces();
    try {
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
            // Nếu thêm mới, tự động chọn vai trò "Nhân viên" làm mặc định
            const defaultRole = roles.value.find((r) => r.title.toLowerCase().includes('nhân viên')) || roles.value[0];
            if (defaultRole) {
                employeeForm.value.idPhanQuyen = defaultRole.value;
            }
            try {
                employeeForm.value.ma = await generateRandomCode('NhanVien');
            } catch (e) {
                console.error('Lỗi khi lấy mã', e);
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
    <v-container id="nhan-vien-form-container" fluid class="pa-6 animate-fade-in overflow-y-auto font-body" style="height: 100vh">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý tài khoản', disabled: false, href: '#' },
            { title: 'Nhân viên', disabled: false, to: PATH.NHAN_VIEN },
            { title: isEditMode ? 'Cập nhật' : 'Thêm mới', disabled: true }
        ]" />

        <!-- Action Header -->
        <div class="d-flex align-center justify-space-between mb-8 mt-4">
            <div class="d-flex align-center gap-4">
                <v-btn icon variant="flat" @click="goBack" class="btn-back-header">
                    <ArrowLeftIcon size="20" />
                </v-btn>
            </div>
            <div class="d-flex gap-3 header-actions__buttons">
                <v-btn variant="flat" class="admin-btn-qr text-none" @click="showQR = true">
                    <v-icon size="20" class="mr-2">mdi-qrcode-scan</v-icon>
                    <span>Quét QR CCCD</span>
                </v-btn>
                <v-btn color="primary" variant="flat" class="add-btn-primary text-none" :loading="saving"
                    @click="handleSave">
                    <v-icon size="18" class="mr-2">mdi-check-all</v-icon>
                    <span>{{ submitButtonText }}</span>
                </v-btn>
            </div>
        </div>

        <!-- Dialog quét QR CCCD -->
        <v-dialog v-model="showQR" max-width="500" transition="dialog-bottom-transition">
            <v-card class="premium-card elevation-0 border border-slate-200">
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

        <v-row class="pb-16">
            <v-col cols="12" lg="8">
                <v-card class="premium-card elevation-0 border border-slate-200 mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <UserIcon class="text-primary" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-700">Thông tin định danh</span>
                        </div>
                        <v-row>
                            <v-col cols="12" md="6">
                                <div class="field-label">Mã nhân viên</div>
                                <v-text-field v-model="employeeForm.ma" readonly placeholder="Hệ thống tự tạo..."
                                    variant="outlined" density="comfortable" class="bg-slate-50"
                                    hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Họ và tên <span class="text-error">*</span></div>
                                <v-text-field v-model="employeeForm.ten" :readonly="isDetailView"
                                    :rules="nameRules"
                                    placeholder="Ví dụ: Nguyễn Văn A" variant="outlined" density="compact"
                                    hide-details="auto"></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Email / Tài khoản <span class="text-error">*</span></div>
                                <v-text-field v-model="employeeForm.email" :readonly="isDetailView"
                                    :rules="emailRules"
                                    placeholder="name@company.com" variant="outlined" density="compact"
                                    hide-details="auto"></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Số điện thoại <span class="text-error">*</span></div>
                                <v-text-field :model-value="employeeForm.sdt" @update:model-value="handlePhoneUpdate" 
                                    :readonly="isDetailView"
                                    :rules="phoneRules"
                                    placeholder="09xx.xxx.xxx" variant="outlined" density="compact"
                                    hide-details="auto" maxlength="10"></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6" v-if="isEditMode">
                                <div class="field-label">Tên tài khoản</div>
                                <v-text-field v-model="employeeForm.tenTaiKhoan" :readonly="isDetailView"
                                    placeholder="Nhập tên tài khoản..." variant="outlined" bg-color="white"
                                    density="compact" hide-details></v-text-field>
                            </v-col>

                            <v-col cols="12" md="6">
                                <div class="field-label">Ngày sinh</div>
                                <AppDatePicker v-model="employeeForm.ngaySinh" :disabled="isDetailView"
                                    placeholder="Chọn ngày sinh"
                                    :text-field-props="{ rules: dobRules }" />
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Giới tính</div>
                                <v-select v-model="employeeForm.gioiTinh" :readonly="isDetailView" :items="[
                                    { title: 'Nam', value: true },
                                    { title: 'Nữ', value: false }
                                ]" variant="outlined" density="compact" hide-details></v-select>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Vai trò</div>
                                <v-select v-model="employeeForm.idPhanQuyen" :readonly="isDetailView" :items="roles"
                                    item-title="title" item-value="value" variant="outlined" density="compact"
                                    hide-details></v-select>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="field-label">Tỉnh / Thành phố *</div>
                                <v-select v-model="employeeForm.tinh" :readonly="isDetailView" :items="provinces"
                                    item-title="name" item-value="code" placeholder="Chọn tỉnh/thành phố"
                                    variant="outlined" density="compact" hide-details
                                    :loading="loadingLocations.provinces"></v-select>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="field-label">Quận / Huyện *</div>
                                <v-select v-model="employeeForm.thanhPho" :readonly="isDetailView" :items="districts"
                                    item-title="name" item-value="code" placeholder="Chọn quận/huyện" variant="outlined"
                                    density="compact" hide-details :loading="loadingLocations.districts"
                                    :disabled="isDetailView || !employeeForm.tinh"></v-select>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="field-label">Phường / Xã *</div>
                                <v-select v-model="employeeForm.phuongXa" :readonly="isDetailView" :items="wards"
                                    item-title="name" item-value="code" placeholder="Chọn phường/xã" variant="outlined"
                                    density="compact" hide-details :loading="loadingLocations.wards"
                                    :disabled="isDetailView || !employeeForm.thanhPho"></v-select>
                            </v-col>
                            <v-col cols="12">
                                <div class="field-label">Địa chỉ chi tiết *</div>
                                <v-textarea v-model="employeeForm.diaChiChiTiet" :readonly="isDetailView"
                                    placeholder="Số nhà, tên đường..." variant="outlined" density="compact" rows="2"
                                    hide-details></v-textarea>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" lg="4">
                <v-card class="premium-card elevation-0 border border-slate-200 mb-6 text-center">
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
                                <v-img :src="employeeForm.hinhAnh || FB_DEFAULT_AVATAR" cover>
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
                                placeholder="Dán URL ảnh hoặc nhấn vào vòng tròn" variant="outlined" bg-color="white"
                                density="comfortable" hide-details class="bg-white"></v-text-field>
                            <p class="text-caption font-weight-medium text-slate-400 mt-3 px-1">
                                Ảnh đại diện sẽ hiển thị trên hồ sơ và thanh thực đơn cá nhân.
                            </p>
                        </div>
                    </v-card-text>
                </v-card>

                <v-card class="premium-card elevation-0 border border-slate-200 bg-slate-50 border-dashed">
                    <v-card-text class="pa-8 opacity-60">
                        <div class="d-flex align-center mb-2">
                            <v-icon color="slate-400" size="20" class="mr-2">mdi-history</v-icon>
                            <span class="text-caption font-weight-medium text-slate-500 uppercase tracking-wider">Hoạt
                                động cuối</span>
                        </div>
                        <div class="text-body-2 font-weight-medium text-slate-400">Chưa ghi nhận hoạt động nào của nhân
                            viên này.</div>
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
    box-shadow:
        0 20px 25px -5px rgba(0, 0, 0, 0.1),
        0 10px 10px -5px rgba(0, 0, 0, 0.04) !important;
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

:deep(input[type="date"]::-webkit-calendar-picker-indicator) {
    display: none;
    -webkit-appearance: none;
}

/* Ép màu đỏ tươi cho các cảnh báo lỗi */
#nhan-vien-form-container :deep(.v-messages__message) {
    color: #ef4444 !important; /* Đỏ tươi */
    font-weight: 500 !important;
    font-size: 11px !important;
}
#nhan-vien-form-container :deep(.v-field--error) {
    --v-field-border-color: #ef4444 !important;
}

:deep(.v-field),
:deep(.v-field__outline) {
    border-radius: 12px !important;
}

:deep(.v-field__input),
:deep(input),
:deep(input::placeholder),
:deep(textarea),
:deep(.v-select__selection-text),
:deep(.v-label) {
    font-size: 13px !important;
}

:deep(.v-field__input::placeholder) {
    font-size: 13px !important;
}

.field-label,
.field-label-small {
    font-size: 13px !important;
}

:global(.v-overlay-container .v-list-item-title),
:global(.v-overlay-container .v-list-item) {
    font-size: 13px !important;
}
</style>


