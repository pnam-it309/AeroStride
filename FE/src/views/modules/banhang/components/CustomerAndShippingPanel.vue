<template>
    <div class="d-flex flex-column">
        <!-- Khách hàng Card -->
        <v-card class="pos-navy-card customer-card-wrapper pa-4 mb-3">
            <!-- Header Row: Title & Action Buttons -->
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

                <div class="d-flex align-center ga-2">
                    <v-btn
                        size="x-small"
                        variant="tonal"
                        color="primary"
                        class="text-none font-weight-bold rounded-lg px-2"
                        style="height: 28px !important"
                        @click="quickCreateCustomer"
                    >
                        <v-icon size="14" class="mr-1">mdi-account-plus</v-icon>
                        Thêm mới
                    </v-btn>

                    <v-btn
                        v-if="order?.idKhachHang || customerForm.ten || customerForm.sdt"
                        icon
                        size="x-small"
                        variant="outlined"
                        class="navy-clear-btn flex-shrink-0 ml-1"
                        @click="$emit('remove-customer')"
                    >
                        <v-icon size="15" color="#64748b">mdi-close</v-icon>
                        <v-tooltip activator="parent" location="top">Gỡ khách hàng</v-tooltip>
                    </v-btn>
                </div>
            </div>

            <!-- Search Row: Full width input with absolute dropdown spanning 100% width -->
            <div class="position-relative w-100 mb-3 search-input-container">
                <v-text-field
                    v-model="customerSearch"
                    placeholder="Tìm theo SĐT, Tên khách hàng..."
                    variant="outlined"
                    density="compact"
                    hide-details
                    maxlength="100"
                    prepend-inner-icon="mdi-magnify"
                    class="navy-input-field w-100"
                    @focus="onFocusCustomerSearch"
                    autocomplete="off"
                />

                <!-- CUSTOMER SEARCH SUGGESTIONS POPOVER (Full width, không bao giờ lệch mép) -->
                <div
                    v-if="showCustomerSuggestions"
                    class="suggestion-popover d-flex flex-column"
                    v-click-outside="() => (showCustomerSuggestions = false)"
                >
                    <!-- Header nhỏ trong popover -->
                    <div class="px-3 py-2 bg-slate-50 border-b d-flex justify-space-between align-center flex-shrink-0">
                        <span class="text-caption font-weight-bold text-slate-700">
                            {{ customerResults.length > 0 ? `Kết quả tìm kiếm (${customerResults.length})` : 'Gợi ý khách hàng' }}
                        </span>
                        <v-btn
                            icon
                            size="x-small"
                            variant="text"
                            class="rounded-circle"
                            style="width: 20px; height: 20px"
                            @click="showCustomerSuggestions = false"
                        >
                            <v-icon size="14" color="#64748b">mdi-close</v-icon>
                        </v-btn>
                    </div>

                    <!-- Khi có kết quả tìm kiếm (vùng cuộn) -->
                    <div v-if="customerResults.length > 0" class="suggestion-list-scroll overflow-y-auto flex-grow-1">
                        <div
                            v-for="c in customerResults"
                            :key="c.id"
                            @click="onSelectSuggestedCustomer(c)"
                            class="suggestion-item d-flex align-center justify-space-between px-3 py-2 cursor-pointer"
                        >
                            <div class="d-flex align-center text-truncate mr-2">
                                <v-avatar size="28" color="blue-lighten-5" class="mr-2 flex-shrink-0">
                                    <span class="text-caption font-weight-bold text-primary">
                                        {{ (c.tenKhachHang || c.hoTen || c.ten || 'K').charAt(0).toUpperCase() }}
                                    </span>
                                </v-avatar>
                                <div class="d-flex flex-column text-truncate">
                                    <span class="customer-name-text text-truncate">{{ c.tenKhachHang || c.hoTen || c.ten || 'Khách hàng' }}</span>
                                    <span class="customer-email-text text-truncate">{{ c.email || 'Chưa đăng ký email' }}</span>
                                </div>
                            </div>
                            <div class="customer-phone-badge flex-shrink-0">
                                {{ c.sdt }}
                            </div>
                        </div>
                    </div>

                    <!-- Khi không có kết quả tìm kiếm -->
                    <div v-else-if="!isLoadingCustomerSearch" class="px-4 py-4 text-center">
                        <v-icon size="28" color="#94a3b8" class="mb-1">mdi-account-search-outline</v-icon>
                        <div class="text-slate-600 text-caption font-weight-medium">
                            {{ customerSearch.trim() ? `Không tìm thấy "${customerSearch.trim()}"` : 'Chưa có gợi ý khách hàng' }}
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
                            <v-icon size="16" class="mr-1.5">mdi-account-plus</v-icon>
                            {{ customerSearch.trim() ? `Thêm nhanh: "${customerSearch.trim()}"` : 'Thêm khách hàng mới' }}
                        </v-btn>
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
                        <div class="d-flex align-center ga-2 text-truncate">
                            <div class="font-weight-bold text-black flex-shrink-0" style="font-size: 15px !important">Thông tin nhận hàng</div>
                            <span class="text-caption text-slate-500 text-truncate font-weight-medium">
                                ({{ isDifferentRecipient ? (recipientName || 'Người nhận khác') : (customerForm.ten || 'Giống người mua') }})
                            </span>
                        </div>
                        <div class="d-flex align-center ga-1 flex-shrink-0">
                            <v-btn
                                variant="tonal"
                                color="primary"
                                size="x-small"
                                class="text-none font-weight-bold rounded-lg px-2"
                                style="height: 26px !important; min-height: 26px"
                                @click="isDifferentRecipient = !isDifferentRecipient"
                            >
                                <v-icon size="13" class="mr-1">
                                    {{ isDifferentRecipient ? 'mdi-account-check-outline' : 'mdi-account-switch-outline' }}
                                </v-icon>
                                {{ isDifferentRecipient ? 'Giống người mua' : 'Đổi người nhận' }}
                            </v-btn>
                            <v-btn
                                v-if="order?.idKhachHang"
                                variant="tonal"
                                color="primary"
                                size="x-small"
                                class="text-none font-weight-bold rounded-lg px-2"
                                style="height: 26px !important; min-height: 26px"
                                @click="showAddressModal = true"
                            >
                                <v-icon size="14" class="mr-1">mdi-book-account-outline</v-icon>
                                Sổ địa chỉ
                            </v-btn>
                        </div>
                    </div>
                    <div class="d-flex flex-column ga-3">
                        <!-- Trường Tên người nhận (Chỉ hiện khi đổi người nhận khác) -->
                        <div v-if="isDifferentRecipient">
                            <v-text-field
                                v-model="recipientName"
                                placeholder="Tên người nhận hàng"
                                variant="outlined"
                                density="compact"
                                hide-details
                                maxlength="100"
                                autocomplete="off"
                                class="navy-input-field w-100"
                                @input="emitShippingChange"
                            />
                        </div>

                        <!-- Row 1: Địa chỉ chi tiết -->
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

                        <!-- Row 2: Tỉnh / Thành phố (50%) & Quận / Huyện (50%) -->
                        <div class="d-flex ga-3">
                            <v-autocomplete
                                v-model="recipientProvince"
                                :items="provincesShip"
                                item-title="name"
                                item-value="code"
                                placeholder="Chọn Tỉnh / Thành phố"
                                density="compact"
                                variant="outlined"
                                hide-details
                                class="navy-select-field flex-grow-1"
                                style="width: 50%"
                                @update:modelValue="onProvinceChange"
                            />
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
                        </div>

                        <!-- Row 3: Phường / Xã (50%) & SĐT người nhận (50%) -->
                        <div class="d-flex ga-3">
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
                            <v-text-field
                                v-model="recipientPhone"
                                placeholder="SĐT người nhận"
                                variant="outlined"
                                density="compact"
                                hide-details
                                maxlength="10"
                                autocomplete="off"
                                class="navy-input-field flex-grow-1"
                                style="width: 50%"
                                @input="(e) => { recipientPhone = String(e.target.value || '').replace(/\D/g, '').slice(0, 10); emitShippingChange(); }"
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

