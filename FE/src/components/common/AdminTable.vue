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
    display: flex; /* Kích hoạt flexbox */
    flex-direction: column; /* Sắp xếp theo chiều dọc */
    height: 100%; /* Chiếm hết chiều cao cha */
    min-height: 450px; /* Chiều cao tối thiểu để không bị quá lùn */
    background: white;
    border: 1px solid #e2e8f0 !important;
    border-radius: 12px !important;
    overflow: hidden; /* Quan trọng để không bị tràn footer */
    box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05) !important;
}

.table-toolbar {
    flex: none; /* Cố định kích thước, không bị co */
    background: #ffffff;
    padding: 14px 20px !important;
}

.table-top {
    flex: none; /* Cố định phần filter bên trên (nếu có) */
    background: #ffffff;
    border-bottom: 1px solid #f1f5f9;
}

.table-wrapper {
    flex: 1 1 auto; /* Tự động chiếm hết không gian trống ở giữa */
    overflow: auto; /* Tự sinh thanh cuộn (dọc/ngang) khi nội dung lớn hơn vùng này */
    position: relative;
    background: white;
}

.native-admin-table {
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;
    table-layout: auto; 
}

.header-cell {
    position: sticky; /* Giữ header luôn ở trên cùng khi cuộn dọc */
    top: 0;
    z-index: 20;
    background: #f8fafc;
    color: #475569;
    padding: 12px 16px;
    font-size: 0.8125rem;
    font-weight: 700;
    border-bottom: 2px solid #f1f5f9;
    letter-spacing: 0.025em;
    white-space: nowrap;
}

:deep(.data-cell) {
    padding: 10px 16px; /* Giảm nhẹ padding dọc để chứa được nhiều dòng hơn khi zoom */
    font-size: 0.875rem;
    color: #1e293b;
    vertical-align: middle;
    border-bottom: 1px solid #f1f5f9;
}

.pagination-footer {
    flex: none; /* Luôn luôn nằm ở dưới cùng và không bị mất */
    background: #ffffff;
    padding: 12px 20px;
    border-top: 1px solid #f1f5f9;
    z-index: 10;
}

/* Các tùy chỉnh phụ khác */
:deep(.v-btn) {
    text-transform: none;
    letter-spacing: normal;
    font-weight: 600;
}

:deep(.tight-switch) {
    transform: scale(0.8);
}

:deep(.add-btn-primary) {
    background: linear-gradient(135deg, #2563eb 100%) !important;
    box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.2) !important;
}
</style>
