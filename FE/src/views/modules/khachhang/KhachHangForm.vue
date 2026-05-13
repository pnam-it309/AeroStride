<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { PATH } from '@/router/routePaths';
import { useRoute, useRouter } from 'vue-router';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import { useNotifications } from '@/services/notificationService';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { ArrowLeftIcon, UserIcon, MapPinIcon, NoteIcon, PlusIcon, EditIcon, TrashIcon, StarIcon } from 'vue-tabler-icons';
import axios from 'axios';

import { dichVuFile } from '@/services/core/dichVuFile';


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

const matchLocation = (list, name) => {
    if (!name) return null;
    const cleanN = cleanName(name);
    if (cleanN.includes('hcm') || cleanN.includes('hồ chí minh')) {
        return list.find(x => cleanName(x.name).includes('hồ chí minh') || cleanName(x.name).includes('hcm'));
    }
    return list.find(x => cleanName(x.name).includes(cleanN) || cleanN.includes(cleanName(x.name)));
};

const loading = ref(false);
const saving = ref(false);
const isEditMode = ref(false);
const isDetailView = computed(() => route.path.includes('/detail') || route.query.view === 'true');
const submitButtonText = computed(() => isEditMode.value ? 'Cập nhật khách hàng' : 'Thêm khách hàng');

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
const invoiceDetailDialog = ref(false);
const selectedInvoice = ref(null);
const detailLoading = ref(false);


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
        customerForm.value = {
            ...data,
            tinh: null,
            thanhPho: null,
            phuongXa: null,
            ma: data.ma || data.maKhachHang || '', // Đảm bảo luôn có trường 'ma' để dùng làm khóa tìm kiếm
        };
        isEditMode.value = true;

        // Map string names to codes for v-autocomplete, or parse from full address string
        if (data.tinh || data.thanhPho || data.phuongXa) {
            if (provinces.value.length === 0) {
                await fetchProvinces();
            }
            const province = provinces.value.find(p => cleanName(p.name) === cleanName(data.tinh) || p.code === data.tinh);
            if (province) {
                customerForm.value.tinh = province.code;
                await fetchDistricts(province.code);
                const district = districts.value.find(d => cleanName(d.name) === cleanName(data.thanhPho) || d.code === data.thanhPho);
                if (district) {
                    customerForm.value.thanhPho = district.code;
                    await fetchWards(district.code);
                    const ward = wards.value.find(w => cleanName(w.name) === cleanName(data.phuongXa) || w.code === data.phuongXa);
                    if (ward) {
                        customerForm.value.phuongXa = ward.code;
                    }
                }
            }
        } else {
            let addressStr = data.diaChiChiTiet || data.diaChi || '';
            if (addressStr && addressStr.includes(',')) {
                const parts = addressStr.split(',').map(p => p.trim());
                if (parts.length >= 4) {
                    if (provinces.value.length === 0) {
                        await fetchProvinces();
                    }
                    const pName = parts[parts.length - 1];
                    const province = matchLocation(provinces.value, pName);
                    if (province) {
                        customerForm.value.tinh = province.code;
                        await fetchDistricts(province.code);
                        
                        const dName = parts[parts.length - 2];
                        const district = matchLocation(districts.value, dName);
                        if (district) {
                            customerForm.value.thanhPho = district.code;
                            await fetchWards(district.code);
                            
                            const wName = parts[parts.length - 3];
                            const ward = matchLocation(wards.value, wName);
                            if (ward) {
                                customerForm.value.phuongXa = ward.code;
                                customerForm.value.diaChiChiTiet = parts.slice(0, parts.length - 3).join(', ');
                            }
                        }
                    }
                }
            }
        }

        // Đồng bộ danh sách địa chỉ (thử mọi trường có thể)
        const rawAddresses = data.listDiaChi || data.diaChis || data.diaChiList || [];
        if (Array.isArray(rawAddresses) && rawAddresses.length > 0) {
            listDiaChi.value = [...rawAddresses];
        }

        // Tải thêm từ API riêng (nếu API này trả về thì sẽ cập nhật sau)
        if (isEditMode.value) {
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
                if (!isEditMode.value || listDiaChi.value.length === 0) {
                    if (!payload.tinh || !payload.thanhPho || !payload.phuongXa || !payload.diaChiChiTiet) {
                        addNotification({
                            title: 'Thiếu thông tin',
                            subtitle: 'Vui lòng nhập đầy đủ địa chỉ',
                            color: 'warning'
                        });
                        saving.value = false;
                        return;
                    }
                }

                if (isEditMode.value) {
                    await dichVuKhachHang.capNhatKhachHang(route.params.id, payload);
                    
                    // Nếu là edit mode mà chưa có địa chỉ, tự động tạo địa chỉ từ form
                    if (listDiaChi.value.length === 0 && payload.tinh && payload.thanhPho && payload.phuongXa && payload.diaChiChiTiet) {
                        try {
                            await dichVuKhachHang.taoDiaChi({
                                idKhachHang: route.params.id,
                                tinh: payload.tinh,
                                thanhPho: payload.thanhPho,
                                phuongXa: payload.phuongXa,
                                diaChiChiTiet: payload.diaChiChiTiet,
                                tenNguoiNhan: payload.ten || customerForm.value.ten,
                                sdtNguoiNhan: payload.sdt || customerForm.value.sdt,
                                laMacDinh: true
                            });
                        } catch (e) {
                            console.error('Lỗi tự động tạo địa chỉ:', e);
                        }
                    }

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
    };
};

