<script setup>
/**
 * Module: Bán hàng tại quầy (Admin)
 * Component: CustomerSelector
 * Chức năng: Tìm kiếm, chọn/tạo mới khách hàng trực tiếp và thiết lập thông tin giao nhận hàng.
 */
import { computed, ref, watch, onMounted } from 'vue';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { useNotifications } from '@/services/notificationService';
import { useLocation } from '@/composables/useLocation';
import { useAddressMapping } from '@/composables/useAddressMapping';

const props = defineProps({
    order: {
        type: Object,
        required: true
    }
});

const emit = defineEmits(['select', 'remove', 'update-shipping']);
const { addNotification } = useNotifications();

const search = ref('');
const results = ref([]);
const loading = ref(false);
const quickAddLoading = ref(false);
let searchTimer = null;

// Customer Profile details loading
const customerDetails = ref(null);
const loadingDetails = ref(false);
const selectedCustomerAddresses = ref([]);
const loadingAddresses = ref(false);

const { provinces, districts, wards, loadingLocations, fetchProvinces, fetchDistricts, fetchWards } = useLocation();
const { mapCodesToNames } = useAddressMapping();

// Customer Form State
const customerForm = ref({
    id: null,
    ten: '',
    sdt: '',
    email: '',
    ngaySinh: '',
    gioiTinh: true
});

// Shipping Form State
const isShipping = ref(false);
const selectedAddressId = ref(null);
const shippingForm = ref({
    tenNguoiNhan: '',
    sdtNguoiNhan: '',
    diaChiChiTiet: '',
    tinh: null,
    thanhPho: null,
    phuongXa: null,
    ngayDuKienNhan: '',
    phiVanChuyen: 0
});

// Format helpers
const cleanName = (val) => {
    if (!val) return '';
    return String(val)
        .toLowerCase()
        .replace(/^(tỉnh|thành phố|thành phó|quận|huyện|thị xã|phường|xã|thị trấn)\s+/i, '')
        .trim();
};

const normalizeText = (value) => String(value ?? '').trim().toLowerCase();
const normalizePhone = (value) => String(value ?? '').replace(/\D/g, '');

const extractCustomerList = (response) => {
    if (Array.isArray(response)) return response;
    if (Array.isArray(response?.content)) return response.content;
    if (Array.isArray(response?.data)) return response.data;
    if (Array.isArray(response?.data?.content)) return response.data.content;
    return [];
};

const findExactCustomer = (list, phone, email) => {
    const normalizedPhone = normalizePhone(phone);
    const normalizedEmail = normalizeText(email);

    return list.find((customer) => {
        const customerPhone = normalizePhone(customer?.sdt);
        const customerEmail = normalizeText(customer?.email);

        if (normalizedPhone && customerPhone === normalizedPhone) return true;
        if (normalizedEmail && customerEmail === normalizedEmail) return true;
        return false;
    });
};

const findExistingCustomerByContact = async (phone, email) => {
    const keywords = [phone, email].filter((keyword) => normalizeText(keyword).length > 0);

    for (const keyword of keywords) {
        try {
            const searchData = await dichVuDonHang.searchKhachHang(keyword);
            const matched = findExactCustomer(Array.isArray(searchData) ? searchData : [], phone, email);
            if (matched) return matched;
        } catch (e) {
            console.error(e);
        }
    }

    const allCustomers = await dichVuKhachHang.layTatCaKhachHang();
    return findExactCustomer(extractCustomerList(allCustomers), phone, email) || null;
};

// Autocomplete Search
watch(search, (value) => {
    if (searchTimer) clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
        onSearch(value);
    }, 300);
});

const onSearch = async (keyword = search.value) => {
    const normalizedKeyword = normalizeText(keyword);
    if (!normalizedKeyword || normalizedKeyword.length < 2) {
        results.value = [];
        return;
    }
    loading.value = true;
    try {
        const data = await dichVuDonHang.searchKhachHang(normalizedKeyword);
        results.value = data || [];
    } catch (e) {
        console.error(e);
    } finally {
        loading.value = false;
    }
};

