<script setup>
import { ref, watch, onMounted, computed } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import { dichVuNhanVien } from '@/services/admin/dichVuNhanVien';
import { dichVuGiaoCa } from '@/services/admin/dichVuGiaoCa';
import { useNotifications } from '@/services/notificationService';

const props = defineProps({
    modelValue: { type: Boolean, default: false },
    mode: { type: String, default: 'open' }, // 'open' or 'close'
    currentShift: { type: Object, default: null } // Used for close mode
});

const emit = defineEmits(['update:modelValue', 'success']);
const { addNotification } = useNotifications();
const authStore = useAuthStore();

const show = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
});

const loading = ref(false);
const tienBanDau = ref(0);
const tienThucTe = ref(0);
const ghiChu = ref('');
const nhanVienNhanCaId = ref(null);
const listNhanVien = ref([]);

const tienChenhLech = computed(() => {
    if (props.mode !== 'close' || !props.currentShift) return 0;
    const initial = props.currentShift.tienBanDau || 0;
    const revenue = props.currentShift.tongDoanhThu || 0;
    const totalExpected = initial + revenue;
    return Number(tienThucTe.value || 0) - totalExpected;
});

const loadEmployees = async () => {
    try {
        const res = await dichVuNhanVien.layTatCaNhanVien();
        const data = res?.data?.content || res?.data || res || [];
        listNhanVien.value = data.filter(e => e.tenTaiKhoan !== authStore.user?.username);
    } catch (e) {
        console.error(e);
    }
};

watch(show, (val) => {
    if (val) {
        tienBanDau.value = 0;
        tienThucTe.value = 0;
        ghiChu.value = '';
        nhanVienNhanCaId.value = null;
        if (props.mode === 'close') loadEmployees();
    }
});

const submit = async () => {
    loading.value = true;
    try {
        if (props.mode === 'open') {
            const currentEmpRes = await dichVuNhanVien.layTatCaNhanVien();
            const employees = currentEmpRes?.data?.content || currentEmpRes?.data || currentEmpRes || [];
            const me = employees.find(e => e.tenTaiKhoan === authStore.user?.username);
            
            if (!me) throw new Error("Không tìm thấy thông tin nhân viên đăng nhập");

            await dichVuGiaoCa.moCa({
                nhanVienId: me.id,
                tienBanDau: tienBanDau.value || 0
            });
            addNotification({ title: 'Thành công', subtitle: 'Đã mở ca làm việc', color: 'success' });
            emit('success');
            show.value = false;
        } else {
            await dichVuGiaoCa.chotCa({
                nhanVienNhanCaId: nhanVienNhanCaId.value,
                tienThucTe: tienThucTe.value || 0,
                ghiChu: ghiChu.value
            });
            addNotification({ title: 'Thành công', subtitle: 'Đã chốt ca làm việc', color: 'success' });
            emit('success');
            show.value = false;
        }
    } catch (e) {
        addNotification({ title: 'Lỗi', subtitle: e.message || 'Thao tác thất bại', color: 'error' });
    } finally {
        loading.value = false;
    }
};

</script>

<template>
    <v-dialog v-model="show" max-width="500" persistent>
        <v-card class="rounded-xl">
            <v-card-title class="font-weight-bold d-flex align-center pa-4 bg-primary text-white">
                <v-icon class="mr-2">{{ mode === 'open' ? 'mdi-store-clock' : 'mdi-store-remove' }}</v-icon>
                {{ mode === 'open' ? 'Mở Ca Làm Việc' : 'Chốt Ca / Giao Ca' }}
            </v-card-title>
            <v-card-text class="pa-4">
                <template v-if="mode === 'open'">
                    <p class="mb-4 text-slate-600">Bạn cần nhập số tiền lẻ ban đầu trong két trước khi bắt đầu bán hàng.</p>
                    <div class="filter-field-label">Tiền mặt đầu ca (VNĐ)</div>
                    <v-text-field v-model.number="tienBanDau" type="number" variant="outlined" density="compact" />
                </template>
                <template v-else>
                    <v-row dense class="mb-4">
                        <v-col cols="6">
                            <div class="text-caption text-slate-500">Tiền đầu ca</div>
                            <div class="text-body-1 font-weight-bold">{{ currentShift?.tienBanDau?.toLocaleString() }} đ</div>
                        </v-col>
                        <v-col cols="6">
                            <div class="text-caption text-slate-500">Doanh thu tiền mặt</div>
                            <div class="text-body-1 font-weight-bold text-success">+{{ currentShift?.tongDoanhThu?.toLocaleString() }} đ</div>
                        </v-col>
                    </v-row>
                    <v-divider class="mb-4" />
                    
                    <div class="filter-field-label">Tiền mặt thực tế đếm được (VNĐ) <span class="text-error">*</span></div>
                    <v-text-field v-model.number="tienThucTe" type="number" variant="outlined" density="compact" />
                    
                    <div class="d-flex justify-space-between align-center mb-4 pa-3 rounded" :class="tienChenhLech < 0 ? 'bg-red-50' : (tienChenhLech > 0 ? 'bg-blue-50' : 'bg-green-50')">
                        <span class="font-weight-medium">Tiền chênh lệch:</span>
                        <span class="font-weight-bold" :class="tienChenhLech < 0 ? 'text-error' : (tienChenhLech > 0 ? 'text-info' : 'text-success')">
                            {{ tienChenhLech > 0 ? '+' : '' }}{{ tienChenhLech.toLocaleString() }} đ
                        </span>
                    </div>

                    <div class="filter-field-label">Nhân viên nhận ca (Tùy chọn)</div>
                    <v-autocomplete
                        v-model="nhanVienNhanCaId"
                        :items="listNhanVien"
                        item-title="ten"
                        item-value="id"
                        placeholder="Chọn nhân viên bàn giao"
                        variant="outlined" density="compact" clearable
                    />

                    <div class="filter-field-label mt-3">Ghi chú giao ca</div>
                    <v-textarea v-model="ghiChu" rows="2" variant="outlined" density="compact" />
                </template>
            </v-card-text>
            <v-card-actions class="pa-4 pt-0">
                <v-spacer />
                <v-btn v-if="mode === 'close'" variant="text" @click="show = false">Hủy bỏ</v-btn>
                <v-btn color="primary" variant="flat" :loading="loading" @click="submit" class="px-6 rounded-lg">
                    {{ mode === 'open' ? 'Bắt Đầu Bán Hàng' : 'Xác Nhận Chốt Ca' }}
                </v-btn>
            </v-card-actions>
        </v-card>
    </v-dialog>
</template>
