<template>
  <div class="d-flex flex-column">
    <!-- Khách hàng Card -->
    <v-card class="pos-card pa-4">
        <!-- Header Row: Title & Search Field Side-by-Side -->
        <div class="d-flex justify-space-between align-center border-b pb-2 mb-3 gap-3">
            <div class="d-flex align-center gap-2 flex-shrink-0">
                <span class="font-weight-semibold" style="font-size: 14px !important; color: #2b2a2a !important;">Khách hàng</span>
                <v-chip v-if="order?.idKhachHang" color="blue-lighten-4"
                    class="text-blue-darken-4 font-weight-bold" size="x-small" variant="flat"
                    style="font-size: 11px !important; height: 20px !important; border-radius: 6px !important;">
                    Số lần mua: {{ customerForm.tongDonHang || 0 }}
                </v-chip>
            </div>

            <!-- Customer Search Field & Clear Button -->
            <div class="position-relative flex-grow-1 d-flex align-center gap-2" style="max-width: 260px;">
                <v-text-field v-model="customerSearch"
                    placeholder="Tìm khách hàng (SĐT, Tên...)" variant="outlined"
                    density="compact" hide-details prepend-inner-icon="mdi-magnify"
                    class="dim-input-field bg-slate-50 flex-grow-1" @focus="showCustomerSuggestions = true"
                    autocomplete="off" />

                <v-btn v-if="order?.idKhachHang || customerForm.ten || customerForm.sdt" icon size="x-small" variant="flat"
                    color="error" class="bg-red-50 flex-shrink-0"
                    style="width: 28px !important; height: 28px !important; border-radius: 6px !important;"
                    @click="$emit('remove-customer')">
                    <v-icon size="14">mdi-close-circle-outline</v-icon>
                </v-btn>

                <div v-if="showCustomerSuggestions && customerSearch.length > 0"
                    class="suggestion-popover overflow-y-auto w-100"
                    style="max-height: 250px; z-index: 100; top: calc(100% + 4px);"
                    v-click-outside="() => showCustomerSuggestions = false">
                    <div v-if="customerResults.length > 0" class="pa-1 d-flex flex-column gap-1">
                        <div v-for="c in customerResults" :key="c.id"
                            @click="onSelectSuggestedCustomer(c)"
                            class="suggestion-item d-flex flex-column px-3 py-2 cursor-pointer transition-all border-b"
                            style="font-size: 13px;">
                            <div class="d-flex w-100 justify-space-between mb-1">
                                <span class="font-weight-bold text-slate-800">{{ c.hoTen || c.ten }}</span>
                                <span class="text-blue-darken-3 font-weight-medium">{{ c.sdt }}</span>
                            </div>
                            <span class="text-slate-500 text-caption">{{ c.email || 'Không có email' }}</span>
                        </div>
                    </div>
                    <div v-else class="pa-1 d-flex flex-column gap-1">
                        <div class="pa-2 text-center text-slate-400 text-caption border-b">
                            Không tìm thấy khách hàng
                        </div>
                        <div @click="quickCreateCustomer"
                            class="suggestion-item d-flex align-center gap-2 px-3 py-2.5 cursor-pointer transition-all text-primary font-weight-medium"
                            style="font-size: 13px;">
                            <v-icon size="16">mdi-plus-circle-outline</v-icon>
                            <span>Thêm nhanh: "{{ customerSearch }}"</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Body Section: Input fields always visible -->
        <div class="d-flex flex-column gap-3">
            <!-- Row 1: Khách hàng Input (Tên khách hàng) -->
            <v-text-field v-model="customerForm.ten" placeholder="Khách hàng" variant="outlined"
                density="compact" hide-details autocomplete="off"
                class="dim-input-field w-100" @input="emitFormChange" />

            <!-- Row 2: Số điện thoại and Địa chỉ email side-by-side -->
            <div class="d-flex gap-3">
                <v-text-field v-model="customerForm.sdt" placeholder="Số điện thoại" variant="outlined"
                    density="compact" hide-details autocomplete="off"
                    class="dim-input-field flex-grow-1" @input="emitFormChange" />
                <v-text-field v-model="customerForm.email" placeholder="Địa chỉ email" variant="outlined"
                    density="compact" hide-details autocomplete="off"
                    class="dim-input-field flex-grow-1" @input="emitFormChange" />
            </div>
        </div>
    </v-card>

    <!-- Nhận hàng Card -->
    <transition name="expand-shipping">
        <div v-if="isGiaoHang" class="shipping-wrapper">
            <v-card class="pos-card pa-4">
                <div class="d-flex justify-space-between align-center mb-3">
                    <div class="font-weight-bold text-slate-800" style="font-size: 14px !important">Thông tin nhận hàng</div>
                    <v-btn v-if="order?.idKhachHang" variant="tonal" color="primary" size="x-small" class="text-none font-weight-bold rounded-lg px-2" @click="showAddressModal = true">
                        <v-icon size="14" class="mr-1">mdi-book-location-outline</v-icon>
                        Chọn địa chỉ
                    </v-btn>
                </div>
                <div class="d-flex flex-column ga-4">
                    <div class="d-flex ga-3">
                        <v-text-field v-model="recipientName" placeholder="Tên người nhận"
                            variant="outlined" density="compact" hide-details autocomplete="off"
                            class="dim-input-field flex-grow-1" @input="emitShippingChange" />
                        <div style="width: 170px; flex: none;">
                            <v-text-field v-model="recipientPhone" placeholder="Số điện thoại"
                                variant="outlined" density="compact" hide-details autocomplete="off"
                                class="dim-input-field" @input="emitShippingChange" />
                        </div>
                    </div>

                    <v-text-field v-model="recipientAddressDetail" placeholder="Địa chỉ chi tiết"
                        variant="outlined" density="compact" hide-details autocomplete="off"
                        class="dim-input-field w-100" @input="emitShippingChange" />

                    <div class="d-flex gap-3">
                        <v-select v-model="recipientProvince" :items="provincesShip" item-title="name"
                            item-value="code" placeholder="Tỉnh/Thành phố" density="compact"
                            variant="outlined" hide-details class="dim-select-field flex-grow-1"
                            style="width: 33.33%;" @update:modelValue="onProvinceChange" />
                        <v-select v-model="recipientDistrict" :items="districtsShip" item-title="name"
                            item-value="code" placeholder="Quận/Huyện" density="compact" variant="outlined"
                            hide-details :disabled="!recipientProvince" class="dim-select-field flex-grow-1"
                            style="width: 33.33%;" @update:modelValue="onDistrictChange" />
                        <v-select v-model="recipientWard" :items="wardsShip" item-title="name"
                            item-value="code" placeholder="Phường/Xã" density="compact" variant="outlined"
                            hide-details :disabled="!recipientDistrict" class="dim-select-field flex-grow-1"
                            style="width: 33.33%;" @update:modelValue="emitShippingChange" />
                    </div>
                </div>
            </v-card>
        </div>
    </transition>

    <!-- Modal chọn địa chỉ -->
    <CustomerAddressModal
        v-model="showAddressModal"
        :customer="currentCustomer"
        :selected-address-id="selectedAddressId"
        :current-shipping="{ name: recipientName, phone: recipientPhone, detail: recipientAddressDetail, province: recipientProvince, district: recipientDistrict, ward: recipientWard }"
        @select-address="applyAddressFromModal"
        @address-changed="fetchCustomerAddresses"
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { useLocation } from '@/composables/useLocation';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import CustomerAddressModal from './CustomerAddressModal.vue';

