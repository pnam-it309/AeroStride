<script setup>
import { useToast } from '@/composable/useToast';
const { toasts, removeToast } = useToast();
</script>

<template>
  <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-2 pointer-events-none">
    <TransitionGroup 
      enter-active-class="transform transition duration-300 ease-out"
      enter-from-class="translate-x-full opacity-0"
      enter-to-class="translate-x-0 opacity-100"
      leave-active-class="transition duration-200 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div 
        v-for="toast in toasts" 
        :key="toast.id"
        class="pointer-events-auto px-6 py-3 rounded-xl shadow-2xl border flex items-center min-w-[300px]"
        :class="[
          toast.type === 'success' ? 'bg-green-50 border-green-200 text-green-800' : 
          toast.type === 'error' ? 'bg-red-50 border-red-200 text-red-800' : 
          'bg-blue-50 border-blue-200 text-blue-800'
        ]"
      >
        <div class="mr-3">
          <i v-if="toast.type === 'success'" class="ph-check-circle-bold text-xl"></i>
          <i v-else-if="toast.type === 'error'" class="ph-x-circle-bold text-xl"></i>
          <i v-else class="ph-info-bold text-xl"></i>
        </div>
        <span class="flex-1 font-medium">{{ toast.message }}</span>
        <button @click="removeToast(toast.id)" class="ml-4 opacity-50 hover:opacity-100">
          <i class="ph-x-bold italic"></i>
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>