// Select Customer Obj
const selectedCustomerObj = computed(() => {
    if (!props.order?.idKhachHang) return null;
    return {
        id: props.order.idKhachHang,
        ten: props.order.tenKhachHang || 'Khách lẻ',
        sdt: props.order.sdtKhachHang || ''
    };
});

const selectCustomer = async (c) => {
    if (!c) {
        emit('remove');
        resetCustomerForm();
        return;
    }

    if (typeof c === 'object' && c.id) {
        emit('select', c);
        search.value = '';
        results.value = [];
    } else {
        // User typed a new name
        emit('remove');
        resetCustomerForm();
        customerForm.value.ten = typeof c === 'string' ? c : c.ten || '';
    }
};

const resetCustomerForm = () => {
    customerForm.value = {
        id: null,
        ten: '',
        sdt: '',
        email: '',
        ngaySinh: '',
        gioiTinh: true
    };
    selectedAddressId.value = null;
    selectedCustomerAddresses.value = [];
};

const resetCustomerFormAndClearParent = () => {
    emit('remove');
    resetCustomerForm();
};

const removeSelectedCustomer = () => {
    emit('remove');
    resetCustomerForm();
};

// Save or Update Customer Details
const saveOrUpdateCustomer = async () => {
    const name = customerForm.value.ten?.trim();
    const phone = customerForm.value.sdt?.trim();

    if (!name || name === 'Khách lẻ') {
        addNotification({ title: 'Cảnh báo', subtitle: 'Vui lòng nhập tên khách hàng hợp lệ.', color: 'warning' });
        return;
    }
    if (!phone) {
        addNotification({ title: 'Cảnh báo', subtitle: 'Vui lòng nhập số điện thoại.', color: 'warning' });
        return;
    }

    const phoneRegex = /^(0[3|5|7|8|9])[0-9]{8}$/;
    if (!phoneRegex.test(phone)) {
        addNotification({ title: 'SĐT không hợp lệ', subtitle: 'Số điện thoại phải có 10 số và bắt đầu bằng 03, 05, 07, 08, hoặc 09.', color: 'warning' });
        return;
    }

    const email = customerForm.value.email?.trim();
    if (email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            addNotification({ title: 'Email không hợp lệ', subtitle: 'Vui lòng nhập đúng định dạng email.', color: 'warning' });
            return;
        }
    }

    quickAddLoading.value = true;
    try {
        const payload = {
            ten: name,
            sdt: phone,
            email: email || '',
            gioiTinh: customerForm.value.gioiTinh,
            ngaySinh: customerForm.value.ngaySinh || null,
            trangThai: 'DANG_HOAT_DONG',
            ghiChu: 'Cập nhật từ màn bán hàng tại quầy'
        };

        let savedCustomer = null;
        if (customerForm.value.id) {
            savedCustomer = await dichVuKhachHang.capNhatKhachHang(customerForm.value.id, payload);
            addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật thông tin khách hàng.', color: 'success' });
        } else {
            const existed = await findExistingCustomerByContact(phone, email);
            if (existed) {
                emit('select', existed);
                addNotification({ title: 'Đã tồn tại', subtitle: `Khách hàng này đã có trên hệ thống, tự động chọn khách hàng: ${existed.ten}`, color: 'info' });
                quickAddLoading.value = false;
                return;
            }

            savedCustomer = await dichVuKhachHang.taoKhachHang({
                ...payload,
                tenTaiKhoan: '',
                matKhau: ''
            });
            addNotification({ title: 'Thành công', subtitle: 'Đã thêm mới và chọn khách hàng.', color: 'success' });
        }

        if (savedCustomer) {
            emit('select', savedCustomer);
        }
    } catch (e) {
        console.error('Lỗi khi lưu khách hàng:', e);
        addNotification({ title: 'Lỗi', subtitle: e?.response?.data?.message || 'Không thể lưu thông tin khách hàng.', color: 'error' });
    } finally {
        quickAddLoading.value = false;
    }
};

// Shipping Address Handlers
watch(selectedAddressId, (newId) => {
    if (!newId) return;
    const addr = selectedCustomerAddresses.value.find(a => a.id === newId);
    if (addr) {
        shippingForm.value.tenNguoiNhan = addr.tenNguoiNhan || customerForm.value.ten;
        shippingForm.value.sdtNguoiNhan = addr.sdtNguoiNhan || customerForm.value.sdt;
        shippingForm.value.diaChiChiTiet = addr.diaChiChiTiet || '';
        updateLocationFromAddress(addr);
    }
});

