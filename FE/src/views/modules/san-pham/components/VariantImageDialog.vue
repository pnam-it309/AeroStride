<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';
import { resolveMediaUrl } from '@/utils/mediaUrl';

const props = defineProps({
    show: { type: Boolean, default: false },
    mode: { type: String, default: 'create' },
    image: { type: Object, default: null },
    statusOptions: { type: Array, default: () => ['DANG_HOAT_DONG', 'KHONG_HOAT_DONG'] },
    loading: { type: Boolean, default: false }
});

const emit = defineEmits(['update:show', 'submit']);

const { addNotification } = useNotifications();
const uploading = ref(false);
const imageInputRef = ref(null);
const localPreviewUrl = ref('');
const previewLoadError = ref(false);

const form = reactive({
    duongDanAnh: '',
    moTa: '',
    hinhAnhDaiDien: false,
    trangThai: 'DANG_HOAT_DONG'
});

const revokeLocalPreview = () => {
    if (localPreviewUrl.value?.startsWith('blob:')) {
        URL.revokeObjectURL(localPreviewUrl.value);
    }
    localPreviewUrl.value = '';
};

const setLocalPreview = (file) => {
    revokeLocalPreview();
    localPreviewUrl.value = URL.createObjectURL(file);
};

const previewImageSrc = computed(() => localPreviewUrl.value || resolveMediaUrl(form.duongDanAnh, 'aerostride/products/variants') || '');
const getApiErrorMessage = (error, fallback) => error?.response?.data?.message || error?.response?.data?.error || fallback;

watch(
    () => [props.show, props.image],
    () => {
        if (!props.show) return;
        revokeLocalPreview();
        previewLoadError.value = false;
        Object.assign(form, {
            duongDanAnh: props.image?.duongDanAnh || '',
            moTa: props.image?.moTa || '',
            hinhAnhDaiDien: Boolean(props.image?.hinhAnhDaiDien),
            trangThai: props.image?.trangThai || 'DANG_HOAT_DONG'
        });
    },
    { immediate: true }
);

watch(previewImageSrc, () => {
    previewLoadError.value = false;
});

onBeforeUnmount(() => {
    revokeLocalPreview();
});

const openFilePicker = () => {
    if (uploading.value) return;
    imageInputRef.value?.click();
};

const handlePreviewError = () => {
    previewLoadError.value = true;
};

const handleUpload = async (event) => {
    const file = event?.target?.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
        addNotification({
            title: 'Sai định dạng',
            subtitle: 'Vui lòng chọn file ảnh hợp lệ',
            color: 'warning'
        });
        event.target.value = '';
        return;
    }

    const maxFileSize = 5 * 1024 * 1024;
    if (file.size > maxFileSize) {
        addNotification({
            title: 'Ảnh quá lớn',
            subtitle: 'Vui lòng chọn ảnh nhỏ hơn 5MB',
            color: 'warning'
        });
        event.target.value = '';
        return;
    }

    setLocalPreview(file);
    previewLoadError.value = false;
    uploading.value = true;
    try {
        const result = await dichVuSanPham.taiLenTepSanPham(file, 'aerostride/products/variants');
        const fileUrl = result?.fileUrl?.trim();

        if (!fileUrl) {
            throw new Error('Missing fileUrl');
        }

        form.duongDanAnh = fileUrl;
        addNotification({
            title: 'Tải ảnh thành công',
            subtitle: 'Đã cập nhật URL ảnh biến thể',
            color: 'success'
        });
    } catch (error) {
        revokeLocalPreview();
        addNotification({
            title: 'Lỗi tải ảnh',
            subtitle: getApiErrorMessage(error, 'Không thể tải ảnh biến thể'),
            color: 'error'
        });
    } finally {
        uploading.value = false;
        event.target.value = '';
    }
};

const handleSubmit = () => {
    if (!form.duongDanAnh?.trim()) {
        addNotification({
            title: 'Thiếu ảnh',
            subtitle: 'Vui lòng tải lên hoặc nhập URL ảnh biến thể',
            color: 'warning'
        });
        return;
    }

    emit('submit', {
        duongDanAnh: form.duongDanAnh?.trim(),
        moTa: form.moTa?.trim() || null,
        hinhAnhDaiDien: Boolean(form.hinhAnhDaiDien),
        trangThai: form.trangThai
    });
};
</script>