const props = defineProps({
    order: {
        type: Object,
        default: null
    },
    isGiaoHang: {
        type: Boolean,
        default: false
    },
    initialCustomerForm: {
        type: Object,
        default: () => ({ ten: '', sdt: '', email: '', gioiTinh: 'Giới tính', tongDonHang: 0 })
    },
    initialShipping: {
        type: Object,
        default: () => ({ name: '', phone: '', detail: '', province: null, district: null, ward: null })
    }
});

const emit = defineEmits(['set-customer', 'remove-customer', 'update:customer-form', 'update:shipping']);

const customerForm = ref({ ...props.initialCustomerForm });
const customerSearch = ref('');
const showCustomerSuggestions = ref(false);
const customerResults = ref([]);

// Shipping & Address Management
const recipientName = ref(props.initialShipping.name);
const recipientPhone = ref(props.initialShipping.phone);
const recipientAddressDetail = ref(props.initialShipping.detail);
const recipientProvince = ref(props.initialShipping.province);
const recipientDistrict = ref(props.initialShipping.district);
const recipientWard = ref(props.initialShipping.ward);

const showAddressModal = ref(false);
const customerAddresses = ref([]);
const selectedAddressId = ref('');

const currentCustomer = computed(() => {
    return {
        id: props.order?.idKhachHang,
        hoTen: customerForm.value.ten,
        ten: customerForm.value.ten,
        sdt: customerForm.value.sdt,
        email: customerForm.value.email
    };
});

