<script setup>
import { PlusIcon, SearchIcon, DownloadIcon, LayoutGridIcon } from 'vue-tabler-icons';

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
    <div class="table-toolbar d-flex align-center justify-space-between pa-4 border-b">
      <div class="d-flex align-center">
        <LayoutGridIcon size="20" class="text-primary mr-3" />
        <h3 class="text-h6 font-weight-bold text-dark tracking-tight">{{ title }}</h3>
      </div>
        <div class="d-flex align-center gap-2">
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
            class="px-6 font-weight-bold"
            @click="$emit('add')"
          >
            {{ addButtonText }}
          </v-btn>
        </div>
    </div>

    <!-- Main Table Table -->
    <div class="table-wrapper">
      <table class="native-admin-table">
        <thead>
          <tr>
            <th v-for="(header, idx) in headers" 
                :key="idx" 
                :style="{ textAlign: 'center', width: header.width || 'auto' }"
                class="header-cell"
            >
              {{ header.text || header }}
            </th>
          </tr>
        </thead>
        <tbody v-if="!loading && items.length > 0">
          <slot name="row" v-for="item in items" :key="item.id" :item="item"></slot>
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
  border: 1px solid #e2e8f0 !important;
  border-radius: 6px !important; /* BO TRÒN NHẸ */
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
}

.table-toolbar {
  background: #f8fafc;
}

.table-wrapper {
  overflow-x: auto;
  overflow-y: auto; /* CUỘN NỘI BỘ */
  max-height: 485px; /* Giới hạn khoảng 5 dòng + header */
  background: white;
}

.native-admin-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.header-cell {
  position: sticky; /* CỐ ĐỊNH TIÊU ĐỀ */
  top: 0;
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
  padding: 16px 12px;
  font-size: 15px;
  color: #1e293b;
  vertical-align: middle;
  text-align: center !important; /* ÉP TOÀN BỘ CĂN GIỮA TRÊN TOÀN HỆ THỐNG */
}

/* Force all flex containers inside data cells to center */
:deep(.data-cell .d-flex) {
  justify-content: center !important;
  text-align: center !important;
}

:deep(.data-cell .text-left) {
  text-align: center !important;
}

:deep(.v-btn) {
  border-radius: 3px !important;
}

/* Pagination container spacing */
.pagination-footer {
  background: #fdfdfd;
  padding: 0 16px 16px 16px;
}

/* GAP SUPPORT FOR ALIGN CENTER CONTAINERS */
.gap-2 { gap: 8px; }
.gap-4 { gap: 16px; }

/* FIX IMAGE ISSUE: Force buttons to be centered and TIGHT together, never at edges */
:deep(.d-flex.align-center.justify-center) {
  gap: 8px !important;
  display: flex !important;
  justify-content: center !important;
  width: 100% !important;
}

/* Fix Switch Width and stop it from growing */
:deep(.tight-switch) {
  flex: none !important; /* KHÔNG CHO GIÃN RỘNG */
  width: 40px !important;
  margin: 0 !important;   /* BỎ MARGIN MẶC ĐỊNH */
  display: inline-flex !important;
  transform: scale(0.85); /* Nhỏ vừa đủ */
}

:deep(.tight-switch .v-selection-control) {
  min-height: unset !important;
}

:deep(.v-btn.rounded-0) {
  flex: none !important;
}
</style>
