<script setup>
import { RefreshIcon } from 'vue-tabler-icons';

defineProps({
  loading: { type: Boolean, default: false },
  isRefreshing: { type: Boolean, default: false }
});

const emit = defineEmits(['refresh', 'search']);
</script>

<template>
  <v-card class="filter-card mb-6 border shadow-none" elevation="0">
    <v-card-text class="pa-4 pt-5">
      <v-row align="center">
        <!-- Main Filter Slots -->
        <slot></slot>

        <!-- Refresh Button Area -->
        <v-col cols="12" md="auto" class="d-flex justify-end ml-auto">
          <v-btn
            icon
            variant="tonal"
            color="primary"
            class="square-btn animation-refresh"
            :loading="isRefreshing || loading"
            @click="emit('refresh')"
          >
            <RefreshIcon size="22" :class="{ 'spin-icon': isRefreshing }" />
            <v-tooltip activator="parent" location="top">Làm mới bộ lọc</v-tooltip>
          </v-btn>
        </v-col>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.filter-card {
  background-color: white;
  border: 1px solid #e2e8f0 !important;
  border-radius: 8px !important; /* BO TRÒN NHẸ */
}

/* Make inner fields slightly rounded */
:deep(.v-field) {
  border-radius: 6px !important; /* BO TRÒN NHẸ */
  border-width: 1px !important;
}

:deep(.v-field--variant-outlined .v-field__outline) {
  border-radius: 6px !important; /* BO TRÒN NHẸ */
}

.square-btn {
  border-radius: 6px !important; /* BO TRÒN NHẸ */
  width: 44px !important;
  height: 44px !important;
  border: 1px solid #e2e8f0;
}

.animation-refresh {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.spin-icon {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
