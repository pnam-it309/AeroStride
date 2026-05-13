<script setup>
import { computed, nextTick } from 'vue';

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
        emit('update:modelValue', 1);
        await nextTick();
        emit('change');
    }
});

const pageSizes = [5, 10, 20, 50, 100];

const startItem = computed(() => (page.value - 1) * props.pageSize + 1);
const endItem = computed(() => Math.min(startItem.value + props.currentSize - 1, props.totalElements));
const shouldShowPagination = computed(() => props.totalElements > 0);
const hasPrev = computed(() => page.value > 1);
const hasNext = computed(() => page.value < props.totalPages);

const visiblePages = computed(() => {
    const total = props.totalPages || 1;
    const current = page.value;
    const maxVisible = 5;
    let start = Math.max(1, current - Math.floor(maxVisible / 2));
    let end = Math.min(total, start + maxVisible - 1);
    if (end - start + 1 < maxVisible) start = Math.max(1, end - maxVisible + 1);
    const pages = [];
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
});

const goPrev = async () => { if (hasPrev.value) page.value--; };
const goNext = async () => { if (hasNext.value) page.value++; };
const goToPage = async (p) => { if (p >= 1 && p <= props.totalPages) page.value = p; };
</script>

<template>
    <div class="pagination-shell d-flex align-center justify-space-between">
        <div class="pagination-summary text-medium-emphasis">
            Đang hiển thị <strong>{{ startItem }}-{{ endItem }}</strong> trong tổng số <strong>{{ totalElements }}</strong> kết quả
        </div>

        <div v-if="shouldShowPagination" class="d-flex align-center flex-wrap justify-end gap-2">
            <div class="d-flex align-center mr-2">
                <span class="pagination-label mr-2">Hiển thị</span>
                <select v-model.number="size" class="page-size-select">
                    <option v-for="s in pageSizes" :key="s" :value="s">{{ s }} dòng</option>
                </select>
            </div>

            <div class="mini-pager" aria-label="Pagination">
                <button type="button" class="pager-btn" :disabled="!hasPrev" @click="goPrev">&lt;</button>
                <template v-if="visiblePages[0] > 1">
                    <button type="button" class="pager-btn" @click="goToPage(1)">1</button>
                    <span v-if="visiblePages[0] > 2" class="pager-ellipsis">...</span>
                </template>
                <button v-for="p in visiblePages" :key="p" type="button" :class="['pager-btn', { 'pager-active': p === page }]" @click="goToPage(p)">{{ p }}</button>
                <template v-if="visiblePages[visiblePages.length - 1] < totalPages">
                    <span v-if="visiblePages[visiblePages.length - 1] < totalPages - 1" class="pager-ellipsis">...</span>
                    <button type="button" class="pager-btn" @click="goToPage(totalPages)">{{ totalPages }}</button>
                </template>
                <button type="button" class="pager-btn" :disabled="!hasNext" @click="goNext">&gt;</button>
            </div>
        </div>
    </div>
</template>

<style scoped>
.pagination-shell {
    width: 100%;
    gap: 12px;
}

.pagination-summary {
    font-size: 13px !important;
}
.pagination-summary strong {
    color: #000;
    font-weight: 700;
}
.pagination-label {
    font-size: 13px !important;
    color: #000;
    font-weight: 500;
}
.page-size-select {
    border: 1px solid #cbd5e1;
    padding: 2px 8px;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 600;
    line-height: 1.2;
    outline: none;
    cursor: pointer;
    background: #ffffff;
    color: #000;
    appearance: auto;
}
.mini-pager {
    display: inline-flex;
    align-items: center;
    gap: 4px;
}
.pager-btn {
    min-width: 32px;
    height: 32px;
    border-radius: 8px;
    border: 1px solid #dbe4ef;
    font-size: 13px !important;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: #ffffff;
    color: #000;
    cursor: pointer;
}
.pager-active {
    border-color: #1e3a8a;
    background-color: #1e3a8a;
    color: #ffffff !important;
}
.pager-btn:disabled {
    color: #cbd5e1;
    cursor: not-allowed;
    background: #f8fafc;
}
.pager-ellipsis {
    color: #94a3b8;
    font-size: 13px;
    padding: 0 2px;
}
</style>
