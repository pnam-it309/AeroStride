<script setup>
import { ref } from 'vue'
import { Search, RotateCcw } from 'lucide-vue-next'

const emit = defineEmits(['search', 'filter'])

const searchQuery = ref('')

const handleSearch = () => {
  emit('search', searchQuery.value)
}

const handleReset = () => {
  searchQuery.value = ''
  emit('search', '')
}
</script>

<template>
  <div class="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div class="relative flex-1 max-w-md">
        <span class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
          <Search :size="16" />
        </span>
        <input v-model="searchQuery" type="text" placeholder="Search items..."
          class="block w-full pl-10 pr-3 py-2.5 border border-gray-200 rounded-xl bg-gray-50/50 text-gray-900 text-sm focus:ring-2 focus:ring-aurora/20 focus:border-aurora outline-none transition-all placeholder:text-gray-400"
          @input="handleSearch" />
      </div>

      <!-- Filters Slot -->
      <div class="flex items-center gap-3">
        <slot name="filters" />
        <div class="relative group">
          <button @click="handleReset"
            class="p-2.5 bg-white border border-gray-200 shadow-sm rounded-xl text-gray-500 hover:text-aurora hover:border-aurora/50 hover:bg-aurora/5 transition-all active:scale-95">
            <RotateCcw :size="18" />
          </button>
          <!-- Tooltip -->
          <div
            class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block transition-all opacity-0 group-hover:opacity-100">
            <div
              class="bg-gray-900 text-white text-[10px] font-black uppercase tracking-widest px-3 py-1.5 rounded-lg whitespace-nowrap shadow-xl">
              làm mới bộ lọc
            </div>
            <div class="w-2 h-2 bg-gray-900 rotate-45 absolute -bottom-1 left-1/2 -translate-x-1/2"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
