<script setup>
import { ref, watch } from 'vue';
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

watch(() => props.show, (val) => {
  if (val) inputValue.value = '';
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
  <v-dialog v-model="props.show" max-width="450" persistent transition="dialog-bottom-transition">
    <v-card class="premium-confirm-card">
      <v-card-text class="pa-8 pb-4">
        <div class="d-flex align-start">
          <div class="icon-box-wrapper mr-5" style="background-color: #f8f9ff">
            <div class="icon-inner" style="background-color: #f0f1ff">
              <AlertTriangleIcon v-if="color === 'warning'" size="28" style="color: #1e257c" />
              <AlertTriangleIcon v-else-if="color === 'error'" size="28" class="text-error" />
              <CheckIcon v-else-if="color === 'success'" size="28" class="text-success" />
              <InfoCircleIcon v-else size="28" style="color: #1e257c" />
            </div>
          </div>
          <div class="flex-grow-1 pt-1">
            <h3 class="text-h6 font-weight-bold text-slate-800 mb-2">{{ title }}</h3>
            <p class="text-body-2 text-slate-500 leading-relaxed">{{ message }}</p>
          </div>
        </div>

        <div v-if="showInput" class="mt-6">
          <v-textarea
            v-model="inputValue"
            :label="inputLabel"
            variant="outlined"
            density="comfortable"
            hide-details
            rows="3"
            class="custom-textarea"
            :placeholder="`Nhập ${inputLabel.toLowerCase()}...`"
          ></v-textarea>
          <div v-if="inputRequired && !inputValue" class="text-caption text-error mt-2 ml-1 d-flex align-center">
            <v-icon size="14" class="mr-1">mdi-alert-circle</v-icon>
            Vui lòng nhập {{ inputLabel.toLowerCase() }}
          </div>
        </div>
      </v-card-text>

      <v-card-actions class="pa-6 pt-2 d-flex justify-end gap-3">
        <v-btn 
          variant="text" 
          class="confirm-btn-cancel px-6" 
          :disabled="loading"
          @click="handleCancel"
        >
          {{ cancelText }}
        </v-btn>
        <v-btn 
          variant="flat" 
          class="confirm-btn-submit px-8" 
          :loading="loading"
          :disabled="inputRequired && !inputValue"
          @click="handleConfirm"
        >
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
  align-center: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-inner {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-center: center;
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
  text-transform: none !important;
  font-weight: 600 !important;
  letter-spacing: 0.3px;
  color: #64748b !important;
}

.confirm-btn-submit {
  background-color: #1e257c !important;
  color: white !important;
  text-transform: none !important;
  font-weight: 700 !important;
  letter-spacing: 0.5px;
  border-radius: 10px !important;
  height: 42px !important;
  box-shadow: 0 4px 12px rgba(30, 37, 124, 0.2) !important;
}

.confirm-btn-submit:hover {
  background-color: #151a5c !important;
  transform: translateY(-1px);
  box-shadow: 0 6px 15px rgba(30, 37, 124, 0.3) !important;
}

.gap-3 {
  gap: 12px;
}
</style>
