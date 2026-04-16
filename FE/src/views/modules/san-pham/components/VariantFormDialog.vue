<script setup>
import { computed, reactive, watch } from 'vue';

const props = defineProps({
    show: { type: Boolean, default: false },
    mode: { type: String, default: 'create' },
    variant: { type: Object, default: null },
    colorOptions: { type: Array, default: () => [] },
    sizeOptions: { type: Array, default: () => [] },
    statusOptions: { type: Array, default: () => ['DANG_HOAT_DONG', 'KHONG_HOAT_DONG'] },
    loading: { type: Boolean, default: false }
});

const emit = defineEmits(['update:show', 'submit']);

const form = reactive({
    idMauSac: '',
    idKichThuoc: '',
    soLuong: 0,
    giaNhap: '',
    giaBan: '',
    trangThai: 'DANG_HOAT_DONG'
});

const isEditMode = computed(() => props.mode === 'edit');

watch(
    () => [props.show, props.variant],
    () => {
        if (!props.show) return;
        Object.assign(form, {
            idMauSac: props.variant?.idMauSac || '',
            idKichThuoc: props.variant?.idKichThuoc || '',
            soLuong: props.variant?.soLuong ?? 0,
            giaNhap: props.variant?.giaNhap ?? '',
            giaBan: props.variant?.giaBan ?? '',
            trangThai: props.variant?.trangThai || 'DANG_HOAT_DONG'
        });
    },
    { immediate: true }
);

const normalizeNumber = (value, fallback = null) => {
    if (value === '' || value === null || value === undefined) return fallback;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
};

const handleSubmit = () => {
    emit('submit', {
        idMauSac: form.idMauSac,
        idKichThuoc: form.idKichThuoc,
        maChiTietSanPham: null,
        soLuong: normalizeNumber(form.soLuong, 0),
        giaNhap: normalizeNumber(form.giaNhap),
        giaBan: normalizeNumber(form.giaBan, 0),
        trangThai: form.trangThai,
        images: []
    });
};
</script>

<template>
    <v-dialog :model-value="show" max-width="760" @update:model-value="emit('update:show', $event)">
        <v-card class="variant-dialog-card border shadow-none">
            <v-card-title class="px-6 py-5 border-b">
                <div class="d-flex align-center justify-space-between">
                    <div>
                        <div class="text-h6 font-weight-bold text-dark">
                            {{ isEditMode ? 'Cập nhật biến thể sản phẩm' : 'Thêm biến thể sản phẩm' }}
                        </div>
                        <div class="text-body-2 text-medium-emphasis mt-1">
                            Biến thể là tổ hợp duy nhất của màu sắc và kích thước.
                        </div>
                    </div>
                    <v-btn icon variant="text" size="36" @click="emit('update:show', false)">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </div>
            </v-card-title>

            <v-card-text class="px-6 py-6">
                <v-row>
                    <v-col cols="12" md="6">
                        <div class="dialog-field-label">Màu sắc *</div>
                        <v-select
                            v-model="form.idMauSac"
                            :items="colorOptions"
                            item-title="ten"
                            item-value="id"
                            placeholder="Chọn màu sắc"
                            variant="outlined"
                            density="comfortable"
                            hide-details
                        />
                    </v-col>

                    <v-col cols="12" md="6">
                        <div class="dialog-field-label">Kích thước *</div>
                        <v-select
                            v-model="form.idKichThuoc"
                            :items="sizeOptions"
                            item-title="ten"
                            item-value="id"
                            placeholder="Chọn kích thước"
                            variant="outlined"
                            density="comfortable"
                            hide-details
                        />
                    </v-col>

                    <v-col cols="12" md="4">
                        <div class="dialog-field-label">Số lượng *</div>
                        <v-text-field
                            v-model.number="form.soLuong"
                            type="number"
                            min="0"
                            variant="outlined"
                            density="comfortable"
                            hide-details
                        />
                    </v-col>

                    <v-col cols="12" md="4">
                        <div class="dialog-field-label">Giá nhập</div>
                        <v-text-field
                            v-model.number="form.giaNhap"
                            type="number"
                            min="0"
                            step="1000"
                            variant="outlined"
                            density="comfortable"
                            hide-details
                        />
                    </v-col>

                    <v-col cols="12" md="4">
                        <div class="dialog-field-label">Giá bán *</div>
                        <v-text-field
                            v-model.number="form.giaBan"
                            type="number"
                            min="0"
                            step="1000"
                            variant="outlined"
                            density="comfortable"
                            hide-details
                        />
                    </v-col>

                    <v-col cols="12" md="6">
                        <div class="dialog-field-label">Mã biến thể</div>
                        <div class="generated-code-box">
                            {{ variant?.maChiTietSanPham || 'Hệ thống sẽ tự sinh sau khi lưu' }}
                        </div>
                    </v-col>

                    <v-col cols="12" md="6">
                        <div class="dialog-field-label">Trạng thái *</div>
                        <v-select
                            v-model="form.trangThai"
                            :items="statusOptions"
                            placeholder="Chọn trạng thái"
                            variant="outlined"
                            density="comfortable"
                            hide-details
                        />
                    </v-col>
                </v-row>
            </v-card-text>

            <v-card-actions class="px-6 py-4 border-t bg-grey-lighten-5">
                <v-spacer />
                <v-btn variant="text" class="text-none font-weight-medium" @click="emit('update:show', false)">
                    Hủy
                </v-btn>
                <v-btn
                    color="primary"
                    class="text-none px-6 font-weight-bold"
                    :loading="loading"
                    :disabled="!form.idMauSac || !form.idKichThuoc || form.giaBan === '' || form.soLuong === ''"
                    @click="handleSubmit"
                >
                    {{ isEditMode ? 'Lưu thay đổi' : 'Thêm biến thể' }}
                </v-btn>
            </v-card-actions>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.variant-dialog-card {
    border-radius: 18px !important;
}

.dialog-field-label {
    font-size: 13px;
    font-weight: 700;
    color: #334155;
    margin-bottom: 6px;
}

.generated-code-box {
    min-height: 56px;
    display: flex;
    align-items: center;
    padding: 0 16px;
    border: 1px dashed #cbd5e1;
    border-radius: 12px;
    background: #f8fafc;
    color: #475569;
    font-size: 13px;
    font-weight: 600;
}

.text-dark {
    color: #0f172a;
}
</style>
