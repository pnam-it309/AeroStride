<script setup>
import { ref } from 'vue';
import { XIcon } from 'vue-tabler-icons';

defineProps({
    modelValue: {
        type: Boolean,
        required: true
    },
    scannerElementId: {
        type: String,
        default: 'reader'
    }
});

const emit = defineEmits(['update:modelValue', 'stop', 'scan-file']);
const fileInput = ref(null);

const close = () => {
    emit('stop');
};

const triggerFileInput = () => {
    fileInput.value?.click();
};

const onFileChange = (e) => {
    const file = e.target.files?.[0];
    if (file) {
        emit('scan-file', file);
    }
    e.target.value = '';
};
</script>

<template>
    <!-- Scanner dialog -->
    <v-dialog
        :model-value="modelValue"
        @update:model-value="(val) => emit('update:modelValue', val)"
        max-width="520"
        transition="dialog-bottom-transition"
    >
        <v-card class="rounded-xl pa-5 bg-white border shadow-lg">
            <div class="d-flex justify-space-between align-center mb-4">
                <div class="d-flex align-center">
                    <v-icon color="primary" class="mr-2" size="22">mdi-qrcode-scan</v-icon>
                    <span class="text-h6 font-weight-bold text-slate-800">Quét mã sản phẩm</span>
                </div>
                <v-btn icon variant="text" size="small" @click="close">
                    <XIcon size="20" />
                </v-btn>
            </div>

            <div :id="scannerElementId" class="qr-reader-box"></div>

            <div class="mt-3 text-center text-caption text-slate-500 font-weight-medium">
                Hướng camera về phía mã QR / Barcode hoặc tải ảnh QR từ file (ảnh từ file ZIP).
            </div>

            <!-- Upload file QR image button -->
            <div class="mt-4 pt-3 border-t d-flex justify-center">
                <input
                    ref="fileInput"
                    type="file"
                    accept="image/*"
                    style="display: none"
                    @change="onFileChange"
                />
                <v-btn
                    color="primary"
                    variant="tonal"
                    prepend-icon="mdi-image-search"
                    class="text-none font-weight-bold rounded-lg px-5 py-2"
                    @click="triggerFileInput"
                >
                    Chọn file ảnh QR từ máy (PNG / JPG)
                </v-btn>
            </div>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.qr-reader-box {
    width: 100%;
    min-height: 320px;
    overflow: hidden;
    border: 1px solid #dbe3ef;
    border-radius: 12px;
    background: #0f172a;
}

:deep(.qr-reader-box video) {
    width: 100% !important;
    min-height: 320px;
    object-fit: cover;
}
</style>
