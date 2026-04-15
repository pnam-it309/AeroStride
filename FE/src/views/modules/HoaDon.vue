<script setup>
import { ref, onMounted } from 'vue';
import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';

// Reusable Components
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import { EyeIcon, ReceiptIcon } from 'vue-tabler-icons';

const loading = ref(false);
const isRefreshing = ref(false);
const orders = ref([]);
const showOrderDetailDialog = ref(false);
const selectedOrder = ref(null);

const filters = ref({
  keyword: '',
  trangThai: null
});

const pagination = ref({
  page: 1,
  size: 5,
  totalElements: 0,
  totalPages: 1
});

const tableHeaders = [
  { text: 'Mã & Ngày tạo', align: 'center', width: '220px' },
  { text: 'Thông tin khách hàng', align: 'center' ,width: '220px'},
  { text: 'Tổng tiền', align: 'center', width: '180px' },
  { text: 'Trạng thái', align: 'center', width: '180px' },
  { text: 'Thao tác', align: 'center', width: '100px' }
];

const loadOrders = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
      size: pagination.value.size,
      search: filters.value.keyword || null,
      trangThai: filters.value.trangThai || null
    };
    const response = await dichVuHoaDon.layHoaDonPhanTrang(params);
    orders.value = response.content || response;
    pagination.value.totalElements = response.totalElements || orders.value.length;
    pagination.value.totalPages = response.totalPages || 1;
  } catch (error) {
    console.error('Error loading orders:', error);
  } finally {
    loading.value = false;
  }
};

const handleRefresh = async () => {
  isRefreshing.value = true;
  filters.value = { keyword: '', trangThai: null };
  pagination.value.page = 1;
  await loadOrders();
  setTimeout(() => isRefreshing.value = false, 800);
};

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
};

const formatDate = (timestamp) => {
  if (!timestamp) return 'N/A';
  return new Date(timestamp).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
};

const getStatusInfo = (status) => {
  switch (status) {
    case 1: return { text: 'CHỜ XÁC NHẬN', color: 'warning' };
    case 2: return { text: 'ĐÃ XÁC NHẬN', color: 'info' };
    case 3: return { text: 'ĐANG GIAO', color: 'primary' };
    case 4: return { text: 'HOÀN THÀNH', color: 'success' };
    case 5: return { text: 'ĐÃ HỦY', color: 'error' };
    default: return { text: 'KHÔNG XÁC ĐỊNH', color: 'grey' };
  }
};

const viewOrderDetail = (order) => {
  selectedOrder.value = order;
  showOrderDetailDialog.value = true;
};

onMounted(() => loadOrders());
</script>

