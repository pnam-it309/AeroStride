<script setup>
import { useLoadingStore } from '@/stores/loadingStore'
import { storeToRefs } from 'pinia'

const loadingStore = useLoadingStore()
const { isOverlayLoading } = storeToRefs(loadingStore)
</script>

<template>
  <Transition name="fade">
    <div
      v-if="isOverlayLoading"
      class="fixed inset-0 z-[10000] flex items-center justify-center bg-obsidian/80 backdrop-blur-md"
    >
      <div class="relative flex flex-col items-center gap-6">
        <!-- Premium Loader -->
        <div class="relative w-24 h-24">
          <!-- Outer Spinning Ring -->
          <div class="absolute inset-0 border-4 border-aurora/10 rounded-full"></div>
          <div class="absolute inset-0 border-4 border-t-aurora rounded-full animate-spin"></div>

          <!-- Inner Gaping Logo/Shape -->
          <div
            class="absolute inset-4 border-2 border-white/5 rounded-full animate-pulse-slow"
          ></div>

          <!-- Central Pulse -->
          <div class="absolute inset-8 bg-aurora/20 rounded-full animate-ping opacity-50"></div>
        </div>

        <!-- Text with Shimmer -->
        <div class="flex flex-col items-center">
          <span class="text-xs font-black text-white uppercase tracking-[0.4em] animate-pulse">
            Processing
          </span>
          <div class="h-0.5 w-12 bg-aurora mt-2 rounded-full overflow-hidden">
            <div class="h-full bg-white/40 w-full animate-shimmer scale-x-50"></div>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.4s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.animate-pulse-slow {
  animation: pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.2;
  }
}

@keyframes shimmer {
  0% {
    transform: translateX(-150%) scaleX(0.5);
  }
  100% {
    transform: translateX(150%) scaleX(0.5);
  }
}

.animate-shimmer {
  animation: shimmer 1.5s infinite linear;
}
</style>
