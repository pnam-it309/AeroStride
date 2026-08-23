<template>
    <div class="d-flex flex-column">
        <!-- Khách hàng Card -->
        <v-card class="pos-navy-card pa-4 mb-3">
            <!-- Header Row: Title & Search Field Side-by-Side -->
            <div class="d-flex justify-space-between align-center border-b pb-2 mb-3 ga-2 flex-wrap">
                <div class="d-flex align-center ga-2 flex-shrink-0">
                    <span class="font-weight-bold text-black" style="font-size: 15px !important">Khách hàng</span>
                    <span
                        v-if="order?.idKhachHang"
                        class="customer-purchase-badge"
                    >
                        Số lần mua: {{ customerForm.tongDonHang || 0 }}
                    </span>
                </div>

                <!-- Customer Search Field & Clear Button -->
                <div class="position-relative d-flex align-center ga-2" style="width: 250px">
                    <v-text-field
                        v-model="customerSearch"
                        placeholder="Tìm theo SĐT, Tên..."
                        variant="outlined"
                        density="compact"
                        hide-details
                        maxlength="100"
                        prepend-inner-icon="mdi-magnify"
                        class="navy-input-field flex-grow-1"
                        @focus="onFocusCustomerSearch"
                        @click="onFocusCustomerSearch"
                        autocomplete="off"
                    />

                    <v-btn
                        v-if="order?.idKhachHang || customerForm.ten || customerForm.sdt"
                        icon
                        size="x-small"
                        variant="outlined"
                        class="navy-clear-btn flex-shrink-0"
                        @click="$emit('remove-customer')"
                    >
                        <v-icon size="16" color="#64748b">mdi-close</v-icon>
                        <v-tooltip activator="parent" location="top">Gỡ khách hàng</v-tooltip>
                    </v-btn>

                    <!-- CUSTOMER SEARCH SUGGESTIONS POPOVER -->
                    <div
                        v-if="showCustomerSuggestions"
                        class="suggestion-popover d-flex flex-column"
                        v-click-outside="() => (showCustomerSuggestions = false)"
                    >
                        <!-- Khi có kết quả tìm kiếm (vùng cuộn) -->
                        <div v-if="customerResults.length > 0" class="suggestion-list-scroll overflow-y-auto flex-grow-1">
                            <div
                                v-for="c in customerResults"
                                :key="c.id"
                                @click="onSelectSuggestedCustomer(c)"
                                class="suggestion-item d-flex align-center justify-space-between px-4 py-2.5 cursor-pointer"
                            >
                                <div class="d-flex flex-column text-truncate pr-2">
                                    <span class="customer-name-text text-truncate">{{ c.tenKhachHang || c.hoTen || c.ten || 'Khách hàng' }}</span>
                                    <span class="customer-email-text text-truncate mt-0.5">{{ c.email || 'Chưa đăng ký email' }}</span>
                                </div>
                                <div class="customer-phone-badge flex-shrink-0 ml-3">
                                    {{ c.sdt }}
                                </div>
                            </div>
                        </div>

                        <!-- Khi không có kết quả tìm kiếm -->
                        <div v-else-if="!isLoadingCustomerSearch" class="px-4 py-3 text-center">
                            <div class="text-slate-500 text-caption font-weight-medium">
                                {{ customerSearch.trim() ? 'Không tìm thấy khách hàng phù hợp' : 'Chưa có gợi ý khách hàng' }}
                            </div>
                        </div>

                        <!-- Khi đang tải -->
                        <div v-else class="pa-4 text-center text-caption text-slate-500 d-flex align-center justify-center">
                            <v-progress-circular indeterminate size="18" width="2" color="primary" class="mr-2" />
                            Đang tìm kiếm...
                        </div>

                        <!-- Nút thêm nhanh LUÔN GHIM Ở ĐÁY POPOVER -->
                        <div class="pa-2 border-t bg-slate-50 flex-shrink-0">
                            <v-btn
                                size="small"
                                variant="flat"
                                color="#2563eb"
                                class="text-white text-none font-weight-bold w-100 rounded-lg py-2 quick-add-btn"
                                style="height: 36px !important"
                                @mousedown.prevent.stop="quickCreateCustomer"
                                @click.stop="quickCreateCustomer"
                            >
                                <v-icon size="16" class="mr-1">mdi-plus</v-icon>
                                {{ customerSearch.trim() ? `Thêm nhanh: "${customerSearch.trim()}"` : 'Thêm khách hàng mới' }}
                            </v-btn>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Body Section: Input fields for Customer -->
            <div class="d-flex flex-column ga-3">
                <!-- Row 1: Khách hàng Input (Tên khách hàng) -->
                <v-text-field
                    v-model="customerForm.ten"
                    placeholder="Tên khách hàng"
                    variant="outlined"
                    density="compact"
                    hide-details
                    maxlength="100"
                    autocomplete="off"
                    class="navy-input-field w-100"
                    @input="emitFormChange"
                />

                <!-- Row 2: Số điện thoại and Địa chỉ email side-by-side -->
                <div class="d-flex ga-3">
                    <v-text-field
                        v-model="customerForm.sdt"
                        placeholder="Số điện thoại"
                        variant="outlined"
                        density="compact"
                        hide-details
                        maxlength="10"
                        autocomplete="off"
                        class="navy-input-field flex-grow-1"
                        @input="(e) => { customerForm.sdt = String(e.target.value || '').replace(/\D/g, '').slice(0, 10); emitFormChange(); }"
                    />
                    <v-text-field
                        v-model="customerForm.email"
                        placeholder="Địa chỉ email"
                        variant="outlined"
                        density="compact"
                        hide-details
                        maxlength="100"
                        autocomplete="off"
                        class="navy-input-field flex-grow-1"
                        @input="emitFormChange"
                    />
                </div>
            </div>
        </v-card>

        <!-- Nhận hàng Card -->
        <transition name="expand-shipping">
            <div v-if="isGiaoHang" class="shipping-wrapper">
                <v-card class="pos-navy-card pa-4">
                    <div class="d-flex justify-space-between align-center border-b pb-2 mb-3">
                        <div class="font-weight-bold text-black" style="font-size: 15px !important">Thông tin nhận hàng</div>
                        <v-btn
                            v-if="order?.idKhachHang"
                            variant="tonal"
                            color="primary"
                            size="x-small"
                            class="text-none font-weight-bold rounded-lg px-2"
                            @click="showAddressModal = true"
                        >
                            <v-icon size="15" class="mr-1">mdi-book-account-outline</v-icon>
                            Sổ địa chỉ
                        </v-btn>
                    </div>
                    <div class="d-flex flex-column ga-3">
                        <!-- Toggle / Link cho trường hợp người nhận khác người mua -->
                        <div class="d-flex align-center justify-space-between text-caption px-1 ga-2">
                            <div class="d-flex align-center text-slate-800 text-truncate mr-1">
                                <span class="mr-1 flex-shrink-0">Người nhận:</span>
                                <strong class="text-black font-weight-bold text-truncate">
                                    {{ isDifferentRecipient ? (recipientName || 'Người nhận khác') : (customerForm.ten || 'Giống người mua') }}
                                </strong>
                                <span v-if="!isDifferentRecipient && customerForm.sdt" class="text-slate-500 ml-1 flex-shrink-0">
                                    ({{ customerForm.sdt }})
                                </span>
                            </div>
                            <v-btn
                                variant="tonal"
                                color="primary"
                                size="x-small"
                                class="text-none font-weight-bold rounded-lg px-2.5 flex-shrink-0"
                                style="height: 26px !important; min-height: 26px"
                                @click="isDifferentRecipient = !isDifferentRecipient"
                            >
                                <v-icon size="13" class="mr-1">
                                    {{ isDifferentRecipient ? 'mdi-account-check-outline' : 'mdi-account-switch-outline' }}
                                </v-icon>
                                {{ isDifferentRecipient ? 'Giống người mua' : 'Thay đổi người nhận' }}
                            </v-btn>
                        </div>

                        <!-- Trường Tên người nhận & Số điện thoại người nhận (Chỉ hiện khi đổi người nhận khác) -->
                        <div v-if="isDifferentRecipient" class="d-flex ga-3">
                            <v-text-field
                                v-model="recipientName"
                                placeholder="Tên người nhận"
                                variant="outlined"
                                density="compact"
                                hide-details
                                maxlength="100"
                                autocomplete="off"
                                class="navy-input-field flex-grow-1"
                                @input="emitShippingChange"
                            />
                            <div style="width: 170px; flex: none">
                                <v-text-field
                                    v-model="recipientPhone"
                                    placeholder="SĐT người nhận"
                                    variant="outlined"
                                    density="compact"
                                    hide-details
                                    maxlength="10"
                                    autocomplete="off"
                                    class="navy-input-field"
                                    @input="(e) => { recipientPhone = String(e.target.value || '').replace(/\D/g, '').slice(0, 10); emitShippingChange(); }"
                                />
                            </div>
                        </div>

                        <v-text-field
                            v-model="recipientAddressDetail"
                            placeholder="Địa chỉ chi tiết (Số nhà, Tên đường...)"
                            variant="outlined"
                            density="compact"
                            hide-details
                            maxlength="255"
                            autocomplete="off"
                            class="navy-input-field w-100"
                            @input="emitShippingChange"
                        />

                        <!-- ĐỊA CHỈ HÀNH CHÍNH (Xếp 2 dòng để hiển thị 100% tên Tỉnh/Huyện/Xã không bị cắt ...) -->
                        <!-- Dòng 1: Tỉnh / Thành phố (Rộng 100% full width) -->
                        <v-autocomplete
                            v-model="recipientProvince"
                            :items="provincesShip"
                            item-title="name"
                            item-value="code"
                            placeholder="Chọn Tỉnh / Thành phố"
                            density="compact"
                            variant="outlined"
                            hide-details
                            class="navy-select-field w-100"
                            @update:modelValue="onProvinceChange"
                        />

                        <!-- Dòng 2: Quận/Huyện (50%) & Phường/Xã (50%) -->
                        <div class="d-flex ga-3">
                            <v-autocomplete
                                v-model="recipientDistrict"
                                :items="districtsShip"
                                item-title="name"
                                item-value="code"
                                placeholder="Chọn Quận / Huyện"
                                density="compact"
                                variant="outlined"
                                hide-details
                                :disabled="!recipientProvince"
                                class="navy-select-field flex-grow-1"
                                style="width: 50%"
                                @update:modelValue="onDistrictChange"
                            />
                            <v-autocomplete
                                v-model="recipientWard"
                                :items="wardsShip"
                                item-title="name"
                                item-value="code"
                                placeholder="Chọn Phường / Xã"
                                density="compact"
                                variant="outlined"
                                hide-details
                                :disabled="!recipientDistrict"
                                class="navy-select-field flex-grow-1"
                                style="width: 50%"
                                @update:modelValue="onWardChange"
                            />
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
            :current-shipping="{
                name: recipientName,
                phone: recipientPhone,
                detail: recipientAddressDetail,
                province: recipientProvince,
                district: recipientDistrict,
                ward: recipientWard
            }"
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