const {
    provinces: provincesShip,
    districts: districtsShip,
    wards: wardsShip,
    fetchProvinces: fetchProvincesShip,
    fetchDistricts: fetchDistrictsShip,
    fetchWards: fetchWardsShip,
    matchLocation
} = useLocation({ allowFallback: true });

onMounted(async () => {
    await fetchProvincesShip();
    if (recipientProvince.value) {
        await fetchDistrictsShip(recipientProvince.value);
    }
    if (recipientDistrict.value) {
        await fetchWardsShip(recipientDistrict.value);
    }
    if (props.order?.idKhachHang) {
        await fetchCustomerAddresses();
    }
});

const fetchCustomerAddresses = async () => {
    const khId = props.order?.idKhachHang;
    if (!khId) {
        customerAddresses.value = [];
        return;
    }
    try {
        const res = await dichVuKhachHang.layDanhSachDiaChi(khId);
        const list = res?.data || res || [];
        customerAddresses.value = Array.isArray(list) ? list : [];

        if (!selectedAddressId.value && customerAddresses.value.length > 0) {
            const defaultAddr = customerAddresses.value.find(a => a.laMacDinh) || customerAddresses.value[0];
            if (defaultAddr) {
                selectedAddressId.value = defaultAddr.id || '';
            }
        }
    } catch (e) {
        console.error('Lỗi lấy danh sách địa chỉ khách hàng:', e);
        customerAddresses.value = [];
    }
};

const isApplyingAddressModal = ref(false);

const applyAddressFromModal = async (addr) => {
    if (!addr) return;
    isApplyingAddressModal.value = true;
    try {
        selectedAddressId.value = addr.id || '';
        recipientName.value = addr.tenNguoiNhan || customerForm.value.ten || '';
        recipientPhone.value = addr.sdtNguoiNhan || customerForm.value.sdt || '';
        recipientAddressDetail.value = addr.diaChiChiTiet || '';

        if (provincesShip.value.length === 0) {
            await fetchProvincesShip();
        }

        let matchedProvince = provincesShip.value.find(p => String(p.code) === String(addr.tinh)) ||
                              matchLocation(provincesShip.value, addr.tinh);

        if (matchedProvince) {
            recipientProvince.value = matchedProvince.code;
            await fetchDistrictsShip(matchedProvince.code);

            let matchedDistrict = districtsShip.value.find(d => String(d.code) === String(addr.thanhPho)) ||
                                  matchLocation(districtsShip.value, addr.thanhPho);

            if (matchedDistrict) {
                recipientDistrict.value = matchedDistrict.code;
                await fetchWardsShip(matchedDistrict.code);

                let matchedWard = wardsShip.value.find(w => String(w.code) === String(addr.phuongXa)) ||
                                  matchLocation(wardsShip.value, addr.phuongXa);

                recipientWard.value = matchedWard ? matchedWard.code : null;
            } else {
                recipientDistrict.value = null;
                recipientWard.value = null;
            }
        } else {
            recipientProvince.value = null;
            recipientDistrict.value = null;
            recipientWard.value = null;
        }

        emitShippingChange();
    } catch (err) {
        console.error('Lỗi binding địa chỉ từ modal:', err);
    } finally {
        isApplyingAddressModal.value = false;
    }
};

const autoFillDefaultAddressIfNeeded = async () => {
    if (!props.isGiaoHang || !props.order?.idKhachHang) return;
    await fetchCustomerAddresses();
    if (customerAddresses.value.length === 0) return;

    // Tìm địa chỉ mặc định hoặc địa chỉ đầu tiên
    const defaultAddr = customerAddresses.value.find(a => a.laMacDinh) || customerAddresses.value[0];
    if (defaultAddr) {
        await applyAddressFromModal(defaultAddr);
    }
};

watch(() => props.order?.idKhachHang, async (newKhId) => {
    if (newKhId) {
        await fetchCustomerAddresses();
        if (props.isGiaoHang) {
            await autoFillDefaultAddressIfNeeded();
        }
    } else {
        customerAddresses.value = [];
        selectedAddressId.value = '';
    }
});

watch(() => props.isGiaoHang, async (newVal) => {
    if (newVal) {
        if (!recipientName.value || !recipientPhone.value || !recipientProvince.value) {
            await autoFillDefaultAddressIfNeeded();
        }
    }
});

