<script setup>
import { LayoutGridIcon } from 'vue-tabler-icons';

const props = defineProps({
    title: { type: String, default: 'Danh sách' },
    headers: { type: Array, default: () => [] },
    items: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    initialLoading: { type: Boolean, default: false }, // lần đầu tải → skeleton
    fetching: { type: Boolean, default: false },        // pagination/filter → dim
    addButtonText: { type: String, default: 'Thêm mới' },
    showAddButton: { type: Boolean, default: true },
    showExportButton: { type: Boolean, default: false },
    showImportButton: { type: Boolean, default: false },
    showTemplateButton: { type: Boolean, default: false },
    exportButtonText: { type: String, default: 'Xuất Excel' },
    emptyText: { type: String, default: 'Không có dữ liệu phù hợp để hiển thị' },
    emptySubtext: { type: String, default: '' },
    emptyIcon: { type: String, default: 'mdi-database-search-outline' },
    showEmptyAction: { type: Boolean, default: false },
    emptyActionText: { type: String, default: '' },
    selectable: { type: Boolean, default: false },
    hideToolbar: { type: Boolean, default: false },
    totalCount: { type: Number, default: 0 },
    skeletonRows: { type: Number, default: 7 }, // số skeleton rows
    maxHeight: { type: [String, Number], default: null },
    centerTitle: { type: Boolean, default: false }
});

const emit = defineEmits(['add', 'export', 'import', 'downloadTemplate', 'empty-action']);

// isSkeletonMode: dùng initialLoading nếu được pass, fallback về loading & items rỗng
const isSkeletonMode = () =>
    props.initialLoading || (props.loading && props.items.length === 0 && !props.fetching);

const isFetchingMode = () =>
    props.fetching || (props.loading && props.items.length > 0);
</script>

<template>
    <div v-bind="$attrs" class="admin-table-main-root">
        <v-card class="admin-table-container elevation-0" elevation="0">
            <!-- Toolbar Area -->
            <div v-if="!hideToolbar" class="table-toolbar d-flex align-center justify-space-between px-3 py-2">
                <div :class="['d-flex align-center', centerTitle ? 'table-title-centered' : '']">
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
            <div
                class="table-wrapper position-relative"
                :style="maxHeight ? { maxHeight: typeof maxHeight === 'number' ? `${maxHeight}px` : maxHeight } : undefined"
            >
                <!-- Top Progress Bar khi pagination/filter (fetching mode) -->
                <v-progress-linear
                    v-if="isFetchingMode()"
                    indeterminate
                    color="primary"
                    height="2.5"
                    class="table-fetching-bar"
                />

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

                    <!-- Skeleton Rows: hiện khi initial load (chưa có data) -->
                    <tbody v-if="isSkeletonMode()" class="skeleton-tbody">
                        <tr v-for="r in skeletonRows" :key="'sk-' + r" class="skeleton-row">
                            <td v-if="selectable" class="text-center px-0"><div class="skeleton-cell skeleton-check"></div></td>
                            <td v-for="(header, idx) in (headers.length ? headers : Array(4).fill({}))"
                                :key="idx"
                                class="skeleton-td"
                            >
                                <div class="skeleton-cell" :style="{ width: idx === 0 ? '40%' : idx === 1 ? '60%' : '50%' }"></div>
                            </td>
                        </tr>
                    </tbody>

                    <!-- Real data rows -->
                    <tbody
                        v-else-if="items.length > 0"
                        :class="['table-body-transition', { 'table-is-fetching': isFetchingMode() }]"
                    >
                        <template v-for="(item, index) in items" :key="item.id ?? index">
                            <slot name="row" :item="item" :index="index"></slot>
                        </template>
                    </tbody>
                </table>

                <!-- Empty state: chỉ hiện khi không còn loading và items rỗng -->
                <div
                    v-if="!isSkeletonMode() && items.length === 0"
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
                        <span v-if="emptySubtext" class="text-slate-400 text-center text-caption mt-1">
                            {{ emptySubtext }}
                        </span>
                        <slot name="empty-action">
                            <v-btn
                                v-if="showEmptyAction && emptyActionText"
                                variant="outlined"
                                color="primary"
                                size="small"
                                class="rounded-pill mt-3 text-none"
                                @click="$emit('empty-action')"
                            >
                                {{ emptyActionText }}
                            </v-btn>
                        </slot>
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
.table-fetching-bar {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    z-index: 10;
}

.table-body-transition {
    transition: opacity 0.12s ease;
}

.table-is-fetching {
    opacity: 0.5;
    pointer-events: none;
}

/* Skeleton styles */
.skeleton-tbody .skeleton-row {
    border-bottom: 1px solid #f1f5f9;
}

.skeleton-td {
    padding: 10px 12px;
}

.skeleton-cell {
    height: 14px;
    border-radius: 6px;
    background: linear-gradient(90deg, #f1f5f9 25%, #e8edf4 50%, #f1f5f9 75%);
    background-size: 200% 100%;
    animation: skeleton-shimmer 1.4s ease infinite;
    min-width: 40px;
}

.skeleton-check {
    width: 18px;
    height: 18px;
    border-radius: 4px;
    margin: 0 auto;
}

@keyframes skeleton-shimmer {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
}
</style>
