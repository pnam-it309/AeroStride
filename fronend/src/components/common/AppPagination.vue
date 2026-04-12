<template>
  <div class="pager" v-if="total > 0">
    <div class="pages rightMost">
      <div class="perPage">
        <span>Hiển thị</span>
        <select :value="pageSize" @change="setPageSize($event.target.value)">
          <option v-for="size in pageSizeOptions" :key="size" :value="size">
            {{ size }} dòng
          </option>
        </select>
      </div>

      <button class="nav" :disabled="page <= 1" @click="setPage(page - 1)">
        <Icon name="chevron-left" />
      </button>

      <button class="num active" type="button">
        {{ page }}
      </button>

      <button
        class="nav"
        :disabled="page >= pageCount"
        @click="setPage(page + 1)"
      >
        <Icon name="chevron-right" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import Icon from "@/components/common/Icon.vue";

const props = defineProps({
  page: { type: Number, default: 1 },
  total: { type: Number, default: 0 },
  pageSize: { type: Number, default: 10 },
  pageSizeOptions: { type: Array, default: () => [5, 10, 20, 50] },
});

const emit = defineEmits(["update:page", "update:pageSize"]);

const pageCount = computed(() =>
  Math.max(1, Math.ceil(props.total / props.pageSize)),
);

function setPage(next) {
  const safe = Math.min(pageCount.value, Math.max(1, next));
  emit("update:page", safe);
}

function setPageSize(next) {
  const safe = Number(next) || 10;
  emit("update:pageSize", safe);
  emit("update:page", 1);
}
</script>

<style scoped>
.pager {
  padding-top: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.perPage {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #6b7280;
  font-size: 12px;
}

.perPage select {
  height: 28px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  padding: 0 6px;
  font-size: 12px;
  color: #374151;
}

.pages {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.rightMost {
  margin-left: 0;
}

.nav,
.num {
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  color: #4b5563;
  font-size: 12px;
  font-weight: 600;
  display: inline-grid;
  place-items: center;
}

.nav :deep(svg) {
  width: 12px;
  height: 12px;
}

.num.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}

.num:not(.dots):hover,
.nav:hover:not(:disabled) {
  background: #f8fafc;
}

button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
