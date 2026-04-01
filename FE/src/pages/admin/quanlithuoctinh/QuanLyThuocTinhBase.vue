<script setup>
import { ref, onMounted } from 'vue'
import AdminDataFilter from '@/components/admin/shared/AdminDataFilter.vue'
import AdminDataTable from '@/components/admin/shared/AdminDataTable.vue'

const props = defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  service: { type: Object, required: true },
})

const data = ref([])
const loading = ref(false)
const currentPage = ref(0)
const totalPages = ref(1)
const currentSize = ref(5)

const columns = [
  { key: 'ma', label: 'Mã' },
  { key: 'ten', label: 'Tên' },
  { key: 'trangThai', label: 'Trạng thái' },
]

const fetchData = async (page = 0, size = currentSize.value) => {
  loading.value = true
  try {
    const response = await props.service.search({ page, size })
    data.value = response.content || []
    totalPages.value = response.totalPages || 1
    currentPage.value = response.pageNumber || 0
    currentSize.value = size
  } catch (error) {
    console.error(`Lỗi khi tải ${props.title}:`, error)
    data.value = []
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  fetchData(page - 1, currentSize.value)
}

const handleSizeChange = (size) => {
  fetchData(0, size)
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="h-full flex flex-col min-h-0 p-6 space-y-6">
    <div class="flex items-center justify-between">
      <div class="space-y-1">
        <h1 class="text-3xl font-display font-bold text-obsidian uppercase tracking-tight">{{ title }}</h1>
        <p class="text-slate-500 text-sm font-medium">{{ description }}</p>
      </div>
    </div>

    <!-- Standardized Filter -->
    <AdminDataFilter @search="(q) => fetchData(0, currentSize)" />

    <!-- Standardized Table -->
    <AdminDataTable
      :columns="columns"
      :data="data"
      :loading="loading"
      :current-page="currentPage + 1"
      :total-pages="totalPages"
      :page-size="currentSize"
      @update:page="handlePageChange"
      @update:size="handleSizeChange"
      @add="() => console.log('Add new clicked')"
    >
      <template #cell-ma="{ row }">
        <span class="font-bold text-aurora tracking-widest">{{ row.ma }}</span>
      </template>

      <template #cell-ten="{ row }">
        <span class="font-bold text-obsidian uppercase">{{ row.ten }}</span>
      </template>

      <template #cell-trangThai="{ row }">
        <span :class="`px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider ${
          row.trangThai === 0 ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'
        }`">
          {{ row.trangThai === 0 ? 'Hoạt động' : 'Ngừng' }}
        </span>
      </template>
    </AdminDataTable>
  </div>
</template>
