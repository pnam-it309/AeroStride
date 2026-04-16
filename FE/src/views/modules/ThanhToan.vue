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
  <div>
    <!-- Header -->
    <div class="d-flex justify-space-between align-center mb-6">
      <div>
        <h1 class="text-h4 font-weight-bold">Quản lý thanh toán</h1>
        <p class="text-subtitle-1 text-medium-emphasis">Quản lý giao dịch và phương thức thanh toán</p>
      </div>
    </div>

    <!-- Statistics Cards -->
    <v-row class="mb-4">
      <v-col cols="12" sm="6" md="3">
        <v-card elevation="2" class="pa-4">
          <div class="d-flex align-center justify-space-between">
            <div>
              <p class="text-subtitle-2 text-medium-emphasis mb-1">Tổng doanh thu</p>
              <p class="text-h4 font-weight-bold text-primary">{{ formatCurrency(statistics.totalRevenue) }}</p>
            </div>
            <v-icon size="40" color="primary">mdi-currency-usd</v-icon>
          </div>
        </v-card>
      </v-col>
      <v-col cols="12" sm="6" md="3">
        <v-card elevation="2" class="pa-4">
          <div class="d-flex align-center justify-space-between">
            <div>
              <p class="text-subtitle-2 text-medium-emphasis mb-1">Doanh thu hôm nay</p>
              <p class="text-h4 font-weight-bold text-success">{{ formatCurrency(statistics.todayRevenue) }}</p>
            </div>
            <v-icon size="40" color="success">mdi-trending-up</v-icon>
          </div>
        </v-card>
      </v-col>
      <v-col cols="12" sm="6" md="3">
        <v-card elevation="2" class="pa-4">
          <div class="d-flex align-center justify-space-between">
            <div>
              <p class="text-subtitle-2 text-medium-emphasis mb-1">Giao dịch thành công</p>
              <p class="text-h4 font-weight-bold text-info">{{ statistics.successfulPayments }}</p>
            </div>
            <v-icon size="40" color="info">mdi-check-circle</v-icon>
          </div>
        </v-card>
      </v-col>
      <v-col cols="12" sm="6" md="3">
        <v-card elevation="2" class="pa-4">
          <div class="d-flex align-center justify-space-between">
            <div>
              <p class="text-subtitle-2 text-medium-emphasis mb-1">Đã hoàn tiền</p>
              <p class="text-h4 font-weight-bold text-warning">{{ formatCurrency(statistics.refundAmount) }}</p>
            </div>
            <v-icon size="40" color="warning">mdi-cash-refund</v-icon>
          </div>
        </v-card>
      </v-col>
    </v-row>

    <!-- Tabs -->
    <v-card elevation="2">
      <v-tabs v-model="selectedTab" @update:model-value="selectedTab === 'methods' ? loadPaymentMethods() : loadTransactions()">
        <v-tab value="transactions">Giao dịch</v-tab>
        <v-tab value="methods">Phương thức thanh toán</v-tab>
      </v-tabs>

      <v-divider></v-divider>

      <!-- Search -->
      <v-card-text v-if="selectedTab === 'transactions'">
        <v-text-field
          v-model="searchQuery"
          placeholder="Tìm kiếm giao dịch..."
          prepend-inner-icon="mdi-magnify"
          variant="outlined"
          hide-details
          @keyup.enter="handleSearch"
        ></v-text-field>
      </v-card-text>

      <!-- Transactions Table -->
      <v-card-text v-if="selectedTab === 'transactions'">
        <v-table>
          <thead>
            <tr>
              <th>Mã giao dịch</th>
              <th>Khách hàng</th>
              <th>Số tiền</th>
              <th>Phương thức</th>
              <th>Trạng thái</th>
              <th>Ngày tạo</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="text-center py-8">
                <v-progress-circular indeterminate color="primary"></v-progress-circular>
              </td>
            </tr>
            <tr v-else-if="transactions.length === 0">
              <td colspan="7" class="text-center py-8">
                <v-icon size="48" color="grey-lighten-1" class="mb-2">mdi-receipt-outline</v-icon>
                <p class="text-medium-emphasis">Không tìm thấy giao dịch nào</p>
              </td>
            </tr>
            <tr v-for="transaction in transactions" :key="transaction.id">
              <td class="font-weight-medium">{{ transaction.transactionCode }}</td>
              <td>{{ transaction.customerName }}</td>
              <td class="font-weight-medium">{{ formatCurrency(transaction.amount) }}</td>
              <td>{{ transaction.paymentMethod }}</td>
              <td>
                <v-chip 
                  :color="getStatusColor(transaction.status)"
                  
                  variant="tonal"
                >
                  {{ getStatusText(transaction.status) }}
                </v-chip>
              </td>
              <td>{{ formatDate(transaction.createdAt) }}</td>
              <td>
                <v-btn
                  icon="mdi-eye"
                  variant="text"
                  color="info"
                  
                ></v-btn>
                <v-btn
                  icon="mdi-cash-refund"
                  variant="text"
                  color="warning"
                  
                  v-if="transaction.status === 'success'"
                  @click="refundPayment(transaction.id)"
                ></v-btn>
              </td>
            </tr>
          </tbody>
        </v-table>

        <!-- Pagination -->
        <v-pagination
          v-if="pagination.total > pagination.itemsPerPage"
          v-model="pagination.page"
          :length="Math.ceil(pagination.total / pagination.itemsPerPage)"
          @update:model-value="loadTransactions"
          class="mt-4"
        ></v-pagination>
      </v-card-text>

      <!-- Payment Methods -->
      <v-card-text v-if="selectedTab === 'methods'">
        <v-row>
          <v-col 
            v-for="method in paymentMethods" 
            :key="method.id"
            cols="12" 
            sm="6" 
            md="4"
          >
            <v-card variant="tonal" :color="method.status === 'active' ? 'primary' : 'grey'" class="pa-4">
              <div class="d-flex align-center justify-space-between">
                <div>
                  <h3 class="text-h6 font-weight-medium">{{ method.name }}</h3>
                  <p class="text-body-2 text-medium-emphasis">{{ method.description }}</p>
                </div>
                <v-icon size="32">{{ method.icon }}</v-icon>
              </div>
              <div class="mt-3">
                <v-chip 
                  :color="method.status === 'active' ? 'success' : 'error'"
                  
                  variant="tonal"
                >
                  {{ method.status === 'active' ? 'Hoạt động' : 'Không hoạt động' }}
                </v-chip>
              </div>
            </v-card>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
  </div>
</template>



