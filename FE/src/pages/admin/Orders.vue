<script setup>
import { useDataTable } from '@/composable/useDataTable';
import { API_ENDPOINTS } from '@/constants';
import AdminPageHeader from '@/components/admin/shared/AdminPageHeader.vue';
import AdminDataTable from '@/components/admin/shared/AdminDataTable.vue';
import AeroStatusBadge from '@/components/base/AeroStatusBadge.vue';

const columns = [
  { key: 'orderId', label: 'Mã đơn hàng', sortable: true },
  { key: 'customer', label: 'Khách hàng', sortable: true },
  { key: 'total', label: 'Tổng tiền', sortable: true },
  { key: 'status', label: 'Trạng thái' },
  { key: 'createdAt', label: 'Ngày đặt', sortable: true }
];

const {
  items: orders,
  loading,
  total,
  pagination,
  handlePageChange,
  handlePageSizeChange,
  handleSort
} = useDataTable(API_ENDPOINTS.ADMIN.ORDERS);
</script>

<template>
  <div class="flex flex-col space-y-6 flex-1 min-h-0">
    <AdminPageHeader 
      title="Đơn hàng" 
      subtitle="Theo dõi và quản lý đơn đặt hàng của khách" 
    />
    
    <AdminDataTable 
      :columns="columns" 
      :data="orders" 
      :loading="loading"
      :total="total"
      :page="pagination.page"
      :size="pagination.size"
      @update:page="handlePageChange"
      @update:size="handlePageSizeChange"
      @sort="handleSort"
    >
      <!-- Custom Badge for Status -->
      <template #cell-status="{ row }">
        <AeroStatusBadge :status="row.status" type="order" />
      </template>
    </AdminDataTable>
  </div>
</template>
