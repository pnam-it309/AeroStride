<script setup>
/**
 * Module: Bán hàng tại quầy (Admin)
 * Component: CustomerSelector
 * Chức năng: Cho phép tìm kiếm và chọn khách hàng để gắn vào hóa đơn, 
 *            hoặc tạo nhanh khách hàng mới dựa trên SĐT/Email.
 */
import { computed, ref, watch } from 'vue';
import { UserIcon, PhoneIcon, XIcon } from 'vue-tabler-icons';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { useNotifications } from '@/services/notificationService';
import { useLocation } from '@/composables/useLocation';
import { useAddressMapping } from '@/composables/useAddressMapping';
import { GIOI_TINH_OPTIONS } from '@/constants/appConstants';

const props = defineProps(['selectedCustomerName', 'selectedCustomerPhone', 'activeOrderId']);
const emit = defineEmits(['select', 'remove']);
const { addNotification } = useNotifications();

const search = ref('');
const results = ref([]);
const loading = ref(false);
const showQuickAddDialog = ref(false);
const quickAddLoading = ref(false);
const quickAddForm = ref({ ten: '', sdt: '', email: '', gioiTinh: true, tinh: null, thanhPho: null, phuongXa: null, diaChiChiTiet: '' });
let searchTimer = null;

const { provinces, districts, wards, loadingLocations, fetchProvinces, fetchDistricts, fetchWards } = useLocation();
const { mapCodesToNames } = useAddressMapping();

const selectedCustomerLabel = computed(() => {
    if (!props.selectedCustomerName || props.selectedCustomerName === 'Khách lẻ') return '';
    return props.selectedCustomerName;
});

// Chuẩn hóa chuỗi văn bản: xóa khoảng trắng thừa, chuyển thành chữ thường
const normalizeText = (value) =>
    String(value ?? '')
        .trim()
        .toLowerCase();
// Chuẩn hóa số điện thoại: loại bỏ tất cả các ký tự không phải số
const normalizePhone = (value) => String(value ?? '').replace(/\D/g, '');

// Trích xuất mảng khách hàng từ cấu trúc dữ liệu trả về của API
const extractCustomerList = (response) => {
    if (Array.isArray(response)) return response;
    if (Array.isArray(response?.content)) return response.content;
    if (Array.isArray(response?.data)) return response.data;
    if (Array.isArray(response?.data?.content)) return response.data.content;
    return [];
};

// Tìm khách hàng khớp chính xác thông tin SĐT hoặc email trong một danh sách
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

// Kiểm tra và tìm kiếm khách hàng đã tồn tại trong DB dựa trên SĐT/Email
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

    // Fallback khi endpoint search trả thiếu dữ liệu.
    const allCustomers = await dichVuKhachHang.layTatCaKhachHang();
    return findExactCustomer(extractCustomerList(allCustomers), phone, email) || null;
};

watch(search, (value) => {
    if (searchTimer) clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
        onSearch(value);
    }, 300);
});

// Gửi yêu cầu tìm kiếm lên server với từ khóa (Debounced)
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
        addNotification({
            title: 'Lỗi',
            subtitle: e?.response?.data?.message || 'Không thể tải danh sách khách hàng.',
            color: 'error'
        });
    } finally {
        loading.value = false;
    }
};

// Xử lý khi user chọn một khách hàng từ danh sách kết quả tìm kiếm
const selectCustomer = (c) => {
    if (!c?.id) return;
    emit('select', c);
    search.value = '';
    results.value = [];
};

// Trả về chuỗi tiêu đề dùng hiển thị trên dòng danh sách autocomplete
const customerTitle = (customer) => customer?.tenKhachHang || customer?.ten || customer?.sdt || 'Khách hàng';
// Trả về chuỗi mô tả (subtitle) hiển thị phụ bên dưới tiêu đề
const customerSubtitle = (customer) => {
    const phone = customer?.sdt ? `SĐT: ${customer.sdt}` : '';
    const email = customer?.email ? `Email: ${customer.email}` : '';
    return [phone, email].filter(Boolean).join(' - ') || 'Chưa có thông tin liên hệ';
};

