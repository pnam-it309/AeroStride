<script setup>
import { computed } from 'vue'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

const props = defineProps({
  currentPage: {
    type: Number,
    required: true,
  },
  totalPages: {
    type: Number,
    required: true,
  },
  pageSize: {
    type: Number,
    default: 10,
  },
})

const emit = defineEmits(['page-change', 'size-change'])

const pages = computed(() => {
  const range = []
  const delta = 2
  const left = props.currentPage - delta
  const right = props.currentPage + delta + 1

  for (let i = 1; i <= props.totalPages; i++) {
    if (i === 1 || i === props.totalPages || (i >= left && i < right)) {
      range.push(i)
    }
  }

  const result = []
  let l
  for (const i of range) {
    if (l) {
      if (i - l === 2) {
        result.push(l + 1)
      } else if (i - l !== 1) {
        result.push('...')
      }
    }
    result.push(i)
    l = i
  }
  return result
})

const handlePrev = () => {
  if (props.currentPage > 1) {
    emit('page-change', props.currentPage - 1)
  }
}

const handleNext = () => {
  if (props.currentPage < props.totalPages) {
    emit('page-change', props.currentPage + 1)
  }
}
</script>

<template>
  <div
    class="flex items-center justify-between px-4 py-3 bg-white/5 backdrop-blur-md border border-white/10 rounded-2xl shadow-xl transition-all duration-300"
  >
    <div class="flex items-center gap-6">
      <span class="text-xs font-black text-gray-400 uppercase tracking-widest hidden lg:block">
        Page <span class="text-aurora">{{ currentPage }}</span> of {{ totalPages }}
      </span>

      <!-- Page Size Selector -->
      <div class="flex items-center gap-2">
        <span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Show</span>
        <select
          :value="pageSize"
          @change="emit('size-change', Number($event.target.value))"
          class="bg-gray-50 border border-gray-100 text-gray-600 text-xs font-bold rounded-lg px-2 py-1 outline-none focus:border-aurora transition-colors"
        >
          <option :value="5">5</option>
          <option :value="10">10</option>
          <option :value="15">15</option>
          <option :value="20">20</option>
          <option :value="25">25</option>
          <option :value="30">30</option>
        </select>
      </div>
    </div>

    <div class="flex items-center gap-2">
      <!-- Previous Button -->
      <button
        @click="handlePrev"
        :disabled="currentPage <= 1"
        class="w-10 h-10 flex items-center justify-center rounded-xl bg-gray-50 border border-gray-100 text-gray-400 hover:text-aurora hover:border-aurora/50 hover:bg-white disabled:opacity-30 disabled:cursor-not-allowed transition-all duration-300 group shadow-sm"
      >
        <ChevronLeft :size="20" class="group-hover:-translate-x-0.5 transition-transform" />
      </button>

      <!-- Page Numbers -->
      <div class="flex items-center gap-1.5 px-2">
        <template v-for="(p, index) in pages" :key="index">
          <span v-if="p === '...'" class="w-8 text-center text-gray-300 font-black tracking-widest">
            {{ p }}
          </span>
          <button
            v-else
            @click="emit('page-change', Number(p))"
            class="min-w-[40px] h-10 px-2 flex items-center justify-center rounded-xl text-[11px] font-black uppercase tracking-tighter transition-all duration-300"
            :class="[
              currentPage === p
                ? 'bg-aurora text-white shadow-lg shadow-aurora/30 scale-105'
                : 'text-gray-400 hover:bg-gray-50 hover:text-aurora',
            ]"
          >
            {{ p }}
          </button>
        </template>
      </div>

      <!-- Next Button -->
      <button
        @click="handleNext"
        :disabled="currentPage >= totalPages"
        class="w-10 h-10 flex items-center justify-center rounded-xl bg-gray-50 border border-gray-100 text-gray-400 hover:text-aurora hover:border-aurora/50 hover:bg-white disabled:opacity-30 disabled:cursor-not-allowed transition-all duration-300 group shadow-sm"
      >
        <ChevronRight :size="20" class="group-hover:translate-x-0.5 transition-transform" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.aurora-glow {
  box-shadow: 0 0 20px rgba(var(--aurora-rgb, 0, 217, 255), 0.3);
}
</style>
