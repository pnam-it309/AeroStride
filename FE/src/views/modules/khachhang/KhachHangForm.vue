<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { PATH } from '@/router/routePaths';
import { useRoute, useRouter } from 'vue-router';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import { useNotifications } from '@/services/notificationService';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { ArrowLeftIcon, UserIcon, MapPinIcon, NoteIcon } from 'vue-tabler-icons';
import axios from 'axios';

import { dichVuFile } from '@/services/core/dichVuFile';
import QrcodeStream from '@/components/common/CCCDQRScanner';
import { parseCCCDQR } from '@/utils/cccdQR';

const FB_DEFAULT_AVATAR = 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);
const isEditMode = ref(false);
const isDetailView = computed(() => route.path.includes('/detail'));

// Address Management
const listDiaChi = ref([]);
const addrDialog = ref(false);
const addrForm = ref({
    id: null,
    tinh: null,
    thanhPho: null,
    phuongXa: null,
    diaChiChiTiet: '',
    tenNguoiNhan: '',
    sdtNguoiNhan: '',
    laMacDinh: false
});
const isEditAddr = ref(false);

const customerForm = ref({
    ma: '',
    ten: '',
    email: '',
    sdt: '',
    tenTaiKhoan: '',
    matKhau: '',
    ngaySinh: '',
    gioiTinh: true,
    trangThai: 'DANG_HOAT_DONG',
    tinh: null,
    thanhPho: null,
    phuongXa: null,
    diaChiChiTiet: '',
    ghiChu: '',
    hinhAnh: ''
});

// Quét QR CCCD
const showQR = ref(false);
function onDetectQR(detectedCodes) {
    if (!detectedCodes || detectedCodes.length === 0) return;
    const result = detectedCodes[0].rawValue;
    
    const info = parseCCCDQR(result);
    if (info) {
        customerForm.value.ten = info.ten;
        customerForm.value.ngaySinh = info.ngaySinh;
        customerForm.value.gioiTinh = info.gioiTinh;
        if (info.sdt) customerForm.value.sdt = info.sdt;
        // Map address if possible, or just place in detailed address for user to confirm
        customerForm.value.diaChiChiTiet = info.diaChi;
        
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
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể tải danh sách tỉnh/thành phố',
            color: 'error'
        });
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
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể tải danh sách quận/huyện',
            color: 'error'
        });
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
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể tải danh sách phường/xã',
            color: 'error'
        });
    } finally {
        loadingLocations.value.wards = false;
    }
};

const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    color: 'success',
    action: null
});

const loadCustomer = async (id) => {
    loading.value = true;
    try {
        const data = await dichVuKhachHang.layChiTietKhachHang(id);
        customerForm.value = { ...data };

        // Recovery location codes from names if possible (complex)
        // For now, if editing, we just show the names in the select if they exist
        isEditMode.value = true;

        // Initial fetch for locations if they exist
        if (data.tinh) {
            await fetchProvinces();
            const p = provinces.value.find((x) => x.name === data.tinh);
            if (p) {
                await fetchDistricts(p.code);
                const d = districts.value.find((x) => x.name === data.thanhPho);
                if (d) await fetchWards(d.code);
            }
        }

        // Load addresses if editing
        if (isEditMode.value || isDetailView.value) {
            loadAddresses(id);
        }
    } catch (error) {
        console.error('Error loading customer:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải thông tin khách hàng', color: 'error' });
    } finally {
        loading.value = false;
    }
};