const fileInput = ref(null);
const uploading = ref(false);

const handleFileClick = () => {
    fileInput.value.click();
};

// Hàm mở trình chọn ngày khi bấm vào icon
const openDatePicker = (event) => {
    const container = event.target.closest('.v-input');
    const input = container ? container.querySelector('input[type="date"]') : null;
    if (input) {
        if (typeof input.showPicker === 'function') input.showPicker();
        else input.click();
    }
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
        const res = await dichVuKhachHang.layDanhSachDiaChi(khId);
        
        let data = [];
        if (Array.isArray(res)) {
            data = res;
        } else if (res?.data) {
            if (Array.isArray(res.data)) data = res.data;
            else if (Array.isArray(res.data.content)) data = res.data.content;
            else if (Array.isArray(res.data.items)) data = res.data.items;
        } else if (Array.isArray(res?.content)) {
            data = res.content;
        }

        if (Array.isArray(data) && data.length > 0) {
            listDiaChi.value = [...data];
        }
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
    if (!addrForm.value.tenNguoiNhan || !addrForm.value.sdtNguoiNhan || !addrForm.value.tinh || !addrForm.value.thanhPho || !addrForm.value.phuongXa || !addrForm.value.diaChiChiTiet) {
        addNotification({
            title: 'Thiếu thông tin',
            subtitle: 'Vui lòng nhập đầy đủ tất cả các trường bắt buộc',
            color: 'warning'
        });
        return;
    }
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
            } catch (e) { }
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
    <v-container id="khach-hang-form-container" fluid class="pa-6 animate-fade-in overflow-y-auto font-body"
        style="height: 100vh">
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý tài khoản', disabled: false, href: '#' },
            { title: 'Khách hàng', disabled: false, to: PATH.KHACH_HANG },
            { title: isEditMode ? 'Cập nhật' : 'Thêm mới', disabled: true }
        ]" />

        <!-- Action Header -->
        <div class="d-flex align-center justify-space-between mb-8 mt-4">
            <div class="d-flex align-center gap-4">
                <v-btn icon variant="flat" @click="router.push(PATH.KHACH_HANG)" class="btn-back-header">
                    <ArrowLeftIcon size="20" />
                </v-btn>
            </div>
            <div class="d-flex gap-3">
                <v-btn color="primary" variant="flat"
                    class="add-btn-primary text-none px-8 rounded-xl h-11 elevation-4" 
                    style="font-size: 16px !important; font-weight: 600 !important;"
                    :loading="saving"
                    @click="handleSave">
                    <v-icon size="18" class="mr-2">mdi-check-all</v-icon>
                    <span style="font-size: 16px !important; font-weight: 600 !important;">{{ submitButtonText }}</span>
                </v-btn>
            </div>

        </div>

        <!-- Row 1: Personal Info & Portrait -->
        <v-row class="mb-6 d-flex align-stretch">
            <v-col cols="12" lg="8" class="d-flex">
                <!-- Basic Info -->
                <v-card class="filter-card elevation-0 w-100 d-flex flex-column mb-0">
                    <v-card-text class="pa-8 flex-grow-1">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <UserIcon class="text-primary" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Thông tin cá nhân</span>
                        </div>

                        <v-row>
                            <v-col cols="12" md="4">
                                <div class="field-label">Mã khách hàng</div>
                                <v-text-field v-model="customerForm.ma" :readonly="isDetailView" placeholder="KH-XXXX"
                                    variant="outlined" density="compact"
                                    class="font-weight-medium bg-slate-50 mono-font" hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="8">
                                <div class="field-label">Họ và tên *</div>
                                <v-text-field v-model="customerForm.ten" :readonly="isDetailView" placeholder="Ví dụ: Nguyễn Văn A"
                                    variant="outlined" density="compact" hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Email *</div>
                                <v-text-field v-model="customerForm.email" :readonly="isDetailView" placeholder="khachhang@gmail.com"
                                    variant="outlined" density="compact" hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Số điện thoại *</div>
                                <v-text-field v-model="customerForm.sdt" :readonly="isDetailView" placeholder="09xx.xxx.xxx" variant="outlined"
                                    density="compact" hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Ngày sinh</div>
                                <v-text-field 
                                v-model="customerForm.ngaySinh" 
                                :readonly="isDetailView" 
                                type="date"
                                append-inner-icon="mdi-calendar" 
                                @click:append-inner="openDatePicker"
                                variant="outlined" 
                                density="compact" 
                                hide-details
                                clearable
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Giới tính</div>
                                <v-select v-model="customerForm.gioiTinh" :readonly="isDetailView" :items="[
                                    { title: 'Nam', value: true },
                                    { title: 'Nữ', value: false }
                                ]" variant="outlined" density="compact" hide-details></v-select>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" lg="4" class="d-flex">
                <v-card class="filter-card elevation-0 w-100 d-flex flex-column mb-0">
                    <v-card-text class="pa-8 text-center flex-grow-1">
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
                            <p class="text-caption font-weight-medium text-slate-400 mt-2 px-1">
                                Gợi ý: Sử dụng ảnh .jpg hoặc .png chất lượng cao.
                            </p>
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <!-- Row 2: Address Book & (Notes + Security) -->
        <v-row class="pb-16 d-flex align-stretch">
            <v-col cols="12" lg="8" class="d-flex">
                <!-- Address Info Card -->
                <v-card class="filter-card elevation-0 w-100 d-flex flex-column mb-0">
                    <v-card-text class="pa-8 flex-grow-1">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-amber-lighten-5 mr-3">
                                <MapPinIcon class="text-amber-darken-3" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Số địa chỉ</span>
                            <v-spacer></v-spacer>
                            <v-btn v-if="isEditMode" variant="flat" color="primary" size="small"
                                class="add-btn-primary text-none"
                                @click="openAddrDialog()">
                                <PlusIcon size="18" class="mr-2" />
                                Thêm địa chỉ mới
                            </v-btn>
                        </div>

                        <div v-if="(!isEditMode || listDiaChi.length === 0) && !isDetailView" class="mb-4">
                            <v-row>
                                <v-col cols="12" md="4">
                                    <div class="field-label">Tỉnh / Thành phố *</div>
                                    <v-autocomplete v-model="customerForm.tinh" :items="provinces" item-title="name"
                                        item-value="code" placeholder="Chọn Tỉnh / Thành phố" variant="outlined"
                                        density="compact" @update:model-value="
                                            (val) => {
                                                customerForm.thanhPho = null;
                                                customerForm.phuongXa = null;
                                                if (val) fetchDistricts(val);
                                            }
                                        " />
                                </v-col>

                                <v-col cols="12" md="4">
                                    <div class="field-label">Quận / Huyện *</div>
                                    <v-autocomplete v-model="customerForm.thanhPho" :items="districts"
                                        item-title="name" item-value="code" placeholder="Chọn Quận / Huyện"
                                        variant="outlined" density="compact" :disabled="!customerForm.tinh"
                                        @update:model-value="
                                            (val) => {
                                                customerForm.phuongXa = null;
                                                if (val) fetchWards(val);
                                            }
                                        " />
                                </v-col>

                                <v-col cols="12" md="4">
                                    <div class="field-label">Phường / Xã *</div>
                                    <v-autocomplete v-model="customerForm.phuongXa" :items="wards" item-title="name"
                                        item-value="code" placeholder="Chọn Phường / Xã" variant="outlined"
                                        density="compact" :disabled="!customerForm.thanhPho" />
                                </v-col>

                                <v-col cols="12">
                                    <div class="field-label">Địa chỉ cụ thể (Số nhà, đường...) *</div>
                                    <v-textarea v-model="customerForm.diaChiChiTiet"
                                        placeholder="Nhập địa chỉ cụ thể" variant="outlined" rows="2"
                                        hide-details />
                                </v-col>
                            </v-row>
                        </div>

                        <div v-else-if="listDiaChi.length > 0" v-for="addr in listDiaChi" :key="addr.id"
                            class="mb-4 pa-5 border rounded-xl d-flex align-center gap-4 bg-white shadow-sm hover-addr-card transition-all">
                            <v-avatar color="primary" class="mr-2 elevation-2" size="36">
                                <v-icon color="white" size="18">mdi-map-marker</v-icon>
                            </v-avatar>
                            <div class="flex-grow-1">
                                <div class="d-flex align-center gap-2 mb-1">
                                    <span class="font-weight-black text-slate-800">{{ addr.tenNguoiNhan }}</span>
                                    <span class="text-caption font-weight-bold text-slate-400 px-2 border-l ml-1">
                                        {{ addr.sdtNguoiNhan }}</span>
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
                                    <EditIcon size="18" />
                                    <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                                </v-btn>
                                <v-btn v-if="!addr.laMacDinh" icon variant="text" size="small" color="success"
                                    @click="handleSetDefault(addr.id)" class="action-icon-btn">
                                    <StarIcon size="18" />
                                    <v-tooltip activator="parent" location="top">Đặt mặc định</v-tooltip>
                                </v-btn>
                                <v-btn v-if="!addr.laMacDinh" icon variant="text" size="small" color="error"
                                    @click="handleDeleteAddr(addr.id)" class="action-icon-btn">
                                    <TrashIcon size="18" />
                                    <v-tooltip activator="parent" location="top">Xóa địa chỉ</v-tooltip>
                                </v-btn>
                            </div>
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" lg="4" class="d-flex flex-column" style="gap: 24px">
                <!-- Notes -->
                <v-card class="filter-card elevation-0 mb-0">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-slate-100 mr-3">
                                <NoteIcon class="text-slate-600" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-bold text-slate-800">Ghi chú & Thông tin
                                thêm</span>
                        </div>
                        <v-textarea v-model="customerForm.ghiChu"
                            placeholder="Ghi chú về khách hàng (Sở thích, lưu ý giao hàng...)" variant="outlined"
                            rows="3" hide-details></v-textarea>
                    </v-card-text>
                </v-card>

                <!-- Security -->
                <v-card class="filter-card elevation-0 bg-primary-lighten-5 border-primary-lighten-4 flex-grow-1 d-flex flex-column justify-center mb-0">
                    <v-card-text class="pa-8">
                        <div class="d-flex align-center mb-4">
                            <v-icon color="primary" size="24" class="mr-3">mdi-shield-check</v-icon>
                            <span class="text-subtitle-1 font-weight-bold text-primary">Bảo mật tài khoản</span>
                        </div>
                        <p class="text-body-2 text-slate-600 mb-0">
                            Hệ thống sẽ tự động khởi tạo các thông số bảo mật khi hồ sơ được tạo thành công.
                        </p>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <!-- SHARED CONFIRM -->
        <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
            :color="confirmDialog.color" @confirm="confirmDialog.action" />

        <!-- INVOICE DETAIL DIALOG -->
        <v-dialog v-model="invoiceDetailDialog" max-width="1000" transition="dialog-bottom-transition" scrollable>
            <v-card class="premium-card rounded-xl khach-hang-dialog-card">
                <v-card-title class="pa-6 font-weight-black border-b text-primary d-flex align-center">
                    <ReceiptIcon size="24" class="mr-3" />
                    Chi tiết hóa đơn: {{ selectedInvoice?.maHoaDon || selectedInvoice?.ma }}
                    <v-spacer />
                    <v-btn icon variant="text" size="small" @click="invoiceDetailDialog = false">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </v-card-title>

                <v-card-text class="pa-6">
                    <div v-if="detailLoading" class="text-center py-16">
                        <v-progress-circular indeterminate color="primary" size="64" />
                        <div class="mt-4 font-weight-bold text-slate-500">Đang tải chi tiết hóa đơn...</div>
                    </div>

                    <div v-else>
                        <div class="admin-table-container border rounded-xl overflow-hidden bg-white">
                            <table class="native-admin-table w-100">
                                <thead>
                                    <tr>
                                        <th class="header-cell text-center" style="width: 60px">STT</th>
                                        <th class="header-cell text-center">Mã sản phẩm</th>
                                        <th class="header-cell text-center">Tên sản phẩm</th>
                                        <th class="header-cell text-center">Mã biến thể</th>
                                        <th class="header-cell text-center">Tên biến thể</th>
                                        <th class="header-cell text-center" style="width: 100px">Số lượng</th>
                                        <th class="header-cell text-center" style="width: 130px">Đơn giá</th>
                                        <th class="header-cell text-center" style="width: 140px">Thành tiền</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="(item, index) in selectedInvoice?.items" :key="item.id" class="data-row">
                                        <td class="data-cell text-center text-slate-400">{{ index + 1 }}</td>
                                        <td class="data-cell text-center">
                                            <span class="mono-font text-slate-500">{{ item.maSanPham || item.maSP ||
                                                'N/A'
                                                }}</span>
                                        </td>
                                        <td class="data-cell text-center">
                                            <span class="text-slate-800">{{ item.tenSanPham }}</span>
                                        </td>
                                        <td class="data-cell text-center">
                                            <span class="mono-font text-slate-500">{{ item.maBienThe || item.maCTSP ||
                                                'N/A'
                                                }}</span>
                                        </td>
                                        <td class="data-cell text-center">
                                            <span class="text-slate-600">{{ item.tenMauSac }} / {{ item.tenKichThuoc
                                                }}</span>
                                        </td>
                                        <td class="data-cell text-center">
                                            <span class="text-slate-600">{{ item.soLuong }}</span>
                                        </td>
                                        <td class="data-cell text-center text-primary"
                                            style="color: #1e257c !important">
                                            {{ (item.donGia || item.giaTien || 0).toLocaleString() }}đ
                                        </td>
                                        <td class="data-cell text-center text-primary"
                                            style="color: #1e257c !important">
                                            {{
                                                (
                                                    item.thanhTien || (item.donGia || item.giaTien || 0) * (item.soLuong || 0)
                                                ).toLocaleString()
                                            }}đ
                                        </td>
                                    </tr>
                                </tbody>
                                <tfoot>
                                    <tr class="bg-slate-50">
                                        <td colspan="7"
                                            class="data-cell text-right font-weight-black text-slate-800 py-4"
                                            style="font-size: 13px !important">
                                            Tổng tiền:
                                        </td>
                                        <td class="data-cell text-right font-weight-black text-primary py-4"
                                            style="font-size: 13px !important; color: #1e257c !important">
                                            {{ (selectedInvoice?.tongTienSauGiam || selectedInvoice?.tongTien ||
                                            0).toLocaleString() }}đ
                                        </td>
                                    </tr>
                                </tfoot>
                            </table>
                        </div>
                    </div>
                </v-card-text>

                <v-card-actions class="pa-4 bg-slate-50 border-t">
                    <v-spacer />
                    <v-btn color="slate-500" variant="text" class="text-none font-weight-bold"
                        @click="invoiceDetailDialog = false">Đóng</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- INVOICE DETAIL DIALOG -->
        <v-dialog v-model="invoiceDetailDialog" max-width="1000" transition="dialog-bottom-transition" scrollable>
            <v-card class="premium-card rounded-xl khach-hang-dialog-card">
                <v-card-title class="pa-6 font-weight-black border-b text-primary d-flex align-center">
                    <ReceiptIcon size="24" class="mr-3" />
                    Chi tiết hóa đơn: {{ selectedInvoice?.maHoaDon || selectedInvoice?.ma }}
                    <v-spacer />
                    <v-btn icon variant="text" size="small" @click="invoiceDetailDialog = false">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </v-card-title>

                <v-card-text class="pa-6">
                    <div v-if="detailLoading" class="text-center py-16">
                        <v-progress-circular indeterminate color="primary" size="64" />
                        <div class="mt-4 font-weight-bold text-slate-500">Đang tải chi tiết hóa đơn...</div>
                    </div>

                    <div v-else>
                        <div class="admin-table-container border rounded-xl overflow-hidden bg-white">
                            <table class="native-admin-table w-100">
                                <thead>
                                    <tr>
                                        <th class="header-cell text-center" style="width: 60px">STT</th>
                                        <th class="header-cell text-center">Mã sản phẩm</th>
                                        <th class="header-cell text-center">Tên sản phẩm</th>
                                        <th class="header-cell text-center">Mã biến thể</th>
                                        <th class="header-cell text-center">Tên biến thể</th>
                                        <th class="header-cell text-center" style="width: 100px">Số lượng</th>
                                        <th class="header-cell text-center" style="width: 130px">Đơn giá</th>
                                        <th class="header-cell text-center" style="width: 140px">Thành tiền</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="(item, index) in selectedInvoice?.items" :key="item.id" class="data-row">
                                        <td class="data-cell text-center text-slate-400">{{ index + 1 }}</td>
                                        <td class="data-cell text-center">
                                            <span class="mono-font text-slate-500">{{ item.maSanPham || item.maSP ||
                                                'N/A'
                                                }}</span>
                                        </td>
                                        <td class="data-cell text-center">
                                            <span class="text-slate-800">{{ item.tenSanPham }}</span>
                                        </td>
                                        <td class="data-cell text-center">
                                            <span class="mono-font text-slate-500">{{ item.maBienThe || item.maCTSP ||
                                                'N/A'
                                                }}</span>
                                        </td>
                                        <td class="data-cell text-center">
                                            <span class="text-slate-600">{{ item.tenMauSac }} / {{ item.tenKichThuoc
                                                }}</span>
                                        </td>
                                        <td class="data-cell text-center">
                                            <span class="text-slate-600">{{ item.soLuong }}</span>
                                        </td>
                                        <td class="data-cell text-center text-primary"
                                            style="color: #1e257c !important">
                                            {{ (item.donGia || item.giaTien || 0).toLocaleString() }}đ
                                        </td>
                                        <td class="data-cell text-center text-primary"
                                            style="color: #1e257c !important">
                                            {{
                                                (
                                                    item.thanhTien || (item.donGia || item.giaTien || 0) * (item.soLuong || 0)
                                                ).toLocaleString()
                                            }}đ
                                        </td>
                                    </tr>
                                </tbody>
                                <tfoot>
                                    <tr class="bg-slate-50">
                                        <td colspan="7"
                                            class="data-cell text-right font-weight-black text-slate-800 py-4"
                                            style="font-size: 13px !important">
                                            Tổng tiền:
                                        </td>
                                        <td class="data-cell text-right font-weight-black text-primary py-4"
                                            style="font-size: 13px !important; color: #1e257c !important">
                                            {{ (selectedInvoice?.tongTienSauGiam || selectedInvoice?.tongTien ||
                                            0).toLocaleString() }}đ
                                        </td>
                                    </tr>
                                </tfoot>
                            </table>
                        </div>
                    </div>
                </v-card-text>

                <v-card-actions class="pa-4 bg-slate-50 border-t">
                    <v-spacer />
                    <v-btn color="slate-500" variant="text" class="text-none font-weight-bold"
                        @click="invoiceDetailDialog = false">Đóng</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- ADDRESS DIALOG -->
        <v-dialog v-model="addrDialog" max-width="600" transition="dialog-bottom-transition">
            <v-card class="filter-card elevation-24">
                <v-card-title class="pa-6 font-weight-black border-b text-primary d-flex align-center">
                    <v-icon start class="mr-3">mdi-map-marker-plus</v-icon>
                    {{ isEditAddr ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ mới' }}</v-card-title>
                <v-card-text class="pa-6">
                    <v-row>
                        <v-col cols="12" md="6">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Tên người nhận *</div>
                            <v-text-field v-model="addrForm.tenNguoiNhan" placeholder="Nhập tên" variant="outlined"
                                density="compact" class="font-weight-medium" hide-details></v-text-field>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Số điện thoại *</div>
                            <v-text-field v-model="addrForm.sdtNguoiNhan" placeholder="09xx..." variant="outlined"
                                density="compact" class="font-weight-medium" hide-details></v-text-field>
                        </v-col>
                        <v-col cols="12" md="4">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Tỉnh / Thành phố *</div>
                            <v-autocomplete v-model="addrForm.tinh" :items="provinces" item-title="name"
                                item-value="code" placeholder="Chọn" variant="outlined" density="compact" hide-details
                                :loading="loadingLocations.provinces" @update:model-value="
                                    (val) => {
                                        addrForm.thanhPho = null;
                                        addrForm.phuongXa = null;
                                        if (val) fetchDistricts(val);
                                    }
                                "></v-autocomplete>
                        </v-col>
                        <v-col cols="12" md="4">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Quận / Huyện *</div>
                            <v-autocomplete v-model="addrForm.thanhPho" :items="districts" item-title="name"
                                item-value="code" placeholder="Chọn" variant="outlined" density="compact" hide-details
                                :loading="loadingLocations.districts" :disabled="!addrForm.tinh" @update:model-value="
                                    (val) => {
                                        addrForm.phuongXa = null;
                                        if (val) fetchWards(val);
                                    }
                                "></v-autocomplete>
                        </v-col>
                        <v-col cols="12" md="4">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Phường / Xã *</div>
                            <v-autocomplete v-model="addrForm.phuongXa" :items="wards" item-title="name"
                                item-value="code" placeholder="Chọn" variant="outlined" density="compact" hide-details
                                :loading="loadingLocations.wards" :disabled="!addrForm.thanhPho"></v-autocomplete>
                        </v-col>
                        <v-col cols="12">
                            <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Địa chỉ cụ thể *</div>
                            <v-textarea v-model="addrForm.diaChiChiTiet" placeholder="Số nhà, đường..."
                                variant="outlined" rows="2" class="font-weight-medium" hide-details></v-textarea>
                        </v-col>
                        <v-col cols="12" v-if="listDiaChi.length > 0">
                            <v-checkbox v-model="addrForm.laMacDinh" label="Đặt làm địa chỉ mặc định" color="primary"
                                hide-details density="compact"></v-checkbox>
                        </v-col>
                    </v-row>
                </v-card-text>
                <v-divider></v-divider>
                <v-card-actions class="pa-4 bg-grey-lighten-4">
                    <v-spacer></v-spacer>
                    <v-btn variant="text" class="text-none font-weight-medium" @click="addrDialog = false">Hủy</v-btn>
                    <v-btn color="primary" variant="flat" class="px-6 text-none rounded-lg"
                        @click="saveAddress">Lưu địa chỉ</v-btn>
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
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

/* Đồng bộ font chữ và cỡ chữ */
:deep(.v-container),
:deep(.v-card),
:deep(.v-btn),
:deep(.v-field),
:deep(.v-list-item-title),
:deep(span),
:deep(div),
:deep(table),
:deep(th),
:deep(td) {
    font-size: 14px !important;
}

:deep(.text-subtitle-1),
:deep(.text-h5) {
}

:deep(.text-body-2),
:deep(.text-subtitle-2),
:deep(.text-subtitle-1),
:deep(.text-h6),
:deep(.text-caption),
:deep(.v-field__input),
:deep(.v-btn__content),
:deep(.v-list-item-title) {
    font-size: 14px !important;
    text-transform: none !important;
}

/* Status chips are now managed globally in _admin-common.scss — NO local overrides */

:deep(.v-btn) {
    opacity: 1 !important;
}

/* Custom Table Styles to match main data table */
.purchase-history-table,
.invoice-items-table {
    border: 1px solid #e2e8f0 !important;
    border-radius: 8px !important;
    overflow: hidden !important;
}

.purchase-history-table :deep(th),
.invoice-items-table :deep(th) {
    background-color: #f8fafc !important;
    color: #475569 !important;
    font-weight: 600 !important;
    height: 44px !important;
    border-bottom: 1px solid #e2e8f0 !important;
    text-transform: none !important;
    letter-spacing: normal !important;
}

.purchase-history-table :deep(td),
.invoice-items-table :deep(td) {
    color: #1e293b !important;
    height: 48px !important;
    border-bottom: 1px solid #f1f5f9 !important;
    padding: 8px 16px !important;
}

.history-row:hover,
.detail-row:hover {
    background-color: #f8fafc !important;
}

.total-row {
    background-color: #f1f5f9 !important;
}

.total-row td {
    font-size: 14px !important;
    color: #1e293b !important;
}

.action-btn {
    opacity: 0.7 !important;
    transition: opacity 0.2s !important;
}

.action-btn:hover {
    opacity: 1 !important;
}

.mono-font {
    font-family: 'JetBrains Mono', 'Fira Code', monospace !important;
}



#khach-hang-form-container,
#khach-hang-form-container *,
.khach-hang-dialog-card,
.khach-hang-dialog-card *,
#khach-hang-form-container .text-subtitle-1,
#khach-hang-form-container .text-subtitle-2,
#khach-hang-form-container .text-h6,
#khach-hang-form-container .text-h5,
#khach-hang-form-container .v-card-title,
#khach-hang-form-container .v-table th,
#khach-hang-form-container .v-table td,
#khach-hang-form-container .v-btn__content,
#khach-hang-form-container .v-field__input {
    font-size: 13px !important;
    text-transform: none !important;
    font-weight: 500 !important;
}

/* Các tiêu đề (header) */
#khach-hang-form-container th,
#khach-hang-form-container .header-cell,
#khach-hang-form-container .v-card-title,
#khach-hang-form-container .font-weight-black,
#khach-hang-form-container .font-weight-bold,
.khach-hang-dialog-card th,
.khach-hang-dialog-card .header-cell,
.khach-hang-dialog-card .v-card-title,
.khach-hang-dialog-card .font-weight-black,
.khach-hang-dialog-card .font-weight-bold {
    font-weight: 700 !important;
}

/* Đảm bảo các mã code monospaced không bị mất font */
#khach-hang-form-container .mono-font {
    font-family: 'JetBrains Mono', 'Fira Code', monospace !important;
    font-size: 13px !important;
}



/* CSS Global để ẩn icon mặc định của trình duyệt */
:deep(input[type="date"]::-webkit-calendar-picker-indicator),
:deep(input[type="date"]::-webkit-inner-spin-button) {
    display: none !important;
    -webkit-appearance: none !important;
}

/* Ép icon lịch to lên kể cả khi dùng density compact */
.date-field-custom .v-field__append-inner .v-icon {
    font-size: 22px !important;
    opacity: 0.8 !important;
    color: #475569 !important;
}

</style>