// Xóa form tạo nhanh khách hàng
const resetQuickAddForm = () => {
    quickAddForm.value = { ten: '', sdt: '', email: '', gioiTinh: true, tinh: null, thanhPho: null, phuongXa: null, diaChiChiTiet: '' };
};

// Mở dialog tạo nhanh khách hàng
const openQuickAddDialog = async () => {
    resetQuickAddForm();
    if (provinces.value.length === 0) {
        await fetchProvinces();
    }
    showQuickAddDialog.value = true;
};

// Xử lý submit form tạo nhanh: Kiểm tra xem SĐT đã tồn tại chưa, nếu chưa thì gọi API tạo
const submitQuickAdd = async () => {
    const phone = quickAddForm.value.sdt?.trim() || '';
    const email = quickAddForm.value.email?.trim() || '';
    const name = quickAddForm.value.ten?.trim() || '';

    if (!phone || !name) {
        addNotification({
            title: 'Thiếu thông tin',
            subtitle: 'Vui lòng nhập Tên và Số điện thoại.',
            color: 'warning'
        });
        return;
    }

    const phoneRegex = /^(0[3|5|7|8|9])[0-9]{8}$/;
    if (!phoneRegex.test(phone)) {
        addNotification({
            title: 'SĐT không hợp lệ',
            subtitle: 'Số điện thoại phải có 10 số và bắt đầu bằng 03, 05, 07, 08, hoặc 09.',
            color: 'warning'
        });
        return;
    }

    if (email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            addNotification({
                title: 'Email không hợp lệ',
                subtitle: 'Vui lòng nhập đúng định dạng email (VD: abc@gmail.com).',
                color: 'warning'
            });
            return;
        }
    }

    quickAddLoading.value = true;
    try {
        const existedCustomer = await findExistingCustomerByContact(phone, email);
        if (existedCustomer) {
            emit('select', existedCustomer);
            addNotification({
                title: 'Đã tìm thấy',
                subtitle: `Khách hàng này đã tồn tại, tự động gán khách hàng ${existedCustomer.ten || existedCustomer.sdt}`,
                color: 'success'
            });
            showQuickAddDialog.value = false;
            return;
        }

        const newCustomerPayload = {
            ten: name,
            sdt: phone,
            email: email,
            gioiTinh: quickAddForm.value.gioiTinh,
            tenTaiKhoan: '',
            matKhau: '',
            trangThai: 'DANG_HOAT_DONG',
            ghiChu: 'Khách tạo nhanh tại quầy'
        };

        const createdCustomer = await dichVuKhachHang.taoKhachHang(newCustomerPayload);
        
        // Tạo địa chỉ nếu có điền tỉnh/tp
        if (createdCustomer?.id && quickAddForm.value.tinh && quickAddForm.value.thanhPho && quickAddForm.value.phuongXa && quickAddForm.value.diaChiChiTiet) {
            try {
                const addressPayload = mapCodesToNames(quickAddForm.value, provinces.value, districts.value, wards.value);
                await dichVuKhachHang.taoDiaChi({
                    idKhachHang: createdCustomer.id,
                    tinh: addressPayload.tinh,
                    thanhPho: addressPayload.thanhPho,
                    phuongXa: addressPayload.phuongXa,
                    diaChiChiTiet: addressPayload.diaChiChiTiet,
                    tenNguoiNhan: name,
                    sdtNguoiNhan: phone,
                    laMacDinh: true
                });
            } catch (err) {
                console.error("Lỗi tạo địa chỉ", err);
            }
        }

        const selectedCustomer = createdCustomer?.id ? createdCustomer : await findExistingCustomerByContact(phone, email);

        if (!selectedCustomer) {
            addNotification({
                title: 'Tạo thành công',
                subtitle: 'Đã thêm khách mới nhưng chưa thể tự gán vào hóa đơn.',
                color: 'info'
            });
            showQuickAddDialog.value = false;
            return;
        }

        emit('select', selectedCustomer);
        addNotification({
            title: 'Thành công',
            subtitle: 'Đã tạo khách mới tại quầy và gán vào hóa đơn.',
            color: 'success'
        });
        showQuickAddDialog.value = false;
    } catch (e) {
        addNotification({
            title: 'Lỗi',
            subtitle: e?.response?.data?.message || 'Không thể kiểm tra/tạo khách hàng.',
            color: 'error'
        });
    } finally {
        quickAddLoading.value = false;
    }
};
</script>

