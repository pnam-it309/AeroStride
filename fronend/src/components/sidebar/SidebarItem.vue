<template>
  <RouterLink
    :to="to"
    class="item"
    :class="{ active: isActive, collapsed: collapsed }"
    :title="label"
  >
    <Icon :name="icon" />
    <span v-show="!collapsed">{{ label }}</span>
  </RouterLink>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import Icon from "@/components/common/Icon.vue";

const props = defineProps({
  to: { type: [String, Object], required: true },
  label: { type: String, required: true },
  icon: { type: String, default: "box" },
  collapsed: { type: Boolean, default: false },
});

const route = useRoute();
const isActive = computed(() => {
  if (typeof props.to === "string") {
    if (props.to === "/") return route.path === "/";
    return route.path.startsWith(props.to);
  }

  if (props.to?.name && route.name !== props.to.name) return false;

  const query = props.to?.query || {};
  return Object.keys(query).every(
    (key) => String(route.query[key] || "") === String(query[key]),
  );
});
</script>

<style scoped>
.item {
  display: flex;
  align-items: center;
  gap: 13px;
  border-radius: 10px;
  color: #334155;
  padding: 11px 12px;
  font-size: 15px;
  letter-spacing: 0.2px;
  line-height: 1.35;
  font-weight: 600;
}

.item:hover {
  background: #f2f6ff;
}

.item.active {
  color: var(--primary);
  background: var(--primary-soft);
}

.item span {
  font-size: 15px;
}

.item.collapsed {
  width: 52px;
  justify-content: center;
  padding: 9px;
}
</style>
