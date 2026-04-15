<template>
  <section class="card">
    <header v-if="title || $slots.header || $slots.actions" class="head">
      <div>
        <slot name="header">
          <h3 v-if="title">{{ title }}</h3>
        </slot>
      </div>
      <div class="actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="body">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: "" },
});
</script>

<style scoped>
.card {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-panel);
  box-shadow: var(--shadow-sm);
  padding: 16px;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

h3 {
  font-size: 17px;
  line-height: 1.2;
  font-weight: 700;
  color: #1f2f46;
}

.actions {
  display: flex;
  gap: 8px;
}
</style>
