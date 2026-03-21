<script setup>
import { computed } from 'vue';
import { ORDER_STATUS } from '@/constants';

const props = defineProps({
  status: {
    type: String,
    required: true
  },
  type: {
    type: String,
    default: 'order' // Can be expanded for other status types
  }
});

const statusConfig = computed(() => {
  if (props.type === 'order') {
    return ORDER_STATUS[props.status] || { 
      text: props.status, 
      color: 'bg-gray-100 text-gray-800 border-gray-200',
      icon: 'ph-info-bold'
    };
  }
  return { text: props.status, color: 'bg-gray-100' };
});
</script>

<template>
  <span 
    :class="[
      'px-3 py-1 rounded-full text-xs font-bold border flex items-center gap-1.5 w-max transition-all duration-300',
      statusConfig.color
    ]"
  >
    <i v-if="statusConfig.icon" :class="statusConfig.icon"></i>
    {{ statusConfig.text }}
  </span>
</template>
