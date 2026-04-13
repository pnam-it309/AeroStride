<script setup>
import { ref } from 'vue';
import { UserIcon, PhoneIcon, XIcon } from 'vue-tabler-icons';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { useNotifications } from '@/services/notificationService';

const props = defineProps(['selectedCustomerName', 'activeOrderId']);
const emit = defineEmits(['select', 'remove']);
const { addNotification } = useNotifications();

const search = ref('');
const results = ref([]);
const loading = ref(false);
const showQuickAddDialog = ref(false);
const quickAddLoading = ref(false);
const quickAddForm = ref({ sdt: '', email: '' });

const normalizeText = (value) =>
    String(value ?? '')
        .trim()
        .toLowerCase();
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

    // Fallback khi endpoint search trả thiếu dữ liệu.
    const allCustomers = await dichVuKhachHang.layTatCaKhachHang();
    return findExactCustomer(extractCustomerList(allCustomers), phone, email) || null;
};

const onSearch = async () => {
    if (!search.value || search.value.length < 2) {
        results.value = [];
        return;
    }
    loading.value = true;
    try {
        const data = await dichVuDonHang.searchKhachHang(search.value);
        results.value = data || [];
    } catch (e) {
        console.error(e);
    } finally {
        loading.value = false;
    }
};

const selectCustomer = (c) => {
    emit('select', c);
    search.value = '';
    results.value = [];
};

const resetQuickAddForm = () => {
    quickAddForm.value = { sdt: '', email: '' };
};

const openQuickAddDialog = () => {
    resetQuickAddForm();
    showQuickAddDialog.value = true;
};

const submitQuickAdd = async () => {
    const phone = quickAddForm.value.sdt?.trim() || '';
    const email = quickAddForm.value.email?.trim() || '';

    if (!phone && !email) {
        addNotification({
            title: 'Thiếu thông tin',
            subtitle: 'Nhập SĐT hoặc email để kiểm tra khách hàng.',
            color: 'warning'
        });
        return;
    }

    quickAddLoading.value = true;
    try {
        const existedCustomer = await findExistingCustomerByContact(phone, email);
        if (existedCustomer) {
            emit('select', existedCustomer);
            addNotification({
                title: 'Đã tìm thấy',
                subtitle: `Đã gắn khách hàng ${existedCustomer.ten || existedCustomer.sdt}`,
                color: 'success'
            });
            showQuickAddDialog.value = false;
            return;
        }

        if (!phone) {
            addNotification({
                title: 'Không thể tạo mới',
                subtitle: 'Khách mới tại quầy bắt buộc có SĐT.',
                color: 'warning'
            });
            return;
        }

        const newCustomerPayload = {
            ten: `Khách lẻ ${phone}`,
            sdt: phone,
            email: email || '',
            tenTaiKhoan: '',
            matKhau: '',
            trangThai: 'DANG_HOAT_DONG',
            ghiChu: 'Khách tạo nhanh tại quầy'
        };

        const createdCustomer = await dichVuKhachHang.taoKhachHang(newCustomerPayload);
        const selectedCustomer = createdCustomer?.id ? createdCustomer : await findExistingCustomerByContact(phone, email);

        if (!selectedCustomer) {
            addNotification({
                title: 'Tạo thành công',
                subtitle: 'Đã thêm khách mới nhưng chưa thể tự gắn vào hóa đơn.',
                color: 'info'
            });
            showQuickAddDialog.value = false;
            return;
        }

        emit('select', selectedCustomer);
        addNotification({
            title: 'Thành công',
            subtitle: 'Đã tạo khách mới tại quầy và gắn vào hóa đơn.',
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
                <UserIcon size="18" class="mr-1 text-#2E4E8E" style="vertical-align: middle" />
                Khách hàng
            </h3>
            <v-btn
                v-if="!selectedCustomerName"
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

        <div v-if="selectedCustomerName" class="selected-customer-card pa-3 border rounded-lg bg-primary-lighten-5 d-flex align-center">
            <div class="flex-grow-1">
                <div class="font-weight-bold text-body-2">{{ selectedCustomerName }}</div>
                <div class="text-caption text-grey-darken-1">Khách hàng thân thiết</div>
            </div>
            <v-btn icon size="x-small" variant="text" color="grey-darken-1" @click="emit('remove')">
                <XIcon size="16" />
            </v-btn>
        </div>

        <div v-else class="position-relative">
            <v-text-field
                v-model="search"
                placeholder="Tìm tên hoặc số điện thoại..."
                variant="outlined"
                density="comfortable"
                hide-details
                prepend-inner-icon="mdi-account-search"
                @input="onSearch"
            ></v-text-field>

            <v-card v-if="results.length > 0" class="search-overlay mt-1 shadow-lg border overflow-hidden">
                <v-list class="pa-0">
                    <v-list-item v-for="c in results" :key="c.id" class="pa-3 border-b" @click="selectCustomer(c)">
                        <v-list-item-title class="font-weight-bold">{{ c.ten }}</v-list-item-title>
                        <v-list-item-subtitle class="d-flex align-center mt-1">
                            <PhoneIcon size="14" class="mr-1 opacity-60" /> {{ c.sdt }}
                        </v-list-item-subtitle>
                    </v-list-item>
                </v-list>
            </v-card>
        </div>

        <v-dialog v-model="showQuickAddDialog" max-width="520">
            <v-card>
                <v-card-title class="text-subtitle-1 font-weight-bold">Thêm hoặc nhận diện khách hàng</v-card-title>
                <v-card-text>
                    <div class="text-body-2 text-medium-emphasis mb-4">
                        Nhập SĐT hoặc email để tìm khách đã tồn tại. Nếu chưa có dữ liệu, hệ thống sẽ tạo khách mới tại quầy theo SĐT.
                    </div>

                    <v-text-field
                        v-model="quickAddForm.sdt"
                        label="Số điện thoại"
                        placeholder="Ví dụ: 0912345678"
                        variant="outlined"
                        density="comfortable"
                        hide-details
                        class="mb-4"
                    />

                    <v-text-field
                        v-model="quickAddForm.email"
                        label="Email"
                        placeholder="Ví dụ: abc@gmail.com"
                        variant="outlined"
                        density="comfortable"
                        hide-details
                    />
                </v-card-text>
                <v-card-actions class="px-6 pb-5">
                    <v-spacer />
                    <v-btn variant="text" color="secondary" @click="showQuickAddDialog = false">Hủy</v-btn>
                    <v-btn :loading="quickAddLoading" color="#2E4E8E" variant="flat" @click="submitQuickAdd"> Kiểm tra / Tạo mới </v-btn>
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

.search-overlay {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 999;
}
</style>