const emit = defineEmits(['set-customer', 'remove-customer', 'open-quick-add', 'update:customer-form', 'update:shipping']);

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

// Toggle người nhận khác
const isDifferentRecipient = ref(
    !!(props.initialShipping.name && props.initialShipping.name !== props.initialCustomerForm.ten) ||
    !!(props.initialShipping.phone && props.initialShipping.phone !== props.initialCustomerForm.sdt)
);

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
            const defaultAddr = customerAddresses.value.find((a) => a.laMacDinh) || customerAddresses.value[0];
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
        if (addr.tenNguoiNhan && addr.tenNguoiNhan !== customerForm.value.ten) {
            isDifferentRecipient.value = true;
        }
        recipientName.value = addr.tenNguoiNhan || customerForm.value.ten || '';
        recipientPhone.value = addr.sdtNguoiNhan || customerForm.value.sdt || '';
        recipientAddressDetail.value = addr.diaChiChiTiet || '';

        if (provincesShip.value.length === 0) {
            await fetchProvincesShip();
        }

        let matchedProvince =
            provincesShip.value.find((p) => String(p.code) === String(addr.tinh)) || matchLocation(provincesShip.value, addr.tinh);

        if (matchedProvince) {
            recipientProvince.value = matchedProvince.code;
            await fetchDistrictsShip(matchedProvince.code);

            let matchedDistrict =
                districtsShip.value.find((d) => String(d.code) === String(addr.thanhPho)) ||
                matchLocation(districtsShip.value, addr.thanhPho);

            if (matchedDistrict) {
                recipientDistrict.value = matchedDistrict.code;
                await fetchWardsShip(matchedDistrict.code);

                let matchedWard =
                    wardsShip.value.find((w) => String(w.code) === String(addr.phuongXa)) || matchLocation(wardsShip.value, addr.phuongXa);

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
    const defaultAddr = customerAddresses.value.find((a) => a.laMacDinh) || customerAddresses.value[0];
    if (defaultAddr) {
        await applyAddressFromModal(defaultAddr);
    }
};

watch(
    () => props.order?.idKhachHang,
    async (newKhId) => {
        if (newKhId) {
            await fetchCustomerAddresses();
            if (props.isGiaoHang) {
                await autoFillDefaultAddressIfNeeded();
            }
        } else {
            customerAddresses.value = [];
            selectedAddressId.value = '';
        }
    }
);

watch(
    () => props.isGiaoHang,
    async (newVal) => {
        if (newVal) {
            if (!recipientName.value || !recipientPhone.value || !recipientProvince.value) {
                await autoFillDefaultAddressIfNeeded();
            }
        }
    }
);

// Đồng bộ thông tin người nhận nếu không chọn người nhận khác
watch([() => customerForm.value.ten, () => customerForm.value.sdt], ([newTen, newSdt]) => {
    if (!isDifferentRecipient.value) {
        recipientName.value = newTen || '';
        recipientPhone.value = newSdt || '';
        emitShippingChange();
    }
});

watch(isDifferentRecipient, (val) => {
    if (!val) {
        recipientName.value = customerForm.value.ten || '';
        recipientPhone.value = customerForm.value.sdt || '';
        emitShippingChange();
    }
});

const isLoadingCustomerSearch = ref(false);

const performCustomerSearch = async (kw = '') => {
    isLoadingCustomerSearch.value = true;
    try {
        const res = await dichVuDonHang.searchKhachHang(kw ? kw.trim() : '');
        customerResults.value = res || [];
    } catch (error) {
        console.error('Lỗi tìm khách hàng:', error);
        customerResults.value = [];
    } finally {
        isLoadingCustomerSearch.value = false;
    }
};

const onFocusCustomerSearch = async () => {
    showCustomerSuggestions.value = true;
    if (customerResults.value.length === 0) {
        await performCustomerSearch(customerSearch.value);
    }
};

let searchTimeout = null;
watch(customerSearch, (newVal) => {
    if (searchTimeout) clearTimeout(searchTimeout);
    searchTimeout = setTimeout(async () => {
        await performCustomerSearch(newVal);
    }, 200);
});

const onSelectSuggestedCustomer = (c) => {
    customerSearch.value = '';
    showCustomerSuggestions.value = false;
    emit('set-customer', c);
};

const quickCreateCustomer = () => {
    const query = customerSearch.value.trim();
    const isPhone = /^[0-9+]+$/.test(query);
    const initialData = isPhone ? { ten: '', sdt: query, email: '' } : { ten: query, sdt: '', email: '' };

    customerSearch.value = '';
    showCustomerSuggestions.value = false;
    emit('open-quick-add', initialData);
};

const emitFormChange = () => {
    emit('update:customer-form', customerForm.value);
};

const emitShippingChange = () => {
    emit('update:shipping', {
        name: recipientName.value || customerForm.value.ten || '',
        phone: recipientPhone.value || customerForm.value.sdt || '',
        detail: recipientAddressDetail.value,
        province: recipientProvince.value,
        district: recipientDistrict.value,
        ward: recipientWard.value
    });
};

const onProvinceChange = async (val) => {
    if (isApplyingAddressModal.value) return;
    if (val !== undefined) recipientProvince.value = val;
    recipientDistrict.value = null;
    recipientWard.value = null;
    if (recipientProvince.value) {
        await fetchDistrictsShip(recipientProvince.value);
    }
    emitShippingChange();
};

const onDistrictChange = async (val) => {
    if (isApplyingAddressModal.value) return;
    if (val !== undefined) recipientDistrict.value = val;
    recipientWard.value = null;
    if (recipientDistrict.value) {
        await fetchWardsShip(recipientDistrict.value);
    }
    emitShippingChange();
};

const onWardChange = (val) => {
    if (isApplyingAddressModal.value) return;
    if (val !== undefined) recipientWard.value = val;
    emitShippingChange();
};

watch(
    () => props.initialCustomerForm,
    (newVal) => {
        customerForm.value = { ...newVal };
    },
    { deep: true }
);

watch(
    () => props.initialShipping,
    async (newVal) => {
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
    },
    { deep: true }
);
</script>

<style scoped>
/* Card Nền Trắng, Chữ Đen, Viền Tinh Tế Nhe Nhàn */
.pos-navy-card {
    background-color: #ffffff !important;
    border: 1px solid #e2e8f0 !important;
    border-radius: 16px !important;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04) !important;
}

