<script setup>
import { ref, watch } from 'vue';
import { XIcon, DeviceFloppyIcon, UploadIcon, PhotoIcon } from 'vue-tabler-icons';
import { dichVuFile } from '@/services/core/dichVuFile';
import { useNotifications } from '@/services/notificationService';

const props = defineProps({
    open: Boolean,
    mode: { type: String, default: 'create' },
    image: Object,
    submitting: Boolean
});

const emit = defineEmits(['close', 'submit']);
const { addNotification } = useNotifications();

const fileInput = ref(null);
const uploading = ref(false);
const formData = ref({
    duongDanAnh: '',
    moTa: '',
    hinhAnhDaiDien: false,
    trangThai: 'DANG_HOAT_DONG'
});

watch(() => props.open, (isOpen) => {
    if (isOpen && props.image) {
        formData.value = {
            duongDanAnh: props.image.duongDanAnh || '',
            moTa: props.image.moTa || '',
            hinhAnhDaiDien: props.image.hinhAnhDaiDien || false,
            trangThai: props.image.trangThai || 'DANG_HOAT_DONG'
        };
    } else if (isOpen) {
        formData.value = {
            duongDanAnh: '',
            moTa: '',
            hinhAnhDaiDien: false,
            trangThai: 'DANG_HOAT_DONG'
        };
    }
});

const triggerFileInput = () => {
    fileInput.value?.click();
};

const handleFileChange = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    // Validate if it's an image
    if (!file.type.startsWith('image/')) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng chọn tệp hình ảnh', color: 'error' });
        return;
    }

    uploading.value = true;
    try {
        const result = await dichVuFile.taiLenFile(file);
        formData.value.duongDanAnh = result.fileUrl;
        addNotification({ title: 'Thành công', subtitle: 'Tải ảnh lên thành công', color: 'success' });
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải ảnh lên Cloudinary', color: 'error' });
    } finally {
        uploading.value = false;
        // Reset file input
        if (event.target) event.target.value = '';
    }
};

const handleSubmit = () => {
    emit('submit', { ...formData.value });
};
</script>

<template>
    <div v-if="open" class="fixed inset-0 z-[3000] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
        <div class="w-full max-w-lg bg-white rounded-[32px] shadow-2xl overflow-hidden animate-scale-in">
            <div class="px-8 py-6 border-b border-slate-100 flex items-center justify-between bg-slate-50">
                <h3 class="font-weight-black text-xl text-slate-900">
                    {{ mode === 'create' ? 'Thêm ảnh mới' : 'Cập nhật ảnh' }}
                </h3>
                <v-btn icon variant="tonal" density="comfortable" color="slate-400" @click="emit('close')">
                    <XIcon size="20" />
                </v-btn>
            </div>

            <div class="p-8 space-y-6">
                <!-- Upload Area -->
                <div 
                    @click="triggerFileInput"
                    class="relative aspect-video rounded-3xl bg-slate-50 border-2 border-dashed border-slate-200 overflow-hidden flex flex-column items-center justify-center cursor-pointer hover:border-primary/50 hover:bg-primary/5 transition-all group"
                >
                    <input 
                        type="file" 
                        ref="fileInput" 
                        class="hidden" 
                        accept="image/*" 
                        @change="handleFileChange"
                    />
                    
                    <div v-if="uploading" class="text-center">
                        <v-progress-circular indeterminate color="primary" class="mb-2"></v-progress-circular>
                        <p class="text-caption font-weight-bold text-slate-500 uppercase tracking-widest">Đang xử lý...</p>
                    </div>
                    <img v-else-if="formData.duongDanAnh" :src="formData.duongDanAnh" class="w-full h-full object-contain" />
                    <div v-else class="text-center group-hover:scale-110 transition-transform duration-300">
                        <UploadIcon class="h-12 w-12 text-slate-300 mx-auto mb-3" />
                        <span class="text-sm text-slate-500 font-bold block mb-1">Nhấn để tải ảnh lên</span>
                        <span class="text-xs text-slate-400 italic">Hỗ trợ JPG, PNG, WEBP (Cloudinary)</span>
                    </div>
                    
                    <div v-if="formData.duongDanAnh && !uploading" class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                        <div class="text-white text-center">
                            <PhotoIcon class="h-8 w-8 mx-auto mb-1" />
                            <span class="text-xs font-bold uppercase tracking-widest">Thay đổi ảnh</span>
                        </div>
                    </div>
                </div>

                <div class="space-y-4">
                    <div>
                        <div class="filter-label mb-2">Đường dẫn ảnh (URL)</div>
                        <v-text-field
                            v-model="formData.duongDanAnh"
                            placeholder="https://..."
                            variant="outlined"
                            density="comfortable"
                            hide-details
                            readonly
                            class="rounded-xl bg-slate-50"
                        >
                            <template #prepend-inner>
                                <PhotoIcon size="18" class="text-slate-400 mr-2" />
                            </template>
                        </v-text-field>
                        <p class="text-[10px] text-slate-400 mt-1 italic pl-1">URL tự động cập nhật sau khi tải ảnh lên Cloudinary</p>
                    </div>

                    <div>
                        <div class="filter-label mb-2">Mô tả hình ảnh</div>
                        <v-text-field
                            v-model="formData.moTa"
                            placeholder="Ví dụ: Mặt trước sản phẩm"
                            variant="outlined"
                            density="comfortable"
                            hide-details
                            class="rounded-xl"
                        ></v-text-field>
                    </div>

                    <div class="pa-4 rounded-2xl bg-slate-50 border border-slate-100 flex items-center justify-space-between cursor-pointer" @click="formData.hinhAnhDaiDien = !formData.hinhAnhDaiDien">
                        <div class="d-flex align-center gap-3">
                            <v-icon :color="formData.hinhAnhDaiDien ? 'primary' : 'slate-300'">
                                {{ formData.hinhAnhDaiDien ? 'mdi-check-circle' : 'mdi-circle-outline' }}
                            </v-icon>
                            <div>
                                <p class="text-sm font-weight-bold text-slate-700 mb-0">Đặt làm ảnh chính</p>
                                <p class="text-caption text-slate-500 mb-0">Hiển thị đầu tiên trong danh sách</p>
                            </div>
                        </div>
                        <v-switch
                            v-model="formData.hinhAnhDaiDien"
                            color="primary"
                            hide-details
                            density="compact"
                        ></v-switch>
                    </div>
                </div>
            </div>

            <div class="px-8 py-6 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-3">
                <v-btn
                    variant="text"
                    @click="emit('close')"
                    class="rounded-xl font-weight-bold px-6 h-11"
                >
                    Hủy bỏ
                </v-btn>
                <v-btn
                    color="primary"
                    @click="handleSubmit"
                    :disabled="submitting || !formData.duongDanAnh || uploading"
                    :loading="submitting"
                    class="rounded-xl font-weight-black px-8 h-11 shadow-md"
                >
                    <DeviceFloppyIcon size="18" class="mr-2" />
                    {{ mode === 'create' ? 'Xác nhận thêm' : 'Lưu thay đổi' }}
                </v-btn>
            </div>
        </div>
    </div>
</template>

<style scoped>
.font-display { font-family: 'Outfit', sans-serif; }
.filter-label {
    font-size: 13px;
    font-weight: 700;
    color: #475569;
}
.animate-scale-in {
    animation: scale-in 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes scale-in {
    from { opacity: 0; transform: scale(0.95); }
    to { opacity: 1; transform: scale(1); }
}
</style>

