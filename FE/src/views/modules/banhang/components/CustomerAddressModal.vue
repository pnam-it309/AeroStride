<script setup>
import { ref, watch, computed } from 'vue';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { useLocation } from '@/composables/useLocation';
import { useAddressMapping } from '@/composables/useAddressMapping';
import { useNotifications } from '@/services/notificationService';
import { MapPinIcon, PlusIcon, XIcon } from 'vue-tabler-icons';

const props = defineProps({
    modelValue: {
        type: Boolean,
        default: false
    },
    customer: {
        type: Object,
        default: null
    },
    selectedAddressId: {
        type: String,
        default: ''
    },
    currentShipping: {
        type: Object,
        default: null
    }
});

const emit = defineEmits(['update:modelValue', 'select-address', 'address-changed']);

const { addNotification } = useNotifications();
const { provinces, districts, wards, loadingLocations, fetchProvinces, fetchDistricts, fetchWards } = useLocation();
const { mapCodesToNames } = useAddressMapping();

const addresses = ref([]);
const loading = ref(false);
const showAddForm = ref(false);
const submitting = ref(false);

const isCurrentAddress = (addr) => {
    if (!addr) return false;
    // 1. Ưu tiên so sánh chính xác theo ID địa chỉ nếu đã chọn
    if (props.selectedAddressId) {
        return String(props.selectedAddressId) === String(addr.id);
    }

    // 2. Nếu chưa có selectedAddressId, so sánh theo toàn bộ thông tin nhận hàng trên form (Bao gồm Tỉnh/Huyện/Xã)
    if (props.currentShipping && props.currentShipping.detail) {
        const currentDetail = String(props.currentShipping.detail || '').trim().toLowerCase();
        const currentName = String(props.currentShipping.name || '').trim().toLowerCase();
        const currentPhone = String(props.currentShipping.phone || '').replace(/\D/g, '');
        const currentProvince = String(props.currentShipping.province || '').trim().toLowerCase();
        const currentDistrict = String(props.currentShipping.district || '').trim().toLowerCase();
        const currentWard = String(props.currentShipping.ward || '').trim().toLowerCase();

        const addrDetail = String(addr.diaChiChiTiet || '').trim().toLowerCase();
        const addrName = String(addr.tenNguoiNhan || props.customer?.ten || '').trim().toLowerCase();
        const addrPhone = String(addr.sdtNguoiNhan || props.customer?.sdt || '').replace(/\D/g, '');
        const addrProvince = String(addr.tinh || '').trim().toLowerCase();
        const addrDistrict = String(addr.thanhPho || '').trim().toLowerCase();
        const addrWard = String(addr.phuongXa || '').trim().toLowerCase();

        const matchDetail = currentDetail !== '' && currentDetail === addrDetail;
        const matchName = currentName === '' || currentName === addrName;
        const matchPhone = currentPhone === '' || currentPhone === addrPhone;

        // Đối soát các cấp Tỉnh, Huyện, Xã nếu form đã chọn
        let matchLocation = true;
        if (currentProvince && addrProvince) {
            matchLocation = matchLocation && (currentProvince === addrProvince || addrProvince.includes(currentProvince) || currentProvince.includes(addrProvince));
        }
        if (currentDistrict && addrDistrict) {
            matchLocation = matchLocation && (currentDistrict === addrDistrict || addrDistrict.includes(currentDistrict) || currentDistrict.includes(addrDistrict));
        }
        if (currentWard && addrWard) {
            matchLocation = matchLocation && (currentWard === addrWard || addrWard.includes(currentWard) || currentWard.includes(addrWard));
        }

        if (matchDetail && matchName && matchPhone && matchLocation) {
            return true;
        }
    }
    return false;
};

const newAddressForm = ref({
    tenNguoiNhan: '',
    sdtNguoiNhan: '',
    tinh: null,
    thanhPho: null,
    phuongXa: null,
    diaChiChiTiet: '',
    laMacDinh: false
});