const updateLocationFromAddress = async (addr) => {
    try {
        await fetchProvinces();
        const p = provinces.value.find(x => cleanName(x.name) === cleanName(addr.tinh) || x.code === addr.tinh);
        if (p) {
            shippingForm.value.tinh = p.code;
            await fetchDistricts(p.code);
            const d = districts.value.find(x => cleanName(x.name) === cleanName(addr.thanhPho) || x.code === addr.thanhPho);
            if (d) {
                shippingForm.value.thanhPho = d.code;
                await fetchWards(d.code);
                const w = wards.value.find(x => cleanName(x.name) === cleanName(addr.phuongXa) || x.code === addr.phuongXa);
                if (w) {
                    shippingForm.value.phuongXa = w.code;
                }
            }
        }
    } catch (e) {
        console.error('Lỗi khi nạp địa chỉ:', e);
    }
};

const onProvinceChange = async (val) => {
    shippingForm.value.thanhPho = null;
    shippingForm.value.phuongXa = null;
    if (val) {
        await fetchDistricts(val);
    }
};

const onDistrictChange = async (val) => {
    shippingForm.value.phuongXa = null;
    if (val) {
        await fetchWards(val);
    }
};

// Listen and load customer info on active order change
watch(
    () => props.order?.idKhachHang,
    async (newId) => {
        if (newId) {
            loadingDetails.value = true;
            try {
                const details = await dichVuKhachHang.layChiTietKhachHang(newId);
                customerDetails.value = details;

                customerForm.value = {
                    id: details.id,
                    ten: details.ten || details.tenKhachHang || '',
                    sdt: details.sdt || '',
                    email: details.email || '',
                    ngaySinh: details.ngaySinh || '',
                    gioiTinh: details.gioiTinh === true || details.gioiTinh === 1 || String(details.gioiTinh).toLowerCase() === 'true'
                };

                loadingAddresses.value = true;
                const res = await dichVuKhachHang.layDanhSachDiaChi(newId);
                let data = [];
                if (Array.isArray(res)) data = res;
                else if (Array.isArray(res?.data)) data = res.data;
                else if (Array.isArray(res?.data?.content)) data = res.data.content;
                selectedCustomerAddresses.value = data;
            } catch (e) {
                console.error(e);
            } finally {
                loadingDetails.value = false;
                loadingAddresses.value = false;
            }
        } else {
            customerDetails.value = null;
            resetCustomerForm();
        }
    },
    { immediate: true }
);

// Listen to shipping form changes to sync back to parent order
watch(
    [shippingForm, provinces, districts, wards],
    () => {
        const hasInfo = !!(
            shippingForm.value.tenNguoiNhan?.trim() ||
            shippingForm.value.sdtNguoiNhan?.trim() ||
            shippingForm.value.diaChiChiTiet?.trim() ||
            shippingForm.value.tinh
        );

        if (!hasInfo) {
            isShipping.value = false;
            emit('update-shipping', {
                loaiDon: 'TAI_QUAY',
                soDienThoaiNguoiNhan: '',
                diaChiNguoiNhan: '',
                ngayDuKienNhan: null,
                phiVanChuyen: 0
            });
            return;
        }

        isShipping.value = true;

        const prov = provinces.value.find(p => p.code === shippingForm.value.tinh)?.name || '';
        const dist = districts.value.find(d => d.code === shippingForm.value.thanhPho)?.name || '';
        const ward = wards.value.find(w => w.code === shippingForm.value.phuongXa)?.name || '';

        const fullAddress = [
            shippingForm.value.diaChiChiTiet?.trim(),
            ward,
            dist,
            prov
        ].filter(Boolean).join(', ');

        emit('update-shipping', {
            loaiDon: 'GIAO_HANG',
            soDienThoaiNguoiNhan: shippingForm.value.sdtNguoiNhan || '',
            diaChiNguoiNhan: fullAddress,
            ngayDuKienNhan: shippingForm.value.ngayDuKienNhan ? new Date(shippingForm.value.ngayDuKienNhan).getTime() : null,
            phiVanChuyen: Number(shippingForm.value.phiVanChuyen || 0)
        });
    },
    { deep: true }
);

