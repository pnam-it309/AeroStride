<script setup>
import { ref } from 'vue'

const emit = defineEmits(['search', 'filter'])

const searchQuery = ref('')

const handleSearch = () => {
  emit('search', searchQuery.value)
}
</script>

<template>
  <div class="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <!-- Search Input -->
      <div class="relative flex-1 max-w-md">
        <span class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
          🔍
        </span>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search items..."
          class="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-900 text-sm focus:ring-aurora focus:border-aurora outline-none transition-all"
          @input="handleSearch"
        />
      </div>

      <!-- Filters Slot -->
      <div class="flex items-center gap-3">
        <slot name="filters" />
        <button class="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 transition-colors">
          Reset
        </button>
      </div>
    </div>
  </div>
</template>
