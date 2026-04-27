<script setup>
import { computed, ref, watch } from 'vue';
import { DeviceFloppyIcon, PhotoIcon, PlusIcon, RefreshIcon, TrashIcon } from 'vue-tabler-icons';
import { dichVuFile } from '@/services/core/dichVuFile';
import { useNotifications } from '@/services/notificationService';
import { dichVuMauSac, dichVuKichThuoc } from '@/services/product/dichVuThuocTinh';

const props = defineProps({
    open: Boolean,
    mode: { type: String, default: 'create' },
    variant: Object,
    options: {
        type: Object,
        default: () => ({
            mauSacs: [],
            kichThuocs: [],
            trangThais: []
        })
    },
    submitting: Boolean,
    productCode: String,
    lockAttributesOnEdit: {
        type: Boolean,
        default: true
    },
    allowImageUpload: {
        type: Boolean,
        default: true
    }
});

const emit = defineEmits(['close', 'submit', 'options-refreshed']);

const { addNotification } = useNotifications();
const formRef = ref(null);
const fileInput = ref(null);
const uploadingImage = ref(false);

const dialogVisible = computed({
    get: () => props.open,
    set: (value) => {
        if (!value) emit('close');
    }
});

const createDefaultFormData = () => ({
    maChiTietSanPham: '',
    idMauSac: '',
    idKichThuoc: '',
    soLuong: 0,
    giaNhap: 0,
    giaBan: 0,
    trangThai: 'DANG_HOAT_DONG',
    urlAnh: ''
});

const formData = ref(createDefaultFormData());

const normalizeUploadedFileUrl = (value) => {
    if (!value) return '';
    if (typeof value === 'string') return value;
    return value.fileUrl || value.url || value.secure_url || value.duongDanAnh || value.data || '';
};

const onKeyUpEnter = (event, field, service, type, label) => {
    const val = event.target.value?.trim();
    if (!val) return;

    const lists = {
        MAU_SAC: props.options.mauSacs || [],
        KICH_THUOC: props.options.kichThuocs || []
    };
    const list = lists[type] || [];
    const exists = list.some(item => item.ten?.toLowerCase() === val.toLowerCase());

    if (!exists) {
        autoCreateAttribute(val, field, service, type, label);
    }
};

const autoCreateAttribute = async (val, field, service, type, label) => {
    try {
        const payload = { ten: val, ma: '', moTa: 'Tự động thêm từ biến thể' };
        if (type === 'MAU_SAC') payload.maMauHex = '#000000';

        const methodName = `tao${type.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join('')}`;
        const newEntity = await service[methodName](payload);

        emit('options-refreshed');

        setTimeout(() => {
            formData.value[field] = newEntity.id;
            addNotification({ title: 'Thành công', subtitle: `Đã thêm nhanh ${label}: ${val}`, color: 'success' });
        }, 100);
    } catch (error) {
        console.error(error);
        addNotification({ title: 'Lỗi', subtitle: `Không thể thêm nhanh ${label}`, color: 'error' });
    }
};

const hasValue = (value) => value !== null && value !== undefined && value !== '';

const rules = {
    required: v => hasValue(v) || 'Trường này là bắt buộc',
    min0: v => Number(v) >= 0 || 'Giá trị phải >= 0'
};

const skuPlaceholder = computed(() => (
    props.productCode ? 'Hệ thống tự động tạo...' : 'SKU sẽ tự sinh khi lưu sản phẩm'
));

const shouldLockAttributes = computed(() => props.mode === 'edit' && props.lockAttributesOnEdit);

watch(() => props.open, (isOpen) => {
    if (!isOpen) return;

    if (props.mode === 'edit' && props.variant) {
        formData.value = {
            maChiTietSanPham: props.variant.maChiTietSanPham || '',
            idMauSac: props.variant.idMauSac || '',
            idKichThuoc: props.variant.idKichThuoc || '',
            soLuong: Number(props.variant.soLuong ?? 0),
            giaNhap: Number(props.variant.giaNhap ?? 0),
            giaBan: Number(props.variant.giaBan ?? 0),
            trangThai: props.variant.trangThai || 'DANG_HOAT_DONG',
            urlAnh: normalizeUploadedFileUrl(props.variant.urlAnh)
        };
        return;
    }

    formData.value = createDefaultFormData();
});

