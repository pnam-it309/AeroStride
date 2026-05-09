<script setup>
import { computed, ref, watch } from 'vue';
import { DeviceFloppyIcon, PhotoIcon, PlusIcon, RefreshIcon, TrashIcon } from 'vue-tabler-icons';
import { dichVuFile } from '@/services/core/dichVuFile';
import { useNotifications } from '@/services/notificationService';
import { dichVuMauSac, dichVuKichThuoc } from '@/services/product/dichVuThuocTinh';
import FormattedNumberField from './FormattedNumberField.vue';
import SafeProductImage from './SafeProductImage.vue';
import logoPlaceholder from '@/assets/images/logos/logo-light.svg';

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
    return value.fileUrl
        || value.url
        || value.secure_url
        || value.duongDanAnh
        || value.duongDan
        || value.path
        || value.data
        || value.hinhAnh
        || value.anh
        || '';
};

const getImageUrlFromCollection = (collection) => {
    if (!Array.isArray(collection)) {
        return normalizeUploadedFileUrl(collection);
    }

    const mainImage = collection.find(image => image?.hinhAnhDaiDien || image?.anhDaiDien || image?.laAnhChinh);
    return normalizeUploadedFileUrl(mainImage) || normalizeUploadedFileUrl(collection[0]);
};

const getVariantImageUrl = (variant) => {
    if (!variant) return '';

    return normalizeUploadedFileUrl(variant.urlAnh)
        || getImageUrlFromCollection(variant.images)
        || getImageUrlFromCollection(variant.hinhAnhs)
        || getImageUrlFromCollection(variant.anhChiTietSanPhams)
        || getImageUrlFromCollection(variant.hinhAnh)
        || normalizeUploadedFileUrl(variant.anh)
        || normalizeUploadedFileUrl(variant.duongDanAnh)
        || normalizeUploadedFileUrl(variant.imageUrl)
        || '';
};

const getNestedValue = (source, keys) => {
    for (const key of keys) {
        const value = source?.[key];
        if (value !== null && value !== undefined && value !== '') {
            return value;
        }
    }
    return '';
};

const getVariantColorId = (variant) => (
    getNestedValue(variant, ['idMauSac', 'mauSacId'])
    || getNestedValue(variant?.mauSac, ['id', 'value', 'ma'])
    || getNestedValue(variant?.color, ['id', 'value', 'ma'])
    || ''
);

const getVariantColorLabel = (variant) => (
    getNestedValue(variant, ['tenMauSac', 'mauSac', 'mau'])
    || getNestedValue(variant?.mauSac, ['ten', 'name', 'label', 'title'])
    || getNestedValue(variant?.color, ['ten', 'name', 'label', 'title'])
    || ''
);

const getVariantSizeId = (variant) => (
    getNestedValue(variant, ['idKichThuoc', 'kichThuocId', 'sizeId'])
    || getNestedValue(variant?.kichThuoc, ['id', 'value', 'ma'])
    || getNestedValue(variant?.size, ['id', 'value', 'ma'])
    || ''
);

const getVariantSizeLabel = (variant) => (
    getNestedValue(variant, ['tenKichThuoc', 'kichThuoc', 'size'])
    || getNestedValue(variant?.kichThuoc, ['ten', 'name', 'label', 'title', 'giaTriKichThuoc'])
    || getNestedValue(variant?.size, ['ten', 'name', 'label', 'title'])
    || ''
);

const normalizeSearchText = (value) => String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();

const comboboxFilter = (itemTitle, queryText, item) => {
    const normalizedQuery = normalizeSearchText(queryText);
    if (!normalizedQuery) {
        return true;
    }

    const normalizedTitle = normalizeSearchText(item?.raw?.ten || itemTitle || item?.value);
    return normalizedTitle.includes(normalizedQuery);
};

const comboboxProps = {
    clearable: true,
    autoSelectFirst: 'exact'
};

