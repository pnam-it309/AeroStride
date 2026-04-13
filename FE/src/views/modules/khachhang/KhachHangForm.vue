<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { useNotifications } from '@/services/notificationService';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { ArrowLeftIcon, UserIcon, MapPinIcon, NoteIcon } from 'vue-tabler-icons';
import axios from 'axios';

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
        console.error(e);
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
        console.error(e);
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
        console.error(e);
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

                if (isEditMode.value) {
                    await dichVuKhachHang.capNhatKhachHang(route.params.id, payload);
                    addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật thông tin khách hàng', color: 'success' });
                } else {
                    await dichVuKhachHang.taoKhachHang(payload);
                    addNotification({ title: 'Thành công', subtitle: 'Đã thêm khách hàng mới', color: 'success' });
                }
                router.push('/khach-hang');
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
        listDiaChi.ref = data; // Note: data is response.data from service
    } catch (e) {
        console.error(e);
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

const handleSetDefault = async (id) => {
    try {
        await dichVuKhachHang.datDiaChiMacDinh(id);
        loadAddresses(route.params.id);
    } catch (e) {
        console.error(e);
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
    <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
        <!-- Header -->
        <div class="d-flex align-center mb-6">
            <v-btn icon variant="tonal" color="secondary" class="mr-4 rounded-lg" @click="router.push('/khach-hang')">
                <ArrowLeftIcon size="20" />
            </v-btn>
            <div>
                <h1 class="text-h4 font-weight-bold">
                    {{ isDetailView ? 'Hồ sơ khách hàng' : isEditMode ? 'Cập nhật thông tin' : 'Thêm khách hàng mới' }}
                </h1>
                <p class="text-subtitle-1 text-medium-emphasis">
                    {{
                        isDetailView
                            ? 'Chi tiết lịch sử và thông tin liên hệ khách hàng'
                            : 'Hệ thống quản lý thông tin khách hàng & địa điểm giao hàng'
                    }}
                </p>
            </div>
            <v-spacer></v-spacer>
            <div class="d-flex gap-2">
                <v-btn
                    v-if="!isDetailView"
                    color="#2E4E8E"
                    prepend-icon="mdi-content-save"
                    class="text-none font-weight-bold px-8 rounded-lg"
                    height="44"
                    :loading="saving"
                    @click="handleSave"
                >
                    Lưu khách hàng
                </v-btn>
                <v-btn
                    v-if="isDetailView"
                    color="#2E4E8E"
                    variant="elevated"
                    prepend-icon="mdi-pencil"
                    class="text-none font-weight-bold px-10 rounded-lg"
                    height="44"
                    @click="router.push(`/khach-hang/form/${route.params.id}`)"
                >
                    Chỉnh sửa hồ sơ
                </v-btn>
            </div>
        </div>

        <v-row v-if="loading">
            <v-col cols="12" class="text-center py-16">
                <v-progress-circular indeterminate color="#2E4E8E" size="64" width="6"></v-progress-circular>
                <div class="mt-4 text-h6 font-weight-medium">Đang tải hồ sơ khách hàng...</div>
            </v-col>
        </v-row>

        <!-- DASHBOARD VIEW (DETAIL) -->
        <v-row v-else-if="isDetailView">
            <v-col cols="12" lg="4">
                <!-- Customer Profile Summary -->
                <v-card class="rounded-xl border shadow-none mb-6 text-center pa-8">
                    <v-avatar size="150" color="#2E4E8E" class="mb-4 border shadow-sm">
                        <v-img v-if="customerForm.hinhAnh" :src="customerForm.hinhAnh"></v-img>
                        <v-icon v-else size="64" color="primary">mdi-account</v-icon>
                    </v-avatar>
                    <h2 class="text-h5 font-weight-black mb-1 text-dark">{{ customerForm.ten }}</h2>
                    <div class="text-subtitle-2 text-medium-emphasis mb-4">{{ customerForm.email }}</div>

                    <v-chip
                        :color="customerForm.trangThai === 'DANG_HOAT_DONG' ? 'success' : 'error'"
                        variant="flat"
                        class="px-6 font-weight-bold mb-4"
                    >
                        {{ customerForm.trangThai === 'DANG_HOAT_DONG' ? 'Đang hoạt động' : 'Tạm dừng / Khóa' }}
                    </v-chip>

                    <v-divider class="my-6"></v-divider>

                    <div class="d-flex justify-center flex-column gap-2 text-left">
                        <div class="d-flex align-center">
                            <v-icon color="success" size="18" class="mr-3">mdi-phone</v-icon>
                            <span class="text-subtitle-2 font-weight-bold text-dark">SĐT: </span>
                            <span class="ml-2 text-subtitle-2 font-weight-black">{{ customerForm.sdt }}</span>
                        </div>
                        <div class="d-flex align-center mt-2">
                            <v-icon color="info" size="18" class="mr-3">mdi-calendar-range</v-icon>
                            <span class="text-subtitle-2 font-weight-bold text-dark">Ngày sinh: </span>
                            <span class="ml-2 text-subtitle-2 font-weight-black">{{ customerForm.ngaySinh || 'Chưa cập nhật' }}</span>
                        </div>
                    </div>
                </v-card>

                <v-card class="rounded-xl border shadow-none">
                    <v-card-title class="pa-5 border-b font-weight-bold text-subtitle-1">Ghi chú hệ thống</v-card-title>
                    <v-card-text class="pa-6">
                        <div class="text-body-2 text-medium-emphasis">
                            {{ customerForm.ghiChu || 'Chưa có ghi chú nào cho khách hàng này.' }}
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" lg="8">
                <!-- Delivery Address Dashboard -->
                <v-card class="rounded-xl border shadow-none mb-6 pb-2">
                    <v-card-title class="pa-6 border-b d-flex align-center justify-space-between">
                        <div class="d-flex align-center">
                            <MapPinIcon class="mr-3 text-primary" size="24" />
                            <span class="font-weight-black">Danh sách địa chỉ nhận hàng</span>
                        </div>
                        <v-chip color="info" size="small" variant="tonal" class="font-weight-bold">
                            {{ listDiaChi.ref?.length || 0 }} Địa chỉ
                        </v-chip>
                    </v-card-title>
                    <v-card-text class="pa-0">
                        <div v-if="!listDiaChi.ref || listDiaChi.ref.length === 0" class="text-center py-16">
                            <v-icon size="48" color="grey-lighten-2">mdi-map-marker-off</v-icon>
                            <div class="mt-4 text-grey">Khách hàng chưa đăng ký địa chỉ nhận hàng</div>
                        </div>
                        <div
                            v-else
                            v-for="(addr, idx) in listDiaChi.ref"
                            :key="addr.id"
                            class="pa-6 d-flex align-start border-b hover-bg-light transition-all"
                        >
                            <v-avatar color="primary-lighten-5" class="mr-4">
                                <v-icon color="primary" size="20">mdi-map-marker</v-icon>
                            </v-avatar>
                            <div class="flex-grow-1">
                                <div class="d-flex align-center gap-2 mb-1">
                                    <span class="font-weight-black text-dark text-h6">{{ addr.tenNguoiNhan }}</span>
                                    <v-chip v-if="addr.laMacDinh" color="success" size="x-small" variant="flat" class="font-weight-bold"
                                        >MẶC ĐỊNH</v-chip
                                    >
                                </div>
                                <div class="text-subtitle-1 font-weight-bold text-primary mb-1">{{ addr.sdtNguoiNhan }}</div>
                                <div class="text-body-1 text-medium-emphasis">
                                    {{ addr.diaChiChiTiet }}, {{ addr.phuongXa }}, {{ addr.thanhPho }}, {{ addr.tinh }}
                                </div>
                            </div>
                        </div>
                    </v-card-text>
                </v-card>

                <!-- Account Security Block -->
                <v-card class="rounded-xl border shadow-none pb-2">
                    <v-card-title class="pa-6 border-b font-weight-black">Thông tin tài khoản truy cập</v-card-title>
                    <v-card-text class="pa-6">
                        <v-row>
                            <v-col cols="12" md="6">
                                <div class="text-caption font-weight-bold text-medium-emphasis mb-1">TÊN ĐĂNG NHẬP</div>
                                <div class="text-h6 font-weight-black text-dark">{{ customerForm.tenTaiKhoan || 'Chưa thiết lập' }}</div>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="text-caption font-weight-bold text-medium-emphasis mb-1">MẬT KHẨU</div>
                                <div class="text-subtitle-1 text-medium-emphasis italic font-italic font-weight-medium">
                                    Dữ liệu được mã hóa bảo mật
                                </div>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <!-- FORM VIEW (CREATE/EDIT) -->
        <v-row v-else>
            <v-col cols="12" lg="8">
                <!-- Basic Info -->
                <v-card class="rounded-xl border shadow-sm mb-6" elevation="0">
                    <v-card-title class="pa-6 border-b d-flex align-center">
                        <UserIcon class="mr-3 text-primary" size="24" />
                        <span class="font-weight-bold">Thông tin cá nhân</span>
                    </v-card-title>
                    <v-card-text class="pa-6">
                        <v-row>
                            <v-col cols="12" md="4">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">
                                    Mã khách hàng <span class="text-caption text-#2E4E8E">(Hệ thống tự tạo)</span>
                                </div>
                                <v-text-field
                                    v-model="customerForm.ma"
                                    readonly
                                    placeholder="KH-XXXX"
                                    variant="outlined"
                                    density="comfortable"
                                    class="font-weight-black"
                                    hide-details
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12" md="8">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Họ và tên *</div>
                                <v-text-field
                                    v-model="customerForm.ten"
                                    :readonly="isDetailView"
                                    placeholder="Ví dụ: Nguyễn Văn A"
                                    variant="outlined"
                                    density="comfortable"
                                    class="font-weight-medium"
                                    hide-details
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Email *</div>
                                <v-text-field
                                    v-model="customerForm.email"
                                    :readonly="isDetailView"
                                    placeholder="khachhang@gmail.com"
                                    variant="outlined"
                                    density="comfortable"
                                    class="font-weight-medium"
                                    hide-details
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Số điện thoại *</div>
                                <v-text-field
                                    v-model="customerForm.sdt"
                                    :readonly="isDetailView"
                                    placeholder="09xx.xxx.xxx"
                                    variant="outlined"
                                    density="comfortable"
                                    class="font-weight-medium"
                                    hide-details
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Ngày sinh</div>
                                <v-text-field
                                    v-model="customerForm.ngaySinh"
                                    :readonly="isDetailView"
                                    type="date"
                                    variant="outlined"
                                    density="comfortable"
                                    class="font-weight-medium"
                                    hide-details
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Giới tính</div>
                                <v-select
                                    v-model="customerForm.gioiTinh"
                                    :readonly="isDetailView"
                                    :items="[
                                        { title: 'Nam', value: true },
                                        { title: 'Nữ', value: false }
                                    ]"
                                    variant="outlined"
                                    density="comfortable"
                                    class="font-weight-medium"
                                    hide-details
                                ></v-select>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <!-- Address Info Card -->
                <v-card class="rounded-xl border shadow-sm mb-6" elevation="0">
                    <v-card-title class="pa-6 border-b d-flex align-center">
                        <MapPinIcon class="mr-3 text-primary" size="24" />
                        <span class="font-weight-bold">Sổ địa chỉ</span>
                        <v-spacer></v-spacer>
                        <v-btn
                            v-if="isEditMode"
                            color="primary"
                            variant="tonal"
                            size="small"
                            class="text-none"
                            prepend-icon="mdi-plus"
                            @click="openAddrDialog()"
                            >Thêm địa chỉ</v-btn
                        >
                    </v-card-title>
                    <v-card-text class="pa-6">
                        <div v-if="!isEditMode && !isDetailView" class="text-center py-4 bg-grey-lighten-4 rounded-xl mb-4">
                            <div class="text-caption text-medium-emphasis">Bạn sẽ thêm địa chỉ chi tiết sau khi tạo khách hàng</div>
                        </div>

                        <div v-else-if="listDiaChi.length === 0" class="text-center py-8">
                            <v-icon size="48" color="grey-lighten-2">mdi-map-marker-off</v-icon>
                            <div class="mt-2 text-medium-emphasis">Chưa có địa chỉ nào</div>
                        </div>

                        <div
                            v-else
                            v-for="addr in listDiaChi"
                            :key="addr.id"
                            class="mb-4 pa-4 border rounded-xl d-flex align-center gap-4 hover-addr"
                        >
                            <v-avatar color="primary-lighten-5" class="mr-2">
                                <v-icon color="primary" size="20">mdi-map-marker</v-icon>
                            </v-avatar>
                            <div class="flex-grow-1">
                                <div class="d-flex align-center gap-2 mb-1">
                                    <span class="font-weight-bold text-dark">{{ addr.tenNguoiNhan }}</span>
                                    <span class="text-caption text-medium-emphasis">| {{ addr.sdtNguoiNhan }}</span>
                                    <v-chip size="x-small" color="primary" variant="tonal" class="ml-2 px-2 font-weight-bold">{{
                                        addr.maDiaChi
                                    }}</v-chip>
                                    <v-chip v-if="addr.laMacDinh" color="success" size="x-small" variant="flat" class="ml-1"
                                        >Mặc định</v-chip
                                    >
                                </div>
                                <div class="text-subtitle-2 text-medium-emphasis">
                                    {{ addr.diaChiChiTiet }}, {{ addr.phuongXa }}, {{ addr.thanhPho }}, {{ addr.tinh }}
                                </div>
                            </div>
                            <div class="d-flex align-center gap-1" v-if="!isDetailView">
                                <v-btn icon variant="text" size="small" color="primary" @click="openAddrDialog(addr)"
                                    ><v-icon>mdi-pencil</v-icon></v-btn
                                >
                                <v-btn
                                    v-if="!addr.laMacDinh"
                                    icon
                                    variant="text"
                                    size="small"
                                    color="error"
                                    @click="handleDeleteAddr(addr.id)"
                                    ><v-icon>mdi-delete</v-icon></v-btn
                                >
                                <v-btn
                                    v-if="!addr.laMacDinh"
                                    variant="tonal"
                                    size="small"
                                    color="info"
                                    class="text-none ml-2"
                                    @click="handleSetDefault(addr.id)"
                                    >Đặt mặc định</v-btn
                                >
                            </div>
                        </div>
                    </v-card-text>
                </v-card>

                <!-- Notes -->
                <v-card class="rounded-xl border shadow-sm" elevation="0">
                    <v-card-title class="pa-6 border-b d-flex align-center">
                        <NoteIcon class="mr-3 text-primary" size="24" />
                        <span class="font-weight-bold">Ghi chú</span>
                    </v-card-title>
                    <v-card-text class="pa-6">
                        <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Ghi chú thêm</div>
                        <v-textarea
                            v-model="customerForm.ghiChu"
                            :readonly="isDetailView"
                            placeholder="Ghi chú về khách hàng..."
                            variant="outlined"
                            rows="2"
                            class="font-weight-medium"
                            hide-details
                        ></v-textarea>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" lg="4">
                <v-card class="rounded-xl border shadow-sm mb-6" elevation="0">
                    <v-card-title class="pa-6 border-b">Ảnh đại diện</v-card-title>
                    <v-card-text class="pa-6 text-center">
                        <v-avatar size="150" color="grey-lighten-4" class="mb-4 border border-dashed rounded-lg">
                            <v-img v-if="customerForm.hinhAnh" :src="customerForm.hinhAnh"></v-img>
                            <v-icon v-else size="64" color="grey-lighten-1">mdi-account-plus</v-icon>
                        </v-avatar>
                        <div v-if="!isDetailView" class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">URL Hình ảnh</div>
                        <v-text-field
                            v-if="!isDetailView"
                            v-model="customerForm.hinhAnh"
                            placeholder="https://..."
                            variant="outlined"
                            density="compact"
                            hide-details
                            class="font-weight-medium"
                        ></v-text-field>
                    </v-card-text>
                </v-card>

                <v-card class="rounded-xl border shadow-sm" elevation="0">
                    <v-card-title class="pa-6 border-b font-weight-bold">Bảo mật tài khoản</v-card-title>
                    <v-card-text class="pa-6">
                        <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Tên đăng nhập</div>
                        <v-text-field
                            v-model="customerForm.tenTaiKhoan"
                            :readonly="true"
                            placeholder="Username"
                            variant="outlined"
                            density="comfortable"
                            class="font-weight-medium mb-4"
                            hide-details
                        ></v-text-field>
                        <div v-if="!isDetailView" class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Mật khẩu</div>
                        <v-text-field
                            v-if="!isDetailView"
                            v-model="customerForm.matKhau"
                            type="password"
                            variant="outlined"
                            density="comfortable"
                            class="font-weight-medium"
                            placeholder="••••••••"
                            hide-details
                        ></v-text-field>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <!-- SHARED CONFIRM -->
        <AdminConfirm
            v-model:show="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :color="confirmDialog.color"
            @confirm="confirmDialog.action"
        />

        <!-- ADDRESS DIALOG -->
        <v-dialog v-model="addrDialog" max-width="600">
            <v-card class="rounded-xl border shadow-2xl">
                <v-card-title class="pa-4 font-weight-bold border-b text-primary">{{
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
                                density="comfortable"
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
                                density="comfortable"
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
                                density="comfortable"
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
                                density="comfortable"
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
                                density="comfortable"
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
.gray-bg {
    background-color: #f8fafc;
}
.font-body {
    font-family: 'Inter', sans-serif;
}
.text-dark {
    color: #1e293b !important;
}
.gap-2 {
    gap: 8px;
}
.italic {
    font-style: italic;
}
.hover-bg-light:hover {
    background-color: #f8fafc;
}
.transition-all {
    transition: all 0.2s ease;
}
:deep(.v-btn) {
    border-radius: 8px !important;
}
.hover-addr {
    transition: all 0.2s;
}
.hover-addr:hover {
    background-color: #f1f5f9;
    border-color: #cbd5e1 !important;
    transform: translateY(-1px);
}
</style>