onMounted(() => {
    fetchProvincesShip();
    if (recipientProvince.value) {
        fetchDistrictsShip(recipientProvince.value);
    }
    if (recipientDistrict.value) {
        fetchWardsShip(recipientDistrict.value);
    }
    if (props.order?.idKhachHang) {
        fetchCustomerAddresses();
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
    if (!customerAddresses.value || customerAddresses.value.length === 0) {
        await fetchCustomerAddresses();
    }
    if (!customerAddresses.value || customerAddresses.value.length === 0) return;

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
                const defaultAddr = customerAddresses.value.find((a) => a.laMacDinh) || customerAddresses.value[0];
                if (defaultAddr) {
                    await applyAddressFromModal(defaultAddr);
                }
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
let lastCustomerSearchKw = null;
let inFlightCustomerSearchPromise = null;

const performCustomerSearch = async (kw = '', force = false) => {
    const cleanKw = kw ? kw.trim() : '';
    if (!force && lastCustomerSearchKw === cleanKw && inFlightCustomerSearchPromise) {
        return inFlightCustomerSearchPromise;
    }
    if (!force && lastCustomerSearchKw === cleanKw && customerResults.value.length > 0) {
        return;
    }
    lastCustomerSearchKw = cleanKw;
    isLoadingCustomerSearch.value = true;

    inFlightCustomerSearchPromise = (async () => {
        try {
            const res = await dichVuDonHang.searchKhachHang(cleanKw);
            customerResults.value = res || [];
        } catch (error) {
            console.error('Lỗi tìm khách hàng:', error);
            customerResults.value = [];
        } finally {
            isLoadingCustomerSearch.value = false;
            inFlightCustomerSearchPromise = null;
        }
    })();

    return inFlightCustomerSearchPromise;
};

const onFocusCustomerSearch = () => {
    showCustomerSuggestions.value = true;
    if (!customerResults.value || customerResults.value.length === 0) {
        performCustomerSearch(customerSearch.value, false);
    }
};

let searchTimeout = null;
watch(customerSearch, (newVal) => {
    if (searchTimeout) clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
        performCustomerSearch(newVal, true);
    }, 250);
});

const onSelectSuggestedCustomer = (c) => {
    customerSearch.value = '';
    showCustomerSuggestions.value = false;
    customerForm.value = {
        ten: c.ten || c.hoTen || c.tenKhachHang || '',
        sdt: c.sdt || c.soDienThoai || '',
        email: c.email || '',
        gioiTinh:
            c.gioiTinh === true || c.gioiTinh === 1 || c.gioiTinh === 'Nam'
                ? 'Nam'
                : c.gioiTinh === false || c.gioiTinh === 0 || c.gioiTinh === 'Nữ'
                  ? 'Nữ'
                  : 'Khác',
        ngaySinh: c.ngaySinh || '',
        tongDonHang: c.tongDonHang || 0
    };
    emit('update:customer-form', customerForm.value);
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

const currentOrderId = ref(props.order?.id || null);

let formChangeTimeout = null;
const emitFormChange = () => {
    if (formChangeTimeout) clearTimeout(formChangeTimeout);
    formChangeTimeout = setTimeout(() => {
        emit('update:customer-form', customerForm.value);
    }, 100);
};

let shippingChangeTimeout = null;
const emitShippingChange = () => {
    if (shippingChangeTimeout) clearTimeout(shippingChangeTimeout);
    shippingChangeTimeout = setTimeout(() => {
        emit('update:shipping', {
            name: recipientName.value || customerForm.value.ten || '',
            phone: recipientPhone.value || customerForm.value.sdt || '',
            detail: recipientAddressDetail.value,
            province: recipientProvince.value,
            district: recipientDistrict.value,
            ward: recipientWard.value
        });
    }, 100);
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
    (newForm) => {
        if (newForm) {
            customerForm.value = {
                ten: newForm.ten || '',
                sdt: newForm.sdt || '',
                email: newForm.email || '',
                gioiTinh: newForm.gioiTinh || 'Giới tính',
                ngaySinh: newForm.ngaySinh || '',
                tongDonHang: newForm.tongDonHang || 0
            };
        }
    },
    { deep: true }
);

watch(
    () => [props.order?.id, props.order?.idKhachHang],
    async ([newOrderId, newKhId], [oldOrderId, oldKhId]) => {
        if (newOrderId && newOrderId !== oldOrderId) {
            currentOrderId.value = newOrderId;
            customerForm.value = { ...props.initialCustomerForm };
            recipientName.value = props.initialShipping?.name || '';
            recipientPhone.value = props.initialShipping?.phone || '';
            recipientAddressDetail.value = props.initialShipping?.detail || '';
            recipientProvince.value = props.initialShipping?.province || null;
            recipientDistrict.value = props.initialShipping?.district || null;
            recipientWard.value = props.initialShipping?.ward || null;

            if (props.initialShipping?.province) {
                await fetchDistrictsShip(props.initialShipping.province);
            }
            if (props.initialShipping?.district) {
                await fetchWardsShip(props.initialShipping.district);
            }

            if (props.order?.idKhachHang) {
                await fetchCustomerAddresses();
            } else {
                customerAddresses.value = [];
                selectedAddressId.value = '';
            }
        }
    }
);
</script>

<style scoped>
/* Card Nền Trắng, Chữ Đen, Viền Tinh Tế Nhe Nhàn */
.pos-navy-card {
    background-color: #ffffff !important;
    border: 1px solid #e2e8f0 !important;
    border-radius: 16px !important;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04) !important;
    overflow: visible !important;
    position: relative;
}

.customer-card-wrapper {
    position: relative;
    z-index: 30;
}

.search-input-container {
    z-index: 100;
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

/* Popover Danh sách Tìm kiếm Khách hàng Mềm mại, Thoáng đãng, Hiển thị 3 khách rõ ràng và cuộn mượt */
.suggestion-popover {
    position: absolute;
    top: calc(100% + 6px);
    left: 0;
    right: 0;
    width: 100%;
    max-height: 380px;
    background: #ffffff !important;
    border: 1px solid #cbd5e1 !important;
    border-radius: 12px !important;
    box-shadow: 0 20px 40px -6px rgba(15, 23, 42, 0.25), 0 8px 16px -2px rgba(15, 23, 42, 0.12) !important;
    z-index: 9999 !important;
    overflow: hidden !important;
    padding: 0;
}

.suggestion-list-scroll {
    max-height: 200px;
    min-height: 120px;
    overflow-y: auto !important;
    padding-bottom: 4px;
}

.suggestion-list-scroll::-webkit-scrollbar {
    width: 6px;
}
.suggestion-list-scroll::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 10px;
}
.suggestion-list-scroll::-webkit-scrollbar-thumb:hover {
    background: #94a3b8;
}
.suggestion-list-scroll::-webkit-scrollbar-track {
    background: #f8fafc;
}

.suggestion-item {
    border-bottom: 1px solid #f1f5f9;
    transition: all 0.15s ease;
    padding: 9px 12px !important;
    min-height: 52px;
    box-sizing: border-box;
}

.suggestion-item:last-child {
    border-bottom: none;
}

.suggestion-item:hover {
    background-color: #f8fafc !important;
    border-left: 3px solid #2563eb !important;
}

.customer-name-text {
    font-size: 13px !important;
    font-weight: 600 !important;
    color: #0f172a !important;
    line-height: 1.3 !important;
    display: block;
}

.customer-email-text {
    font-size: 11px !important;
    color: #64748b !important;
    font-weight: 400 !important;
    line-height: 1.2 !important;
    display: block;
}

.customer-phone-badge {
    font-size: 11px !important;
    font-weight: 600 !important;
    color: #1d4ed8 !important;
    background-color: #eff6ff !important;
    border: 1px solid #dbeafe !important;
    padding: 2px 8px !important;
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
    padding: 2px;
    box-sizing: border-box;
}
.shipping-wrapper > .pos-navy-card {
    min-height: 0;
}
.expand-shipping-enter-active,
.expand-shipping-leave-active {
    overflow: hidden;
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
    padding-top: 0 !important;
    padding-bottom: 0 !important;
}
</style>
