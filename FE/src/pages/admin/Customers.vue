<script setup>
import AdminPageHeader from '@/components/admin/shared/AdminPageHeader.vue'
import AdminDataTable from '@/components/admin/shared/AdminDataTable.vue'
import { ref } from 'vue'

const columns = [
  { key: 'name', label: 'Name' },
  { key: 'email', label: 'Email' },
  { key: 'role', label: 'Role' },
  { key: 'status', label: 'Status' }
]

const customers = ref([
  { name: 'Alice Smith', email: 'alice@example.com', role: 'Premium Buyer', status: 'Active' }
])
</script>

<template>
  <div class="flex flex-col space-y-6 flex-1 min-h-0">
    <AdminPageHeader title="Customers" subtitle="Manage your user base" />
    <AdminDataTable :columns="columns" :data="customers" />
  </div>
</template>
