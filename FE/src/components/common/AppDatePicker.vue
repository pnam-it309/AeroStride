<template>
  <VueDatePicker
    v-bind="$attrs"
    :enable-time-picker="enableTimePicker"
    :range="range"
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :format="displayFormat"
    locale="vi"
    auto-apply
    :clearable="clearable"
    position="left"
    :month-change-on-scroll="false"
  >
    <template #dp-input="{ value, onInput, onEnter, onTab, onClear }">
      <v-text-field
        :model-value="value"
        readonly
        :placeholder="placeholder"
        variant="outlined"
        density="compact"
        hide-details
        append-inner-icon="mdi-calendar-month-outline"
        :class="inputClass"
        v-bind="textFieldProps"
        @click:clear="onClear"
        :clearable="clearable && !!value"
      ></v-text-field>
    </template>
  </VueDatePicker>
</template>

<script setup>
import { computed } from 'vue';
import { VueDatePicker } from '@vuepic/vue-datepicker';
import '@vuepic/vue-datepicker/dist/main.css';

const props = defineProps({
  modelValue: {
    type: [Date, Array, String, Number, Object],
    default: null
  },
  range: {
    type: Boolean,
    default: false
  },
  enableTimePicker: {
    type: Boolean,
    default: false
  },
  placeholder: {
    type: String,
    default: 'Chọn ngày'
  },
  format: {
    type: [String, Function],
    default: null
  },
  textFieldProps: {
    type: Object,
    default: () => ({})
  },
  inputClass: {
    type: [String, Object, Array],
    default: 'compact-input date-field'
  },
  clearable: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['update:modelValue']);

const displayFormat = computed(() => {
  if (props.format) return props.format;
  if (props.enableTimePicker) return 'dd/MM/yyyy HH:mm';
  return 'dd/MM/yyyy';
});
</script>

<style>
/* Tùy chỉnh màu sắc chính cho VueDatePicker khớp với giao diện AeroStride */
:root {
  --dp-primary-color: #4f46e5;
  --dp-primary-text-color: #ffffff;
  --dp-hover-color: #f3f4f6;
  --dp-hover-text-color: #111827;
  --dp-hover-icon-color: #4b5563;
  --dp-primary-disabled-color: #a5b4fc;
}

.dp__theme_light {
  --dp-background-color: #ffffff;
  --dp-text-color: #1f2937;
  --dp-hover-color: #f3f4f6;
  --dp-hover-text-color: #111827;
  --dp-hover-icon-color: #4b5563;
  --dp-primary-color: #4f46e5;
  --dp-primary-disabled-color: #a5b4fc;
  --dp-primary-text-color: #ffffff;
  --dp-secondary-color: #9ca3af;
  --dp-border-color: #d1d5db;
  --dp-menu-border-color: #e5e7eb;
  --dp-border-color-hover: #9ca3af;
  --dp-disabled-color: #f9fafb;
  --dp-scroll-bar-background: #f3f4f6;
  --dp-scroll-bar-color: #9ca3af;
  --dp-success-color: #10b981;
  --dp-success-color-disabled: #6ee7b7;
  --dp-icon-color: #6b7280;
  --dp-danger-color: #ef4444;
  --dp-marker-color: #ef4444;
  --dp-tooltip-color: #f9fafb;
  --dp-disabled-color-text: #9ca3af;
  --dp-highlight-color: #e0e7ff;
}

/* Đảm bảo ô input của VueDatePicker khớp chuẩn giao diện Vuetify */
.dp__main {
  font-family: 'Inter', system-ui, -apple-system, sans-serif !important;
}
</style>
