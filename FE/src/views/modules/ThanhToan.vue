<script setup>
import { ref, onMounted } from 'vue';
import { dichVuThanhToan } from '@/services/sales/dichVuThanhToan';

const loading = ref(false);
const transactions = ref([]);
const paymentMethods = ref([]);
const selectedTab = ref('transactions');
const searchQuery = ref('');
const statistics = ref({
  totalRevenue: 0,
  todayRevenue: 0,
  successfulPayments: 0,
  failedPayments: 0,
  refundAmount: 0
});

const pagination = ref({
  page: 1,
  itemsPerPage: 10,
  total: 0
});

const loadTransactions = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.itemsPerPage,
      search: searchQuery.value
    };
    const response = await dichVuThanhToan.layGiaoDichThanhToan(params);
    transactions.value = response.content || response;
    pagination.value.total = response.totalElements || response.length;
  } catch (error) {
    console.error('Error loading transactions:', error);
  } finally {
    loading.value = false;
  }
};

const loadPaymentMethods = async () => {
  loading.value = true;
  try {
    const methods = await dichVuThanhToan.layPhuongThucThanhToan();
    paymentMethods.value = methods;
  } catch (error) {
    console.error('Error loading payment methods:', error);
  } finally {
    loading.value = false;
  }
};

const loadStatistics = async () => {
  try {
    const stats = await dichVuThanhToan.layThongKeThanhToan();
    statistics.value = stats;
  } catch (error) {
    console.error('Error loading statistics:', error);
  }
};

const refundPayment = async (paymentId) => {
  try {
    await dichVuThanhToan.hoanTien(paymentId, { reason: 'Yêu cầu hoàn tiền' });
    loadTransactions(); // Reload transactions
  } catch (error) {
    console.error('Error refunding payment:', error);
  }
};

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount);
};

const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString('vi-VN');
};

const getStatusColor = (status) => {
  switch (status) {
    case 'success': return 'success';
    case 'failed': return 'error';
    case 'pending': return 'warning';
    case 'refunded': return 'info';
    default: return 'grey';
  }
};

const getStatusText = (status) => {
  switch (status) {
    case 'success': return 'Thành công';
    case 'failed': return 'Thất bại';
    case 'pending': return 'Đang xử lý';
    case 'refunded': return 'Đã hoàn tiền';
    default: return 'Không xác định';
  }
};

const handleSearch = () => {
  pagination.value.page = 1;
  if (selectedTab.value === 'transactions') {
    loadTransactions();
  }
};

onMounted(() => {
  loadTransactions();
  loadStatistics();
});
</script>