let searchTimeout = null;
watch(customerSearch, (newVal) => {
    if (searchTimeout) clearTimeout(searchTimeout);
    if (!newVal || newVal.length < 2) {
        customerResults.value = [];
        return;
    }
    searchTimeout = setTimeout(async () => {
        try {
            const res = await dichVuDonHang.searchKhachHang(newVal);
            customerResults.value = res || [];
        } catch (error) {
            console.error('Lỗi tìm khách hàng:', error);
        }
    }, 300);
});

const onSelectSuggestedCustomer = (c) => {
    customerSearch.value = '';
    showCustomerSuggestions.value = false;
    emit('set-customer', c);
};

const quickCreateCustomer = () => {
    const query = customerSearch.value.trim();
    emit('remove-customer');
    
    const isPhone = /^[0-9+]+$/.test(query);
    if (isPhone) {
        customerForm.value = {
            ten: '',
            sdt: query,
            email: '',
            gioiTinh: 'Giới tính',
            tongDonHang: 0
        };
    } else {
        customerForm.value = {
            ten: query,
            sdt: '',
            email: '',
            gioiTinh: 'Giới tính',
            tongDonHang: 0
        };
    }
    
    customerSearch.value = '';
    showCustomerSuggestions.value = false;
    emitFormChange();
};

const emitFormChange = () => {
    emit('update:customer-form', customerForm.value);
};

const emitShippingChange = () => {
    emit('update:shipping', {
        name: recipientName.value,
        phone: recipientPhone.value,
        detail: recipientAddressDetail.value,
        province: recipientProvince.value,
        district: recipientDistrict.value,
        ward: recipientWard.value
    });
};

const onProvinceChange = async () => {
    if (isApplyingAddressModal.value) return;
    recipientDistrict.value = null;
    recipientWard.value = null;
    if (recipientProvince.value) {
        await fetchDistrictsShip(recipientProvince.value);
    }
    emitShippingChange();
};

const onDistrictChange = async () => {
    if (isApplyingAddressModal.value) return;
    recipientWard.value = null;
    if (recipientDistrict.value) {
        await fetchWardsShip(recipientDistrict.value);
    }
    emitShippingChange();
};

watch(() => props.initialCustomerForm, (newVal) => {
    customerForm.value = { ...newVal };
}, { deep: true });

watch(() => props.initialShipping, async (newVal) => {
    recipientName.value = newVal.name;
    recipientPhone.value = newVal.phone;
    recipientAddressDetail.value = newVal.detail;
    
    if (recipientProvince.value !== newVal.province) {
        recipientProvince.value = newVal.province;
        if (newVal.province) await fetchDistrictsShip(newVal.province);
    }
    
    if (recipientDistrict.value !== newVal.district) {
        recipientDistrict.value = newVal.district;
        if (newVal.district) await fetchWardsShip(newVal.district);
    }
    
    recipientWard.value = newVal.ward;
}, { deep: true });
</script>

<style scoped>
.pos-card {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important;
}
.dim-input-field :deep(.v-field__input) {
    font-size: 13px !important;
    min-height: 36px !important;
    padding-top: 4px !important;
    padding-bottom: 4px !important;
}
.dim-select-field :deep(.v-field__input) {
    font-size: 13px !important;
    min-height: 36px !important;
    padding-top: 4px !important;
    padding-bottom: 4px !important;
}
.dim-select :deep(.v-field__input) {
    font-size: 13px !important;
    min-height: 28px !important;
    padding-top: 0 !important;
    padding-bottom: 0 !important;
}
.suggestion-popover {
    position: absolute;
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
}
.suggestion-item:hover {
    background-color: #f8fafc;
}
.shipping-wrapper {
    display: grid;
    grid-template-rows: 1fr;
    transition: grid-template-rows 0.3s cubic-bezier(0.25, 0.8, 0.25, 1),
                opacity 0.25s ease-out,
                margin-top 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
    opacity: 1;
    margin-top: 12px;
    overflow: hidden;
}
.shipping-wrapper > .pos-card {
    min-height: 0;
}
.expand-shipping-enter-active, .expand-shipping-leave-active {
    transition: grid-template-rows 0.3s cubic-bezier(0.25, 0.8, 0.25, 1),
                opacity 0.25s ease-out,
                margin-top 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.expand-shipping-enter-from, .expand-shipping-leave-to {
    grid-template-rows: 0fr !important;
    opacity: 0 !important;
    margin-top: 0 !important;
}
</style>