// Initialize Order Shipping Values on change
watch(
    () => props.order,
    async (newOrder) => {
        if (!newOrder) return;
        isShipping.value = newOrder.loaiDon === 'GIAO_HANG';

        if (newOrder.loaiDon === 'GIAO_HANG') {
            shippingForm.value.sdtNguoiNhan = newOrder.soDienThoaiNguoiNhan || '';
            shippingForm.value.phiVanChuyen = newOrder.phiVanChuyen || 0;

            if (newOrder.ngayDuKienNhan) {
                const date = new Date(newOrder.ngayDuKienNhan);
                const yyyy = date.getFullYear();
                const mm = String(date.getMonth() + 1).padStart(2, '0');
                const dd = String(date.getDate()).padStart(2, '0');
                shippingForm.value.ngayDuKienNhan = `${yyyy}-${mm}-${dd}`;
            } else {
                shippingForm.value.ngayDuKienNhan = '';
            }

            if (newOrder.diaChiNguoiNhan) {
                const parts = newOrder.diaChiNguoiNhan.split(',').map(s => s.trim());
                if (parts.length >= 4) {
                    shippingForm.value.diaChiChiTiet = parts[0];
                    await fetchProvinces();
                    const p = provinces.value.find(x => cleanName(x.name) === cleanName(parts[parts.length - 1]));
                    if (p) {
                        shippingForm.value.tinh = p.code;
                        await fetchDistricts(p.code);
                        const d = districts.value.find(x => cleanName(x.name) === cleanName(parts[parts.length - 2]));
                        if (d) {
                            shippingForm.value.thanhPho = d.code;
                            await fetchWards(d.code);
                            const w = wards.value.find(x => cleanName(x.name) === cleanName(parts[parts.length - 3]));
                            if (w) {
                                shippingForm.value.phuongXa = w.code;
                            }
                        }
                    }
                } else {
                    shippingForm.value.diaChiChiTiet = newOrder.diaChiNguoiNhan;
                }
            }
        }
    },
    { immediate: true }
);

onMounted(async () => {
    if (provinces.value.length === 0) {
        await fetchProvinces();
    }
});
</script>

