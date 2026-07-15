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

import QuickAddCustomerDialog from './QuickAddCustomerDialog.vue';

const props = defineProps(['selectedCustomerName', 'selectedCustomerPhone', 'activeOrderId']);
const emit = defineEmits(['select', 'remove']);
const { addNotification } = useNotifications();

const search = ref('');
const results = ref([]);
const loading = ref(false);
const showQuickAddDialog = ref(false);


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

// Mở dialog tạo nhanh khách hàng
const openQuickAddDialog = () => {
    showQuickAddDialog.value = true;
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

        <QuickAddCustomerDialog 
            v-model="showQuickAddDialog"
            @success="(customer) => emit('select', customer)"
        />
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
