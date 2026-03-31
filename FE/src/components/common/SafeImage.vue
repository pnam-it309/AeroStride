<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  src: {
    type: String,
    required: true,
  },
  alt: {
    type: String,
    default: 'Image',
  },
  fallback: {
    type: String,
    default: '/assets/images/placeholder.png', // Ensure this exists or use a remote placeholder
  },
  placeholder: {
    type: String,
    default: 'https://placehold.co/600x400/f3f4f6/9ca3af?text=Loading...',
  },
})

const currentSrc = ref(props.src || props.fallback)
const isLoaded = ref(false)
const hasError = ref(false)

watch(
  () => props.src,
  (newSrc) => {
    currentSrc.value = newSrc
    isLoaded.value = false
    hasError.value = false
  },
)

const handleLoad = () => {
  isLoaded.value = true
}

const handleError = () => {
  if (currentSrc.value !== props.fallback) {
    currentSrc.value = props.fallback
    hasError.value = true
  }
}
</script>

<template>
  <div class="relative overflow-hidden bg-gray-100 rounded-lg">
    <!-- Skeleton/Loading state -->
    <div v-if="!isLoaded && !hasError" class="absolute inset-0 animate-pulse bg-gray-200"></div>

    <img
      :src="currentSrc"
      :alt="alt"
      @load="handleLoad"
      @error="handleError"
      v-bind="$attrs"
      :class="['transition-opacity duration-500', isLoaded ? 'opacity-100' : 'opacity-0']"
    />
  </div>
</template>
