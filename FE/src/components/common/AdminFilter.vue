<script setup>
defineProps({
    title: { type: String, default: 'Bộ lọc' },
    loading: { type: Boolean, default: false },
    isRefreshing: { type: Boolean, default: false },
    resetText: { type: String, default: 'Đặt lại' }
});

const emit = defineEmits(['refresh', 'search']);
</script>

<template>
    <v-card class="filter-card mb-2 p-3 border shadow-none" elevation="0">
        <v-card-text class="pa-3 pa-md-4">
            <div class="filter-header d-flex align-center mb-3">
                <v-icon color="#000000" class="mr-2 filter-main-icon">mdi-filter-variant</v-icon>
                <div class="filter-title text-slate-900">{{ title }}</div>
            </div>

            <v-row dense align="end" class="filter-grid">
                <!-- Các ô filter -->
                <slot></slot>
                
                <!-- Nút làm mới - luôn hiển thị bên ngoài slot -->
                <v-col cols="auto" class="filter-reset-col ml-auto align-self-end pb-1">
                    <v-btn variant="outlined" color="primary" class="reset-btn" :disabled="loading || isRefreshing"
                        @click="emit('refresh')">
                        <v-icon size="18">
                            {{ isRefreshing ? 'mdi-loading' : 'mdi-refresh' }}
                        </v-icon>
                        <v-tooltip activator="parent" location="top"> Làm mới bộ lọc </v-tooltip>
                    </v-btn>
                </v-col>
            </v-row>
        </v-card-text>
    </v-card>
</template>

<style scoped>
/* Scoped styles kept minimal; core styles moved to _admin-common.scss */
:deep(.v-field) {
    border-radius: 10px !important;
}
:deep(.v-field--variant-outlined .v-field__outline) {
    color: #d5deea !important;
}
.reset-btn :deep(.v-icon) {
    transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
:deep(.reset-btn .mdi-loading) {
    animation: filter-spin 1s linear infinite;
}
@keyframes filter-spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}
</style>