/* Badge Số lần mua */
.customer-purchase-badge {
    font-size: 11px !important;
    font-weight: 700 !important;
    color: #000000 !important;
    background-color: #f1f5f9 !important;
    border: 1px solid #cbd5e1 !important;
    padding: 2px 10px !important;
    border-radius: 20px !important;
    white-space: nowrap !important;
    line-height: 1.3 !important;
}

/* Form Input Tinh Tế, Chữ Đen */
.navy-input-field :deep(.v-field) {
    border-radius: 8px !important;
    background-color: #ffffff !important;
}
.navy-input-field :deep(.v-field__outline) {
    --v-field-border-color: #cbd5e1 !important;
    --v-field-border-opacity: 1 !important;
}
.navy-input-field :deep(.v-field--focused .v-field__outline) {
    --v-field-border-color: #0f172a !important;
    border-width: 1px !important;
}
.navy-input-field :deep(.v-field__input) {
    color: #000000 !important;
    font-size: 13px !important;
    font-weight: 500 !important;
    min-height: 38px !important;
    padding-top: 4px !important;
    padding-bottom: 4px !important;
}

/* Dropdown Select Tinh Tế */
.navy-select-field :deep(.v-field) {
    border-radius: 8px !important;
    background-color: #ffffff !important;
}
.navy-select-field :deep(.v-field__outline) {
    --v-field-border-color: #cbd5e1 !important;
    --v-field-border-opacity: 1 !important;
}
.navy-select-field :deep(.v-field--focused .v-field__outline) {
    --v-field-border-color: #0f172a !important;
    border-width: 1px !important;
}
.navy-select-field :deep(.v-field__input) {
    color: #000000 !important;
    font-size: 13px !important;
    font-weight: 500 !important;
    min-height: 38px !important;
    padding-top: 4px !important;
    padding-bottom: 4px !important;
    white-space: nowrap !important;
    overflow: hidden !important;
    text-overflow: ellipsis !important;
}

