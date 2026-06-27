<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { PATH } from '@/router/routePaths';
import { useRoute, useRouter } from 'vue-router';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { generateRandomCode } from '@/utils/codeGenerator';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import { useNotifications } from '@/services/notificationService';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import TableEmptyState from '@/components/common/TableEmptyState.vue';
import { ArrowLeftIcon, UserIcon, MapPinIcon, NoteIcon, PlusIcon, EditIcon, TrashIcon, StarIcon, ReceiptIcon } from 'vue-tabler-icons';
import { useLocation } from '@/composables/useLocation';
import axios from 'axios';

import { dichVuFile } from '@/services/core/dichVuFile';
import { TRANG_THAI_KHACH_HANG, ADDRESS_CONSTANTS, DEFAULT_AVATAR_URL, INVOICE_TABLE_HEADERS } from '@/constants/khachHangConstants';
import { GIOI_TINH_OPTIONS } from '@/constants/appConstants';
import { useAddressMapping } from '@/composables/useAddressMapping';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const formRef = ref(null);
const addrFormRef = ref(null);

const { provinces, districts, wards, loadingLocations, fetchProvinces, fetchDistricts, fetchWards, cleanName, matchLocation } =
    useLocation();
const { mapCodesToNames, isLegacyAddressId, createLegacyAddressId, parseAddressString } = useAddressMapping();

const loading = ref(false);
const saving = ref(false);
const isEditMode = ref(false);
const isDetailView = computed(() => route.path.includes('/detail') || route.query.view === 'true');
const submitButtonText = computed(() => (isEditMode.value ? 'Cập nhật khách hàng' : 'Thêm khách hàng'));

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
    trangThai: TRANG_THAI_KHACH_HANG.DANG_HOAT_DONG,
    tinh: null,
    thanhPho: null,
    phuongXa: null,
    diaChiChiTiet: '',
    ghiChu: '',
    hinhAnh: ''
});

