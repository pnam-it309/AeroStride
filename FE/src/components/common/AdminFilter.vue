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
            <div class="filter-title text-slate-900 mb-2">{{ title }}</div>

            <v-row dense align="start" class="filter-grid">
                <slot></slot>
                <v-col cols="12" sm="12" md="2" class="filter-reset-col d-flex align-end justify-end">
                    <v-btn
                        variant="outlined"
                        color="primary"
                        class="reset-btn"
                        :icon="isRefreshing ? 'mdi-loading' : 'mdi-refresh'"
                        :title="resetText"
                        :disabled="loading || isRefreshing"
                        @click="emit('refresh')"
                    />
                </v-col>
            </v-row>
        </v-card-text>
    </v-card>
</template>

<style scoped>
.filter-card {
    background-color: white;
    border: 1px solid #dbe4ef !important;
    border-radius: 8px !important;
    box-shadow: none !important;
    width: 100%;
    max-width: 100%;
    margin-right: 0;
}

.filter-title {
    font-size: 16px;
    font-weight: 700;
    color: #1e3a8a;
}

:deep(.filter-grid) {
    row-gap: 4px;
}

:deep(.filter-grid > .v-col) {
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
    padding-top: 4px;
    padding-bottom: 4px;
}

:deep(.v-field) {
    border-radius: 10px !important;
    border-width: 1px !important;
}

:deep(.v-field input),
:deep(.v-field__input),
:deep(.v-select__selection-text),
:deep(.v-field__input::placeholder) {
    font-size: 13px !important;
}

:deep(.v-field--variant-outlined .v-field__outline) {
    border-radius: 10px !important;
    color: #d5deea !important;
    --v-field-border-opacity: 0.45;
}

:deep(.v-field:hover .v-field__outline) {
    color: #cdd8e6 !important;
    --v-field-border-opacity: 0.6;
}

:deep(.v-field--focused .v-field__outline) {
    color: #a8bad5 !important;
    --v-field-border-opacity: 0.75;
}

:deep(.v-field input[type='date']) {
    font-size: 13px !important;
    width: 100% !important;
    min-width: 0;
    box-sizing: border-box;
    padding-top: 10px;
    padding-bottom: 10px;
    padding-left: 10px;
    padding-right: 0;
}

:deep(.v-field input[type='date']::-webkit-calendar-picker-indicator) {
    margin-left: auto;
    margin-right: 0;
    padding: 0;
    cursor: pointer;
}

.reset-btn {
    width: 36px !important;
    min-width: 36px !important;
    height: 36px !important;
    border-radius: 8px !important;
    border-color: #3b5ba9 !important;
    background: #ffffff !important;
    color: #3b5ba9 !important;
    box-shadow: none !important;
}

:deep(.reset-btn .v-icon) {
    font-size: 18px;
}

:deep(.reset-btn .mdi-loading) {
    animation: filter-spin 1s linear infinite;
}

@keyframes filter-spin {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
    }
}

.filter-reset-col {
    flex: 0 0 auto;
    align-self: flex-start;
    padding-bottom: 4px;
}
</style>
