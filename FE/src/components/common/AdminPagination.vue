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
const shouldShowPagination = computed(() => props.totalElements > 5);
const hasPrev = computed(() => page.value > 1);
const hasNext = computed(() => page.value < props.totalPages);

const pageSizes = [5, 10, 15, 20];

const goPrev = async () => {
    if (!hasPrev.value) return;
    page.value = page.value - 1;
};

const goNext = async () => {
    if (!hasNext.value) return;
    page.value = page.value + 1;
};
</script>

<template>
    <div class="pagination-shell d-flex align-center justify-space-between pt-2 px-1 border-t">
        <div class="pagination-summary text-medium-emphasis">
            Đang hiển thị {{ currentSize }} bản ghi trong tổng {{ totalElements }} kết quả
        </div>

        <div v-if="shouldShowPagination" class="d-flex align-center flex-wrap justify-end gap-2">
            <!-- Page Size Selector -->
            <div class="d-flex align-center mr-1">
                <span class="pagination-label mr-2">Hiển thị</span>
                <select v-model.number="size" class="page-size-select">
                    <option v-for="s in pageSizes" :key="s" :value="s">{{ s }} dòng</option>
                </select>
            </div>

            <div class="mini-pager" aria-label="Pagination">
                <button type="button" class="pager-btn" :disabled="!hasPrev" @click="goPrev">&lt;</button>
                <button type="button" class="pager-page" aria-current="page">{{ page }}</button>
                <button type="button" class="pager-btn" :disabled="!hasNext" @click="goNext">&gt;</button>
            </div>
        </div>
    </div>
</template>

<style scoped>
.pagination-shell {
    gap: 12px;
}

.pagination-summary {
    font-size: 12px !important;
}

.pagination-label {
    font-size: 12px;
    color: #475569;
    font-weight: 500;
}

.page-size-select {
    border: 1px solid #cbd5e1;
    padding: 4px 10px;
    border-radius: 9px;
    font-size: 13px;
    font-weight: 500;
    line-height: 1.2;
    outline: none;
    cursor: pointer;
    background: #ffffff;
    color: #0f172a;
    appearance: auto;
    transition: all 0.2s;
}

.page-size-select:focus {
    border-color: #94a3b8;
}

.mini-pager {
    display: inline-flex;
    align-items: center;
    gap: 6px;
}

.pager-btn,
.pager-page {
    width: 31px;
    min-width: 31px;
    height: 31px;
    border-radius: 8px;
    border: 1px solid #dbe4ef;
    font-size: 12px;
    font-weight: 700;
    line-height: 1;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: #ffffff;
    color: #334155;
}

.pager-page {
    border-color: #1e3a8a;
    background-color: #1e3a8a;
    color: #ffffff !important;
}

.pager-btn {
    background: #f8fafc;
    color: #64748b;
    cursor: pointer;
}

.pager-btn:disabled {
    color: #cbd5e1;
    cursor: not-allowed;
}
</style>
