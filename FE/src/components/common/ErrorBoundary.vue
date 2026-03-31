<script setup>
import { ref, onErrorCaptured } from 'vue'

const props = defineProps({
  fallback: {
    type: Object,
    default: null,
  },
})

const error = ref(null)

onErrorCaptured((err) => {
  error.value = err
  // Prevent the error from propagating further
  return false
})

const reset = () => {
  error.value = null
}
</script>

<template>
  <div v-if="error">
    <slot name="fallback" :error="error" :reset="reset">
      <div class="p-6 bg-red-50 border border-red-200 rounded-lg text-center">
        <h3 class="text-lg font-semibold text-red-800 mb-2">Something went wrong</h3>
        <p class="text-red-600 mb-4">{{ error.message || 'An unexpected error occurred.' }}</p>
        <button
          @click="reset"
          class="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 transition"
        >
          Try again
        </button>
      </div>
    </slot>
  </div>
  <slot v-else></slot>
</template>