<template>
    <v-dialog :model-value="show" max-width="860" @update:model-value="emit('update:show', $event)">
        <v-card class="variant-dialog-card border shadow-none">
            <v-card-title class="px-6 py-5 border-b">
                <div class="d-flex align-center justify-space-between">
                    <div>
                        <div class="text-h6 font-weight-bold text-dark">
                            {{ mode === 'edit' ? 'Cập nhật ảnh biến thể' : 'Thêm ảnh biến thể' }}
                        </div>
                        <div class="text-body-2 text-medium-emphasis mt-1">
                            Mỗi biến thể chỉ nên có một ảnh đại diện đang hoạt động.
                        </div>
                    </div>
                    <v-btn icon variant="text" size="36" @click="emit('update:show', false)">
                        <v-icon>mdi-close</v-icon>
                    </v-btn>
                </div>
            </v-card-title>

            <v-card-text class="px-6 py-6">
                <v-row>
                    <v-col cols="12" md="7">
                        <div class="dialog-field-label">URL ảnh *</div>
                        <v-text-field
                            v-model="form.duongDanAnh"
                            placeholder="https://..."
                            variant="outlined"
                            density="comfortable"
                            hide-details
                            class="mb-4"
                        />

                        <div class="dialog-field-label">Hoặc tải ảnh từ máy</div>
                        <input
                            ref="imageInputRef"
                            type="file"
                            accept="image/*"
                            class="d-none"
                            @change="handleUpload"
                        />
                        <div class="upload-box mb-4" @click="openFilePicker">
                            <div class="d-flex align-center justify-space-between flex-wrap gap-3">
                                <div>
                                    <div class="text-subtitle-2 font-weight-bold text-dark">Chọn file ảnh</div>
                                    <div class="text-body-2 text-medium-emphasis">JPG, PNG, WEBP. Chọn file là xem trước ngay, sau đó hệ thống sẽ tải lên kho lưu trữ.</div>
                                </div>
                                <v-btn color="primary" variant="tonal" class="text-none" :loading="uploading" @click.stop="openFilePicker">
                                    {{ uploading ? 'Đang tải...' : 'Tải ảnh' }}
                                </v-btn>
                            </div>
                        </div>

                        <div class="dialog-field-label">Mô tả ảnh</div>
                        <v-textarea
                            v-model="form.moTa"
                            rows="4"
                            variant="outlined"
                            density="comfortable"
                            hide-details
                            class="mb-4"
                        />

                        <v-row>
                            <v-col cols="12" md="6">
                                <div class="dialog-field-label">Trạng thái *</div>
                                <v-select
                                    v-model="form.trangThai"
                                    :items="statusOptions"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details
                                />
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="dialog-field-label">Ảnh đại diện</div>
                                <div class="switch-box">
                                    <div>
                                        <div class="text-subtitle-2 font-weight-bold text-dark">Đặt làm ảnh đại diện</div>
                                        <div class="text-body-2 text-medium-emphasis">
                                            Nếu bật, backend sẽ tự bỏ cờ đại diện ở ảnh cũ.
                                        </div>
                                    </div>
                                    <v-switch
                                        v-model="form.hinhAnhDaiDien"
                                        color="primary"
                                        hide-details
                                        inset
                                    />
                                </div>
                            </v-col>
                        </v-row>
                    </v-col>

                    <v-col cols="12" md="5">
                        <div class="preview-box">
                            <div class="preview-label">Xem trước</div>
                            <div class="preview-frame">
                                <v-img
                                    v-if="previewImageSrc && !previewLoadError"
                                    :src="previewImageSrc"
                                    cover
                                    height="280"
                                    class="rounded-lg"
                                    @error="handlePreviewError"
                                />
                                <div v-else class="empty-preview">
                                    <v-icon size="42" color="grey">mdi-image-outline</v-icon>
                                    <span>{{ form.duongDanAnh ? 'Không tải được ảnh hiện tại' : 'Chưa có ảnh' }}</span>
                                </div>
                            </div>

                            <div class="preview-meta">
                                <div class="text-subtitle-2 font-weight-bold text-dark">
                                    {{ form.moTa || 'Chưa có mô tả ảnh' }}
                                </div>
                                <div class="text-body-2 text-medium-emphasis mt-2">
                                    {{ form.hinhAnhDaiDien ? 'Ảnh đại diện' : 'Ảnh phụ' }} • {{ form.trangThai }}
                                </div>
                            </div>
                        </div>
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
                    :disabled="uploading || !form.duongDanAnh"
                    @click="handleSubmit"
                >
                    {{ mode === 'edit' ? 'Lưu ảnh' : 'Thêm ảnh' }}
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

.upload-box {
    display: block;
    border: 1px dashed #cbd5e1;
    border-radius: 14px;
    padding: 16px;
    background: #f8fafc;
    cursor: pointer;
}

.switch-box {
    min-height: 56px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 0 16px;
    border: 1px solid #dbe4ef;
    border-radius: 12px;
    background: #ffffff;
}

.preview-box {
    border: 1px solid #dbe4ef;
    border-radius: 18px;
    background: #f8fafc;
    padding: 16px;
}

.preview-label {
    font-size: 13px;
    font-weight: 700;
    color: #334155;
    margin-bottom: 10px;
}

.preview-frame {
    border-radius: 14px;
    overflow: hidden;
    border: 1px solid #e2e8f0;
    background: #ffffff;
}

.empty-preview {
    height: 280px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: #94a3b8;
}

.preview-meta {
    margin-top: 14px;
    padding: 14px;
    border-radius: 14px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
}

.text-dark {
    color: #0f172a;
}
</style>