<template>
    <div class="pos-customer-flow d-flex flex-column gap-4">
        <!-- Card 1: Khách hàng -->
        <v-card class="pos-form-card" variant="flat">
            <div class="d-flex justify-space-between align-center mb-3">
                <span class="pos-form-card-title">Khách hàng</span>
                <div class="d-flex align-center gap-2">
                    <v-select
                        v-model="customerForm.gioiTinh"
                        :items="[
                            { title: 'Nam', value: true },
                            { title: 'Nữ', value: false }
                        ]"
                        item-title="title"
                        item-value="value"
                        variant="plain"
                        density="compact"
                        hide-details
                        class="gender-select"
                        style="max-width: 90px; font-size: 13px;"
                    />
                    
                    <v-menu>
                        <template #activator="{ props: menuProps }">
                            <v-btn icon="mdi-dots-horizontal" variant="text" size="small" v-bind="menuProps" />
                        </template>
                        <v-list density="compact">
                            <v-list-item prepend-icon="mdi-account-plus-outline" @click="resetCustomerFormAndClearParent">
                                <v-list-item-title style="font-size: 13px;">Thêm mới/Làm trống</v-list-item-title>
                            </v-list-item>
                            <v-list-item prepend-icon="mdi-content-save-outline" @click="saveOrUpdateCustomer" :loading="quickAddLoading">
                                <v-list-item-title style="font-size: 13px;">
                                    {{ customerForm.id ? 'Cập nhật khách hàng' : 'Lưu khách hàng mới' }}
                                </v-list-item-title>
                            </v-list-item>
                            <v-list-item v-if="customerForm.id" prepend-icon="mdi-account-remove-outline" @click="removeSelectedCustomer">
                                <v-list-item-title style="font-size: 13px; color: #ef4444;">Bỏ chọn khách hàng</v-list-item-title>
                            </v-list-item>
                        </v-list>
                    </v-menu>
                </div>
            </div>

            <!-- Customer inputs -->
            <v-row dense class="mx-0">
                <v-col cols="6" class="pos-input-col">
                    <v-combobox
                        :model-value="selectedCustomerObj"
                        v-model:search="search"
                        :items="results"
                        :loading="loading"
                        :custom-filter="() => true"
                        item-title="ten"
                        item-value="id"
                        label="Tên khách hàng"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="pos-input-field"
                        @update:model-value="selectCustomer"
                    >
                        <template #item="{ props: itemProps, item }">
                            <v-list-item v-bind="itemProps" class="py-1">
                                <template #title>
                                    <span class="font-weight-bold text-slate-800" style="font-size: 13px;">
                                        {{ item.raw.ten || item.raw.tenKhachHang || 'Chưa rõ tên' }}
                                    </span>
                                </template>
                                <template #subtitle>
                                    <span class="text-slate-500" style="font-size: 11px;">
                                        SĐT: {{ item.raw.sdt || 'N/A' }} - Email: {{ item.raw.email || 'N/A' }}
                                    </span>
                                </template>
                            </v-list-item>
                        </template>
                        <template #selection="{ item }">
                            <span style="font-size: 13px; font-weight: 500;">
                                {{ item.raw.ten || item.raw.tenKhachHang || 'Khách lẻ' }}
                            </span>
                        </template>
                    </v-combobox>
                </v-col>
                
                <v-col cols="6" class="pos-input-col">
                    <v-text-field
                        v-model="customerForm.sdt"
                        label="SĐT"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="pos-input-field"
                        @input="customerForm.sdt = String($event.target.value || '').replace(/[^0-9]/g, '')"
                    />
                </v-col>
                
                <v-col cols="6" class="pos-input-col">
                    <v-text-field
                        v-model="customerForm.email"
                        label="Địa chỉ email"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="pos-input-field"
                    />
                </v-col>
                
                <v-col cols="6" class="pos-input-col">
                    <v-text-field
                        v-model="customerForm.ngaySinh"
                        type="date"
                        label="Ngày sinh"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="pos-input-field"
                    />
                </v-col>
            </v-row>
        </v-card>

        <!-- Card 2: Nhận hàng -->
        <v-card class="pos-form-card" variant="flat">
            <div class="d-flex justify-space-between align-center mb-3">
                <span class="pos-form-card-title">Nhận hàng</span>
                
                <!-- Choose address dropdown -->
                <v-select
                    v-model="selectedAddressId"
                    :items="selectedCustomerAddresses"
                    item-title="diaChiChiTiet"
                    item-value="id"
                    label="Chọn địa chỉ"
                    variant="plain"
                    density="compact"
                    hide-details
                    style="max-width: 150px; font-size: 13px;"
                    :disabled="!selectedCustomerAddresses.length"
                >
                    <template #selection="{ item }">
                        <span style="font-size: 12px; color: #1e3a8a; font-weight: 500;">
                            {{ item.raw.diaChiChiTiet }}
                        </span>
                    </template>
                </v-select>
            </div>

            <div class="shipping-fields-container">
                <!-- Delivery expected date -->
                <div class="d-flex justify-space-between align-center mb-3">
                    <span class="text-slate-600 font-weight-medium" style="font-size: 13px;">Dự kiến nhận hàng</span>
                    <v-text-field
                        v-model="shippingForm.ngayDuKienNhan"
                        type="date"
                        variant="outlined"
                        density="compact"
                        hide-details
                        class="pos-input-field date-picker-input"
                        style="max-width: 160px;"
                    />
                </div>

                <!-- Recipient Info -->
                <v-row dense class="mx-0">
                    <v-col cols="6" class="pos-input-col">
                        <v-text-field
                            v-model="shippingForm.tenNguoiNhan"
                            label="Tên người nhận"
                            variant="outlined"
                            density="compact"
                            hide-details
                            class="pos-input-field"
                        />
                    </v-col>
                    
                    <v-col cols="6" class="pos-input-col">
                        <v-text-field
                            v-model="shippingForm.sdtNguoiNhan"
                            label="Số điện thoại"
                            variant="outlined"
                            density="compact"
                            hide-details
                            class="pos-input-field"
                            @input="shippingForm.sdtNguoiNhan = String($event.target.value || '').replace(/[^0-9]/g, '')"
                        />
                    </v-col>
                    
                    <v-col cols="12" class="pos-input-col">
                        <v-text-field
                            v-model="shippingForm.diaChiChiTiet"
                            label="Địa chỉ chi tiết"
                            variant="outlined"
                            density="compact"
                            hide-details
                            class="pos-input-field"
                        />
                    </v-col>
                    
                    <!-- Cascading dropdowns -->
                    <v-col cols="4" class="pos-input-col">
                        <v-autocomplete
                            v-model="shippingForm.tinh"
                            :items="provinces"
                            item-title="name"
                            item-value="code"
                            label="Tỉnh/TP"
                            variant="outlined"
                            density="compact"
                            hide-details
                            :loading="loadingLocations.provinces"
                            @update:model-value="onProvinceChange"
                            class="pos-input-field"
                        />
                    </v-col>
                    
                    <v-col cols="4" class="pos-input-col">
                        <v-autocomplete
                            v-model="shippingForm.thanhPho"
                            :items="districts"
                            item-title="name"
                            item-value="code"
                            label="Quận/Huyện"
                            variant="outlined"
                            density="compact"
                            hide-details
                            :loading="loadingLocations.districts"
                            :disabled="!shippingForm.tinh"
                            @update:model-value="onDistrictChange"
                            class="pos-input-field"
                        />
                    </v-col>
                    
                    <v-col cols="4" class="pos-input-col">
                        <v-autocomplete
                            v-model="shippingForm.phuongXa"
                            :items="wards"
                            item-title="name"
                            item-value="code"
                            label="Phường/Xã"
                            variant="outlined"
                            density="compact"
                            hide-details
                            :loading="loadingLocations.wards"
                            :disabled="!shippingForm.thanhPho"
                            class="pos-input-field"
                        />
                    </v-col>

                    <!-- Shipping cost -->
                    <v-col cols="12" class="pos-input-col">
                        <v-text-field
                            v-model.number="shippingForm.phiVanChuyen"
                            label="Phí vận chuyển (VND)"
                            type="number"
                            variant="outlined"
                            density="compact"
                            hide-details
                            class="pos-input-field"
                        />
                    </v-col>
                </v-row>
            </div>
        </v-card>
    </div>