const resolveOptionId = (items, rawValue, fallbackLabel = '') => {
    const optionItems = Array.isArray(items) ? items : [];

    if (rawValue && typeof rawValue === 'object') {
        if (rawValue.id) {
            return rawValue.id;
        }

        const nestedValue = rawValue.value ?? rawValue.ten ?? rawValue.title ?? '';
        return resolveOptionId(optionItems, nestedValue, fallbackLabel);
    }

    const normalizedValue = String(rawValue ?? '').trim();
    if (normalizedValue) {
        const matchedById = optionItems.find(item => item.id === normalizedValue);
        if (matchedById) {
            return matchedById.id;
        }

        const normalizedText = normalizeSearchText(normalizedValue);
        const matchedByLabel = optionItems.find(item => normalizeSearchText(item.ten) === normalizedText);
        if (matchedByLabel) {
            return matchedByLabel.id;
        }

        return normalizedValue;
    }

    const normalizedFallback = normalizeSearchText(fallbackLabel);
    if (!normalizedFallback) {
        return '';
    }

    return optionItems.find(item => normalizeSearchText(item.ten) === normalizedFallback)?.id || fallbackLabel;
};

const getResolvedOptionLabel = (items, selectedId, fallbackLabel = '') => {
    const optionItems = Array.isArray(items) ? items : [];
    const resolvedId = resolveOptionId(optionItems, selectedId, fallbackLabel);
    const matchedOption = optionItems.find(item => item.id === resolvedId || normalizeSearchText(item.ten) === normalizeSearchText(resolvedId));
    return matchedOption?.ten || fallbackLabel || String(resolvedId || '');
};

const withCurrentOption = (items, currentValue, currentLabel) => {
    const optionItems = Array.isArray(items) ? items : [];
    const normalizedCurrentValue = String(currentValue ?? '').trim();
    const normalizedCurrentLabel = String(currentLabel ?? '').trim();

    if (!normalizedCurrentValue && !normalizedCurrentLabel) {
        return optionItems;
    }

    const exists = optionItems.some(item => (
        item.id === normalizedCurrentValue
        || normalizeSearchText(item.ten) === normalizeSearchText(normalizedCurrentValue)
        || normalizeSearchText(item.ten) === normalizeSearchText(normalizedCurrentLabel)
    ));

    if (exists) {
        return optionItems;
    }

    return [
        ...optionItems,
        {
            id: normalizedCurrentValue || normalizedCurrentLabel,
            ten: normalizedCurrentLabel || normalizedCurrentValue
        }
    ];
};