const generateSKU = () => {
    if (shouldLockAttributes.value) return;

    const color = (props.options.mauSacs || []).find(item => item.id === formData.value.idMauSac);
    const size = (props.options.kichThuocs || []).find(item => item.id === formData.value.idKichThuoc);

    if (!props.productCode || !color || !size) {
        formData.value.maChiTietSanPham = '';
        return;
    }

    formData.value.maChiTietSanPham = `${props.productCode}-${color.ma}-${size.ten}`.toUpperCase();
};

watch(
    () => [formData.value.idMauSac, formData.value.idKichThuoc, props.productCode, props.mode],
    generateSKU
);

const triggerFileInput = () => {
    fileInput.value?.click();
};

const handleImageUpload = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    uploadingImage.value = true;
    try {
        const url = normalizeUploadedFileUrl(await dichVuFile.taiLenFile(file));
        formData.value.urlAnh = url;
        addNotification({ title: 'Thành công', subtitle: 'Tải ảnh lên thành công', color: 'success' });
    } catch (error) {
        console.error(error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải ảnh lên', color: 'error' });
    } finally {
        uploadingImage.value = false;
        event.target.value = '';
    }
};

const handleSubmit = async () => {
    const result = await formRef.value?.validate();
    if (!result?.valid) return;

    emit('submit', { ...formData.value });
};

const refreshOptions = () => {
    emit('options-refreshed');
};
</script>

