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
    exportButtonText: { type: String, default: 'Xuất Excel' },
    emptyText: { type: String, default: 'Không có dữ liệu hiển thị' },
    emptyIcon: { type: String, default: 'mdi-package-variant' }
});

const emit = defineEmits(['add', 'export', 'import', 'downloadTemplate']);
</script>

<template>
    <v-card class="admin-table-container shadow-none border" elevation="0">
        <!-- Toolbar Area -->
        <div class="table-toolbar d-flex align-center justify-space-between pa-3 border-b">
            <div class="d-flex align-center">
                <LayoutGridIcon size="20" class="text-primary mr-3" />
                <h3 class="text-h6 font-weight-bold text-dark tracking-tight">{{ title }}</h3>
            </div>
            <div class="d-flex align-center flex-wrap justify-end gap-2">
                <v-btn
                    v-if="showTemplateButton"
                    prepend-icon="mdi-download"
                    variant="flat"
                    color="light-blue-lighten-5"
                    height="40"
                    class="px-4 font-weight-bold text-light-blue-darken-2"
                    @click="$emit('downloadTemplate')"
                >
                    Tải mẫu
                </v-btn>
                <v-btn
                    v-if="showImportButton"
                    prepend-icon="mdi-upload"
                    variant="flat"
                    color="success"
                    height="40"
                    class="px-4 font-weight-bold"
                    @click="$emit('import')"
                >
                    Nhập Excel
                </v-btn>
                <v-btn
                    v-if="showExportButton"
                    prepend-icon="mdi-microsoft-excel"
                    variant="tonal"
                    color="success"
                    height="40"
                    class="px-4 font-weight-bold"
                    @click="$emit('export')"
                >
                    {{ exportButtonText }}
                </v-btn>
                <v-btn
                    v-if="showAddButton"
                    prepend-icon="mdi-plus"
                    variant="flat"
                    color="primary"
                    height="40"
                    class="px-6 font-weight-bold add-btn-primary"
                    @click="$emit('add')"
                >
                    {{ addButtonText }}
                </v-btn>
            </div>
        </div>

        <div v-if="$slots.top" class="table-top">
            <slot name="top"></slot>
        </div>

        <!-- Main Table Table -->
        <div class="table-wrapper">
            <table class="native-admin-table">
                <thead>
                    <tr>
                        <th
                            v-for="(header, idx) in headers"
                            :key="idx"
                            :style="{ textAlign: header.align || 'left', width: header.width || 'auto' }"
                            class="header-cell"
                        >
                            {{ header.text || header }}
                        </th>
                    </tr>
                </thead>
                <tbody v-if="!loading && items.length > 0">
                    <slot name="row" v-for="(item, index) in items" :key="item.id ?? index" :item="item" :index="index"></slot>
                </tbody>
                <tbody v-else>
                    <tr>
                        <td :colspan="headers.length" class="empty-state py-16 text-center">
                            <div v-if="loading" class="d-flex flex-column align-center">
                                <v-progress-circular indeterminate color="primary" size="48" width="6" class="mb-4" />
                                <span class="text-subtitle-1 font-weight-bold text-medium-emphasis">Đang tải dữ liệu...</span>
                            </div>
                            <div v-else class="d-flex flex-column align-center">
                                <v-icon :icon="emptyIcon" size="64" color="grey-lighten-2" class="mb-2" />
                                <span class="text-subtitle-1 font-weight-bold text-medium-emphasis">{{ emptyText }}</span>
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
</template>

<style scoped>
.admin-table-container {
    background: white;
    border: 1px solid #dbe4ef !important;
    border-radius: 8px !important;
    overflow: hidden;
    box-shadow: none !important;
}

.table-toolbar {
    background: #ffffff;
}

.table-wrapper {
    overflow-x: auto;
    background: white;
}

.table-top {
    background: #ffffff;
    border-bottom: 1px solid #e2e8f0;
}

.native-admin-table {
    width: 100%;
    border-collapse: collapse;
    table-layout: fixed;
}

.header-cell {
    z-index: 10;
    background: #f8fafc;
    color: #0f172a; /* ĐEN ĐẬM */
    padding: 14px 12px;
    font-size: 0.875rem;
    font-weight: 600;
    text-transform: none;
    border-bottom: 2px solid #e2e8f0;
    letter-spacing: 0.05em;
    white-space: nowrap;
}

:deep(.data-row) {
    transition: all 0.2s ease;
    border-bottom: 1px solid #f1f5f9;
}

:deep(.data-row:hover) {
    background-color: #f8fafc;
}

:deep(.data-cell) {
    padding: 12px 10px;
    font-size: 14px;
    color: #1e293b;
    vertical-align: middle;
}

:deep(.v-btn) {
    border-radius: 10px !important;
}

/* Pagination container spacing */
.pagination-footer {
    background: #fdfdfd;
    padding: 0 14px 12px 14px;
}

:deep(.tight-switch) {
    flex: none !important;
    width: 40px !important;
    margin: 0 !important;
    display: inline-flex !important;
    transform: scale(0.85);
}

:deep(.tight-switch .v-selection-control) {
    min-height: unset !important;
}

:deep(.v-btn.rounded-0) {
    flex: none !important;
}

:deep(.add-btn-primary) {
    background-color: #1e3a8a !important;
    color: #ffffff !important;
    border-color: #1e3a8a !important;
}

:deep(.add-btn-primary:hover),
:deep(.add-btn-primary:focus),
:deep(.add-btn-primary:active) {
    background-color: #1d4ed8 !important;
    border-color: #1d4ed8 !important;
}
</style>
