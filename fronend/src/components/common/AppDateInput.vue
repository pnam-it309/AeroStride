<template>
  <AppInput
    :label="label"
    :model-value="modelValue"
    type="date"
    @update:model-value="$emit('update:modelValue', $event)"
  />
</template>

<script setup>
import AppInput from "@/components/common/AppInput.vue";

defineProps({
  modelValue: { type: String, default: "" },
  label: { type: String, default: "" },
});

defineEmits(["update:modelValue"]);
</script>
