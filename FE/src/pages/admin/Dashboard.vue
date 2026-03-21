<script setup>
import { ref, computed } from 'vue'
import AdminPageHeader from '../../components/admin/shared/AdminPageHeader.vue'
import AdminDataFilter from '../../components/admin/shared/AdminDataFilter.vue'
import AdminDataTable from '../../components/admin/shared/AdminDataTable.vue'

const searchQuery = ref('')
const filterStatus = ref('All')

const columns = [
  { key: 'name', label: 'Product Name' },
  { key: 'category', label: 'Category' },
  { key: 'price', label: 'Price' },
  { key: 'stock', label: 'Stock' },
  { key: 'status', label: 'Status' }
]

const products = ref([
  { id: 1, name: 'AeroPulse V2', category: 'Running', price: '$129.99', stock: 45, status: 'Active' },
  { id: 2, name: 'CloudStride Lite', category: 'Training', price: '$99.00', stock: 12, status: 'Low Stock' },
  { id: 3, name: 'Velocity Pro', category: 'Racing', price: '$249.50', stock: 8, status: 'Critical' },
  { id: 4, name: 'TrailForge X', category: 'Trail', price: '$150.00', stock: 0, status: 'Out of Stock' },
  { id: 5, name: 'Ignite Run', category: 'Running', price: '$110.00', stock: 120, status: 'Active' },
])

const filteredProducts = computed(() => {
  return products.value.filter(p => {
    const matchesSearch = p.name.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchesStatus = filterStatus.value === 'All' || p.status === filterStatus.value
    return matchesSearch && matchesStatus
  })
})

const getStatusClass = (status) => {
  if (status === 'Active') return 'bg-emerald-50 text-emerald-600'
  if (status === 'Low Stock') return 'bg-amber-50 text-amber-600'
  return 'bg-red-50 text-red-600'
}
</script>

<template>
  <div class="flex-1 flex flex-col space-y-6 min-h-0">
      
      <AdminPageHeader 
        title="Dashboard Overview" 
        subtitle="Manage your inventory and track product performance across all categories."
      />

      <AdminDataFilter @search="(q) => searchQuery = q">
        <template #filters>
          <select v-model="filterStatus" class="border border-gray-300 rounded-lg text-sm px-3 py-2 bg-white outline-none focus:ring-1 focus:ring-aurora focus:border-aurora">
            <option>All</option>
            <option>Active</option>
            <option>Low Stock</option>
          </select>
        </template>
      </AdminDataFilter>

      <AdminDataTable
        :columns="columns"
        :data="filteredProducts"
        :current-page="1"
        :total-pages="1"
      >
        <template #cell-name="{ row }">
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 rounded-lg bg-gray-50 flex items-center justify-center font-bold text-xs border border-gray-100 italic">
              {{ row.name[0] }}
            </div>
            <span class="font-bold text-gray-900">{{ row.name }}</span>
          </div>
        </template>

        <template #cell-status="{ row }">
          <span :class="getStatusClass(row.status)" class="px-2 py-1 rounded-md text-[10px] font-black uppercase tracking-wider">
            {{ row.status }}
          </span>
        </template>

        <template #cell-stock="{ row }">
          <div class="flex items-center gap-2">
            <div class="flex-1 max-w-[60px] h-1 bg-gray-100 rounded-full overflow-hidden">
              <div class="h-full bg-aurora" :style="{ width: Math.min(row.stock, 100) + '%' }"></div>
            </div>
            <span class="text-[10px] font-bold text-gray-400 uppercase">{{ row.stock }} U</span>
          </div>
        </template>
      </AdminDataTable>

    </div>
</template>
