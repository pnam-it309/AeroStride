<script setup>
import AeroButton from '@/components/base/AeroButton.vue'

defineProps({
  title: {
    type: String,
    default: 'Không thể tải nội dung',
  },
  message: {
    type: String,
    default: 'Vui lòng thử lại sau.',
  },
  icon: {
    type: String,
    default: 'ph-warning-octagon-bold',
  },
})

const emit = defineEmits(['retry'])
</script>

<template>
  <div
    class="flex flex-col items-center justify-center p-12 text-center bg-white rounded-3xl border border-gray-100 shadow-sm animate-in fade-in zoom-in duration-500"
  >
    <div class="w-20 h-20 rounded-2xl bg-gray-50 flex items-center justify-center mb-6">
      <i :class="[icon, 'text-4xl text-gray-400']"></i>
    </div>
    <h4 class="text-xl font-black text-gray-900 uppercase tracking-tight">{{ title }}</h4>
    <p class="text-gray-500 mt-2 mb-8 max-w-xs mx-auto font-medium">{{ message }}</p>

    <AeroButton @click="emit('retry')" variant="outline" size="sm" glow>
      <i class="ph-arrows-clockwise-bold mr-2"></i>
      Tải lại dữ liệu
    </AeroButton>
  </div>
</template>
