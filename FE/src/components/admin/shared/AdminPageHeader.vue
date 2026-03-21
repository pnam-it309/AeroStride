<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <div class="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
    <div class="flex flex-col gap-1">
      <h1 class="text-2xl font-bold text-gray-900 font-display">
        {{ title }}
      </h1>
      <p v-if="subtitle" class="text-sm text-gray-500 font-body">
        {{ subtitle }}
      </p>
    </div>
  </div>
</template>
