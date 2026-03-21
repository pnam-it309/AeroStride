<script setup>
import { useDataTable } from '@/composable/useDataTable';
import { API_ENDPOINTS } from '@/constants';
import AdminPageHeader from '@/components/admin/shared/AdminPageHeader.vue';
import AdminDataTable from '@/components/admin/shared/AdminDataTable.vue';

const columns = [
  { key: 'id', label: 'ID', sortable: true },
  { key: 'name', label: 'Tên sản phẩm', sortable: true },
  { key: 'category', label: 'Danh mục' },
  { key: 'price', label: 'Giá', sortable: true },
  { key: 'stock', label: 'Kho', sortable: true },
  { key: 'status', label: 'Trạng thái' }
];

const {
  items: products,
  loading,
  total,
  pagination,
  handlePageChange,
  handlePageSizeChange,
  handleSort
} = useDataTable(API_ENDPOINTS.ADMIN.PRODUCTS);
</script>

<template>
  <div class="flex flex-col space-y-6 flex-1 min-h-0">
    <AdminPageHeader 
      title="Sản phẩm" 
      subtitle="Quản lý kho hàng và mẫu mã sản phẩm" 
    />
    
    <AdminDataTable 
      :columns="columns" 
      :data="products" 
      :loading="loading"
      :total="total"
      :page="pagination.page"
      :size="pagination.size"
      @update:page="handlePageChange"
      @update:size="handlePageSizeChange"
      @sort="handleSort"
    />
  </div>
</template>
