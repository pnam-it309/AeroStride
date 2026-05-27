<script setup>
import { ref, watch, computed } from 'vue';
import { AlertTriangleIcon, InfoCircleIcon, CheckIcon } from 'vue-tabler-icons';

const props = defineProps({
    show: { type: Boolean, default: false },
    title: { type: String, default: 'Xác nhận thao tác' },
    message: { type: String, default: 'Bạn có chắc chắn muốn thực hiện hành động này không?' },
    confirmText: { type: String, default: 'Đồng ý' },
    cancelText: { type: String, default: 'Hủy bỏ' },
    color: { type: String, default: 'primary' },
    loading: { type: Boolean, default: false },
    showInput: { type: Boolean, default: false },
    inputLabel: { type: String, default: 'Ghi chú' },
    inputRequired: { type: Boolean, default: false }
});

const emit = defineEmits(['update:show', 'confirm', 'cancel']);
const inputValue = ref('');

watch(
    () => props.show,
    (val) => {
        if (val) inputValue.value = '';
    }
);

// Dynamic color mapping using Tailwind-compatible colors
const colorClasses = computed(() => {
    const maps = {
        warning: { outer: 'bg-orange-50', inner: 'bg-orange-100', icon: 'text-orange-700' },
        error: { outer: 'bg-red-50', inner: 'bg-red-100', icon: 'text-red-700' },
        success: { outer: 'bg-green-50', inner: 'bg-green-100', icon: 'text-green-700' },
        primary: { outer: 'bg-blue-50', inner: 'bg-blue-100', icon: 'text-blue-900' },
        info: { outer: 'bg-blue-50', inner: 'bg-blue-100', icon: 'text-blue-900' }
    };
    return maps[props.color] || maps.primary;
});

const handleConfirm = () => {
    emit('confirm', inputValue.value);
};

const handleCancel = () => {
    emit('update:show', false);
    emit('cancel');
};
</script>

<template>
    <v-dialog v-model="props.show" max-width="450" persistent transition="confirm-dialog-transition">
        <v-card class="premium-confirm-card">
            <v-card-text class="pa-8 pb-4">
                <div class="d-flex align-start">
                    <!-- Icon Section with Dynamic Tailwind Classes -->
                    <div class="icon-box-wrapper mr-5" :class="colorClasses.outer">
                        <div class="icon-inner" :class="colorClasses.inner">
                            <AlertTriangleIcon v-if="color === 'warning'" size="28" :class="colorClasses.icon" />
                            <AlertTriangleIcon v-else-if="color === 'error'" size="28" :class="colorClasses.icon" />
                            <CheckIcon v-else-if="color === 'success'" size="28" :class="colorClasses.icon" />
                            <InfoCircleIcon v-else size="28" :class="colorClasses.icon" />
                        </div>
                    </div>
                    <div class="flex-grow-1 pt-1">
                        <h3 class="text-h6 font-weight-bold text-slate-800 mb-2">{{ title }}</h3>
                        <p class="text-body-2 text-slate-500 leading-relaxed">{{ message }}</p>
                    </div>
                </div>

                <div v-if="showInput" class="mt-6">
                    <label class="text-body-2 text-slate-700 font-weight-medium mb-2 d-block">{{ inputLabel }}</label>
                    <v-textarea v-model="inputValue" variant="outlined" density="comfortable" hide-details rows="3"
                        class="custom-textarea" :placeholder="`Nhập ${inputLabel.toLowerCase()}...`"></v-textarea>
                    <div v-if="inputRequired && !inputValue"
                        class="text-caption text-red-500 mt-2 ml-1 d-flex align-center">
                        <v-icon size="14" class="mr-1">mdi-alert-circle</v-icon>
                        Vui lòng nhập {{ inputLabel.toLowerCase() }}
                    </div>
                </div>
            </v-card-text>

            <v-card-actions class="pa-6 pt-2 d-flex justify-end gap-3">
                <v-btn variant="text" class="confirm-btn-cancel px-6" :disabled="loading" @click="handleCancel">
                    {{ cancelText }}
                </v-btn>
                <v-btn variant="flat" :color="color || 'primary'" class="confirm-btn-submit px-8" :loading="loading"
                    :disabled="inputRequired && !inputValue" @click="handleConfirm">
                    {{ confirmText }}
                </v-btn>
            </v-card-actions>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.premium-confirm-card {
    border-radius: 20px !important;
    border: 1px solid rgba(0, 0, 0, 0.05) !important;
    box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.1) !important;
    overflow: hidden;
    background: white;
}

.icon-box-wrapper {
    width: 64px;
    height: 64px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

/* Semantic States */
.icon-box-wrapper.is-warning {
    background-color: #fff7ed;
}

.icon-box-wrapper.is-warning .icon-inner {
    background-color: #ffedd5;
    color: #c2410c;
}

.icon-box-wrapper.is-error {
    background-color: #fef2f2;
}

.icon-box-wrapper.is-error .icon-inner {
    background-color: #fee2e2;
    color: #b91c1c;
}

.icon-box-wrapper.is-success {
    background-color: #f0fdf4;
}

.icon-box-wrapper.is-success .icon-inner {
    background-color: #dcfce7;
    color: #15803d;
}

.icon-box-wrapper.is-primary,
.icon-box-wrapper.is-info {
    background-color: #eff6ff;
}

.icon-box-wrapper.is-primary .icon-inner,
.icon-box-wrapper.is-info .icon-inner {
    background-color: #dbeafe;
    color: #1e3a8a;
}

.icon-inner {
    width: 46px;
    height: 46px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.leading-relaxed {
    line-height: 1.6;
}

.custom-textarea :deep(.v-field) {
    border-radius: 12px !important;
    font-size: 14px;
}

.confirm-btn-cancel {
    font-weight: 600 !important;
    color: #64748b !important;
}

.confirm-btn-submit {
    font-weight: 700 !important;
    border-radius: 10px !important;
    height: 42px !important;
}
</style>
