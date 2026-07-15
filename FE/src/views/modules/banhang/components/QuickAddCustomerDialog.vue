<script setup>
import { ref, watch } from 'vue';
import { XIcon } from 'vue-tabler-icons';
import { GIOI_TINH_OPTIONS } from '@/constants/appConstants';
import { useLocation } from '@/composables/useLocation';
import { useAddressMapping } from '@/composables/useAddressMapping';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import { useNotifications } from '@/services/notificationService';

const props = defineProps({
    modelValue: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['update:modelValue', 'success']);

const { addNotification } = useNotifications();
const { provinces, districts, wards, loadingLocations, fetchProvinces, fetchDistricts, fetchWards } = useLocation();
const { mapCodesToNames } = useAddressMapping();

const loading = ref(false);
const form = ref({
    ten: '',
    sdt: '',
    email: '',
    gioiTinh: true,
    tinh: null,
    thanhPho: null,
    phuongXa: null,
    diaChiChiTiet: ''
});

watch(() => props.modelValue, async (newVal) => {
    if (newVal) {
        form.value = { ten: '', sdt: '', email: '', gioiTinh: true, tinh: null, thanhPho: null, phuongXa: null, diaChiChiTiet: '' };
        if (provinces.value.length === 0) {
            await fetchProvinces();
        }
    }
});

const findExactCustomer = (list, phone, email) => {
    const normalizedPhone = String(phone ?? '').replace(/\D/g, '');
    const normalizedEmail = String(email ?? '').trim().toLowerCase();

    return list.find((customer) => {
        const customerPhone = String(customer?.sdt ?? '').replace(/\D/g, '');
        const customerEmail = String(customer?.email ?? '').trim().toLowerCase();

        if (normalizedPhone && customerPhone === normalizedPhone) return true;
        if (normalizedEmail && customerEmail === normalizedEmail) return true;
        return false;
    });
};

const findExistingCustomerByContact = async (phone, email) => {
    const keywords = [phone, email].filter(Boolean);
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
    const content = allCustomers?.content || [];
    return findExactCustomer(content, phone, email) || null;
};

const submitQuickAdd = async () => {
    const phone = form.value.sdt?.trim() || '';
    const email = form.value.email?.trim() || '';
    const name = form.value.ten?.trim() || '';

    if (!phone || !name) {
        addNotification({ title: 'Thiếu thông tin', subtitle: 'Vui lòng nhập Tên và Số điện thoại.', color: 'warning' });
        return;
    }

    const phoneRegex = /^(0[3|5|7|8|9])[0-9]{8}$/;
    if (!phoneRegex.test(phone)) {
        addNotification({ title: 'SĐT không hợp lệ', subtitle: 'Số điện thoại phải có 10 số và bắt đầu bằng 03, 05, 07, 08, hoặc 09.', color: 'warning' });
        return;
    }

    loading.value = true;
    try {
        const existedCustomer = await findExistingCustomerByContact(phone, email);
        if (existedCustomer) {
            addNotification({
                title: 'Đã tìm thấy',
                subtitle: `Khách hàng này đã tồn tại, tự động gán khách hàng ${existedCustomer.ten || existedCustomer.sdt}`,
                color: 'success'
            });
            emit('success', existedCustomer);
            emit('update:modelValue', false);
            return;
        }

        const newCustomerPayload = {
            ten: name,
            sdt: phone,
            email: email,
            gioiTinh: form.value.gioiTinh,
            tenTaiKhoan: '',
            matKhau: '',
            trangThai: 'DANG_HOAT_DONG',
            ghiChu: 'Khách tạo nhanh tại quầy'
        };

        const createdCustomer = await dichVuKhachHang.taoKhachHang(newCustomerPayload);

        if (createdCustomer?.id && form.value.tinh && form.value.thanhPho && form.value.phuongXa && form.value.diaChiChiTiet) {
            try {
                const addressPayload = mapCodesToNames(form.value, provinces.value, districts.value, wards.value);
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

        const targetCustomer = createdCustomer?.id ? createdCustomer : await findExistingCustomerByContact(phone, email);
        if (targetCustomer) {
            addNotification({ title: 'Thành công', subtitle: 'Đã thêm khách mới vào hóa đơn.', color: 'success' });
            emit('success', targetCustomer);
        } else {
            addNotification({ title: 'Lỗi đồng bộ', subtitle: 'Tạo khách hàng nhưng không lấy được thông tin.', color: 'warning' });
        }
        emit('update:modelValue', false);
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: e?.response?.data?.message || 'Không thể tạo khách hàng.', color: 'error' });
    } finally {
        loading.value = false;
    }
};

const close = () => {
    emit('update:modelValue', false);
};
</script>

<template>
    <v-dialog :model-value="modelValue" @update:model-value="(val) => emit('update:modelValue', val)" max-width="650" transition="dialog-bottom-transition" persistent>
        <v-card class="rounded-lg overflow-hidden">
            <v-card-title class="text-subtitle-1 font-weight-bold pa-4 border-b bg-slate-50 d-flex justify-space-between align-center">
                Thêm nhanh thông tin khách hàng
                <v-btn icon size="small" variant="text" @click="close">
                    <XIcon size="20" />
                </v-btn>
            </v-card-title>
            <v-card-text class="pa-5">
                <div class="text-body-2 text-medium-emphasis mb-4">
                    Nếu SĐT đã tồn tại, hệ thống sẽ tự động nhận diện và gán khách hàng vào đơn.
                </div>

                <v-row dense>
                    <v-col cols="12" md="6">
                        <v-text-field v-model="form.ten" label="Tên khách hàng" placeholder="Nhập tên..."
                            variant="outlined" density="comfortable" hide-details="auto" class="mb-3 text-body-2"
                            maxlength="100" />
                    </v-col>
                    <v-col cols="12" md="6">
                        <v-text-field v-model="form.sdt" label="Số điện thoại"
                            placeholder="Ví dụ: 0912345678" variant="outlined" density="comfortable"
                            hide-details="auto" class="mb-3 text-body-2"
                            @input="form.sdt = String($event.target.value || '').replace(/[^0-9]/g, '')" />
                    </v-col>
                    <v-col cols="12" md="6">
                        <v-select v-model="form.gioiTinh" :items="GIOI_TINH_OPTIONS" label="Giới tính"
                            variant="outlined" density="comfortable" hide-details="auto" class="mb-3 text-body-2" />
                    </v-col>
                    <v-col cols="12" md="6">
                        <v-text-field v-model="form.email" label="Email (Không bắt buộc)"
                            placeholder="Ví dụ: abc@gmail.com" variant="outlined" density="comfortable"
                            hide-details="auto" class="mb-3 text-body-2" />
                    </v-col>

                    <v-col cols="12">
                        <div class="text-subtitle-2 font-weight-bold mt-2 mb-2 text-slate-700">Địa chỉ (Tùy chọn)</div>
                    </v-col>
                    <v-col cols="12" md="4">
                        <v-autocomplete v-model="form.tinh" :items="provinces" item-title="name"
                            item-value="code" placeholder="Tỉnh / Thành phố" variant="outlined" bg-color="white"
                            density="compact" hide-details :loading="loadingLocations.provinces"
                            @update:model-value="(val) => { form.thanhPho = null; form.phuongXa = null; if (val) fetchDistricts(val); }"
                            class="mb-3 text-body-2" />
                    </v-col>
                    <v-col cols="12" md="4">
                        <v-autocomplete v-model="form.thanhPho" :items="districts" item-title="name"
                            item-value="code" placeholder="Quận / Huyện" variant="outlined" bg-color="white"
                            density="compact" hide-details :loading="loadingLocations.districts"
                            :disabled="!form.tinh"
                            @update:model-value="(val) => { form.phuongXa = null; if (val) fetchWards(val); }"
                            class="mb-3 text-body-2" />
                    </v-col>
                    <v-col cols="12" md="4">
                        <v-autocomplete v-model="form.phuongXa" :items="wards" item-title="name"
                            item-value="code" placeholder="Phường / Xã" variant="outlined" bg-color="white"
                            density="compact" hide-details :loading="loadingLocations.wards"
                            :disabled="!form.thanhPho" class="mb-3 text-body-2" />
                    </v-col>
                    <v-col cols="12">
                        <v-text-field v-model="form.diaChiChiTiet"
                            placeholder="Địa chỉ cụ thể (Số nhà, đường...)" variant="outlined" density="compact"
                            hide-details class="text-body-2" />
                    </v-col>
                </v-row>
            </v-card-text>
            <v-card-actions class="px-6 pb-5 border-t bg-slate-50">
                <v-spacer />
                <v-btn variant="tonal" color="slate-500" class="rounded-lg text-none" @click="close">Hủy</v-btn>
                <v-btn :loading="loading" color="primary" variant="flat" class="px-6 rounded-lg font-weight-bold text-none" @click="submitQuickAdd">Thêm nhanh</v-btn>
            </v-card-actions>
        </v-card>
    </v-dialog>
</template>
