<script setup>
import { EditIcon } from 'vue-tabler-icons';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';

const props = defineProps({
    title: String,
    headers: Array,
    items: Array,
    loading: Boolean,
    pagination: Object,
    tab: String,
    tabs: Array,
    meta: Object
});

const emit = defineEmits(['add', 'edit', 'change-status', 'load-items', 'update:tab', 'update:page', 'update:size']);

const isActiveStatus = (status) => {
    if (status === null || status === undefined) return false;
    if (typeof status === 'number') return status === 0;
    const normalized = String(status).toUpperCase();
    return normalized === 'DANG_HOAT_DONG' || normalized === 'ACTIVE' || normalized === 'HOAT_DONG' || normalized === '0';
};

const getDisplayStatus = (status) => (isActiveStatus(status) ? 'Hoạt động' : 'Ngừng hoạt động');

const formatDateTime = (value) => {
    if (!value) return '--';
    const date = typeof value === 'number' ? new Date(value) : new Date(String(value));
    if (Number.isNaN(date.getTime())) return '--';
    const d = String(date.getDate()).padStart(2, '0');
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const y = date.getFullYear();
    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    return `${d}/${m}/${y} ${hh}:${mm}`;
};

const pickFirst = (item, keys, fallback = '--') => {
    for (const key of keys) {
        const val = item?.[key];
        if (val !== null && val !== undefined && String(val).trim() !== '') return val;
    }
    return fallback;
};

const getItemCode = (item) => String(pickFirst(item, props.meta.codeKeys));
const getItemName = (item) => String(pickFirst(item, props.meta.nameKeys));
const getCreatedAt = (item) => item?.ngayTao ?? item?.createdAt ?? item?.ngay_tao ?? null;
</script>

<template>
    <div class="d-flex flex-column h-100 overflow-hidden">
        <AdminTable
            :title="title"
            :addButtonText="`Thêm ${title.toLowerCase().replace('danh sách ', '')}`"
            :headers="headers"
            :items="items"
            :total-count="pagination.totalElements"
            :loading="loading"
            @add="emit('add')"
            class="h-100"
        >
            <template #top>
                <v-tabs 
                    :model-value="tab"
                    @update:model-value="emit('update:tab', $event)"
                    color="primary" 
                    class="attribute-tabs" 
                    grow 
                    density="compact"
                >
                    <v-tab v-for="tabItem in tabs" :key="tabItem.value" :value="tabItem.value" class="text-none font-weight-bold attr-tab-item">
                        <v-icon start size="14">{{ tabItem.icon }}</v-icon>
                        {{ tabItem.title }}
                    </v-tab>
                </v-tabs>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell text-center">{{ (pagination.page - 1) * pagination.size + index + 1 }}</td>
                    <td class="data-cell text-center">
                        <div class="font-weight-bold text-slate-900" style="font-size: 13px;">{{ getItemCode(item) }}</div>
                    </td>
                    <td class="data-cell text-center">
                        <div class="text-slate-800 font-weight-medium">{{ getItemName(item) }}</div>
                    </td>
                    <td class="data-cell text-center">
                        <v-chip
                            variant="tonal"
                            size="x-small"
                            class="font-weight-bold"
                            :color="isActiveStatus(item.trangThai) ? 'success' : 'warning'"
                            >{{ getDisplayStatus(item.trangThai) }}</v-chip
                        >
                    </td>
                    <td class="data-cell text-center text-slate-500" style="font-size: 11px;">{{ formatDateTime(getCreatedAt(item)) }}</td>
                    <td class="data-cell text-center">
                        <div class="d-flex align-center justify-center action-controls">
                            <!-- Edit Button -->
                            <v-btn
                                icon
                                variant="text"
                                size="28"
                                color="slate-700"
                                class="rounded-lg action-icon-btn"
                                @click.stop="emit('edit', item)"
                            >
                                <EditIcon size="15" />
                                <v-tooltip activator="parent" location="top">Sửa</v-tooltip>
                            </v-btn>

                            <!-- Switch -->
                            <div class="switch-wrapper d-flex align-center">
                                <v-switch
                                    :model-value="isActiveStatus(item.trangThai)"
                                    color="primary"
                                    hide-details
                                    density="compact"
                                    class="action-switch-compact ms-0"
                                    @click.prevent.stop="emit('change-status', item)"
                                />
                                <v-tooltip activator="parent" location="top">Trạng thái</v-tooltip>
                            </div>
                        </div>
                    </td>
                </tr>
            </template>

            <template #pagination>
                <AdminPagination
                    v-model="pagination.page"
                    :page-size="pagination.size"
                    :total-pages="pagination.totalPages"
                    :total-elements="pagination.totalElements"
                    :current-size="items.length"
                    @update:pageSize="emit('update:size', $event)"
                    @change="emit('load-items')"
                />
            </template>
        </AdminTable>
    </div>
</template>

<style scoped>
:deep(.data-cell) {
    padding: 10px 8px !important;
    font-size: 13px !important;
    font-weight: 600 !important;
    text-align: center !important;
    vertical-align: middle !important;
}

:deep(.header-cell) {
    padding: 12px 8px !important;
    font-size: 13px !important;
    font-weight: 700 !important;
    text-align: center !important;
    border-bottom: 2px solid #e2e8f0 !important;
}

:deep(.attribute-tabs) {
    border-bottom: 1px solid #e2e8f0;
}

:deep(.attribute-tabs .v-tab) {
    font-size: 13px !important;
    font-weight: 700 !important;
    min-height: 48px !important;
    text-transform: none !important;
    letter-spacing: 0 !important;
}

.action-switch-compact {
    display: inline-flex;
    margin: 0 !important;
}

:deep(.action-switch-compact .v-selection-control) {
    min-height: 0 !important;
}

:deep(.v-selection-control__wrapper) {
    width: 32px !important;
    height: 18px !important;
    margin: 0 !important;
}

:deep(.action-switch-compact .v-switch__track) {
    background: #ffffff !important;
    border: 1px solid #cbd5e1 !important;
    opacity: 1 !important;
    min-height: 18px !important;
    max-height: 18px !important;
    width: 32px !important;
    border-radius: 99px !important;
}

:deep(.action-switch-compact .v-selection-control--dirty .v-switch__track) {
    background: #d9e6fb !important;
    border-color: #d9e6fb !important;
    opacity: 1 !important;
}

:deep(.action-switch-compact .v-switch__thumb) {
    background: #94a3b8 !important;
    width: 14px !important;
    height: 14px !important;
    box-shadow: none !important;
}

:deep(.action-switch-compact .v-selection-control--dirty .v-switch__thumb) {
    background: #1e3a8a !important;
}

.action-controls {
    gap: 8px;
}

:deep(.action-icon-btn) {
    background: transparent !important;
    border: none !important;
    outline: none !important;
    box-shadow: none !important;
    min-width: 28px !important;
    width: 28px !important;
    height: 28px !important;
    padding: 0 !important;
    transition: all 0.2s ease-in-out;
}

:deep(.action-icon-btn .v-btn__overlay),
:deep(.action-icon-btn .v-btn__underlay),
:deep(.action-icon-btn .v-ripple__container) {
    display: none !important;
}

.action-icon-btn:hover {
    background-color: #f1f5f9 !important;
    transform: translateY(-2px);
}
</style>