<template>
  <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
    <!-- Header -->
    <v-row class="mb-4">
      <v-col cols="12">
        <h2 class="text-h3 font-weight-black tracking-tight text-dark mb-1">Quản lý hóa đơn</h2>
        <div class="text-subtitle-1 text-medium-emphasis">Theo dõi dòng tiền và trạng thái đơn hàng AeroStride</div>
      </v-col>
    </v-row>

    <!-- 1. FILTER -->
    <AdminFilter :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
      <v-col cols="12" md="5">
        <v-text-field
          v-model="filters.keyword"
          placeholder="Tên khách hàng, Mã HĐ, SĐT..."
          variant="outlined"
          density="comfortable"
          hide-details
          prepend-inner-icon="mdi-magnify"
          class="font-weight-bold"
          rounded="md"
          @keyup.enter="loadOrders"
        ></v-text-field>
      </v-col>
      <v-col cols="12" md="3">
        <v-select
          v-model="filters.trangThai"
          :items="[
            { title: 'Tất cả trạng thái', value: null },
            { title: 'Chờ xác nhận', value: 1 },
            { title: 'Đã xác nhận', value: 2 },
            { title: 'Đang giao hàng', value: 3 },
            { title: 'Hoàn thành', value: 4 },
            { title: 'Đã hủy', value: 5 }
          ]"
          variant="outlined"
          density="comfortable"
          hide-details
          class="font-weight-bold"
          rounded="md"
          @update:model-value="loadOrders"
        ></v-select>
      </v-col>
    </AdminFilter>

    <!-- 2. TABLE -->
    <AdminTable
      title="DANH SÁCH ĐƠN HÀNG"
      :showAddButton="false"
      :headers="tableHeaders"
      :items="orders"
      :total-count="pagination.totalElements"
      :loading="loading"
    >
      <template #row="{ item }">
        <tr class="data-row">
          <td class="data-cell" style="text-align: start;">
            <div class="text-subtitle-1 font-weight-black text-primary">{{ item.maHoaDon || 'HD' + item.id }}</div>
            <div class="text-caption font-weight-bold text-medium-emphasis">{{ formatDate(item.ngayTao) }}</div>
          </td>
          
          <td class="data-cell" style="text-align: start;">
            <div class="text-subtitle-2 font-weight-black text-dark">{{ item.tenKhachHang || 'Khách vãng lai' }}</div>
            <div class="text-caption font-weight-bold text-medium-emphasis">{{ item.soDienThoai }}</div>
          </td>

          <td class="data-cell" style="text-align: center;">
            <div class="text-h6 font-weight-black text-error">{{ formatCurrency(item.tongTien) }}</div>
          </td>

          <td class="data-cell" style="text-align: center;">
            <v-chip 
              :color="getStatusInfo(item.trangThai).color"
              size="x-small"
              variant="flat"
              class="font-weight-black px-4"
            >
              {{ getStatusInfo(item.trangThai).text }}
            </v-chip>
          </td>

          <td class="data-cell" style="text-align: center;">
            <v-btn icon variant="tonal" size="32" color="primary" class="rounded-md" @click.stop="viewOrderDetail(item)">
              <EyeIcon size="18" />
            </v-btn>
          </td>
        </tr>
      </template>

      <template #pagination>
        <AdminPagination
          v-model="pagination.page"
          :page-size="pagination.size"
          @update:pageSize="pagination.size = $event"
          @update:page-size="pagination.size = $event"
          :total-pages="pagination.totalPages"
          :total-elements="pagination.totalElements"
          :current-size="orders.length"
          @change="loadOrders"
        />
      </template>
    </AdminTable>

    <!-- Detail Dialog -->
    <v-dialog v-model="showOrderDetailDialog" max-width="850">
      <v-card class="rounded-lg overflow-hidden" v-if="selectedOrder">
        <v-card-title class="pa-5 d-flex justify-space-between align-center border-b bg-grey-lighten-4">
          <div class="d-flex align-center">
            <ReceiptIcon size="24" class="mr-3 text-primary" />
            <span class="font-weight-black uppercase">CHI TIẾT HÓA ĐƠN #{{ selectedOrder.maHoaDon }}</span>
          </div>
          <v-chip :color="getStatusInfo(selectedOrder.trangThai).color" variant="flat" class="font-weight-bold px-6">
            {{ getStatusInfo(selectedOrder.trangThai).text }}
          </v-chip>
        </v-card-title>
        
        <v-card-text class="pa-6">
          <v-row class="mb-6">
            <v-col cols="12" md="6">
              <div class="text-overline text-medium-emphasis font-weight-black mb-2">Thông tin khách hàng</div>
              <p class="mb-1 text-subtitle-1">Họ tên: <span class="font-weight-black">{{ selectedOrder.tenKhachHang || 'Khách lẻ' }}</span></p>
              <p class="text-subtitle-1">Số điện thoại: <span class="font-weight-black">{{ selectedOrder.soDienThoai }}</span></p>
            </v-col>
            <v-col cols="12" md="6">
              <div class="text-overline text-medium-emphasis font-weight-black mb-2">Thông tin thanh toán</div>
              <p class="mb-1 text-subtitle-1">Ngày mua: <span class="font-weight-black">{{ formatDate(selectedOrder.ngayTao) }}</span></p>
              <p class="text-h5 font-weight-black text-error">Tổng tiền: {{ formatCurrency(selectedOrder.tongTien) }}</p>
            </v-col>
          </v-row>

          <v-divider class="mb-6"></v-divider>

          <div class="text-overline text-medium-emphasis font-weight-black mb-4">Danh sách sản phẩm</div>
          <table class="w-100 invoice-table border">
            <thead class="bg-grey-lighten-4">
              <tr>
                <th class="pa-3 text-start border-b">Tên sản phẩm</th>
                <th class="pa-3 text-center border-b">Số lượng</th>
                <th class="pa-3 text-end border-b">Đơn giá</th>
                <th class="pa-3 text-end border-b">Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in selectedOrder.items" :key="item.name">
                <td class="pa-3 border-b">{{ item.name }}</td>
                <td class="pa-3 text-center border-b font-weight-bold">{{ item.quantity }}</td>
                <td class="pa-3 text-end border-b">{{ formatCurrency(item.price) }}</td>
                <td class="pa-3 text-end border-b font-weight-black">{{ formatCurrency(item.price * item.quantity) }}</td>
              </tr>
            </tbody>
          </table>
        </v-card-text>

        <v-card-actions class="pa-4 bg-grey-lighten-4">
          <v-spacer></v-spacer>
          <v-btn variant="text" class="text-none font-weight-bold" @click="showOrderDetailDialog = false">Thoát</v-btn>
          <v-btn color="primary" variant="flat" rounded="md" class="px-8 text-none font-weight-bold">
            In hóa đơn (PDF)
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<style scoped>
.gray-bg { background-color: #f8fafc; }
.text-dark {
  color: #000000 !important;
}
.font-body { font-family: 'Public Sans', sans-serif; }
.invoice-table { border-collapse: collapse; overflow: hidden; border-radius: 8px; }
.invoice-table th, .invoice-table td { border-bottom: 1px solid #e2e8f0; }
</style>
