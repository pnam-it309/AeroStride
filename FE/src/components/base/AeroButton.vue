<script setup>
defineProps({
  variant: {
    type: String,
    default: 'primary', // primary, outline, ghost
  },
  glow: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <button
    class="px-6 py-2.5 rounded-lg font-bold transition-all duration-300 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
    :class="[
      variant === 'primary' ? 'bg-aurora text-obsidian hover:bg-aurora/90' : '',
      variant === 'outline' ? 'border-2 border-aurora text-aurora hover:bg-aurora/10' : '',
      variant === 'ghost' ? 'text-cloud hover:bg-white/10' : '',
      glow ? 'aurora-glow' : '',
    ]"
  >
    <slot />
  </button>
</template>
