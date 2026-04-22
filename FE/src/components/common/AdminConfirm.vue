<script setup>
import { defineProps, defineEmits, ref, watch } from 'vue';
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
  <v-dialog v-model="props.show" max-width="450" persistent>
    <v-card class="rounded-0 border shadow-2xl overflow-hidden p-0">
      <div 
        class="h-2 w-full" 
        :class="`bg-${color}`"
      ></div>
      
      <v-card-text class="pa-6">
        <div class="d-flex align-center mb-4">
          <div 
            class="pa-3 mr-4" 
            :class="`bg-${color}-lighten-4 rounded-0 d-flex align-center justify-center`"
            style="width: 56px; height: 56px;"
          >
            <AlertTriangleIcon v-if="color === 'warning'" size="32" class="text-warning" />
            <AlertTriangleIcon v-else-if="color === 'error'" size="32" class="text-error" />
            <CheckIcon v-else-if="color === 'success'" size="32" class="text-success" />
            <InfoCircleIcon v-else size="32" class="text-primary" />
          </div>
          <div>
            <h3 class="text-h6 font-weight-black text-dark tracking-wide">{{ title }}</h3>
            <p class="text-subtitle-2 text-medium-emphasis mt-1">{{ message }}</p>
          </div>
        </div>

        <div v-if="showInput" class="mt-4">
          <v-textarea
            v-model="inputValue"
            :label="inputLabel"
            variant="outlined"
            density="comfortable"
            hide-details
            rows="3"
            class="rounded-0"
            :placeholder="`Nhập ${inputLabel.toLowerCase()}...`"
          ></v-textarea>
          <div v-if="inputRequired && !inputValue" class="text-caption text-error mt-1">
            * Vui lòng nhập {{ inputLabel.toLowerCase() }}
          </div>
        </div>
      </v-card-text>

      <v-divider></v-divider>
      
      <v-card-actions class="pa-4 bg-grey-lighten-4 d-flex justify-end gap-2">
        <v-btn 
          variant="text" 
          class="font-weight-black text-none px-6" 
          rounded="0"
          :disabled="loading"
          @click="handleCancel"
        >
          {{ cancelText }}
        </v-btn>
        <v-btn 
          :color="color" 
          variant="flat" 
          class="font-weight-black text-none px-8 text-white" 
          rounded="0"
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
.shadow-2xl {
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}
.w-full { width: 100%; }
.h-2 { height: 8px; }
.gap-2 { gap: 8px; }
</style>