<template>
    <div class="customer-selector">
        <div class="d-flex justify-space-between align-center mb-3">
            <h3 class="text-subtitle-1 font-weight-bold">
                <UserIcon size="18" class="mr-1" style="vertical-align: middle; color: #2E4E8E" />
                Khách hàng
            </h3>
            <v-btn
                v-if="!selectedCustomerLabel"
                size="small"
                variant="text"
                color="#2E4E8E"
                prepend-icon="mdi-plus"
                class="text-none"
                @click="openQuickAddDialog"
            >
                Thêm mới
            </v-btn>
        </div>

        <div v-if="selectedCustomerLabel" class="selected-customer-card pa-3 border rounded-lg bg-primary-lighten-5 d-flex align-center">
            <div class="flex-grow-1">
                <div class="font-weight-bold text-body-2">{{ selectedCustomerLabel }}</div>
                <div class="text-caption text-grey-darken-1">
                    {{ selectedCustomerPhone ? `SĐT: ${selectedCustomerPhone}` : 'Khách hàng đã gắn với hóa đơn' }}
                </div>
            </div>
            <v-btn icon size="x-small" variant="text" color="grey-darken-1" @click="emit('remove')">
                <XIcon size="16" />
            </v-btn>
        </div>

        <div v-else>
            <v-autocomplete
                :model-value="null"
                v-model:search="search"
                :items="results"
                :loading="loading"
                :custom-filter="() => true"
                :item-title="customerTitle"
                item-value="id"
                return-object
                clearable
                placeholder="Tìm tên hoặc số điện thoại..."
                variant="outlined"
                density="comfortable"
                hide-details
                prepend-inner-icon="mdi-account-search"
                no-data-text="Nhập ít nhất 2 ký tự để tìm khách hàng"
                @update:model-value="selectCustomer"
            >
                <template #item="{ props: itemProps, item }">
                    <v-list-item v-bind="itemProps" class="py-2">
                        <template #prepend>
                            <v-avatar color="primary" variant="tonal" size="34">
                                <UserIcon size="18" />
                            </v-avatar>
                        </template>
                        <template #title>
                            <span class="font-weight-bold">{{ customerTitle(item.raw) }}</span>
                        </template>
                        <template #subtitle>
                            <span class="d-flex align-center mt-1 text-caption">
                                <PhoneIcon size="14" class="mr-1 opacity-60" />
                                {{ customerSubtitle(item.raw) }}
                            </span>
                        </template>
                    </v-list-item>
                </template>
                <template #selection="{ item }">
                    <span>{{ customerTitle(item.raw) }}</span>
                </template>
            </v-autocomplete>
        </div>

        <v-dialog v-model="showQuickAddDialog" max-width="650" transition="dialog-bottom-transition" persistent>
            <v-card class="rounded-xl overflow-hidden">
                <v-card-title class="text-subtitle-1 font-weight-bold pa-4 border-b bg-slate-50 d-flex justify-space-between align-center">
                    Thêm nhanh thông tin khách hàng
                    <v-btn icon size="small" variant="text" @click="showQuickAddDialog = false"><XIcon size="20"/></v-btn>
                </v-card-title>
                <v-card-text class="pa-5">
                    <div class="text-body-2 text-medium-emphasis mb-4">
                        Nếu SĐT đã tồn tại, hệ thống sẽ tự động nhận diện và gán khách hàng vào đơn.
                    </div>

                    <v-row dense>
                        <v-col cols="12" md="6">
                            <v-text-field
                                v-model="quickAddForm.ten"
                                label="Tên khách hàng"
                                placeholder="Nhập tên..."
                                variant="outlined"
                                density="comfortable"
                                hide-details="auto"
                                class="mb-3"
                                maxlength="100"
                            />
                        </v-col>
                        <v-col cols="12" md="6">
                            <v-text-field
                                v-model="quickAddForm.sdt"
                                label="Số điện thoại"
                                placeholder="Ví dụ: 0912345678"
                                variant="outlined"
                                density="comfortable"
                                hide-details="auto"
                                class="mb-3"
                                @input="quickAddForm.sdt = String($event.target.value || '').replace(/[^0-9]/g, '')"
                            />
                        </v-col>
                        <v-col cols="12" md="6">
                            <v-select
                                v-model="quickAddForm.gioiTinh"
                                :items="GIOI_TINH_OPTIONS"
                                label="Giới tính"
                                variant="outlined"
                                density="comfortable"
                                hide-details="auto"
                                class="mb-3"
                            />
                        </v-col>
                        <v-col cols="12" md="6">
                            <v-text-field
                                v-model="quickAddForm.email"
                                label="Email (Không bắt buộc)"
                                placeholder="Ví dụ: abc@gmail.com"
                                variant="outlined"
                                density="comfortable"
                                hide-details="auto"
                                class="mb-3"
                            />
                        </v-col>
                        
                        <v-col cols="12">
                            <div class="text-subtitle-2 font-weight-bold mt-2 mb-2">Địa chỉ (Tùy chọn)</div>
                        </v-col>
                        <v-col cols="12" md="4">
                            <v-autocomplete v-model="quickAddForm.tinh" :items="provinces" item-title="name"
                                item-value="code" placeholder="Tỉnh / Thành phố" variant="outlined" bg-color="white"
                                density="compact" hide-details :loading="loadingLocations.provinces"
                                @update:model-value="(val) => { quickAddForm.thanhPho = null; quickAddForm.phuongXa = null; if (val) fetchDistricts(val); }" 
                                class="mb-3"/>
                        </v-col>
                        <v-col cols="12" md="4">
                            <v-autocomplete v-model="quickAddForm.thanhPho" :items="districts" item-title="name"
                                item-value="code" placeholder="Quận / Huyện" variant="outlined" bg-color="white"
                                density="compact" hide-details :loading="loadingLocations.districts"
                                :disabled="!quickAddForm.tinh" 
                                @update:model-value="(val) => { quickAddForm.phuongXa = null; if (val) fetchWards(val); }" 
                                class="mb-3"/>
                        </v-col>
                        <v-col cols="12" md="4">
                            <v-autocomplete v-model="quickAddForm.phuongXa" :items="wards" item-title="name"
                                item-value="code" placeholder="Phường / Xã" variant="outlined" bg-color="white"
                                density="compact" hide-details :loading="loadingLocations.wards"
                                :disabled="!quickAddForm.thanhPho" class="mb-3"/>
                        </v-col>
                        <v-col cols="12">
                            <v-text-field v-model="quickAddForm.diaChiChiTiet" placeholder="Địa chỉ cụ thể (Số nhà, đường...)"
                                variant="outlined" density="compact" hide-details />
                        </v-col>
                    </v-row>
                </v-card-text>
                <v-card-actions class="px-6 pb-5 border-t bg-slate-50">
                    <v-spacer />
                    <v-btn variant="tonal" color="slate-500" class="rounded-lg" @click="showQuickAddDialog = false">Hủy</v-btn>
                    <v-btn :loading="quickAddLoading" color="primary" variant="flat" class="px-6 rounded-lg font-weight-bold" @click="submitQuickAdd"> Thêm nhanh </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </div>
</template>

<style scoped>
.selected-customer-card {
    border-color: rgba(var(--v-theme-primary), 0.2) !important;
    background-color: rgba(var(--v-theme-primary), 0.05);
}

.customer-selector :deep(.v-field) {
    border-radius: 10px !important;
}
</style>
