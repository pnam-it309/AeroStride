<script setup>
defineProps({
  type: {
    type: String,
    default: 'text', // text, circle, rectangle, card
  },
  count: {
    type: Number,
    default: 1,
  },
  width: {
    type: String,
    default: '100%',
  },
  height: {
    type: String,
    default: '1rem',
  },
})
</script>

<template>
  <div class="flex flex-col gap-2 w-full">
    <div
      v-for="n in count"
      :key="n"
      :class="[
        'animate-pulse bg-gray-200',
        type === 'text' ? 'rounded' : '',
        type === 'circle' ? 'rounded-full' : '',
        type === 'rectangle' ? 'rounded-lg' : '',
        type === 'card' ? 'rounded-2xl h-48 w-full' : '',
      ]"
      :style="{ width, height: type === 'card' ? '' : height }"
    ></div>
  </div>
</template>

<style scoped>
@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
.animate-pulse {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
</style>
