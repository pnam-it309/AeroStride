<template>
  <label class="field">
    <span v-if="label" class="label">{{ label }}</span>
    <input
      class="input"
      :type="type"
      :placeholder="placeholder"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  </label>
</template>

<script setup>
defineProps({
  modelValue: { type: [String, Number], default: "" },
  label: { type: String, default: "" },
  placeholder: { type: String, default: "" },
  type: { type: String, default: "text" },
});

defineEmits(["update:modelValue"]);
</script>

<style scoped>
.field {
  display: grid;
  gap: 6px;
}

.label {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.input {
  height: 40px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  background: #fff;
  padding: 0 12px;
  color: var(--text);
  outline: none;
}

.input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(11, 107, 203, 0.14);
}
</style>