const resolvedAvatarUrl = computed(() => {
    const img = customerForm.value.hinhAnh;
    if (!img) return DEFAULT_AVATAR_URL;

    // 1. If it's a seed filename like 'kh11.jpg' or 'kh11'
    if (/^kh(\d+)(\.jpg)?$/i.test(img)) {
        return DEFAULT_AVATAR_URL;
    }

    // 2. Absolute uploads path
    if (img.startsWith('/uploads/')) {
        return img;
    }
    if (img.startsWith('uploads/')) {
        return `/${img}`;
    }

    // 3. Absolute URLs (Cloudinary, standard http/https, data urls, blob urls)
    if (img.startsWith('http://') || img.startsWith('https://') || img.startsWith('data:') || img.startsWith('blob:')) {
        if (img.includes('/uploads/')) {
            const index = img.indexOf('/uploads/');
            return img.substring(index);
        }
        return img;
    }

    // 4. If it already contains '/api/common/storage/files/'
    if (img.includes('/api/common/storage/files/')) {
        const apiBase = import.meta.env.VITE_API_URL || '';
        const cleanBase = apiBase.replace(/\/+$/, '');
        const cleanImg = img.startsWith('/') ? img : `/${img}`;
        return `${cleanBase}${cleanImg}`;
    }

    // 5. If it's a relative path containing /
    if (img.includes('/') && !img.startsWith('/')) {
        return `/uploads/${img}`;
    }

    return dichVuFile.layUrlFile(img);
});

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

        // Trích xuất danh sách địa chỉ từ API (bao gồm cả data.addresses từ Backend)
        const rawAddresses = data.addresses || data.listDiaChi || data.diaChis || data.diaChiList || [];
        listDiaChi.value = Array.isArray(rawAddresses) ? [...rawAddresses] : [];

        // Xác định địa chỉ mặc định hoặc địa chỉ đầu tiên của khách hàng để làm địa chỉ hiện tại
        const defaultAddr = listDiaChi.value.find((a) => a.laMacDinh) || listDiaChi.value[0];

        customerForm.value = {
            ...data,
            tinh: null,
            thanhPho: null,
            phuongXa: null,
            diaChiChiTiet: defaultAddr ? defaultAddr.diaChiChiTiet : data.diaChiChiTiet || '',
            ma: data.ma || data.maKhachHang || '' // Đảm bảo luôn có trường 'ma' để dùng làm khóa tìm kiếm
        };
        isEditMode.value = true;

        const targetAddress = defaultAddr || data;

        // Map string names to codes for v-autocomplete, or parse from full address string
        if (targetAddress.tinh || targetAddress.thanhPho || targetAddress.phuongXa) {
            if (provinces.value.length === 0) {
                await fetchProvinces();
            }
            const province = provinces.value.find(
                (p) => cleanName(p.name) === cleanName(targetAddress.tinh) || p.code === targetAddress.tinh
            );
            if (province) {
                customerForm.value.tinh = province.code;
                await fetchDistricts(province.code);
                const district = districts.value.find(
                    (d) => cleanName(d.name) === cleanName(targetAddress.thanhPho) || d.code === targetAddress.thanhPho
                );
                if (district) {
                    customerForm.value.thanhPho = district.code;
                    await fetchWards(district.code);
                    const ward = wards.value.find(
                        (w) => cleanName(w.name) === cleanName(targetAddress.phuongXa) || w.code === targetAddress.phuongXa
                    );
                    if (ward) {
                        customerForm.value.phuongXa = ward.code;
                    }
                }
            }
        } else {
            let addressStr = targetAddress.diaChiChiTiet || targetAddress.diaChi || '';
            if (addressStr && addressStr.includes(',')) {
                const parsed = parseAddressString(addressStr);
                if (parsed.tinh) {
                    if (provinces.value.length === 0) {
                        await fetchProvinces();
                    }
                    const province = matchLocation(provinces.value, parsed.tinh);
                    if (province) {
                        customerForm.value.tinh = province.code;
                        await fetchDistricts(province.code);

                        const district = matchLocation(districts.value, parsed.thanhPho);
                        if (district) {
                            customerForm.value.thanhPho = district.code;
                            await fetchWards(district.code);

                            const ward = matchLocation(wards.value, parsed.phuongXa);
                            if (ward) {
                                customerForm.value.phuongXa = ward.code;
                                customerForm.value.diaChiChiTiet = parsed.diaChiChiTiet;
                            }
                        }
                    }
                }
            }
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

const handleSave = async () => {
    if (formRef.value) {
        const { valid } = await formRef.value.validate();
        if (!valid) {
            addNotification({
                title: 'Lỗi xác thực',
                subtitle: 'Vui lòng kiểm tra lại các trường thông tin không hợp lệ',
                color: 'error'
            });
            return;
        }
    }

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
                const mappedPayload = mapCodesToNames(customerForm.value, provinces.value, districts.value, wards.value);
                Object.assign(payload, mappedPayload);
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
                    const created = await dichVuKhachHang.taoKhachHang(payload);
                    if (created?.id && payload.tinh && payload.thanhPho && payload.phuongXa && payload.diaChiChiTiet) {
                        try {
                            await dichVuKhachHang.taoDiaChi({
                                idKhachHang: created.id,
                                tinh: payload.tinh,
                                thanhPho: payload.thanhPho,
                                phuongXa: payload.phuongXa,
                                diaChiChiTiet: payload.diaChiChiTiet,
                                tenNguoiNhan: payload.ten || customerForm.value.ten,
                                sdtNguoiNhan: payload.sdt || customerForm.value.sdt,
                                laMacDinh: true
                            });
                        } catch (e) {
                            console.error('Lỗi tự động tạo địa chỉ cho khách hàng mới:', e);
                        }
                    }
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

        if (Array.isArray(data)) {
            const newList = [...data];

            // Bảo toàn địa chỉ "gốc" (legacy) từ customerForm nếu có
            const hasLegacyInfo =
                customerForm.value && (customerForm.value.tinh || customerForm.value.thanhPho || customerForm.value.diaChiChiTiet);

            if (hasLegacyInfo) {
                const legacyId = createLegacyAddressId(customerForm.value.id || khId);

                // Chỉ thêm nếu trong data chưa có ID này (tránh trùng lặp)
                if (!newList.some((addr) => addr.id === legacyId)) {
                    const p = provinces.value.find((x) => x.code === customerForm.value.tinh || x.name === customerForm.value.tinh);
                    const d = districts.value.find((x) => x.code === customerForm.value.thanhPho || x.name === customerForm.value.thanhPho);
                    const w = wards.value.find((x) => x.code === customerForm.value.phuongXa || x.name === customerForm.value.phuongXa);

                    newList.push({
                        id: legacyId,
                        tinh: p ? p.name : customerForm.value.tinh,
                        thanhPho: d ? d.name : customerForm.value.thanhPho,
                        phuongXa: w ? w.name : customerForm.value.phuongXa,
                        diaChiChiTiet: customerForm.value.diaChiChiTiet,
                        tenNguoiNhan: customerForm.value.ten,
                        sdtNguoiNhan: customerForm.value.sdt,
                        laMacDinh: data.length === 0 // Chỉ mặc định nếu không có địa chỉ thực nào khác
                    });
                }
            }
            listDiaChi.value = newList;
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
    if (addrFormRef.value) {
        const { valid } = await addrFormRef.value.validate();
        if (!valid) {
            addNotification({
                title: 'Lỗi xác thực',
                subtitle: 'Vui lòng kiểm tra lại các trường thông tin không hợp lệ',
                color: 'error'
            });
            return;
        }
    }
    try {
        const payload = { ...addrForm.value, idKhachHang: route.params.id };
        const p = provinces.value.find((x) => x.code === addrForm.value.tinh);
        const d = districts.value.find((x) => x.code === addrForm.value.thanhPho);
        const w = wards.value.find((x) => x.code === addrForm.value.phuongXa);
        if (p) payload.tinh = p.name;
        if (d) payload.thanhPho = d.name;
        if (w) payload.phuongXa = w.name;

        // Xử lý trường hợp ID giả (legacy address)
        const isRealId = payload.id && !isLegacyAddressId(payload.id);

        if (isEditAddr.value && isRealId) {
            await dichVuKhachHang.capNhatDiaChi(addrForm.value.id, payload);
        } else {
            // Nếu thêm mới hoặc là edit từ địa chỉ gốc giả lập -> gọi API Add (tạo địa chỉ thực mới)
            delete payload.id;
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

onMounted(async () => {
    fetchProvinces();
    if (route.params.id) {
        loadCustomer(route.params.id);
    } else {
        try {
            customerForm.value.ma = await generateRandomCode('KhachHang');
        } catch (e) {
            console.error('Lỗi khi lấy mã', e);
        }
    }
});
const goBack = () => {
    router.push(PATH.KHACH_HANG);
};

const handleNameBlur = () => {
    customerForm.value.ten = customerForm.value.ten ? customerForm.value.ten.replace(/\s+/g, ' ').trim() : '';
};

const handleEmailBlur = () => {
    customerForm.value.email = customerForm.value.email ? customerForm.value.email.replace(/\s+/g, '') : '';
};

const handlePhoneInput = () => {
    customerForm.value.sdt = customerForm.value.sdt ? customerForm.value.sdt.replace(/[^\d+]/g, '') : '';
};

const nameRules = [
    (v) => !!v || 'Vui lòng nhập họ và tên',
    (v) => /^[\p{L}0-9\s]+$/u.test(v) || 'Họ và tên không được chứa ký tự đặc biệt',
    (v) => (v && v.trim() === v) || 'Không được chứa khoảng trắng ở 2 đầu'
];

const emailRules = [
    (v) => !!v || 'Vui lòng nhập email',
    (v) => /.+@.+\..+/.test(v) || 'Email không hợp lệ'
];

const phoneRules = [
    (v) => !!v || 'Vui lòng nhập số điện thoại',
    (v) => /^(0|\+84)[0-9]{9}$/.test(v) || 'Số điện thoại phải hợp lệ (VD: 09xx hoặc +849xx)'
];

const dobRules = [
    (v) => {
        if (!v) return true;
        const bd = new Date(v);
        const now = new Date();
        if (bd.getTime() > now.getTime()) return 'Ngày sinh không thể ở trong tương lai';
        let age = now.getFullYear() - bd.getFullYear();
        const m = now.getMonth() - bd.getMonth();
        if (m < 0 || (m === 0 && now.getDate() < bd.getDate())) {
            age--;
        }
        return age >= 18 || 'Khách hàng phải từ 18 tuổi trở lên';
    }
];
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
                <v-btn icon variant="flat" @click="goBack" class="btn-back-header">
                    <ArrowLeftIcon size="20" />
                </v-btn>
            </div>
            <div class="d-flex gap-3">
                <v-btn color="primary" variant="flat" class="add-btn-primary text-none px-8 rounded-xl h-11 elevation-4"
                    style="font-size: 16px !important; font-weight: 600 !important" :loading="saving"
                    @click="handleSave">
                    <v-icon size="18" class="mr-2">mdi-check-all</v-icon>
                    <span style="font-size: 16px !important; font-weight: 600 !important">{{ submitButtonText }}</span>
                </v-btn>
            </div>
        </div>

        <!-- Row 1: Personal Info & Portrait -->
        <v-form ref="formRef">
            <v-row class="mb-6 d-flex align-stretch">
                <v-col cols="12" lg="8" class="d-flex">
                    <!-- Basic Info -->
                    <v-card class="filter-card elevation-0 border border-slate-200 w-100 d-flex flex-column mb-0">
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
                                    <v-text-field v-model="customerForm.ma" readonly
                                        :placeholder="isEditMode ? 'KH-XXXX' : 'Hệ thống tự sinh khi lưu'"
                                        variant="outlined" bg-color="white" density="compact"
                                        class="font-weight-medium mono-font" hide-details></v-text-field>
                                </v-col>
                                <v-col cols="12" md="8">
                                    <div class="field-label">Họ và tên <span class="text-error">*</span></div>
                                    <v-text-field v-model="customerForm.ten" :readonly="isDetailView"
                                        @blur="handleNameBlur" :rules="nameRules" placeholder="Ví dụ: Nguyễn Văn A"
                                        variant="outlined" bg-color="white" density="compact"
                                        hide-details="auto"></v-text-field>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="field-label">Email <span class="text-error">*</span></div>
                                    <v-text-field v-model="customerForm.email" :readonly="isDetailView"
                                        @blur="handleEmailBlur" :rules="emailRules" placeholder="khachhang@gmail.com"
                                        variant="outlined" bg-color="white" density="compact"
                                        hide-details="auto"></v-text-field>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="field-label">Số điện thoại <span class="text-error">*</span></div>
                                    <v-text-field v-model="customerForm.sdt" :readonly="isDetailView"
                                        @input="handlePhoneInput" :rules="phoneRules" placeholder="09xx.xxx.xxx"
                                        variant="outlined" bg-color="white" density="compact"
                                        hide-details="auto"></v-text-field>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="field-label">Ngày sinh</div>
                                    <AppDatePicker v-model="customerForm.ngaySinh" :disabled="isDetailView"
                                        placeholder="Chọn ngày sinh" :text-field-props="{ rules: dobRules }" />
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="field-label">Giới tính</div>
                                    <v-select v-model="customerForm.gioiTinh" :readonly="isDetailView"
                                        :items="GIOI_TINH_OPTIONS" variant="outlined" bg-color="white" density="compact"
                                        hide-details></v-select>
                                </v-col>
                            </v-row>
                        </v-card-text>
                    </v-card>
                </v-col>

                <v-col cols="12" lg="4" class="d-flex">
                    <v-card class="filter-card elevation-0 border border-slate-200 w-100 d-flex flex-column mb-0">
                        <v-card-text class="pa-8 text-center flex-grow-1">
                            <div class="section-header d-flex align-center mb-6 text-left">
                                <div class="icon-blob bg-blue-lighten-5 mr-3">
                                    <v-icon color="primary" size="18">mdi-camera</v-icon>
                                </div>
                                <span class="text-subtitle-1 font-weight-bold text-slate-800">Ảnh chân dung</span>
                            </div>

                            <div class="position-relative d-inline-block mx-auto mb-6">
                                <v-avatar size="160" color="blue-lighten-5"
                                    class="cursor-pointer avatar-hover transition-all overflow-hidden"
                                    @click="handleFileClick">
                                    <v-img :src="resolvedAvatarUrl" cover>
                                        <template v-slot:placeholder>
                                            <v-row class="fill-height ma-0" align="center" justify="center">
                                                <v-progress-circular indeterminate
                                                    color="primary"></v-progress-circular>
                                            </v-row>
                                        </template>
                                    </v-img>
                                    <div v-if="uploading" class="upload-overlay d-flex align-center justify-center">
                                        <v-progress-circular indeterminate size="40" color="white"
                                            width="5"></v-progress-circular>
                                    </div>
                                </v-avatar>
                            </div>
                            <input type="file" ref="fileInput" class="d-none" accept="image/*" @change="onFileChange" />

                            <div v-if="!isDetailView" class="text-left">
                                <div class="field-label">Liên kết ảnh (URL)</div>
                                <v-text-field v-if="!isDetailView" v-model="customerForm.hinhAnh"
                                    placeholder="Dán URL ảnh hoặc nhấn vào vòng tròn phía trên" variant="outlined"
                                    bg-color="white" density="compact" hide-details
                                    class="font-weight-medium bg-slate-50"></v-text-field>
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
                    <v-card class="filter-card elevation-0 border border-slate-200 w-100 d-flex flex-column mb-0">
                        <v-card-text class="pa-8 flex-grow-1">
                            <div class="section-header d-flex align-center mb-6">
                                <div class="icon-blob bg-amber-lighten-5 mr-3">
                                    <MapPinIcon class="text-amber-darken-3" size="20" />
                                </div>
                                <span class="text-subtitle-1 font-weight-bold text-slate-800">Số địa chỉ</span>
                                <v-spacer></v-spacer>
                                <v-btn v-if="isEditMode" variant="flat" color="primary" size="small"
                                    class="add-btn-primary text-none" @click="openAddrDialog()">
                                    <PlusIcon size="18" class="mr-2" />
                                    Thêm địa chỉ mới
                                </v-btn>
                            </div>

                            <div v-if="(!isEditMode || listDiaChi.length === 0) && !isDetailView" class="mb-4">
                                <v-row>
                                    <v-col cols="12" md="4">
                                        <div class="field-label">Tỉnh / Thành phố</div>
                                        <v-autocomplete v-model="customerForm.tinh" :items="provinces" item-title="name"
                                            item-value="code" placeholder="Chọn Tỉnh / Thành phố" variant="outlined"
                                            bg-color="white" density="compact" />
                                    </v-col>

                                    <v-col cols="12" md="4">
                                        <div class="field-label">Quận / Huyện</div>
                                        <v-autocomplete v-model="customerForm.thanhPho" :items="districts"
                                            item-title="name" item-value="code" placeholder="Chọn Quận / Huyện"
                                            variant="outlined" bg-color="white" density="compact"
                                            :disabled="!customerForm.tinh" />
                                    </v-col>

                                    <v-col cols="12" md="4">
                                        <div class="field-label">Phường / Xã</div>
                                        <v-autocomplete v-model="customerForm.phuongXa" :items="wards" item-title="name"
                                            item-value="code" placeholder="Chọn Phường / Xã" variant="outlined"
                                            bg-color="white" density="compact" :disabled="!customerForm.thanhPho" />
                                    </v-col>

                                    <v-col cols="12">
                                        <div class="field-label">Địa chỉ cụ thể (Số nhà, đường...)</div>
                                        <v-textarea v-model="customerForm.diaChiChiTiet"
                                            placeholder="Nhập địa chỉ cụ thể" variant="outlined" bg-color="white"
                                            rows="2" hide-details />
                                    </v-col>
                                </v-row>
                            </div>

                            <div v-else-if="listDiaChi.length > 0" class="address-list-scrollable custom-scrollbar pt-1"
                                style="max-height: 400px; overflow-y: auto; padding-right: 8px; margin-right: -8px;">
                                <div v-for="addr in listDiaChi" :key="addr.id"
                                    class="mb-4 pa-5 border rounded-xl d-flex align-center gap-4 bg-white shadow-sm hover-addr-card transition-all">
                                    <v-avatar color="primary" class="mr-2 elevation-2" size="36">
                                        <v-icon color="white" size="18">mdi-map-marker</v-icon>
                                    </v-avatar>
                                    <div class="flex-grow-1">
                                        <div class="d-flex align-center gap-2 mb-1">
                                            <span class="font-weight-black text-slate-800">{{ addr.tenNguoiNhan
                                                }}</span>
                                            <span
                                                class="text-caption font-weight-bold text-slate-400 px-2 border-l ml-1">
                                                {{ addr.sdtNguoiNhan }}</span>
                                            <v-chip v-if="addr.laMacDinh" color="success" size="x-small" variant="flat"
                                                class="ml-2 font-weight-black px-3" style="color: white !important">Mặc
                                                định</v-chip>
                                        </div>
                                        <div class="text-body-2 font-weight-bold text-slate-500">
                                            {{ addr.diaChiChiTiet }}, {{ addr.phuongXa }}, {{ addr.thanhPho }}, {{
                                                addr.tinh
                                            }}
                                        </div>
                                    </div>
                                    <div class="d-flex align-center gap-1" v-if="!isDetailView">
                                        <v-btn icon variant="text" size="small" color="primary"
                                            @click="openAddrDialog(addr)" class="action-icon-btn">
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
                            </div>
                        </v-card-text>
                    </v-card>
                </v-col>

                <v-col cols="12" lg="4" class="d-flex flex-column" style="gap: 24px">
                    <!-- Notes -->
                    <v-card class="filter-card elevation-0 border border-slate-200 mb-0">
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
                                bg-color="white" rows="3" hide-details></v-textarea>
                        </v-card-text>
                    </v-card>

                    <!-- Security -->
                    <v-card
                        class="filter-card elevation-0 bg-primary-lighten-5 border-primary-lighten-4 flex-grow-1 d-flex flex-column justify-center mb-0">
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
        </v-form>

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
                                    <TableEmptyState
                                        v-if="!selectedInvoice?.items || selectedInvoice?.items.length === 0"
                                        :colspan="8" text="Không có sản phẩm nào trong hóa đơn này." />
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
                    <v-form ref="addrFormRef">
                        <v-row>
                            <v-col cols="12" md="6">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Tên người nhận <span
                                        class="text-error">*</span></div>
                                <v-text-field v-model="addrForm.tenNguoiNhan" placeholder="Nhập tên" variant="outlined"
                                    :rules="[(v) => !!v || 'Vui lòng nhập tên người nhận']" :readonly="true"
                                    bg-color="white" density="compact" class="font-weight-medium bg-slate-50"
                                    hide-details="auto"></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Số điện thoại <span
                                        class="text-error">*</span></div>
                                <v-text-field v-model="addrForm.sdtNguoiNhan" placeholder="09xx..." variant="outlined"
                                    :rules="[
                                        (v) => !!v || 'Vui lòng nhập số điện thoại',
                                        (v) => /^[0-9]{10}$/.test(v) || 'Số điện thoại không hợp lệ'
                                    ]" :readonly="true" bg-color="white" density="compact"
                                    class="font-weight-medium bg-slate-50" hide-details="auto"></v-text-field>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Tỉnh / Thành phố <span
                                        class="text-error">*</span></div>
                                <v-autocomplete v-model="addrForm.tinh" :items="provinces" item-title="name"
                                    :rules="[(v) => !!v || 'Vui lòng chọn tỉnh/thành phố']" item-value="code"
                                    placeholder="Chọn" variant="outlined" bg-color="white" density="compact"
                                    hide-details="auto" :loading="loadingLocations.provinces" @update:model-value="
                                        (val) => {
                                            addrForm.thanhPho = null;
                                            addrForm.phuongXa = null;
                                            if (val) fetchDistricts(val);
                                        }
                                    "></v-autocomplete>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Quận / Huyện <span
                                        class="text-error">*</span></div>
                                <v-autocomplete v-model="addrForm.thanhPho" :items="districts" item-title="name"
                                    :rules="[(v) => !!v || 'Vui lòng chọn quận/huyện']" item-value="code"
                                    placeholder="Chọn" variant="outlined" bg-color="white" density="compact"
                                    hide-details="auto" :loading="loadingLocations.districts" :disabled="!addrForm.tinh"
                                    @update:model-value="
                                        (val) => {
                                            addrForm.phuongXa = null;
                                            if (val) fetchWards(val);
                                        }
                                    "></v-autocomplete>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Phường / Xã <span
                                        class="text-error">*</span></div>
                                <v-autocomplete v-model="addrForm.phuongXa" :items="wards" item-title="name"
                                    :rules="[(v) => !!v || 'Vui lòng chọn phường/xã']" item-value="code"
                                    placeholder="Chọn" variant="outlined" bg-color="white" density="compact"
                                    hide-details="auto" :loading="loadingLocations.wards"
                                    :disabled="!addrForm.thanhPho"></v-autocomplete>
                            </v-col>
                            <v-col cols="12">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Địa chỉ cụ thể <span
                                        class="text-error">*</span></div>
                                <v-textarea v-model="addrForm.diaChiChiTiet" placeholder="Số nhà, đường..."
                                    :rules="[(v) => !!v || 'Vui lòng nhập địa chỉ cụ thể']" variant="outlined"
                                    bg-color="white" rows="2" class="font-weight-medium"
                                    hide-details="auto"></v-textarea>
                            </v-col>
                            <v-col cols="12" v-if="listDiaChi.length > 0">
                                <v-checkbox v-model="addrForm.laMacDinh" label="Đặt làm địa chỉ mặc định"
                                    color="primary" hide-details density="compact"></v-checkbox>
                            </v-col>
                        </v-row>
                    </v-form>
                </v-card-text>
                <v-divider></v-divider>
                <v-card-actions class="pa-4 bg-grey-lighten-4">
                    <v-spacer></v-spacer>
                    <v-btn variant="text" class="text-none font-weight-medium" @click="addrDialog = false">Hủy</v-btn>
                    <v-btn color="primary" variant="flat" class="px-6 text-none rounded-lg" @click="saveAddress">Lưu địa
                        chỉ</v-btn>
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
:deep(input[type='date']::-webkit-calendar-picker-indicator),
:deep(input[type='date']::-webkit-inner-spin-button) {
    display: none !important;
    -webkit-appearance: none !important;
}

/* Ép icon lịch của date-field lên 20px để đồng bộ */
#khach-hang-form-container :deep(.date-field .v-icon),
.khach-hang-dialog-card :deep(.date-field .v-icon) {
    font-size: 20px !important;
    opacity: 0.8 !important;
    color: #475569 !important;
}

/* Ép màu đỏ tươi cho các cảnh báo lỗi */
#khach-hang-form-container :deep(.v-messages__message) {
    color: #ef4444 !important;
    /* Đỏ tươi */
    font-weight: 500 !important;
    font-size: 11px !important;
}

#khach-hang-form-container :deep(.v-field--error) {
    --v-field-border-color: #ef4444 !important;
}
</style>
