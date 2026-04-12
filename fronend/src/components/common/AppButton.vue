<template>
  <button
    class="btn"
    :class="`btn-${variant}`"
    :type="type"
    :disabled="disabled"
  >
    <slot />
  </button>
</template>

<script setup>
defineProps({
  variant: { type: String, default: "primary" },
  type: { type: String, default: "button" },
  disabled: { type: Boolean, default: false },
});
</script>

<style scoped>
.btn {
  height: 40px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s ease;
}

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn-primary {
  color: #fff;
  background: var(--primary);
}

.btn-primary:hover:not(:disabled) {
  background: var(--primary-strong);
}

.btn-ghost {
  color: var(--text);
  background: #fff;
  border-color: var(--border);
}

.btn-ghost:hover:not(:disabled) {
  background: var(--bg-soft);
}
</style>
