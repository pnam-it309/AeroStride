<script setup>
import { useLoadingStore } from '@/stores/loadingStore'
import { storeToRefs } from 'pinia'

const loadingStore = useLoadingStore()
const { isProgressBarLoading, progress } = storeToRefs(loadingStore)
</script>

<template>
  <div 
    v-if="isProgressBarLoading"
    class="fixed top-0 left-0 right-0 z-[9999] h-1 bg-gray-100/10 overflow-hidden"
  >
    <div 
      class="h-full bg-gradient-to-r from-aurora via-aurora/80 to-aurora shadow-[0_0_10px_rgba(var(--aurora-rgb),0.5)] transition-all duration-300 ease-out"
      :style="{ width: progress + '%' }"
    ></div>
  </div>
</template>

<style scoped>
/* Any specific styles if needed */
</style>