/* Clear Button Gọn Gàng */
.navy-clear-btn {
    border: 1px solid #cbd5e1 !important;
    background-color: #f8fafc !important;
    width: 30px !important;
    height: 30px !important;
    border-radius: 8px !important;
}
.navy-clear-btn:hover {
    background-color: #e2e8f0 !important;
}

/* Popover Danh sách Tìm kiếm Khách hàng Mềm mại, Thoáng đãng */
.suggestion-popover {
    position: absolute;
    top: calc(100% + 6px);
    right: 0;
    width: 360px !important;
    max-height: 320px;
    background: #ffffff !important;
    border: 1px solid #e2e8f0 !important;
    border-radius: 14px !important;
    box-shadow: 0 16px 36px -4px rgba(15, 23, 42, 0.16), 0 4px 12px rgba(15, 23, 42, 0.06) !important;
    z-index: 9999 !important;
    overflow: hidden !important;
    padding: 0;
}

.suggestion-list-scroll {
    max-height: 220px;
    overflow-y: auto !important;
}

.suggestion-list-scroll::-webkit-scrollbar {
    width: 5px;
}
.suggestion-list-scroll::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 10px;
}
.suggestion-list-scroll::-webkit-scrollbar-track {
    background: transparent;
}

.suggestion-item {
    border-bottom: 1px solid #f1f5f9;
    transition: all 0.15s ease;
    padding: 10px 16px !important;
    min-height: 52px;
}

