<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';

// Reusable Components
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import { downloadFile } from '@/utils/fileUtils';
import { EyeIcon, ReceiptIcon, PrinterIcon } from 'vue-tabler-icons';

const router = useRouter();
const loading = ref(false);
const isRefreshing = ref(false);
const orders = ref([]);
const counts = ref({ all: 0, pending: 0, confirmed: 0, shipping: 0, completed: 0, cancelled: 0 });
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
  { text: 'Mã & Thời gian', align: 'center', width: '180px' },
  { text: 'Khách hàng', align: 'center', width: '180px' },
  { text: 'Loại đơn & Phí ship', align: 'center', width: '180px' },
  { text: 'Thanh toán', align: 'center', width: '200px' },
  { text: 'Ghi chú & NV', align: 'center', width: '200px' },
  { text: 'Trạng thái', align: 'center', width: '150px' },
  { text: 'Thao tác', align: 'center', width: '100px' }
];

const loadCounts = async () => {
  try {
    const data = await dichVuHoaDon.laySoLuongHoaDon();
    counts.value = {
      all: data.all || 0,
      pending: data['1'] || 0,
      confirmed: data['2'] || 0,
      shipping: data['6'] || 0,
      delivering: data['3'] || 0,
      completed: data['4'] || 0,
      cancelled: data['5'] || 0
    };
  } catch (e) { console.error('Error counts:', e); }
};

const loadOrders = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
      size: pagination.value.size,
      search: filters.value.keyword || null,
      trangThai: filters.value.trangThai
    };
    const response = await dichVuHoaDon.layHoaDonPhanTrang(params);
    orders.value = response.content || response;
    pagination.value.totalElements = response.totalElements || orders.value.length;
    pagination.value.totalPages = response.totalPages || 1;
    
    // Tải lại count để số liệu luôn mới
    loadCounts();
  } catch (error) { console.error(error); } finally { loading.value = false; }
};



const handleRefresh = async () => {
  isRefreshing.value = true;
  filters.value = { keyword: '', trangThai: null };
  pagination.value.page = 1;
  await loadOrders();
  setTimeout(() => isRefreshing.value = false, 800);
};

const handleExport = async () => {
  try {
    const params = {
      search: filters.value.keyword || null,
      trangThai: filters.value.trangThai
    };
    const blob = await dichVuHoaDon.xuatExcelHoaDon(params);
    downloadFile(blob, 'danh_sach_hoa_don.xlsx');
  } catch (error) {
    console.error('Lỗi xuất Excel:', error);
  }
};

const formatPrice = (amount) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
};

const handlePrint = async (orderId) => {
  try {
    const html = await dichVuHoaDon.inHoaDon(orderId);
    // Mở cửa sổ mới để in
    const printWindow = window.open('', '_blank', 'width=900,height=800');
    printWindow.document.write(html);
    printWindow.document.close();
    // printWindow.print(); // Sẽ in ngay khi load xong nếu đã có script trong HTML
  } catch (error) {
    console.error('Lỗi in hóa đơn:', error);
  }
};

const formatDateTime = (timestamp) => {
  if (!timestamp) return 'N/A';
  return new Date(timestamp).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
};

const getStatusInfo = (status) => {
  switch (status) {
    case 1: return { text: 'Chờ xác nhận', color: 'warning' };
    case 2: return { text: 'Đã xác nhận', color: 'info' };
    case 3: return { text: 'Đang giao', color: 'primary' };
    case 4: return { text: 'Hoàn thành', color: 'success' };
    case 5: return { text: 'Đã hủy', color: 'error' };
    default: return { text: 'Không xác định', color: 'grey' };
  }
};

const viewOrderDetail = (order) => {
  router.push(`/hoa-don/chi-tiet/${order.id}`);
};

onMounted(() => loadOrders());
</script>

