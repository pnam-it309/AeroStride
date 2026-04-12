<script setup>
import { useUIStore } from '@/stores/ui';
import { storeToRefs } from 'pinia';

const uiStore = useUIStore();
const { globalLoading: overlay, progressBar, loadingMessage: message } = storeToRefs(uiStore);
</script>

<template>
  <div>
    <!-- TOP PROGRESS BAR (FOR NORMAL LOADING) -->
    <v-progress-linear
      v-if="progressBar"
      indeterminate
      color="primary"
      height="4"
      class="top-progress"
      fixed
      top
    ></v-progress-linear>

    <!-- FULL SCREEN OVERLAY (FOR IMPORTANT RELOADS) -->
    <v-overlay
      :model-value="overlay"
      class="align-center justify-center global-overlay"
      persistent
      scrim="#ffffff"
    >
      <div class="text-center">
        <div class="logo-container mb-6 animate-pulse">
            <img src="@/assets/images/logos/logo.jpg" alt="Logo" class="loader-logo" />
        </div>
        <v-progress-circular
          indeterminate
          color="primary"
          size="64"
          width="6"
          class="mb-4"
        ></v-progress-circular>
        <h2 class="text-h4 font-weight-black text-primary tracking-widest text-uppercase animate-bounce-slow">
            {{ message }}
        </h2>
        <div class="mt-4 text-grey-darken-1 font-weight-bold">Vui lòng đợi trong giây lát...</div>
      </div>
    </v-overlay>
  </div>
</template>

<style scoped>
.top-progress {
  z-index: 9999;
}

.global-overlay {
  backdrop-filter: blur(15px);
  z-index: 10000;
}

.loader-logo {
    height: 120px;
    filter: drop-shadow(0 10px 20px rgba(var(--v-theme-primary), 0.2));
}

.animate-pulse {
    animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: .7; transform: scale(0.95); }
}

.animate-bounce-slow {
    animation: bounce 3s infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(-5%); animation-timing-function: cubic-bezier(0.8, 0, 1, 1); }
  50% { transform: translateY(0); animation-timing-function: cubic-bezier(0, 0, 0.2, 1); }
}

.tracking-widest {
    letter-spacing: 0.25em !important;
}
</style>
