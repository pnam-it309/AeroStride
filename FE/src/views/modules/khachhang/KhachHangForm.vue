<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { PATH } from '@/router/routePaths';
import { useRoute, useRouter } from 'vue-router';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import { useNotifications } from '@/services/notificationService';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { ArrowLeftIcon, UserIcon, MapPinIcon, NoteIcon, ReceiptIcon } from 'vue-tabler-icons';
import axios from 'axios';

import { dichVuFile } from '@/services/core/dichVuFile';
import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';
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

// Purchase History
const listHoaDon = ref([]);
const invoiceDetailDialog = ref(false);
const selectedInvoice = ref(null);
const invoiceLoading = ref(false);
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
        isEditMode.value = true;

        // Đồng bộ danh sách địa chỉ (thử mọi trường có thể)
        const rawAddresses = data.listDiaChi || data.diaChis || data.diaChiList || [];
        if (Array.isArray(rawAddresses) && rawAddresses.length > 0) {
            listDiaChi.value = [...rawAddresses];
        }

        // Tải thêm từ API riêng (nếu API này trả về thì sẽ cập nhật sau)
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

const loadPurchaseHistory = async (id) => {
    invoiceLoading.value = true;
    try {
        const res = await dichVuHoaDon.layHoaDonPhanTrang({ 
            idKhachHang: id,
            search: customerForm.value.ma, 
            size: 50
        });
        
        const allData = res?.content || res?.data || res || [];
        
        const filtered = allData.filter(inv => {
            if (inv.idKhachHang && String(inv.idKhachHang) === String(id)) return true;
            if (inv.maKhachHang === customerForm.value.ma) return true;
            if (inv.tenKhachHang === customerForm.value.ten) return true;
            return false;
        });

        listHoaDon.value = filtered;

        // Bổ sung: Lấy số lượng thực tế nếu bị thiếu (0) vì API summary không trả về
        listHoaDon.value.forEach(async (inv, idx) => {
            if (!inv.tongSanPham && !inv.soLuong && !inv.tongSoLuong) {
                try {
                    const detail = await dichVuHoaDon.layChiTietHoaDon(inv.id);
                    if (detail && detail.listsHoaDonChiTiet) {
                        const count = detail.listsHoaDonChiTiet.reduce((sum, item) => sum + (Number(item.soLuong) || 0), 0);
                        listHoaDon.value[idx].tongSoLuong = count;
                    }
                } catch (e) {
                    console.error("Lỗi lấy số lượng chi tiết:", e);
                }
            }
        });
    } catch (error) {
        console.error('Error loading purchase history:', error);
    } finally {
        invoiceLoading.value = false;
    }
};

const viewInvoiceDetail = async (invoice) => {
    selectedInvoice.value = invoice;
    invoiceDetailDialog.value = true;
    detailLoading.value = true;
    try {
        const fullDetail = await dichVuHoaDon.layChiTietHoaDon(invoice.id);
        selectedInvoice.value = { ...invoice, items: fullDetail?.listsHoaDonChiTiet || fullDetail?.chiTiets || fullDetail || [] };
    } catch (error) {
        console.error('Error loading invoice detail:', error);
    } finally {
        detailLoading.value = false;
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
        customerForm.value.hinhAnh = typeof res === 'object' ? res.url || res.data || res.secure_url : res;
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
        // Kiểm tra mọi cấu trúc: res là mảng, res.data là mảng, hoặc res.content
        const newAddresses = Array.isArray(res) ? res : (res?.data || res?.content || res?.items || []);
        
        // Nếu tìm thấy địa chỉ mới, ghi đè lên listDiaChi.value
        if (Array.isArray(newAddresses) && newAddresses.length > 0) {
            listDiaChi.value = [...newAddresses];
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
        if (isDetailView.value) {
            loadPurchaseHistory(route.params.id);
        }
    }
});
</script>