const customerName = computed(() => props.customer?.hoTen || props.customer?.ten || 'Khách hàng');

const fetchCustomerAddresses = async () => {
    if (!props.customer?.id) {
        addresses.value = [];
        return;
    }
    loading.value = true;
    try {
        const res = await dichVuKhachHang.layDanhSachDiaChi(props.customer.id);
        const list = res?.data || res || [];
        addresses.value = Array.isArray(list) ? list : [];
    } catch (err) {
        console.error('Lỗi lấy sổ địa chỉ:', err);
        addresses.value = [];
    } finally {
        loading.value = false;
    }
};

watch(() => props.modelValue, async (val) => {
    if (val) {
        showAddForm.value = false;
        await fetchCustomerAddresses();
        if (provinces.value.length === 0) {
            await fetchProvinces();
        }
    }
});

const handleSelectAddress = (addr) => {
    emit('select-address', addr);
    emit('update:modelValue', false);
};

const openAddForm = () => {
    newAddressForm.value = {
        tenNguoiNhan: props.customer?.hoTen || props.customer?.ten || '',
        sdtNguoiNhan: props.customer?.sdt || '',
        tinh: null,
        thanhPho: null,
        phuongXa: null,
        diaChiChiTiet: '',
        laMacDinh: addresses.value.length === 0
    };
    showAddForm.value = true;
};

const handleSaveAddress = async () => {
    if (!newAddressForm.value.tenNguoiNhan || !newAddressForm.value.sdtNguoiNhan) {
        addNotification({ title: 'Thiếu thông tin', subtitle: 'Vui lòng nhập Tên và SĐT người nhận.', color: 'warning' });
        return;
    }
    if (!newAddressForm.value.tinh || !newAddressForm.value.thanhPho || !newAddressForm.value.phuongXa || !newAddressForm.value.diaChiChiTiet) {
        addNotification({ title: 'Thiếu địa chỉ', subtitle: 'Vui lòng chọn đầy đủ Tỉnh/Thành, Quận/Huyện, Phường/Xã và Địa chỉ chi tiết.', color: 'warning' });
        return;
    }

    submitting.value = true;
    try {
        const mapped = mapCodesToNames(newAddressForm.value, provinces.value, districts.value, wards.value);
        const payload = {
            idKhachHang: props.customer.id,
            tenNguoiNhan: mapped.tenNguoiNhan,
            sdtNguoiNhan: mapped.sdtNguoiNhan,
            tinh: mapped.tinh,
            thanhPho: mapped.thanhPho,
            phuongXa: mapped.phuongXa,
            diaChiChiTiet: mapped.diaChiChiTiet,
            laMacDinh: newAddressForm.value.laMacDinh
        };

        await dichVuKhachHang.taoDiaChi(payload);
        addNotification({ title: 'Thành công', subtitle: 'Đã thêm địa chỉ mới cho khách hàng.', color: 'success' });
        showAddForm.value = false;
        await fetchCustomerAddresses();
        emit('address-changed');
    } catch (err) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể lưu địa chỉ mới.', color: 'error' });
    } finally {
        submitting.value = false;
    }
};

const close = () => {
    emit('update:modelValue', false);
};
</script>