.suggestion-item:last-child {
    border-bottom: none;
}

.suggestion-item:hover {
    background-color: #f8fafc !important;
    border-left: 3px solid #2563eb !important;
    padding-left: 17px !important;
}

.customer-name-text {
    font-size: 14px !important;
    font-weight: 600 !important;
    color: #0f172a !important;
    line-height: 1.4 !important;
    display: block;
}

.customer-email-text {
    font-size: 12px !important;
    color: #64748b !important;
    font-weight: 400 !important;
    line-height: 1.3 !important;
    display: block;
}

.customer-phone-badge {
    font-size: 12px !important;
    font-weight: 600 !important;
    color: #1d4ed8 !important;
    background-color: #eff6ff !important;
    border: 1px solid #dbeafe !important;
    padding: 4px 10px !important;
    border-radius: 20px !important;
    white-space: nowrap;
}

/* Shipping Wrapper Animation */
.shipping-wrapper {
    display: grid;
    grid-template-rows: 1fr;
    transition:
        grid-template-rows 0.3s cubic-bezier(0.25, 0.8, 0.25, 1),
        opacity 0.25s ease-out,
        margin-top 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
    opacity: 1;
    margin-top: 12px;
    overflow: hidden;
}
.shipping-wrapper > .pos-navy-card {
    min-height: 0;
}
.expand-shipping-enter-active,
.expand-shipping-leave-active {
    transition:
        grid-template-rows 0.3s cubic-bezier(0.25, 0.8, 0.25, 1),
        opacity 0.25s ease-out,
        margin-top 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.expand-shipping-enter-from,
.expand-shipping-leave-to {
    grid-template-rows: 0fr !important;
    opacity: 0 !important;
    margin-top: 0 !important;
}
</style>