<template>
    <v-container id="khach-hang-form-container" fluid class="pa-6 animate-fade-in overflow-y-auto font-body" style="height: 100vh;">
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
                    class="text-none font-weight-black text-white px-8 rounded-xl h-11 elevation-4" @click="showQR = true">
                    <v-icon size="20" class="mr-2 text-white">mdi-qrcode-scan</v-icon>
                    Quét QR CCCD
                </v-btn>
                <v-btn v-if="!isDetailView" color="primary" variant="flat"
                    class="text-none font-weight-black text-white px-8 rounded-xl h-11 elevation-4" :loading="saving"
                    @click="handleSave">
                    <v-icon size="18" class="mr-2 text-white">mdi-check-all</v-icon>
                    Lưu hồ sơ khách hàng
                </v-btn>
                <v-btn v-if="isDetailView" color="primary" variant="flat"
                    class="text-none font-weight-black text-white px-8 rounded-xl h-11 elevation-4"
                    @click="router.push(`${PATH.KHACH_HANG_FORM}/${route.params.id}`)">
                    <v-icon size="18" class="mr-2 text-white">mdi-pencil</v-icon>
                    Chỉnh sửa hồ sơ
                </v-btn>
            </div>
        </div>

        <!-- Dialog quét QR CCCD -->
        <v-dialog v-model="showQR" max-width="500" transition="dialog-bottom-transition">
            <v-card class="premium-card">
                <v-card-title class="pa-6 font-weight-black border-b">
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
                    <v-btn color="slate-400" variant="text" class="text-none font-weight-bold" @click="showQR = false">Hủy bỏ</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <v-row v-if="loading">
            <v-col cols="12" class="text-center py-16">
                <v-progress-circular indeterminate color="primary" size="64" width="6"></v-progress-circular>
                <div class="mt-4 text-subtitle-1 font-weight-bold text-slate-500">Đang tải hồ sơ khách hàng...</div>
            </v-col>
        </v-row>

        <!-- DASHBOARD VIEW (DETAIL) -->
        <v-row v-else-if="isDetailView">
            <v-col cols="12" lg="4">
                <!-- Customer Profile Summary -->
                <v-card class="premium-card mb-6 text-center">
                    <v-card-text class="pa-8">
                        <v-avatar size="140" color="blue-lighten-5" class="mb-4 border-xl border-white elevation-4">
                            <v-img :src="customerForm.hinhAnh || FB_DEFAULT_AVATAR"></v-img>
                        </v-avatar>
                        <h2 class="text-h5 font-weight-medium mb-1 text-slate-800">{{ customerForm.ten }}</h2>
                        <div class="text-subtitle-2 text-slate-400 mb-6">{{ customerForm.email }}</div>

                        <v-chip
                            size="small"
                            variant="flat"
                            :class="['status-chip', customerForm.trangThai === 'DANG_HOAT_DONG' ? 'status-chip-active' : 'status-chip-inactive']"
                            class="px-8 mb-8"
                        >
                            {{ customerForm.trangThai === 'DANG_HOAT_DONG' ? 'Hoạt động' : 'Tạm dừng / Khóa' }}
                        </v-chip>

                        <v-divider class="mb-6 opacity-50"></v-divider>

                        <div class="text-left">
                            <div class="d-flex align-center mb-4 bg-slate-50 pa-3 rounded-xl border">
                                <div class="icon-blob bg-success-lighten-4 mr-3">
                                    <v-icon color="success" size="18">mdi-phone</v-icon>
                                </div>
                                <div>
                                    <div class="text-caption font-weight-medium text-slate-400">Số điện thoại</div>
                                    <div class="text-subtitle-2 text-slate-700">{{ customerForm.sdt }}
                                    </div>
                                </div>
                            </div>
                            <div class="d-flex align-center bg-slate-50 pa-3 rounded-xl border">
                                <div class="icon-blob bg-info-lighten-4 mr-3">
                                    <v-icon color="info" size="18">mdi-calendar-range</v-icon>
                                </div>
                                <div>
                                    <div class="text-caption font-weight-medium text-slate-400">Ngày sinh</div>
                                    <div class="text-subtitle-2 text-slate-700">{{ customerForm.ngaySinh
                                        || 'Chưa cập nhật' }}</div>
                                </div>
                            </div>
                        </div>
                    </v-card-text>
                </v-card>

                <v-card class="premium-card">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-4">
                            <div class="icon-blob bg-slate-100 mr-3">
                                <NoteIcon size="18" class="text-slate-600" />
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Ghi chú hệ thống</span>
                        </div>
                        <div class="text-body-2 font-weight-medium text-slate-500 leading-relaxed">
                            {{ customerForm.ghiChu || 'Chưa có ghi chú nào cho khách hàng này.' }}
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" lg="8">
                <!-- Delivery Address Dashboard -->
                <v-card class="premium-card mb-6 pb-2">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <MapPinIcon class="text-primary" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Danh sách địa chỉ nhận hàng</span>
                            <v-chip color="primary" size="small" variant="tonal" class="ml-3 font-weight-black">
                                {{ (listDiaChi.length > 0 ? listDiaChi.length : (customerForm.listDiaChi?.length || 0)) }} Địa chỉ
                            </v-chip>
                        </div>

                        <v-divider class="mb-6 opacity-30"></v-divider>

                        <!-- Không có địa chỉ -->
                        <div v-if="(listDiaChi.length === 0 && (!customerForm.listDiaChi || customerForm.listDiaChi.length === 0))" class="text-center py-16 bg-slate-50 rounded-xl border-dashed border">
                            <v-icon size="48" color="slate-200">mdi-map-marker-off</v-icon>
                            <div class="mt-4 text-slate-400">Khách hàng chưa đăng ký địa chỉ nhận hàng</div>
                        </div>

                        <!-- Hiển thị toàn bộ địa chỉ -->
                        <div v-else v-for="(addr, idx) in (listDiaChi.length > 0 ? listDiaChi : (customerForm.listDiaChi || []))" :key="addr.id || idx"
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
                        </div>
                    </v-card-text>
                </v-card>

                <!-- Account Security Block -->
                <v-card class="premium-card">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-slate-100 mr-3">
                                <v-icon color="slate-600" size="18">mdi-shield-account</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-medium text-slate-800">Thông tin tài khoản</span>
                        </div>
                        <v-row>
                            <v-col cols="12" md="6">
                                <div class="bg-slate-50 pa-4 rounded-xl border">
                                    <div class="text-caption font-weight-medium text-slate-400 mb-1">Tên đăng nhập</div>
                                    <div class="text-subtitle-1 font-weight-medium text-primary">{{ customerForm.tenTaiKhoan ||
                                        'Chưa thiết lập' }}</div>
                                </div>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="bg-slate-50 pa-4 rounded-xl border d-flex align-center">
                                    <div class="flex-grow-1">
                                        <div class="text-caption font-weight-medium text-slate-400 mb-1">Mật khẩu</div>
                                        <div class="text-subtitle-2 text-slate-400 italic font-italic">
                                            Dữ liệu được mã hóa bảo mật
                                        </div>
                                    </div>
                                    <v-icon color="slate-300">mdi-lock-check</v-icon>
                                </div>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <!-- Purchase History Dashboard -->
                <v-card class="premium-card mt-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-purple-lighten-5 mr-3">
                                <ReceiptIcon class="text-purple-darken-3" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Lịch sử mua hàng</span>
                        </div>

                        <v-divider class="mb-6 opacity-30"></v-divider>

                        <v-table class="purchase-history-table">
                            <thead>
                                <tr>
                                    <th class="text-center font-weight-bold" style="width: 150px">STT</th>
                                    <th class="text-left font-weight-bold" style="width: 130px">Mã hóa đơn</th>
                                    <th class="text-center font-weight-bold" >Tổng sản phẩm</th>
                                    <th class="text-left font-weight-bold" style="width: 150px">Tổng tiền</th>
                                    <th class="text-center font-weight-bold" >Hành động</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-if="invoiceLoading">
                                    <td colspan="5" class="text-center py-10">
                                        <v-progress-circular indeterminate color="primary" size="32" />
                                    </td>
                                </tr>
                                <tr v-else-if="listHoaDon.length === 0">
                                    <td colspan="5" class="text-center py-10 text-slate-400 font-weight-medium">
                                        Chưa có lịch sử giao dịch nào.
                                    </td>
                                </tr>
                                <tr v-for="(invoice, index) in listHoaDon" :key="invoice.id" class="history-row">
                                    <td class="data-cell text-center text-slate-400">{{ index + 1 }}</td>
                                    <td class="data-cell text-left">
                                        <span class="text-slate-800">{{ invoice.maHoaDon || invoice.ma }}</span>
                                    </td>
                                    <td class="data-cell text-center text-slate-600">{{ invoice.tongSanPham || invoice.soLuong || invoice.tongSoLuong || 0 }}</td>
                                    <td class="data-cell text-left">
                                        <span class="text-primary" style="color: #1e257c !important;">{{ (invoice.tongTienSauGiam || invoice.tongTien || 0).toLocaleString() }}đ</span>
                                    </td>
                                    <td class="text-center">
                                        <v-btn icon variant="text" size="small" color="primary" class="action-btn" @click="viewInvoiceDetail(invoice)">
                                            <v-icon size="18">mdi-eye-outline</v-icon>
                                        </v-btn>
                                    </td>
                                </tr>
                            </tbody>
                        </v-table>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <!-- FORM VIEW (CREATE/EDIT) -->
        <v-row v-else class="pb-16">
            <v-col cols="12" lg="8">
                <!-- Basic Info -->
                <v-card class="premium-card mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <UserIcon class="text-primary" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Thông tin cá nhân</span>
                        </div>

                        <v-row>
                            <v-col cols="12" md="4">
                                <div class="field-label">Mã khách hàng</div>
                                <v-text-field v-model="customerForm.ma" readonly placeholder="KH-XXXX" variant="outlined"
                                    density="comfortable" class="font-weight-black bg-slate-50 mono-font"
                                    hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="8">
                                <div class="field-label">Họ và tên *</div>
                                <v-text-field v-model="customerForm.ten" :readonly="isDetailView"
                                    placeholder="Ví dụ: Nguyễn Văn A" variant="outlined" density="comfortable"
                                    class="font-weight-bold" hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Email *</div>
                                <v-text-field v-model="customerForm.email" :readonly="isDetailView"
                                    placeholder="khachhang@gmail.com" variant="outlined" density="comfortable"
                                    class="font-weight-bold" hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Số điện thoại *</div>
                                <v-text-field v-model="customerForm.sdt" :readonly="isDetailView"
                                    placeholder="09xx.xxx.xxx" variant="outlined" density="comfortable"
                                    class="font-weight-bold" hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Ngày sinh</div>
                                <v-text-field v-model="customerForm.ngaySinh" :readonly="isDetailView" type="date"
                                    variant="outlined" density="comfortable" class="font-weight-bold"
                                    hide-details></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="field-label">Giới tính</div>
                                <v-select v-model="customerForm.gioiTinh" :readonly="isDetailView" :items="[
                                    { title: 'Nam', value: true },
                                    { title: 'Nữ', value: false }
                                ]" variant="outlined" density="comfortable" class="font-weight-bold"
                                    hide-details></v-select>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <!-- Address Info Card -->
                <v-card class="premium-card mb-6">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-amber-lighten-5 mr-3">
                                <MapPinIcon class="text-amber-darken-3" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Sổ địa chỉ</span>
                            <v-spacer></v-spacer>
                            <v-btn v-if="isEditMode" color="primary" variant="tonal" size="small"
                                class="text-none font-weight-black rounded-lg" prepend-icon="mdi-plus"
                                @click="openAddrDialog()">
                                Thêm địa chỉ mới
                            </v-btn>
                        </div>

                        <div v-if="!isEditMode && !isDetailView" class="mb-4 bg-slate-50 pa-6 rounded-xl border-dashed border">
                            <v-row>
                                <v-col cols="12" md="4">
                                    <v-autocomplete v-model="customerForm.tinh" :items="provinces" item-title="name"
                                        item-value="code" label="Tỉnh / Thành phố" variant="outlined" density="comfortable"
                                        @update:model-value="(val) => { customerForm.thanhPho = null; customerForm.phuongXa = null; if (val) fetchDistricts(val); }" />
                                </v-col>

                                <v-col cols="12" md="4">
                                    <v-autocomplete v-model="customerForm.thanhPho" :items="districts" item-title="name"
                                        item-value="code" label="Quận / Huyện" variant="outlined" density="comfortable"
                                        :disabled="!customerForm.tinh"
                                        @update:model-value="(val) => { customerForm.phuongXa = null; if (val) fetchWards(val); }" />
                                </v-col>

                                <v-col cols="12" md="4">
                                    <v-autocomplete v-model="customerForm.phuongXa" :items="wards" item-title="name"
                                        item-value="code" label="Phường / Xã" variant="outlined" density="comfortable"
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
                            <div class="mt-2 text-slate-400 font-weight-bold">Chưa có địa chỉ nào được đăng ký</div>
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
                                    class="text-none ml-2 font-weight-black h-8 px-3 rounded-lg"
                                    @click="handleSetDefault(addr.id)">
                                    Mặc định
                                </v-btn>
                            </div>
                        </div>
                    </v-card-text>
                </v-card>

                <!-- Notes -->
                <v-card class="premium-card">
                    <v-card-text class="pa-8">
                        <div class="section-header d-flex align-center mb-6">
                            <div class="icon-blob bg-slate-100 mr-3">
                                <NoteIcon class="text-slate-600" size="20" />
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Ghi chú & Thông tin thêm</span>
                        </div>
                        <v-textarea v-model="customerForm.ghiChu" :readonly="isDetailView"
                            placeholder="Ghi chú về khách hàng (Sở thích, lưu ý giao hàng...)" variant="outlined" rows="3"
                            class="font-weight-bold" hide-details></v-textarea>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" lg="4">
                <v-card class="premium-card mb-6">
                    <v-card-text class="pa-8 text-center">
                        <div class="section-header d-flex align-center mb-6 text-left">
                            <div class="icon-blob bg-blue-lighten-5 mr-3">
                                <v-icon color="primary" size="18">mdi-camera</v-icon>
                            </div>
                            <span class="text-subtitle-1 font-weight-black text-slate-800">Ảnh chân dung</span>
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
                                density="comfortable" hide-details class="font-weight-medium bg-slate-50"></v-text-field>
                            <p class="text-caption font-weight-bold text-slate-400 mt-2 px-1">Gợi ý: Sử dụng ảnh .jpg hoặc
                                .png chất lượng cao.</p>
                        </div>
                    </v-card-text>
                </v-card>

                <v-card class="premium-card bg-primary-lighten-5 border-primary-lighten-4">
                    <v-card-text class="pa-8">
                        <div class="d-flex align-center mb-4">
                            <v-icon color="primary" size="24" class="mr-3">mdi-shield-check</v-icon>
                            <span class="text-subtitle-1 font-weight-black text-primary">Bảo mật tài khoản</span>
                        </div>
                        <p class="text-body-2 font-weight-bold text-slate-600 mb-0">Hệ thống sẽ tự động khởi tạo các thông số bảo mật khi hồ sơ được tạo thành công.</p>
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
                                            <span class="mono-font text-slate-500">{{ item.maSanPham || item.maSP || 'N/A' }}</span>
                                        </td>
                                        <td class="data-cell text-center">
                                            <span class="text-slate-800">{{ item.tenSanPham }}</span>
                                        </td>
                                        <td class="data-cell text-center">
                                            <span class="mono-font text-slate-500">{{ item.maBienThe || item.maCTSP || 'N/A' }}</span>
                                        </td>
                                        <td class="data-cell text-center">
                                            <span class="text-slate-600">{{ item.tenMauSac }} / {{ item.tenKichThuoc }}</span>
                                        </td>
                                        <td class="data-cell text-center">
                                            <span class="text-slate-600">{{ item.soLuong }}</span>
                                        </td>
                                        <td class="data-cell text-center text-primary" style="color: #1e257c !important;">
                                            {{ (item.donGia || item.giaTien || 0).toLocaleString() }}đ
                                        </td>
                                        <td class="data-cell text-center text-primary" style="color: #1e257c !important;">
                                            {{ (item.thanhTien || ((item.donGia || item.giaTien || 0) * (item.soLuong || 0))).toLocaleString() }}đ
                                        </td>
                                    </tr>
                                </tbody>
                                <tfoot>
                                    <tr class="bg-slate-50">
                                        <td colspan="7" class="data-cell text-right font-weight-black text-slate-800 py-4" style="font-size: 13px !important;">Tổng tiền:</td>
                                        <td class="data-cell text-right font-weight-black text-primary py-4" style="font-size: 13px !important; color: #1e257c !important;">
                                            {{ (selectedInvoice?.tongTienSauGiam || selectedInvoice?.tongTien || 0).toLocaleString() }}đ
                                        </td>
                                    </tr>
                                </tfoot>
                            </table>
                        </div>
                    </div>
                </v-card-text>
                
                <v-card-actions class="pa-4 bg-slate-50 border-t">
                    <v-spacer />
                    <v-btn color="slate-500" variant="text" class="text-none font-weight-bold" @click="invoiceDetailDialog = false">Đóng</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- ADDRESS DIALOG -->
        <v-dialog v-model="addrDialog" max-width="600" transition="dialog-bottom-transition">
            <v-card class="premium-card elevation-24">
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
/* Scoped styles removed in favor of global _admin-common.scss */
.italic {
    font-style: italic;
}
.camera-icon-bubble {
    border: 4px solid #fff;
    cursor: pointer;
    box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);
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
    font-family: 'Inter', 'Outfit', sans-serif !important;
    font-size: 14px !important;
}