const onKeyUpEnter = (event, field, service, type, label) => {
    const val = event.target.value?.trim();
    if (!val) return;

    const lists = {
        MAU_SAC: props.options.mauSacs || [],
        KICH_THUOC: props.options.kichThuocs || []
    };
    const list = lists[type] || [];
    const normalizedValue = normalizeSearchText(val);
    const exists = list.some(item => normalizeSearchText(item.ten) === normalizedValue);

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
const lockedColorLabel = computed(() => getResolvedOptionLabel(
    props.options.mauSacs,
    formData.value.idMauSac,
    getVariantColorLabel(props.variant)
));
const lockedSizeLabel = computed(() => getResolvedOptionLabel(
    props.options.kichThuocs,
    formData.value.idKichThuoc,
    getVariantSizeLabel(props.variant)
));
const currentVariantImageUrl = computed(() => getVariantImageUrl(props.variant));
const colorComboboxItems = computed(() => withCurrentOption(
    props.options.mauSacs,
    formData.value.idMauSac || getVariantColorId(props.variant),
    getVariantColorLabel(props.variant) || lockedColorLabel.value
));
const sizeComboboxItems = computed(() => withCurrentOption(
    props.options.kichThuocs,
    formData.value.idKichThuoc || getVariantSizeId(props.variant),
    getVariantSizeLabel(props.variant) || lockedSizeLabel.value
));

const getVariantSku = (variant) => (
    variant?.maChiTietSanPham
    || variant?.sku
    || variant?.maSku
    || variant?.maBienThe
    || variant?.ma
    || ''
);

const populateEditFormData = () => {
    if (!props.variant) {
        formData.value = createDefaultFormData();
        return;
    }

    formData.value = {
        maChiTietSanPham: getVariantSku(props.variant),
        idMauSac: resolveOptionId(props.options.mauSacs, getVariantColorId(props.variant), getVariantColorLabel(props.variant)),
        idKichThuoc: resolveOptionId(props.options.kichThuocs, getVariantSizeId(props.variant), getVariantSizeLabel(props.variant)),
        soLuong: Number(props.variant.soLuong ?? 0),
        giaNhap: Number(props.variant.giaNhap ?? 0),
        giaBan: Number(props.variant.giaBan ?? 0),
        trangThai: props.variant.trangThai || 'DANG_HOAT_DONG',
        urlAnh: getVariantImageUrl(props.variant)
    };
};

watch(
    () => [props.open, props.mode, props.variant],
    ([isOpen, mode, variant]) => {
        if (!isOpen) return;

        if (mode === 'edit' && variant) {
            populateEditFormData();
            return;
        }

        formData.value = createDefaultFormData();
    },
    { immediate: true }
);

watch(
    () => [props.open, props.mode, props.variant?.id, props.options.mauSacs?.length, props.options.kichThuocs?.length],
    ([isOpen, mode]) => {
        if (!isOpen || mode !== 'edit' || !props.variant) {
            return;
        }

        const resolvedColorId = resolveOptionId(
            props.options.mauSacs,
            formData.value.idMauSac || getVariantColorId(props.variant),
            getVariantColorLabel(props.variant)
        );
        const resolvedSizeId = resolveOptionId(
            props.options.kichThuocs,
            formData.value.idKichThuoc || getVariantSizeId(props.variant),
            getVariantSizeLabel(props.variant)
        );

        formData.value = {
            ...formData.value,
            maChiTietSanPham: formData.value.maChiTietSanPham || getVariantSku(props.variant),
            idMauSac: resolvedColorId || formData.value.idMauSac,
            idKichThuoc: resolvedSizeId || formData.value.idKichThuoc,
            urlAnh: formData.value.urlAnh || getVariantImageUrl(props.variant)
        };
    }
);

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

watch(
    () => [props.open, props.mode, props.variant?.maChiTietSanPham],
    ([isOpen, mode]) => {
        if (!isOpen || mode !== 'edit') return;

        const currentSku = getVariantSku(props.variant);
        if (currentSku && !formData.value.maChiTietSanPham) {
            formData.value.maChiTietSanPham = currentSku;
        }
    }
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
    formData.value = {
        ...formData.value,
        maChiTietSanPham: formData.value.maChiTietSanPham || getVariantSku(props.variant),
        idMauSac: resolveOptionId(props.options.mauSacs, formData.value.idMauSac, getVariantColorLabel(props.variant)) || getVariantColorId(props.variant),
        idKichThuoc: resolveOptionId(props.options.kichThuocs, formData.value.idKichThuoc, getVariantSizeLabel(props.variant)) || getVariantSizeId(props.variant)
    };

    if (!formData.value.idMauSac || !formData.value.idKichThuoc) {
        addNotification({
            title: 'Lỗi',
            subtitle: 'Không thể xác định màu sắc hoặc kích thước của biến thể. Hãy tải lại danh sách thuộc tính.',
            color: 'error'
        });
        return;
    }

    const result = await formRef.value?.validate();
    if (!result?.valid) return;

    emit('submit', { ...formData.value });
};

const refreshOptions = () => {
    emit('options-refreshed');
};

const submitButtonText = computed(() => props.mode === 'create' ? 'Thêm biến thể' : 'Cập nhật biến thể');
const headerTitle = computed(() => props.mode === 'create' ? 'Thêm biến thể mới' : 'Chỉnh sửa biến thể');
</script>

<template>
    <v-dialog v-model="dialogVisible" max-width="860" persistent transition="dialog-bottom-transition" scrollable>
        <v-card class="rounded-xl border shadow-2xl p-0">
            <div class="h-1-5 w-full bg-primary"></div>

            <v-card-title class="px-8 py-6 border-b d-flex align-center justify-space-between bg-slate-50/50">
                <div>
                    <h3 class="text-h5 font-weight-black text-slate-900 tracking-tight">
                        {{ headerTitle }}
                    </h3>
                    <p class="text-subtitle-2 text-slate-500 font-weight-medium mt-1">
                        Thiết lập SKU, tồn kho và giá cho sản phẩm.
                    </p>
                </div>
            </v-card-title>

            <v-form ref="formRef">
                <v-card-text class="pa-8">
                    <v-row class="variant-modal-layout">
                        <v-col cols="12" md="4">
                            <div class="form-group h-100">
                                <div class="mb-2 px-1">
                                    <span class="text-caption font-weight-bold text-slate-700 tracking-wider">
                                        Hình ảnh
                                    </span>
                                </div>
                                <div class="variant-image-panel pa-0 position-relative overflow-hidden">
                                    <div class="variant-image-panel__preview">
                                        <div 
                                            class="image-wrapper rounded-2xl cursor-pointer hover-lift transition-all w-100"
                                            style="aspect-ratio: 1/1; position: relative; overflow: hidden; background-color: #f8fafc;"
                                            @click="triggerFileInput"
                                        >
                                            <v-img v-if="formData.urlAnh" :src="formData.urlAnh"
                                                cover class="fill-height w-100">
                                                <template #placeholder>
                                                    <div class="d-flex align-center justify-center fill-height">
                                                        <v-progress-circular indeterminate color="primary" />
                                                    </div>
                                                </template>
                                            </v-img>
                                            <div v-else class="d-flex flex-column align-center justify-center fill-height text-slate-300">
                                                <PhotoIcon size="84" stroke-width="1" class="mb-4 opacity-40" />
                                                <span class="text-subtitle-1 font-weight-medium text-slate-400">Chưa có ảnh</span>
                                                <span class="text-caption text-slate-400 mt-1 opacity-60">Nhấp vào đây để tải ảnh</span>
                                            </div>

                                            <!-- Overlay when hovering -->
                                            <div class="image-overlay d-flex align-center justify-center">
                                                <v-icon color="white" size="32">mdi-camera-plus-outline</v-icon>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="variant-image-panel__body mt-4">
                                        <input type="file" ref="fileInput" class="d-none" accept="image/*"
                                            @change="handleImageUpload" />
                                        
                                        <p class="text-center text-xs text-slate-400 leading-relaxed italic">
                                            Sử dụng ảnh vuông (1:1) để có hiển thị tốt nhất trên website.
                                        </p>
                                        
                                        <div v-if="formData.urlAnh" class="d-flex justify-center mt-2">
                                            <v-btn variant="text" color="error" density="compact" class="text-none" @click.stop="formData.urlAnh = ''">
                                                <v-icon start size="14">mdi-trash-can-outline</v-icon>
                                                Xóa ảnh
                                            </v-btn>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </v-col>

                        <v-col cols="12" md="8">
                            <v-row class="variant-modal-fields">
                                <v-col cols="12" md="6">
                                    <div class="form-group">
                                        <div class="d-flex align-center justify-space-between mb-2 px-1">
                                            <span class="text-caption font-weight-bold text-slate-700 tracking-wider">
                                                Màu sắc
                                            </span>
                                            <v-btn variant="text" color="primary" density="compact"
                                                class="text-none px-2 h-auto" @click="refreshOptions">
                                                <RefreshIcon size="12" class="mr-1" /> Làm mới
                                                <v-tooltip activator="parent" location="top" text="Tải lại danh sách thuộc tính" />
                                            </v-btn>
                                        </div>
                                        <v-combobox v-model="formData.idMauSac" v-bind="comboboxProps"
                                            :custom-filter="comboboxFilter" :items="colorComboboxItems" item-title="ten"
                                            item-value="id" :placeholder="lockedColorLabel || 'Chọn màu sắc'"
                                            variant="outlined"
                                            density="comfortable" hide-details="auto" :return-object="false"
                                            class="modern-select"
                                            :rules="[rules.required]"
                                            @keyup.enter="(e) => onKeyUpEnter(e, 'idMauSac', dichVuMauSac, 'MAU_SAC', 'màu sắc')"></v-combobox>
                                    </div>
                                </v-col>

                                <v-col cols="12" md="6">
                                    <div class="form-group">
                                        <div class="mb-2 px-1">
                                            <span class="text-caption font-weight-bold text-slate-700 tracking-wider">
                                                Số lượng tồn
                                            </span>
                                        </div>
                                        <FormattedNumberField v-model="formData.soLuong" min="0"
                                            :rules="[rules.required, rules.min0]" variant="outlined"
                                            density="comfortable" hide-details="auto" placeholder="0"
                                            class="modern-input"></FormattedNumberField>
                                    </div>
                                </v-col>

                                <v-col cols="12" md="6">
                                    <div class="form-group">
                                        <div class="mb-2 px-1">
                                            <span class="text-caption font-weight-bold text-slate-700 tracking-wider">
                                                Kích thước
                                            </span>
                                        </div>
                                        <v-combobox v-model="formData.idKichThuoc" v-bind="comboboxProps"
                                            :custom-filter="comboboxFilter" :items="sizeComboboxItems" item-title="ten"
                                            item-value="id" :placeholder="lockedSizeLabel || 'Chọn kích thước'"
                                            variant="outlined"
                                            density="comfortable" hide-details="auto" :return-object="false"
                                            class="modern-select"
                                            :rules="[rules.required]"
                                            @keyup.enter="(e) => onKeyUpEnter(e, 'idKichThuoc', dichVuKichThuoc, 'KICH_THUOC', 'kích thước')"></v-combobox>
                                    </div>
                                </v-col>

                                <v-col cols="12" md="6">
                                    <div class="form-group">
                                        <div class="mb-2 px-1">
                                            <span class="text-caption font-weight-bold text-slate-700 tracking-wider">
                                                Giá nhập (VNĐ)
                                            </span>
                                        </div>
                                        <FormattedNumberField v-model="formData.giaNhap" min="0" :rules="[rules.min0]"
                                            variant="outlined" density="comfortable" hide-details="auto" placeholder="0"
                                            suffix="₫" class="modern-input"></FormattedNumberField>
                                    </div>
                                </v-col>

                                <v-col cols="12">
                                    <div class="form-group">
                                        <div class="mb-2 px-1">
                                            <span class="text-caption font-weight-bold text-slate-700 tracking-wider">
                                                Mã SKU
                                            </span>
                                        </div>
                                        <v-text-field v-model="formData.maChiTietSanPham" :placeholder="skuPlaceholder"
                                            variant="outlined" density="comfortable" readonly hide-details="auto"
                                            class="modern-input bg-slate-50"></v-text-field>
                                    </div>
                                </v-col>

                                <v-col cols="12">
                                    <div class="form-group">
                                        <div class="mb-2 px-1">
                                            <span class="text-caption font-weight-bold text-slate-700 text-uppercase tracking-wider">
                                                Giá bán (VNĐ)
                                            </span>
                                        </div>
                                        <FormattedNumberField v-model="formData.giaBan" min="0"
                                            :rules="[rules.required, rules.min0]" variant="outlined"
                                            density="comfortable" hide-details="auto" placeholder="0" suffix="₫"
                                            class="modern-input"></FormattedNumberField>
                                    </div>
                                </v-col>
                            </v-row>
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
                    {{ submitButtonText }}
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

.variant-modal-layout {
    align-items: stretch;
}

.variant-image-panel {
    min-height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 20px;
}

.variant-image-panel__preview {
    display: flex;
    justify-content: center;
}

.variant-image-panel__body {
    width: 100%;
}

.variant-modal-fields {
    row-gap: 4px;
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

@media (max-width: 959px) {
    .variant-image-panel {
        min-height: auto;
    }
}
</style>
