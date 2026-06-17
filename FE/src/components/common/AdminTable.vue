<script setup>
import { LayoutGridIcon } from 'vue-tabler-icons';

defineProps({
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
                    <v-btn v-if="showTemplateButton" prepend-icon="mdi-download" variant="flat"
                        class="admin-btn-secondary" @click="$emit('downloadTemplate')">
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

            <!-- Top Slot Area -->
            <div v-if="$slots.top" class="table-top">
                <slot name="top"></slot>

            </div>

            <!-- Main Table -->
            <div class="table-wrapper">
                <table class="native-admin-table">
                    <thead>
                        <slot name="headers">
                            <tr>
                                <th v-if="selectable" style="width: 50px" class="header-cell text-center px-0">
                                    <slot name="header-select"></slot>
                                </th>
                                <th v-for="(header, idx) in headers" :key="idx"
                                    :style="{ width: header.width || 'auto' }" :class="['header-cell',
                                        header.align === 'start' ? 'text-left' :
                                            header.align === 'end' ? 'text-right' : 'text-center']">
                                    {{ header.text || header }}
                                </th>
                            </tr>
                        </slot>
                    </thead>
                    <tbody v-if="!loading && items.length > 0">
                        <template v-for="(item, index) in items" :key="item.id ?? index">
                            <slot name="row" :item="item" :index="index"></slot>
                        </template>
                    </tbody>
                    <tbody v-else>
                        <tr>
                            <td :colspan="headers.length || 20" class="empty-state py-16 text-center">
                                <div v-if="loading" class="d-flex flex-column align-center">
                                    <v-progress-circular indeterminate color="primary" size="48" width="6"
                                        class="mb-4" />
                                    <span class="text-subtitle-1 font-weight-bold text-medium-emphasis">Đang tải dữ liệu...</span>
                                </div>
                                <div v-else
                                    class="d-flex flex-column align-center py-12 bg-slate-50/30 rounded-lg mx-4 my-2">
                                    <v-icon :icon="emptyIcon" size="48"
                                        style="color: #94a3b8 !important; opacity: 0.6;" class="mb-3" />
                                    <span class="text-slate-500"
                                        style="font-size: 14px !important; font-weight: 400 !important;">{{
                                            emptyText }}</span>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- Pagination Area -->
            <div class="pagination-footer">
                <slot name="pagination"></slot>
            </div>
        </v-card>
    </div>
</template>

<style scoped>
/* Core styles are now centralized in _admin-common.scss */
</style>