:deep(.text-subtitle-1),
:deep(.text-h5) {
    font-family: 'Inter', 'Outfit', sans-serif !important;
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



:deep(.status-chip-active) {
    background-color: #f0f1ff !important;
    color: #1e257c !important;
    font-weight: 700 !important;
}
:deep(.status-chip-active .v-chip__content) {
    color: #1e257c !important;
}
:deep(.v-btn) {
    opacity: 1 !important;
}

/* Custom Table Styles to match main data table */
.purchase-history-table, .invoice-items-table {
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

.history-row:hover, .detail-row:hover {
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
    font-family: 'JetBrains Mono', 'Courier New', monospace !important;
}

:deep(.status-chip-inactive) {
    background-color: #f8fafc !important;
    color: #64748b !important;
    font-weight: 700 !important;
}
:deep(.status-chip-inactive .v-chip__content) {
    color: #64748b !important;
}

:deep(.status-chip) {
    border-radius: 12px !important;
    font-size: 13px !important;
    padding: 0 16px !important;
    min-height: 28px !important;
    display: inline-flex !important;
    align-items: center !important;
    justify-content: center !important;
}
</style>

<style>
/* 
   FORCE GLOBAL OVERRIDES FOR THIS PAGE 
   Sử dụng ID cụ thể để đảm bảo độ ưu tiên cao nhất, vượt qua mọi file CSS khác
*/
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
    font-family: 'Inter', 'Outfit', sans-serif !important;
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
    font-family: 'JetBrains Mono', monospace !important;
    font-size: 13px !important;
}
</style>