const handleSave = () => {
    confirmDialog.value = {
        show: true,
        title: isEditMode.value ? 'Cập nhật khách hàng' : 'Thêm khách hàng mới',
        message: `Xác nhận lưu thông tin khách hàng [${customerForm.value.ten}]?`,
        color: 'primary',
        action: async () => {
            saving.value = true;
            try {
                // Map names before sending
                const payload = { ...customerForm.value };
                const p = provinces.value.find((x) => x.code === customerForm.value.tinh);
                const d = districts.value.find((x) => x.code === customerForm.value.thanhPho);
                const w = wards.value.find((x) => x.code === customerForm.value.phuongXa);

                if (p) payload.tinh = p.name;
                if (d) payload.thanhPho = d.name;
                if (w) payload.phuongXa = w.name;
                // validate address fields
                if (!payload.tinh || !payload.thanhPho || !payload.phuongXa || !payload.diaChiChiTiet) {
                    addNotification({
                        title: 'Thiếu thông tin',
                        subtitle: 'Vui lòng nhập đầy đủ địa chỉ',
                        color: 'warning'
                    });
                    saving.value = false;
                    return;
                }

                if (isEditMode.value) {
                    await dichVuKhachHang.capNhatKhachHang(route.params.id, payload);
                    addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật thông tin khách hàng', color: 'success' });
                } else {
                    await dichVuKhachHang.taoKhachHang(payload);
                    addNotification({ title: 'Thành công', subtitle: 'Đã thêm khách hàng mới', color: 'success' });
                }
                router.push(PATH.KHACH_HANG);
            } catch (error) {
                console.error('Error saving customer:', error);
                addNotification({
                    title: 'Lỗi',
                    subtitle: error.response?.data?.message || 'Có lỗi xảy ra khi lưu thông tin',
                    color: 'error'
                });
            } finally {
                saving.value = false;
                confirmDialog.value.show = false;
            }
        }
    }
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
        customerForm.value.hinhAnh = typeof res === 'object' ? res.fileUrl || res.url || res.data || res.secure_url : res;
        addNotification({ title: 'Thành công', subtitle: 'Đã tải lên ảnh đại diện', color: 'success' });
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải lên ảnh', color: 'error' });
    } finally {
        uploading.value = false;
    }
};

watch(
    () => customerForm.value.tinh,
    (newVal) => {
        customerForm.value.thanhPho = null;
        customerForm.value.phuongXa = null;
        if (newVal) fetchDistricts(newVal);
    }
);

watch(
    () => customerForm.value.thanhPho,
    (newVal) => {
        customerForm.value.phuongXa = null;
        if (newVal) fetchWards(newVal);
    }
);

const loadAddresses = async (khId) => {
    try {
        const data = await dichVuKhachHang.layDanhSachDiaChi(khId);

        listDiaChi.value = Array.isArray(data) ? data : data?.content ?? data?.data ?? [];

    } catch (e) {
        console.error('Error loading addresses:', e);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể tải danh sách địa chỉ',
            color: 'error'
        });
    }
};
const openAddrDialog = (item = null) => {
    if (item) {
        isEditAddr.value = true;
        addrForm.value = { ...item };
        // Fetch location data for selects
        const p = provinces.value.find((x) => x.name === item.tinh);
        if (p) {
            fetchDistricts(p.code).then(() => {
                const d = districts.value.find((x) => x.name === item.thanhPho);
                if (d) fetchWards(d.code);
            });
        }
    } else {
        isEditAddr.value = false;
        addrForm.value = {
            tinh: null,
            thanhPho: null,
            phuongXa: null,
            diaChiChiTiet: '',
            tenNguoiNhan: customerForm.value.ten,
            sdtNguoiNhan: customerForm.value.sdt,
            laMacDinh: listDiaChi.value.length === 0
        };
    }
    addrDialog.value = true;
};

const saveAddress = async () => {
    try {
        const payload = { ...addrForm.value, idKhachHang: route.params.id };
        const p = provinces.value.find((x) => x.code === addrForm.value.tinh);
        const d = districts.value.find((x) => x.code === addrForm.value.thanhPho);
        const w = wards.value.find((x) => x.code === addrForm.value.phuongXa);
        if (p) payload.tinh = p.name;
        if (d) payload.thanhPho = d.name;
        if (w) payload.phuongXa = w.name;

        if (isEditAddr.value) {
            await dichVuKhachHang.capNhatDiaChi(addrForm.value.id, payload);
        } else {
            await dichVuKhachHang.taoDiaChi(payload);
        }
        addNotification({ title: 'Thành công', subtitle: 'Đã lưu địa chỉ', color: 'success' });
        addrDialog.value = false;
        loadAddresses(route.params.id);
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể lưu địa chỉ', color: 'error' });
    }
};

