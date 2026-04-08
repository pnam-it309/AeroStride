<script setup>
import { defineProps, defineEmits, computed, nextTick } from 'vue';

const props = defineProps({
  modelValue: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  totalPages: { type: Number, default: 1 },
  totalElements: { type: Number, default: 0 },
  currentSize: { type: Number, default: 0 }
});

const emit = defineEmits(['update:modelValue', 'update:pageSize', 'update:page-size', 'change']);

const page = computed({
  get: () => props.modelValue,
  set: async (val) => {
    emit('update:modelValue', val);
    await nextTick();
    emit('change');
  }
});

const size = computed({
  get: () => props.pageSize,
  set: async (val) => {
    emit('update:pageSize', val);
    emit('update:page-size', val);
    emit('update:modelValue', 1); // Reset to page 1
    await nextTick();
    emit('change');
  }
});

const startItem = computed(() => (page.value - 1) * props.pageSize + 1);
const endItem = computed(() => startItem.value + props.currentSize - 1);

const pageSizes = [5, 10, 15, 20];
</script>

<template>
  <div class="d-flex align-center justify-space-between pt-4 px-2 border-t">
    <div class="text-caption font-weight-bold text-medium-emphasis">
      Hiển thị {{ startItem }}-{{ endItem }} trên tổng {{ totalElements }} bản ghi
    </div>
    
    <div class="d-flex align-center gap-4">
      <!-- Page Size Selector -->
      <div class="d-flex align-center mr-6">
        <span class="text-caption font-weight-black mr-2 text-uppercase">Số dòng:</span>
        <select 
          v-model.number="size" 
          class="page-size-select font-weight-black"
        >
          <option v-for="s in pageSizes" :key="s" :value="s">{{ s }} dòng</option>
        </select>
      </div>

      <!-- Pagination Buttons -->
      <v-pagination
        v-model="page"
        :length="totalPages"
        :total-visible="5"
        density="comfortable"
        variant="flat"
        color="primary"
        class="custom-pagination"
        active-color="primary"
      ></v-pagination>
    </div>
  </div>
</template>

<style scoped>
.page-size-select {
  border: 2px solid #e2e8f0;
  padding: 4px 8px;
  border-radius: 4px; /* BO TRÒN NHẸ */
  font-size: 13px;
  outline: none;
  cursor: pointer;
  background: white;
  transition: all 0.2s;
}

.page-size-select:focus {
  border-color: #1e293b;
}

/* Custom Vuetify Pagination to make it SQUARE-ISH */
:deep(.v-pagination__item), 
:deep(.v-pagination__next), 
:deep(.v-pagination__prev) {
  border-radius: 4px !important; /* BO TRÒN NHẸ */
  border: 1px solid #e2e8f0 !important;
  margin: 0 2px !important;
  font-weight: 800 !important;
}

:deep(.v-pagination__item--is-active) {
  border-color: var(--v-primary-base) !important;
}
</style>
