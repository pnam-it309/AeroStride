<template>
  <span class="badge" :class="toneClass">{{ normalizedText }}</span>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  text: { type: String, default: "-" },
  tone: { type: String, default: "gray" },
});

const statusLabelMap = {
  ACTIVE: "Hoạt động",
  INACTIVE: "Ngừng hoạt động",
};

const normalizedText = computed(() => {
  const value = String(props.text ?? "").trim();
  const key = value.toUpperCase();

  if (statusLabelMap[key]) return statusLabelMap[key];

  return value || "-";
});

const toneClass = computed(() => {
  const map = {
    green: "badge-green",
    yellow: "badge-yellow",
    orange: "badge-orange",
    teal: "badge-teal",
    red: "badge-red",
    blue: "badge-blue",
    inactive: "badge-inactive",
    gray: "badge-gray",
  };

  const key = String(props.tone || "gray").toLowerCase();
  return map[key] || map.gray;
});
</script>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  line-height: 1;
  font-weight: 500;
  letter-spacing: 0;
  white-space: nowrap;
}

.badge-green {
  color: #157a72;
  background: #e8fbf8;
  border-color: #d4f4ef;
}

.badge-yellow {
  color: #b57612;
  background: #fff7df;
  border-color: #f4e5b7;
}

.badge-orange {
  color: #bf5b12;
  background: #fff1e3;
  border-color: #f6dcc2;
}

.badge-teal {
  color: #157a72;
  background: #edfdfa;
  border-color: #d7f6f0;
}

.badge-red {
  color: #c23939;
  background: #fff0f0;
  border-color: #f6d8d8;
}

.badge-blue {
  color: #3559a5;
  background: #f1f5ff;
  border-color: #dce4fb;
}

.badge-inactive {
  color: #8b97aa;
  background: #f7f9fd;
  border-color: #e8edf4;
}

.badge-gray {
  color: #738095;
  background: #f8fafc;
  border-color: #e8edf4;
}
</style>