<template>
    <v-dialog :model-value="modelValue" @update:model-value="(val) => emit('update:modelValue', val)" max-width="850" transition="dialog-bottom-transition" persistent>
        <v-card class="rounded-xl overflow-hidden shadow-2xl">
            <!-- Header -->
            <v-card-title class="pa-5 bg-slate-50 border-b d-flex justify-space-between align-center">
                <div class="d-flex align-center ga-3">
                    <div class="bg-primary-lighten-5 pa-2 rounded-lg text-primary d-flex align-center">
                        <MapPinIcon size="24" />
                    </div>
                    <div>
                        <div class="text-subtitle-1 font-weight-bold text-slate-800">
                            Quản lý địa chỉ — <span class="text-primary">{{ customerName }}</span>
                        </div>
                        <div class="text-caption text-slate-500">
                            Chọn địa chỉ nhận hàng có sẵn hoặc thêm địa chỉ mới cho khách hàng
                        </div>
                    </div>
                </div>
                <v-btn icon variant="text" size="small" color="slate-400" @click="close">
                    <XIcon size="20" />
                </v-btn>
            </v-card-title>

            <!-- Body -->
            <v-card-text class="pa-6" style="max-height: 520px; overflow-y: auto;">
                <div v-if="loading" class="d-flex justify-center align-center py-12">
                    <v-progress-circular indeterminate color="primary" size="36" />
                </div>

                <div v-else>
                    <v-row>
                        <!-- Left Column: Address List -->
                        <v-col cols="12" :md="showAddForm ? 6 : 12">
                            <div class="d-flex justify-space-between align-center mb-4">
                                <span class="text-subtitle-2 font-weight-bold text-slate-700">
                                    Địa chỉ hiện tại ({{ addresses.length }})
                                </span>
                                <v-btn v-if="!showAddForm" variant="tonal" color="primary" size="small" class="text-none font-weight-semibold rounded-lg" @click="openAddForm">
                                    <PlusIcon size="16" class="mr-1" /> Thêm địa chỉ mới
                                </v-btn>
                            </div>

                            <div v-if="addresses.length === 0" class="text-center py-8 border-dashed rounded-xl bg-slate-50">
                                <v-icon size="40" color="slate-300">mdi-map-marker-off-outline</v-icon>
                                <div class="text-body-2 text-slate-500 mt-2">Khách hàng chưa có địa chỉ nào được lưu.</div>
                                <v-btn variant="flat" color="primary" size="small" class="mt-3 text-none font-weight-bold rounded-lg" @click="openAddForm">
                                    + Thêm địa chỉ đầu tiên
                                </v-btn>
                            </div>

                            <div v-else class="d-flex flex-column ga-3">
                                <div v-for="addr in addresses" :key="addr.id"
                                    @click="handleSelectAddress(addr)"
                                    class="address-card position-relative pa-4 rounded-xl border transition-all cursor-pointer mb-1"
                                    :class="{
                                        'active-card border-primary bg-blue-50/50 shadow-sm': isCurrentAddress(addr),
                                        'hover-card border-slate-200 bg-white': !isCurrentAddress(addr)
                                    }">
                                    
                                    <div class="d-flex justify-space-between align-start mb-2">
                                        <div class="d-flex align-center flex-wrap ga-2">
                                            <span class="font-weight-bold text-slate-800 text-body-2">{{ addr.tenNguoiNhan || customerName }}</span>
                                            
                                            <!-- Badge Đang áp dụng -->
                                            <span v-if="isCurrentAddress(addr)" class="px-2 py-0-5 rounded-md text-caption font-weight-bold bg-primary text-white d-inline-flex align-center ga-1" style="font-size: 11px !important;">
                                                <v-icon size="12" color="white">mdi-check-circle</v-icon> Đang áp dụng
                                            </span>

                                            <!-- Badge Mặc định -->
                                            <span v-if="addr.laMacDinh" class="default-badge px-2 py-0-5 rounded-md text-caption font-weight-bold bg-amber-100 text-amber-800">
                                                Mặc định
                                            </span>
                                        </div>
                                    </div>

                                    <div class="text-caption text-slate-600 mb-1 d-flex align-center ga-2">
                                        <v-icon size="14" color="slate-400">mdi-phone-outline</v-icon>
                                        <span>{{ addr.sdtNguoiNhan || props.customer?.sdt || 'Chưa có SĐT' }}</span>
                                    </div>

                                    <div class="text-caption text-slate-700 font-weight-medium d-flex align-start ga-2">
                                        <v-icon size="14" color="slate-400" class="mt-0-5">mdi-map-marker-outline</v-icon>
                                        <span>
                                            {{ addr.diaChiChiTiet ? addr.diaChiChiTiet + ', ' : '' }}
                                            {{ addr.phuongXa ? addr.phuongXa + ', ' : '' }}
                                            {{ addr.thanhPho ? addr.thanhPho + ', ' : '' }}
                                            {{ addr.tinh || '' }}
                                        </span>
                                    </div>

                                    <div v-if="isCurrentAddress(addr)" class="active-check-badge">
                                        <v-icon size="14" color="white">mdi-check</v-icon>
                                    </div>
                                </div>
                            </div>
                        </v-col>

                        <!-- Right Column: Add New Address Form -->
                        <v-col v-if="showAddForm" cols="12" md="6" class="border-l pl-5">
                            <div class="d-flex justify-space-between align-center mb-4">
                                <span class="text-subtitle-2 font-weight-bold text-slate-800">Thêm địa chỉ mới</span>
                                <v-btn icon size="x-small" variant="text" color="slate-400" @click="showAddForm = false">
                                    <XIcon size="16" />
                                </v-btn>
                            </div>

                            <div class="d-flex flex-column ga-3">
                                <v-text-field v-model="newAddressForm.tenNguoiNhan" label="Tên người nhận *" variant="outlined" density="compact" hide-details class="text-body-2" />
                                <v-text-field v-model="newAddressForm.sdtNguoiNhan" label="SĐT người nhận *" variant="outlined" density="compact" hide-details class="text-body-2" />
                                
                                <v-autocomplete v-model="newAddressForm.tinh" :items="provinces" item-title="name" item-value="code"
                                    label="Tỉnh / Thành phố *" variant="outlined" density="compact" hide-details :loading="loadingLocations.provinces"
                                    @update:model-value="(val) => { newAddressForm.thanhPho = null; newAddressForm.phuongXa = null; if (val) fetchDistricts(val); }" class="text-body-2" />

                                <v-autocomplete v-model="newAddressForm.thanhPho" :items="districts" item-title="name" item-value="code"
                                    label="Quận / Huyện *" variant="outlined" density="compact" hide-details :loading="loadingLocations.districts" :disabled="!newAddressForm.tinh"
                                    @update:model-value="(val) => { newAddressForm.phuongXa = null; if (val) fetchWards(val); }" class="text-body-2" />

                                <v-autocomplete v-model="newAddressForm.phuongXa" :items="wards" item-title="name" item-value="code"
                                    label="Phường / Xã *" variant="outlined" density="compact" hide-details :loading="loadingLocations.wards" :disabled="!newAddressForm.thanhPho" class="text-body-2" />

                                <v-text-field v-model="newAddressForm.diaChiChiTiet" label="Địa chỉ chi tiết *" placeholder="Số nhà, tên đường..." variant="outlined" density="compact" hide-details class="text-body-2" />

                                <v-checkbox v-model="newAddressForm.laMacDinh" label="Đặt làm địa chỉ mặc định" color="primary" density="compact" hide-details class="mt-1" />

                                <div class="d-flex justify-end ga-2 mt-2">
                                    <v-btn variant="tonal" color="slate-500" size="small" class="rounded-lg text-none" @click="showAddForm = false">Hủy</v-btn>
                                    <v-btn :loading="submitting" color="primary" size="small" class="rounded-lg font-weight-bold text-none px-4" @click="handleSaveAddress">Lưu địa chỉ</v-btn>
                                </div>
                            </div>
                        </v-col>
                    </v-row>
                </div>
            </v-card-text>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.address-card {
    border-width: 1.5px !important;
}
.hover-card:hover {
    border-color: #94a3b8 !important;
    background-color: #f8fafc;
}
.active-card {
    border-color: #2563eb !important;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08) !important;
}
.active-check-badge {
    position: absolute;
    top: -1px;
    right: -1px;
    background-color: #2563eb;
    border-bottom-left-radius: 8px;
    border-top-right-radius: 10px;
    padding: 2px 6px;
}
.default-badge {
    font-size: 11px !important;
}
</style>
