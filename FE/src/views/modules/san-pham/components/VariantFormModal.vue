<script setup>
import { ref, watch } from 'vue';
import { XIcon, DeviceFloppyIcon, RefreshIcon } from 'vue-tabler-icons';

const props = defineProps({
    open: Boolean,
    mode: { type: String, default: 'create' },
    variant: Object,
    options: Object,
    submitting: Boolean
});

const emit = defineEmits(['close', 'submit', 'options-refreshed']);

const formData = ref({
    maChiTietSanPham: '',
    idMauSac: '',
    idKichThuoc: '',
    soLuong: 0,
    giaNhap: 0,
    giaBan: 0,
    trangThai: 'DANG_HOAT_DONG'
});

const rules = {
    required: v => !!v || 'Trường này là bắt buộc',
    min0: v => v >= 0 || 'Giá trị phải >= 0'
};

watch(() => props.open, (isOpen) => {
    if (isOpen) {
        if (props.mode === 'edit' && props.variant) {
            formData.value = {
                maChiTietSanPham: props.variant.maChiTietSanPham,
                idMauSac: props.variant.idMauSac,
                idKichThuoc: props.variant.idKichThuoc,
                soLuong: props.variant.soLuong,
                giaNhap: props.variant.giaNhap,
                giaBan: props.variant.giaBan,
                trangThai: props.variant.trangThai
            };
        } else {
            formData.value = {
                maChiTietSanPham: '',
                idMauSac: '',
                idKichThuoc: '',
                soLuong: 0,
                giaNhap: 0,
                giaBan: 0,
                trangThai: 'DANG_HOAT_DONG'
            };
        }
    }
});

const handleSubmit = () => {
    emit('submit', { ...formData.value });
};

const refreshOptions = () => {
    emit('options-refreshed');
};
</script>

<template>
    <v-dialog v-model="props.open" max-width="700" persistent transition="dialog-bottom-transition">
        <v-card class="rounded-xl border shadow-2xl overflow-hidden p-0">
            <!-- Header bar -->
            <div class="h-1-5 w-full bg-primary"></div>
            
            <v-card-title class="px-8 py-6 border-b d-flex align-center justify-space-between bg-slate-50/50">
                <div>
                    <h3 class="text-h5 font-weight-black text-slate-900 tracking-tight">
                        {{ mode === 'create' ? 'Thêm biến thể mới' : 'Chỉnh sửa biến thể' }}
                    </h3>
                    <p class="text-subtitle-2 text-slate-500 font-weight-medium mt-1">Thiết lập SKU, tồn kho và giá cho sản phẩm.</p>
                </div>
            </v-card-title>

            <v-card-text class="pa-8">
                <v-row>
                    <!-- Left Column: Attributes -->
                    <v-col cols="12" md="6">
                        <div class="space-y-5">
                            <div class="form-group">
                                <div class="d-flex align-center justify-space-between mb-2 px-1">
                                    <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">Màu sắc</span>
                                    <v-btn variant="text" color="primary" density="compact" class="text-none px-2 h-auto" @click="refreshOptions">
                                        <RefreshIcon size="12" class="mr-1" /> Làm mới
                                    </v-btn>
                                </div>
                                <v-select
                                    v-model="formData.idMauSac"
                                    :items="options.mauSacs"
                                    item-title="ten"
                                    item-value="id"
                                    :disabled="mode === 'edit'"
                                    placeholder="Chọn màu sắc"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details
                                    class="modern-select"
                                ></v-select>
                            </div>

                            <div class="form-group">
                                <div class="mb-2 px-1">
                                    <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">Kích thước</span>
                                </div>
                                <v-select
                                    v-model="formData.idKichThuoc"
                                    :items="options.kichThuocs"
                                    item-title="ten"
                                    item-value="id"
                                    :disabled="mode === 'edit'"
                                    placeholder="Chọn kích thước"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details
                                    class="modern-select"
                                ></v-select>
                            </div>

                            <div class="form-group">
                                <div class="mb-2 px-1">
                                    <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">Mã SKU</span>
                                </div>
                                <v-text-field
                                    v-model="formData.maChiTietSanPham"
                                    placeholder="Hệ thống tự động tạo..."
                                    variant="outlined"
                                    density="comfortable"
                                    readonly
                                    hide-details="auto"
                                    class="modern-input bg-slate-50"
                                ></v-text-field>
                            </div>

                        </div>
                    </v-col>

                    <!-- Right Column: Inventory & Price -->
                    <v-col cols="12" md="6">
                        <div class="space-y-5">
                            <div class="form-group">
                                <div class="mb-2 px-1">
                                    <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">Số lượng tồn</span>
                                </div>
                                <v-text-field
                                    v-model.number="formData.soLuong"
                                    type="number"
                                    min="0"
                                    :rules="[rules.required, rules.min0]"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details="auto"
                                    placeholder="0"
                                    class="modern-input"
                                ></v-text-field>
                            </div>

                            <div class="form-group">
                                <div class="mb-2 px-1">
                                    <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">Giá nhập (VNĐ)</span>
                                </div>
                                <v-text-field
                                    v-model.number="formData.giaNhap"
                                    type="number"
                                    min="0"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details
                                    placeholder="0"
                                    suffix="₫"
                                    class="modern-input"
                                ></v-text-field>
                            </div>

                            <div class="form-group">
                                <div class="mb-2 px-1">
                                    <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">Giá bán (VNĐ)</span>
                                </div>
                                <v-text-field
                                    v-model.number="formData.giaBan"
                                    type="number"
                                    min="0"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details
                                    placeholder="0"
                                    suffix="₫"
                                    class="modern-input"
                                ></v-text-field>
                            </div>
                        </div>
                    </v-col>
                </v-row>
            </v-card-text>

            <v-divider></v-divider>

            <v-card-actions class="px-8 py-6 bg-slate-50 d-flex justify-end gap-3">
                <v-btn variant="tonal" color="slate-500" class="px-6 rounded-xl font-weight-bold h-11" @click="emit('close')">
                    Hủy bỏ
                </v-btn>
                <v-btn color="primary" variant="flat" class="px-8 rounded-xl font-weight-black h-11" :loading="submitting" @click="handleSubmit">
                    <template #prepend>
                        <DeviceFloppyIcon v-if="!submitting" size="18" />
                        <v-progress-circular v-else indeterminate size="18" width="2" color="white" />
                    </template>
                    {{ mode === 'create' ? 'Tạo biến thể' : 'Lưu cập nhật' }}
                </v-btn>
            </v-card-actions>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.h-1-5 { height: 6px; }
.shadow-2xl { box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); }
.gap-3 { gap: 12px; }
.space-y-5 > * + * { margin-top: 1.25rem; }

:deep(.modern-select .v-field), :deep(.modern-input .v-field) {
    border-radius: 12px !important;
    background-color: #fcfdfe !important;
}

:deep(.v-field--focused .v-field__outline) {
    --v-field-border-width: 2px;
}

:deep(.v-btn) {
    letter-spacing: 0.02em;
}
</style>