</template>

<style scoped>
.pos-form-card {
    background-color: #ffffff !important;
    border: 1px solid #e2e8f0 !important;
    border-radius: 12px !important;
    padding: 16px !important;
    box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05) !important;
}

.pos-form-card-title {
    font-size: 14px;
    font-weight: 700;
    color: #1e293b;
}

.pos-input-col {
    padding: 6px !important;
}

.pos-input-field :deep(.v-field) {
    border-radius: 8px !important;
    background-color: #f8fafc !important;
}

.pos-input-field :deep(.v-field__outline) {
    color: #e2e8f0 !important;
    --v-field-border-width: 1px !important;
    --v-field-border-opacity: 1 !important;
}

.pos-input-field :deep(.v-field:hover .v-field__outline) {
    color: #cbd5e1 !important;
}

.pos-input-field :deep(.v-field--focused .v-field__outline) {
    color: #2E4E8E !important;
}

.pos-input-field :deep(.v-field__input),
.pos-input-field :deep(.v-field__input input),
.pos-input-field :deep(.v-select__selection-text),
.pos-input-field :deep(.v-select__selection) {
    font-size: 13px !important;
    color: #334155 !important;
    font-weight: 400 !important;
}

.pos-input-field :deep(input::placeholder),
.pos-input-field :deep(.v-field__input::placeholder),
.pos-input-field :deep(.v-label) {
    font-size: 13px !important;
    color: #94a3b8 !important;
    opacity: 0.8 !important;
    font-weight: 400 !important;
}

.gender-select :deep(.v-select__selection-text) {
    color: #2e4e8e !important;
    font-weight: 600 !important;
    font-size: 13px !important;
}

.shipping-checkbox {
    transform: scale(0.85);
    margin-left: -8px;
}

.date-picker-input :deep(input[type="date"]::-webkit-calendar-picker-indicator) {
    cursor: pointer;
}
</style>
