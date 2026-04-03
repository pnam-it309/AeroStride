<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AdminBreadcrumb from './AdminBreadcrumb.vue'

const route = useRoute()

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    default: '',
  },
  breadcrumbs: {
    type: Array,
    default: null,
  },
})

const resolvedBreadcrumbs = computed(() => props.breadcrumbs || route.meta?.breadcrumbs || [])
</script>

<template>
  <div class="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
    <div class="flex flex-col gap-1">
      <AdminBreadcrumb :items="resolvedBreadcrumbs" />
      <h1 class="text-2xl font-bold text-gray-900 font-display">
        {{ title }}
      </h1>
      <p v-if="subtitle" class="text-sm text-gray-500 font-body">
        {{ subtitle }}
      </p>
    </div>
  </div>
</template>
