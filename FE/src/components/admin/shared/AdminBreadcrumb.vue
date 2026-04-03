<script setup>
defineProps({
  items: {
    type: Array,
    default: () => [],
  },
})
</script>

<template>
  <nav v-if="items.length" class="flex flex-wrap items-center gap-2 text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">
    <template v-for="(item, index) in items" :key="`${item.label}-${index}`">
      <router-link
        v-if="item.to && index !== items.length - 1"
        :to="item.to"
        class="transition hover:text-aurora"
      >
        {{ item.label }}
      </router-link>
      <span v-else :class="index === items.length - 1 ? 'text-slate-600' : ''">
        {{ item.label }}
      </span>
      <span v-if="index !== items.length - 1" class="text-slate-300">/</span>
    </template>
  </nav>
</template>
