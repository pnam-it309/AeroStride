<script setup>
import { EditIcon } from 'vue-tabler-icons';
import { AdminTable, AdminPagination } from '@/components/common';
import { isActiveStatus, getStatusLabel, getStatusColor } from '@/utils/statusUtils';
import { formatDateTime } from '@/utils/formatters';

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
            :addButtonText="`Tạo mới`"
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
                    class="equal-tabs admin-tabs" 
                    grow 
                    height="54"
                >
                    <v-tab v-for="tabItem in tabs" :key="tabItem.value" :value="tabItem.value" class="text-none font-weight-medium px-4 tab-item">
                        <v-icon start size="16">{{ tabItem.icon }}</v-icon>
                        {{ tabItem.title }}
                    </v-tab>
                </v-tabs>
            </template>

            <template #row="{ item, index }">
                <tr class="data-row">
                    <td class="data-cell">
                        <div class="text-slate-500">{{ (pagination.page - 1) * pagination.size + index + 1 }}</div>
                    </td>
                    <td class="data-cell">
                        <div class="text-slate-600 text-truncate" :title="getItemCode(item)">{{ getItemCode(item) }}</div>
                    </td>
                    <td class="data-cell">
                        <div class="text-slate-600 text-truncate" :title="getItemName(item)">{{ getItemName(item) }}</div>
                    </td>
                    <td class="data-cell">
                        <v-chip
                            variant="flat"
                            :class="['status-chip', isActiveStatus(item.trangThai) ? 'status-chip-active' : 'status-chip-inactive']"
                        >
                            {{ getStatusLabel(item.trangThai) }}
                        </v-chip>
                    </td>
                    <td class="data-cell">
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
                                <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                            </v-btn>

                            <!-- Switch -->
                            <div class="switch-wrapper d-flex align-center">
                                <v-switch
                                    :model-value="isActiveStatus(item.trangThai)"
                                    color="primary"
                                    hide-details
                                    density="compact"
                                    class="tight-switch action-switch"
                                    @click.prevent.stop="emit('change-status', item)"
                                />
                                <v-tooltip activator="parent" location="top">Chuyển đổi trạng thái</v-tooltip>
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
/* Scoped styles removed in favor of global _admin-common.scss */
</style>