<template>
  <div class="pa-6 font-body">
    <!-- Header -->
    <div class="header-section mb-8">
      <div>
        <h1 class="text-h4 font-black text-dark mb-1">Quản lý thanh toán</h1>
        <p class="text-subtitle-1 text-slate-500 font-bold">Quản lý giao dịch và phương thức thanh toán hệ thống</p>
      </div>
    </div>

    <!-- Statistics Cards -->
    <v-row class="mb-4">
      <v-col cols="12" sm="6" md="3">
        <v-card class="premium-card pa-6 h-100">
          <div class="d-flex align-center justify-space-between mb-2">
            <div class="icon-blob bg-primary-light">
              <v-icon color="primary">mdi-currency-usd</v-icon>
            </div>
          </div>
          <div>
            <p class="text-caption text-slate-500 font-bold text-uppercase mb-1">Tổng doanh thu</p>
            <p class="text-h5 font-black text-dark">{{ formatCurrency(statistics.totalRevenue) }}</p>
          </div>
        </v-card>
      </v-col>
      <v-col cols="12" sm="6" md="3">
        <v-card class="premium-card pa-6 h-100">
          <div class="d-flex align-center justify-space-between mb-2">
            <div class="icon-blob bg-success-light">
              <v-icon color="success">mdi-trending-up</v-icon>
            </div>
          </div>
          <div>
            <p class="text-caption text-slate-500 font-bold text-uppercase mb-1">Doanh thu hôm nay</p>
            <p class="text-h5 font-black text-dark">{{ formatCurrency(statistics.todayRevenue) }}</p>
          </div>
        </v-card>
      </v-col>
      <v-col cols="12" sm="6" md="3">
        <v-card class="premium-card pa-6 h-100">
          <div class="d-flex align-center justify-space-between mb-2">
            <div class="icon-blob bg-info-light">
              <v-icon color="info">mdi-check-circle</v-icon>
            </div>
          </div>
          <div>
            <p class="text-caption text-slate-500 font-bold text-uppercase mb-1">Thành công</p>
            <p class="text-h5 font-black text-dark">{{ statistics.successfulPayments }} GD</p>
          </div>
        </v-card>
      </v-col>
      <v-col cols="12" sm="6" md="3">
        <v-card class="premium-card pa-6 h-100">
          <div class="d-flex align-center justify-space-between mb-2">
            <div class="icon-blob bg-warning-light">
              <v-icon color="warning">mdi-cash-refund</v-icon>
            </div>
          </div>
          <div>
            <p class="text-caption text-slate-500 font-bold text-uppercase mb-1">Số tiền hoàn</p>
            <p class="text-h5 font-black text-dark">{{ formatCurrency(statistics.refundAmount) }}</p>
          </div>
        </v-card>
      </v-col>
    </v-row>

    <!-- Content Tabs -->
    <v-card class="premium-card mt-6">
      <v-tabs v-model="selectedTab" class="equal-tabs bg-slate-50" color="primary" 
              @update:model-value="selectedTab === 'methods' ? loadPaymentMethods() : loadTransactions()">
        <v-tab value="transactions" class="font-bold">Giao dịch hệ thống</v-tab>
        <v-tab value="methods" class="font-bold">Phương thức thanh toán</v-tab>
      </v-tabs>

      <v-divider></v-divider>

      <!-- Search & Filters -->
      <div v-if="selectedTab === 'transactions'" class="pa-6 pb-0">
        <div class="filter-field-label">Tìm kiếm giao dịch</div>
        <v-text-field
          v-model="searchQuery"
          placeholder="Mã giao dịch, tên khách hàng..."
          prepend-inner-icon="mdi-magnify"
          variant="outlined"
          density="compact"
          hide-details
          class="bg-white"
          @keyup.enter="handleSearch"
        ></v-text-field>
      </div>

      <!-- Transactions Table -->
      <v-card-text v-if="selectedTab === 'transactions'" class="pa-0">
        <div class="admin-table-container border-0 rounded-0 mt-4 overflow-hidden">
          <table class="native-admin-table">
            <thead>
              <tr>
                <th class="header-cell">Mã giao dịch</th>
                <th class="header-cell">Khách hàng</th>
                <th class="header-cell text-right">Số tiền</th>
                <th class="header-cell">Phương thức</th>
                <th class="header-cell">Trạng thái</th>
                <th class="header-cell">Ngày tạo</th>
                <th class="header-cell text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="7" class="text-center py-12">
                  <v-progress-circular indeterminate color="primary"></v-progress-circular>
                </td>
              </tr>
              <tr v-else-if="transactions.length === 0">
                <td colspan="7" class="text-center py-12">
                  <v-icon size="48" color="slate-200" class="mb-4">mdi-receipt-outline</v-icon>
                  <p class="text-slate-500 font-bold">Không tìm thấy giao dịch nào</p>
                </td>
              </tr>
              <tr v-for="transaction in transactions" :key="transaction.id" class="data-row">
                <td class="data-cell"><span class="mono-font">{{ transaction.transactionCode }}</span></td>
                <td class="data-cell">{{ transaction.customerName }}</td>
                <td class="data-cell text-right font-black text-primary">{{ formatCurrency(transaction.amount) }}</td>
                <td class="data-cell">{{ transaction.paymentMethod }}</td>
                <td class="data-cell">
                  <v-chip :color="getStatusColor(transaction.status)" variant="flat" size="small" class="status-chip font-bold px-3">
                    {{ getStatusText(transaction.status) }}
                  </v-chip>
                </td>
                <td class="data-cell text-slate-500">{{ formatDate(transaction.createdAt) }}</td>
                <td class="data-cell text-right">
                  <div class="action-controls justify-end">
                    <v-btn icon class="action-icon-btn" color="info">
                      <v-icon size="20">mdi-eye</v-icon>
                    </v-btn>
                    <v-btn v-if="transaction.status === 'success'" icon class="action-icon-btn" color="warning" 
                           @click="refundPayment(transaction.id)">
                      <v-icon size="20">mdi-cash-refund</v-icon>
                    </v-btn>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div class="pa-6 pt-0 d-flex justify-end">
          <v-pagination
            v-if="pagination.total > pagination.itemsPerPage"
            v-model="pagination.page"
            :length="Math.ceil(pagination.total / pagination.itemsPerPage)"
            @update:model-value="loadTransactions"
            density="compact"
            active-color="primary"
          ></v-pagination>
        </div>
      </v-card-text>

      <!-- Payment Methods -->
      <v-card-text v-if="selectedTab === 'methods'" class="pa-6">
        <v-row>
          <v-col v-for="method in paymentMethods" :key="method.id" cols="12" sm="6" md="4">
            <v-card class="pa-5 border rounded-xl bg-slate-50 hover-addr-card h-100">
              <div class="d-flex align-center justify-space-between mb-4">
                <div class="icon-blob" :class="method.status === 'active' ? 'bg-primary-light' : 'bg-slate-200'">
                  <v-icon :color="method.status === 'active' ? 'primary' : 'slate-500'">{{ method.icon }}</v-icon>
                </div>
                <v-chip :color="method.status === 'active' ? 'success' : 'error'" variant="flat" size="x-small" class="font-bold">
                  {{ method.status === 'active' ? 'Hoạt động' : 'Đã tắt' }}
                </v-chip>
              </div>
              <h3 class="text-h6 font-black text-dark mb-1">{{ method.name }}</h3>
              <p class="text-body-2 text-slate-500 leading-relaxed">{{ method.description }}</p>
            </v-card>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
  </div>
</template>

<style scoped>
.bg-info-light { background: #e0f2fe; }
.bg-warning-light { background: #fff7ed; }
.bg-primary-light { background: #eff6ff; }
.bg-success-light { background: #ecfdf5; }
</style>