<template>
    <v-dialog v-model="dialogVisible" max-width="700" persistent transition="dialog-bottom-transition" scrollable>
        <v-card class="rounded-xl border shadow-2xl p-0">
            <div class="h-1-5 w-full bg-primary"></div>

            <v-card-title class="px-8 py-6 border-b d-flex align-center justify-space-between bg-slate-50/50">
                <div>
                    <h3 class="text-h5 font-weight-black text-slate-900 tracking-tight">
                        {{ mode === 'create' ? 'Thêm biến thể mới' : 'Chỉnh sửa biến thể' }}
                    </h3>
                    <p class="text-subtitle-2 text-slate-500 font-weight-medium mt-1">
                        Thiết lập SKU, tồn kho và giá cho sản phẩm.
                    </p>
                </div>
            </v-card-title>

            <v-form ref="formRef">
                <v-card-text class="pa-8">
                    <v-row>
                        <v-col cols="12" md="6">
                            <div class="space-y-5">
                                <div class="form-group">
                                    <div class="d-flex align-center justify-space-between mb-2 px-1">
                                        <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">
                                            Màu sắc
                                        </span>
                                        <v-btn variant="text" color="primary" density="compact"
                                            class="text-none px-2 h-auto" @click="refreshOptions">
                                            <RefreshIcon size="12" class="mr-1" /> Làm mới
                                        </v-btn>
                                    </div>
                                    <v-combobox v-model="formData.idMauSac" :items="options.mauSacs" item-title="ten"
                                        item-value="id" :disabled="shouldLockAttributes" placeholder="Chọn màu sắc"
                                        variant="outlined" density="comfortable" hide-details="auto"
                                        :return-object="false" class="modern-select" :rules="[rules.required]"
                                        @keyup.enter="(e) => onKeyUpEnter(e, 'idMauSac', dichVuMauSac, 'MAU_SAC', 'màu sắc')"></v-combobox>
                                </div>

                                <div class="form-group">
                                    <div class="mb-2 px-1">
                                        <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">
                                            Kích thước
                                        </span>
                                    </div>
                                    <v-combobox v-model="formData.idKichThuoc" :items="options.kichThuocs" item-title="ten"
                                        item-value="id" :disabled="shouldLockAttributes" placeholder="Chọn kích thước"
                                        variant="outlined" density="comfortable" hide-details="auto"
                                        :return-object="false" class="modern-select" :rules="[rules.required]"
                                        @keyup.enter="(e) => onKeyUpEnter(e, 'idKichThuoc', dichVuKichThuoc, 'KICH_THUOC', 'kích thước')"></v-combobox>
                                </div>

                                <div class="form-group">
                                    <div class="mb-2 px-1">
                                        <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">
                                            Mã SKU
                                        </span>
                                    </div>
                                    <v-text-field v-model="formData.maChiTietSanPham" :placeholder="skuPlaceholder"
                                        variant="outlined" density="comfortable" readonly hide-details="auto"
                                        class="modern-input bg-slate-50"></v-text-field>
                                </div>

                                <div class="form-group">
                                    <div class="mb-2 px-1">
                                        <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">
                                            Hình ảnh
                                        </span>
                                    </div>
                                    <div class="variant-image-upload border-dashed rounded-xl pa-2 d-flex align-center gap-4 bg-slate-50/50">
                                        <v-avatar rounded="lg" size="80" class="border bg-white shadow-sm">
                                            <v-img v-if="formData.urlAnh" :src="formData.urlAnh" cover></v-img>
                                            <div v-else class="fill-height d-flex align-center justify-center text-slate-300">
                                                <PhotoIcon size="32" />
                                            </div>
                                        </v-avatar>
                                        <div class="flex-grow-1">
                                            <input type="file" ref="fileInput" class="d-none" accept="image/*"
                                                @change="handleImageUpload" />

                                            <div v-if="allowImageUpload" class="d-flex gap-2">
                                                <v-btn variant="flat" color="primary" size="small"
                                                    class="rounded-lg text-none" :loading="uploadingImage"
                                                    @click="triggerFileInput">
                                                    <PlusIcon size="14" class="mr-1" /> Chọn ảnh
                                                </v-btn>
                                                <v-btn v-if="formData.urlAnh" variant="tonal" color="error" size="small"
                                                    class="rounded-lg text-none px-2" @click="formData.urlAnh = ''">
                                                    <TrashIcon size="14" />
                                                </v-btn>
                                            </div>

                                            <p v-if="allowImageUpload" class="text-caption text-slate-400 mt-2 line-height-tight">
                                                Dung lượng tối đa 2MB. Định dạng JPG, PNG.
                                            </p>
                                            <p v-else class="text-caption text-slate-500 mt-2 line-height-tight">
                                                Ảnh của biến thể đang được quản lý ở màn chi tiết biến thể để tránh ghi đè dữ liệu ngoài ý muốn.
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </v-col>

                        <v-col cols="12" md="6">
                            <div class="space-y-5">
                                <div class="form-group">
                                    <div class="mb-2 px-1">
                                        <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">
                                            Số lượng tồn
                                        </span>
                                    </div>
                                    <v-text-field v-model.number="formData.soLuong" type="number" min="0"
                                        :rules="[rules.required, rules.min0]" variant="outlined" density="comfortable"
                                        hide-details="auto" placeholder="0" class="modern-input"></v-text-field>
                                </div>

                                <div class="form-group">
                                    <div class="mb-2 px-1">
                                        <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">
                                            Giá nhập (VNĐ)
                                        </span>
                                    </div>
                                    <v-text-field v-model.number="formData.giaNhap" type="number" min="0"
                                        :rules="[rules.min0]" variant="outlined" density="comfortable" hide-details="auto"
                                        placeholder="0" suffix="₫" class="modern-input"></v-text-field>
                                </div>

                                <div class="form-group">
                                    <div class="mb-2 px-1">
                                        <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">
                                            Giá bán (VNĐ)
                                        </span>
                                    </div>
                                    <v-text-field v-model.number="formData.giaBan" type="number" min="0"
                                        :rules="[rules.required, rules.min0]" variant="outlined" density="comfortable"
                                        hide-details="auto" placeholder="0" suffix="₫" class="modern-input"></v-text-field>
                                </div>
                            </div>
                        </v-col>
                    </v-row>
                </v-card-text>
            </v-form>

            <v-divider></v-divider>

            <v-card-actions class="px-8 py-6 bg-slate-50 d-flex justify-end gap-3">
                <v-btn variant="tonal" color="slate-500" class="px-6 rounded-xl font-weight-bold h-11"
                    @click="emit('close')">
                    Hủy bỏ
                </v-btn>
                <v-btn color="primary" variant="flat" class="px-8 rounded-xl font-weight-black h-11 text-white"
                    :loading="submitting" @click="handleSubmit">
                    <template #prepend>
                        <DeviceFloppyIcon v-if="!submitting" size="18" class="text-white" />
                        <v-progress-circular v-else indeterminate size="18" width="2" color="white" />
                    </template>
                    {{ mode === 'create' ? 'Tạo biến thể' : 'Lưu cập nhật' }}
                </v-btn>
            </v-card-actions>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.h-1-5 {
    height: 6px;
}

.shadow-2xl {
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.gap-3 {
    gap: 12px;
}

.space-y-5>*+* {
    margin-top: 1.25rem;
}

:deep(.modern-select .v-field),
:deep(.modern-input .v-field) {
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

