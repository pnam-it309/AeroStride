<script setup>
import { computed } from 'vue';
import { LayoutGridIcon } from 'vue-tabler-icons';
import TableEmptyState from './TableEmptyState.vue';

const props = defineProps({
    title: { type: String, default: 'Danh sách' },
    headers: { type: Array, default: () => [] },
    items: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    addButtonText: { type: String, default: 'Thêm mới' },
    showAddButton: { type: Boolean, default: true },
    showExportButton: { type: Boolean, default: false },
    showImportButton: { type: Boolean, default: false },
    showTemplateButton: { type: Boolean, default: false },
    exportButtonText: { type: String, default: 'Tải Excel' },
    emptyText: { type: String, default: 'Không có dữ liệu phù hợp để hiển thị' },
    emptyIcon: { type: String, default: 'mdi-package-variant' },
    selectable: { type: Boolean, default: false },
    hideToolbar: { type: Boolean, default: false }
});

const emit = defineEmits(['add', 'export', 'import', 'downloadTemplate']);

const tableKey = computed(() => {
    return props.headers.map(h => `${h.text || h}-${h.width || ''}-${h.align || ''}`).join('|');
});
</script>

<template>
    <div v-bind="$attrs" class="admin-table-main-root">
        <v-card class="admin-table-container shadow-none" elevation="0">
            <!-- Toolbar Area -->
            <div v-if="!hideToolbar" class="table-toolbar d-flex align-center justify-space-between pa-3">
                <div class="d-flex align-center">
                    <LayoutGridIcon size="20" class="text-primary mr-3" />
                    <h3 class="text-h6 font-weight-bold text-black tracking-tight">{{ title }}</h3>
                </div>
                <div class="d-flex align-center flex-wrap justify-end admin-toolbar-actions">
                    <v-btn v-if="showTemplateButton" prepend-icon="mdi-download" variant="flat" class="admin-btn-secondary"
                        @click="$emit('downloadTemplate')">
                        Tải mẫu
                    </v-btn>
                    <v-btn v-if="showImportButton" prepend-icon="mdi-upload" variant="flat" class="admin-btn-secondary"
                        @click="$emit('import')">
                        Nhập Excel
                    </v-btn>
                    <v-btn v-if="showExportButton" prepend-icon="mdi-microsoft-excel" variant="flat"
                        class="admin-btn-export" @click="$emit('export')">
                        {{ exportButtonText }}
                    </v-btn>
                    <slot name="extra-actions"></slot>
                    <v-btn v-if="showAddButton" prepend-icon="mdi-plus" variant="flat" color="primary"
                        class="add-btn-primary" @click="$emit('add', $event)">
                        {{ addButtonText }}
                    </v-btn>
                </div>
            </div>

            <div v-if="$slots.top" class="table-top">
                <slot name="top"></slot>
            </div>

            <!-- Main Table Table -->
            <div class="table-wrapper">
                <table :key="tableKey" class="native-admin-table">
                    <thead>
                        <slot name="headers">
                            <tr>
                                <th v-if="selectable" class="header-cell" style="width: 50px;">
                                    <slot name="header-select"></slot>
                                </th>
                                <th v-for="(header, idx) in headers" :key="idx"
                                    :style="{ minWidth: header.width || 'auto', width: header.width || 'auto' }"
                                    :class="['header-cell', header.align ? 'text-' + header.align : 'text-center']"
                                    style="white-space: nowrap;">
                                    {{ header.text || header }}
                                </th>
                            </tr>
                        </slot>
                    </thead>
                    <tbody :class="{ 'is-loading': loading }">
                        <template v-if="items.length > 0">
                            <template v-for="(item, index) in items" :key="item.id ?? index">
                                <slot name="row" :item="item" :index="index"></slot>
                            </template>
                        </template>
                        <TableEmptyState v-else-if="!loading" :colspan="headers.length || 20" :icon="emptyIcon" :text="emptyText" />
                    </tbody>
                </table>

                <!-- Smooth Loading Overlay -->
                <Transition name="fade">
                    <div v-if="loading" class="table-loading-overlay d-flex flex-column align-center justify-center">
                        <v-progress-circular indeterminate color="primary" size="48" width="5" class="mb-4" />
                        <span class="text-subtitle-1 font-weight-bold text-slate-600">Đang tải dữ liệu...</span>
                    </div>
                </Transition>
            </div>
        </v-card>

        <!-- Pagination Area - Moved outside for separate card style -->
        <div v-if="$slots.pagination" class="pagination-footer mt-4">
            <slot name="pagination"></slot>
        </div>
    </div>
</template>

<style scoped>
.table-wrapper {
    position: relative;
    min-height: 200px;
}

.table-loading-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(2px);
    z-index: 50;
    border-radius: 0 0 16px 16px;
}

/* Maintain opacity for rows during loading for a softer look */
.is-loading {
    opacity: 0.4;
    filter: grayscale(0.2);
    transition: all 0.3s ease;
    pointer-events: none;
}

/* Fade Transition */
.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.4s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}
</style>
