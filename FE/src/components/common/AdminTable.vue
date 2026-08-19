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
    hideToolbar: { type: Boolean, default: false },
    totalCount: { type: Number, default: 0 }
});

const emit = defineEmits(['add', 'export', 'import', 'downloadTemplate']);
</script>

<template>
    <div v-bind="$attrs" class="admin-table-main-root">
        <v-card class="admin-table-container elevation-0" elevation="0">
            <!-- Toolbar Area -->
            <div v-if="!hideToolbar" class="table-toolbar d-flex align-center justify-space-between px-3 py-2">
                <div class="d-flex align-center">
                    <LayoutGridIcon size="18" class="text-primary mr-2" />
                    <h3 class="text-subtitle-1 font-weight-bold text-black tracking-tight" style="font-size: 15px !important">{{ title }}</h3>
                </div>
                <div class="d-flex align-center flex-wrap justify-end admin-toolbar-actions">
                    <v-btn
                        v-if="showTemplateButton"
                        prepend-icon="mdi-download"
                        variant="flat"
                        class="admin-btn-secondary"
                        @click="$emit('downloadTemplate')"
                    >
                        Tải mẫu
                    </v-btn>
                    <v-btn
                        v-if="showImportButton"
                        prepend-icon="mdi-upload"
                        variant="flat"
                        class="admin-btn-secondary"
                        @click="$emit('import')"
                    >
                        Nhập Excel
                    </v-btn>
                    <v-btn
                        v-if="showExportButton"
                        prepend-icon="mdi-microsoft-excel"
                        variant="flat"
                        class="admin-btn-export"
                        @click="$emit('export')"
                    >
                        {{ exportButtonText }}
                    </v-btn>
                    <slot name="extra-actions"></slot>
                    <v-btn
                        v-if="showAddButton"
                        prepend-icon="mdi-plus"
                        variant="flat"
                        color="primary"
                        class="add-btn-primary"
                        @click="$emit('add', $event)"
                    >
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
                                <th
                                    v-for="(header, idx) in headers"
                                    :key="idx"
                                    :style="{ width: header.width || 'auto' }"
                                    :class="[
                                        'header-cell',
                                        header.align === 'start' ? 'text-left' : header.align === 'end' ? 'text-right' : 'text-center',
                                        header.class
                                    ]"
                                >
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
                </table>

                <!-- Render empty and loading states outside the table -->
                <div
                    v-if="loading || items.length === 0"
                    class="empty-state-wrapper py-12 w-100 d-flex flex-column align-center justify-center border-t"
                >
                    <div v-if="loading" class="d-flex flex-column align-center justify-center w-100 py-6">
                        <v-progress-circular indeterminate color="primary" size="42" width="4" class="mb-3" />
                        <span class="text-subtitle-2 font-weight-medium text-slate-500">Đang tải dữ liệu...</span>
                    </div>
                    <div v-else class="d-flex flex-column align-center justify-center py-6 px-4 w-100 animate-fade-in">
                        <div
                            class="empty-state-icon-box d-flex align-center justify-center rounded-circle mb-3"
                            style="width: 68px; height: 68px; background: rgba(241, 245, 249, 0.8); border: 1.5px dashed #cbd5e1"
                        >
                            <v-icon :icon="emptyIcon || 'mdi-database-search-outline'" size="32" style="color: #94a3b8 !important" />
                        </div>
                        <span
                            class="text-slate-600 text-center font-weight-medium"
                            style="font-size: 14px !important; line-height: 1.5; max-width: 420px"
                        >
                            {{ emptyText }}
                        </span>
                    </div>
                </div>
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
