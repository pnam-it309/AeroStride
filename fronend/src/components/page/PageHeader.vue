<template>
  <div class="head">
    <div class="left">
      <button v-if="backTo" class="back" @click="goBack">←</button>
      <div>
        <div class="title">{{ title }}</div>
        <div v-if="subtitle" class="muted">{{ subtitle }}</div>
      </div>
    </div>
    <div class="right">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: "" },
  backTo: { type: Object, default: null },
});

const router = useRouter();
function goBack() {
  router.push(props.backTo);
}
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 2px;
}
.left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.01em;
}
.back {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  font-weight: 900;
}
.back:hover {
  background: rgba(22, 126, 242, 0.06);
  border-color: rgba(22, 126, 242, 0.2);
}
</style>
