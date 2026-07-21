<script setup>
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

const emit = defineEmits(['update:modelValue', 'stop']);

const close = () => {
    emit('stop');
};
</script>

<template>
    <!-- Scanner dialog -->
    <v-dialog :model-value="modelValue" @update:model-value="(val) => emit('update:modelValue', val)" max-width="500" transition="dialog-bottom-transition">
        <v-card class="rounded-lg pa-4">
            <div class="d-flex justify-space-between align-center mb-4">
                <span class="text-h6 font-weight-bold">Quét mã sản phẩm</span>
                <v-btn icon variant="text" @click="close">
                    <XIcon />
                </v-btn>
            </div>
            <div :id="scannerElementId" class="qr-reader-box"></div>
            <div class="mt-4 text-center text-caption text-grey">Đưa mã QR hoặc Barcode của sản phẩm vào khung hình</div>
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
