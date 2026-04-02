<script setup>
import AdminPageHeader from '@/components/admin/shared/AdminPageHeader.vue'

defineProps({
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    default: 'Module này đã được tách route và menu, nhưng màn hình nghiệp vụ chi tiết chưa được triển khai.',
  },
})
</script>

<template>
  <div class="space-y-6">
    <AdminPageHeader :title="title" :subtitle="subtitle" />

    <section class="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
      <p class="text-sm text-slate-500">
        Trang này hiện được dùng làm placeholder để giữ cấu trúc module, router, menu và breadcrumb ổn định.
      </p>
    </section>
  </div>
</template>
