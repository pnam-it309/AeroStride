<template>
  <div class="d-flex flex-column gap-4">
    <!-- Khách hàng Card -->
    <v-card class="pos-card pa-4 rounded-lg border">
        <div class="d-flex justify-space-between align-center border-b pb-2 mb-3">
            <div class="d-flex align-center gap-2">
                <span class="font-weight-bold text-slate-800" style="font-size: 14px !important">Khách hàng</span>
                <v-chip v-if="order?.idKhachHang" color="blue-lighten-4"
                    class="text-blue-darken-4 font-weight-bold" size="x-small" variant="flat"
                    style="font-size: 11px !important; height: 20px !important; border-radius: 6px !important;">
                    Số lần mua: {{ customerForm.tongDonHang || 0 }}
                </v-chip>
                <v-chip
                    v-else-if="customerForm.ten?.trim() || customerForm.sdt?.trim() || customerForm.email?.trim() || (customerForm.gioiTinh && customerForm.gioiTinh !== 'Giới tính')"
                    color="green-lighten-4" class="text-green-darken-4 font-weight-bold" size="x-small"
                    variant="flat"
                    style="font-size: 11px !important; height: 20px !important; border-radius: 6px !important;">
                    Thêm mới
                </v-chip>
            </div>
            <div class="d-flex align-center gap-2">
                <v-select v-model="customerForm.gioiTinh" :items="['Giới tính', 'Nam', 'Nữ', 'Khác']"
                    density="compact" variant="outlined" hide-details class="dim-select"
                    style="width: 200px" @update:modelValue="emitFormChange" />
                <v-btn v-if="order?.idKhachHang" icon size="x-small" variant="flat"
                    color="error" class="bg-red-50"
                    style="width: 28px !important; height: 28px !important; border-radius: 6px !important;"
                    @click="$emit('remove-customer')">
                    <v-icon size="14">mdi-close-circle-outline</v-icon>
                </v-btn>
            </div>
        </div>

        <div class="d-flex flex-column gap-4">
            <!-- Input tìm kiếm khách hàng -->
            <div class="position-relative">
                <v-text-field v-model="customerSearch"
                    placeholder="Tìm kiếm khách hàng (SĐT, Tên, Email)" variant="outlined"
                    density="compact" hide-details prepend-inner-icon="mdi-magnify"
                    class="dim-input-field bg-slate-50" @focus="showCustomerSuggestions = true"
                    autocomplete="off" />

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
                    <div v-else class="pa-3 text-center text-slate-500">
                        <span class="text-caption">Không tìm thấy khách hàng</span>
                    </div>
                </div>
            </div>

            <div class="d-flex gap-3">
                <v-text-field v-model="customerForm.ten" placeholder="Tên khách hàng" variant="outlined"
                    density="compact" hide-details autocomplete="off"
                    class="dim-input-field flex-grow-1" @input="emitFormChange" />
                <div style="width: 170px; flex: none;">
                    <v-text-field v-model="customerForm.sdt" placeholder="Số điện thoại"
                        variant="outlined" density="compact" hide-details autocomplete="off"
                        class="dim-input-field" @input="emitFormChange" />
                </div>
            </div>

            <v-text-field v-model="customerForm.email" placeholder="Địa chỉ email" variant="outlined"
                density="compact" hide-details autocomplete="off" class="dim-input-field w-100" @input="emitFormChange" />
        </div>
    </v-card>

    <!-- Nhận hàng Card -->
    <transition name="expand-fade">
        <v-card v-if="isGiaoHang" class="pos-card pa-4 rounded-lg border">
            <div class="font-weight-bold text-slate-800 mb-3" style="font-size: 14px !important">Thông tin nhận hàng</div>
            <div class="d-flex flex-column gap-4">
                <div class="d-flex gap-3">
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
    </transition>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { useLocation } from '@/composables/useLocation';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';

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

// Shipping
const recipientName = ref(props.initialShipping.name);
const recipientPhone = ref(props.initialShipping.phone);
const recipientAddressDetail = ref(props.initialShipping.detail);
const recipientProvince = ref(props.initialShipping.province);
const recipientDistrict = ref(props.initialShipping.district);
const recipientWard = ref(props.initialShipping.ward);

const {
    provinces: provincesShip,
    districts: districtsShip,
    wards: wardsShip,
    fetchProvinces: fetchProvincesShip,
    fetchDistricts: fetchDistrictsShip,
    fetchWards: fetchWardsShip
} = useLocation({ allowFallback: true });

onMounted(async () => {
    await fetchProvincesShip();
    if (recipientProvince.value) {
        await fetchDistrictsShip(recipientProvince.value);
    }
    if (recipientDistrict.value) {
        await fetchWardsShip(recipientDistrict.value);
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
    recipientDistrict.value = null;
    recipientWard.value = null;
    if (recipientProvince.value) {
        await fetchDistrictsShip(recipientProvince.value);
    }
    emitShippingChange();
};

const onDistrictChange = async () => {
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
.expand-fade-enter-active, .expand-fade-leave-active {
    transition: all 0.3s ease;
}
.expand-fade-enter-from, .expand-fade-leave-to {
    opacity: 0;
    transform: translateY(-10px);
}
</style>