<template>
  <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
    <!-- Header -->
    <div class="d-flex justify-space-between align-center mb-6">
      <div>
        <h1 class="text-h4 font-weight-bold">Quản lý hóa đơn</h1>
        <p class="text-subtitle-1 text-medium-emphasis">Tra cứu lịch sử đơn hàng và trạng thái giao dịch</p>
      </div>
    </div>

    <!-- Status Tabs (Image 3 Request) -->
    <v-card class="rounded-lg mb-4 border-0 shadow-sm overflow-hidden" elevation="0">
      <v-tabs
        v-model="filters.trangThai"
        bg-color="white"
        color="primary"
        align-tabs="start"
        @update:model-value="loadOrders"
        height="50"
      >
        <v-tab :value="null" class="text-none font-weight-bold px-6">
          <v-icon start size="18">mdi-package-variant-closed</v-icon>
          Tất cả
          <v-chip size="x-small" color="primary" class="ml-2 font-weight-bold" variant="tonal">{{ counts.all }}</v-chip>
        </v-tab>
        <v-tab :value="1" class="text-none font-weight-bold px-6">
          <v-icon start size="18">mdi-clock-outline</v-icon>
          Chờ xác nhận
          <v-chip size="x-small" color="#ff8a65" class="ml-2 font-weight-bold" variant="flat">{{ counts.pending }}</v-chip>
        </v-tab>
        <v-tab :value="2" class="text-none font-weight-bold px-6">
          <v-icon start size="18">mdi-check-circle-outline</v-icon>
          Đã xác nhận
          <v-chip size="x-small" color="#ff8a65" class="ml-2 font-weight-bold" variant="flat">{{ counts.confirmed }}</v-chip>
        </v-tab>
        <v-tab :value="6" class="text-none font-weight-bold px-6">
          <v-icon start size="18">mdi-truck-fast-outline</v-icon>
          Chờ giao
          <v-chip size="x-small" color="#ff8a65" class="ml-2 font-weight-bold" variant="flat">{{ counts.shipping }}</v-chip>
        </v-tab>
        <v-tab :value="3" class="text-none font-weight-bold px-6">
          <v-icon start size="18">mdi-truck-delivery-outline</v-icon>
          Đang giao
          <v-chip size="x-small" color="#ff8a65" class="ml-2 font-weight-bold" variant="flat">{{ counts.delivering }}</v-chip>
        </v-tab>
        <v-tab :value="4" class="text-none font-weight-bold px-6">
          <v-icon start size="18">mdi-checkbox-marked-circle-outline</v-icon>
          Hoàn thành
          <v-chip size="x-small" color="#ff8a65" class="ml-2 font-weight-bold" variant="flat">{{ counts.completed }}</v-chip>
        </v-tab>
        <v-tab :value="5" class="text-none font-weight-bold px-6">
          <v-icon start size="18">mdi-close-circle-outline</v-icon>
          Đã hủy
          <v-chip size="x-small" color="#ff8a65" class="ml-2 font-weight-bold" variant="flat">{{ counts.cancelled }}</v-chip>
        </v-tab>
      </v-tabs>
    </v-card>

    <!-- 1. FILTER -->
    <AdminFilter :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
      <v-col cols="12" md="6">
        <v-text-field
          v-model="filters.keyword"
          label="Tìm kiếm hóa đơn"
          placeholder="Tên khách hàng, Mã HĐ, SĐT..."
          persistent-placeholder
          variant="outlined"
          density="comfortable"
          hide-details
          prepend-inner-icon="mdi-magnify"
          class="font-weight-bold"
          rounded="lg"
          @keyup.enter="loadOrders"
        ></v-text-field>
      </v-col>
    </AdminFilter>

    <!-- 2. TABLE -->
    <AdminTable
      title="Danh sách hóa đơn"
      :showAddButton="false"
      show-export-button
      :headers="tableHeaders"
      :items="orders"
      :total-count="pagination.totalElements"
      :loading="loading"
      @export="handleExport"
    >
      <template #row="{ item }">
        <tr class="data-row">
          <td class="data-cell">
            <div class="text-subtitle-2 font-weight-bold text-primary">{{ item.maHoaDon }}</div>
            <div class="text-caption font-weight-bold text-secondary">{{ formatDateTime(item.ngayTao) }}</div>
          </td>
          
          <td class="data-cell">
            <div class="text-subtitle-2 font-weight-bold">{{ item.tenKhachHang || 'Khách vãng lai' }}</div>
            <div class="text-caption text-secondary">{{ item.soDienThoai || 'N/A' }}</div>
          </td>

          <td class="data-cell">
            <v-chip
              :color="item.loaiDon === 'TẠI QUẦY' ? 'indigo' : 'orange'"
              class="font-weight-bold mb-1"
              variant="tonal"
              size="x-small"
            >
              {{ item.loaiDon }}
            </v-chip>
            <div class="text-caption text-info font-weight-bold" v-if="item.phiVanChuyen > 0">
              + {{ formatPrice(item.phiVanChuyen) }} ship
            </div>
          </td>

          <td class="data-cell">
            <div class="text-caption text-decoration-line-through text-secondary" v-if="item.tongTienSauGiam && item.tongTienSauGiam < item.tongTien">
              {{ formatPrice(item.tongTien) }}
            </div>
            <div class="text-subtitle-1 font-weight-black text-error">
              {{ formatPrice(item.tongTienSauGiam || item.tongTien) }}
            </div>
          </td>

          <td class="data-cell">
            <div class="text-caption text-truncate" style="max-width: 150px;">{{ item.ghiChu || 'Không có ghi chú' }}</div>
            <div class="text-caption font-italic text-primary">NV: {{ item.tenNhanVien || 'Hệ thống' }}</div>
          </td>

          <td class="data-cell">
            <v-chip 
              :color="getStatusInfo(item.trangThai).color"
              variant="flat"
              class="font-weight-bold px-4"
              size="small"
            >
              {{ getStatusInfo(item.trangThai).text }}
            </v-chip>
          </td>

          <td class="data-cell" style="text-align: center;">
            <div class="d-flex align-center justify-center gap-1">
              <v-btn icon variant="tonal" size="32" color="primary" class="rounded-lg" @click.stop="viewOrderDetail(item)">
                <EyeIcon size="18" />
                <v-tooltip activator="parent" location="top">Xem chi tiết</v-tooltip>
              </v-btn>
              <v-btn icon variant="tonal" size="32" color="success" class="rounded-lg" @click.stop="handlePrint(item.id)">
                <PrinterIcon size="18" />
                <v-tooltip activator="parent" location="top">In hóa đơn</v-tooltip>
              </v-btn>
            </div>
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
            <span class="font-weight-medium">Chi tiết hóa đơn #{{ selectedOrder.maHoaDon }}</span>
          </div>
          <v-chip :color="getStatusInfo(selectedOrder.trangThai).color" variant="flat" class="font-weight-bold px-6">
            {{ getStatusInfo(selectedOrder.trangThai).text }}
          </v-chip>
        </v-card-title>
        
        <v-card-text class="pa-6">
          <v-row class="mb-6">
            <v-col cols="12" md="6">
              <div class="text-overline text-medium-emphasis font-weight-medium mb-2">Thông tin khách hàng</div>
              <p class="mb-1 text-subtitle-1">Họ tên: <span class="font-weight-medium">{{ selectedOrder.tenKhachHang || 'Khách lẻ' }}</span></p>
              <p class="text-subtitle-1">Số điện thoại: <span class="font-weight-medium">{{ selectedOrder.soDienThoai }}</span></p>
            </v-col>
            <v-col cols="12" md="6">
              <div class="text-overline text-medium-emphasis font-weight-medium mb-2">Thông tin thanh toán</div>
              <p class="mb-1 text-subtitle-1">Ngày mua: <span class="font-weight-medium">{{ formatDate(selectedOrder.ngayTao) }}</span></p>
              <p class="text-h5 font-weight-medium text-error">Tổng tiền: {{ formatCurrency(selectedOrder.tongTien) }}</p>
            </v-col>
          </v-row>

          <v-divider class="mb-6"></v-divider>

          <div class="text-overline text-medium-emphasis font-weight-medium mb-4">Danh sách sản phẩm</div>
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
                <td class="pa-3 text-end border-b font-weight-medium">{{ formatCurrency(item.price * item.quantity) }}</td>
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