const defaultAddress = computed(() => {
    if (!listDiaChi.value || listDiaChi.value.length === 0) return null;

    // ưu tiên mặc định
    const macDinh = listDiaChi.value.find((x) => x.laMacDinh === true);

    // nếu không có thì lấy cái đầu
    return macDinh || listDiaChi.value[0];
});

const handleSetDefault = async (id) => {
    try {
        await dichVuKhachHang.datDiaChiMacDinh(id);
        loadAddresses(route.params.id);
    } catch (e) {
        console.error('Error setting default address:', e);
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể đặt địa chỉ mặc định',
            color: 'error'
        });
    }
};

const handleDeleteAddr = (id) => {
    confirmDialog.value = {
        show: true,
        title: 'Xóa địa chỉ',
        message: 'Bạn có chắc muốn xóa địa chỉ này?',
        color: 'error',
        action: async () => {
            try {
                await dichVuKhachHang.xoaDiaChi(id);
                loadAddresses(route.params.id);
                confirmDialog.value.show = false;
            } catch (e) {}
        }
    };
};

onMounted(() => {
    fetchProvinces();
    if (route.params.id) {
        loadCustomer(route.params.id);
    }
});
</script>

<template>
    <v-container fluid class="pa-6 animate-fade-in overflow-y-auto font-body" style="height: 100vh;">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý tài khoản', disabled: false, href: '#' },
            { title: 'Khách hàng', disabled: false, to: PATH.KHACH_HANG },
            { title: isDetailView ? 'Chi tiết' : isEditMode ? 'Cập nhật' : 'Thêm mới', disabled: true }
        ]" />

        <!-- Action Header -->
        <div class="d-flex align-center justify-space-between mb-8 mt-4">
            <div class="d-flex align-center gap-4">
                <v-btn icon variant="flat" @click="router.push(PATH.KHACH_HANG)" class="btn-back-header">
                    <ArrowLeftIcon size="20" />
                </v-btn>
            </div>
            <div class="d-flex gap-3">
                <v-btn v-if="!isDetailView" color="success" variant="flat"
                    class="text-none font-weight-bold text-white px-8 rounded-xl h-11 elevation-4" @click="showQR = true">
                    <v-icon size="20" class="mr-2 text-white">mdi-qrcode-scan</v-icon>
                    Quét QR CCCD
                </v-btn>
                <v-btn v-if="!isDetailView" color="primary" variant="flat"
                    class="text-none font-weight-bold text-white px-8 rounded-xl h-11 elevation-4" :loading="saving"
                    @click="handleSave">
                    <v-icon size="18" class="mr-2 text-white">mdi-check-all</v-icon>
                    Lưu hồ sơ khách hàng
                </v-btn>
                <v-btn v-if="isDetailView" color="primary" variant="flat"
                    class="text-none font-weight-bold text-white px-8 rounded-xl h-11 elevation-4"
                    @click="router.push(`${PATH.KHACH_HANG_FORM}/${route.params.id}`)">
                    <v-icon size="18" class="mr-2 text-white">mdi-pencil</v-icon>
                    Chỉnh sửa hồ sơ
                </v-btn>
            </div>
        </div>

        <!-- Dialog quét QR CCCD -->
        <v-dialog v-model="showQR" max-width="500" transition="dialog-bottom-transition">
            <v-card class="filter-card elevation-0">
                <v-card-title class="pa-6 font-weight-bold border-b">
                    <v-icon start color="success" class="mr-2">mdi-qrcode-scan</v-icon>
                    Quét mã QR CCCD
                </v-card-title>
                <v-card-text class="pa-6">
                    <div class="border-4 border-dashed rounded-lg overflow-hidden mb-4">
                        <QrcodeStream @detect="onDetectQR" @init="onInitQR" />
                    </div>
                    <div class="text-caption font-weight-bold text-slate-400 text-center">Đưa mã QR CCCD vào camera để tự động nhận diện thông tin.</div>
                </v-card-text>
                <v-divider></v-divider>
                <v-card-actions class="pa-4 bg-slate-50">
                    <v-spacer></v-spacer>
                    <v-btn color="slate-400" variant="text" class="text-none font-weight-medium" @click="showQR = false">Hủy bỏ</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <v-row v-if="loading">
            <v-col cols="12" class="text-center py-16">
                <v-progress-circular indeterminate color="primary" size="64" width="6"></v-progress-circular>
                <div class="mt-4 text-subtitle-1 font-weight-medium text-slate-500">Đang tải hồ sơ khách hàng...</div>
            </v-col>
        </v-row>

        <!-- DASHBOARD VIEW (DETAIL) -->
        <v-row v-else-if="isDetailView">
            <v-col cols="12" lg="4">
                <!-- Customer Profile Summary -->
                <v-card class="filter-card elevation-0 mb-6 text-center">
                    <v-card-text class="pa-8">
                        <v-avatar size="140" color="blue-lighten-5" class="mb-4 border-xl border-white elevation-4">
                            <v-img :src="customerForm.hinhAnh || FB_DEFAULT_AVATAR"></v-img>
                        </v-avatar>
                        <h2 class="text-h5 font-weight-bold mb-1 text-slate-800">{{ customerForm.ten }}</h2>
                        <div class="text-subtitle-2 font-weight-medium text-slate-400 mb-6">{{ customerForm.email }}</div>

                        <v-chip :color="customerForm.trangThai === 'DANG_HOAT_DONG' ? 'success' : 'error'" variant="flat"
                            class="px-8 font-weight-medium rounded-lg mb-8">
                            {{ customerForm.trangThai === 'DANG_HOAT_DONG' ? 'Đang hoạt động' : 'Tạm dừng / Khóa' }}
                        </v-chip>

                        <v-divider class="mb-6 opacity-50"></v-divider>

                        <div class="text-left">
                            <div class="d-flex align-center mb-4 bg-slate-50 pa-3 rounded-xl border">
                                <div class="icon-blob bg-success-lighten-4 mr-3">
                                    <v-icon color="success" size="18">mdi-phone</v-icon>
                                </div>
                                <div>
                                    <div class="text-caption font-weight-medium text-slate-400 uppercase tracking-wider">SỐ ĐIỆN THOẠI</div>
                                    <div class="text-subtitle-2 font-weight-bold text-slate-700">{{ customerForm.sdt }}
                                    </div>
                                </div>
                            </div>
                            <div class="d-flex align-center bg-slate-50 pa-3 rounded-xl border">
                                <div class="icon-blob bg-info-lighten-4 mr-3">
                                    <v-icon color="info" size="18">mdi-calendar-range</v-icon>
                                </div>
                                <div>
                                    <div class="text-caption font-weight-medium text-slate-400 uppercase tracking-wider">NGÀY SINH</div>
                                    <div class="text-subtitle-2 font-weight-bold text-slate-700">{{ customerForm.ngaySinh
                                        || 'Chưa cập nhật' }}</div>
                                </div>
                            </div>
                        </div>
                    </v-card-text>
                </v-card>

                <v-card class="filter-card elevation-0">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-4">
                            <div class="icon-blob bg-slate-100 mr-3">
                                <NoteIcon size="18" class="text-slate-600" />
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Ghi chú hệ thống</span>
                        </div>
                        <div class="text-body-2 font-weight-medium text-slate-500 leading-relaxed">
                            {{ customerForm.ghiChu || 'Chưa có ghi chú nào cho khách hàng này.' }}
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" lg="8">
                <!-- Delivery Address Dashboard -->
                <v-card class="filter-card elevation-0 mb-6 pb-2">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <MapPinIcon class="text-primary" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Danh sách địa chỉ nhận hàng</span>
                            <v-chip color="primary" size="small" variant="tonal" class="ml-3 font-weight-medium">
                                {{ listDiaChi.length || 0 }} Địa chỉ
                            </v-chip>
                        </div>

                        <v-divider class="mb-6 opacity-30"></v-divider>

                        <!-- Không có địa chỉ -->
                        <div v-if="!listDiaChi || listDiaChi.length === 0" class="text-center py-16 bg-slate-50 rounded-xl border-dashed border">
                            <v-icon size="48" color="slate-200">mdi-map-marker-off</v-icon>
                            <div class="mt-4 text-slate-400 font-weight-medium">Khách hàng chưa đăng ký địa chỉ nhận hàng</div>
                        </div>

                        <!-- Hiển thị toàn bộ địa chỉ -->
                        <div v-else v-for="addr in listDiaChi" :key="addr.id"
                            class="mb-4 pa-5 border rounded-xl d-flex align-center gap-4 bg-white shadow-sm hover-addr-card transition-all">
                            <v-avatar color="primary" class="mr-2 elevation-2" size="36">
                                <v-icon color="white" size="18">mdi-map-marker</v-icon>
                            </v-avatar>
                            <div class="flex-grow-1">
                                <div class="d-flex align-center gap-2 mb-1">
                                    <span class="font-weight-bold text-slate-800">{{ addr.tenNguoiNhan }}</span>
                                    <span class="text-caption font-weight-medium text-slate-400 px-2 border-l ml-1"> {{
                                        addr.sdtNguoiNhan }}</span>
                                    <v-chip v-if="addr.laMacDinh" color="success" size="x-small" variant="flat"
                                        class="ml-2 font-weight-medium px-3">MẶC ĐỊNH</v-chip>
                                </div>
                                <div class="text-body-2 font-weight-medium text-slate-500">
                                    {{ addr.diaChiChiTiet }}, {{ addr.phuongXa }}, {{ addr.thanhPho }}, {{ addr.tinh }}
                                </div>
                            </div>
                        </div>
                    </v-card-text>
                </v-card>

                <!-- Account Security Block -->
                <v-card class="filter-card elevation-0">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-slate-100 mr-3">
                                <v-icon color="slate-600" size="18">mdi-shield-account</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Thông tin tài khoản</span>
                        </div>
                        <v-row>
                            <v-col cols="12" md="6">
                                <div class="bg-slate-50 pa-4 rounded-xl border">
                                    <div class="text-caption font-weight-medium text-slate-400 mb-1 uppercase tracking-wider">TÊN ĐĂNG NHẬP</div>
                                    <div class="text-h6 font-weight-bold text-primary">{{ customerForm.tenTaiKhoan ||
                                        'Chưa thiết lập' }}</div>
                                </div>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="bg-slate-50 pa-4 rounded-xl border d-flex align-center">
                                    <div class="flex-grow-1">
                                        <div class="text-caption font-weight-medium text-slate-400 mb-1 uppercase tracking-wider">MẬT KHẨU</div>
                                        <div class="text-subtitle-2 text-slate-400 italic font-italic font-weight-medium">
                                            Dữ liệu được mã hóa bảo mật
                                        </div>
                                    </div>
                                    <v-icon color="slate-300">mdi-lock-check</v-icon>
                                </div>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <!-- FORM VIEW (CREATE/EDIT) -->
        <v-row v-else class="pb-16">
            <v-col cols="12" lg="8">
                <!-- Basic Info -->
                <v-card class="filter-card elevation-0 mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <UserIcon class="text-primary" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Thông tin cá nhân</span>
                        </div>

                        <v-row>
                            <v-col cols="12" md="4">
                                <div class="field-label">Mã khách hàng</div>
                                <v-text-field v-model="customerForm.ma" readonly placeholder="KH-XXXX" variant="outlined"
                                    density="compact" class="font-weight-medium bg-slate-50 mono-font"
                                    hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="8">
                                <div class="field-label">Họ và tên *</div>
                                <v-text-field v-model="customerForm.ten" :readonly="isDetailView"
                                    placeholder="Ví dụ: Nguyễn Văn A" variant="outlined" density="compact"
                                    hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Email *</div>
                                <v-text-field v-model="customerForm.email" :readonly="isDetailView"
                                    placeholder="khachhang@gmail.com" variant="outlined" density="compact"
                                    hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Số điện thoại *</div>
                                <v-text-field v-model="customerForm.sdt" :readonly="isDetailView"
                                    placeholder="09xx.xxx.xxx" variant="outlined" density="compact"
                                    hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Ngày sinh</div>
                                <v-text-field v-model="customerForm.ngaySinh" :readonly="isDetailView" type="date"
                                    variant="outlined" density="compact" 
                                    hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Giới tính</div>
                                <v-select v-model="customerForm.gioiTinh" :readonly="isDetailView" :items="[
                                    { title: 'Nam', value: true },
                                    { title: 'Nữ', value: false }
                                ]" variant="outlined" density="compact" 
                                    hide-details></v-select>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <!-- Address Info Card -->
                <v-card class="filter-card elevation-0 mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-amber-lighten-5 mr-3">
                                <MapPinIcon class="text-amber-darken-3" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Sổ địa chỉ</span>
                            <v-spacer></v-spacer>
                            <v-btn v-if="isEditMode" color="primary" variant="tonal" size="small"
                                class="text-none font-weight-bold rounded-lg" prepend-icon="mdi-plus"
                                @click="openAddrDialog()">
                                Thêm địa chỉ mới
                            </v-btn>
                        </div>

                        <div v-if="!isEditMode && !isDetailView" class="mb-4 bg-slate-50 pa-6 rounded-xl border-dashed border">
                            <v-row>
                                <v-col cols="12" md="4">
                                    <v-autocomplete v-model="customerForm.tinh" :items="provinces" item-title="name"
                                        item-value="code" label="Tỉnh / Thành phố" variant="outlined" density="compact"
                                        @update:model-value="(val) => { customerForm.thanhPho = null; customerForm.phuongXa = null; if (val) fetchDistricts(val); }" />
                                </v-col>

                                <v-col cols="12" md="4">
                                    <v-autocomplete v-model="customerForm.thanhPho" :items="districts" item-title="name"
                                        item-value="code" label="Quận / Huyện" variant="outlined" density="compact"
                                        :disabled="!customerForm.tinh"
                                        @update:model-value="(val) => { customerForm.phuongXa = null; if (val) fetchWards(val); }" />
                                </v-col>

                                <v-col cols="12" md="4">
                                    <v-autocomplete v-model="customerForm.phuongXa" :items="wards" item-title="name"
                                        item-value="code" label="Phường / Xã" variant="outlined" density="compact"
                                        :disabled="!customerForm.thanhPho" />
                                </v-col>

                                <v-col cols="12">
                                    <v-textarea v-model="customerForm.diaChiChiTiet" label="Địa chỉ cụ thể (Số nhà, đường...)"
                                        variant="outlined" rows="2" hide-details />
                                </v-col>
                            </v-row>
                        </div>

                        <div v-else-if="listDiaChi.length === 0" class="text-center py-12 bg-slate-50 rounded-xl border-dashed border">
                            <v-icon size="48" color="slate-200">mdi-map-marker-off</v-icon>
                            <div class="mt-2 text-slate-400 font-weight-medium">Chưa có địa chỉ nào được đăng ký</div>
                        </div>

                        <div v-else v-for="addr in listDiaChi" :key="addr.id"
                            class="mb-4 pa-5 border rounded-xl d-flex align-center gap-4 bg-white shadow-sm hover-addr-card transition-all">
                            <v-avatar color="primary" class="mr-2 elevation-2" size="36">
                                <v-icon color="white" size="18">mdi-map-marker</v-icon>
                            </v-avatar>
                            <div class="flex-grow-1">
                                <div class="d-flex align-center gap-2 mb-1">
                                    <span class="font-weight-black text-slate-800">{{ addr.tenNguoiNhan }}</span>
                                    <span class="text-caption font-weight-bold text-slate-400 px-2 border-l ml-1"> {{
                                        addr.sdtNguoiNhan }}</span>
                                    <v-chip v-if="addr.laMacDinh" color="success" size="x-small" variant="flat"
                                        class="ml-2 font-weight-black px-3">MẶC ĐỊNH</v-chip>
                                </div>
                                <div class="text-body-2 font-weight-bold text-slate-500">
                                    {{ addr.diaChiChiTiet }}, {{ addr.phuongXa }}, {{ addr.thanhPho }}, {{ addr.tinh }}
                                </div>
                            </div>
                            <div class="d-flex align-center gap-1" v-if="!isDetailView">
                                <v-btn icon variant="text" size="small" color="primary" @click="openAddrDialog(addr)"
                                    class="action-icon-btn">
                                    <v-icon size="18">mdi-pencil</v-icon>
                                </v-btn>
                                <v-btn v-if="!addr.laMacDinh" icon variant="text" size="small" color="error"
                                    @click="handleDeleteAddr(addr.id)" class="action-icon-btn">
                                    <v-icon size="18">mdi-delete</v-icon>
                                </v-btn>
                                <v-btn v-if="!addr.laMacDinh" variant="tonal" size="x-small" color="info"
                                    class="text-none ml-2 font-weight-bold h-8 px-3 rounded-lg"
                                    @click="handleSetDefault(addr.id)">
                                    Mặc định
                                </v-btn>
                            </div>
                        </div>
                    </v-card-text>
                </v-card>

                <!-- Notes -->
                <v-card class="filter-card elevation-0">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-slate-100 mr-3">
                                <NoteIcon class="text-slate-600" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Ghi chú & Thông tin thêm</span>
                        </div>
                        <v-textarea v-model="customerForm.ghiChu" :readonly="isDetailView"
                            placeholder="Ghi chú về khách hàng (Sở thích, lưu ý giao hàng...)" variant="outlined" rows="3"
                            hide-details></v-textarea>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" lg="4">
                <v-card class="filter-card elevation-0 mb-6">
                    <v-card-text class="pa-8 text-center">
                        <div class="section-header d-flex align-center mb-6 text-left">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <v-icon color="primary" size="18">mdi-camera</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Ảnh chân dung</span>
                        </div>

                        <div class="position-relative d-inline-block mx-auto mb-6">
                            <v-avatar size="160" color="blue-lighten-5"
                                class="border-xl border-white elevation-6 cursor-pointer avatar-hover transition-all overflow-hidden"
                                @click="handleFileClick">
                                <v-img :src="customerForm.hinhAnh || FB_DEFAULT_AVATAR" cover>
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
                            <v-text-field v-if="!isDetailView" v-model="customerForm.hinhAnh"
                                placeholder="Dán URL ảnh hoặc nhấn vào vòng tròn phía trên" variant="outlined"
                                density="compact" hide-details class="font-weight-medium bg-slate-50"></v-text-field>
                            <p class="text-caption font-weight-medium text-slate-400 mt-2 px-1">Gợi ý: Sử dụng ảnh .jpg hoặc
                                .png chất lượng cao.</p>
                        </div>
                    </v-card-text>
                </v-card>

                <v-card class="filter-card elevation-0 bg-primary-lighten-5 border-primary-lighten-4">
                    <v-card-text class="pa-8">
                        <div class="d-flex align-center mb-4">
                            <v-icon color="primary" size="24" class="mr-3">mdi-shield-check</v-icon>
                            <span class="text-subtitle-1 font-weight-bold text-primary">Bảo mật tài khoản</span>
                        </div>
                        <p class="text-body-2 font-weight-bold text-slate-600 mb-0">Hệ thống sẽ tự động khởi tạo các thông số bảo mật khi hồ sơ được tạo thành công.</p>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <!-- SHARED CONFIRM -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" @confirm="confirmDialog.action" />

        <!-- ADDRESS DIALOG -->
        <v-dialog v-model="addrDialog" max-width="600" transition="dialog-bottom-transition">
            <v-card class="filter-card elevation-24">
                <v-card-title class="pa-6 font-weight-black border-b text-primary d-flex align-center">
                    <v-icon start class="mr-3">mdi-map-marker-plus</v-icon>
                    {{
                    isEditAddr ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ mới'
                }}</v-card-title>
                <v-card-text class="pa-6">
                    <v-row>
                        <v-col cols="12" md="6">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Tên người nhận *</div>
                            <v-text-field
                                v-model="addrForm.tenNguoiNhan"
                                placeholder="Nhập tên"
                                variant="outlined"
                                density="compact"
                                class="font-weight-medium"
                                hide-details
                            ></v-text-field>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Số điện thoại *</div>
                            <v-text-field
                                v-model="addrForm.sdtNguoiNhan"
                                placeholder="09xx..."
                                variant="outlined"
                                density="compact"
                                class="font-weight-medium"
                                hide-details
                            ></v-text-field>
                        </v-col>
                        <v-col cols="12" md="4">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Tỉnh / Thành phố *</div>
                            <v-autocomplete
                                v-model="addrForm.tinh"
                                :items="provinces"
                                item-title="name"
                                item-value="code"
                                placeholder="Chọn"
                                variant="outlined"
                                density="compact"
                                hide-details
                                :loading="loadingLocations.provinces"
                                @update:model-value="
                                    (val) => {
                                        addrForm.thanhPho = null;
                                        addrForm.phuongXa = null;
                                        if (val) fetchDistricts(val);
                                    }
                                "
                            ></v-autocomplete>
                        </v-col>
                        <v-col cols="12" md="4">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Quận / Huyện *</div>
                            <v-autocomplete
                                v-model="addrForm.thanhPho"
                                :items="districts"
                                item-title="name"
                                item-value="code"
                                placeholder="Chọn"
                                variant="outlined"
                                density="compact"
                                hide-details
                                :loading="loadingLocations.districts"
                                :disabled="!addrForm.tinh"
                                @update:model-value="
                                    (val) => {
                                        addrForm.phuongXa = null;
                                        if (val) fetchWards(val);
                                    }
                                "
                            ></v-autocomplete>
                        </v-col>
                        <v-col cols="12" md="4">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Phường / Xã *</div>
                            <v-autocomplete
                                v-model="addrForm.phuongXa"
                                :items="wards"
                                item-title="name"
                                item-value="code"
                                placeholder="Chọn"
                                variant="outlined"
                                density="compact"
                                hide-details
                                :loading="loadingLocations.wards"
                                :disabled="!addrForm.thanhPho"
                            ></v-autocomplete>
                        </v-col>
                        <v-col cols="12">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Địa chỉ cụ thể *</div>
                            <v-textarea
                                v-model="addrForm.diaChiChiTiet"
                                placeholder="Số nhà, đường..."
                                variant="outlined"
                                rows="2"
                                class="font-weight-medium"
                                hide-details
                            ></v-textarea>
                        </v-col>
                        <v-col cols="12" v-if="listDiaChi.length > 0">
                            <v-checkbox
                                v-model="addrForm.laMacDinh"
                                label="Đặt làm địa chỉ mặc định"
                                color="primary"
                                hide-details
                                density="compact"
                            ></v-checkbox>
                        </v-col>
                    </v-row>
                </v-card-text>
                <v-divider></v-divider>
                <v-card-actions class="pa-4 bg-grey-lighten-4">
                    <v-spacer></v-spacer>
                    <v-btn variant="text" class="text-none font-weight-medium" @click="addrDialog = false">Hủy</v-btn>
                    <v-btn
                        color="primary"
                        variant="flat"
                        class="px-6 text-none font-weight-medium rounded-lg"
                        @click="saveAddress"
                        :disabled="
                            !addrForm.tenNguoiNhan ||
                            !addrForm.sdtNguoiNhan ||
                            !addrForm.tinh ||
                            !addrForm.thanhPho ||
                            !addrForm.phuongXa ||
                            !addrForm.diaChiChiTiet
                        "
                        >Lưu địa chỉ</v-btn
                    >
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-container>
</template>

<style scoped>
/* Scoped styles removed in favor of global _admin-common.scss */
.italic {
    font-style: italic;
}
.camera-icon-bubble {
    border: 4px solid #fff;
    cursor: pointer;
    box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);
}
</style>

